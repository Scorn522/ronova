package dev.ronova.coremod.observation;

import java.util.Objects;

/** Consecutive, already receiver-verified events for one task. Never spans another
 * task, a copy event, a different parent/scheduler, or a changed proof generation.
 * Only exactly paired exits become completed counts; one open token is retained. */
final class TaskEventRun {
    final Object task,parent;
    final long changes,dropped;
    Object scheduler;
    long completed,submissions,execution,events=1;
    boolean running=true,hasScheduler;
    /**
     * Ordered queue effect of this run, accumulated as events arrive. {@code unmatchedEntries} counts
     * entries that found no submission left to consume; {@code localPending} is the run's own
     * leftover submissions. Together they are the composable transform the graph applies to whatever
     * depth it already held — a single net number cannot express this, because consuming an earlier
     * submission and queueing a later one cancel out while meaning opposite things.
     */
    long unmatchedEntries=1,localPending;
    TaskEventRun(Object task,Object parent,long execution,long changes,long dropped) {
        this.task=Objects.requireNonNull(task);this.parent=parent==task?null:parent;
        if(execution<1)throw new IllegalArgumentException("invalid execution token");
        this.execution=execution;this.changes=changes;this.dropped=dropped;
    }
    boolean append(int kind,Object task,Object scheduler,Object parent,long token,long changes,long dropped) {
        if(this.task!=task||this.parent!=(parent==task?null:parent)||this.changes!=changes||this.dropped!=dropped||events==Long.MAX_VALUE)return false;
        if(kind==1) {
            if(running||token<=execution)return false;
            execution=token;running=true;consumeOrQueue();
        } else if(kind==2) {
            if(!running||token!=execution||completed==Long.MAX_VALUE)return false;
            completed++;running=false;
        } else if(kind==3) {
            if(token!=0||submissions==Long.MAX_VALUE||hasScheduler&&this.scheduler!=scheduler)return false;
            this.scheduler=scheduler;hasScheduler=true;submissions++;localPending++;
        } else return false;
        events++;return true;
    }
    /** An entry takes the oldest unmatched submission still available inside this run. */
    private void consumeOrQueue(){if(localPending>0)localPending--;else unmatchedEntries++;}
    /**
     * Merges the run as one ordered batch. Every entry it covers — the completed pairs plus the
     * still-open tail token — is counted here, and the queue effect travels as an ordered transform
     * rather than as a total. The open token is then registered without counting an entry again and
     * without touching the queue depth, which the transform already accounted for.
     */
    void mergeInto(TaskExecutionGraph graph) {
        graph.observedCompletedRun(task,scheduler,parent,Math.addExact(completed,running?1:0),completed,submissions,
            new TaskExecutionGraph.QueueTransform(unmatchedEntries,localPending));
        if(running)graph.registerOpenExecution(task,execution,parent);
    }
}
