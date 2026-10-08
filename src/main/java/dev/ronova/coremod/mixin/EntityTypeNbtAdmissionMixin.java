package dev.ronova.coremod.mixin;

import dev.ronova.coremod.engine.SpawnFirewall;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stops serialized respawns before the entity factory allocates an object. */
@Mixin(EntityType.class)
public abstract class EntityTypeNbtAdmissionMixin {
    @Inject(
        method = "create(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void ronova$rejectSerializedSpawn(
        CompoundTag tag,
        Level level,
        CallbackInfoReturnable<Optional<Entity>> callback
    ) {
        if (SpawnFirewall.rejectNbt(level, tag)) {
            callback.setReturnValue(Optional.empty());
        }
    }
}
