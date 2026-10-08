package dev.ronova.coremod.observation;

import dev.ronova.coremod.budget.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

/** Owned, bounded preparation worker. Requests contain no world objects and never write source files. */
public final class StorageImageTask implements AutoCloseable {
    public enum State { RUNNING, CANCEL_PENDING, EXPIRED_PENDING, READY, FINISHED, CANCELLED, EXPIRED, FAILED }
    public record Request(Path path,List<List<NbtStoragePreservation.Step>> positions,int encodedLimit,int decodedLimit,int nodes,String writtenHash,String writtenFileIdentity) {
        public Request(Path path,List<List<NbtStoragePreservation.Step>> positions,int encodedLimit,int decodedLimit,int nodes,String writtenHash) {
            this(path,positions,encodedLimit,decodedLimit,nodes,writtenHash,null);
        }
        public Request(Path path,List<List<NbtStoragePreservation.Step>> positions,int encodedLimit,int decodedLimit,int nodes) {
            this(path,positions,encodedLimit,decodedLimit,nodes,null);
        }
        public Request {
            Objects.requireNonNull(path);positions=positions.stream().map(List::copyOf).toList();
            if(writtenHash!=null)writtenHash=dev.ronova.coremod.root.api.EffectRecord.checkedHash(writtenHash);
            if(positions.isEmpty()||positions.size()>64||positions.stream().anyMatch(p->p.isEmpty()||p.size()>64)
                ||encodedLimit<1||encodedLimit>NbtStorageImage.MAX_IMAGE||decodedLimit<1||decodedLimit>NbtStorageImage.MAX_IMAGE
                ||nodes<1||nodes>65536)throw new IllegalArgumentException("invalid storage preparation request");
        }
    }
    public record Prepared(RecoveryFileReadback.Result baseline,NbtStorageImage.Candidate candidate) {}
    public static final class Output implements AutoCloseable {
        private Prepared value;private final Runnable release;
        private Output(Prepared value,Runnable release){this.value=value;this.release=release;}
        public synchronized Prepared value(){if(value==null)throw new IllegalStateException("output released");return value;}
        @Override public synchronized void close(){if(value!=null){value=null;release.run();}}
    }
    private static final Semaphore WORKERS=new Semaphore(2);
    private final AtomicBoolean cancelled=new AtomicBoolean();
    private final long issued=System.nanoTime();
    private final Thread worker;
    private boolean closed,failed,expired;private Output output;
    private StorageImageTask(Function<BooleanSupplier,Prepared> operation,Runnable releaseWork,Runnable releaseOutput) {
        worker=new Thread(()->{
            try {
                Prepared prepared=active()?operation.apply(this::active):null;
                synchronized(this) {
                    long age=System.nanoTime()-issued;expired=age<0||age>=5_000_000_000L;
                    if(prepared!=null&&active()&&!closed)output=new Output(prepared,releaseOutput);
                    else releaseOutput.run();
                }
            } catch(RuntimeException|Error failure) {synchronized(this){failed=true;releaseOutput.run();}}
            finally {
                try {releaseWork.run();} catch(RuntimeException|Error failure){synchronized(this){failed=true;}}
                finally {WORKERS.release();}
            }
        },"ronova-storage-image");
        worker.setDaemon(true);worker.setContextClassLoader(null);
    }
    public static StorageImageTask start(Request request,HostResourceGovernor resources) {
        return startOperation(request,resources,active->{
            var captured=RecoveryFileReadback.capture(request.path(),request.encodedLimit(),active);
            if(!active.getAsBoolean()||captured.result().status()!=RecoveryFileReadback.Status.OBSERVED_IMAGE)
                return new Prepared(captured.result(),null);
            if(request.writtenHash()!=null&&!request.writtenHash().equals(captured.result().observedHash())
                ||request.writtenFileIdentity()!=null&&!request.writtenFileIdentity().equals(captured.result().fileIdentity()))
                return new Prepared(new RecoveryFileReadback.Result(RecoveryFileReadback.Status.DIFFERENT_IMAGE,
                    RecoveryFileReadback.Reason.NONE,captured.result().bytes(),captured.result().observedHash(),captured.result().fileIdentity()),null);
            try {
                var candidate=NbtStorageImage.remove(captured.encoded(),request.positions(),request.decodedLimit(),request.nodes());
                if(!candidate.baselineHash().equals(captured.result().observedHash()))throw new IllegalStateException("baseline changed during preparation");
                return new Prepared(captured.result(),candidate);
            } catch(java.io.IOException unsupported) {throw new IllegalArgumentException("storage image unsupported",unsupported);}
        });
    }
    static StorageImageTask startOperation(Request request,HostResourceGovernor resources,Function<BooleanSupplier,Prepared> operation) {
        Objects.requireNonNull(request);Objects.requireNonNull(resources);Objects.requireNonNull(operation);
        if(!WORKERS.tryAcquire())return null;
        HostResourceGovernor.Reservation work=null,result=null;
        try {
            var charges=Map.of(HostResourceDomain.JAVA_HEAP,12L*(request.encodedLimit()+request.decodedLimit())+128L*request.nodes()+65536,
                HostResourceDomain.THREAD,1L,HostResourceDomain.THREAD_STACK,1024L*1024,
                HostResourceDomain.FILE_DESCRIPTOR,2L,HostResourceDomain.PLATFORM_HANDLE,2L,
                HostResourceDomain.JNI_JNA_NATIVE,262144L);
            for(var domain:charges.keySet())resources.declareAccounting(domain,HostResourceGovernor.Accounting.PARTIAL_ESTIMATE,"storage-image-work");
            var allocation=resources.reserve(charges,HostResourceGovernor.Priority.RECOVERY);
            if(!allocation.granted()){WORKERS.release();return null;}work=allocation.reservation();
            allocation=resources.reserve(Map.of(HostResourceDomain.JAVA_HEAP,2L*request.encodedLimit()+65536),HostResourceGovernor.Priority.RECOVERY);
            if(!allocation.granted()){work.close();WORKERS.release();return null;}result=allocation.reservation();
            var task=new StorageImageTask(operation,work::close,result::close);task.worker.start();return task;
        } catch(RuntimeException|Error failure) {
            if(result!=null)result.close();if(work!=null)work.close();WORKERS.release();throw failure;
        }
    }
    private boolean active(){long age=System.nanoTime()-issued;return !cancelled.get()&&age>=0&&age<5_000_000_000L;}
    public boolean done(){return !worker.isAlive();}
    public synchronized State state(){
        // READY reports a retained candidate, not source validity or permission to commit it.
        if(done())return failed?State.FAILED:cancelled.get()?State.CANCELLED:expired?State.EXPIRED:
            output!=null&&output.value().candidate()!=null?State.READY:State.FINISHED;
        if(cancelled.get())return State.CANCEL_PENDING;
        return active()?State.RUNNING:State.EXPIRED_PENDING;
    }
    /** Transfer the result's reservation to the consumer only after actual worker termination. */
    public synchronized Output take(){if(!done()||closed||failed)return null;var value=output;output=null;return value;}
    @Override public synchronized void close(){closed=true;if(!done())cancelled.set(true);if(output!=null){output.close();output=null;}}
}
