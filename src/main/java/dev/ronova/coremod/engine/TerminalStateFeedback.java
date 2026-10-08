package dev.ronova.coremod.engine;

import dev.ronova.coremod.budget.*;
import dev.ronova.coremod.policy.TerminalActionPolicy;
import dev.ronova.coremod.root.api.*;
import dev.ronova.coremod.runtime.*;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Follows successful registered state writes; never replays death, loot, removal callbacks or commands. */
final class TerminalStateFeedback {
    private static final int CAPACITY=256, WAITING_CAPACITY=64, SLICE=16;
    private record Key(EffectKey effect,UUID nonce,TerminalActionPolicy.Step step) {}
    private static final class Entry {
        final WeakReference<Entity> entity;
        TerminalOperationPlan plan;
        final StateFeedbackSchedule schedule;
        final HostResourceGovernor.Reservation reservation;
        boolean identityGap;
        Entry(Entity entity,TerminalOperationPlan plan,StateFeedbackSchedule schedule,HostResourceGovernor.Reservation reservation) {
            this.entity=new WeakReference<>(entity);this.plan=plan;this.schedule=schedule;this.reservation=reservation;
        }
    }
    private final DominionServerRuntime runtime;
    private final LinkedHashMap<Key,Entry> entries=new LinkedHashMap<>();
    private final LinkedHashMap<Key,Entry> waiting=new LinkedHashMap<>();
    private long observations,rebounds,repairs,unavailable,dropped;
    TerminalStateFeedback(DominionServerRuntime runtime) {
        this.runtime=runtime;
        runtime.resources().declareAccounting(HostResourceDomain.JAVA_HEAP,HostResourceGovernor.Accounting.PARTIAL_ESTIMATE,
            "terminal-state-feedback/1024-bytes");
    }
    synchronized void follow(Entity entity,TerminalOperationPlan plan,TerminalActionPolicy.Step step) {
        if(!dev.ronova.coremod.integrity.RawAccessorDispatch.matches(entity,Entity.class,
            dev.ronova.coremod.mixin.EntityStateAccessor.class)) {
            dropped=increment(dropped);runtime.obligations().discoveryGap(plan.qualification().targetEpoch());return;
        }
        var state=(dev.ronova.coremod.mixin.EntityStateAccessor)entity;
        if(!(state.ronova$getLevel() instanceof ServerLevel level)||!level.getServer().isSameThread()
            ||dev.ronova.coremod.bootstrap.ServerRuntimeRegistry.getIfPresent(level.getServer())!=runtime) {
            dropped=increment(dropped);runtime.obligations().discoveryGap(plan.qualification().targetEpoch());return;
        }
        if(step!=TerminalActionPolicy.Step.LIFE&&step!=TerminalActionPolicy.Step.REMOVAL)return;
        var policy=runtime.strengthPolicy();var permission=TerminalActionPolicy.resolve(policy,step,true);
        if(!permission.permitted())return;
        var key=new Key(plan.effectKey(),plan.physicalNonce(),step);
        var previous=entries.get(key);
        if(previous==null)previous=waiting.get(key);
        if(previous!=null) {
            if(previous.entity.get()==entity&&previous.plan.qualification().equals(plan.qualification())) {
                previous.plan=plan;
                previous.schedule.updateDepth(level.getServer().getTickCount(),TerminalActionPolicy.requestedDepth(policy,step));
                return;
            }
            entries.remove(key);waiting.remove(key);previous.reservation.close();
        }
        if(entries.size()>=CAPACITY&&waiting.size()>=WAITING_CAPACITY){dropped=increment(dropped);runtime.obligations().discoveryGap(plan.qualification().targetEpoch());return;}
        var reserved=runtime.resources().reserve(Map.of(HostResourceDomain.JAVA_HEAP,1024L),HostResourceGovernor.Priority.RECOVERY);
        if(!reserved.granted()){dropped=increment(dropped);runtime.obligations().discoveryGap(plan.qualification().targetEpoch());return;}
        var entry=new Entry(entity,plan,new StateFeedbackSchedule(level.getServer().getTickCount(),
            TerminalActionPolicy.requestedDepth(policy,step)),reserved.reservation());
        if(entries.size()<CAPACITY)entries.put(key,entry);
        else {
            waiting.put(key,entry);
            runtime.obligations().discoveryGap(plan.qualification().targetEpoch());
        }
    }
    synchronized void pulse(MinecraftServer server,WorkBudget budget,TerminalPlanExecutor executor) {
        if(!server.isSameThread())return;
        fillVacancies();
        int count=Math.min(SLICE,entries.size()),processed=0;
        var visited=new ArrayList<Key>(count);
        try {
            for(int i=0;i<count&&budget.tryConsumeCount(1);i++) {
                processed++;
                var key=entries.keySet().iterator().next();var entry=entries.remove(key);entries.put(key,entry);
                visited.add(key);
                try {
                var entity=entry.entity.get();var old=entry.plan;var store=runtime.durableStore();
                if(entity==null||!runtime.incarnationNonce(entity).equals(old.physicalNonce())) {
                    retire(key,entry);continue;
                }
                if(store==null||store.persistencePending())continue;
                var plan=store.approveTerminalPlan(old.subjectKey().subjectId(),old.physicalNonce());
                var policy=runtime.strengthPolicy();
                if(plan==null||!plan.qualification().equals(old.qualification())
                    ||!TerminalActionPolicy.resolve(policy,key.step(),true).permitted()) {
                    retire(key,entry);continue;
                }
                int tick=server.getTickCount();
                entry.plan=plan;
                entry.schedule.updateDepth(tick,TerminalActionPolicy.requestedDepth(policy,key.step()));
                if(!entry.schedule.due(tick))continue;
                Boolean identity=currentIdentity(entity,plan,server);
                if(!Boolean.TRUE.equals(identity)) {
                    unavailable=increment(unavailable);entry.schedule.unavailable(tick);
                    // An unreadable or changed field is not proof that this subject's
                    // continuous result has ended. Preserve the plan and its unresolved debt.
                    if(!entry.identityGap) {
                        runtime.obligations().discoveryGap(plan.qualification().targetEpoch());entry.identityGap=true;
                    }
                    continue;
                }
                entry.identityGap=false;
                var result=executor.recheckRegisteredState(entity,plan,key.step());
                if(result.status()==OperationReport.Status.FENCED||result.status()==OperationReport.Status.UNAVAILABLE) {
                    unavailable=increment(unavailable);entry.schedule.unavailable(tick);continue;
                }
                observations=increment(observations);
                boolean drift=result.status()!=OperationReport.Status.CONVERGED;
                if(drift) {
                    rebounds=increment(rebounds);
                    var repair=executor.reassertRegisteredState(entity,plan,key.step());
                    if(repair.status()==OperationReport.Status.DISPATCHED||repair.status()==OperationReport.Status.CONVERGED)repairs=increment(repairs);
                    else unavailable=increment(unavailable);
                }
                entry.schedule.observed(tick,drift);
                if(entry.schedule.finished())retire(key,entry);
                } catch(RuntimeException|LinkageError failure) {
                    unavailable=increment(unavailable);entry.schedule.unavailable(server.getTickCount());
                }
            }
        } finally {
            // Exchange only entries actually visited this round. Moving unvisited
            // heads or repeatedly exchanging newly promoted entries can starve them.
            try {rotateWaiting(visited);}
            finally {if(processed>0)budget.finish(processed,0);}
        }
    }
    private void fillVacancies() {
        while(entries.size()<CAPACITY&&!waiting.isEmpty()) {
            var key=waiting.keySet().iterator().next();entries.put(key,waiting.remove(key));
        }
    }
    private void rotateWaiting(List<Key> visited) {
        fillVacancies();
        for(var served:visited) {
            if(waiting.isEmpty())break;
            var entry=entries.remove(served);
            if(entry==null)continue;
            var next=waiting.keySet().iterator().next();
            entries.put(next,waiting.remove(next));waiting.put(served,entry);
        }
    }
    private Boolean currentIdentity(Entity entity,TerminalOperationPlan plan,MinecraftServer server) {
        try(var observation=runtime.enforcement().beginObservation(1)) {
            if(observation==null||!dev.ronova.coremod.integrity.RawAccessorDispatch.matches(entity,Entity.class,
                dev.ronova.coremod.mixin.EntityStateAccessor.class))return null;
            var state=(dev.ronova.coremod.mixin.EntityStateAccessor)entity;
            var uuid=state.ronova$getUuid();var level=state.ronova$getLevel();
            boolean matches=plan.subjectKey().subjectId().equals(uuid)&&level instanceof ServerLevel current
                &&current.getServer()==server&&server.getLevel(current.dimension())==current
                &&dev.ronova.coremod.bootstrap.ServerRuntimeRegistry.getIfPresent(server)==runtime
                &&Objects.equals(uuid,state.ronova$getUuid())&&state.ronova$getLevel()==level;
            return observation.finish()?matches:null;
        }
    }
    private void retire(Key key,Entry entry){entries.remove(key);entry.reservation.close();}
    synchronized String summary(){return "状态复查 "+entries.size()+"；待接续 "+waiting.size()+"；反弹 "+rebounds+"；重写 "+repairs+"；未就绪 "+unavailable+"；未接纳 "+dropped;}
    synchronized void clear(){
        RuntimeException failed=null;
        for(var entry:entries.values())try {entry.reservation.close();}catch(RuntimeException failure) {
            if(failed==null)failed=failure;else if(failed!=failure)failed.addSuppressed(failure);
        }
        for(var entry:waiting.values())try {entry.reservation.close();}catch(RuntimeException failure) {
            if(failed==null)failed=failure;else if(failed!=failure)failed.addSuppressed(failure);
        }
        entries.clear();waiting.clear();if(failed!=null)throw failed;
    }
    private static long increment(long value){return value==Long.MAX_VALUE?value:value+1;}
}
