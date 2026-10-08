package dev.ronova.coremod.adapter;

import dev.ronova.coremod.contract.SubjectIdentity;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** The only canonical Entity-to-contract conversion used by R1 domains. */
public final class MinecraftSubjectAdapter {
    private MinecraftSubjectAdapter() {}

    public static SubjectIdentity from(Entity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity is required");
        }
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        return create(
            entity.getUUID(), type, entity.getClass().getName(),
            entity.level().dimension().location().toString(),
            entity.getX(), entity.getY(), entity.getZ(), entity instanceof Player
        );
    }

    public static SubjectIdentity fromSerialized(
        UUID uuid,
        String type,
        String runtimeClass,
        String dimension,
        double x,
        double y,
        double z
    ) {
        return create(
            uuid, type, runtimeClass, dimension, x, y, z,
            "minecraft:player".equals(type)
        );
    }

    private static SubjectIdentity create(
        UUID uuid,
        String type,
        String runtimeClass,
        String dimension,
        double x,
        double y,
        double z,
        boolean player
    ) {
        return new SubjectIdentity(
            uuid, type, runtimeClass, dimension,
            finiteOrZero(x), finiteOrZero(y), finiteOrZero(z), player
        );
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0D;
    }
}
