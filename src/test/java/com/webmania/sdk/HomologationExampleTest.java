package com.webmania.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Compiles examples/homologacao/Homologacao.kt with the Kotlin compiler of the local Maven repository and runs it in
 * a child JVM against a loopback TLS fixture, offline, to pin its four guards: the host allow-list fails closed before credentials and requests, only the
 * decoded top-level JSON integer 2 is accepted as ambiente, an unknown cancellation is ledgered as unknown and
 * never resent, and the consult after a cancellation succeeds only with status cancelado. It also pins the cleanup
 * of a document approved after the issue call and that a refused document is never cancelled.
 */
class HomologationExampleTest {
 private static final ObjectMapper JSON=new ObjectMapper();
 private static final String PASSWORD="test-fixture-only";
 private static final String NFE="00000000-0000-4000-8000-0000000000a1",NFSE="00000000-0000-4000-8000-0000000000b2";
 private static final Map<String,String> CREDENTIALS=Map.of("WEBMANIA_CONSUMER_KEY","example-key-value","WEBMANIA_CONSUMER_SECRET","example-secret-value",
  "WEBMANIA_ACCESS_TOKEN","example-access-value","WEBMANIA_ACCESS_TOKEN_SECRET","example-access-secret-value","WEBMANIA_ISSUER_TOKEN","example-issuer-value");
 @TempDir static Path dir;
 private static Path classes,keystore;
 private static String classpath;
 private static Map<String,JsonNode> fixtures;
 private static SSLContext serverTls;

 record Run(int exit,List<String> lines,String output) { }

 @BeforeAll static void compileExample()throws Exception {
  classpath=System.getProperty("surefire.test.class.path",System.getProperty("java.class.path"));
  classes=Files.createDirectories(dir.resolve("classes"));
  Path source=Path.of(System.getProperty("basedir",".")).resolve("examples/homologacao/Homologacao.kt");
  // The compiler is the one the build itself resolved: same version as the Kotlin standard library on the test classpath.
  Path stdlib=Path.of(kotlin.KotlinVersion.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  Path repository=stdlib.getParent().getParent().getParent().getParent().getParent().getParent();
  String compiler=String.join(File.pathSeparator,closure(repository,"org.jetbrains.kotlin","kotlin-compiler-embeddable",stdlib.getParent().getFileName().toString()));
  var build=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-cp",compiler,"org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
   "-no-stdlib","-no-reflect","-jvm-target","17","-Xjdk-release=17","-Werror","-classpath",classpath,"-d",classes.toString(),source.toString()).redirectErrorStream(true);
  build.environment().clear();
  Process kotlinc=build.start();kotlinc.getOutputStream().close();
  String diagnostics=new String(kotlinc.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
  assertTrue(kotlinc.waitFor(300,TimeUnit.SECONDS),"Kotlin compiler did not finish");
  assertEquals(0,kotlinc.exitValue(),diagnostics);
  keystore=dir.resolve("fixture.p12");
  String tool=Path.of(System.getProperty("java.home"),"bin","keytool").toString();
  var process=new ProcessBuilder(tool,"-genkeypair","-alias","fixture","-keyalg","RSA","-keysize","2048","-dname","CN=localhost","-ext","SAN=dns:localhost","-validity","2","-storetype","PKCS12","-keystore",keystore.toString(),"-storepass",PASSWORD,"-keypass",PASSWORD).redirectErrorStream(true).start();
  process.getInputStream().readAllBytes();assertEquals(0,process.waitFor(),"TLS fixture setup failed");
  KeyStore keys=KeyStore.getInstance("PKCS12");try(var in=Files.newInputStream(keystore)){keys.load(in,PASSWORD.toCharArray());}
  var km=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());km.init(keys,PASSWORD.toCharArray());
  serverTls=SSLContext.getInstance("TLS");serverTls.init(km.getKeyManagers(),null,null);
  fixtures=new HashMap<>();
  try(InputStream in=HomologationExampleTest.class.getResourceAsStream("/operation-fixtures.json")){for(JsonNode op:JSON.readTree(in))fixtures.put(op.path("id").asText(),op);}
 }

 @Test void guardsStopBeforeAnyCredentialUseOrRequest()throws Exception {
  try(var fixture=new Fixture(new Service("cancelado",200,List.of("cancelado"),200))) {
   Path folder=Files.createTempDirectory(dir,"guards");
   Map<String,String> payloads=Map.of("text","{\"ambiente\":\"2\"}","decimal","{\"ambiente\":2.0}","exponent","{\"ambiente\":2e0}","scaled","{\"ambiente\":20e-1}",
    "nested","{\"x\":{\"ambiente\":2}}","production","{\"ambiente\":1}","missing","{}","array","[{\"ambiente\":2}]");
   for(var p:payloads.entrySet())Files.writeString(folder.resolve(p.getKey()+".json"),p.getValue());
   Files.writeString(folder.resolve("valid.json"),nfePayload().toString());
   String ledger=folder.resolve("ledger.jsonl").toString();
   Map<String,String> live=with(fixture.origins(),"WEBMANIA_LIVE","1"),hosts=with(live,"WEBMANIA_LIVE_ALLOWED_HOSTS","localhost");
   Map<String,Map<String,String>> cases=new java.util.LinkedHashMap<>();
   Map<String,String> expected=new HashMap<>();
   cases.put("opt_in_missing",merge(fixture.origins(),CREDENTIALS,Map.of("WEBMANIA_LIVE_ALLOWED_HOSTS","localhost")));expected.put("opt_in_missing",null);
   cases.put("allowed_hosts_absent",live);
   cases.put("allowed_hosts_absent_with_credentials",merge(live,CREDENTIALS));
   cases.put("allowed_hosts_blank",merge(with(live,"WEBMANIA_LIVE_ALLOWED_HOSTS"," , "),CREDENTIALS));
   for(String name:List.of("allowed_hosts_absent","allowed_hosts_absent_with_credentials","allowed_hosts_blank"))expected.put(name,"allowed_hosts");
   cases.put("host_not_allowed",merge(with(live,"WEBMANIA_LIVE_ALLOWED_HOSTS","127.0.0.1"),CREDENTIALS));expected.put("host_not_allowed","host_not_allowed");
   for(String name:payloads.keySet()) {
    cases.put("ambiente_"+name,merge(hosts,CREDENTIALS,Map.of("WEBMANIA_LIVE_SCOPE","nfe","WEBMANIA_LIVE_NFE_PAYLOAD",folder.resolve(name+".json").toString(),"WEBMANIA_LIVE_LEDGER",ledger)));
    expected.put("ambiente_"+name,"ambiente");
   }
   cases.put("nfse_ambiente_nested",merge(hosts,CREDENTIALS,Map.of("WEBMANIA_LIVE_SCOPE","nfse","WEBMANIA_LIVE_NFSE_PAYLOAD",folder.resolve("nested.json").toString(),"WEBMANIA_LIVE_LEDGER",ledger)));
   expected.put("nfse_ambiente_nested","ambiente");
   // The JSON integer 2 passes the payload guard and stops at the next one.
   cases.put("integer_then_ledger_required",merge(hosts,CREDENTIALS,Map.of("WEBMANIA_LIVE_SCOPE","nfe","WEBMANIA_LIVE_NFE_PAYLOAD",folder.resolve("valid.json").toString())));
   expected.put("integer_then_ledger_required","ledger");
   cases.put("ledger_then_credentials_required",merge(hosts,Map.of("WEBMANIA_LIVE_SCOPE","nfe","WEBMANIA_LIVE_NFE_PAYLOAD",folder.resolve("valid.json").toString(),"WEBMANIA_LIVE_LEDGER",ledger)));
   expected.put("ledger_then_credentials_required","credentials");
   for(var c:cases.entrySet()) {
    Run run=run(c.getValue(),60);
    String reason=expected.get(c.getKey());
    String line=reason==null?"{\"result\":\"blocked\"}":"{\"result\":\"blocked\",\"reason\":\""+reason+"\"}";
    assertEquals(77,run.exit(),c.getKey()+": "+run.output());
    assertEquals(List.of(line),run.lines(),c.getKey());
   }
   assertEquals(List.of(),fixture.requests,"a guard let a request through");
   assertEquals("",Files.readString(Path.of(ledger)),"nothing is ledgered before a mutation");
  }
 }

 @Test void unknownCancellationIsLedgeredAsUnknownAndNeverResent()throws Exception {
  for(String model:List.of("nfe","nfse")) {
   boolean nfe=model.equals("nfe");
   try(var fixture=new Fixture(new Service("cancelado",nfe?503:200,List.of("cancelado"),nfe?200:503))) {
    Path folder=Files.createTempDirectory(dir,"unknown");Path ledger=folder.resolve("ledger.jsonl");
    Run run=run(scope(fixture,folder,model,ledger),90);
    assertEquals(1,run.exit(),run.output());
    String operation=nfe?"nfeCancelDocument":"emitterCancelNfse",cancel=nfe?"PUT /1/nfe/cancelar/":"PUT /2/nfse/cancelar";
    var entry=JSON.createObjectNode().put("model",model).put("identifier",nfe?NFE:NFSE).put("status","unknown").put("operation",operation);
    assertTrue(entries(ledger).contains(entry),"unknown cancellation is ledgered: "+entries(ledger));
    assertEquals(1,fixture.count(cancel),"the cancellation is sent exactly once");
    assertEquals(cancel,fixture.requests.get(fixture.requests.size()-1),"nothing is sent after an unknown outcome");
    JsonNode last=JSON.readTree(run.lines().get(run.lines().size()-1));
    assertEquals(operation,last.path("operation").asText());assertEquals("unknown",last.path("outcome").asText());
    assertFalse(run.output().contains(nfe?NFE:NFSE),"identifiers are never printed");
   }
  }
 }

 @Test void consultAfterCancellationSucceedsOnlyWithStatusCancelado()throws Exception {
  try(var fixture=new Fixture(new Service("cancelado",200,List.of("cancelado"),200))) {
   Path folder=Files.createTempDirectory(dir,"cancelado");
   Run run=run(scope(fixture,folder,"all",folder.resolve("ledger.jsonl")),90);
   assertEquals(0,run.exit(),run.output());
   for(String line:run.lines())assertTrue(JSON.readTree(line).path("ok").asBoolean(),line);
   assertEquals(1,fixture.count("GET /xmlnfe/"+NFE+"/cancelamento"));
  }
  try(var fixture=new Fixture(new Service("aprovado",200,List.of("aprovado"),200))) {
   Path folder=Files.createTempDirectory(dir,"aprovado");
   Run run=run(scope(fixture,folder,"all",folder.resolve("ledger.jsonl")),90);
   assertEquals(1,run.exit(),"a cancellation the consult does not show must fail the run: "+run.output());
   assertEquals(0,fixture.count("GET /xmlnfe/"+NFE+"/cancelamento"),"no cancellation XML without status cancelado");
   assertEquals(1,fixture.count("PUT /1/nfe/cancelar/"));assertEquals(1,fixture.count("PUT /2/nfse/cancelar"));
  }
  // Scope nfse alone, so a failing NF-e cycle cannot stand in for the NFS-e check; the positive NFS-e control
  // (processando_cancelamento, then cancelado: exit 0) is nfseCancellationIsConsultedWhileProcessandoCancelamento.
  try(var fixture=new Fixture(new Service("cancelado",200,List.of("aprovado"),200))) {
   Path folder=Files.createTempDirectory(dir,"nfse-aprovado");
   Run run=run(scope(fixture,folder,"nfse",folder.resolve("ledger.jsonl")),90);
   assertEquals(1,run.exit(),"an NFS-e cancellation the consult does not show must fail the run: "+run.output());
   assertEquals(1,fixture.count("PUT /2/nfse/cancelar"));assertEquals(0,fixture.count("PUT /1/nfe/cancelar/"));
   assertEquals(List.of("GET /2/nfse/consulta/"+NFSE),fixture.requests.subList(fixture.requests.indexOf("PUT /2/nfse/cancelar")+1,fixture.requests.size()));
  }
 }

 @Test void documentApprovedAfterTheIssueIsCancelledAndARefusedOneIsNot()throws Exception {
  // The issue call answers processando and the consult shows aprovado: the document is cleaned up like one approved at issue.
  try(var fixture=new Fixture(new Service("processando","aprovado","cancelado",200,List.of("cancelado"),200))) {
   Path folder=Files.createTempDirectory(dir,"approved-later");Path ledger=folder.resolve("ledger.jsonl");
   Run run=run(scope(fixture,folder,"all",ledger),90);
   assertEquals(0,run.exit(),run.output());
   assertEquals(1,fixture.count("PUT /1/nfe/cancelar/"));assertEquals(1,fixture.count("PUT /2/nfse/cancelar"));
   assertEquals(1,fixture.count("POST /1/nfe/emissao/"));assertEquals(1,fixture.count("POST /2/nfse/emissao"));
   assertEquals(1,fixture.count("GET /xmlnfe/"+NFE+"/cancelamento"));
   for(String model:List.of("nfe","nfse")) {
    String id=model.equals("nfe")?NFE:NFSE,consult=model.equals("nfe")?"nfeGetDocument":"emitterGetNfse";
    assertTrue(entries(ledger).contains(JSON.createObjectNode().put("model",model).put("identifier",id).put("status","aprovado").put("operation",consult)),entries(ledger).toString());
   }
  }
  // A document a consult shows refused is never cancelled.
  try(var fixture=new Fixture(new Service("processando","reprovado","cancelado",200,List.of("cancelado"),200))) {
   Path folder=Files.createTempDirectory(dir,"refused");
   Run run=run(scope(fixture,folder,"all",folder.resolve("ledger.jsonl")),90);
   assertEquals(1,run.exit(),run.output());
   assertEquals(0,fixture.count("PUT /1/nfe/cancelar/"));assertEquals(0,fixture.count("PUT /2/nfse/cancelar"));
   assertEquals(1,fixture.count("POST /1/nfe/emissao/"));assertEquals(1,fixture.count("POST /2/nfse/emissao"));
  }
 }

 @Test void nfseCancellationIsConsultedWhileProcessandoCancelamento()throws Exception {
  try(var fixture=new Fixture(new Service("cancelado",200,List.of("processando_cancelamento","cancelado"),200))) {
   Path folder=Files.createTempDirectory(dir,"processando");
   Run run=run(scope(fixture,folder,"nfse",folder.resolve("ledger.jsonl")),120);
   assertEquals(0,run.exit(),run.output());
   List<String> requests=fixture.requests;int cancel=requests.indexOf("PUT /2/nfse/cancelar");
   assertTrue(cancel>0,requests.toString());
   assertEquals(List.of("GET /2/nfse/consulta/"+NFSE,"GET /2/nfse/consulta/"+NFSE),requests.subList(cancel+1,requests.size()));
  }
 }

 private static Map<String,String> scope(Fixture fixture,Path folder,String scope,Path ledger)throws IOException {
  Path nfe=Files.writeString(folder.resolve("nfe.json"),nfePayload().toString()),nfse=Files.writeString(folder.resolve("nfse.json"),fixtures.get("emitterIssueNfse").path("input").path("body").toString());
  return merge(fixture.origins(),CREDENTIALS,Map.of("WEBMANIA_LIVE","1","WEBMANIA_LIVE_ALLOWED_HOSTS","localhost","WEBMANIA_LIVE_SCOPE",scope,
   "WEBMANIA_LIVE_NFE_PAYLOAD",nfe.toString(),"WEBMANIA_LIVE_NFSE_PAYLOAD",nfse.toString(),"WEBMANIA_LIVE_LEDGER",ledger.toString()));
 }
 private static ObjectNode nfePayload(){ObjectNode body=fixtures.get("nfeIssueDocument").path("input").path("body").deepCopy();body.put("ambiente",2);return body;}
 private static List<JsonNode> entries(Path ledger)throws IOException {
  List<JsonNode> out=new ArrayList<>();for(String line:Files.readAllLines(ledger))out.add(JSON.readTree(line));return out;
 }
 /** Runtime classpath of a Maven artifact from its POMs in the local repository (compile and runtime scopes). */
 private static List<String> closure(Path repository,String group,String artifact,String version)throws Exception {
  var jars=new java.util.LinkedHashMap<String,String>();var pending=new ArrayDeque<String[]>();pending.add(new String[]{group,artifact,version,"follow"});
  var factory=javax.xml.parsers.DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
  while(!pending.isEmpty()) {
   String[] c=pending.poll();if(jars.containsKey(c[0]+":"+c[1]))continue;
   Path base=repository.resolve(c[0].replace('.','/')).resolve(c[1]).resolve(c[2]);Path jar=base.resolve(c[1]+"-"+c[2]+".jar");
   assertTrue(Files.isRegularFile(jar),"missing from the local Maven repository: "+jar);jars.put(c[0]+":"+c[1],jar.toString());
   if(!c[3].equals("follow"))continue;
   org.w3c.dom.Element pom=factory.newDocumentBuilder().parse(base.resolve(c[1]+"-"+c[2]+".pom").toFile()).getDocumentElement();
   for(org.w3c.dom.Element dependencies:children(pom,"dependencies"))for(org.w3c.dom.Element d:children(dependencies,"dependency")) {
    String scope=text(d,"scope"),v=text(d,"version");
    if(scope!=null && !scope.equals("compile") && !scope.equals("runtime") || "true".equals(text(d,"optional")))continue;
    assertTrue(v!=null && !v.contains("$"),"unresolved version in "+base);
    boolean leaf=false;for(org.w3c.dom.Element exclusions:children(d,"exclusions"))for(org.w3c.dom.Element e:children(exclusions,"exclusion"))leaf|="*".equals(text(e,"groupId")) && "*".equals(text(e,"artifactId"));
    pending.add(new String[]{text(d,"groupId"),text(d,"artifactId"),v,leaf?"leaf":"follow"});
   }
  }
  return new ArrayList<>(jars.values());
 }
 private static List<org.w3c.dom.Element> children(org.w3c.dom.Element parent,String name) {
  List<org.w3c.dom.Element> out=new ArrayList<>();
  for(org.w3c.dom.Node n=parent.getFirstChild();n!=null;n=n.getNextSibling())if(n instanceof org.w3c.dom.Element e && e.getTagName().equals(name))out.add(e);
  return out;
 }
 private static String text(org.w3c.dom.Element parent,String name){List<org.w3c.dom.Element> found=children(parent,name);return found.isEmpty()?null:found.get(0).getTextContent().strip();}
 @SafeVarargs private static Map<String,String> merge(Map<String,String>... parts){Map<String,String> out=new HashMap<>();for(var part:parts)out.putAll(part);return out;}
 private static Map<String,String> with(Map<String,String> base,String key,String value){return merge(base,Map.of(key,value));}

 /** The example in a child JVM: an empty environment except the given WEBMANIA_* values, trusting only the fixture. */
 private static Run run(Map<String,String> environment,int seconds)throws Exception {
  var builder=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Djavax.net.ssl.trustStore="+keystore,"-Djavax.net.ssl.trustStorePassword="+PASSWORD,
   "-Djavax.net.ssl.trustStoreType=PKCS12","-cp",classes+File.pathSeparator+classpath,"HomologacaoKt");
  builder.environment().clear();builder.environment().putAll(environment);builder.directory(dir.toFile());
  Path output=Files.createTempFile(dir,"stdout",".txt"),errors=Files.createTempFile(dir,"stderr",".txt");
  builder.redirectOutput(output.toFile()).redirectError(errors.toFile());
  Process process=builder.start();process.getOutputStream().close();
  boolean finished=process.waitFor(seconds,TimeUnit.SECONDS);
  if(!finished){process.destroyForcibly().waitFor();}
  String out=Files.readString(output);
  if(!finished)throw new AssertionError("example did not finish: "+out);
  List<String> lines=out.lines().filter(l->!l.isBlank()).toList();
  return new Run(process.exitValue(),lines,out+Files.readString(errors));
 }

 /** API 1.0, files and API 2.0 routes of the example; unless told otherwise the documents are approved at issue. */
 private static final class Service implements java.util.function.Function<String,Reply> {
  private final String atIssue,beforeCancel,nfeAfterCancel;
  private final int nfeCancel,nfseCancel;
  private final Deque<String> nfseAfterCancel;
  private boolean nfeCancelled,nfseCancelled;
  Service(String nfeAfterCancel,int nfeCancel,List<String> nfseAfterCancel,int nfseCancel){this("aprovado","aprovado",nfeAfterCancel,nfeCancel,nfseAfterCancel,nfseCancel);}
  /** atIssue is the status of both issue responses, beforeCancel what both consults show until a cancellation. */
  Service(String atIssue,String beforeCancel,String nfeAfterCancel,int nfeCancel,List<String> nfseAfterCancel,int nfseCancel){this.atIssue=atIssue;this.beforeCancel=beforeCancel;this.nfeAfterCancel=nfeAfterCancel;this.nfeCancel=nfeCancel;this.nfseAfterCancel=new ArrayDeque<>(nfseAfterCancel);this.nfseCancel=nfseCancel;}
  @Override public synchronized Reply apply(String request) {
   String nfeStatus=nfeCancelled?nfeAfterCancel:beforeCancel;
   switch(request) {
    case "GET /1/nfe/sefaz/": return fixture("nfeGetSefazStatus");
    case "GET /1/nfe/certificado/": return fixture("nfeGetCertificateValidity");
    case "GET /1/nfe/classe-imposto/": return fixture("nfeListTaxClasses");
    case "GET /2/nfse/status": return fixture("emitterGetNfseProviderStatus");
    case "POST /1/nfe/emissao/": return json(JSON.createObjectNode().put("uuid",NFE).put("status",atIssue));
    case "GET /1/nfe/consulta/": case "POST /1/nfe/consulta/": return json(JSON.createObjectNode().put("uuid",NFE).put("status",nfeStatus));
    case "PUT /1/nfe/cancelar/": nfeCancelled=true;return nfeCancel==200?json(JSON.createObjectNode().put("status","cancelado")):new Reply(nfeCancel,"text/plain",FileFixtures.bytes("unavailable"));
    case "POST /2/nfse/emissao": return json(nfse("emitterIssueNfse",atIssue));
    case "GET /2/nfse/consulta/"+NFSE: return json(nfse("emitterGetNfse",!nfseCancelled?beforeCancel:nfseAfterCancel.size()>1?nfseAfterCancel.poll():nfseAfterCancel.peek()));
    case "PUT /2/nfse/cancelar": nfseCancelled=true;return nfseCancel==200?json(nfse("emitterCancelNfse","processando_cancelamento")):new Reply(nfseCancel,"text/plain",FileFixtures.bytes("unavailable"));
    case "GET /danfe/"+NFE+"/": case "GET /danfe/simples/"+NFE+"/": case "GET /nfse/"+NFSE: case "GET /darps/"+NFSE: return new Reply(200,"application/pdf",FileFixtures.valid("application/pdf"));
    case "GET /xmlnfe/"+NFE+"/": case "GET /xmlnfe/"+NFE+"/cancelamento": return new Reply(200,"application/xml",FileFixtures.valid("application/xml"));
    case "GET /xmlnfse/"+NFSE: return new Reply(200,"text/xml",FileFixtures.valid("text/xml"));
    default: return new Reply(404,"application/json",FileFixtures.bytes("{\"error\":\"unexpected request\"}"));
   }
  }
  private static Reply fixture(String id){return json(fixtures.get(id).path("response"));}
  private static ObjectNode nfse(String id,String status){ObjectNode node=fixtures.get(id).path("response").deepCopy();node.put("uuid",NFSE).put("status",status);return node;}
  private static Reply json(JsonNode node){return new Reply(200,"application/json",node.toString().getBytes(StandardCharsets.UTF_8));}
 }
 record Reply(int status,String media,byte[] body) { }

 /** One HTTPS exchange per connection on 127.0.0.1; every request line (method and path) is recorded in order. */
 private static final class Fixture implements AutoCloseable {
  final List<String> requests=new CopyOnWriteArrayList<>();
  private final java.util.function.Function<String,Reply> service;
  private final ServerSocket listener;
  private final ExecutorService executor=Executors.newCachedThreadPool(r->{var thread=new Thread(r,"homologation-fixture");thread.setDaemon(true);return thread;});
  Fixture(java.util.function.Function<String,Reply> service)throws IOException {
   this.service=service;
   listener=serverTls.getServerSocketFactory().createServerSocket(0,50,InetAddress.getByName("127.0.0.1"));
   executor.execute(()->{while(!listener.isClosed())try{Socket socket=listener.accept();executor.execute(()->serve(socket));}catch(IOException e){return;}});
  }
  Map<String,String> origins() {
   String origin="https://localhost:"+listener.getLocalPort();
   return Map.of("WEBMANIA_API1_BASE_URL",origin,"WEBMANIA_FILES_BASE_URL",origin,"WEBMANIA_API2_BASE_URL",origin);
  }
  int count(String request){return (int)requests.stream().filter(request::equals).count();}
  private void serve(Socket socket) {
   try(socket) {
    socket.setSoTimeout(10_000);InputStream in=socket.getInputStream();
    String[] first=line(in).split(" ");int length=0;String header;
    while(!(header=line(in)).isEmpty())if(header.toLowerCase(Locale.ROOT).startsWith("content-length:"))length=Integer.parseInt(header.substring(15).strip());
    if(in.readNBytes(length).length!=length)throw new IOException("incomplete request body");
    String request=first[0]+" "+first[1].split("\\?",2)[0];requests.add(request);
    Reply reply=service.apply(request);var out=socket.getOutputStream();
    out.write(("HTTP/1.1 "+reply.status()+" Fixture\r\nContent-Type: "+reply.media()+"\r\nContent-Length: "+reply.body().length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
    out.write(reply.body());out.flush();
   } catch(IOException e) { /* a closed or reset connection ends this exchange */ }
  }
  private static String line(InputStream in)throws IOException {
   var out=new ByteArrayOutputStream();int previous=-1;
   while(true){int value=in.read();if(value<0)throw new IOException("connection closed");if(previous==13 && value==10){byte[] data=out.toByteArray();return new String(data,0,data.length-1,StandardCharsets.US_ASCII);}out.write(value);if(out.size()>65536)throw new IOException("header too long");previous=value;}
  }
  @Override public void close()throws IOException {listener.close();executor.shutdownNow();}
 }
}
