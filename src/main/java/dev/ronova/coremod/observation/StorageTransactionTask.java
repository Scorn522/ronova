package dev.ronova.coremod.observation;

import dev.ronova.coremod.budget.*;
import dev.ronova.coremod.nativebridge.NativeBridge;
import java.util.Map;

/** A privately staged OS transaction. No world access on its worker; no permission in its token. */
public final class StorageTransactionTask implements AutoCloseable {
    public enum State { STAGING, READY, UNSUPPORTED, BASELINE_CHANGED, FAILED, ROLLED_BACK, COMMITTED, READING, READBACK_MATCH, READBACK_CHANGED, UNKNOWN, CLOSED }
    private RecoverySourceTracker.StorageCommitRequest request;
    private final HostResourceGovernor.Reservation reservation;
    private volatile Thread worker;
    private State state=State.STAGING;
    private boolean closed,released;
    private long token,error;
    private RecoveryFileReadback.Result readback;
    private StorageTransactionTask(RecoverySourceTracker.StorageCommitRequest request,HostResourceGovernor.Reservation reservation) {
        this.request=request;this.reservation=reservation;
        worker=new Thread(this::stage,"ronova-storage-transaction");worker.setDaemon(true);worker.setContextClassLoader(null);
    }
    static StorageTransactionTask start(RecoverySourceTracker.StorageCommitRequest request,HostResourceGovernor resources) {
        var charges=Map.of(HostResourceDomain.JAVA_HEAP,2L*request.encodedSize()+65536,
            HostResourceDomain.JNI_JNA_NATIVE,(long)request.encodedSize()+262144,
            HostResourceDomain.THREAD,1L,HostResourceDomain.THREAD_STACK,1024L*1024,
            HostResourceDomain.FILE_DESCRIPTOR,1L,HostResourceDomain.PLATFORM_HANDLE,3L);
        for(var domain:charges.keySet())resources.declareAccounting(domain,HostResourceGovernor.Accounting.PARTIAL_ESTIMATE,"storage-transaction");
        var allocation=resources.reserve(charges,HostResourceGovernor.Priority.RECOVERY);if(!allocation.granted())return null;
        try {var work=new StorageTransactionTask(request,allocation.reservation());work.worker.start();return work;}
        catch(RuntimeException|Error failure){allocation.reservation().close();throw failure;}
    }
    private void stage() {
        try {
            long[] result=NativeBridge.prepareStorageTransaction(request);
            synchronized(this) {
                if(result==null||result.length!=3){state=State.UNKNOWN;return;}
                token=result[1];error=result[2];
                state=result[0]==1&&token>0?State.READY:result[0]==4?State.UNSUPPORTED:result[0]==5?State.BASELINE_CHANGED:State.FAILED;
                if(closed&&token>0)rollback();
            }
        } catch(RuntimeException|Error failure){synchronized(this){state=State.UNKNOWN;}}
        // Reservations survive worker unwind. The server confirms actual thread exit before releasing them.
    }
    public boolean done(){Thread active=worker;return active==null||!active.isAlive();}
    public synchronized State state(){return done()?state:state==State.READING?State.READING:State.STAGING;}
    public synchronized RecoveryFileReadback.Result readback(){return done()?readback:null;}
    public synchronized long error(){return error;}
    synchronized void unverifiedCommit(){if(state==State.COMMITTED)state=State.UNKNOWN;}
    synchronized State commit(RecoverySourceTracker.StoragePreparationResult.StorageWritePermit permit) {
        if(!done()||closed||state!=State.READY||permit==null||!permit.validFor(request))return state();
        // An exception at the JNI return boundary must never leave a retryable READY operation.
        state=State.UNKNOWN;
        long[] result=NativeBridge.finishStorageTransaction(request,token,permit);token=0;
        if(result==null||result.length!=3){state=State.UNKNOWN;return state;}
        error=result[2];state=result[0]==1?State.COMMITTED:result[0]==2?State.ROLLED_BACK:State.UNKNOWN;return state;
    }
    private void rollback() {
        long[] result=NativeBridge.finishStorageTransaction(request,token,null);token=0;
        if(result!=null&&result.length==3){error=result[2];state=result[0]==2?State.ROLLED_BACK:State.UNKNOWN;}
        else state=State.UNKNOWN;
    }
    synchronized void startReadback() {
        if(closed||!done()||state!=State.COMMITTED)return;
        state=State.READING;
        worker=new Thread(()->{
            final long deadline=System.nanoTime()+5_000_000_000L;
            try {
                var result=RecoveryFileReadback.read(java.nio.file.Path.of(request.path()),request.expectedHash(),
                    NbtStorageImage.MAX_IMAGE,()->{synchronized(this){return !closed&&System.nanoTime()<deadline;}});
                synchronized(this) {
                    readback=result;
                    state=result.status()==RecoveryFileReadback.Status.OBSERVED_MATCH&&request.fileIdentity().equals(result.fileIdentity())
                        ?State.READBACK_MATCH:State.READBACK_CHANGED;
                }
            } catch(RuntimeException|Error failure){synchronized(this){state=State.UNKNOWN;}}
        },"ronova-storage-readback");
        worker.setDaemon(true);worker.setContextClassLoader(null);
        try {worker.start();}catch(RuntimeException|Error failure){state=State.UNKNOWN;throw failure;}
    }
    @Override public synchronized void close() {
        closed=true;if(!done())return;
        if(token>0)rollback();
        if(!released){request=null;worker=null;reservation.close();released=true;}
    }
    public synchronized boolean resourcesReleased(){return released;}
}
