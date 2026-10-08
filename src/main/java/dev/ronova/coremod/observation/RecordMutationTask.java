package dev.ronova.coremod.observation;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/** Bounded foreign-monitor waits; cancellation never masquerades as thread termination. */
public final class RecordMutationTask {
    public enum State { RUNNING, CANCEL_PENDING, EXPIRED_PENDING, FINISHED, FAILED }
    private static final Semaphore WORKERS = new Semaphore(2);
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final long issued = System.nanoTime();
    private final Thread thread;
    private volatile AtomicSnapshotRecord.Result result;
    private volatile boolean failed;
    private RecordMutationTask(Function<BooleanSupplier, AtomicSnapshotRecord.Result> operation, Runnable release) {
        thread = new Thread(() -> {
            try { result = active() ? operation.apply(this::active) : AtomicSnapshotRecord.Result.FENCED; }
            catch (AtomicSnapshotRecord.CompletionFailure failure) {
                result = failure.completedResult(); failed = true;
            }
            catch (RuntimeException | Error failure) { failed = true; }
            finally {
                try { release.run(); }
                catch (RuntimeException | Error failure) { failed = true; }
                finally { WORKERS.release(); }
            }
        }, "ronova-record-disposition");
        thread.setDaemon(true);
        thread.setContextClassLoader(null);
    }
    /** On rejection the caller still owns its reservation; on success the worker releases it. */
    public static RecordMutationTask start(Function<BooleanSupplier, AtomicSnapshotRecord.Result> operation, Runnable release) {
        if (!WORKERS.tryAcquire()) return null;
        try {
            var task = new RecordMutationTask(operation, release);
            task.thread.start(); return task;
        } catch (RuntimeException | Error failure) { WORKERS.release(); throw failure; }
    }
    public void cancel() { cancelled.set(true); }
    public boolean active() {
        long age = System.nanoTime() - issued;
        return !cancelled.get() && age >= 0 && age < 5_000_000_000L;
    }
    public boolean done() { return !thread.isAlive(); }
    public State state() {
        if (done()) return failed ? State.FAILED : State.FINISHED;
        long age = System.nanoTime() - issued;
        if (age < 0 || age >= 5_000_000_000L) return State.EXPIRED_PENDING;
        return cancelled.get() ? State.CANCEL_PENDING : State.RUNNING;
    }
    /** JVM monitor wait only; not a claim about which external source owns the monitor. */
    public boolean monitorBlocked() { return thread.getState() == Thread.State.BLOCKED; }
    public boolean failed() { return failed; }
    public AtomicSnapshotRecord.Result result() { return result; }
    /** Resource/permit cleanup failure does not turn a known rejected write into an unknown mutation. */
    public dev.ronova.coremod.root.api.RecoveryMutationIntent.Observation observation(boolean verified) {
        var uncertain = dev.ronova.coremod.root.api.RecoveryMutationIntent.Observation.WRITE_UNVERIFIED;
        if (!done() || result == null) return uncertain;
        return switch (result) {
            case REMOVED -> verified && !failed
                ? dev.ronova.coremod.root.api.RecoveryMutationIntent.Observation.WRITE_OBSERVED : uncertain;
            case NOT_CURRENT -> dev.ronova.coremod.root.api.RecoveryMutationIntent.Observation.NOT_CURRENT;
            case FENCED, UNSUPPORTED -> dev.ronova.coremod.root.api.RecoveryMutationIntent.Observation.NOT_EXECUTED;
        };
    }
}
