package dev.ronova.coremod.observation;

import java.io.*;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class SavedDataEncodedWriteSelfTest {
    public static void main(String[] args)throws Exception {
        var trace=new SavedDataSaveTrace();var image=new Object();var sink=new ByteArrayOutputStream();
        trace.begin();trace.serialized();trace.writing();
        try(var capture=SavedDataEncodedWrite.open(trace,image)) {
            var output=SavedDataEncodedWrite.observe(image,sink);output.write(7);output.write(new byte[]{1,2,3},1,2);output.close();
            capture.succeeded();trace.wrote();
        }
        trace.end(true);
        require(trace.encodedWrite()!=null&&trace.encodedWrite().bytes()==3&&trace.encodedWrite().hash().equals(
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(sink.toByteArray()))),"actual stream digest mismatch");
        require(trace.encodedWrite().fileIdentity()==null,"memory stream acquired a platform file identity");
        trace.begin();trace.serialized();trace.writing();
        var failure=new IOException("fixture");
        try(var capture=SavedDataEncodedWrite.open(trace,image)) {
            var output=SavedDataEncodedWrite.observe(image,new OutputStream(){public void write(int b)throws IOException{throw failure;}});
            try{output.write(1);throw new AssertionError("write failure lost");}catch(IOException expected){require(expected==failure,"exception identity changed");}
            trace.failed();
        }
        trace.end(true);require(trace.encodedWrite()==null,"failed write reused old digest");
        trace.begin();trace.serialized();trace.writing();
        try(var capture=SavedDataEncodedWrite.open(trace,image)) {
            SavedDataEncodedWrite.observe(new Object(),new ByteArrayOutputStream());capture.succeeded();trace.wrote();
        }
        trace.end(true);require(trace.encodedWrite()==null,"different image accepted");
        trace.begin();trace.serialized();trace.writing();
        try(var capture=SavedDataEncodedWrite.open(trace,image)) {
            var output=SavedDataEncodedWrite.observe(image,OutputStream.nullOutputStream());var chunk=new byte[1024];
            for(int i=0;i<16385;i++)output.write(chunk);capture.succeeded();trace.wrote();
        }
        trace.end(true);require(trace.encodedWrite()==null,"oversize digest published");
        var direct=new ByteArrayOutputStream();require(SavedDataEncodedWrite.observe(image,direct)==direct,"scope retained after close");
        trace.begin();trace.serialized();trace.writing();
        try(var capture=SavedDataEncodedWrite.open(trace,image)) {
            var output=SavedDataEncodedWrite.observe(image,new OutputStream(){public void write(int b)throws IOException{throw failure;}});
            try{output.write(1);}catch(IOException expected){require(expected==failure,"swallowed failure identity changed");}
            capture.succeeded();trace.wrote(); // Deliberately swallow the producer failure.
        }
        trace.end(true);require(trace.encodedWrite()==null,"swallowed partial write published a digest");
        trace.begin();trace.serialized();trace.writing();
        try(var capture=SavedDataEncodedWrite.open(trace,image)) {
            var output=SavedDataEncodedWrite.observe(image,OutputStream.nullOutputStream());
            var worker=new Thread(()->{try{output.write(3);}catch(IOException error){throw new AssertionError(error);}});
            worker.start();worker.join();capture.succeeded();trace.wrote();
        }
        trace.end(true);require(trace.encodedWrite()==null,"foreign worker produced trusted digest");
        trace.begin();trace.serialized();trace.writing();
        OutputStream retained;
        try(var capture=SavedDataEncodedWrite.open(trace,image)) {
            retained=SavedDataEncodedWrite.observe(image,OutputStream.nullOutputStream());retained.write(1);capture.succeeded();trace.wrote();
        }
        trace.end(true);require(trace.encodedWrite()!=null,"late-write control did not publish");
        retained.write(2);require(trace.encodedWrite()==null,"late write retained old digest");
        trace.begin();trace.serialized();trace.writing();
        try(var capture=SavedDataEncodedWrite.open(trace,image)) {
            SavedDataEncodedWrite.observe(image,OutputStream.nullOutputStream()).write(4);capture.succeeded();trace.wrote();
        }
        trace.end(true);var renewed=trace.encodedWrite();require(renewed!=null,"new generation did not recover");
        retained.write(5);require(renewed.equals(trace.encodedWrite()),"old stream revoked a new generation");
        var outer=SavedDataEncodedWrite.open(trace,image);var inner=SavedDataEncodedWrite.open(trace,new Object());
        outer.close();inner.close();require(SavedDataEncodedWrite.observe(image,direct)==direct,"out-of-order close retained thread context");
        System.out.println("SavedDataEncodedWriteSelfTest PASS");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
