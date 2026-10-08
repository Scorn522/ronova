package dev.ronova.coremod.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SynchedEntityData.DataItem.class)
public interface SynchedDataItemStateAccessor {
    @Accessor("accessor") EntityDataAccessor<?> ronova$getAccessor();
    @Accessor("value") Object ronova$getValue();
    @Accessor("value") void ronova$setValue(Object value);
    @Accessor("dirty") void ronova$setDirty(boolean dirty);
}
