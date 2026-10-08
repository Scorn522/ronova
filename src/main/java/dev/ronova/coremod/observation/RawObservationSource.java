package dev.ronova.coremod.observation;

import dev.ronova.coremod.budget.BudgetReceipt;
import dev.ronova.coremod.budget.WorkBudget;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Server-owned raw observation boundary used by the 7.14A enabled mode. */
public final class RawObservationSource implements TopologyObservation {
    private final MinecraftServer owner;
    private final TopologyGraph graph;

    public RawObservationSource(MinecraftServer owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
        graph = new TopologyGraph(owner);
    }

    @Override
    public void observe(Entity entity) {
        // TopologyGraph owns the authoritative server/level eligibility check.
        // Keeping it in one place avoids repeating that work on every admission
        // boundary while preserving the same rejection semantics.
        graph.observe(entity);
    }

    @Override
    public List<Entity> snapshot(ServerLevel level) {
        return level != null && level.getServer() == owner
            ? graph.snapshot(level) : List.of();
    }

    @Override
    public List<Entity> snapshot(UUID uuid) {
        return graph.snapshot(uuid);
    }

    @Override
    public List<Entity> related(Entity source) {
        return belongsToOwner(source) ? graph.related(source) : List.of();
    }

    @Override
    public BudgetReceipt maintainStale(WorkBudget budget) {
        return graph.maintainStale(budget);
    }

    @Override
    public int size() {
        return graph.size();
    }

    @Override
    public void clear() {
        graph.clear();
    }

    private boolean belongsToOwner(Entity entity) {
        return entity != null
            && entity.level() instanceof ServerLevel level
            && level.getServer() == owner;
    }
}
