package dev.ronova.coremod.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityStateAccessor {
    @Accessor("id")
    int ronova$getEntityId();
    @Accessor("uuid")
    java.util.UUID ronova$getUuid();
    @Accessor("uuid")
    void ronova$setUuid(java.util.UUID uuid);
    @Accessor("stringUUID")
    String ronova$getStringUuid();
    @Accessor("stringUUID")
    void ronova$setStringUuid(String uuid);

    @Accessor("level")
    net.minecraft.world.level.Level ronova$getLevel();

    @Accessor("removalReason")
    Entity.RemovalReason ronova$getRemovalReason();

    @Accessor(value = "isAddedToWorld", remap = false)
    boolean ronova$isAddedToWorld();

    @Accessor("entityData")
    net.minecraft.network.syncher.SynchedEntityData ronova$getEntityData();

    @Accessor("removalReason")
    void ronova$setRemovalReason(Entity.RemovalReason reason);

    @Accessor(value = "isAddedToWorld", remap = false)
    void ronova$setAddedToWorld(boolean value);

    @Accessor("levelCallback")
    EntityInLevelCallback ronova$getLevelCallback();

    @Accessor("levelCallback")
    void ronova$setLevelCallback(EntityInLevelCallback callback);
}
