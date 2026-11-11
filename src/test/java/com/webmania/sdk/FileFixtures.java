package com.webmania.sdk;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;

/** Actual minimal representations replacing the former invalid prefix-only positives. */
final class FileFixtures {
 private FileFixtures() {}
 static byte[] bytes(String value){return value.getBytes(StandardCharsets.UTF_8);}
 static byte[] pdf(String trailer,String content) {
  var out=new StringBuilder("%PDF-1.7\n");int one=out.length();out.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R ").append(content).append(" >>\nendobj\n");int two=out.length();out.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");int three=out.length();out.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >>\nendobj\n");int four=out.length();out.append("4 0 obj\n<< /Length 0 >>\nstream\n\nendstream\nendobj\n");int xref=out.length();out.append("xref\n0 5\n0000000000 65535 f \n").append(String.format(java.util.Locale.ROOT,"%010d 00000 n \n%010d 00000 n \n%010d 00000 n \n%010d 00000 n \n",one,two,three,four)).append("trailer\n<< /Size 5 /Root 1 0 R ").append(trailer).append(" >>\nstartxref\n").append(xref).append("\n%%EOF\n");return bytes(out.toString());
 }
 static byte[] xml(){return bytes("<?xml version=\"1.0\" encoding=\"UTF-8\"?><nfeProc xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\"><NFe><infNFe Id=\"NFe"+"1".repeat(44)+"\"><ide><mod>55</mod></ide></infNFe></NFe></nfeProc>");}
 static void number(ByteArrayOutputStream out,long value,int width){for(int i=0;i<width;i++)out.write((int)(value>>(8*i))&255);}
 static long number(byte[] b,int p,int width){long value=0;for(int i=0;i<width;i++)value|=(b[p+i]&255L)<<(8*i);return value;}
 static void set(byte[] b,int p,long value,int width){for(int i=0;i<width;i++)b[p+i]=(byte)(value>>(8*i));}
 static byte[] zip(Map<String,byte[]> files,int flags,boolean signed) {
  var out=new ByteArrayOutputStream();var central=new ByteArrayOutputStream();
  for(var file:files.entrySet()){byte[] name=bytes(file.getKey()),data=file.getValue();int offset=out.size();var check=new CRC32();check.update(data);long crc=check.getValue();boolean descriptor=(flags&8)!=0;
   number(out,0x04034b50,4);number(out,20,2);number(out,flags,2);number(out,0,2);number(out,0,4);number(out,descriptor?0:crc,4);number(out,descriptor?0:data.length,4);number(out,descriptor?0:data.length,4);number(out,name.length,2);number(out,0,2);out.writeBytes(name);out.writeBytes(data);
   if(descriptor){if(signed)number(out,0x08074b50,4);number(out,crc,4);number(out,data.length,4);number(out,data.length,4);}
   number(central,0x02014b50,4);number(central,20,2);number(central,20,2);number(central,flags,2);number(central,0,2);number(central,0,4);number(central,crc,4);number(central,data.length,4);number(central,data.length,4);number(central,name.length,2);number(central,0,2);number(central,0,2);number(central,0,2);number(central,0,2);number(central,0,4);number(central,offset,4);central.writeBytes(name);
  }
  int offset=out.size();out.writeBytes(central.toByteArray());number(out,0x06054b50,4);number(out,0,4);number(out,files.size(),2);number(out,files.size(),2);number(out,central.size(),4);number(out,offset,4);number(out,0,2);return out.toByteArray();
 }
 static byte[] zip(){return zip(Map.of("document.xml",xml()),0,false);}
 static byte[] zip64(byte[] original,boolean saturated) {
  int e=original.length-22;var out=new ByteArrayOutputStream();out.write(original,0,e);number(out,0x06064b50,4);number(out,44,8);number(out,45,2);number(out,45,2);number(out,0,8);number(out,number(original,e+8,2),8);number(out,number(original,e+10,2),8);number(out,number(original,e+12,4),8);number(out,number(original,e+16,4),8);number(out,0x07064b50,4);number(out,0,4);number(out,e,8);number(out,1,4);byte[] end=java.util.Arrays.copyOfRange(original,e,original.length);if(saturated){set(end,8,65535,2);set(end,10,65535,2);set(end,12,0xffff_ffffL,4);set(end,16,0xffff_ffffL,4);}out.writeBytes(end);return out.toByteArray();
 }
 static Map<String,byte[]> workbook() {
  var files=new LinkedHashMap<String,byte[]>();
  files.put("[Content_Types].xml",bytes("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>"));
  files.put("_rels/.rels",bytes("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"office\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>"));
  files.put("xl/workbook.xml",bytes("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Sheet1\" sheetId=\"1\" r:id=\"sheet1\"/></sheets></workbook>"));
  files.put("xl/_rels/workbook.xml.rels",bytes("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"sheet1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>"));
  files.put("xl/worksheets/sheet1.xml",bytes("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData/></worksheet>"));return files;
 }
 static byte[] valid(String media){return switch(media){case "application/pdf"->pdf("","");case "application/zip"->zip();case "text/html"->bytes("<!doctype html><html><body>Fiscal document</body></html>");case "text/csv"->bytes("id;valor\r\n1;10.00\r\n");case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"->zip(workbook(),0,false);default->xml();};}
}
