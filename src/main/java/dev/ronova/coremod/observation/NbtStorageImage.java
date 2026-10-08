package dev.ronova.coremod.observation;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Pure bounded candidate construction. Captured positions still need Root provenance and commit authorization. */
public final class NbtStorageImage {
    public static final int MAX_IMAGE=16*1024*1024;
    public static final class Candidate {
        private final byte[] encoded;
        private final String baselineHash,expectedHash;
        private Candidate(byte[] before,byte[] encoded){this.encoded=encoded;baselineHash=hash(before);expectedHash=hash(encoded);}
        public byte[] encoded(){return encoded.clone();}
        public String baselineHash(){return baselineHash;}
        public String expectedHash(){return expectedHash;}
    }
    private static final class Node {
        boolean remove;final Map<String,Node> keys=new HashMap<>();final Map<Integer,Node> indices=new HashMap<>();
        boolean children(){return !keys.isEmpty()||!indices.isEmpty();}
    }
    private static final Node EMPTY=new Node();
    private record Edit(int start,int end,byte[] replacement) {}
    private NbtStorageImage() {}
    /** Reject unsupported/ambiguous inputs; never return a partial candidate. Only a single plain gzip member is supported. */
    public static Candidate remove(byte[] baseline,List<List<NbtStoragePreservation.Step>> positions,int decodedLimit,int nodeLimit) throws IOException {
        if(baseline==null||baseline.length>MAX_IMAGE||decodedLimit<1||decodedLimit>MAX_IMAGE||nodeLimit<1||nodeLimit>65536
            ||positions==null||positions.isEmpty()||positions.size()>64)throw new IOException("unsupported image limits or positions");
        byte[] original=baseline.clone();Node root=new Node();
        for(var path:positions) {
            if(path==null||path.isEmpty()||path.size()>64)throw new IOException("invalid removal path");
            Node at=root;
            for(var step:path) {
                if(at.remove)throw new IOException("overlapping removal paths");
                if(step instanceof NbtStoragePreservation.Key key)at=at.keys.computeIfAbsent(key.value(),ignored->new Node());
                else if(step instanceof NbtStoragePreservation.Index index)at=at.indices.computeIfAbsent(index.value(),ignored->new Node());
                else throw new IOException("unknown path step");
            }
            if(at.remove||at.children())throw new IOException("duplicate or overlapping removal paths");at.remove=true;
        }
        byte[] raw=inflate(original,decodedLimit);var parser=new Parser(raw,nodeLimit);
        if(parser.u8()!=10)throw new IOException("SavedData root is not a compound");parser.utf();parser.payload(10,root,0);
        if(parser.at!=raw.length||parser.matched!=positions.size())throw new IOException("trailing data or missing removal position");
        parser.edits.sort(Comparator.comparingInt(Edit::start));
        var patched=new ByteArrayOutputStream(raw.length);int at=0;
        for(var edit:parser.edits) {
            if(edit.start<at)throw new IOException("overlapping byte edits");
            patched.write(raw,at,edit.start-at);patched.write(edit.replacement);at=edit.end;
        }
        patched.write(raw,at,raw.length-at);
        var check=new Parser(patched.toByteArray(),nodeLimit);
        if(check.u8()!=10)throw new IOException("invalid candidate root");check.utf();check.payload(10,EMPTY,0);
        if(check.at!=check.raw.length)throw new IOException("invalid candidate tail");
        var output=new LimitedOutput(MAX_IMAGE);
        try(var gzip=new GZIPOutputStream(output)){patched.writeTo(gzip);}
        byte[] encoded=output.bytes();System.arraycopy(original,0,encoded,0,10); // Preserve supported gzip header metadata.
        return new Candidate(original,encoded);
    }
    private static byte[] inflate(byte[] encoded,int limit) throws IOException {
        if(encoded.length<18||(encoded[0]&255)!=31||(encoded[1]&255)!=139||encoded[2]!=8||encoded[3]!=0)
            throw new IOException("unsupported gzip header");
        var inflater=new Inflater(true);var output=new LimitedOutput(limit);var crc=new CRC32();
        try {
            inflater.setInput(encoded,10,encoded.length-10);byte[] buffer=new byte[8192];
            while(!inflater.finished()) {
                int read=inflater.inflate(buffer);
                if(read==0&&!inflater.finished())throw new IOException("incomplete or dictionary-compressed image");
                output.write(buffer,0,read);crc.update(buffer,0,read);
            }
            if(inflater.getRemaining()!=8)throw new IOException("trailing gzip member or bytes");
            byte[] raw=output.bytes();int tail=encoded.length-8;
            if(little(encoded,tail)!=crc.getValue()||little(encoded,tail+4)!=raw.length)throw new IOException("gzip checksum or length mismatch");
            return raw;
        } catch(DataFormatException malformed){throw new IOException("malformed deflate image",malformed);}
        finally {inflater.end();}
    }
    private static long little(byte[] bytes,int at){return (bytes[at]&255L)|((bytes[at+1]&255L)<<8)|((bytes[at+2]&255L)<<16)|((bytes[at+3]&255L)<<24);}
    private static String hash(byte[] bytes) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    private static final class LimitedOutput extends OutputStream {
        final ByteArrayOutputStream output=new ByteArrayOutputStream();final int limit;
        LimitedOutput(int limit){this.limit=limit;}
        @Override public void write(int value) throws IOException {if(output.size()>=limit)throw new IOException("image limit");output.write(value);}
        @Override public void write(byte[] bytes,int start,int length) throws IOException {
            if(length>limit-output.size())throw new IOException("image limit");output.write(bytes,start,length);
        }
        byte[] bytes(){return output.toByteArray();}
    }
    private static final class Parser {
        final byte[] raw;int at,nodes,matched;final List<Edit> edits=new ArrayList<>();
        Parser(byte[] raw,int nodes){this.raw=raw;this.nodes=nodes;}
        void skip(long count) throws IOException {if(count<0||count>raw.length-at)throw new IOException("truncated NBT");at+=(int)count;}
        int u8() throws IOException {if(at>=raw.length)throw new IOException("truncated NBT");return raw[at++]&255;}
        int i32() throws IOException {return u8()<<24|u8()<<16|u8()<<8|u8();}
        String utf() throws IOException {
            int start=at;int length=u8()<<8|u8();skip(length);
            try(var input=new DataInputStream(new ByteArrayInputStream(raw,start,length+2))){return input.readUTF();}
        }
        void payload(int type,Node scope,int depth) throws IOException {
            if(--nodes<0||depth>64)throw new IOException("NBT traversal limit");
            switch(type) {
                case 1 -> skip(1);case 2 -> skip(2);case 3,5 -> skip(4);case 4,6 -> skip(8);
                case 7,11,12 -> {int count=i32();if(count<0)throw new IOException("negative array length");skip((long)count*(type==7?1:type==11?4:8));}
                case 8 -> utf();
                case 9 -> {
                    int element=u8(),lengthAt=at,count=i32(),removed=0;
                    if(count<0||element>12||element==0&&count!=0)throw new IOException("invalid list");
                    for(int index=0;index<count;index++) {
                        int start=at;Node child=scope.indices.getOrDefault(index,EMPTY);payload(element,child.remove?EMPTY:child,depth+1);
                        if(child.remove){edits.add(new Edit(start,at,new byte[0]));matched++;removed++;}
                    }
                    if(removed!=0) {int next=count-removed;edits.add(new Edit(lengthAt,lengthAt+4,new byte[]{(byte)(next>>>24),(byte)(next>>>16),(byte)(next>>>8),(byte)next}));}
                }
                case 10 -> {
                    var names=new HashSet<String>();
                    while(true) {
                        int start=at,childType=u8();if(childType==0)break;
                        String name=utf();if(!names.add(name))throw new IOException("duplicate compound key");
                        Node child=scope.keys.getOrDefault(name,EMPTY);payload(childType,child.remove?EMPTY:child,depth+1);
                        if(child.remove){edits.add(new Edit(start,at,new byte[0]));matched++;}
                    }
                }
                default -> throw new IOException("unknown NBT type");
            }
        }
    }
}
