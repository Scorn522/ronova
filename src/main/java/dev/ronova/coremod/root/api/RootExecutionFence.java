package dev.ronova.coremod.root.api;

import java.util.Objects;
import java.util.function.LongSupplier;

/** Excludes new canonical commits during one registered raw-write step of this Root instance. */
public final class RootExecutionFence {
    public static final long MAX_DURATION_NANOS=250_000_000L;
    private final LongSupplier clock;
    private Lease active;
    public RootExecutionFence() { this(System::nanoTime); }
    RootExecutionFence(LongSupplier clock) { this.clock=Objects.requireNonNull(clock); }
    public synchronized Lease tryAcquire(TerminalOperationPlan plan) {
        Objects.requireNonNull(plan);
        if(active!=null)return null;
        active=new Lease(plan,clock.getAsLong());return active;
    }
    public synchronized Lease tryAcquireOwner(AuthorityStamp authority) {
        Objects.requireNonNull(authority);
        if(active!=null||!authority.owner()||!authority.settled())return null;
        active=new Lease(authority,clock.getAsLong());return active;
    }
    public synchronized boolean busy() { return active!=null; }
    public synchronized boolean ownsTerminal(Lease lease,TerminalOperationPlan plan) {
        return lease!=null&&active==lease&&lease.valid(plan);
    }
    public synchronized boolean ownsStorage(Lease lease,RecoveryStoragePlan plan) {
        return lease!=null&&active==lease&&lease.valid(plan);
    }
    public synchronized Lease tryAcquireRecord(RecoveryRecordPlan plan) {
        Objects.requireNonNull(plan);
        if(active!=null)return null;
        active=new Lease(plan,clock.getAsLong());return active;
    }
    public synchronized Lease tryAcquireSnapshot(RecoverySnapshotPlan plan) {
        Objects.requireNonNull(plan);
        if(active!=null)return null;
        active=new Lease(plan,clock.getAsLong());return active;
    }
    public synchronized Lease tryAcquireTask(RecoveryTaskPlan plan) {
        Objects.requireNonNull(plan);if(active!=null)return null;active=new Lease(plan,clock.getAsLong());return active;
    }
    public synchronized Lease tryAcquireStorage(RecoveryStoragePlan plan) {
        Objects.requireNonNull(plan);if(active!=null)return null;active=new Lease(plan,clock.getAsLong());return active;
    }

    public final class Lease implements AutoCloseable {
        private final Object scope;
        private final long issued;
        private final Thread owner=Thread.currentThread();
        private boolean revoked;
        private Lease(Object scope,long issued) { this.scope=scope;this.issued=issued; }
        public boolean valid(TerminalOperationPlan expected) {return validScope(expected);}
        public boolean valid(AuthorityStamp expected) {return validScope(expected);}
        public boolean valid(RecoverySnapshotPlan expected) {return validScope(expected);}
        public boolean valid(RecoveryRecordPlan expected) {return validScope(expected);}
        public boolean valid(RecoveryTaskPlan expected) {return validScope(expected);}
        public boolean valid(RecoveryStoragePlan expected) {return validScope(expected);}
        private boolean validScope(Object expected) {
            synchronized(RootExecutionFence.this) {
                long age=clock.getAsLong()-issued;
                if(active!=this||revoked)return false;
                if(Thread.currentThread()!=owner||!scope.equals(expected)||age<0||age>=MAX_DURATION_NANOS){revoked=true;return false;}
                return true;
            }
        }
        @Override public void close() {
            synchronized(RootExecutionFence.this) {
                if(Thread.currentThread()!=owner)throw new IllegalStateException("Root execution fence belongs to another thread");
                if(active==this)active=null;
            }
        }
    }
    // Expiry revokes further writes but never forcibly releases a possibly paused
    // writer. The owning try/finally must leave the step before a new commit enters.
}
