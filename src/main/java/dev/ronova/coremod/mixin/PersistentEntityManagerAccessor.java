package dev.ronova.coremod.mixin;

import java.util.Set;
import java.util.UUID;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PersistentEntitySectionManager.class)
public interface PersistentEntityManagerAccessor {
    @Accessor("visibleEntityStorage")
    net.minecraft.world.level.entity.EntityLookup<?> ronova$getVisibleStorage();

    @Accessor("sectionStorage")
    net.minecraft.world.level.entity.EntitySectionStorage<?> ronova$getSectionStorage();

    @Accessor("knownUuids")
    Set<UUID> ronova$getKnownUuids();
}
