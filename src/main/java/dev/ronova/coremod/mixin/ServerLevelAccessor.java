package dev.ronova.coremod.mixin;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerLevel.class)
public interface ServerLevelAccessor {
    @Accessor("players")
    List<ServerPlayer> ronova$getPlayers();

    @Accessor("entityTickList")
    EntityTickList ronova$getEntityTickList();

    @Accessor("entityManager")
    PersistentEntitySectionManager<Entity> ronova$getEntityManager();
}
