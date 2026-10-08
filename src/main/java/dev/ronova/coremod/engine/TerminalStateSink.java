package dev.ronova.coremod.engine;

import dev.ronova.coremod.mixin.EntityStateAccessor;
import dev.ronova.coremod.mixin.LivingEntityStateAccessor;
import dev.ronova.coremod.mixin.SynchedDataItemStateAccessor;
import dev.ronova.coremod.mixin.SynchedEntityDataStateAccessor;
import dev.ronova.coremod.root.api.OperationReport;
import dev.ronova.coremod.root.api.TerminalOperationPlan;
import dev.ronova.coremod.root.api.WriteSetLease;
import dev.ronova.coremod.root.api.WriteSinkId;
import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import dev.ronova.coremod.integrity.RawAccessorDispatch;
import net.minecraft.network.syncher.SynchedEntityData;

/** Idempotent final field writes, separate from once-only death/removal callbacks and their unknown effects. */
final class TerminalStateSink {
    static final WriteSinkId LIFE_SINK=new WriteSinkId("ronova:terminal-state/life");
    static final WriteSinkId REMOVAL_SINK=new WriteSinkId("ronova:terminal-state/removal");
    private TerminalStateSink() {}

    static OperationReport.StepReceipt reassert(Entity entity, TerminalOperationPlan plan,
        net.minecraft.server.level.ServerLevel expectedLevel, WriteSetLease writeSet,boolean rewriteLife,boolean forceRemoval) {
        if(!rewriteLife&&!forceRemoval)return receipt(OperationReport.Status.UNAVAILABLE,"no-permitted-state-action");
        if(rewriteLife&&!forceRemoval&&!(entity instanceof LivingEntity))
            return receipt(OperationReport.Status.UNAVAILABLE,"entity-has-no-registered-life-storage");
        if (!writeSet.current()) return receipt(OperationReport.Status.FENCED, "terminal-state-permission-expired");
        if (!RawAccessorDispatch.matches(entity, Entity.class, EntityStateAccessor.class)
            || entity instanceof LivingEntity && !RawAccessorDispatch.matches(entity, LivingEntity.class, LivingEntityStateAccessor.class))
            return receipt(OperationReport.Status.UNAVAILABLE, "terminal-accessor-dispatch-unverified");
        EntityStateAccessor state = (EntityStateAccessor) entity;
        BooleanSupplier current = () -> expectedLevel != null && writeSet.current()
            && plan.subjectKey().subjectId().equals(state.ronova$getUuid()) && state.ronova$getLevel() == expectedLevel
            && expectedLevel.getServer().isSameThread() && expectedLevel.getServer().getLevel(expectedLevel.dimension()) == expectedLevel;
        if (!current.getAsBoolean()) return receipt(OperationReport.Status.FENCED,"terminal-physical-identity-changed");
        if (entity instanceof LivingEntity living && rewriteLife) {
            var entityData=state.ronova$getEntityData();
            if (!RawAccessorDispatch.matches(entityData, SynchedEntityData.class, SynchedEntityDataStateAccessor.class))
                return receipt(OperationReport.Status.UNAVAILABLE, "terminal-data-accessor-dispatch-unverified");
            SynchedEntityDataStateAccessor data = (SynchedEntityDataStateAccessor) entityData;
            var storage=registeredStorage(data);
            if (storage==null || !storage.bound(state,entity)) return receipt(OperationReport.Status.UNAVAILABLE, "terminal-health-storage-semantics-unverified");
            var lock = storage.lock().writeLock();
            if (!lock.tryLock()) return receipt(OperationReport.Status.UNAVAILABLE, "terminal-health-data-busy");
            try {
                var health = LivingEntityStateAccessor.ronova$getHealthDataAccessor();
                if (health == null || health.getClass() != net.minecraft.network.syncher.EntityDataAccessor.class)
                    return receipt(OperationReport.Status.UNAVAILABLE, "terminal-health-key-dispatch-unverified");
                int healthId=health.getId();
                var item = storage.items().get(healthId);
                if (!RawAccessorDispatch.matches(item, SynchedEntityData.DataItem.class, SynchedDataItemStateAccessor.class)
                    || !(item instanceof SynchedDataItemStateAccessor value) || value.ronova$getAccessor() != health
                    || !(value.ronova$getValue() instanceof Float))
                    return receipt(OperationReport.Status.UNAVAILABLE, "terminal-health-data-binding-unverified");
                // Recheck after obtaining the final storage slot. No user/entity callback runs in this write path.
                if (!current.getAsBoolean()) return receipt(OperationReport.Status.FENCED, "terminal-health-write-revoked");
                if(!writeSet.consume(LIFE_SINK))return receipt(OperationReport.Status.FENCED,"write-set-life-rejected");
                if(!storage.bound(state,entity)||storage.items().get(healthId)!=item)
                    return receipt(OperationReport.Status.UNAVAILABLE,"terminal-health-storage-replaced");
                if (!Float.valueOf(0.0F).equals(value.ronova$getValue())) {
                    value.ronova$setValue(0.0F);
                    value.ronova$setDirty(true);
                    data.ronova$setDirty(true);
                }
                if (!current.getAsBoolean()||!storage.bound(state,entity)||storage.items().get(healthId)!=item
                    ||!Float.valueOf(0.0F).equals(value.ronova$getValue()))
                    return receipt(OperationReport.Status.UNAVAILABLE, "terminal-health-write-not-observed");
            } finally { lock.unlock(); }
        }
        if(forceRemoval) {
            if (!current.getAsBoolean()) return receipt(OperationReport.Status.FENCED, "terminal-removal-write-revoked");
            // One declared sink covers the grouped death/removal field writes below. Consume it before
            // the first mutation so an undeclared or revoked write set can never partially mutate state.
            if(!writeSet.consume(REMOVAL_SINK))return receipt(OperationReport.Status.FENCED,"write-set-removal-rejected");
        }
        if(entity instanceof LivingEntity living && forceRemoval) {
            if (!current.getAsBoolean()) return receipt(OperationReport.Status.FENCED, "terminal-living-write-revoked");
            var livingState = (LivingEntityStateAccessor) living;
            livingState.ronova$setDead(true);
            living.deathTime = Math.max(20, living.deathTime);
            if (!livingState.ronova$isDead() || living.deathTime < 20)
                return receipt(OperationReport.Status.UNAVAILABLE, "terminal-living-write-not-observed");
        }
        if(forceRemoval) {
            if (!current.getAsBoolean()) return receipt(OperationReport.Status.FENCED, "terminal-removal-write-revoked");
            state.ronova$setRemovalReason(Entity.RemovalReason.KILLED);
            state.ronova$setAddedToWorld(false);
            state.ronova$setLevelCallback(EntityInLevelCallback.NULL);
        }
        if (!current.getAsBoolean()) return receipt(OperationReport.Status.FENCED, "terminal-state-postcheck-revoked");
        if(!rewriteLife||!forceRemoval)return receipt(OperationReport.Status.DISPATCHED,
            rewriteLife?"registered-health-written;removal-separate":"registered-removal-written;life-separate");
        if (!matches(entity)) return receipt(OperationReport.Status.DISPATCHED, "permitted-fields-written;remaining-terminal-state-unproved");
        return receipt(OperationReport.Status.CONVERGED, "registered-terminal-fields/v1;callbacks-not-replayed;indexes-and-effects-separate");
    }
    static boolean removalObserved(Entity entity) {
        return RawAccessorDispatch.matches(entity,Entity.class,EntityStateAccessor.class)
            &&((EntityStateAccessor)entity).ronova$getRemovalReason()!=null;
    }

    static boolean matches(Entity entity) {
        return matchesRemoval(entity)&&(!(entity instanceof LivingEntity)||matchesLife(entity));
    }
    static boolean matchesRemoval(Entity entity) {
        return Boolean.TRUE.equals(observeRemoval(entity));
    }
    static Boolean observeRemoval(Entity entity) {
        if (!RawAccessorDispatch.matches(entity, Entity.class, EntityStateAccessor.class)
            || !(entity instanceof EntityStateAccessor state))return null;
        if(entity instanceof LivingEntity&&!RawAccessorDispatch.matches(entity,LivingEntity.class,LivingEntityStateAccessor.class))return null;
        return state.ronova$getRemovalReason()==Entity.RemovalReason.KILLED&&!state.ronova$isAddedToWorld()
            &&state.ronova$getLevelCallback()==EntityInLevelCallback.NULL
            &&(!(entity instanceof LivingEntity living)||((LivingEntityStateAccessor)living).ronova$isDead()&&living.deathTime>=20);
    }
    static boolean matchesLife(Entity entity) {
        return Float.valueOf(0.0F).equals(observeLife(entity));
    }
    static Float observeLife(Entity entity) {
        if(!(entity instanceof LivingEntity)||!RawAccessorDispatch.matches(entity,Entity.class,EntityStateAccessor.class))return null;
        var state=(EntityStateAccessor)entity;
        var entityData=state.ronova$getEntityData();
        if (!RawAccessorDispatch.matches(entity, LivingEntity.class, LivingEntityStateAccessor.class)
            || !RawAccessorDispatch.matches(entityData, SynchedEntityData.class, SynchedEntityDataStateAccessor.class)) return null;
        var data = (SynchedEntityDataStateAccessor) entityData;
        var storage=registeredStorage(data);
        if (storage==null||!storage.bound(state,entity)) return null;
        var lock = storage.lock().readLock();
        if (!lock.tryLock()) return null;
        try {
            var key = LivingEntityStateAccessor.ronova$getHealthDataAccessor();
            if (key == null || key.getClass() != net.minecraft.network.syncher.EntityDataAccessor.class) return null;
            int healthId=key.getId();var item = storage.items().get(healthId);
            if(!RawAccessorDispatch.matches(item,SynchedEntityData.DataItem.class,SynchedDataItemStateAccessor.class)
                ||!(item instanceof SynchedDataItemStateAccessor value)||value.ronova$getAccessor()!=key)return null;
            Object observed=value.ronova$getValue();
            return observed instanceof Float health&&storage.bound(state,entity)&&storage.items().get(healthId)==item
                &&value.ronova$getValue()==observed?health:null;
        } finally { lock.unlock(); }
    }

    private static OperationReport.StepReceipt receipt(OperationReport.Status status, String detail) {
        return new OperationReport.StepReceipt(TerminalOperationPlan.Step.TERMINAL_STATE, status, detail);
    }
    private record HealthStorage(SynchedEntityDataStateAccessor data,java.util.concurrent.locks.ReadWriteLock lock,
        it.unimi.dsi.fastutil.ints.Int2ObjectMap<SynchedEntityData.DataItem<?>> items) {
        boolean bound(EntityStateAccessor state,Entity entity) {
            return state.ronova$getEntityData()==data&&data.ronova$getOwner()==entity
                &&data.ronova$getLock()==lock&&data.ronova$getItems()==items;
        }
    }
    private static HealthStorage registeredStorage(SynchedEntityDataStateAccessor data) {
        var lock=data.ronova$getLock();var items=data.ronova$getItems();
        return lock!=null&&items!=null&&lock.getClass()==java.util.concurrent.locks.ReentrantReadWriteLock.class
            &&items.getClass()==it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap.class
            ?new HealthStorage(data,lock,items):null;
    }
}
