package dev.ronova.coremod.root.api;

import dev.ronova.coremod.contract.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RootExecutionFenceSelfTest {
    public static void main(String[] args) throws Exception {
        var key=new SubjectKey(new RealmId(new UUID(717,951)),new UUID(717,952));
        var q=QualificationPreconditions.baseline(key).judgmentTransition(new UUID(717,953));
        var plan=TerminalOperationPlan.terminal(new UUID(717,954),EffectRecord.hash("fence-fixture"),1,1,q,1,1);
        long[] now={0};var fence=new RootExecutionFence(()->now[0]);
        var lease=fence.tryAcquire(plan);
        require(lease!=null&&lease.valid(plan)&&fence.busy()&&fence.tryAcquire(plan)==null,"overlapping write steps admitted");
        now[0]=RootExecutionFence.MAX_DURATION_NANOS;
        require(!lease.valid(plan)&&fence.busy(),"expiry admitted commits before the paused step left");
        now[0]=0;require(!lease.valid(plan),"expired step revived after clock reversal");
        lease.close();require(!fence.busy(),"closed step kept Root busy");
        var next=fence.tryAcquire(plan);lease.close();
        require(next!=null&&next.valid(plan)&&fence.busy(),"old close released another step");
        AtomicBoolean denied=new AtomicBoolean();
        Thread other=new Thread(()->{
            try {next.close();}catch(IllegalStateException expected){denied.set(true);}
        },"ronova-fence-wrong-thread");
        other.setDaemon(true);other.start();other.join(2000);
        require(!other.isAlive()&&denied.get()&&fence.busy(),"another thread released a running write step");
        next.close();require(!fence.busy(),"owner could not release its step");
        try(var exceptional=fence.tryAcquire(plan)){throw new FixtureFailure();}catch(FixtureFailure expected){}
        require(!fence.busy(),"exception bypassed fence cleanup");
        var ownerStamp=new AuthorityStamp(key,null,1,true,1,true,DominionEpoch.LOCAL_INITIAL);
        try(var ownerLease=fence.tryAcquireOwner(ownerStamp)) {
            require(ownerLease!=null&&ownerLease.valid(ownerStamp)&&fence.tryAcquire(plan)==null,
                "Owner and terminal writes did not share Root exclusion");
            require(!ownerLease.valid(plan)&&fence.busy(),"Owner lease was reused as terminal execution permission");
        }
        require(fence.tryAcquireOwner(new AuthorityStamp(key,null,1,false,1,true,DominionEpoch.LOCAL_INITIAL))==null
            &&fence.tryAcquireOwner(new AuthorityStamp(key,null,1,true,1,false,DominionEpoch.LOCAL_INITIAL))==null,
            "non-Owner or unsettled state acquired Owner exclusion");
        var snapshotPlan = new RecoverySnapshotPlan(plan, EffectRecord.hash("snapshot-cell"), 1, 2, 1);
        try (var snapshotLease = fence.tryAcquireSnapshot(snapshotPlan)) {
            require(snapshotLease != null && snapshotLease.valid(snapshotPlan) && fence.tryAcquire(plan) == null
                && fence.tryAcquireOwner(ownerStamp) == null, "snapshot writes did not share Root exclusion");
            require(!snapshotLease.valid(new RecoverySnapshotPlan(plan, snapshotPlan.sourceLocator(), 2, 2, 1)),
                "snapshot lease survived a source version change");
            require(!snapshotLease.valid(snapshotPlan), "revoked snapshot lease became valid again");
        }
        try (var snapshotLease = fence.tryAcquireSnapshot(snapshotPlan)) {
            require(!snapshotLease.valid(plan), "snapshot lease granted general terminal write permission");
        }
        var recordPlan = new RecoveryRecordPlan(snapshotPlan);
        var loadedPlan = new RecoverySnapshotPlan(plan, snapshotPlan.sourceLocator(), 1, 2, 1,
            RecoverySnapshotPlan.StorageScope.LOADED_SAVED_DATA);
        try (var loadedLease = fence.tryAcquireSnapshot(loadedPlan)) {
            require(loadedLease != null && loadedLease.valid(loadedPlan) && !loadedLease.valid(snapshotPlan),
                "loaded snapshot lease granted static-manager permission");
        }
        try (var recordLease = fence.tryAcquireRecord(recordPlan)) {
            require(recordLease != null && recordLease.valid(recordPlan) && fence.tryAcquireSnapshot(snapshotPlan) == null
                && fence.tryAcquire(plan) == null && fence.tryAcquireOwner(ownerStamp) == null,
                "record mutation did not share Root exclusion");
            require(!recordLease.valid(snapshotPlan) && !recordLease.valid(recordPlan),
                "record permission was reused as a cell write or revived after revocation");
        }
        System.out.println("RootExecutionFenceSelfTest PASS");
    }
    private static final class FixtureFailure extends RuntimeException {}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
