package dev.ronova.coremod.engine;

import dev.ronova.coremod.authority.AuthorityView;
import dev.ronova.coremod.budget.*;
import dev.ronova.coremod.root.api.*;
import dev.ronova.coremod.runtime.*;
import dev.ronova.coremod.policy.TerminalActionPolicy;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Production ports for the Root-approved server terminal plan. No local step writes Judgment. */
public final class TerminalPlanExecutor {
    private final DominionServerRuntime runtime;
    private final OperationPlanRunner runner = new OperationPlanRunner();
    private final TerminalCompletionMonitor completion;
    private final TerminalIndexRepair indices;
    private final TerminalStateFeedback stateFeedback;
    public record ReportedOperation(OperationReport report, boolean acceptedByRoot) {}
    private final ArrayDeque<ReportedOperation> reports = new ArrayDeque<>();
    private final LinkedHashSet<String> dispatched = new LinkedHashSet<>();
    private record Participation(dev.ronova.coremod.network.ClientParticipationTracker tracker,HostResourceGovernor.Reservation reservation) {}
    private final LinkedHashMap<EffectKey,Participation> participants=new LinkedHashMap<>();
    private long accepted, fenced;
    private record Identity(UUID subject,ServerLevel level,UUID nonce,int entityId) {}
    public TerminalPlanExecutor(DominionServerRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime);
        completion = new TerminalCompletionMonitor(runtime);
        indices = new TerminalIndexRepair(runtime);
        stateFeedback = new TerminalStateFeedback(runtime);
        runtime.resources().declareAccounting(HostResourceDomain.JAVA_HEAP, HostResourceGovernor.Accounting.PARTIAL_ESTIMATE,
            "terminal-plan-live/16384-bytes");
        runtime.resources().declareAccounting(HostResourceDomain.JAVA_HEAP, HostResourceGovernor.Accounting.PARTIAL_ESTIMATE,
            "frozen-client-contract/4096+256-per-member");
    }
    public boolean execute(Entity entity, AuthorityView view) {
        if (entity == null || view == null || runtime.durableStore() == null) return false;
        var identity=readIdentity(entity);
        if(identity==null){countFenced();return false;}
        var level=identity.level();
        var store = runtime.durableStore();
        TerminalOperationPlan plan = store.approveTerminalPlan(identity.subject(), identity.nonce());
        if (plan == null) { countFenced(); return false; }
        int initialEntityId=identity.entityId();
        runtime.spawnAdmission().rememberClassLineage(entity,plan);
        Participation participation=participation(plan,level.getServer());
        var resource = runtime.resources().reserve(Map.of(HostResourceDomain.JAVA_HEAP, plan.reservedHeapBytes()), HostResourceGovernor.Priority.RECOVERY);
        if (!resource.granted()) { countFenced(); return false; }
        try (var ignored = resource.reservation();
             var observations = new dev.ronova.coremod.observation.RecoveryObservationScope(runtime.recoverySources(), level.getServer())) {
            OperationReport report = runner.run(plan, new OperationPlanRunner.Ports() {
                @Override public boolean current(TerminalOperationPlan value) {
                    var current=readIdentity(entity);
                    return current!=null&&current.level()==level&&current.subject().equals(value.subjectKey().subjectId())
                        &&current.nonce().equals(value.physicalNonce())&&store.validateTerminalPlan(value)
                        && runtime.projection().snapshot().epoch().compareTo(store.epoch()) >= 0;
                }
                @Override public OperationReport.StepReceipt controlled(TerminalOperationPlan value, TerminalOperationPlan.Step step) {
                    return controlled(value, step, () -> false);
                }
                @Override public OperationReport.StepReceipt controlled(TerminalOperationPlan value, TerminalOperationPlan.Step step,
                    java.util.function.BooleanSupplier permitted) {
                    if (!permitted.getAsBoolean())
                        return new OperationReport.StepReceipt(step, OperationReport.Status.FENCED, "final-write-permission-expired");
                    switch (step) {
                        case ADMISSION_FENCE -> {
                            SpawnFirewall.beginExecution(entity); SpawnFirewall.commitExecution(entity);
                            return new OperationReport.StepReceipt(step, runtime.spawnAdmission().plannedFencePresent(entity)
                                ? OperationReport.Status.CONVERGED : OperationReport.Status.UNAVAILABLE, "server-owned-admission-fence");
                        }
                        case SERVER_PRESENCE -> {
                            QuarantineView.register(level.getServer(), entity);
                            return new OperationReport.StepReceipt(step, QuarantineView.registered(level.getServer(), entity)
                                ? OperationReport.Status.CONVERGED : OperationReport.Status.UNAVAILABLE, "server-owned-presence-registry");
                        }
                        case SOURCE_QUALIFICATION -> {
                            var policy = dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(runtime.strengthPolicy(),
                                dev.ronova.coremod.policy.RecoveryChainPolicy.Step.DIRECT_SOURCE);
                            if (!policy.observationRequested()) return new OperationReport.StepReceipt(step,
                                OperationReport.Status.CONVERGED, "source-discovery-outside-active-depth;base-admission-retained");
                            runtime.recoverySources().watch(level, value);
                            runtime.recoverySources().pulse(runtime.budget(BudgetCategory.OBSERVATION));
                            return new OperationReport.StepReceipt(step, OperationReport.Status.DISPATCHED,
                                "source-observation-and-qualified-outlet;record-task-save-disposition-separate");
                        }
                        case TERMINAL_STATE -> { return stateActions(entity,value,permitted); }
                        case WORLD_RECORDS -> { return observedStep(value,step,TerminalActionPolicy.Step.REMOVED_RECORDS.mask(),permitted,
                            gate -> {
                                boolean removed=gate.getAsBoolean()&&TerminalStateSink.removalObserved(entity);
                                var action=removed?TerminalActionPolicy.Step.REMOVED_RECORDS:TerminalActionPolicy.Step.BYPASS_RECORDS;
                                var decision=TerminalActionPolicy.resolve(runtime.strengthPolicy(),action,gate.getAsBoolean());
                                if(!decision.permitted())return new OperationReport.StepReceipt(step,OperationReport.Status.UNAVAILABLE,decision.reason());
                                if(!indices.prepare(entity,value,initialEntityId))return new OperationReport.StepReceipt(step,OperationReport.Status.UNAVAILABLE,"world-index-budget-unavailable");
                                return indices.repair(entity,value,()->gate.getAsBoolean()
                                    &&(!removed||TerminalStateSink.removalObserved(entity)));
                            }); }
                        case NETWORK_PROJECTION -> {
                            JudgmentMark mark = runtime.projection().admission().mark(value.subjectKey().subjectId());
                            if (mark == null) return new OperationReport.StepReceipt(step, OperationReport.Status.UNAVAILABLE, "missing-current-root-mark");
                            synchronized (TerminalPlanExecutor.this) {
                                if (!dispatched.contains(value.causalityKey())) {
                                    dev.ronova.coremod.network.QuarantineSyncService.broadcastMarks(level.getServer(), List.of(mark));
                                    if (dispatched.size() >= 128) dispatched.remove(dispatched.iterator().next());
                                    dispatched.add(value.causalityKey());
                                }
                            }
                            if(participation==null)return new OperationReport.StepReceipt(step,OperationReport.Status.DISPATCHED,"client-view-contract-budget-unavailable");
                            var tracker=participation.tracker();var status=tracker.sample(member->dev.ronova.coremod.network.QuarantineSyncService
                                .confirmsParticipation(level.getServer(),tracker.contract(),member),Integer.toUnsignedLong(level.getServer().getTickCount()));
                            return new OperationReport.StepReceipt(step,status.requiredViewsStable()?OperationReport.Status.CONVERGED:OperationReport.Status.DISPATCHED,
                                "client-view-contract="+tracker.contract().id()+";required-missing="+status.missingRequired().size()
                                    +";optional-missing="+status.missingOptional().size()+";deadline="+status.deadlineExceeded()+";pixels-unproven");
                        }
                        default -> throw new IllegalArgumentException("opaque step cannot use controlled port");
                    }
                }
                @Override public EffectClaim prepare(TerminalOperationPlan value) {
                    return store.prepareTerminalEffect(value, identity.subject() + "#" + initialEntityId + "@" + identity.nonce());
                }
                @Override public boolean enter(EffectClaim claim) { return store.enterEffect(claim); }
                @Override public void opaque(TerminalOperationPlan value) { TerminalLifecycle.runPlannedIntegration(entity, view, () -> current(value)); }
                @Override public void opaque(TerminalOperationPlan value, java.util.function.BooleanSupplier permitted) {
                    TerminalLifecycle.runPlannedIntegration(entity, view, permitted);
                }
                @Override public void finishUnknown(EffectClaim claim) { store.finishEffectUnknown(claim); }
            });
            if (!report.state().equals("REENTRANT")) {
                boolean approved = store.acceptOperationReport(report);
                remember(report, approved);
                if (approved) completion.track(entity, plan);
            }
            return report.opaqueEntered();
        }
    }
    private synchronized void countFenced() { if (fenced != Long.MAX_VALUE) fenced++; }
    private Identity readIdentity(Entity entity) {
        try(var observation=runtime.enforcement().beginObservation(1)) {
            if(observation==null||!dev.ronova.coremod.integrity.RawAccessorDispatch.matches(entity,Entity.class,
                dev.ronova.coremod.mixin.EntityStateAccessor.class))return null;
            var state=(dev.ronova.coremod.mixin.EntityStateAccessor)entity;
            var subject=state.ronova$getUuid();var rawLevel=state.ronova$getLevel();int id=state.ronova$getEntityId();
            if(subject==null||!(rawLevel instanceof ServerLevel level)||!level.getServer().isSameThread()
                ||level.getServer().getLevel(level.dimension())!=level
                ||dev.ronova.coremod.bootstrap.ServerRuntimeRegistry.getIfPresent(level.getServer())!=runtime)return null;
            var nonce=runtime.incarnationNonce(entity);
            if(!subject.equals(state.ronova$getUuid())||state.ronova$getLevel()!=level||state.ronova$getEntityId()!=id
                ||!observation.finish())return null;
            return new Identity(subject,level,nonce,id);
        } catch(RuntimeException|LinkageError unavailable) {return null;}
    }
    private OperationReport.StepReceipt stateActions(Entity entity,TerminalOperationPlan plan,
        java.util.function.BooleanSupplier permitted) {
        // Separate observations and write scopes keep unavailable health storage from vetoing removal.
        var life=stateAction(entity,plan,TerminalActionPolicy.Step.LIFE,permitted);
        var removal=stateAction(entity,plan,TerminalActionPolicy.Step.REMOVAL,permitted);
        var status=life.status()==OperationReport.Status.FENCED||removal.status()==OperationReport.Status.FENCED
            ?OperationReport.Status.FENCED
            :life.status()==OperationReport.Status.UNAVAILABLE||removal.status()==OperationReport.Status.UNAVAILABLE
                ?OperationReport.Status.UNAVAILABLE
                :life.status()==OperationReport.Status.CONVERGED&&removal.status()==OperationReport.Status.CONVERGED
                    ?OperationReport.Status.CONVERGED:OperationReport.Status.DISPATCHED;
        if(status==OperationReport.Status.CONVERGED||status==OperationReport.Status.DISPATCHED) {
            // Separate successful writes do not prove their combined postcondition is still present.
            status=OperationReport.Status.DISPATCHED;
            try(var observation=runtime.enforcement().beginObservation(0x8063)) {
                if(observation!=null&&permitted.getAsBoolean()&&TerminalStateSink.matches(entity)
                    &&permitted.getAsBoolean()&&observation.finish())status=OperationReport.Status.CONVERGED;
            } catch(RuntimeException|LinkageError unavailable) {status=OperationReport.Status.DISPATCHED;}
        }
        String evidence="life="+life.status()+":"+life.evidence()+";removal="+removal.status()+":"+removal.evidence();
        var writeSets=java.util.stream.Stream.concat(life.writeSets().stream(),removal.writeSets().stream()).toList();
        return new OperationReport.StepReceipt(TerminalOperationPlan.Step.TERMINAL_STATE,status,
            evidence.substring(0,Math.min(256,evidence.length())),writeSets);
    }
    private OperationReport.StepReceipt stateAction(Entity entity,TerminalOperationPlan plan,TerminalActionPolicy.Step action,
        java.util.function.BooleanSupplier permitted) {
        var result=writeStateAction(entity,plan,action,permitted);
        if(result.status()==OperationReport.Status.DISPATCHED||result.status()==OperationReport.Status.CONVERGED)
            stateFeedback.follow(entity,plan,action);
        return result;
    }
    private OperationReport.StepReceipt writeStateAction(Entity entity,TerminalOperationPlan plan,TerminalActionPolicy.Step action,
        java.util.function.BooleanSupplier permitted) {
        var policy=runtime.strengthPolicy();
        // Check requested settings first; actual boundary evidence is acquired below before any write.
        var requested=TerminalActionPolicy.resolve(policy,action,true);
        if(!requested.permitted())return new OperationReport.StepReceipt(TerminalOperationPlan.Step.TERMINAL_STATE,
            OperationReport.Status.UNAVAILABLE,requested.reason());
        return observedStep(plan,TerminalOperationPlan.Step.TERMINAL_STATE,action.mask(),permitted,gate->{
            var sink=action==TerminalActionPolicy.Step.LIFE?TerminalStateSink.LIFE_SINK:TerminalStateSink.REMOVAL_SINK;
            try(var writeSet=WriteSetLease.forPlan(plan,gate,sink)) {
                var result=TerminalStateSink.reassert(entity,plan,registeredStateLevel(entity,plan),writeSet,
                    action==TerminalActionPolicy.Step.LIFE,action==TerminalActionPolicy.Step.REMOVAL);
                var assessment=writeSet.assessment();
                if((result.status()==OperationReport.Status.DISPATCHED||result.status()==OperationReport.Status.CONVERGED)&&!assessment.exact())
                    return new OperationReport.StepReceipt(TerminalOperationPlan.Step.TERMINAL_STATE,OperationReport.Status.FENCED,
                        "write-set-inexact;"+assessment.summary());
                String writeEvidence=writeSet.evidence().stream()
                    .filter(value->value.outcome()==WriteSinkEvidence.Outcome.ACCEPTED).map(WriteSinkEvidence::evidenceHash)
                    .findFirst().orElse("none");
                String evidence=result.evidence()+";ws="+(assessment.exact()?"exact":"not-consumed")+";wsh="+writeEvidence;
                String bounded=evidence.substring(0,Math.min(256,evidence.length()));
                if(writeSet.evidence().isEmpty())return new OperationReport.StepReceipt(result.step(),result.status(),bounded);
                return new OperationReport.StepReceipt(result.step(),result.status(),bounded,writeSet.receipt());
            }
        });
    }
    private ServerLevel registeredStateLevel(Entity entity,TerminalOperationPlan plan) {
        if(!dev.ronova.coremod.integrity.RawAccessorDispatch.matches(entity,Entity.class,
            dev.ronova.coremod.mixin.EntityStateAccessor.class))return null;
        var state=(dev.ronova.coremod.mixin.EntityStateAccessor)entity;
        if(!plan.subjectKey().subjectId().equals(state.ronova$getUuid())
            ||!(state.ronova$getLevel() instanceof ServerLevel level)||!level.getServer().isSameThread()
            ||level.getServer().getLevel(level.dimension())!=level
            ||dev.ronova.coremod.bootstrap.ServerRuntimeRegistry.getIfPresent(level.getServer())!=runtime)return null;
        return level;
    }
    private boolean currentStateTarget(Entity entity,TerminalOperationPlan plan) {
        return registeredStateLevel(entity,plan)!=null&&runtime.incarnationNonce(entity).equals(plan.physicalNonce())
            &&runtime.durableStore()!=null&&runtime.durableStore().validateTerminalPlan(plan);
    }
    OperationReport.StepReceipt recheckRegisteredState(Entity entity,TerminalOperationPlan plan,TerminalActionPolicy.Step action) {
        if(action!=TerminalActionPolicy.Step.LIFE&&action!=TerminalActionPolicy.Step.REMOVAL)throw new IllegalArgumentException("not a state action");
        return observedStep(plan,TerminalOperationPlan.Step.TERMINAL_STATE,action.mask(),()->currentStateTarget(entity,plan),gate->{
            if(!gate.getAsBoolean())return new OperationReport.StepReceipt(TerminalOperationPlan.Step.TERMINAL_STATE,
                OperationReport.Status.FENCED,"state-feedback-revoked");
            Boolean matches;
            if(action==TerminalActionPolicy.Step.LIFE) {
                Float health=TerminalStateSink.observeLife(entity);matches=health==null?null:Float.valueOf(0.0F).equals(health);
            } else matches=TerminalStateSink.observeRemoval(entity);
            if(matches==null)return new OperationReport.StepReceipt(TerminalOperationPlan.Step.TERMINAL_STATE,
                OperationReport.Status.UNAVAILABLE,"state-feedback-binding-unverified");
            return new OperationReport.StepReceipt(TerminalOperationPlan.Step.TERMINAL_STATE,
                matches?OperationReport.Status.CONVERGED:OperationReport.Status.DISPATCHED,
                matches?"registered-state-held":"registered-state-rebounded");
        });
    }
    OperationReport.StepReceipt reassertRegisteredState(Entity entity,TerminalOperationPlan plan,TerminalActionPolicy.Step action) {
        return writeStateAction(entity,plan,action,()->currentStateTarget(entity,plan));
    }
    public void maintainStateFeedback(net.minecraft.server.MinecraftServer server,WorkBudget budget){
        stateFeedback.pulse(server,budget,this);
    }
    public String stateFeedbackSummary(){return stateFeedback.summary();}
    private OperationReport.StepReceipt observedStep(TerminalOperationPlan plan,TerminalOperationPlan.Step step,int mask,
        java.util.function.BooleanSupplier permitted,
        java.util.function.Function<java.util.function.BooleanSupplier,OperationReport.StepReceipt> operation) {
        try(var observation=runtime.enforcement().beginObservation(mask)) {
            if(observation==null)return new OperationReport.StepReceipt(step,OperationReport.Status.UNAVAILABLE,
                "registered-execution-observation-unavailable");
            try(var rootLease=runtime.durableStore().acquireTerminalWrite(plan)) {
                if(rootLease==null)return new OperationReport.StepReceipt(step,OperationReport.Status.FENCED,"root-write-step-unavailable");
                OperationReport.StepReceipt result;
                try {result=operation.apply(()->rootLease.valid(plan)&&permitted.getAsBoolean()&&observation.stillCurrent());}
                catch(RuntimeException unavailable) {result=new OperationReport.StepReceipt(step,OperationReport.Status.UNAVAILABLE,
                    "registered-step-failed:"+unavailable.getClass().getSimpleName());}
                if(!rootLease.valid(plan)||!permitted.getAsBoolean())return new OperationReport.StepReceipt(step,OperationReport.Status.FENCED,"root-write-step-expired");
                // Root commit exclusion and Native observation are separate guarantees.
                // This read-back cannot roll back a write or exclude arbitrary native/data mutations.
                if(!observation.finish())return new OperationReport.StepReceipt(step,OperationReport.Status.FENCED,
                    "registered-execution-observation-"+observation.state().name().toLowerCase(java.util.Locale.ROOT));
                for(var writeSet:result.writeSets())if(!runtime.durableStore().registerWriteSetReceipt(plan,rootLease,writeSet))
                    return new OperationReport.StepReceipt(step,OperationReport.Status.FENCED,"write-set-receipt-provenance-rejected");
                return result;
            }
        }
    }
    private synchronized Participation participation(TerminalOperationPlan plan,net.minecraft.server.MinecraftServer server) {
        var existing=participants.get(plan.effectKey());
        if(existing!=null) {
            var previous=existing.tracker().contract();
            if(!previous.plan().equals(plan)||!previous.epoch().equals(runtime.durableStore().epoch())) {
                var refreshed=runtime.durableStore().refreshClientParticipation(previous,plan);
                if(refreshed==null)return null;
                existing.tracker().refresh(refreshed,Integer.toUnsignedLong(server.getTickCount()));
            }
            return existing;
        }
        participants.entrySet().removeIf(entry->{var old=entry.getValue().tracker().contract().plan();
            var current=runtime.durableStore().subjectQualification(old.subjectKey());
            if(current!=null&&current.terminal()&&current.targetEpoch().equals(old.qualification().targetEpoch()))return false;
            entry.getValue().reservation().close();return true;});
        if(participants.size()>=128)return null;
        var endpoints=dev.ronova.coremod.network.QuarantineSyncService.participationEndpoints(server);
        if(endpoints.size()>64)return null;
        var reserved=runtime.resources().reserve(Map.of(HostResourceDomain.JAVA_HEAP,4096L+256L*endpoints.size()),HostResourceGovernor.Priority.RECOVERY);
        if(!reserved.granted())return null;
        boolean retained=false;
        try {
            var contract=runtime.durableStore().approveClientParticipation(plan,endpoints);
            if(contract==null)return null;
            var created=new Participation(new dev.ronova.coremod.network.ClientParticipationTracker(contract),reserved.reservation());
            participants.put(plan.effectKey(),created);retained=true;return created;
        } finally {if(!retained)reserved.reservation().close();}
    }
    private synchronized void remember(OperationReport report, boolean approved) {
        runtime.obligations().plan(report,approved);
        if (reports.size() >= 32) reports.removeFirst(); reports.addLast(new ReportedOperation(report, approved));
        if (approved) { if (accepted != Long.MAX_VALUE) accepted++; } else countFenced();
    }
    public synchronized List<ReportedOperation> reports() { return List.copyOf(reports); }
    public synchronized long acceptedReports() { return accepted; }
    public synchronized long fencedPlans() { return fenced; }
    public TerminalCompletionMonitor completion() { return completion; }
    public TerminalIndexRepair indices() { return indices; }
    public void observeCompletion(net.minecraft.server.MinecraftServer server,WorkBudget budget) {
        if((server.getTickCount()&1)==0) { pulseParticipants(server,budget);completion.pulse(server,budget); }
        else { completion.pulse(server,budget);pulseParticipants(server,budget); }
    }
    private synchronized void pulseParticipants(net.minecraft.server.MinecraftServer server,WorkBudget budget) {
        int processed=0,count=Math.min(16,participants.size());
        try {
            for(int i=0;i<count&&budget.tryConsumeCount(1);i++) {
                var key=participants.keySet().iterator().next();var value=participants.remove(key);processed++;
                var old=value.tracker().contract();
                var plan=runtime.durableStore().approveTerminalPlan(old.plan().subjectKey().subjectId(),old.plan().physicalNonce());
                if(plan==null||!plan.effectKey().equals(key)) {
                    var qualification=runtime.durableStore().subjectQualification(old.plan().subjectKey());
                    if(qualification!=null&&qualification.terminal()&&qualification.targetEpoch().equals(old.plan().qualification().targetEpoch())) {
                        participants.put(key,value);
                        value.tracker().sample(member->false,Integer.toUnsignedLong(server.getTickCount()));
                    } else value.reservation().close();
                    continue;
                }
                participants.put(key,value);
                if(!old.plan().equals(plan)||!old.epoch().equals(runtime.durableStore().epoch())) {
                    var updated=runtime.durableStore().refreshClientParticipation(old,plan);
                    if(updated==null)continue;
                    value.tracker().refresh(updated,Integer.toUnsignedLong(server.getTickCount()));
                }
                var contract=value.tracker().contract();
                dev.ronova.coremod.network.QuarantineSyncService.ensureParticipationImage(server,contract);
                value.tracker().sample(member->dev.ronova.coremod.network.QuarantineSyncService.confirmsParticipation(server,contract,member),
                    Integer.toUnsignedLong(server.getTickCount()));
            }
        } finally {if(processed>0)budget.finish(processed,0);}
    }
    public synchronized void clear() { reports.clear(); dispatched.clear();participants.values().forEach(p->p.reservation().close());participants.clear(); completion.clear(); indices.clear();stateFeedback.clear(); }
}
