package dev.ronova.coremod.observation;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public final class NbtStorageImageSelfTest {
    public static void main(String[] args) throws Exception {
        byte[] raw=image(false),baseline=gzip(raw);baseline[4]=42;baseline[9]=3;
        var positions=List.<List<NbtStoragePreservation.Step>>of(
            List.of(new NbtStoragePreservation.Key("target")),
            List.of(new NbtStoragePreservation.Key("rows"),new NbtStoragePreservation.Index(0)),
            List.of(new NbtStoragePreservation.Key("rows"),new NbtStoragePreservation.Index(2)));
        var result=NbtStorageImage.remove(baseline,positions,65536,4096);
        require(Arrays.equals(inflate(result.encoded()),image(true)),"unselected raw bytes or original list indices changed");
        require(result.encoded()[4]==42&&result.encoded()[9]==3,"gzip metadata lost");
        byte[] exposed=result.encoded();exposed[0]=0;
        require(result.encoded()[0]==31&&Arrays.equals(raw,inflate(baseline)),"candidate or baseline is mutable through returned bytes");
        reject(baseline,List.of(List.of(new NbtStoragePreservation.Key("missing"))),65536,4096);
        reject(baseline,List.of(positions.get(0),positions.get(0)),65536,4096);
        reject(baseline,List.of(List.of(new NbtStoragePreservation.Key("rows")),positions.get(1)),65536,4096);
        reject(baseline,positions,8,4096);reject(baseline,positions,65536,1);
        byte[] corrupt=baseline.clone();corrupt[corrupt.length-8]^=1;reject(corrupt,positions,65536,4096);
        byte[] trailing=Arrays.copyOf(baseline,baseline.length+1);reject(trailing,positions,65536,4096);
        var combined=new ByteArrayOutputStream();combined.write(baseline);combined.write(gzip(raw));reject(combined.toByteArray(),positions,65536,4096);
        reject(gzip(Arrays.copyOf(raw,raw.length-1)),positions,65536,4096);
        var duplicateBytes=new ByteArrayOutputStream();var duplicate=new DataOutputStream(duplicateBytes);
        duplicate.writeByte(10);duplicate.writeUTF("");
        for(int i=0;i<2;i++){duplicate.writeByte(1);duplicate.writeUTF("target");duplicate.writeByte(i);}
        duplicate.writeByte(0);
        reject(gzip(duplicateBytes.toByteArray()),List.of(positions.get(0)),65536,4096);
        var invalidBytes=new ByteArrayOutputStream();var invalid=new DataOutputStream(invalidBytes);
        invalid.writeByte(10);invalid.writeUTF("");invalid.writeByte(7);invalid.writeUTF("target");invalid.writeInt(-1);invalid.writeByte(0);
        reject(gzip(invalidBytes.toByteArray()),List.of(positions.get(0)),65536,4096);
        byte[] header=baseline.clone();header[3]=8;reject(header,positions,65536,4096);
        require(!result.baselineHash().equals(result.expectedHash()),"changed image reused its baseline hash");
        System.out.println("NbtStorageImageSelfTest PASS (raw-byte preservation, list indices, frozen image, bounded gzip/paths; no write authority)");
    }
    private static byte[] image(boolean removed) throws IOException {
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
        out.writeByte(10);out.writeUTF("root-name-preserved");
        out.writeByte(6);out.writeUTF("unknown_nan");out.writeLong(0x7ff8000000000001L);
        out.writeByte(6);out.writeUTF("unknown_negative_zero");out.writeLong(Long.MIN_VALUE);
        if(!removed){out.writeByte(10);out.writeUTF("target");item(out,99);}
        out.writeByte(9);out.writeUTF("rows");out.writeByte(10);out.writeInt(removed?1:3);
        if(!removed)item(out,0);item(out,1);if(!removed)item(out,2);
        out.writeByte(8);out.writeUTF("unknown_text");out.writeUTF("keep-all-fields");out.writeByte(0);return bytes.toByteArray();
    }
    private static void item(DataOutputStream out,int value) throws IOException {out.writeByte(3);out.writeUTF("value");out.writeInt(value);out.writeByte(0);}
    private static byte[] gzip(byte[] bytes) throws IOException {var out=new ByteArrayOutputStream();try(var gzip=new GZIPOutputStream(out)){gzip.write(bytes);}return out.toByteArray();}
    private static byte[] inflate(byte[] bytes) throws IOException {try(var input=new GZIPInputStream(new ByteArrayInputStream(bytes))){return input.readAllBytes();}}
    private static void reject(byte[] image,List<List<NbtStoragePreservation.Step>> paths,int bytes,int nodes) throws Exception {
        try{NbtStorageImage.remove(image,paths,bytes,nodes);throw new AssertionError("unsafe proposal accepted");}catch(IOException expected){}
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
