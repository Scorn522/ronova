package dev.ronova.coremod.observation;

import dev.ronova.coremod.budget.BudgetReceipt;
import dev.ronova.coremod.budget.WorkBudget;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Runtime-selected raw observation port. It intentionally exposes no terminal API. */
public interface TopologyObservation {
    void observe(Entity entity);

    List<Entity> snapshot(ServerLevel level);

    List<Entity> snapshot(UUID uuid);

    List<Entity> related(Entity source);

    /** Bounded cleanup of weak references; the gameplay query paths stay unchanged. */
    BudgetReceipt maintainStale(WorkBudget budget);

    int size();

    void clear();
}
