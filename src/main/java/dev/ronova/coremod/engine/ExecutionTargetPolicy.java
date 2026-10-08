package dev.ronova.coremod.engine;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.entity.PartEntity;

/**
 * Generic runtime target classification. Dropped stacks and experience are
 * world results, not combatants; indexing or judging them turns ordinary loot
 * into an ever-growing candidate set.
 */
public final class ExecutionTargetPolicy {
    private ExecutionTargetPolicy() {}

    public static boolean isCombatObject(Entity entity) {
        return entity != null
            && !(entity instanceof ItemEntity)
            && !(entity instanceof ExperienceOrb);
    }

    /** Raw-observation eligibility depends only on the physical object kind. */
    public static boolean shouldIndex(Entity entity) {
        return isCombatObject(entity)
            && (entity instanceof LivingEntity || entity instanceof PartEntity<?>);
    }

    /** Judgment eligibility remains wider than the construction-time raw index. */
    public static boolean shouldJudge(Entity entity) {
        return isCombatObject(entity) && !JudgmentState.isJudged(entity);
    }

}
