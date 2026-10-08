package dev.ronova.coremod.runtime;

import dev.ronova.coremod.authority.AuthoritySplitFlags;
import dev.ronova.coremod.authority.AuthorityView;
import dev.ronova.coremod.budget.BudgetCategory;
import dev.ronova.coremod.budget.BudgetReceipt;
import dev.ronova.coremod.budget.BudgetTelemetrySnapshot;
import dev.ronova.coremod.budget.FrameBudgetGovernor;
import dev.ronova.coremod.budget.WorkBudget;
import dev.ronova.coremod.capability.CapabilityRuntime;
import dev.ronova.coremod.capability.AuthorityRuntimeState;
import dev.ronova.coremod.engine.AuthorityEnforcement;
import dev.ronova.coremod.engine.spawn.SpawnAdmissionRuntime;
import dev.ronova.coremod.integrity.EnforcementRuntime;
import dev.ronova.coremod.observation.RawObservationFlags;
import dev.ronova.coremod.observation.TopologyObservation;
import dev.ronova.coremod.persistence.LocalDominionStore;
import dev.ronova.coremod.policy.StrengthPolicySnapshot;
import dev.ronova.coremod.projection.ProjectionBundle;
import dev.ronova.coremod.projection.ProjectionCoordinator;
import dev.ronova.coremod.terminal.TerminalWatchSet;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.world.entity.Entity;

/** Explicit per-server object graph assembled by the composition root. */
public final class DominionServerRuntime {
    private final dev.ronova.coremod.event.RuntimeEventLedger eventLedger=new dev.ronova.coremod.event.RuntimeEventLedger();
    public dev.ronova.coremod.event.RuntimeEventLedger eventLedger(){return eventLedger;}
    private final RootCommandBus commands;
    private final ProjectionCoordinator projections;
    private final AuthorityView authorityView;
    private final AuthoritySplitFlags.Selection authoritySplitSelection;
    private final AuthorityRuntimeState authorityState;
    private final CapabilityRuntime capabilities;
    private final AuthorityEnforcement authorityEnforcement;
    private final SpawnAdmissionRuntime spawnAdmission;
    private final EnforcementRuntime enforcement;
    private final TopologyObservation observation;
    private final TerminalWatchSet terminalWatch;
    private final RawObservationFlags.Selection rawObservationSelection;
    private final FrameBudgetGovernor frameBudgetGovernor;
    private final LocalDominionStore durableStore;
    private final dev.ronova.coremod.surface.RuntimeDiagnostics diagnostics;
    private final dev.ronova.coremod.engine.TerminalPlanExecutor terminalPlans;
    private final dev.ronova.coremod.obligation.ObligationCoordinator obligations;
    private final dev.ronova.coremod.observation.RecoverySourceTracker recoverySources;
    private final IngressWorkRegistry<net.minecraft.network.Connection> ingressWork;
    private final AtomicLong passiveWatchNotificationFailures = new AtomicLong();

    public DominionServerRuntime(
        RootCommandBus commands,
        ProjectionCoordinator projections,
        AuthorityView authorityView,
        AuthoritySplitFlags.Selection authoritySplitSelection,
        AuthorityRuntimeState authorityState,
        CapabilityRuntime capabilities,
        AuthorityEnforcement authorityEnforcement,
        SpawnAdmissionRuntime spawnAdmission,
        EnforcementRuntime enforcement,
        TopologyObservation observation,
        TerminalWatchSet terminalWatch,
        RawObservationFlags.Selection rawObservationSelection
    ) {
        this(
            commands, projections, authorityView, authoritySplitSelection, authorityState,
            capabilities, authorityEnforcement, spawnAdmission, enforcement, observation,
            terminalWatch, rawObservationSelection,
            projections == null || projections.frameBudgetGovernor() == null
                ? new FrameBudgetGovernor() : projections.frameBudgetGovernor(), null
        );
    }

    public DominionServerRuntime(
        RootCommandBus commands,
        ProjectionCoordinator projections,
        AuthorityView authorityView,
        AuthoritySplitFlags.Selection authoritySplitSelection,
        AuthorityRuntimeState authorityState,
        CapabilityRuntime capabilities,
        AuthorityEnforcement authorityEnforcement,
        SpawnAdmissionRuntime spawnAdmission,
        EnforcementRuntime enforcement,
        TopologyObservation observation,
        TerminalWatchSet terminalWatch,
        RawObservationFlags.Selection rawObservationSelection,
        FrameBudgetGovernor frameBudgetGovernor
    ) {
        this(
            commands, projections, authorityView, authoritySplitSelection, authorityState,
            capabilities, authorityEnforcement, spawnAdmission, enforcement, observation,
            terminalWatch, rawObservationSelection, frameBudgetGovernor, null
        );
    }

    public DominionServerRuntime(
        RootCommandBus commands,
        ProjectionCoordinator projections,
        AuthorityView authorityView,
        AuthoritySplitFlags.Selection authoritySplitSelection,
        AuthorityRuntimeState authorityState,
        CapabilityRuntime capabilities,
        AuthorityEnforcement authorityEnforcement,
        SpawnAdmissionRuntime spawnAdmission,
        EnforcementRuntime enforcement,
        TopologyObservation observation,
        TerminalWatchSet terminalWatch,
        RawObservationFlags.Selection rawObservationSelection,
        FrameBudgetGovernor frameBudgetGovernor,
        LocalDominionStore durableStore
    ) {
        this.commands = Objects.requireNonNull(commands, "commands");
        this.projections = Objects.requireNonNull(projections, "projections");
        this.authorityView = Objects.requireNonNull(authorityView, "authorityView");
        this.authoritySplitSelection = Objects.requireNonNull(
            authoritySplitSelection, "authoritySplitSelection"
        );
        this.authorityState = Objects.requireNonNull(authorityState, "authorityState");
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.authorityEnforcement = Objects.requireNonNull(
            authorityEnforcement, "authorityEnforcement"
        );
        this.spawnAdmission = Objects.requireNonNull(spawnAdmission, "spawnAdmission");
        this.enforcement = Objects.requireNonNull(enforcement, "enforcement");
        this.observation = Objects.requireNonNull(observation, "observation");
        this.terminalWatch = Objects.requireNonNull(terminalWatch, "terminalWatch");
        this.rawObservationSelection = Objects.requireNonNull(
            rawObservationSelection, "rawObservationSelection"
        );
        this.frameBudgetGovernor = Objects.requireNonNull(
            frameBudgetGovernor, "frameBudgetGovernor"
        );
        this.durableStore = durableStore;
        // A completed work item retained by a transport future must not retain this runtime through its allocator.
        var ingressResources=resources();
        ingressResources.declareAccounting(dev.ronova.coremod.budget.HostResourceDomain.JAVA_HEAP,
            dev.ronova.coremod.budget.HostResourceGovernor.Accounting.PARTIAL_ESTIMATE,"owned-ingress/256-per-connection+4096-per-callback");
        this.ingressWork=new IngressWorkRegistry<>(1024,256,bytes->{
            var requested=ingressResources.reserve(java.util.Map.of(dev.ronova.coremod.budget.HostResourceDomain.JAVA_HEAP,(long)bytes),
                dev.ronova.coremod.budget.HostResourceGovernor.Priority.NORMAL);
            return requested.granted()?requested.reservation()::close:null;
        });
        if(durableStore!=null)durableStore.shadow().attachRuntimeResources(resources());
        this.diagnostics = new dev.ronova.coremod.surface.RuntimeDiagnostics(this);
        this.terminalPlans = new dev.ronova.coremod.engine.TerminalPlanExecutor(this);
        this.obligations = new dev.ronova.coremod.obligation.ObligationCoordinator(this);
        this.recoverySources = new dev.ronova.coremod.observation.RecoverySourceTracker(this);
        spawnAdmission.attachAdmissionObserver(obligations::admission);
        spawnAdmission.attachReconstructionGate(recoverySources::reconstructing);
        if (durableStore != null) spawnAdmission.attachCandidateAdmissionGate(durableStore::candidateAdmissionHeld);
        if (durableStore != null) spawnAdmission.attachPhysicalBindingResolver(durableStore::conflictingPhysicalBinding);
        commands.initialize();
    }

    public RootCommandBus commands() { return commands; }
    public UUID incarnationNonce(Object entity) { return spawnAdmission.incarnationNonce(entity); }
    public void clearIncarnations() { spawnAdmission.clearIncarnations(); }
    public dev.ronova.coremod.budget.HostResourceGovernor resources() { return spawnAdmission.resources(); }
    public dev.ronova.coremod.surface.RuntimeDiagnostics diagnostics() { return diagnostics; }
    public dev.ronova.coremod.engine.TerminalPlanExecutor terminalPlans() { return terminalPlans; }
    public dev.ronova.coremod.obligation.ObligationCoordinator obligations() { return obligations; }
    public dev.ronova.coremod.observation.RecoverySourceTracker recoverySources() { return recoverySources; }
    public IngressWorkRegistry<net.minecraft.network.Connection> ingressWork() { return ingressWork; }

    /** Pure Authority query view for this server's current projection. */
    public AuthorityView authorityView() { return authorityView; }

    public boolean authoritySplitEnabled() { return authoritySplitSelection.enabled(); }

    public AuthoritySplitFlags.Source authoritySplitFlagSource() {
        return authoritySplitSelection.source();
    }

    public String authoritySplitMode() {
        return authoritySplitEnabled() ? "SPLIT" : "LEGACY";
    }

    public ProjectionBundle projection() { return projections.published(); }

    public ProjectionCoordinator projectionCoordinator() { return projections; }

    public AuthorityRuntimeState authorityState() { return authorityState; }

    public CapabilityRuntime capabilities() { return capabilities; }

    public AuthorityEnforcement authorityEnforcement() { return authorityEnforcement; }

    public SpawnAdmissionRuntime spawnAdmission() { return spawnAdmission; }

    public EnforcementRuntime enforcement() { return enforcement; }

    public TopologyObservation observation() { return observation; }

    public TerminalWatchSet terminalWatch() { return terminalWatch; }

    public FrameBudgetGovernor frameBudgetGovernor() { return frameBudgetGovernor; }

    /** The authoritative SavedData adapter used by the composed server runtime. */
    public LocalDominionStore durableStore() { return durableStore; }

    /**
     * Coherent configured/active strength view. A configured change remains
     * PENDING_RESTART until the durable Root is loaded by the next JVM.
     */
    public StrengthPolicySnapshot strengthPolicy() {
        return durableStore == null
            ? StrengthPolicySnapshot.defaults()
            : dev.ronova.coremod.policy.RuntimeCapabilityProjection.apply(durableStore.strengthPolicySnapshot(),enforcement.published(),enforcement.codeIntegrity(),enforcement.challengeProof());
    }

    /** Claims once-only lifecycle evidence without making the runtime a second writer. */
    public boolean claimLifecycleIntegration(UUID judgmentId, String physicalIdentity) {
        return durableStore != null
            && durableStore.claimLifecycleIntegration(judgmentId, physicalIdentity) != null;
    }

    /** Executes outside the Root monitor after durable intent and the one-attempt entry check. */
    public boolean integrateLifecycleOnce(UUID subjectId, String physicalIdentity, Runnable body) {
        if (durableStore == null || body == null) return false;
        dev.ronova.coremod.root.api.EffectClaim claim = durableStore.prepareLifecycleEffect(subjectId, physicalIdentity);
        if (claim.status() == dev.ronova.coremod.root.api.EffectClaim.Status.LEGACY_UNQUALIFIED) {
            if (!claimLifecycleIntegration(subjectId, physicalIdentity)) return false;
            body.run();
            return true;
        }
        if (!claim.granted()) return false;
        try {
            if (!durableStore.enterEffect(claim)) return false;
            body.run();
            return true;
        } finally {
            durableStore.finishEffectUnknown(claim);
        }
    }

    public WorkBudget budget(BudgetCategory category) {
        return frameBudgetGovernor.budget(category);
    }

    public BudgetTelemetrySnapshot budgetTelemetry() {
        return frameBudgetGovernor.snapshot();
    }

    public BudgetReceipt recordMeasurement(
        BudgetCategory category,
        long attempted,
        long processed,
        long deferred,
        long carryIn,
        long carryOut,
        long elapsedUs
    ) {
        return frameBudgetGovernor.recordMeasurement(
            category, attempted, processed, deferred, carryIn, carryOut, elapsedUs
        );
    }

    public boolean rawObservationEnabled() { return rawObservationSelection.enabled(); }

    public RawObservationFlags.Source rawObservationFlagSource() {
        return rawObservationSelection.source();
    }

    public String rawObservationMode() {
        return rawObservationEnabled() ? "RAW" : "LEGACY";
    }

    /**
     * Historical compatibility seam. Since 7.16 terminal state never erases
     * raw observation knowledge; the durable watch needs rediscovery.
     */
    @Deprecated(forRemoval = false)
    public void legacyForgetOnTerminal(Entity entity) {
        // Deliberate no-op.
    }

    public void recordPassiveWatchNotificationFailure() {
        passiveWatchNotificationFailures.incrementAndGet();
    }

    public long passiveWatchNotificationFailures() {
        return passiveWatchNotificationFailures.get();
    }

    public boolean reconcileNative() {
        boolean recoveredRootPublication = commands.reconcilePendingPublication();
        if (commands.hasPendingPublication()) {
            return recoveredRootPublication;
        }
        return projections.reconcileNative() || recoveredRootPublication;
    }
}
