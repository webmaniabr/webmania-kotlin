#!/usr/bin/env python3
"""Generate scoped Kotlin facades from a pinned Java archive and source inventory.
Run inside a local container with the pinned Java SDK archive available.
"""
import argparse,hashlib,json,struct,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA_SHA='29e262427ca8ec15918c2072020c674c64f88be9427efa949b92ddd802f494e3'
# contract_digest of the contracts bundled in the pinned Java archive.
DIGEST='371a499f74012bb6b41f23642869dee4954e8394dcba7642edc701aedb3dd59e'
# Report downloads (legacy and current) share NfeFilesClient and its explicit access modes.
SCOPES={'api1/fiscal.openapi.json':'Nfe','api1/files.openapi.json':'NfeFiles','api2/reports.openapi.json':'NfeFiles','api1/b2b.openapi.json':'Reseller','api2/emitter-files.openapi.json':'EmitterFiles'}
def require(condition, message):
 if not condition:raise ValueError(message)

def group(op):
 if op['module'] in SCOPES:return SCOPES[op['module']]
 return 'Monitor' if '/monitor-' in op['module'] else 'Emitter'
def api(blob):
 # Read the classfile member table rather than assuming source and archive agree.
 pos=8; count=struct.unpack_from('>H',blob,pos)[0];pos+=2;cp=[None]*count;i=1
 while i<count:
  tag=blob[pos];pos+=1
  if tag==1:
   size=struct.unpack_from('>H',blob,pos)[0];pos+=2;cp[i]=blob[pos:pos+size].decode('utf-8');pos+=size
  elif tag in (3,4):pos+=4
  elif tag in (5,6):pos+=8;i+=1
  elif tag in (7,8,16,19,20):pos+=2
  elif tag in (9,10,11,12,17,18):pos+=4
  elif tag==15:pos+=3
  else:raise ValueError('Unsupported class constant '+str(tag))
  i+=1
 pos+=6;interfaces=struct.unpack_from('>H',blob,pos)[0];pos+=2+2*interfaces
 result=[]
 for kind in ('field','method'):
  size=struct.unpack_from('>H',blob,pos)[0];pos+=2
  for _ in range(size):
   access,name,descriptor,attrs=struct.unpack_from('>HHHH',blob,pos);pos+=8
   if access&1:result.append((kind,cp[name],cp[descriptor]))
   for _ in range(attrs):
    _,length=struct.unpack_from('>HI',blob,pos);pos+=6+length
 return result

def main():
 p=argparse.ArgumentParser();p.add_argument('--java',type=Path,required=True);p.add_argument('--jar',type=Path,required=True);p.add_argument('--check',action='store_true');a=p.parse_args()
 require(hashlib.sha256(a.jar.read_bytes()).hexdigest()==JAVA_SHA,'Pinned Java artifact SHA-256 differs')
 src=a.java/'src/main'
 archive=zipfile.ZipFile(a.jar)
 rawops=archive.read('contracts/operations.json')
 require(rawops==(src/'resources/contracts/operations.json').read_bytes(),'Source operation metadata differs from pinned archive')
 provenance=json.loads(archive.read('META-INF/contract-provenance.json'))
 require(provenance['contract_digest']==DIGEST,'Pinned archive contract digest differs')
 modules=json.loads(archive.read('contracts/manifest.json'))['contracts']
 require(sorted(modules)==sorted(k for k in provenance['sha256'] if k!='contracts/manifest.json'),'Bundled modules differ from the provenance')
 for name,digest in provenance['sha256'].items():require(hashlib.sha256(archive.read(name)).hexdigest()==digest,'Bundled contract provenance mismatch: '+name)
 ops=json.loads(rawops);declared=[op for m in modules for path in json.loads(archive.read(m))['paths'].values() for method,op in path.items() if method in ('get','post','put','delete','patch')]
 require(len(ops)==len(declared) and sum(bool(x['enabled']) for x in ops)==sum(op.get('x-sdk-generation-enabled',True) for op in declared),'Operation metadata differs from the bundled modules')
 contracts={n:hashlib.sha256(archive.read(n)).hexdigest() for n in sorted(archive.namelist()) if n.startswith('contracts/') and n.endswith('.json')}
 for name in contracts:require(archive.read(name)==(src/'resources'/name).read_bytes(),'Source contract differs: '+name)
 javaapi=api(archive.read('com/webmania/sdk/WebmaniaClient.class'))
 downloadapi=api(archive.read('com/webmania/sdk/Download.class'))
 for name,descriptor in [('kind','()Lcom/webmania/sdk/Download$Kind;'),('encrypted','()Z'),('protection','()Lcom/webmania/sdk/Download$Protection;')]:require(('method',name,descriptor) in downloadapi,'Missing pinned file classification API: '+name)
 filekinds=[name for kind,name,descriptor in api(archive.read('com/webmania/sdk/Download$Kind.class')) if kind=='field' and descriptor=='Lcom/webmania/sdk/Download$Kind;']
 require(set(filekinds)=={'PDF','ENCRYPTED_PDF','XML','ZIP','ENCRYPTED_ZIP','XLSX','CSV','HTML'},'Unexpected pinned representation kinds')
 sdkclasses=sorted(name for name in archive.namelist() if name.startswith('com/webmania/sdk/') and name.endswith('.class'))
 sdkresources=sorted(name for name in archive.namelist() if not name.endswith('/') and (name.startswith('contracts/') or name.endswith('.json')))
 modelapi={name:api(archive.read(name)) for name in sorted(archive.namelist()) if name.startswith('com/webmania/sdk/model/') and name.endswith('.class') and '$' not in name}
 expected={str(f.relative_to(src/'java')).replace('.java','.class') for f in (src/'java/com/webmania/sdk/model').rglob('*.java')}
 require(set(modelapi)==expected,'Source model set differs from pinned archive')
 for op in ops:
  descriptor='(Lcom/webmania/sdk/'+op['package'].replace('.','/')+'/'+op['request']+';)Lcom/webmania/sdk/ApiResult;'
  require(('method',op['id'],descriptor) in javaapi,'Pinned runtime method mismatch: '+op['id'])

 output={}; counts={}; methods=[]
 for scope in ['Nfe','Monitor','Emitter','Reseller','NfeFiles','EmitterFiles']:
  cred={'Nfe':'CompanyCredentials','Monitor':'MonitorCredentials','Emitter':'IssuerCredentials','Reseller':'ResellerCredentials','NfeFiles':'NfeFileAuth','EmitterFiles':'EmitterFileAuth'}[scope]
  text=['package com.webmania.sdk.kotlin','',f'/** Typed {scope} operation facade. Blocking calls observe native thread interruption. */',f'class {scope}Client(credentials: {cred}, config: com.webmania.sdk.ClientConfig = clientConfig()): AutoCloseable {{','    private val delegate = engine(config, credentials.raw)','    override fun close() = delegate.close()',f'    override fun toString() = "{scope}Client[protected]"']
  picked=[x for x in ops if group(x)==scope]
  for op in picked:
   oid=op['id'];request='com.webmania.sdk.'+op['package']+'.'+op['request'];response='com.webmania.sdk.Download' if op['binary'] else 'com.webmania.sdk.'+op['package']+'.'+op['response']
   # Operation IDs are stable callable names; scopes prevent methods from being used by other credential clients.
   text += [f'    fun {oid}(request: {request} = {request}(), configure: {request}.() -> Unit = {{}}): com.webmania.sdk.ApiResult<{response}> =',f'        delegate.{oid}(request.apply(configure))']
   methods.append({'operation_id':oid,'scope':scope,'request_type':request,'response_type':response,'enabled':op['enabled'],'module':op['module'],'method':op['method'],'path':op['path']})
  if scope=='Monitor':
   text += ['    fun collectCompanies(request: com.webmania.sdk.model.api2monitorreads.MonitorListCompaniesRequest, options: com.webmania.sdk.PaginationOptions) = delegate.pagination().companies(request, options)', '    fun collectCompanyMunicipalities(request: com.webmania.sdk.model.api2monitorreads.MonitorListCompanyMunicipalitiesRequest, options: com.webmania.sdk.PaginationOptions) = delegate.pagination().companyMunicipalities(request, options)', '    fun collectReports(request: com.webmania.sdk.model.api2monitordocuments.MonitorListReportsRequest, options: com.webmania.sdk.PaginationOptions) = delegate.pagination().reports(request, options)', '    fun waitImport(request: com.webmania.sdk.model.api2monitordocuments.MonitorGetImportStatusRequest, options: com.webmania.sdk.PollingOptions) = delegate.processing().monitorImport(request, options)']
  if scope=='Nfe':text+=['    fun waitReport(request: com.webmania.sdk.model.api1fiscal.FiscalCreateOrPollReportRequest, options: com.webmania.sdk.PollingOptions) = delegate.processing().fiscalReport(request, options)']
  if scope=='NfeFiles':text+=['    /** Downloads the url of a completed report only from the configured report origin and /relatorios/ prefix. */','    fun downloadReport(url: String): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> = delegate.fiscalReportDownload(url)']
  text+=['}'];output[f'src/main/kotlin/com/webmania/sdk/kotlin/{scope}Client.kt']='\n'.join(text)+'\n';counts[scope]=len(picked)
 aliases=constructors=0
 for module in sorted((src/'java/com/webmania/sdk/model').iterdir()):
  if not module.is_dir():continue
  text=['package com.webmania.sdk.kotlin.models.'+module.name,'','// Fixed aliases preserve the exact qualified Java DTO runtime; no data or enums are erased.']
  for file in sorted(module.glob('*.java')):
   name=file.stem;qualified='com.webmania.sdk.model.'+module.name+'.'+name
   text.append(f'typealias {name} = {qualified}');aliases+=1
   if ('method','<init>','()V') in modelapi[qualified.replace('.','/')+'.class']:
    factory=name[0].lower()+name[1:];text += [f'fun {factory}(configure: {name}.() -> Unit = {{}}): {name} {{ com.webmania.sdk.kotlin.RuntimePin.check(); return {qualified}().apply(configure) }}'];constructors+=1
  output[f'src/main/kotlin/com/webmania/sdk/kotlin/models/{module.name}/Models.kt']='\n'.join(text)+'\n'
 output['runtime-contract.lock.json']=json.dumps({'java_artifact_sha256':JAVA_SHA,'contract_digest':DIGEST,'bundled_contract_sha256':contracts,'public_model_api_sha256':hashlib.sha256(json.dumps(modelapi,sort_keys=True,separators=(',',':')).encode()).hexdigest(),'model_classes':len(modelapi),'download_api_sha256':hashlib.sha256(json.dumps(downloadapi,separators=(',',':')).encode()).hexdigest(),'file_representation_kinds':filekinds,'runtime_origin_policy':{'sdk_classes':len(sdkclasses),'class_inventory_sha256':hashlib.sha256(('\n'.join(sdkclasses)+'\n').encode()).hexdigest(),'initialize_classes':False,'resources':sdkresources,'resource_origin_and_bytes':True}},indent=2)+'\n'
 output['operation-map.json']=json.dumps({'contract_digest':DIGEST,'java_artifact_sha256':JAVA_SHA,'operations':methods,'scopes':counts,'model_aliases':aliases,'typed_model_factories':constructors,'blocking_api':True,'coroutine_api':False,'typed_factory_runtime_pin':True,'default_config_runtime_pin':True},indent=2)+'\n'
 managed={str(f.relative_to(ROOT)) for f in (ROOT/'src/main/kotlin/com/webmania/sdk/kotlin/models').glob('*/Models.kt')} | {str(f.relative_to(ROOT)) for f in (ROOT/'src/main/kotlin/com/webmania/sdk/kotlin').glob('*Client.kt')}
 require(not (managed-set(output)),'Unexpected generated files: '+str(managed-set(output)))
 for name,content in output.items():
  file=ROOT/name
  if a.check:
   require(file.read_text()==content,'generated drift '+name)
  else:file.parent.mkdir(parents=True,exist_ok=True);file.write_text(content)
 print(json.dumps({'operations':len(methods),'enabled':sum(bool(x['enabled']) for x in ops),'scopes':counts,'aliases':aliases,'typedfactories':constructors,'check':a.check}))
if __name__=='__main__':main()
