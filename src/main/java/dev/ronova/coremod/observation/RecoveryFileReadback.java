package dev.ronova.coremod.observation;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Read only one observed location. Raw image equality is not a durability, ownership or semantic receipt. */
public final class RecoveryFileReadback {
    public static final long MAX_BYTES = 16L * 1024 * 1024;
    public enum Status { OBSERVED_IMAGE, OBSERVED_MATCH, DIFFERENT_IMAGE, UNAVAILABLE, BUDGET_EXHAUSTED }
    public enum Reason { NONE, NOT_REGULAR, TOO_LARGE, CHANGED_DURING_READ, FILE_IDENTITY_UNAVAILABLE, IO_OR_UNSUPPORTED, PRECONDITION_UNPROVEN, RESOURCE_UNAVAILABLE, BUDGET }
    public record Result(Status status, Reason reason, long bytes, String observedHash, String fileIdentity) {
        public Result(Status status,Reason reason,long bytes,String observedHash){this(status,reason,bytes,observedHash,null);}
    }
    public static final class CapturedImage {
        private final Result result;private final byte[] encoded;
        private CapturedImage(Result result,byte[] encoded){this.result=result;this.encoded=encoded;}
        public Result result(){return result;}
        public byte[] encoded(){return encoded==null?null:encoded.clone();}
    }
    /** Collect bytes and identity through the same Native observation; no expected image or commit is inferred. */
    public static CapturedImage capture(Path observedPath,long limit,BooleanSupplier allowance) {
        var bytes=new java.io.ByteArrayOutputStream();
        var result=readInternal(observedPath,null,limit,allowance,bytes);
        if(result.status()!=Status.OBSERVED_IMAGE||result.fileIdentity()==null)
            return new CapturedImage(result,null);
        if(result.bytes()!=bytes.size())return new CapturedImage(unavailable(Reason.CHANGED_DURING_READ,bytes.size()),null);
        return new CapturedImage(result,bytes.toByteArray());
    }
    private RecoveryFileReadback() {}

    /** expectedHash must describe the complete encoded file image, not a logical NBT subset. */
    public static Result read(Path observedPath, String expectedHash, long limit, BooleanSupplier allowance) {
        if(expectedHash==null)throw new IllegalArgumentException("missing expected image hash");
        return readInternal(observedPath,expectedHash,limit,allowance,null);
    }
    private static Result readInternal(Path observedPath,String expectedHash,long limit,BooleanSupplier allowance,
        java.io.ByteArrayOutputStream collected) {
        Objects.requireNonNull(observedPath);Objects.requireNonNull(allowance);
        if(expectedHash!=null&&!expectedHash.matches("[0-9a-f]{64}")||limit<1||limit>MAX_BYTES)
            throw new IllegalArgumentException("invalid raw-image readback request");
        long count=0;
        try {
            var nativeDigest=MessageDigest.getInstance("SHA-256");
            long[] captured={0,0};
            var nativeResult=dev.ronova.coremod.nativebridge.NativeBridge.readFileImage(observedPath,limit,
                new dev.ronova.coremod.nativebridge.NativeBridge.FileReadObserver() {
                    public boolean allow(){captured[1]++;return allowance.getAsBoolean();}
                    public void accept(byte[] bytes,int length){
                        if(length<0||length>bytes.length||length>limit-captured[0])throw new IllegalStateException("invalid Native read chunk");
                        nativeDigest.update(bytes,0,length);if(collected!=null)collected.write(bytes,0,length);captured[0]+=length;
                    }
                });
            if(nativeResult!=null) {
                if(nativeResult.length!=8)return unavailable(Reason.IO_OR_UNSUPPORTED,captured[0]);
                long state=nativeResult[0];
                if(state==-1)return budget(captured[0]);
                if(state!=1)return unavailable(state==-2?Reason.TOO_LARGE:state==-3?Reason.CHANGED_DURING_READ:
                    state==-6?Reason.FILE_IDENTITY_UNAVAILABLE:Reason.IO_OR_UNSUPPORTED,captured[0]);
                if(nativeResult[1]!=captured[0]||nativeResult[6]!=captured[0])return unavailable(Reason.CHANGED_DURING_READ,captured[0]);
                String hash=HexFormat.of().formatHex(nativeDigest.digest());
                String identity="win32-file-id/1:"+Long.toUnsignedString(nativeResult[2],16)+":"
                    +Long.toUnsignedString(nativeResult[3],16)+":"+Long.toUnsignedString(nativeResult[4],16);
                return new Result(expectedHash==null?Status.OBSERVED_IMAGE:hash.equals(expectedHash)?Status.OBSERVED_MATCH:Status.DIFFERENT_IMAGE,Reason.NONE,captured[0],hash,identity);
            }
            // Never silently retry a partially entered Native observation using another file open.
            if(captured[1]!=0)return unavailable(Reason.IO_OR_UNSUPPORTED,captured[0]);
            if(collected!=null)return unavailable(Reason.FILE_IDENTITY_UNAVAILABLE,0);
            if(!allowance.getAsBoolean())return budget(count);
            var before=Files.readAttributes(observedPath,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
            if(!before.isRegularFile())return unavailable(Reason.NOT_REGULAR,count);
            if(before.size()>limit)return unavailable(Reason.TOO_LARGE,count);
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            try(var channel=FileChannel.open(observedPath,StandardOpenOption.READ,LinkOption.NOFOLLOW_LINKS)) {
                var buffer=ByteBuffer.allocate(16*1024);
                while(true) {
                    if(!allowance.getAsBoolean())return budget(count);
                    buffer.clear();
                    // At most one byte beyond the declared limit, solely to detect growth.
                    buffer.limit((int)Math.min(buffer.capacity(),limit-count+1));
                    int read=channel.read(buffer);
                    if(read<0)break;
                    if(read==0)return unavailable(Reason.IO_OR_UNSUPPORTED,count);
                    count+=read;
                    if(count>limit)return unavailable(Reason.TOO_LARGE,count);
                    buffer.flip();digest.update(buffer);
                }
                if(!allowance.getAsBoolean())return budget(count);
                var after=Files.readAttributes(observedPath,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
                if(!after.isRegularFile()||(before.fileKey()!=null&&after.fileKey()!=null&&!before.fileKey().equals(after.fileKey()))
                    ||!before.lastModifiedTime().equals(after.lastModifiedTime())
                    ||before.size()!=count||after.size()!=count||channel.size()!=count)
                    return unavailable(Reason.CHANGED_DURING_READ,count);
                if(before.fileKey()==null||after.fileKey()==null)return unavailable(Reason.FILE_IDENTITY_UNAVAILABLE,count);
            }
            String hash=HexFormat.of().formatHex(digest.digest());
            return new Result(hash.equals(expectedHash)?Status.OBSERVED_MATCH:Status.DIFFERENT_IMAGE,Reason.NONE,count,hash);
        } catch(IOException|UnsupportedOperationException|SecurityException error) {
            return unavailable(Reason.IO_OR_UNSUPPORTED,count);
        } catch(NoSuchAlgorithmException impossible) {throw new AssertionError(impossible);}
    }
    private static Result unavailable(Reason reason,long bytes){return new Result(Status.UNAVAILABLE,reason,bytes,null);}
    private static Result budget(long bytes){return new Result(Status.BUDGET_EXHAUSTED,Reason.BUDGET,bytes,null);}
}
