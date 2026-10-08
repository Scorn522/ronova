package dev.ronova.coremod.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityStateAccessor {
    @Accessor("DATA_HEALTH_ID")
    static EntityDataAccessor<Float> ronova$getHealthDataAccessor() {
        throw new AssertionError();
    }

    @Accessor("dead")
    void ronova$setDead(boolean dead);

    @Accessor("dead")
    boolean ronova$isDead();
}
