package dev.ronova.coremod.bootstrap;

import dev.ronova.coremod.RonovaDominion;
import dev.ronova.coremod.authority.AuthoritySplitFlags;
import dev.ronova.coremod.budget.BudgetSplitFlags;
import dev.ronova.coremod.budget.FrameBudgetGovernor;
import dev.ronova.coremod.authority.AuthorityView;
import dev.ronova.coremod.capability.AuthorityRuntimeState;
import dev.ronova.coremod.capability.CapabilityRuntime;
import dev.ronova.coremod.persistence.LocalDominionStore;
import dev.ronova.coremod.engine.AuthorityEnforcement;
import dev.ronova.coremod.engine.QuarantineView;
import dev.ronova.coremod.engine.spawn.SpawnAdmissionRuntime;
import dev.ronova.coremod.presence.PresenceSplitFlags;
import dev.ronova.coremod.integrity.EnforcementRuntime;
import dev.ronova.coremod.nativebridge.NativeIntegrityBridge;
import dev.ronova.coremod.observation.LegacyTopologyObservation;
import dev.ronova.coremod.observation.RawObservationFlags;
import dev.ronova.coremod.observation.RawObservationSource;
import dev.ronova.coremod.observation.TopologyObservation;
import dev.ronova.coremod.projection.ProjectionCoordinator;
import dev.ronova.coremod.root.local.LocalDominionRootGateway;
import dev.ronova.coremod.runtime.DominionServerRuntime;
import dev.ronova.coremod.runtime.RootCommandBus;
import dev.ronova.coremod.terminal.DurableTerminalWatchSet;
import dev.ronova.coremod.terminal.TerminalWatchSet;
import net.minecraft.server.MinecraftServer;

/** The sole place that wires concrete Root, projection and capability implementations. */
public final class DominionCompositionRoot {
    private DominionCompositionRoot() {}

    public static DominionServerRuntime create(MinecraftServer server) {
        if (server == null) {
            throw new IllegalArgumentException("server is required");
        }
        // Validate/open durable truth before attaching projections or other process resources.
        LocalDominionStore durableStore = LocalDominionStore.open(server);
        BudgetSplitFlags.Selection budgetSelection = BudgetSplitFlags.resolve();
        FrameBudgetGovernor frameBudgetGovernor = new FrameBudgetGovernor(budgetSelection);
        if (budgetSelection.hasWarning()) {
            RonovaDominion.LOGGER.warn(budgetSelection.warning());
        }
        RonovaDominion.LOGGER.info(
            "Ronova 7.14D frame budget mode={} source={}",
            budgetSelection.enabled() ? "BOUNDED" : "LEGACY",
            budgetSelection.source()
        );
        ProjectionCoordinator projections = ProjectionCoordinator.attach(
            server, frameBudgetGovernor
        );
        AuthoritySplitFlags.Selection authoritySelection = AuthoritySplitFlags.resolve();
        AuthorityView authorityView = AuthorityView.attach(
            server, projections, authoritySelection, frameBudgetGovernor
        );
        if (authoritySelection.hasWarning()) {
            RonovaDominion.LOGGER.warn(authoritySelection.warning());
        }
        RonovaDominion.LOGGER.info(
            "Ronova 7.14B authority mode={} source={}",
            authoritySelection.enabled() ? "SPLIT" : "LEGACY",
            authoritySelection.source()
        );
        PresenceSplitFlags.Selection presenceSelection = QuarantineView.splitSelection();
        if (presenceSelection.hasWarning()) {
            RonovaDominion.LOGGER.warn(presenceSelection.warning());
        }
        RonovaDominion.LOGGER.info(
            "Ronova 7.14C presence mode={} source={}",
            presenceSelection.enabled() ? "SPLIT" : "LEGACY",
            presenceSelection.source()
        );
        LocalDominionRootGateway root = new LocalDominionRootGateway(durableStore);
        RootCommandBus commands = new RootCommandBus(root, projections);
        AuthorityRuntimeState authorityState = new AuthorityRuntimeState(server);
        RawObservationFlags.Selection rawSelection = RawObservationFlags.resolve();
        TopologyObservation observation = rawSelection.enabled()
            ? new RawObservationSource(server) : new LegacyTopologyObservation();
        TerminalWatchSet terminalWatch = new DurableTerminalWatchSet(
            server, () -> projections.published().admission(), authorityView, observation
        );
        if (rawSelection.hasWarning()) {
            RonovaDominion.LOGGER.warn(rawSelection.warning());
        }
        RonovaDominion.LOGGER.info(
            "Ronova 7.14A observation mode={} source={} watch={}",
            rawSelection.enabled() ? "RAW" : "LEGACY",
            rawSelection.source(),
            terminalWatch.metrics().mode()
        );
        CapabilityRuntime capabilities = CapabilityRuntime.standard(
            authorityState, observation
        );
        AuthorityEnforcement authorityEnforcement = new AuthorityEnforcement(
            server, authorityView, authorityState, capabilities
        );
        SpawnAdmissionRuntime spawnAdmission = new SpawnAdmissionRuntime(
            server, () -> projections.published().admission(), authorityView, observation,
            terminalWatch, durableStore::authorityStamp
        );
        EnforcementRuntime enforcement = new EnforcementRuntime(NativeIntegrityBridge.INSTANCE);
        return new DominionServerRuntime(
            commands, projections, authorityView, authoritySelection, authorityState,
            capabilities, authorityEnforcement, spawnAdmission, enforcement, observation,
            terminalWatch, rawSelection, frameBudgetGovernor, durableStore
        );
    }
}
