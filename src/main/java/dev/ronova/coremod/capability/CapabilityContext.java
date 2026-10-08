package dev.ronova.coremod.capability;

import dev.ronova.coremod.contract.Capability;
import dev.ronova.coremod.observation.TopologyObservation;
import java.util.Objects;
import net.minecraft.world.entity.player.Player;

/** Per-module execution context with counted best-effort compatibility operations. */
public final class CapabilityContext {
    private final Player player;
    private final Capability capability;
    private final AuthorityRuntimeState authorityState;
    private final TopologyObservation observation;
    private final CapabilityDiagnostics diagnostics;
    private final boolean topologyBroken;

    CapabilityContext(
        Player player,
        Capability capability,
        AuthorityRuntimeState authorityState,
        TopologyObservation observation,
        CapabilityDiagnostics diagnostics,
        boolean topologyBroken
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.capability = Objects.requireNonNull(capability, "capability");
        this.authorityState = authorityState;
        this.observation = observation;
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        this.topologyBroken = topologyBroken;
    }

    public Player player() { return player; }

    public AuthorityRuntimeState authorityState() { return authorityState; }

    public TopologyObservation observation() { return observation; }

    public boolean topologyBroken() { return topologyBroken; }

    public void attempt(Runnable operation) {
        try {
            operation.run();
        } catch (Throwable failure) {
            diagnostics.record(capability);
        }
    }
}
