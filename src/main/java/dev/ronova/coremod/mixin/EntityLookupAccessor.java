package dev.ronova.coremod.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityLookup.class)
public interface EntityLookupAccessor {
    @Accessor("byId") Int2ObjectMap<EntityAccess> ronova$getById();
    @Accessor("byUuid") Map<UUID, EntityAccess> ronova$getByUuid();
}
