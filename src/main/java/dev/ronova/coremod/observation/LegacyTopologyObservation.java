package dev.ronova.coremod.observation;

import dev.ronova.coremod.budget.BudgetReceipt;
import dev.ronova.coremod.budget.WorkBudget;
import dev.ronova.coremod.engine.TopologyIndex;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Disabled-mode adapter that preserves the process-global 7.13 topology behavior. */
public final class LegacyTopologyObservation implements TopologyObservation {
    @Override
    public void observe(Entity entity) {
        TopologyIndex.observe(entity);
    }

    @Override
    public List<Entity> snapshot(ServerLevel level) {
        return TopologyIndex.snapshot(level);
    }

    @Override
    public List<Entity> snapshot(UUID uuid) {
        return TopologyIndex.snapshot(uuid);
    }

    @Override
    public List<Entity> related(Entity source) {
        return TopologyIndex.related(source);
    }

    @Override
    public BudgetReceipt maintainStale(WorkBudget budget) {
        return TopologyIndex.maintainStale(budget);
    }

    @Override
    public int size() {
        return TopologyIndex.size();
    }

    @Override
    public void clear() {
        TopologyIndex.clear();
    }

    public void forgetLegacy(Entity entity) {
        TopologyIndex.forget(entity);
    }
}
