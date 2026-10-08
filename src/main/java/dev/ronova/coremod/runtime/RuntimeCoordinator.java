package dev.ronova.coremod.runtime;

import dev.ronova.coremod.baseline.BaselineTelemetry;
import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.budget.BudgetCategory;
import dev.ronova.coremod.budget.WorkBudget;
import dev.ronova.coremod.engine.SpawnFirewall;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.server.MinecraftServer;

/** EventBus-independent runtime lane. All entry points are idempotent. */
public final class RuntimeCoordinator {
    private static final Map<MinecraftServer, TickState> TICKS = new WeakHashMap<>();
    private static final Map<MinecraftServer, ChallengeLogState> CHALLENGE_LOGS = new WeakHashMap<>();
    private static final AtomicLong SERVER_PULSES = new AtomicLong();

    private RuntimeCoordinator() {}

    public static void serverTickHead(MinecraftServer server) {
        BaselineTelemetry.serverTickHead(server);
        serverTickHeadInternal(server,true);
    }

    private static boolean serverTickHeadInternal(MinecraftServer server,boolean mixinOwned) {
        if (server == null||!server.isSameThread()) {
            return false;
        }
        long tick = beginTick(server,mixinOwned);
        if (tick < 0L) {
            return false;
        }
        SERVER_PULSES.incrementAndGet();
        var runtime = ServerRuntimeRegistry.runtime(server);
        try {
            synchronized (TICKS) {
                TICKS.get(server).observationScope = new dev.ronova.coremod.observation.RecoveryObservationScope(runtime.recoverySources(), server);
            }
            runtime.frameBudgetGovernor().setSnapshotEpoch(snapshotEpoch(runtime));
            measure(runtime, BudgetCategory.MAINTENANCE, runtime::reconcileNative);
            measure(runtime, BudgetCategory.MAINTENANCE, () -> runtime.ingressWork().maintain(System.nanoTime(),32,
                net.minecraft.network.Connection::isConnected));
            measure(runtime, BudgetCategory.MAINTENANCE, () -> runtime.diagnostics().sampleIfDue(System.nanoTime()));
            runtime.obligations().pulse(runtime.budget(BudgetCategory.MAINTENANCE));
            if((server.getTickCount()&15)==0)measure(runtime,BudgetCategory.MAINTENANCE,
                dev.ronova.coremod.nativebridge.NativeBridge::ensureIntegrityWatchdog);
            measure(runtime, BudgetCategory.MAINTENANCE,
                () -> runtime.enforcement().pulse(server.getTickCount()));
            logChallengeIfDue(server,runtime,tick);
            SpawnFirewall.drainConstructed(server, runtime.budget(BudgetCategory.ADMISSION));
            var observationBudget=runtime.budget(BudgetCategory.OBSERVATION);
            // Give final storage entries a bounded first turn too; a permanently
            // stale-first budget order must not expire every staged transaction.
            if((tick&3)==0)runtime.recoverySources().drainStorageCommits(server,observationBudget);
            // Both consumers share the same bounded allowance. A fixed stale-first order
            // can starve task entry/exit capture until its bounded inbox overflows.
            if((tick&1)==0) {
                runtime.recoverySources().maintain(server,observationBudget);
                runtime.observation().maintainStale(observationBudget);
            } else {
                runtime.observation().maintainStale(observationBudget);
                runtime.recoverySources().maintain(server,observationBudget);
            }
            if((tick&3)!=0)runtime.recoverySources().drainStorageCommits(server,observationBudget);
            measure(runtime, BudgetCategory.MAINTENANCE,
                () -> dev.ronova.coremod.network.QuarantineSyncService.pulse(server));
            var terminalBudget=runtime.budget(BudgetCategory.TERMINAL);
            if((tick&1)==0) {
                runtime.terminalPlans().maintainStateFeedback(server,terminalBudget);
            }
            runtime.terminalWatch().maintain(terminalBudget);
            if((tick&1)!=0) {
                runtime.terminalPlans().maintainStateFeedback(server,terminalBudget);
            }
            measure(runtime, BudgetCategory.CAPABILITY,
                runtime.authorityEnforcement()::maintainAnchors);
        } catch (Throwable failure) {
            try {
                synchronized (TICKS) {
                    TickState state = TICKS.get(server);
                    state.markTailCompleted(); state.closeObservationScope();
                }
            } catch (RuntimeException closeFailure) { failure.addSuppressed(closeFailure); }
            endTickAfterFailure(runtime, tick);
            throw failure;
        }
        return true;
    }

    public static void serverTickTail(MinecraftServer server) {
        try {
            serverTickTailInternal(server,true);
        } finally {
            BaselineTelemetry.serverTickTail(server);
        }
    }

    private static void serverTickTailInternal(MinecraftServer server,boolean mixinTail) {
        if (server == null||!server.isSameThread()) {
            return;
        }
        TickState state;
        synchronized (TICKS) {
            state = TICKS.get(server);
            if (state == null || state.logicalTick() < 0L
                || state.tailCompleted() || state.mixinOwned!=mixinTail
                || !mixinTail&&state.rawTick() != server.getTickCount()) {
                return;
            }
            state.markTailCompleted();
        }
        var runtime = ServerRuntimeRegistry.getIfPresent(server);
        if (runtime == null) { state.closeObservationScope(); return; }
        try {
            // A second repair after every level/event tick wins direct field writes
            // regardless of Forge listener registration order.
            measure(runtime, BudgetCategory.CAPABILITY,
                runtime.authorityEnforcement()::maintainAnchors);
        } finally {
            try { runtime.terminalPlans().observeCompletion(server, runtime.budget(BudgetCategory.MAINTENANCE)); }
            finally {
                try { state.closeObservationScope(); }
                finally { runtime.frameBudgetGovernor().endTick(state.logicalTick()); }
            }
        }
    }

    /** Forge-only fallback when the core Mixin is unavailable. */
    public static void serverTick(MinecraftServer server) {
        // The fallback runs at Forge END and cannot provide true tickServer latency.
        // An END listener must not close the Mixin's still-running tick before later listeners and vanilla TAIL.
        if(serverTickHeadInternal(server,false))serverTickTailInternal(server,false);
        BaselineTelemetry.serverPulse(server);
    }

    private static long beginTick(MinecraftServer server,boolean mixinOwned) {
        int rawTick = server.getTickCount();
        synchronized (TICKS) {
            TickState state = TICKS.get(server);
            // Vanilla increments rawTick inside tickServer, between the Mixin HEAD and Forge END.
            if(state!=null&&state.mixinOwned&&!state.tailCompleted()&&!mixinOwned)return -1L;
            if (state != null && state.rawTick() == rawTick) {
                return -1L;
            }
            long logicalTick = state == null ? 0L : state.logicalTick() + 1L;
            TickState next = new TickState(rawTick, logicalTick,mixinOwned);
            ServerRuntimeRegistry.runtime(server).frameBudgetGovernor().beginTick(logicalTick);
            if (state != null) state.closeObservationScope();
            TICKS.put(server, next);
            next.markHeadCompleted();
            return logicalTick;
        }
    }

    private static void endTickAfterFailure(
        dev.ronova.coremod.runtime.DominionServerRuntime runtime, long tick
    ) {
        try {
            runtime.frameBudgetGovernor().endTick(tick);
        } catch (RuntimeException ignored) {
            // Preserve the original gameplay failure while closing telemetry state.
        }
    }


    private static void logChallengeIfDue(MinecraftServer server,DominionServerRuntime runtime,long tick) {
        var challenge=runtime.enforcement().challengeProof();
        String status=challenge.current()?"VERIFIED":challenge.supported()?"DEGRADED":"UNAVAILABLE";
        boolean log;
        synchronized(CHALLENGE_LOGS) {
            ChallengeLogState prior=CHALLENGE_LOGS.get(server);
            log=prior==null||!prior.status().equals(status)||tick-prior.tick()>=600L;
            if(log)CHALLENGE_LOGS.put(server,new ChallengeLogState(status,tick));
        }
        if(log)dev.ronova.coremod.RonovaDominion.LOGGER.info(
            "Ronova code challenge status={} generation={} attempts={} successes={} failures={} read_failures={}",
            status,challenge.generation(),challenge.attempts(),challenge.successes(),challenge.failures(),runtime.enforcement().challengeReadFailures());
    }

    private record ChallengeLogState(String status,long tick) {}

    private static long snapshotEpoch(DominionServerRuntime runtime) {
        var epoch = runtime.projection().snapshot().epoch();
        return epoch.generation() == 0L && epoch.sequence() == 0L
            ? 0L : Math.max(0L, epoch.generation() ^ Long.rotateLeft(epoch.sequence(), 17));
    }

    private static void measure(
        DominionServerRuntime runtime,
        BudgetCategory category,
        Runnable operation
    ) {
        long started = System.nanoTime();
        try {
            operation.run();
        } finally {
            long elapsedUs = Math.max(0L, System.nanoTime() - started) / 1_000L;
            try {
                runtime.recordMeasurement(category, 1L, 1L, 0L, 0L, 0L, elapsedUs);
            } catch (RuntimeException ignored) {
                // Measurement failure must not change the maintenance decision.
            }
        }
    }

    private static final class TickState {
        private final int rawTick;
        private final long logicalTick;
        private final boolean mixinOwned;
        private boolean headCompleted;
        private boolean tailCompleted;
        private dev.ronova.coremod.observation.RecoveryObservationScope observationScope;

        private TickState(int rawTick, long logicalTick,boolean mixinOwned) {
            this.rawTick = rawTick;
            this.logicalTick = logicalTick;
            this.mixinOwned=mixinOwned;
        }

        private int rawTick() { return rawTick; }
        private long logicalTick() { return logicalTick; }
        private boolean headCompleted() { return headCompleted; }
        private boolean tailCompleted() { return tailCompleted; }
        private void markHeadCompleted() { headCompleted = true; }
        private void markTailCompleted() { tailCompleted = true; }
        private void closeObservationScope() {
            if (observationScope != null) { observationScope.close(); observationScope = null; }
        }
    }

    public static long serverPulses() {
        return SERVER_PULSES.get();
    }

    public static long rejectedAdmissions() {
        return SpawnFirewall.rejectedAdmissions();
    }
    /** Stops retaining a failed/aborted tick even when the vanilla TAIL was never reached. */
    public static void release(MinecraftServer server) {
        TickState state;
        synchronized (TICKS) { state = TICKS.remove(server); }
        synchronized (CHALLENGE_LOGS) { CHALLENGE_LOGS.remove(server); }
        if (state != null && state.observationScope != null && state.observationScope.ownedByCurrentThread())
            state.closeObservationScope();
    }
}
