package dev.ronova.coremod.capability.recovery;

import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.integrity.RawAccessorDispatch;
import dev.ronova.coremod.mixin.*;
import dev.ronova.coremod.policy.*;
import java.util.Set;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Repairs the registered health cell for a current Owner without dispatching a mod's health setter. */
final class RegisteredOwnerVitals {
    private static final Set<StrengthOptionKey> KEYS=Set.of(StrengthOptionKey.PROTECT_LIFE,StrengthOptionKey.PROTECT_AUTO_REPAIR,
        StrengthOptionKey.RUNTIME_NATIVE,StrengthOptionKey.RUNTIME_JVMTI,StrengthOptionKey.PROTECT_CODE_INTEGRITY,
        StrengthOptionKey.RUNTIME_CRITICAL_CLASS_WATCH);
    private RegisteredOwnerVitals(){}
    static boolean repair(ServerPlayer player,float health) {
        if(player.server==null||!player.server.isSameThread()||!Float.isFinite(health)||health<=0)return false;
        var runtime=ServerRuntimeRegistry.getIfPresent(player.server);
        if(runtime==null||runtime.durableStore()==null||runtime.authorityState().shuttingDown())return false;
        var policy=runtime.strengthPolicy();if(policy.pendingRestart())return false;
        for(var key:KEYS)if(ProfilePresetMatrix.resolve(policy.activeMatrixRevision(),policy.activeProfile(),key,
            policy.activeLevels().get(key)).requestedLevel().rank()<MechanismLevel.PROTECT_RONOVA.rank())return false;
        var store=runtime.durableStore();
        try(var observation=runtime.enforcement().beginObservation(0x8063)) {
            if(observation==null||!RawAccessorDispatch.matches(player,Entity.class,EntityStateAccessor.class))return false;
            var state=(EntityStateAccessor)player;
            var uuid=state.ronova$getUuid();var level=state.ronova$getLevel();
            if(uuid==null||!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)
                ||serverLevel.getServer()!=player.server||player.server.getLevel(serverLevel.dimension())!=level)return false;
            var connection=player.connection;
            if(connection==null||connection.player!=player||replacedOnlineInstance(player,uuid))return false;
            var stamp=store.authorityStamp(uuid);
            if(stamp==null||!stamp.owner()||!stamp.settled())return false;
            try(var lease=store.acquireOwnerWrite(stamp)) {
            if(lease==null)return false;
            var entityData=state.ronova$getEntityData();
            if(!RawAccessorDispatch.matches(entityData,SynchedEntityData.class,SynchedEntityDataStateAccessor.class))return false;
            var data=(SynchedEntityDataStateAccessor)entityData;var lock=data.ronova$getLock();var items=data.ronova$getItems();
            if(lock==null||lock.getClass()!=java.util.concurrent.locks.ReentrantReadWriteLock.class||items==null
                ||items.getClass()!=it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap.class||data.ronova$getOwner()!=player)return false;
            if(!lock.writeLock().tryLock())return false;
            boolean written;
            try {
                var key=LivingEntityStateAccessor.ronova$getHealthDataAccessor();
                if(key==null||key.getClass()!=net.minecraft.network.syncher.EntityDataAccessor.class)return false;
                int id=key.getId();var item=items.get(id);
                if(!RawAccessorDispatch.matches(item,SynchedEntityData.DataItem.class,SynchedDataItemStateAccessor.class)
                    ||!(item instanceof SynchedDataItemStateAccessor cell)||cell.ronova$getAccessor()!=key
                    ||!(cell.ronova$getValue() instanceof Float))return false;
                if(!lease.valid(stamp)||!observation.stillCurrent()||runtime.authorityState().shuttingDown()
                    ||!uuid.equals(state.ronova$getUuid())||state.ronova$getLevel()!=level
                    ||player.connection!=connection||connection.player!=player||replacedOnlineInstance(player,uuid)
                    ||state.ronova$getEntityData()!=entityData||data.ronova$getOwner()!=player||data.ronova$getLock()!=lock
                    ||data.ronova$getItems()!=items||items.get(id)!=item)return false;
                if(!Float.valueOf(health).equals(cell.ronova$getValue())) {
                    cell.ronova$setValue(health);cell.ronova$setDirty(true);data.ronova$setDirty(true);
                }
                written=Float.valueOf(health).equals(cell.ronova$getValue())&&items.get(id)==item
                    &&uuid.equals(state.ronova$getUuid())&&state.ronova$getLevel()==level
                    &&player.connection==connection&&connection.player==player&&!replacedOnlineInstance(player,uuid)
                    &&state.ronova$getEntityData()==entityData&&data.ronova$getItems()==items&&data.ronova$getLock()==lock
                    &&data.ronova$getOwner()==player;
            } finally {lock.writeLock().unlock();}
            return written&&lease.valid(stamp)&&observation.finish();
            }
        }
    }
    private static boolean replacedOnlineInstance(ServerPlayer player,java.util.UUID uuid) {
        var listed=player.server.getPlayerList().getPlayer(uuid);
        // Missing indexes remain a recovery obligation, not grounds to disable every repair.
        return listed!=null&&listed!=player;
    }
}
