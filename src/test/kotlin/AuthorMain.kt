package com.webmania.sdk

import com.webmania.sdk.kotlin.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private var assertions = 0
private var transfers = 0
private var schemaCases = 0
private var fileControls = 0
private fun checkThat(ok: Boolean, note: String) { assertions++; check(ok) { note } }
private fun data(file: String) = Json.parse(checkNotNull(object {}.javaClass.getResourceAsStream("/" + file)).use { it.readAllBytes() })
private val fixtures by lazy { data("operation-fixtures.json").associateBy { it.path("id").asText() } }
private fun company()=CompanyCredentials.create("kotlin-key","kotlin-secret","kotlin-access","kotlin-access-secret")
private fun scope(module: String)=when(module){"api1/fiscal.openapi.json"->"Nfe";"api1/files.openapi.json","api2/reports.openapi.json"->"NfeFiles";"api1/b2b.openapi.json"->"Reseller";"api2/emitter-files.openapi.json"->"EmitterFiles";else->if(module.contains("/monitor-"))"Monitor" else "Emitter"}
private fun client(op: JsonNode, mode: String, config: ClientConfig): AutoCloseable {
 return when(scope(op.path("module").asText())) {
  "Nfe"->NfeClient(company(),config)
  "Monitor"->MonitorClient(MonitorCredentials.create("kotlin-monitor"),config)
  "Emitter"->EmitterClient(IssuerCredentials.create("kotlin-issuer"),config)
  "Reseller"->ResellerClient(ResellerCredentials.create("kotlin-key","kotlin-secret","kotlin-access","kotlin-access-secret"),config)
  "NfeFiles"->NfeFilesClient(when{mode=="token"->NfeFileAuth.documentToken("kotlin-document+token/=");mode=="anonymous"||op.path("security").isEmpty->NfeFileAuth.publicReport();else->NfeFileAuth.company(company())},config)
  else->EmitterFilesClient(when{mode=="token"->EmitterFileAuth.documentToken("kotlin-document+token/=");mode=="anonymous"||op.path("security").isEmpty->EmitterFileAuth.anonymous();else->EmitterFileAuth.companyToken("kotlin-files")},config)
 }
}
private fun request(op:JsonNode,value:JsonNode):JsonModel=Class.forName("com.webmania.sdk."+op.path("package").asText()+"."+op.path("request").asText()).getConstructor(JsonNode::class.java).newInstance(value) as JsonModel
private fun call(client: AutoCloseable,id:String,request:JsonModel):Any? = try {
 val method=client.javaClass.methods.single { it.name==id && it.parameterCount==2 }
 method.invoke(client,request,{ _: Any -> Unit })
} catch(e:java.lang.reflect.InvocationTargetException) { throw e.targetException }
private fun error(block:()->Any?):ApiException {try{block();error("Expected SDK error")}catch(e:ApiException){return e}}
private fun matrix(tls:Boolean) {
 SocketFixture(tls).use { server ->
  for(id in Contract.operationIds().sorted()) {
   val op=Contract.operation(id);val schema=Contract.operationSchema(op);val fixture=fixtures[id]?:error("Missing fixture "+id)
   val security=op.path("security").toString();val modes=mutableListOf("default")
   if(security.contains("DocumentToken")||security.contains("FiscalDocumentLinkToken"))modes.add("token")
   if(op.path("binary").asBoolean()&&(schema.path("x-auth-origin-policy").has("unauthenticated")||security.contains("{}")))modes.add("anonymous")
   for(mode in modes) {
    if(op.path("binary").asBoolean()) {
     val content=schema.path("responses").path("200").path("content")
     val media=content.fieldNames().asSequence().firstOrNull{it!="application/json"}?:"application/pdf"
     val body=FileFixtures.valid(media)
     server.reply=SocketFixture.Reply.bytes(media,body)
    } else {
     val status=schema.path("responses").fieldNames().asSequence().filter{it.matches(Regex("2[0-9]{2}"))}.map{it.toInt()}.minOrNull()?:200
     server.reply=SocketFixture.Reply(status,"application/json",Json.bytes(fixture.path("response")),mapOf(),0,0,null,false)
    }
    client(op,mode,server.config(op.path("module").asText())).use { c ->
     val before=server.requests.size;val connectionsBefore=server.connections.get()
     if(!op.path("enabled").asBoolean()) {
      checkThat(error{call(c,id,request(op,fixture.path("input")))}.kind()==ApiException.Kind.CAPABILITY_DISABLED,"Blocked kind "+id)
      checkThat(before==server.requests.size&&server.connections.get()==connectionsBefore,"Blocked before any socket I/O");continue
     }
     val result=call(c,id,request(op,fixture.path("input"))) as ApiResult<*>
     checkThat(server.requests.size==before+1&&server.connections.get()==connectionsBefore+1,"One request and one connection "+id);transfers++
     checkThat(result.httpStatus==server.reply.status(),"HTTP status "+id)
     val sent=server.requests.last();checkThat(sent.method()==op.path("method").asText(),"Method "+id)
     var route=op.path("path").asText()
     for(parameter in schema.path("parameters"))if(parameter.path("in").asText()=="path")route=route.replace("{"+parameter.path("name").asText()+"}",fixture.path("input").path(parameter.path("name").asText()).asText())
     checkThat(sent.target().substringBefore('?')==route,"Path "+id)
     val actualQuery=sent.target().substringAfter('?',"").split('&').filter{it.isNotEmpty()}.map {
      URLDecoder.decode(it.substringBefore('='),StandardCharsets.UTF_8) to URLDecoder.decode(it.substringAfter('=',""),StandardCharsets.UTF_8)
     }.sortedWith(compareBy({it.first},{it.second}))
     val expectedQuery=mutableListOf<Pair<String,String>>()
     for(parameter in schema.path("parameters"))if(parameter.path("in").asText()=="query") {
      val name=parameter.path("name").asText();val value=fixture.path("input").get(name)
      if(value!=null&&!value.isNull) {
       if(value.isArray)value.forEach{expectedQuery.add(name to it.asText())} else expectedQuery.add(name to value.asText())
      }
     }
     if(mode=="token")expectedQuery.add("token" to "kotlin-document+token/=")
     checkThat(actualQuery==expectedQuery.sortedWith(compareBy({it.first},{it.second})),"Exact query "+id+" "+mode)
     if(fixture.path("input").has("body"))checkThat(sent.body().contentEquals(Json.bytes(fixture.path("input").path("body"))),"Exact body "+id) else checkThat(sent.body().isEmpty(),"No body "+id)
     val expectedHeaders=when {
      mode=="token"||mode=="anonymous"||op.path("security").isEmpty->emptyMap()
      security.contains("ConsumerKey")||security.contains("Reseller")->mapOf("x-consumer-key" to "kotlin-key","x-consumer-secret" to "kotlin-secret","x-access-token" to "kotlin-access","x-access-token-secret" to "kotlin-access-secret")
      scope(op.path("module").asText())=="Monitor"->mapOf("authorization" to "Bearer kotlin-monitor")
      scope(op.path("module").asText())=="Emitter"->mapOf("authorization" to "Bearer kotlin-issuer")
      else->mapOf("authorization" to "Bearer kotlin-files")
     }
     val credentialHeaders=sent.headers().filterKeys { it=="authorization"||it.startsWith("x-consumer-")||it.startsWith("x-access-") }
     checkThat(credentialHeaders==expectedHeaders,"Exact credential headers "+id+" "+mode)
     checkThat((mode=="token")==sent.target().contains("token=kotlin-document%2Btoken%2F%3D"),"Opaque token encoding")
     checkThat(!result.toString().contains("controlled"),"Envelope inspect private")
     if(op.path("binary").asBoolean()) {
      val download=result.data() as Download
      checkThat(download.bytes().contentEquals(server.reply.body()),"Binary bytes")
      checkThat(download.fileKind==expectedFileKind(server.reply.media())&&!download.isEncrypted,"Actual file classification "+id)
      val protection=when { mode=="token"->FileProtection.DOCUMENT_TOKEN;op.path("security").isEmpty->FileProtection.PUBLIC_CAPABILITY;mode=="anonymous"->FileProtection.POSSIBLY_PASSWORD_PROTECTED;else->FileProtection.AUTHENTICATED }
      checkThat(download.fileProtection==protection,"Representation and authorization are separate "+id)
      val copy=download.bytes();if(copy.isNotEmpty())copy[0]=(copy[0].toInt() xor 1).toByte()
      checkThat(download.bytes().contentEquals(server.reply.body()),"Defensive file bytes "+id)
     }
     else {
      checkThat(result.data()!!.javaClass.name=="com.webmania.sdk."+op.path("package").asText()+"."+op.path("response").asText(),"Typed response "+id)
      checkThat((result.data() as JsonValue).toJsonBytes().contentEquals(Json.bytes(fixture.path("response"))),"Exact response "+id)
     }
     server.reply=SocketFixture.Reply.json("{\"error\":false}")
     checkThat(error{call(c,id,request(op,fixture.path("input")))}.kind()==ApiException.Kind.API,"Error property presence");transfers++
     val invalid=fixture.path("input").deepCopy<ObjectNode>();invalid.put("_unexpected_wrapper_field",true);val n=server.requests.size;val sockets=server.connections.get()
     checkThat(error{call(c,id,request(op,invalid))}.kind()==ApiException.Kind.VALIDATION,"Wrapper validation")
     checkThat(server.requests.size==n&&server.connections.get()==sockets,"Validation zero socket I/O")
    }
   }
  }
 }
}
private fun values() {
 val model=com.webmania.sdk.kotlin.models.api1fiscal.nfeCancelRequest{setUuid(com.webmania.sdk.kotlin.models.api1fiscal.Uuid.ofVariant0("00000000-0000-4000-8000-000000000001"));setMotivo("Synthetic Kotlin cancellation control")}
 model.extension("extra",jsonValue("{\"zero\":0,\"false\":false,\"exact\":9007199254740999.990001,\"list\":[null,1]}"))
 checkThat(!model.toString().contains("Synthetic"),"DTO inspection protected")
 checkThat(model.toJsonBytes().decodeToString().contains("9007199254740999.990001"),"Decimal exact")
 model.explicitNull("motivo");checkThat(model.has("motivo")&&model.isNull("motivo"),"Explicit null")
 model.omit("motivo");checkThat(!model.has("motivo"),"Omission")
 checkThat(!company().toString().contains("kotlin-secret"),"Credentials protected")
 for(c in data("schema-cases.json")) {
  val actual=Contract.accepts(c.path("module").asText(),c.path("schema").asText(),JsonValue(c.get("instance")))
  checkThat(actual==c.path("valid").asBoolean(),"Schema case "+c.path("id").asText());schemaCases++
 }
 checkThat(schemaCases==418,"Frozen schema corpus actually validated")
 val amount=java.math.BigDecimal("9007199254740999.990001")
 val union=com.webmania.sdk.kotlin.models.api1fiscal.NfePaymentEventItemValorPagamento.ofVariant1(amount)
 val item=com.webmania.sdk.kotlin.models.api1fiscal.nfePaymentEventItem { setValorPagamento(union);setIndicadorPagamento(BigInteger.ZERO) }
 item.extension("False",jsonValue("false"));item.extension("false",jsonValue("0"));item.extension("nil",jsonValue("null"))
 val copy=com.webmania.sdk.kotlin.models.api1fiscal.NfePaymentEventItem(Json.parse(item.toJsonBytes()))
 checkThat(copy.getValorPagamento().asVariant1()==amount,"Typed decimal union roundtrip")
 checkThat(Json.parse(com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(copy.toJson())).path("valor_pagamento").decimalValue()==amount,"Explicit JsonValue tree Jackson interoperability")
 checkThat(copy.getIndicadorPagamento()==BigInteger.ZERO,"Typed zero roundtrip")
 checkThat(copy.extension("False").toJson().isBoolean&&!copy.extension("False").toJson().asBoolean(),"False is preserved")
 checkThat(copy.extension("false").toJson().isNumber&&copy.extension("nil").toJson().isNull,"Case-sensitive extensions and null")
 val stringUnion=com.webmania.sdk.kotlin.models.api1fiscal.NfePaymentEventItemValorPagamento.ofVariant0("0.00")
 checkThat(stringUnion.asVariant0()=="0.00"&&stringUnion.toJson().isTextual,"String union branch")
 val snapshot=copy.toJson() as ObjectNode;snapshot.put("indicador_pagamento",99)
 checkThat(copy.getIndicadorPagamento()==BigInteger.ZERO,"Defensive JSON read")
 checkThat(runCatching{copy.extension("valor_pagamento",jsonValue("0"))}.isFailure,"Declared extension collision")
 checkThat(!copy.toString().contains("9007199254740999"),"DTO string inspection protected")
}
private fun lifecycle() {
 for(tls in listOf(false,true))for(id in listOf("nfeIssueDocument","nfeGetSefazStatus")) {
  val op=Contract.operation(id);val input=fixtures.getValue(id).path("input").deepCopy<ObjectNode>()
  if(id=="nfeIssueDocument")(input.path("body") as ObjectNode).put("ID","K-INT-001")
  SocketFixture(tls).use { server ->
   val received=CountDownLatch(1)
   server.responder=java.util.function.Function { received.countDown();SocketFixture.Reply(200,"application/json","{}".toByteArray(),emptyMap(),0,2000,null,false) }
   client(op,"default",server.config(op.path("module").asText())).use { c ->
    val failure=AtomicReference<Throwable>();val interrupted=AtomicBoolean()
    val thread=Thread { try { call(c,id,request(op,input)) }catch(e:Throwable){failure.set(e)}finally{interrupted.set(Thread.currentThread().isInterrupted)} }
    thread.start();checkThat(received.await(3,TimeUnit.SECONDS),"Interrupt request reached fixture "+id+" tls="+tls+" result="+failure.get())
    // Let the client observe the complete 200 headers (the body is delayed) before interrupting: the recovery
    // selector is only retained once the response headers were fully observed.
    Thread.sleep(250);thread.interrupt();thread.join(1500)
    checkThat(!thread.isAlive,"Native thread interruption bounded")
    val e=failure.get() as ApiException
    checkThat(e.kind()==ApiException.Kind.CANCELLED&&e.operation()==id,"Interrupt retains call context")
    checkThat(interrupted.get(),"Native interrupt flag retained")
    checkThat(e.outcomeUncertain()==(id=="nfeIssueDocument"),"Mutation uncertainty after send")
    if(id=="nfeIssueDocument")checkThat(e.reference().value()=="K-INT-001","Explicit reconciliation selector retained")
    Thread.sleep(75);checkThat(server.requests.size==1,"Interrupted call never replayed")
   }
  }
  SocketFixture(tls).use { server ->
   server.reply=SocketFixture.Reply(200,"application/json","{}".toByteArray(),emptyMap(),0,2000,null,false)
   val config=clientConfig { origin(op.path("module").asText(),server.origin());deadline(Duration.ofMillis(400));if(tls)sslContext(SocketFixture.trust) }
   client(op,"default",config).use { c ->
    val start=System.nanoTime();val e=error { call(c,id,request(op,input)) }
    checkThat(e.kind()==ApiException.Kind.TIMEOUT,"Total deadline kind")
    checkThat(System.nanoTime()-start<TimeUnit.SECONDS.toNanos(2),"Total deadline bounded")
    checkThat(e.outcomeUncertain()==(id=="nfeIssueDocument"),"Deadline mutation uncertainty")
    Thread.sleep(75);checkThat(server.requests.size==1,"Deadline no replay")
   }
  }
  SocketFixture(tls).use { server ->
   val received=CountDownLatch(1);server.responder=java.util.function.Function { received.countDown();SocketFixture.Reply(200,"application/json","{}".toByteArray(),emptyMap(),0,2000,null,false) }
   val c=client(op,"default",server.config(op.path("module").asText()));val failure=AtomicReference<Throwable>()
   val thread=Thread { try{call(c,id,request(op,input))}catch(e:Throwable){failure.set(e)} };thread.start()
   checkThat(received.await(3,TimeUnit.SECONDS),"Close request reached fixture");c.close();thread.join(1500)
   checkThat(!thread.isAlive,"Close releases blocking caller")
   checkThat((failure.get() as ApiException).kind()==ApiException.Kind.CLOSED,"Close kind")
   val count=server.requests.size;val sockets=server.connections.get()
   checkThat(error{call(c,id,request(op,input))}.kind()==ApiException.Kind.CLOSED,"Closed client refuses calls")
   checkThat(server.requests.size==count&&server.connections.get()==sockets,"Closed client zero additional socket I/O");c.close()
  }
 }
}
private fun queryPage(target:String):Int=target.substringAfter('?').split('&').single{it.startsWith("page=")}.substringAfter('=').toInt()
private fun page(id:String,current:Int,last:Int):SocketFixture.Reply {
 val node=Json.parse("{\"data\":[],\"meta\":{\"current_page\":$current,\"last_page\":$last,\"per_page\":10,\"total\":12}}") as ObjectNode
 val items=node.path("data") as com.fasterxml.jackson.databind.node.ArrayNode
 for(index in 0 until if(current==1)10 else 2) {
  val uuid="00000000-0000-4000-8000-"+(current*10+index).toString().padStart(12,'0')
  val item=Json.parse("{\"uuid\":\"$uuid\"}") as ObjectNode
  if(id=="monitorListCompanyMunicipalities")item.put("tipo_download",1).put("municipio","Synthetic").put("uf","SP").put("codigo_ibge","3550308")
  if(id=="monitorListReports")item.put("relatorio","xml").put("modelo","nfe").put("status","concluido")
  items.add(item)
 }
 return SocketFixture.Reply.json(node.toString())
}
private fun collect(c:MonitorClient,id:String,input:JsonNode,options:PaginationOptions):PageCollection<*> = when(id) {
 "monitorListCompanies"->c.collectCompanies(com.webmania.sdk.kotlin.models.api2monitorreads.MonitorListCompaniesRequest(input),options)
 "monitorListCompanyMunicipalities"->c.collectCompanyMunicipalities(com.webmania.sdk.kotlin.models.api2monitorreads.MonitorListCompanyMunicipalitiesRequest(input),options)
 else->c.collectReports(com.webmania.sdk.kotlin.models.api2monitordocuments.MonitorListReportsRequest(input),options)
}
private fun importState(state:String)=SocketFixture.Reply.json("{\"uuid\":\"00000000-0000-4000-8000-000000000001\",\"status\":\"$state\",\"processado\":0,\"importados\":[],\"descartados\":[]}")
private fun helpers() {
 for(tls in listOf(false,true)) {
  for(id in listOf("monitorListCompanies","monitorListCompanyMunicipalities","monitorListReports"))SocketFixture(tls).use { server ->
   val op=Contract.operation(id);val input=fixtures.getValue(id).path("input").deepCopy<ObjectNode>().put("page",1).put("per_page",10)
   server.responder=java.util.function.Function{page(id,queryPage(it.target()),2)}
   MonitorClient(MonitorCredentials.create("kotlin-monitor"),server.config(op.path("module").asText())).use { c ->
    val result=collect(c,id,input,PaginationOptions(3,20,Duration.ofSeconds(3)))
    checkThat(result.values.size==12&&result.pagesFetched()==2&&result.stop()==PageCollection.Stop.COMPLETE,"Typed paginator completes "+id)
    checkThat(server.requests.size==2&&server.requests.map{queryPage(it.target())}==listOf(1,2),"Paginator exact pages")
    checkThat(server.requests.all{it.target().contains("per_page=10")&&it.headers()["authorization"]=="Bearer kotlin-monitor"},"Page size and auth retained")
    val limited=collect(c,id,input,PaginationOptions(3,11,Duration.ofSeconds(3)))
    checkThat(limited.values.size==11&&limited.stop()==PageCollection.Stop.ITEM_LIMIT&&limited.nextPage()==BigInteger.TWO&&limited.nextIndex()==1,"Paginator exact item continuation")
    val bounded=collect(c,id,input,PaginationOptions(1,20,Duration.ofSeconds(3)))
    checkThat(bounded.stop()==PageCollection.Stop.PAGE_LIMIT&&bounded.nextPage()==BigInteger.TWO&&bounded.values.size==10,"Paginator page bound")
   }
  }
  SocketFixture(tls).use { server ->
   val count=AtomicInteger();server.responder=java.util.function.Function{importState(if(count.incrementAndGet()==1)"processando" else "concluido")}
   MonitorClient(MonitorCredentials.create("kotlin-monitor"),server.config("api2/monitor-documents.openapi.json")).use { c ->
    val req=com.webmania.sdk.kotlin.models.api2monitordocuments.monitorGetImportStatusRequest{setUuid("00000000-0000-4000-8000-000000000001")}
    val done=c.waitImport(req,PollingOptions(3,Duration.ofSeconds(12),Duration.ofSeconds(5)))
    checkThat(done.completed()&&done.attempts()==2,"Import polling completes")
    checkThat(server.requests[0].target()==server.requests[1].target(),"Polling exact frozen selector")
    checkThat(server.receivedNanos[1]-server.receivedNanos[0]>=TimeUnit.SECONDS.toNanos(5),"Polling five second minimum")
    server.responder=null;server.reply=importState("processando")
    checkThat(c.waitImport(req,PollingOptions(1,Duration.ofSeconds(2),Duration.ofSeconds(5))).stop()==WaitResult.Stop.ATTEMPT_LIMIT,"Wait attempt bound")
    checkThat(c.waitImport(req,PollingOptions(3,Duration.ofMillis(150),Duration.ofSeconds(5))).stop()==WaitResult.Stop.DEADLINE,"Wait deadline between exchanges")
    server.reply=importState("future-state")
    checkThat(c.waitImport(req,PollingOptions(3,Duration.ofSeconds(2),Duration.ofSeconds(5))).stop()==WaitResult.Stop.UNKNOWN_STATE,"Wait unknown state")
   }
  }
  SocketFixture(tls).use { server ->
   server.reply=SocketFixture.Reply.json("{\"uuid\":\"00000000-0000-4000-8000-000000000001\",\"status\":\"concluido\",\"url\":\"https://example.invalid/report?token=kotlin-report-capability\"}")
   NfeClient(company(),server.config("api1/fiscal.openapi.json")).use { c ->
    val req=com.webmania.sdk.kotlin.models.api1fiscal.fiscalCreateOrPollReportRequest { setBody(com.webmania.sdk.kotlin.models.api1fiscal.fiscalReportRequest{setModelo("nfe");setRelatorio("danfe");setNumeroInicio("1");setNumeroFinal("5")}) }
    val done=c.waitReport(req,PollingOptions(2,Duration.ofSeconds(3),Duration.ofSeconds(5)))
    checkThat(done.completed()&&done.attempts()==1&&server.requests.size==1,"Report waiter typed completion")
    checkThat(!done.toString().contains("kotlin-report-capability")&&!com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(done).contains("kotlin-report-capability"),"Report wait inspection protected")
   }
  }
 }
}
private fun capabilities() {
 for(tls in listOf(false,true))SocketFixture(tls).use { server -> SocketFixture(tls).use { target ->
  val destination=target.origin().toString()+"/synthetic-private?token=kotlin-redirect-capability"
  server.reply=SocketFixture.Reply(302,"text/html",byteArrayOf(),mapOf("Location" to destination),0,0,null,false)
  val op=Contract.operation("fileGetNfsePdf")
  EmitterFilesClient(EmitterFileAuth.companyToken("kotlin-files"),server.config(op.path("module").asText())).use { c ->
   val result=call(c,"fileGetNfsePdf",request(op,fixtures.getValue("fileGetNfsePdf").path("input"))) as ApiResult<*>
   val download=result.data() as Download
   checkThat(result.httpStatus==302&&download.isRedirect&&download.redirectTarget().value()==destination,"Explicit file redirect capability")
   checkThat(target.requests.isEmpty()&&target.connections.get()==0&&server.requests.size==1,"Redirect never followed")
   checkThat(download.fileKind==null&&!download.isEncrypted,"Redirect is distinct from inspected bytes")
   checkThat(!download.toString().contains("kotlin-redirect-capability")&&!com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result).contains("kotlin-redirect-capability"),"File capability inspection protected")
  }
  val privateOp=Contract.operation("fiscalDownloadReport");val before=server.requests.size;val sockets=server.connections.get()
  NfeFilesClient(NfeFileAuth.company(company()),server.config(privateOp.path("module").asText())).use { c ->
   checkThat(error{call(c,"fiscalDownloadReport",request(privateOp,fixtures.getValue("fiscalDownloadReport").path("input")))}.kind()==ApiException.Kind.AUTH_SCOPE,"Public capability endpoint rejects company credential mode")
   checkThat(server.requests.size==before&&server.connections.get()==sockets,"Auth mismatch zero socket I/O")
  }
 } }
}
fun main() { matrix(false);matrix(true);values();lifecycle();helpers();capabilities();fileRepresentations();serializationProbe();println("KOTLIN_RESULT "+Json.parse("{\"assertions\":"+assertions+",\"matrix_requests\":"+transfers+",\"schema_cases\":"+schemaCases+",\"file_controls\":"+fileControls+",\"operations\":"+Contract.operationIds().size+",\"enabled\":"+Contract.operationIds().count{Contract.operation(it).path("enabled").asBoolean()}+",\"tls_and_http\":true,\"scope\":\"author controlled bindings only, no liveAPI\"}")); }

class KotlinAuthorTest { @org.junit.jupiter.api.Test fun bindings() { main() } }

private fun serializationProbe() {
 val dto=com.webmania.sdk.kotlin.models.api2monitordocuments.monitorImportStatus { setUuid("KOTLIN-PRIVATE-DTO-SENTINEL");setStatus("concluido") }
 checkThat(com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(dto).contains("KOTLIN-PRIVATE-DTO-SENTINEL"),"Explicit simple DTO serialization exposes deliberate data")
 val union=com.webmania.sdk.kotlin.models.api1fiscal.nfePaymentEventItem { setValorPagamento(com.webmania.sdk.kotlin.models.api1fiscal.NfePaymentEventItemValorPagamento.ofVariant1(java.math.BigDecimal("0.01"))) }
 checkThat(runCatching{com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(union)}.isFailure,"Arbitrary Jackson union bean mapping remains unsupported")
 checkThat(Json.parse(union.toJsonBytes()).path("valor_pagamento").decimalValue()==java.math.BigDecimal("0.01"),"Explicit union wire conversion is supported")
}
private fun expectedFileKind(media:String):FileKind = when(media) {
 "application/pdf"->FileKind.PDF
 "application/zip"->FileKind.ZIP
 "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"->FileKind.XLSX
 "text/csv"->FileKind.CSV
 "text/html"->FileKind.HTML
 else->FileKind.XML
}
private data class FileCase(val name:String,val operation:String,val media:String,val bytes:ByteArray,val expected:FileKind?)
private fun resourceBytes(name:String)=checkNotNull(object {}.javaClass.getResourceAsStream("/"+name)).use{it.readAllBytes()}
private fun fileRepresentations() {
 val xlsx="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
 val workbook=FileFixtures.workbook();workbook.remove("xl/worksheets/sheet1.xml")
 val badRelationships=FileFixtures.workbook();badRelationships["_rels/.rels"]=badRelationships.getValue("_rels/.rels").decodeToString().replace("http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument","urn:unrelated/officeDocument").toByteArray()
 val cases=mutableListOf(
  FileCase("pdf-complete","nfeDownloadDanfe","application/pdf",FileFixtures.valid("application/pdf"),FileKind.PDF),
  FileCase("xml-fiscal","nfeDownloadXml","application/xml",FileFixtures.xml(),FileKind.XML),
  FileCase("xml-utf16","nfeDownloadXml","text/xml","<?xml version=\"1.0\" encoding=\"UTF-16\"?><nfeProc xmlns=\"http://www.portalfiscal.inf.br/nfe\"/>".toByteArray(StandardCharsets.UTF_16LE),FileKind.XML),
  FileCase("zip-complete","fiscalDownloadReport","application/zip",FileFixtures.zip(),FileKind.ZIP),
  FileCase("zip-empty","fiscalDownloadReport","application/zip",FileFixtures.zip(emptyMap(),0,false),FileKind.ZIP),
  FileCase("xlsx-workbook","fiscalDownloadReport",xlsx,FileFixtures.valid(xlsx),FileKind.XLSX),
  FileCase("csv-rows","fiscalDownloadReport","text/csv",FileFixtures.valid("text/csv"),FileKind.CSV),
  FileCase("html-declared","fileGetNfsePdf","text/html",FileFixtures.valid("text/html"),FileKind.HTML),
  FileCase("regression-old-pdf-prefix","nfeDownloadDanfe","application/pdf","%PDF-".toByteArray(),null),
  FileCase("regression-html-as-xml","nfeDownloadXml","application/xml","<html>not fiscal XML</html>".toByteArray(),null),
  FileCase("regression-old-pk-prefix","fileGetNfseXml","application/zip",byteArrayOf(80,75,3,4),null),
  FileCase("xml-dtd","nfeDownloadXml","application/xml","<!DOCTYPE NFe [<!ENTITY leak SYSTEM 'http://127.0.0.1:1/never'>]><NFe>&leak;</NFe>".toByteArray(),null),
  FileCase("xlsx-missing-sheet","fiscalDownloadReport",xlsx,FileFixtures.zip(workbook,0,false),null),
  FileCase("xlsx-unrelated-relationship","fiscalDownloadReport",xlsx,FileFixtures.zip(badRelationships,0,false),null),
  FileCase("csv-inconsistent-width","fiscalDownloadReport","text/csv","one,two\nthree\n".toByteArray(),null),
  FileCase("declared-mime-required","nfeDownloadXml","application/pdf",FileFixtures.valid("application/pdf"),null)
 )
 for(c in data("file-fixtures.json").path("fixtures")) {
  val name=c.path("name").asText();val bytes=resourceBytes("file-fixtures/"+name)
  val sha=java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt() and 255)}
  checkThat(sha==c.path("sha256").asText(),"Sealed synthetic fixture identity "+name)
  cases.add(FileCase(name,if(c.path("kind").asText()=="ENCRYPTED_PDF")"nfeDownloadDanfe" else "fileGetNfseXml",c.path("media").asText(),bytes,if(c.path("supported").asBoolean(true))FileKind.valueOf(c.path("kind").asText()) else null))
 }
 val receipts=com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode()
 val kinds=mutableSetOf<FileKind>()
 for(tls in listOf(false,true))SocketFixture(tls).use { server ->
  for(case in cases) {
   val op=Contract.operation(case.operation)
   for(mode in if(case.expected in listOf(FileKind.ENCRYPTED_PDF,FileKind.ENCRYPTED_ZIP))listOf("default","token") else listOf("default")) {
    server.reply=SocketFixture.Reply.bytes(case.media,case.bytes)
    client(op,mode,server.config(op.path("module").asText())).use { c ->
     val before=server.requests.size;val sockets=server.connections.get()
     val input=request(op,fixtures.getValue(case.operation).path("input"))
     if(case.expected==null)checkThat(error{call(c,case.operation,input)}.kind()==ApiException.Kind.PROTOCOL,"Reject malformed file "+case.name)
     else {
      val result=call(c,case.operation,input) as ApiResult<*>;val download=result.value as Download;kinds.add(case.expected)
      checkThat(result.httpStatus==200&&download.fileKind==case.expected,"Inspected file kind "+case.name)
      checkThat(download.isEncrypted==(case.expected in listOf(FileKind.ENCRYPTED_PDF,FileKind.ENCRYPTED_ZIP)),"Encryption classification "+case.name)
      val protection=when {mode=="token"->FileProtection.DOCUMENT_TOKEN;op.path("security").isEmpty->FileProtection.PUBLIC_CAPABILITY;else->FileProtection.AUTHENTICATED}
      checkThat(download.fileProtection==protection,"Authorization does not imply decryption "+case.name)
      checkThat(download.contentType()==case.media&&download.bytes().contentEquals(case.bytes),"Exact representation "+case.name)
      val copy=download.bytes();if(copy.isNotEmpty())copy[0]=(copy[0].toInt() xor 1).toByte()
      checkThat(download.bytes().contentEquals(case.bytes),"File bytes defensive copy "+case.name)
      checkThat(!download.pending()&&!download.isRedirect,"Inspected bytes distinct from pending or redirect "+case.name)
     }
     checkThat(server.requests.size==before+1&&server.connections.get()==sockets+1,"One exchange; no file retry "+case.name)
     receipts.addObject().put("name",case.name).put("tls",tls).put("mode",mode).put("expected",case.expected?.name?:"PROTOCOL").put("passed",true)
     fileControls++
    }
   }
  }
  val op=Contract.operation("fileGetNfsePdf");server.reply=SocketFixture.Reply.json("{\"msg\":\"PDF is being retrieved\"}")
  client(op,"default",server.config(op.path("module").asText())).use { c ->
   val before=server.requests.size
   val result=call(c,"fileGetNfsePdf",request(op,fixtures.getValue("fileGetNfsePdf").path("input"))) as ApiResult<*>
   val download=result.value as Download
   checkThat(download.pending()&&!download.isRedirect&&download.fileKind==null&&!download.isEncrypted&&download.bytes().isEmpty(),"Pending JSON is not file bytes")
   checkThat(download.pendingStatus().toJson().path("msg").asText()=="PDF is being retrieved","Explicit pending payload")
   Thread.sleep(75);checkThat(server.requests.size==before+1,"Pending response never secretly polled")
   fileControls++
  }
 }
 checkThat(kinds==FileKind.values().toSet(),"All eight representation kinds exercised")
 println("KOTLIN_FILE_CONTROLS "+receipts)
}
