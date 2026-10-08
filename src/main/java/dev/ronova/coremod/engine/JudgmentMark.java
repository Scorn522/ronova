package dev.ronova.coremod.engine;

import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

/** Network-safe description of a judged identity and its generic lineage. */
public record JudgmentMark(
    UUID uuid,
    String type,
    String dimension,
    double x,
    double y,
    double z,
    double radius,
    JudgmentScope scope
) implements LineageRecord {
    public static JudgmentMark from(Entity entity, JudgmentScope scope, double radius) {
        return new JudgmentMark(
            entity.getUUID(),
            BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),
            entity.level().dimension().location().toString(),
            entity.getX(), entity.getY(), entity.getZ(), Math.max(0.0D, radius), scope
        );
    }

}
