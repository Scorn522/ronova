package dev.ronova.coremod.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.concurrent.locks.ReadWriteLock;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Direct state access for the registered terminal-state sink; it never invokes entity callbacks. */
@Mixin(SynchedEntityData.class)
public interface SynchedEntityDataStateAccessor {
    @Accessor("entity") Entity ronova$getOwner();
    @Accessor("itemsById") Int2ObjectMap<SynchedEntityData.DataItem<?>> ronova$getItems();
    @Accessor("lock") ReadWriteLock ronova$getLock();
    @Accessor("isDirty") void ronova$setDirty(boolean dirty);
}
