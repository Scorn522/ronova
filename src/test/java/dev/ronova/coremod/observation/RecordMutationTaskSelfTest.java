package dev.ronova.coremod.observation;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.LockSupport;

public final class RecordMutationTaskSelfTest {
    public static void main(String[] args) throws Exception {
        verifyFinalEntryGuard();
        var map = new ConcurrentHashMap<UUID,Object>();
        var id = UUID.randomUUID(); var value = new Object(); map.put(id, value);
        var locked = new CountDownLatch(1); var unlock = new CountDownLatch(1);
        Thread holder = new Thread(() -> map.computeIfPresent(id, (key, current) -> {
            locked.countDown();
            try { if (!unlock.await(5, TimeUnit.SECONDS)) throw new AssertionError("fixture lock timed out"); }
            catch (InterruptedException error) { throw new AssertionError(error); }
            return current;
        }));
        holder.setDaemon(true); holder.start();
        var reached = new CountDownLatch(1); var acquired = new AtomicInteger(); var released = new AtomicInteger();
        var resources = new dev.ronova.coremod.budget.HostResourceGovernor();
        resources.declareAccounting(dev.ronova.coremod.budget.HostResourceDomain.THREAD,
            dev.ronova.coremod.budget.HostResourceGovernor.Accounting.PARTIAL_ESTIMATE, "fixture-worker-permit");
        var reservation = resources.reserve(java.util.Map.of(dev.ronova.coremod.budget.HostResourceDomain.THREAD, 1L),
            dev.ronova.coremod.budget.HostResourceGovernor.Priority.RECOVERY);
        require(reservation.granted(), "fixture worker reservation failed");
        RecordMutationTask task = null;
        try {
            require(locked.await(5, TimeUnit.SECONDS), "holder did not obtain the entry lock");
            task = RecordMutationTask.start(active -> {
                reached.countDown();
                return AtomicSnapshotRecord.removeWithPermit(map, id, value, () -> {
                    if (!active.getAsBoolean()) return null;
                    acquired.incrementAndGet();
                    throw new AssertionError("cancelled waiter acquired authority");
                });
            }, () -> { reservation.reservation().close(); released.incrementAndGet(); });
            require(task != null && reached.await(5, TimeUnit.SECONDS), "worker did not start");
            require(acquired.get() == 0 && !task.done(), "authority was acquired before the container lock");
            task.cancel();
            require(!task.done() && task.state() == RecordMutationTask.State.CANCEL_PENDING && released.get() == 0
                && threadPermits(resources) == 1, "cancellation pretended a blocked worker exited or released its permit");
        } finally { unlock.countDown(); holder.join(5000); }
        await(task);
        require(task.result() == AtomicSnapshotRecord.Result.FENCED && map.get(id) == value
            && acquired.get() == 0 && released.get() == 1 && threadPermits(resources) == 0
            && task.state() == RecordMutationTask.State.FINISHED,
            "late cancelled task mutated its record or leaked reservation");

        var closedAfterPublication = new AtomicBoolean();
        task = RecordMutationTask.start(active -> AtomicSnapshotRecord.removeWithPermit(map, id, value,
            () -> new AtomicSnapshotRecord.Permit() {
                public boolean valid() { return active.getAsBoolean(); }
                public void close() { closedAfterPublication.set(!map.containsKey(id)); }
            }), released::incrementAndGet);
        require(task != null, "finished worker did not release capacity"); await(task);
        require(task.result() == AtomicSnapshotRecord.Result.REMOVED && closedAfterPublication.get()
            && released.get() == 2, "authority ended before actual entry publication");
        cancelledWorkersRetainCapacity();
        failedReleaseDoesNotLeakCapacity();
        permitCleanupKeepsPublicationResult();
        receiptClassification();
        System.out.println("RecordMutationTaskSelfTest PASS");
    }
    private static void cancelledWorkersRetainCapacity() throws Exception {
        var entered = new CountDownLatch(2); var leave = new CountDownLatch(1);
        var releases = new AtomicInteger();
        var workers = new RecordMutationTask[2];
        try {
            for (int index = 0; index < workers.length; index++) {
                workers[index] = RecordMutationTask.start(active -> {
                    entered.countDown();
                    try { if (!leave.await(5, TimeUnit.SECONDS)) throw new AssertionError("capacity fixture timed out"); }
                    catch (InterruptedException failure) { throw new AssertionError(failure); }
                    return active.getAsBoolean() ? AtomicSnapshotRecord.Result.REMOVED : AtomicSnapshotRecord.Result.FENCED;
                }, releases::incrementAndGet);
                require(workers[index] != null, "worker capacity unavailable before saturation");
            }
            require(entered.await(5, TimeUnit.SECONDS), "workers did not enter operation");
            for (var worker : workers) worker.cancel();
            var rejectedRelease = new AtomicInteger();
            var extra = RecordMutationTask.start(active -> {
                throw new AssertionError("cancelled but live workers allowed a replacement");
            }, rejectedRelease::incrementAndGet);
            require(extra == null && rejectedRelease.get() == 0 && releases.get() == 0,
                "cancellation freed capacity or rejection took ownership of the caller reservation");
        } finally {
            leave.countDown();
            for (var worker : workers) if (worker != null) await(worker);
        }
        require(releases.get() == 2, "cancelled workers did not release exactly once after exiting");
        for (var worker : workers) require(worker.result() == AtomicSnapshotRecord.Result.FENCED,
            "cancelled operation reported a successful write");
    }
    private static void failedReleaseDoesNotLeakCapacity() {
        var released = new AtomicInteger();
        var task = RecordMutationTask.start(active -> AtomicSnapshotRecord.Result.REMOVED, () -> {
            released.incrementAndGet(); throw new IllegalStateException("fixture accounting failure");
        });
        require(task != null, "failure fixture could not start");
        awaitExit(task);
        require(task.failed() && task.state() == RecordMutationTask.State.FAILED
            && task.result() == AtomicSnapshotRecord.Result.REMOVED && released.get() == 1,
            "cleanup failure erased the operation result or repeated resource release");
        task = RecordMutationTask.start(active -> AtomicSnapshotRecord.Result.FENCED, released::incrementAndGet);
        require(task != null, "cleanup failure stranded a worker permit");
        await(task);
        require(released.get() == 2, "replacement task did not release its reservation");
    }
    private static void await(RecordMutationTask task) {
        awaitExit(task);
        require(!task.failed(), "worker did not exit successfully");
    }
    private static void receiptClassification() {
        var id = UUID.randomUUID(); var expected = new Object();
        for (boolean supported : new boolean[] { true, false }) {
            Object map = supported ? new ConcurrentHashMap<UUID, Object>() : new java.util.HashMap<UUID, Object>();
            var task = RecordMutationTask.start(active -> AtomicSnapshotRecord.remove(map, id, expected, active),
                () -> { throw new IllegalStateException("fixture resource release error"); });
            require(task != null, "classification task did not start"); awaitExit(task);
            require(task.failed() && task.observation(false) == (supported
                ? dev.ronova.coremod.root.api.RecoveryMutationIntent.Observation.NOT_CURRENT
                : dev.ronova.coremod.root.api.RecoveryMutationIntent.Observation.NOT_EXECUTED),
                "known absent/unsupported operation was reported as a possible write");
        }
        var task = RecordMutationTask.start(active -> { throw new IllegalStateException("fixture unknown operation failure"); }, () -> {});
        require(task != null, "unknown classification task did not start"); awaitExit(task);
        require(task.observation(true) == dev.ronova.coremod.root.api.RecoveryMutationIntent.Observation.WRITE_UNVERIFIED,
            "missing operation result became confirmed or not-executed");
    }
    private static void permitCleanupKeepsPublicationResult() {
        var id = UUID.randomUUID(); var map = new ConcurrentHashMap<UUID, Object>();
        var value = new Object(); var released = new AtomicInteger();
        for (boolean allowed : new boolean[] { true, false }) {
            map.put(id, value);
            var failure = new IllegalStateException("fixture permit close");
            var task = RecordMutationTask.start(active -> AtomicSnapshotRecord.removeWithPermit(map, id, value,
                () -> new AtomicSnapshotRecord.Permit() {
                    public boolean valid() { return allowed && active.getAsBoolean(); }
                    public void close() { throw failure; }
                }), released::incrementAndGet);
            require(task != null, "publication fixture worker was rejected"); awaitExit(task);
            require(task.failed() && task.state() == RecordMutationTask.State.FAILED
                && task.result() == (allowed ? AtomicSnapshotRecord.Result.REMOVED : AtomicSnapshotRecord.Result.FENCED)
                && (allowed ? !map.containsKey(id) : map.get(id) == value),
                "permit cleanup erased the published result or invented a write");
            require(task.observation(true) == (allowed
                ? dev.ronova.coremod.root.api.RecoveryMutationIntent.Observation.WRITE_UNVERIFIED
                : dev.ronova.coremod.root.api.RecoveryMutationIntent.Observation.NOT_EXECUTED),
                "cleanup failure misclassified a known rejection or confirmed a failed write");
        }
        require(released.get() == 2, "failed permit cleanup leaked worker resources");
        map.put(id, value);
        var bodyFailure = new IllegalStateException("fixture before publication");
        var closeFailure = new IllegalStateException("fixture secondary cleanup");
        boolean caught = false;
        try {
            AtomicSnapshotRecord.removeWithPermit(map, id, value, () -> new AtomicSnapshotRecord.Permit() {
                public boolean valid() { throw bodyFailure; }
                public void close() { throw closeFailure; }
            });
        } catch (IllegalStateException actual) {
            caught = true;
            require(actual == bodyFailure && actual.getSuppressed().length == 1
                && actual.getSuppressed()[0] == closeFailure, "cleanup replaced the original pre-publication failure");
        }
        require(caught && map.get(id) == value, "failed map operation claimed publication");
    }
    private static void verifyFinalEntryGuard() throws Exception {
        var map=new ConcurrentHashMap<UUID,Object>();UUID target=UUID.randomUUID(),other=UUID.randomUUID();
        Object independent=new Object(),inserted=new Object();map.put(other,independent);
        var attempting=new CountDownLatch(1);var published=new AtomicBoolean();
        Thread writer=new Thread(()->{attempting.countDown();map.put(target,inserted);published.set(true);},"fixture-entry-reinsert");
        writer.setDaemon(true);
        try {
            require(AtomicSnapshotRecord.guardSlot(map,target,null,()->{
                writer.start();
                try {require(attempting.await(2,TimeUnit.SECONDS),"reinsert writer did not start");}
                catch(InterruptedException failure){throw new AssertionError(failure);}
                long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
                while(writer.getState()!=Thread.State.BLOCKED&&System.nanoTime()<end)LockSupport.parkNanos(1_000_000);
                require(writer.getState()==Thread.State.BLOCKED&&!published.get(),"insertion crossed the final entry guard");
                require(map.get(other)==independent,"guard changed a shared entry");
            }),"absent entry guard not entered");
        } finally {writer.join(3000);}
        require(!writer.isAlive()&&published.get()&&map.get(target)==inserted,"entry guard did not release the competing publisher");
        require(!AtomicSnapshotRecord.guardSlot(map,target,null,()->{throw new AssertionError("stale absence authorized commit");}),
            "stale entry guard accepted a reinserted record");
    }
    private static void awaitExit(RecordMutationTask task) {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!task.done() && System.nanoTime() < end) LockSupport.parkNanos(1_000_000);
        require(task.done(), "worker did not exit");
    }
    private static long threadPermits(dev.ronova.coremod.budget.HostResourceGovernor resources) {
        return resources.snapshot().stream().filter(row -> row.domain() == dev.ronova.coremod.budget.HostResourceDomain.THREAD)
            .findFirst().orElseThrow().ownedUsage();
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
