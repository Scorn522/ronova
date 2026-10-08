package dev.ronova.coremod.engine.spawn;

import dev.ronova.coremod.RonovaDominion;
import dev.ronova.coremod.baseline.BaselineTelemetry;
import dev.ronova.coremod.budget.BudgetReceipt;
import dev.ronova.coremod.budget.FrameBudgetGovernor;
import dev.ronova.coremod.budget.BudgetCategory;
import dev.ronova.coremod.budget.WorkBudget;
import dev.ronova.coremod.adapter.MinecraftSubjectAdapter;
import dev.ronova.coremod.authority.AuthorityView;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.SubjectIdentity;
import dev.ronova.coremod.engine.ExecutionTargetPolicy;
import dev.ronova.coremod.engine.JudgmentState;
import dev.ronova.coremod.engine.QuarantineView;
import dev.ronova.coremod.engine.TerminalLifecycle;
import dev.ronova.coremod.observation.TopologyObservation;
import dev.ronova.coremod.projection.AdmissionProjection;
import dev.ronova.coremod.terminal.NoopTerminalWatchSet;
import dev.ronova.coremod.terminal.TerminalWatchSet;
import dev.ronova.coremod.util.WeakIdentitySet;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Per-server admission state and decisions assembled by the composition root. */
public final class SpawnAdmissionRuntime implements AutoCloseable {
    private static final int CONSTRUCTION_STALE_RECLAIM_LIMIT = 16;
    private volatile boolean closed;

    private final MinecraftServer server;
    private final Supplier<AdmissionProjection> projection;
    private final AuthorityView authorityView;
    private final TopologyObservation observation;
    private final TerminalWatchSet terminalWatch;
    private final java.util.function.Function<UUID, dev.ronova.coremod.root.api.AuthorityStamp> authority;
    private final dev.ronova.coremod.runtime.PhysicalIncarnationTracker incarnations = new dev.ronova.coremod.runtime.PhysicalIncarnationTracker();
    private final dev.ronova.coremod.util.WeakIdentityMap<Entity, AdmissionPass> passes = new dev.ronova.coremod.util.WeakIdentityMap<>();
    private final java.util.concurrent.atomic.AtomicLong staleConstructionWork = new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong heldAdmissionAttempts = new java.util.concurrent.atomic.AtomicLong();
    private final dev.ronova.coremod.budget.HostResourceGovernor resources = new dev.ronova.coremod.budget.HostResourceGovernor();
    private final java.util.concurrent.atomic.AtomicLong constructionBackpressure = new java.util.concurrent.atomic.AtomicLong();
    private volatile java.util.function.BiConsumer<dev.ronova.coremod.root.api.AuthorityStamp, UUID> admissionObserver=(stamp,nonce)->{};
    private volatile java.util.function.BiConsumer<CompoundTag, dev.ronova.coremod.root.api.AuthorityStamp>
        reconstructionObserver = (tag, stamp) -> {};
    private volatile java.util.function.BiPredicate<CompoundTag, dev.ronova.coremod.root.api.AuthorityStamp>
        reconstructionGate = (tag, stamp) -> false;
    private volatile java.util.function.BiPredicate<UUID, UUID> candidateAdmissionGate=(subject,nonce)->false;
    private volatile java.util.function.BiPredicate<UUID, UUID> conflictingPhysicalBinding=(subject,nonce)->false;
    private final WeakIdentitySet<Entity> passed = new WeakIdentitySet<>();
    private final WeakIdentitySet<Entity> rejected = new WeakIdentitySet<>();
    private final WeakIdentitySet<Entity> trackedConstruction = new WeakIdentitySet<>();
    private final WeakIdentitySet<Entity> trackedFactoryResults = new WeakIdentitySet<>();
    private final BoundedRetryQueue<ConstructionWork> constructed =
        new BoundedRetryQueue<>(BoundedRetryQueue.DEFAULT_MAX_ATTEMPTS, BoundedRetryQueue.DEFAULT_DEAD_LETTER_CAPACITY,
            work -> work.reservation().close());

    public SpawnAdmissionRuntime(
        MinecraftServer server,
        Supplier<AdmissionProjection> projection,
        AuthorityView authorityView,
        TopologyObservation observation
    ) {
        this(
            server, projection, authorityView, observation,
            NoopTerminalWatchSet.INSTANCE
        );
    }

    public SpawnAdmissionRuntime(
        MinecraftServer server,
        Supplier<AdmissionProjection> projection,
        AuthorityView authorityView,
        TopologyObservation observation,
        TerminalWatchSet terminalWatch
    ) {
        this(server, projection, authorityView, observation, terminalWatch, ignored -> null);
    }

    public SpawnAdmissionRuntime(MinecraftServer server, Supplier<AdmissionProjection> projection,
        AuthorityView authorityView, TopologyObservation observation, TerminalWatchSet terminalWatch,
        java.util.function.Function<UUID, dev.ronova.coremod.root.api.AuthorityStamp> authority) {
        this.server = Objects.requireNonNull(server, "server");
        this.projection = Objects.requireNonNull(projection, "projection");
        this.authorityView = Objects.requireNonNull(authorityView, "authorityView");
        this.observation = Objects.requireNonNull(observation, "observation");
        this.terminalWatch = Objects.requireNonNull(terminalWatch, "terminalWatch");
        this.authority = Objects.requireNonNull(authority, "authority");
        resources.declareAccounting(dev.ronova.coremod.budget.HostResourceDomain.JAVA_HEAP,
            dev.ronova.coremod.budget.HostResourceGovernor.Accounting.PARTIAL_ESTIMATE, "construction-observation-slot/4096-bytes");
        resources.declareAccounting(dev.ronova.coremod.budget.HostResourceDomain.JAVA_HEAP,
            dev.ronova.coremod.budget.HostResourceGovernor.Accounting.PARTIAL_ESTIMATE, "queued-ingress-slot/4096-bytes");
    }
    public UUID incarnationNonce(Object entity) { return incarnations.nonce(entity); }
    private dev.ronova.coremod.contract.ClassLineageResolver classLineages;
    private synchronized dev.ronova.coremod.contract.ClassLineageResolver classLineages() {
        if(classLineages==null)classLineages=new dev.ronova.coremod.contract.ClassLineageResolver(256,resources);
        return classLineages;
    }
    private boolean currentLineage(dev.ronova.coremod.root.api.SubjectQualification expected) {
        var stamp=authority.apply(expected.subjectKey().subjectId());
        return stamp!=null&&stamp.settled()&&!stamp.owner()&&expected.equals(stamp.qualification());
    }
    public void rememberClassLineage(Entity entity,dev.ronova.coremod.root.api.TerminalOperationPlan plan) {
        if(!belongsTo(entity)||!entity.getUUID().equals(plan.subjectKey().subjectId())
            ||!incarnationNonce(entity).equals(plan.physicalNonce())||!currentLineage(plan.qualification()))return;
        var mark=projection.get().mark(entity.getUUID());
        if(mark!=null)classLineages().remember(plan.qualification(),mark,entity.getClass(),this::currentLineage);
    }
    public boolean matchesVerifiedClassLineage(Entity entity) {
        return classLineages!=null&&belongsTo(entity)&&classLineages.resolve(entity.getClass(),MinecraftSubjectAdapter.from(entity),this::currentLineage)!=null;
    }
    public void clearIncarnations() { incarnations.clear(); if(classLineages!=null)classLineages.clear(); }
    public long staleConstructionWork() { return staleConstructionWork.get(); }
    public long heldAdmissionAttempts() { return heldAdmissionAttempts.get(); }
    public dev.ronova.coremod.budget.HostResourceGovernor resources() { return resources; }
    public long constructionBackpressure() { return constructionBackpressure.get(); }
    public void attachAdmissionObserver(java.util.function.BiConsumer<dev.ronova.coremod.root.api.AuthorityStamp, UUID> observer) {
        admissionObserver=Objects.requireNonNull(observer,"observer");
    }
    public void attachReconstructionObserver(java.util.function.BiConsumer<CompoundTag,
        dev.ronova.coremod.root.api.AuthorityStamp> observer) {
        reconstructionObserver = Objects.requireNonNull(observer, "observer");
    }
    public void attachReconstructionGate(java.util.function.BiPredicate<CompoundTag,
        dev.ronova.coremod.root.api.AuthorityStamp> gate) {
        reconstructionGate = Objects.requireNonNull(gate, "gate");
    }
    public void attachCandidateAdmissionGate(java.util.function.BiPredicate<UUID, UUID> gate) {
        candidateAdmissionGate=Objects.requireNonNull(gate,"gate");
    }
    public void attachPhysicalBindingResolver(java.util.function.BiPredicate<UUID, UUID> resolver) {
        conflictingPhysicalBinding=Objects.requireNonNull(resolver,"resolver");
    }

    /** Construction callbacks stay cheap and defer position-sensitive decisions. */
    public void observeConstruction(Entity entity) {
        observeConstruction(entity, trackedConstruction);
    }
    /** A factory result may carry a different UUID than the Entity base-constructor observation. */
    public void observeFactoryResult(Entity entity) {
        observeConstruction(entity, trackedFactoryResults);
    }
    private synchronized void observeConstruction(Entity entity, WeakIdentitySet<Entity> phase) {
        if (closed) return;
        observation.observe(entity);
        if (!ExecutionTargetPolicy.isCombatObject(entity) || !belongsTo(entity)
            || phase.contains(entity)) {
            return;
        }
        var reserved = resources.reserve(java.util.Map.of(dev.ronova.coremod.budget.HostResourceDomain.JAVA_HEAP, 4096L),
            dev.ronova.coremod.budget.HostResourceGovernor.Priority.NORMAL);
        if (!reserved.granted()) { constructionBackpressure.incrementAndGet(); return; }
        boolean queued = false;
        boolean tracked = false;
        try {
            tracked = phase.addBounded(entity, CONSTRUCTION_STALE_RECLAIM_LIMIT);
            if (!tracked) return;
            UUID observedId = entity.getUUID();
            if (closed) return;
            constructed.addLast(new ConstructionWork(observedId, new WeakReference<>(entity),
                authority.apply(observedId), incarnationNonce(entity), reserved.reservation()));
            queued = true;
        } finally { if (!queued) { if (tracked) phase.remove(entity); reserved.reservation().close(); } }
    }

    public boolean reject(
        ServerLevel level,
        Entity entity,
        boolean finalDecision
    ) {
        return reject(level, entity, finalDecision, false);
    }

    public boolean rejectExecution(ServerLevel level, Entity entity) {
        if (level == null || entity == null || level.getServer() != server) return false;
        var stamp = authority.apply(entity.getUUID());
        AdmissionPass cached = passes.get(entity);
        if (entity.tickCount > 1 && stamp != null && cached != null && !JudgmentState.isJudged(entity)
            && !rejected.contains(entity) && cached.current(stamp, incarnationNonce(entity), projection.get().epoch())) return false;
        return reject(level, entity, true, true);
    }

    private boolean reject(ServerLevel level, Entity entity, boolean finalDecision, boolean executionBoundary) {
        if (level == null || entity == null || level.getServer() != server) {
            return false;
        }
        long baselineStarted = BaselineTelemetry.admissionStarted(server);
        boolean rejectedResult = false;
        try {
            observation.observe(entity);
            boolean combatObject = ExecutionTargetPolicy.isCombatObject(entity);
            boolean protectedSubject = combatObject && authorityView.isProtected(entity);
            var stamp = authority.apply(entity.getUUID());
            if (stamp != null) protectedSubject = combatObject && entity instanceof net.minecraft.world.entity.player.Player && stamp.owner();
            var qualified = stamp == null ? null : stamp.qualification();
            UUID nonce = incarnationNonce(entity);
            AdmissionProjection view = projection.get();
            if (combatObject && !protectedSubject && (candidateAdmissionGate.test(entity.getUUID(),nonce)
                || conflictingPhysicalBinding.test(entity.getUUID(),nonce)
                || stamp != null && (!stamp.settled() || view.epoch().compareTo(stamp.authorityEpoch()) < 0)
                || qualified != null && !qualified.terminal() && (JudgmentState.isJudged(entity) || rejected.contains(entity)
                    || qualified.revisions() != null && (qualified.revisions().activeIncarnation() == null
                        || !qualified.revisions().activeIncarnation().nonce().equals(nonce))))) {
                passed.remove(entity); passes.remove(entity); heldAdmissionAttempts.incrementAndGet();
                try { admissionObserver.accept(stamp,nonce); }
                catch(RuntimeException unavailable) { /* Observation cannot weaken the independent admission gate. */ }
                // A stale/unbound ACTIVE incarnation is held, never converted into TERMINAL or killed.
                // Retain raw registration/NBT so a held incarnation can be rebound instead of disappearing at save.
                // It receives no pass; every normal tick remains gated until current Root qualification is restored.
                rejectedResult = executionBoundary; return executionBoundary;
            }
            boolean alreadyTerminal = combatObject
                && (qualified != null && qualified.terminal() || stamp==null && (rejected.contains(entity) || JudgmentState.isJudged(entity)));
            AdmissionPass cached = passes.get(entity);
            boolean passedFinalBoundary = finalDecision && passed.contains(entity)
                && (stamp == null || cached != null && cached.current(stamp, nonce, view.epoch()));
            DominionDecision.Admission decision = view.decide(
                MinecraftSubjectAdapter.from(entity), combatObject, protectedSubject,
                alreadyTerminal, passedFinalBoundary, finalDecision
            );
            if (combatObject&&!protectedSubject&&!alreadyTerminal&&matchesVerifiedClassLineage(entity)) {
                rejectObject(entity); rejectedResult=true; return true;
            }
            if (!decision.admitted()) {
                rejectObject(entity);
                rejectedResult = true;
                return true;
            }
            if (finalDecision && combatObject && !protectedSubject) {
                passed.add(entity);
                if (stamp != null) passes.put(entity, new AdmissionPass(stamp, nonce, view.epoch()));
            }
            return false;
        } finally {
            BaselineTelemetry.recordAdmission(
                server, false, rejectedResult, baselineStarted
            );
        }
    }

    /** Rejects serialized spawns before factories can create an entity object. */
    public boolean rejectNbt(Level level, CompoundTag tag) {
        if (!(level instanceof ServerLevel serverLevel)
            || serverLevel.getServer() != server || tag == null) {
            return false;
        }
        long baselineStarted = BaselineTelemetry.admissionStarted(server);
        boolean rejectedResult = false;
        try {
            String type = tag.getString("id");
            if (type.isBlank() || isWorldResultType(type)) {
                return false;
            }
            UUID uuid = tag.hasUUID("UUID") ? tag.getUUID("UUID") : null;
            double x = 0.0D;
            double y = 0.0D;
            double z = 0.0D;
            ListTag position = tag.getList("Pos", Tag.TAG_DOUBLE);
            if (position.size() >= 3) {
                x = position.getDouble(0);
                y = position.getDouble(1);
                z = position.getDouble(2);
            }
            SubjectIdentity subject = MinecraftSubjectAdapter.fromSerialized(
                uuid, type, null, serverLevel.dimension().location().toString(), x, y, z
            );
            AdmissionProjection current = projection.get();
            var stamp = uuid == null ? null : authority.apply(uuid);
            // This outlet may reject a changed UUID only through a freshly proved, Root-qualified source.
            // It does not create a Judgment for the proposed UUID or write its NBT.
            boolean sourceHeld = false;
            try { sourceHeld = reconstructionGate.test(tag, stamp); } catch (RuntimeException unavailable) { }
            if (sourceHeld) {
                current.recordNbtRejection(); SpawnAdmissionMetrics.recordNbtRejection(); rejectedResult = true; return true;
            }
            if (stamp != null && (!stamp.settled() || current.epoch().compareTo(stamp.authorityEpoch()) < 0
                && (stamp.qualification() == null || !stamp.qualification().terminal()))) {
                // Preserve raw NBT while Root recovery/publication is pending; object tick gates remain closed.
                return false;
            }
            if (stamp != null && !stamp.owner() && stamp.qualification() != null && stamp.qualification().terminal()) {
                // Observation cannot turn an already justified rejection into admission.
                try { reconstructionObserver.accept(tag, stamp); } catch (RuntimeException unavailable) { }
                current.recordNbtRejection(); SpawnAdmissionMetrics.recordNbtRejection(); rejectedResult = true; return true;
            }
            DominionDecision.Admission decision = current.decide(
                subject, true, false, false, false, false
            );
            if (decision.admitted()) {
                return false;
            }
            current.recordNbtRejection();
            SpawnAdmissionMetrics.recordNbtRejection();
            rejectedResult = true;
            return true;
        } catch (Throwable ignored) {
            // Object-level admission gates remain independent.
            return false;
        } finally {
            BaselineTelemetry.recordAdmission(
                server, true, rejectedResult, baselineStarted
            );
        }
    }

    public boolean matchesTombstone(Entity entity) {
        return ExecutionTargetPolicy.isCombatObject(entity)
            && !authorityView.isProtected(entity)
            && belongsTo(entity)
            && projection.get().matches(MinecraftSubjectAdapter.from(entity));
    }

    public void beginExecution(Entity entity) {
        passed.remove(entity);
        passes.remove(entity);
    }

    public void commitExecution(Entity entity) {
        rejected.add(entity);
    }
    public boolean plannedFencePresent(Entity entity) { return rejected.contains(entity) && !passed.contains(entity); }

    public void invalidatePasses() {
        passed.clear();
        passes.clear();
    }

    public void reassert(Entity entity) {
        if (entity != null && belongsTo(entity)
            && !authorityView.isProtected(entity)) {
            TerminalLifecycle.completeCommitted(entity, authorityView);
        }
    }

    /** Compatibility drain retaining the pre-14D ungated scheduling path. */
    public void drainConstructed() {
        long deadLettersBefore = constructed.deadLetteredTotal();
        constructed.drain(this::processConstructed);
        reportNewDeadLetters(deadLettersBefore);
    }

    /** Bounded 7.14D construction/admission drain. */
    public BudgetReceipt drainConstructed(WorkBudget budget) {
        if (budget == null) {
            throw new IllegalArgumentException("budget is required");
        }
        long deadLettersBefore = constructed.deadLetteredTotal();
        BudgetReceipt receipt = constructed.drain(budget, this::processConstructed);
        reportNewDeadLetters(deadLettersBefore);
        return receipt;
    }

    private void reportNewDeadLetters(long previousTotal) {
        long currentTotal = constructed.deadLetteredTotal();
        if (currentTotal <= previousTotal) {
            return;
        }
        List<BoundedRetryQueue.DeadLetter<ConstructionWork>> diagnostics =
            constructed.deadLetters();
        BoundedRetryQueue.DeadLetter<ConstructionWork> latest =
            diagnostics.isEmpty() ? null : diagnostics.get(diagnostics.size() - 1);
        RonovaDominion.LOGGER.warn(
            "RONOVA_ADMISSION_DEAD_LETTER runtime={} new={} retained={} total={} "
                + "attempts={} entity_uuid={}",
            Integer.toUnsignedString(System.identityHashCode(server)),
            currentTotal - previousTotal,
            diagnostics.size(),
            currentTotal,
            latest == null ? constructed.maxAttempts() : latest.attempts(),
            latest == null ? "unresolved" : latest.value().uuid()
        );
    }

    private boolean processConstructed(ConstructionWork work) {
        Entity entity = work == null ? null : work.reference().get();
        if (entity == null) {
            return true;
        }
        if (!work.uuid().equals(entity.getUUID()) || !work.physicalNonce().equals(incarnationNonce(entity))
            || (work.stamp() == null ? authority.apply(work.uuid()) != null : !work.stamp().sameAuthority(authority.apply(work.uuid())))) {
            staleConstructionWork.incrementAndGet(); return true;
        }
        try {
            if (entity.level() instanceof ServerLevel level && level.getServer() == server) {
                reject(level, entity, false);
            }
            return true;
        } catch (VirtualMachineError | ThreadDeath fatal) {
            throw fatal;
        } catch (Throwable ignored) {
            // Level, manager and first-tick gates remain independent.
            return false;
        }
    }

    public long rejectedAdmissions() {
        return projection.get().rejectedAdmissions();
    }

    public long nbtRejections() {
        return projection.get().nbtRejections();
    }

    public int passedObjects() {
        return passed.size();
    }

    public int rejectedObjects() {
        return rejected.size();
    }

    /** Active construction work that can still be retried. */
    public int pendingConstructionRetries() {
        return constructed.size();
    }

    /** Currently retained bounded dead-letter diagnostics. */
    public int quarantinedConstructionFailures() {
        return constructed.deadLetterCount();
    }

    /** Process-lifetime count for this server runtime, including evicted diagnostics. */
    public long deadLetteredConstructionFailures() {
        return constructed.deadLetteredTotal();
    }

    public int constructionRetryLimit() {
        return constructed.maxAttempts();
    }

    public UUID latestQuarantinedConstructionUuid() {
        List<BoundedRetryQueue.DeadLetter<ConstructionWork>> diagnostics =
            constructed.deadLetters();
        return diagnostics.isEmpty() ? null
            : diagnostics.get(diagnostics.size() - 1).value().uuid();
    }

    public void clearIdentity(UUID uuid) {
        if (uuid == null) {
            return;
        }
        if(classLineages!=null)classLineages.forget(uuid);
        passed.removeIf(entity -> uuid.equals(entity.getUUID()));
        passes.removeIf(entity -> uuid.equals(entity.getUUID()));
        rejected.removeIf(entity -> uuid.equals(entity.getUUID()));
        trackedConstruction.removeIf(entity -> uuid.equals(entity.getUUID()));
        trackedFactoryResults.removeIf(entity -> uuid.equals(entity.getUUID()));
        constructed.removeIf(work -> uuid.equals(work.uuid()));
        terminalWatch.clearIdentity(uuid);
    }

    public void clearCaches() {
        passed.clear();
        passes.clear();
        rejected.clear();
    }

    public void clear() {
        Throwable failed = null;
        for (Runnable cleanup : java.util.List.<Runnable>of(this::clearCaches, trackedConstruction::clear,
            trackedFactoryResults::clear, constructed::clear, this::clearIncarnations)) {
            try { cleanup.run(); }
            catch (RuntimeException | Error error) {
                if (failed == null) failed = error; else if (failed != error) failed.addSuppressed(error);
            }
        }
        if (failed instanceof RuntimeException error) throw error;
        if (failed instanceof Error error) throw error;
    }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        clear();
    }

    private void rejectObject(Entity entity) {
        passed.remove(entity);
        passes.remove(entity);
        if (rejected.add(entity)) {
            SpawnAdmissionMetrics.recordObjectRejection();
            projection.get().recordObjectRejection();
        }
        QuarantineView.register(server, entity);
        TerminalLifecycle.completeCommitted(entity, authorityView);
        terminalWatch.observeIncarnation(entity);
    }

    private boolean belongsTo(Entity entity) {
        try {
            return entity != null && entity.level() instanceof ServerLevel level
                && level.getServer() == server;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isWorldResultType(String type) {
        return "minecraft:item".equals(type) || "minecraft:experience_orb".equals(type);
    }

    private record ConstructionWork(UUID uuid, WeakReference<Entity> reference,
        dev.ronova.coremod.root.api.AuthorityStamp stamp, UUID physicalNonce,
        dev.ronova.coremod.budget.HostResourceGovernor.Reservation reservation) {
        private ConstructionWork {
            Objects.requireNonNull(uuid, "uuid");
            Objects.requireNonNull(reference, "reference");
            Objects.requireNonNull(physicalNonce, "physicalNonce");
            Objects.requireNonNull(reservation, "reservation");
        }
    }
}
