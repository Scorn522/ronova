package dev.ronova.coremod.observation;

import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;

public final class RecoveryFileReadbackSelfTest {
    public static void main(String[] args) throws Exception {
        Path base=Path.of("build","readback-tests");Files.createDirectories(base);
        Path folder=Files.createTempDirectory(base,"case-");Path file=folder.resolve("image.dat");
        byte[] content=new byte[32769];for(int i=0;i<content.length;i++)content[i]=(byte)(i*37);
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        try {
            Files.write(file,content);
            boolean identityAvailable=Files.readAttributes(file,java.nio.file.attribute.BasicFileAttributes.class).fileKey()!=null;
            var result=RecoveryFileReadback.read(file,hash,content.length,()->true);
            require((identityAvailable?result.status()==RecoveryFileReadback.Status.OBSERVED_MATCH:result.status()==RecoveryFileReadback.Status.UNAVAILABLE&&result.reason()==RecoveryFileReadback.Reason.FILE_IDENTITY_UNAVAILABLE)&&result.bytes()==content.length,"exact image read failed: "+result);
            result=RecoveryFileReadback.read(file,"0".repeat(64),content.length,()->true);
            require(identityAvailable?result.status()==RecoveryFileReadback.Status.DIFFERENT_IMAGE:
                result.status()==RecoveryFileReadback.Status.UNAVAILABLE&&result.reason()==RecoveryFileReadback.Reason.FILE_IDENTITY_UNAVAILABLE,"different image became a match");
            require(RecoveryFileReadback.read(file,hash,content.length-1,()->true).reason()==RecoveryFileReadback.Reason.TOO_LARGE,"size bound ignored");
            require(RecoveryFileReadback.read(folder.resolve("missing"),hash,100,()->true).status()==RecoveryFileReadback.Status.UNAVAILABLE,"missing file became an empty success");
            require(RecoveryFileReadback.read(folder,hash,100,()->true).reason()==RecoveryFileReadback.Reason.NOT_REGULAR,"directory accepted as an image");
            var permits=new AtomicInteger();
            result=RecoveryFileReadback.read(file,hash,content.length,()->permits.incrementAndGet()<3);
            require(result.status()==RecoveryFileReadback.Status.BUDGET_EXHAUSTED&&result.observedHash()==null,"partial read fabricated a full image hash");
            result=RecoveryFileReadback.read(file,hash,content.length,()->true);
            require(identityAvailable?result.status()==RecoveryFileReadback.Status.OBSERVED_MATCH:
                result.status()==RecoveryFileReadback.Status.UNAVAILABLE&&result.reason()==RecoveryFileReadback.Reason.FILE_IDENTITY_UNAVAILABLE,"failed read retained an open handle or corrupted retry");
            permits.set(0);
            result=RecoveryFileReadback.read(file,hash,content.length+1,()->{
                if(permits.incrementAndGet()==3)try {Files.write(file,new byte[]{1},StandardOpenOption.APPEND);}
                catch(Exception error){throw new AssertionError(error);}
                return true;
            });
            require(result.status()==RecoveryFileReadback.Status.UNAVAILABLE&&result.observedHash()==null,"concurrent growth became a stable read");
            System.out.println("RecoveryFileReadbackSelfTest PASS identity_available="+identityAvailable+" (no durable completion)");
        } finally {Files.deleteIfExists(file);Files.delete(folder);}
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
