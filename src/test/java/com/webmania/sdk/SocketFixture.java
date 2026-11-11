package com.webmania.sdk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/** Shared Java author fixture, extended locally with an accepted-connection counter. */
final class SocketFixture implements AutoCloseable {
 final java.util.concurrent.atomic.AtomicInteger connections=new java.util.concurrent.atomic.AtomicInteger();
 record Request(String method,String target,Map<String,String> headers,byte[] body) { }
 record Reply(int status,String media,byte[] body,Map<String,String> headers,long delayHeaders,long delayBody,Integer claimedLength,boolean chunked) {
  static Reply json(String text) { return new Reply(200,"application/json",text.getBytes(StandardCharsets.UTF_8),Map.of(),0,0,null,false); }
  static Reply bytes(String media,byte[] bytes) { return new Reply(200,media,bytes,Map.of(),0,0,null,false); }
 }
 static SSLContext trust;
 private static SSLContext serverTls;
 static synchronized void tls()throws Exception {
  if(serverTls!=null)return;
  var path=java.nio.file.Files.createTempDirectory("webmania-test-tls");var file=path.resolve("fixture.p12");
  try {
   String tool=java.nio.file.Path.of(System.getProperty("java.home"),"bin","keytool").toString();
   var process=new ProcessBuilder(tool,"-genkeypair","-alias","fixture","-keyalg","RSA","-keysize","2048","-dname","CN=localhost","-ext","SAN=dns:localhost","-validity","2","-storetype","PKCS12","-keystore",file.toString(),"-storepass","test-fixture-only","-keypass","test-fixture-only").redirectErrorStream(true).start();
   process.getInputStream().readAllBytes();if(process.waitFor()!=0)throw new IOException("TLS fixture setup failed");
   KeyStore keys=KeyStore.getInstance("PKCS12");try(var in=java.nio.file.Files.newInputStream(file)){keys.load(in,"test-fixture-only".toCharArray());}
   var km=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());km.init(keys,"test-fixture-only".toCharArray());
   var tm=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());tm.init(keys);
   serverTls=SSLContext.getInstance("TLS");serverTls.init(km.getKeyManagers(),null,null);
   trust=SSLContext.getInstance("TLS");trust.init(null,tm.getTrustManagers(),null);
  } finally {java.nio.file.Files.deleteIfExists(file);java.nio.file.Files.deleteIfExists(path);}
 }
 final CopyOnWriteArrayList<Long> receivedNanos=new CopyOnWriteArrayList<>();
 volatile java.util.function.Function<Request,Reply> responder;
 final CopyOnWriteArrayList<Request> requests=new CopyOnWriteArrayList<>();
 volatile boolean provisional;
 volatile boolean trailers;
 volatile Reply reply=Reply.json("{}");
 private final ServerSocket listener;
 private final java.util.concurrent.ExecutorService executor=Executors.newCachedThreadPool();
 private volatile boolean stopped;
 final boolean secure;
 SocketFixture(boolean secure)throws Exception {
  this.secure=secure;if(secure)tls();
  listener=secure?serverTls.getServerSocketFactory().createServerSocket(0,50,java.net.InetAddress.getByName("127.0.0.1")):new ServerSocket(0,50,java.net.InetAddress.getByName("127.0.0.1"));
  executor.execute(()->{while(!stopped){try{Socket s=listener.accept();connections.incrementAndGet();executor.execute(()->handle(s));}catch(IOException ignored){}}});
 }
 java.net.URI origin(){return java.net.URI.create((secure?"https":"http")+"://localhost:"+listener.getLocalPort());}
 ClientConfig config(String module){var b=ClientConfig.builder().origin(module,origin());if(secure)b.sslContext(trust);return b.build();}
 private void handle(Socket socket) {
  try(socket){socket.setSoTimeout(5000);var in=socket.getInputStream();String line=line(in);String[] first=line.split(" ");Map<String,String> headers=new java.util.HashMap<>();
   while(!(line=line(in)).isEmpty()){int i=line.indexOf(':');headers.put(line.substring(0,i).toLowerCase(java.util.Locale.ROOT),line.substring(i+1).strip());}
   int length=Integer.parseInt(headers.getOrDefault("content-length","0"));byte[] body=in.readNBytes(length);Request captured=new Request(first[0],first[1],Map.copyOf(headers),body);requests.add(captured);receivedNanos.add(System.nanoTime());
   Reply current=responder==null?reply:responder.apply(captured);Thread.sleep(current.delayHeaders());var out=socket.getOutputStream();if(provisional)out.write("HTTP/1.1 100 Continue\r\n\r\nHTTP/1.1 103 Early Hints\r\nLink: </fixture>\r\n\r\n".getBytes(StandardCharsets.US_ASCII));StringBuilder response=new StringBuilder("HTTP/1.1 "+current.status()+" Fixture\r\nContent-Type: "+current.media()+"\r\nConnection: close\r\n");
   for(var h:current.headers().entrySet())response.append(h.getKey()).append(": ").append(h.getValue()).append("\r\n");
   if(current.chunked())response.append("Transfer-Encoding: chunked\r\n");
   else if(current.claimedLength()==null || current.claimedLength()>=0) response.append("Content-Length: ").append(current.claimedLength()==null?current.body().length:current.claimedLength()).append("\r\n");
   out.write((response+"\r\n").getBytes(StandardCharsets.US_ASCII));out.flush();Thread.sleep(current.delayBody());
   if(current.chunked()){out.write((Integer.toHexString(current.body().length)+"\r\n").getBytes(StandardCharsets.US_ASCII));out.write(current.body());out.write((trailers?"\r\n0\r\nX-Fixture-Trailer: synthetic\r\n\r\n":"\r\n0\r\n\r\n").getBytes(StandardCharsets.US_ASCII));}
   else out.write(current.body());out.flush();
  }catch(IOException ignored){}catch(InterruptedException e){Thread.currentThread().interrupt();}
 }
 private static String line(java.io.InputStream in)throws IOException {var out=new ByteArrayOutputStream();int previous=-1;while(true){int b=in.read();if(b<0)throw new IOException();if(previous==13 && b==10){byte[] a=out.toByteArray();return new String(a,0,a.length-1,StandardCharsets.US_ASCII);}out.write(b);if(out.size()>65536)throw new IOException();previous=b;}}
 @Override public void close() {stopped=true;try{listener.close();}catch(IOException ignored){}executor.shutdownNow();}
}
