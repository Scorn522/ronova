package dev.ronova.coremod.network;

import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.root.api.DominionCommand;
import dev.ronova.coremod.util.WeakIdentityMap;
import net.minecraft.server.level.ServerPlayer;

/** GUI edits share the canonical Root command path and operator permission policy. */
public final class StrengthControlService {
    private static final WeakIdentityMap<ServerPlayer,Long> LAST_READ=new WeakIdentityMap<>();
    private static final WeakIdentityMap<ServerPlayer,Long> LAST_WRITE=new WeakIdentityMap<>();
    private StrengthControlService() {}
    public static void execute(ServerPlayer player,StrengthControl.Request request) {
        if(player==null||player.server==null||!player.server.isSameThread())return;
        var stream=QuarantineSyncService.streamStatus(player);var c=request.context();
        if(stream==null||!stream.realmId().equals(c.realm())||!stream.streamId().equals(c.stream())||stream.contextRevision()!=c.generation())return;
        long tick=Integer.toUnsignedLong(player.server.getTickCount());
        var limiter=request.operation()==StrengthControl.Operation.READ?LAST_READ:LAST_WRITE;
        boolean throttled;
        synchronized(limiter){Long prior=limiter.get(player);throttled=prior!=null&&tick>=prior&&tick-prior<4;if(!throttled)limiter.put(player,tick);}
        if(throttled&&request.operation()==StrengthControl.Operation.READ)return;
        var runtime=ServerRuntimeRegistry.getIfPresent(player.server);if(runtime==null||runtime.durableStore()==null)return;
        boolean editable=player.hasPermissions(2);var outcome=StrengthControl.Outcome.SNAPSHOT;
        if(request.operation()!=StrengthControl.Operation.READ) {
            if(!editable)outcome=StrengthControl.Outcome.FORBIDDEN;
            else if(throttled)outcome=StrengthControl.Outcome.UNAVAILABLE;
            else if(runtime.durableStore().persistencePending())outcome=StrengthControl.Outcome.UNAVAILABLE;
            else {
                var current=runtime.durableStore().strengthPolicyConfiguration();
                if(current.revision().value()!=request.expectedRevision())outcome=StrengthControl.Outcome.STALE;
                else {
                    var next=StrengthControl.apply(request,current);
                    var receipt=runtime.commands().commit(new DominionCommand.ConfigureStrengthPolicy(request.requestId(),next));
                    outcome=receipt.committed()?StrengthControl.Outcome.SAVED:StrengthControl.Outcome.UNAVAILABLE;
                }
            }
        }
        var boundaries=new java.util.EnumMap<dev.ronova.coremod.policy.TerminalActionPolicy.Step,Boolean>(
            dev.ronova.coremod.policy.TerminalActionPolicy.Step.class);
        for(var step:java.util.List.of(dev.ronova.coremod.policy.TerminalActionPolicy.Step.LIFE,
            dev.ronova.coremod.policy.TerminalActionPolicy.Step.REMOVAL,dev.ronova.coremod.policy.TerminalActionPolicy.Step.REMOVED_RECORDS)) {
            try(var observation=runtime.enforcement().beginObservation(step.mask())) {
                boundaries.put(step,observation!=null&&observation.finish());
            }
        }
        var snapshot=StrengthControl.snapshot(request,runtime.strengthPolicy(),editable,outcome,boundaries);
        var reports=runtime.terminalPlans().reports();
        if(!reports.isEmpty()) {
            var last=reports.get(reports.size()-1);var report=last.report();
            var q=runtime.durableStore().subjectQualification(report.plan().subjectKey());
            boolean terminal=q!=null&&q.terminal()&&q.targetEpoch().equals(report.plan().qualification().targetEpoch());
            var completion=runtime.terminalPlans().completion().current(report.plan(),player.server);
            snapshot=snapshot.operation(terminal?StrengthControl.AttackOutcome.TERMINAL_COMMITTED:StrengthControl.AttackOutcome.CHANGED,
                report.state().equals("FENCED")||completion!=null&&completion.fenced()?StrengthControl.Execution.FENCED:StrengthControl.Execution.PARTIAL,
                terminal&&last.acceptedByRoot()&&completion!=null&&completion.complete());
            snapshot=snapshot.completion(completion==null?0:completion.revision(),completionDetail(completion));
        }
        String detail = snapshot.completionDetail();
        String source = StrengthStatusDetail.recovery(runtime.recoverySources().removedStaticRecords(),
            runtime.recoverySources().removedLoadedRecords(),
            runtime.recoverySources().clearedStaticSnapshots(),runtime.recoverySources().clearedLoadedSnapshots());
        String delivery = QuarantineSyncService.deliverySummary(player);
        snapshot = snapshot.completion(snapshot.completionRevision(),StrengthStatusDetail.compose(detail,source,delivery,
            runtime.terminalPlans().stateFeedbackSummary(),runtime.recoverySources().recordTaskCounters()));
        RonovaChannel.sendToPlayer(player,new StrengthControlPackets.SnapshotPacket(snapshot));
    }
    private static String completionDetail(dev.ronova.coremod.runtime.CompletionTracker.Evaluation evaluation) {
        if(evaluation==null)return "等待当前执行结果的观察";
        if(evaluation.complete())return "服务端结果通过持续观察";
        var labels=new java.util.ArrayList<String>();
        for(var condition:dev.ronova.coremod.runtime.CompletionTracker.Condition.values())if(evaluation.missing().contains(condition))
            labels.add(switch(condition) {
                case BODY_DISABLED->"实体状态";case WORLD_RECORDS->"世界记录";case OLD_EPOCH_REJECTED->"旧资格拦截";
                case RECOVERY_TASKS->"恢复任务";case REBUILD_ENTRYPOINTS->"重建入口";case SAVE_QUALIFICATION->"存档资格";
                case TRACKING->"追踪发布";case SERVER_PRESENCE->"服务端显示";case NO_PENDING_WORK->"未决工作";
            });
        if(evaluation.unresolvedEffect())labels.add("副作用结果");
        return labels.isEmpty()?"持续观察中 "+evaluation.stableTicks()+"/20 tick":"待确认："+String.join("、",labels);
    }
}
