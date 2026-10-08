package dev.ronova.coremod.engine;

import dev.ronova.coremod.budget.*;
import dev.ronova.coremod.contract.SubjectKey;
import dev.ronova.coremod.root.api.TerminalOperationPlan;
import dev.ronova.coremod.runtime.CompletionTracker;
import dev.ronova.coremod.runtime.DominionServerRuntime;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Bounded, expiring completion observations. Eviction removes evidence, never an obligation or Root fact. */
public final class TerminalCompletionMonitor {
    private static final int LIMIT = 32;
    private final DominionServerRuntime runtime;
    private final LinkedHashMap<SubjectKey, Entry> entries = new LinkedHashMap<>();
    private final ArrayDeque<SubjectKey> rotation = new ArrayDeque<>();
    private static final class Entry {
        final CompletionTracker tracker = new CompletionTracker();
        final HostResourceGovernor.Reservation reservation;
        WeakReference<Entity> entity;
        TerminalOperationPlan plan;
        Entry(Entity entity, TerminalOperationPlan plan, HostResourceGovernor.Reservation reservation) {
            this.entity = new WeakReference<>(entity); this.plan = plan; this.reservation = reservation;
        }
    }
    public TerminalCompletionMonitor(DominionServerRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime);
        runtime.resources().declareAccounting(HostResourceDomain.JAVA_HEAP, HostResourceGovernor.Accounting.PARTIAL_ESTIMATE,
            "completion-observation/4096-bytes");
    }
    public synchronized void track(Entity entity, TerminalOperationPlan plan) {
        if(entity==null||plan==null||runtime.authorityState().shuttingDown()||runtime.durableStore()==null
            ||!runtime.durableStore().validateTerminalPlan(plan)||!runtime.incarnationNonce(entity).equals(plan.physicalNonce()))return;
        Entry existing = entries.get(plan.subjectKey());
        if (existing != null) {
            boolean changed=existing.entity.get()!=entity||!CompletionTracker.Scope.of(existing.plan).equals(CompletionTracker.Scope.of(plan));
            existing.entity = new WeakReference<>(entity); existing.plan = plan;
            if(changed)existing.tracker.sample(CompletionTracker.Scope.of(plan),tick(runtime.authorityState().server()),Map.of(),
                true,runtime.durableStore().terminalEffectResolved(plan));
            return;
        }
        if (entries.size() >= LIMIT) {
            SubjectKey oldest = entries.keySet().iterator().next();
            var evicted=entries.remove(oldest);rotation.remove(oldest);evicted.reservation.close();
        }
        var reserve = runtime.resources().reserve(Map.of(HostResourceDomain.JAVA_HEAP, 4096L), HostResourceGovernor.Priority.RECOVERY);
        if (!reserve.granted()) return;
        entries.put(plan.subjectKey(), new Entry(entity, plan, reserve.reservation())); rotation.addLast(plan.subjectKey());
    }
    public synchronized void pulse(MinecraftServer server, WorkBudget budget) {
        if(!server.isSameThread()||runtime.authorityState().shuttingDown())return;
        int processed = 0, count = Math.min(8, rotation.size());
        try {
            for (int i = 0; i < count && budget.tryConsumeCount(1); i++) {
                var key = rotation.removeFirst(); rotation.addLast(key); processed++;
                Entry entry = entries.get(key);
                if(entry==null){rotation.remove(key);continue;}
                try {
                    if(entry.entity.get()==null){entries.remove(key);rotation.remove(key);entry.reservation.close();continue;}
                    sample(entry, server);
                }
                catch (RuntimeException | LinkageError unavailable) {
                    entry.tracker.sample(CompletionTracker.Scope.of(entry.plan), tick(server), Map.of(), false, false);
                }
            }
        } finally { if (processed > 0) budget.finish(processed, 0); }
    }
    private void sample(Entry entry, MinecraftServer server) {
        var plan = entry.plan; var store = runtime.durableStore(); var entity = entry.entity.get();
        var current = store.approveTerminalPlan(plan.subjectKey().subjectId(), plan.physicalNonce());
        if(sameOperation(current,plan)) {entry.plan=current;plan=current;}
        boolean authority = current != null && CompletionTracker.Scope.of(current).equals(CompletionTracker.Scope.of(plan));
        var evidence = new EnumMap<CompletionTracker.Condition, CompletionTracker.Evidence>(CompletionTracker.Condition.class);
        // The sealed observer alone does not prove the Minecraft accessors it reads.
        // Require Entity/LivingEntity/SynchedData/DataItem and the health-key getter too.
        if (authority && entity!=null && runtime.incarnationNonce(entity).equals(plan.physicalNonce())) {
            try(var observation=runtime.enforcement().beginObservation(0x8063)) {
                if(observation!=null&&dev.ronova.coremod.integrity.RawAccessorDispatch.matches(entity,
                    net.minecraft.world.entity.Entity.class,dev.ronova.coremod.mixin.EntityStateAccessor.class)) {
                    var state=(dev.ronova.coremod.mixin.EntityStateAccessor)entity;
                    var rawLevel=state.ronova$getLevel();
                    boolean bound=rawLevel instanceof ServerLevel level&&level.getServer()==server
                        &&server.getLevel(level.dimension())==level&&plan.subjectKey().subjectId().equals(state.ronova$getUuid());
                    boolean present=TerminalLifecycle.controlledStatePresent(entity);
                    if(bound&&state.ronova$getLevel()==rawLevel&&plan.subjectKey().subjectId().equals(state.ronova$getUuid())
                        &&observation.finish())evidence.put(CompletionTracker.Condition.BODY_DISABLED, proof(present,
                        plan.authorityCommitIndex(), "registered-terminal-fields/v1"));
                }
            }
        }
        // These facts prove only their explicitly registered scope. In particular a
        // local quarantine registry does not prove arbitrary server BossBar/tracking closure.
        if (authority && !store.persistencePending()) {
            evidence.put(CompletionTracker.Condition.SAVE_QUALIFICATION,
                proof(true, plan.authorityCommitIndex(), "acknowledged-root-qualification/v1"));
        }
        var obligations = runtime.obligations().registry();
        if (authority && !obligations.pending(plan.qualification().targetEpoch())
            && !store.obligationCheckpoint().pending(plan.qualification().targetEpoch())
            && !runtime.terminalPlans().indices().pending(plan)
            && !runtime.recoverySources().pending(plan.qualification().targetEpoch()))
            evidence.put(CompletionTracker.Condition.NO_PENDING_WORK, proof(true, plan.authorityCommitIndex(), "acknowledged-and-live-obligations/v2"));
        entry.tracker.sample(CompletionTracker.Scope.of(plan), tick(server), evidence, authority,
            authority && store.terminalEffectResolved(current));
    }
    private static CompletionTracker.Evidence proof(boolean verified, long revision, String ref) {
        return new CompletionTracker.Evidence(verified ? CompletionTracker.State.VERIFIED : CompletionTracker.State.FAILED, revision, ref);
    }
    public synchronized CompletionTracker.Evaluation current(TerminalOperationPlan plan, MinecraftServer server) {
        Entry entry = entries.get(plan.subjectKey());
        if (entry == null) return null;
        var approved = runtime.durableStore().approveTerminalPlan(plan.subjectKey().subjectId(), plan.physicalNonce());
        if(sameOperation(approved,entry.plan)&&!CompletionTracker.Scope.of(approved).equals(CompletionTracker.Scope.of(entry.plan))) {
            entry.plan=approved;
            // A policy refresh needs new evidence; never return the old satisfied certificate.
            return entry.tracker.sample(CompletionTracker.Scope.of(approved),tick(server),Map.of(),true,
                runtime.durableStore().terminalEffectResolved(approved));
        }
        if (approved == null || !CompletionTracker.Scope.of(approved).equals(CompletionTracker.Scope.of(entry.plan)))
            return entry.tracker.sample(CompletionTracker.Scope.of(entry.plan), tick(server), Map.of(), false, false);
        return entry.tracker.current(tick(server));
    }
    private static boolean sameOperation(TerminalOperationPlan current,TerminalOperationPlan old) {
        return current!=null&&current.commandId().equals(old.commandId())&&current.authorityHash().equals(old.authorityHash())
            &&current.qualification().equals(old.qualification())&&current.physicalNonce().equals(old.physicalNonce());
    }
    public synchronized void clear() {
        var remaining=List.copyOf(entries.values());entries.clear();rotation.clear();Throwable failed=null;
        for(var entry:remaining)try {entry.reservation.close();}
        catch(RuntimeException|Error failure){if(failed==null)failed=failure;else if(failed!=failure)failed.addSuppressed(failure);}
        if(failed instanceof RuntimeException error)throw error;
        if(failed instanceof Error error)throw error;
    }
    private static long tick(MinecraftServer server) { return Integer.toUnsignedLong(server.getTickCount()); }
}
