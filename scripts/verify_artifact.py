#!/usr/bin/env python3
"""Check a packaged Kotlin facade and its pinned runtime archives without loading code."""
import argparse,hashlib,json,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def main():
 p=argparse.ArgumentParser();p.add_argument('--jar',type=Path,required=True);p.add_argument('--maven-repo',type=Path,required=True);a=p.parse_args()
 lock=json.loads((ROOT/'dependencies.lock.json').read_text())
 for d in lock['runtime']:
  base=a.maven_repo/d['group'].replace('.','/')/d['artifact']/d['version']
  for ext in ('jar','pom'):
   file=base/(d['artifact']+'-'+d['version']+'.'+ext)
   assert sha(file)==d[ext+'_sha256'],'Dependency differs: '+str(file)
 with zipfile.ZipFile(a.jar) as z:
  for name in ('operation-map.json','runtime-contract.lock.json','dependencies.lock.json'):
   assert z.read('META-INF/webmania/'+name)==(ROOT/name).read_bytes(),'Packaged identity differs: '+name
  assert z.read('META-INF/LICENSE')==(ROOT/'LICENSE').read_bytes()
  assert z.read('META-INF/NOTICE')==(ROOT/'NOTICE').read_bytes()
  classes=[n for n in z.namelist() if n.endswith('.class')]
  assert classes and all(n.startswith('com/webmania/sdk/kotlin/') for n in classes),'Unexpected bundled runtime classes'
  assert all(int.from_bytes(z.read(n)[6:8],'big')==61 for n in classes),'Bytecode target is not JDK 17'
  assert 'Automatic-Module-Name: com.webmania.sdk.kotlin' in z.read('META-INF/MANIFEST.MF').decode()
 print(json.dumps({'jar_sha256':sha(a.jar),'class_files':len(classes),'runtime_archives_checked':len(lock['runtime']),'metadata_and_notices':True}))
if __name__=='__main__':main()
