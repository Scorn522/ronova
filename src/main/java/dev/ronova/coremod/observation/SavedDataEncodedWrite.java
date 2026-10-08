package dev.ronova.coremod.observation;

import java.io.*;
import java.security.*;
import java.util.HexFormat;

/** Bounded digest of bytes accepted by the actual compressed output stream, not a commit receipt. */
public final class SavedDataEncodedWrite implements AutoCloseable {
    private static final ThreadLocal<SavedDataEncodedWrite> CURRENT=new ThreadLocal<>();
    private final SavedDataEncodedWrite previous;
    private final SavedDataSaveTrace trace;
    private final long generation;
    private Object image;
    private Object holder;
    private final Thread owner=Thread.currentThread();
    private final MessageDigest digest;
    private final NbtWriteReferences references;
    private boolean bound,closed,successful;
    private volatile boolean invalid;
    private long bytes;
    private String fileIdentity;
    private boolean fileIdentityConfirmed;
    private SavedDataEncodedWrite(SavedDataSaveTrace trace,Object image,Object holder) {
        this.trace=trace;this.image=image;this.holder=holder;references=new NbtWriteReferences(image);generation=trace.snapshot().generation();previous=CURRENT.get();
        if(previous!=null)previous.invalid=true;
        try {digest=MessageDigest.getInstance("SHA-256");} catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
        CURRENT.set(this);
    }
    public static SavedDataEncodedWrite open(SavedDataSaveTrace trace,Object image){return open(trace,image,null);}
    public static SavedDataEncodedWrite open(SavedDataSaveTrace trace,Object image,Object holder){return trace==null?null:new SavedDataEncodedWrite(trace,image,holder);}
    public static AutoCloseable enterReference(Object parent,net.minecraft.nbt.Tag child,String key) {
        var capture=CURRENT.get();if(capture==null||capture.invalid||capture.closed)return null;
        Class<?> type=child==null?null:child.getClass();
        boolean compound=type==net.minecraft.nbt.CompoundTag.class;
        boolean supported=compound||type==net.minecraft.nbt.ListTag.class||type==net.minecraft.nbt.ByteTag.class
            ||type==net.minecraft.nbt.ShortTag.class||type==net.minecraft.nbt.IntTag.class||type==net.minecraft.nbt.LongTag.class
            ||type==net.minecraft.nbt.FloatTag.class||type==net.minecraft.nbt.DoubleTag.class||type==net.minecraft.nbt.StringTag.class
            ||type==net.minecraft.nbt.ByteArrayTag.class||type==net.minecraft.nbt.IntArrayTag.class||type==net.minecraft.nbt.LongArrayTag.class;
        return capture.references.enter(parent,child,key,compound,supported);
    }
    public static void leaveReference(AutoCloseable scope) {
        if(scope!=null)try {scope.close();}catch(Exception unavailable){var capture=CURRENT.get();if(capture!=null)capture.invalidate();}
    }
    public static OutputStream observe(Object image,OutputStream stream) {
        var capture=CURRENT.get();
        if(capture==null)return stream;
        if(capture.image!=image||capture.bound||capture.closed){capture.invalid=true;return stream;}
        capture.bound=true;
        // Only the exact JDK stream is supported. Wrappers are not traversed by
        // guessed fields and a synthetic stream can never confer a file identity.
        final FileOutputStream file=stream.getClass()==FileOutputStream.class?(FileOutputStream)stream:null;
        if(file!=null)try {capture.fileIdentity=dev.ronova.coremod.nativebridge.NativeBridge.savedFileIdentity(file.getFD());}
        catch(IOException|RuntimeException|LinkageError unavailable){capture.fileIdentity=null;}
        return new FilterOutputStream(stream) {
            @Override public void write(int value)throws IOException {
                try {out.write(value);}catch(IOException|RuntimeException|Error failure){capture.invalidate();throw failure;}
                synchronized(capture){if(capture.accept(1))capture.digest.update((byte)value);}
            }
            @Override public void write(byte[] value,int offset,int length)throws IOException {
                try {out.write(value,offset,length);}catch(IOException|RuntimeException|Error failure){capture.invalidate();throw failure;}
                synchronized(capture){if(capture.accept(length))capture.digest.update(value,offset,length);}
            }
            @Override public void flush()throws IOException {
                try {out.flush();}catch(IOException|RuntimeException|Error failure){capture.invalidate();throw failure;}
            }
            @Override public void close()throws IOException {
                if(file!=null&&!capture.fileIdentityConfirmed)try {
                    String after=dev.ronova.coremod.nativebridge.NativeBridge.savedFileIdentity(file.getFD());
                    capture.fileIdentityConfirmed=capture.fileIdentity!=null&&capture.fileIdentity.equals(after);
                } catch(IOException|RuntimeException|LinkageError unavailable){capture.fileIdentityConfirmed=false;}
                try {out.close();}catch(IOException|RuntimeException|Error failure){capture.invalidate();throw failure;}
            }
        };
    }
    private synchronized void invalidate(){invalid=true;trace.invalidateEncoded(generation);}
    private boolean accept(int count){
        if(Thread.currentThread()!=owner||closed){invalidate();return false;}
        if(invalid)return false;
        if(count<0||bytes>16L*1024*1024-count){invalid=true;return false;}
        bytes+=count;return true;
    }
    public synchronized void succeeded(){if(Thread.currentThread()!=owner)invalidate();else successful=true;}
    @Override public synchronized void close(){
        if(Thread.currentThread()!=owner){invalidate();return;}
        if(closed)return;closed=true;
        if(CURRENT.get()!=this){invalid=true;releaseReferences();return;}
        var restore=previous;while(restore!=null&&restore.closed)restore=restore.previous;
        if(restore==null)CURRENT.remove();else CURRENT.set(restore);
        try {if(successful&&bound&&!invalid) {
            var encoded=trace.encoded(generation,HexFormat.of().formatHex(digest.digest()),bytes,references.snapshot(),
                fileIdentityConfirmed?fileIdentity:null);
            try {
                if(encoded!=null&&holder instanceof net.minecraft.world.level.saveddata.SavedData data)
                    dev.ronova.coremod.bootstrap.ServerRuntimeRegistry.savedDataEncoded(data,encoded);
            } catch(RuntimeException|LinkageError unavailable) {
                // Absence of a source confirmation must not suppress the original save.
            } finally {java.lang.ref.Reference.reachabilityFence(image);}
        }} finally {releaseReferences();}
    }
    private void releaseReferences(){references.release();image=null;holder=null;}
}
