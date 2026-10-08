package dev.ronova.coremod.observation;

import java.lang.ref.WeakReference;
import java.util.*;

/** Volatile execution evidence. Root alone owns durable intents and permissions. No foreign methods are invoked. */
public final class TaskExecutionGraph {
    public enum State { SUBMISSION_UNCONFIRMED, EXECUTING, EXIT_OBSERVED, CANCEL_REQUESTED, CANCEL_RETURNED, UNKNOWN }
    /** Queue disposition kept separate from execution exit: a queued task that never ran must not claim EXECUTION_EXITED. */
    public enum QueueState { UNOBSERVED_SUBMISSION, SUBMITTED_NEVER_ENTERED, ENTERED, PRECLUDED_NEVER_ENTERED, EVIDENCE_LOST }
    /**
     * Queue effect of one compressed event run, derived by scanning that run in order: how many of
     * its entries found no submission to consume, and how many of its submissions no later entry
     * consumed. Cumulative counts cannot express this — an entry that consumes an earlier submission
     * and a submission that follows the last entry cancel out arithmetically while meaning opposite
     * things, and historical entry headroom then hides the queued epoch entirely.
     */
    record QueueTransform(long unmatchedEntries,long unmatchedSubmissions) {
        QueueTransform {
            if(unmatchedEntries<0||unmatchedSubmissions<0)throw new IllegalArgumentException("negative queue transform");
        }
        /** Applies the run in order: its own leftover entries drain earlier submissions, then its leftovers queue. */
        long apply(long pending){return Math.addExact(Math.max(0L,pending-unmatchedEntries),unmatchedSubmissions);}
    }
    public record SourceUse(dev.ronova.coremod.root.api.SubjectQualification qualification,String locator,long sourceVersion,int copyDepth) {
        public SourceUse {Objects.requireNonNull(qualification);locator=dev.ronova.coremod.root.api.EffectRecord.checkedHash(locator);if(sourceVersion<1||copyDepth<0)throw new IllegalArgumentException("invalid source version/depth");}
    }
    public record View(UUID registration,State state,long submissions,long entries,long exits,int activeExecutions,
                       long pendingSubmissions,long precludedSubmissions,
                       Set<UUID> parents,Set<UUID> children,boolean cancellationReturned,boolean cancellationAccepted,
                       boolean taskReachable,boolean evidenceLost,long revision,Set<SourceUse> sources) {
        public View { parents=Set.copyOf(parents);children=Set.copyOf(children);sources=Set.copyOf(sources); }
        /**
         * Every entered execution has exited, and no observed submission is still waiting to enter.
         *
         * <p>{@code entries==exits} alone is not enough, and neither is any comparison of cumulative
         * totals: {@code submitted then entered} and {@code entered then submitted again} produce
         * identical counts, while {@code entries} may legitimately exceed {@code submissions} (a bare
         * {@code entered} has no submission, and a merged run adds paired entries), so headroom in the
         * totals hides a genuinely queued epoch. Only {@code pendingSubmissions} — the explicit count
         * of observed submissions not yet matched by an entry — decides it.</p>
         */
        public boolean executionsExited(){return !evidenceLost&&entries>0&&activeExecutions==0&&entries==exits&&pendingSubmissions==0;}
        /**
         * Observed submission still waiting to enter. This is intentionally not an exit fact:
         * a wrapper that is cancelled while still queued never ran, so it cannot discharge
         * EXECUTION_EXITED, and it cannot discharge a resource or descendant obligation either.
         */
        public QueueState queueState(){
            if(evidenceLost)return QueueState.EVIDENCE_LOST;
            if(pendingSubmissions>0)return activeExecutions>0?QueueState.ENTERED:QueueState.SUBMITTED_NEVER_ENTERED;
            if(activeExecutions>0)return QueueState.ENTERED;
            if(precludedSubmissions>0)return QueueState.PRECLUDED_NEVER_ENTERED;
            return entries>0?QueueState.ENTERED:QueueState.UNOBSERVED_SUBMISSION;
        }
        /**
         * True only when a submission was actually observed and the current epoch has not entered.
         * This is deliberately about the <em>current</em> epoch rather than the object's whole
         * history: a task that ran once and was resubmitted is queued again, and a settlement path
         * gated on historical {@code entries==0} could never reach it.
         */
        public boolean queuedNeverEntered(){return queueState()==QueueState.SUBMITTED_NEVER_ENTERED;}
        /**
         * An observed submission no entry has matched, regardless of whether some other epoch is
         * currently executing. A periodic task queues its next epoch from inside the run that is
         * still executing, so the active set says nothing about whether a queued epoch exists: with
         * a bare {@code activeExecutions==0} requirement the queued epoch stays hidden, and a
         * cancellation accepted for this task would leave it outstanding forever.
         */
        public boolean queuedEpochOutstanding(){return !evidenceLost&&pendingSubmissions>0;}
        /**
         * A queue epoch an accepted cancel settled without an entry. This settles that queue epoch,
         * but it does not discharge a different epoch that is still executing. Node/subtree closure
         * therefore combines this fact with the entered-execution counters rather than treating it as
         * a blanket substitute for {@link #executionsExited()}.
         */
        public boolean queuedEpochPrecluded(){return !evidenceLost&&precludedSubmissions>0;}
        // No queue-publication, descendant, resource or storage success is inferred from this observation.
    }
    private static final int MAX_RELATIONS=64,MAX_EXECUTIONS=64;
    private final int capacity;
    private final Map<Identity,Node> tasks=new HashMap<>();
    private final Map<UUID,Node> registrations=new LinkedHashMap<>();
    private final ArrayDeque<UUID> retirementScan=new ArrayDeque<>();
    private long revision,losses;
    private static final class Identity extends WeakReference<Object> {
        private final int hash;
        Identity(Object value){super(Objects.requireNonNull(value));hash=System.identityHashCode(value);}
        @Override public int hashCode(){return hash;}
        @Override public boolean equals(Object value){return this==value||value instanceof Identity other&&get()!=null&&get()==other.get();}
    }
    private static final class Node {
        final UUID registration=UUID.randomUUID();
        final Identity task;
        final WeakReference<ClassLoader> loader;
        final Set<UUID> parents=new LinkedHashSet<>(),children=new LinkedHashSet<>();
        final Set<SourceUse> sources=new LinkedHashSet<>();
        final Set<Long> active=new HashSet<>();
        final Map<UUID,Boolean> cancellationResults=new LinkedHashMap<>();
        final Set<Identity> schedulers=new LinkedHashSet<>();
        boolean missingScheduler;
        long submissions,entries,exits,revision;
        /** Observed submissions not yet matched by an entered execution: the current queue epoch depth. */
        long pendingSubmissions;
        /**
         * Submissions an accepted cancel settled without an entry. They are moved here rather than
         * dropped, so the queue epoch keeps a state of its own instead of decaying into "never
         * observed a submission" — a settlement that erases its own evidence cannot discharge closure.
         */
        long precludedSubmissions;
        boolean lost,cancelRequested,cancelReturned,cancelAccepted;
        Node(Object value){task=new Identity(value);loader=new WeakReference<>(value.getClass().getClassLoader());}
    }
    public TaskExecutionGraph(int capacity){if(capacity<1||capacity>4096)throw new IllegalArgumentException("invalid task capacity");this.capacity=capacity;}
    private Node node(Object task) {
        if(task==null)return null;
        Node found=tasks.get(new Identity(task));if(found!=null)return found;
        if(tasks.size()>=capacity){lost();return null;}
        Node created=new Node(task);tasks.put(created.task,created);registrations.put(created.registration,created);retirementScan.addLast(created.registration);touch(created);return created;
    }
    /** Called by the server merge only after checking the production boundary event. */
    synchronized UUID submitted(Object task,Object scheduler,Object parent) {
        Node target=node(task);if(target==null)return null;
        target.submissions=Math.addExact(target.submissions,1);
        target.pendingSubmissions=Math.addExact(target.pendingSubmissions,1);
        rememberScheduler(target,scheduler);
        if(parent!=null&&parent!=task){Node owner=node(parent);if(owner==null)target.lost=true;else relate(owner,target);}
        touch(target);return target.registration;
    }
    synchronized UUID entered(Object task,long execution,Object parent) {
        if(execution<1)throw new IllegalArgumentException("invalid execution token");
        Node target=node(task);if(target==null)return null;
        if(target.active.size()>=MAX_EXECUTIONS||!target.active.add(execution)){target.lost=true;lost();}
        else {
            target.entries=Math.addExact(target.entries,1);
            // This entry matches one observed submission. A bare entered has none to match and is
            // therefore not evidence of a settled queue.
            if(target.pendingSubmissions>0)target.pendingSubmissions--;
        }
        if(parent!=null&&parent!=task){Node owner=node(parent);if(owner==null)target.lost=true;else relate(owner,target);}
        touch(target);return target.registration;
    }
    synchronized void exited(Object task,long execution) {
        Node target=task==null?null:tasks.get(new Identity(task));
        if(target==null){lost();return;}
        if(!target.active.remove(execution)){target.lost=true;lost();}
        else target.exits=Math.addExact(target.exits,1);
        touch(target);
    }
    /**
     * Merges one receiver-verified consecutive run, which contains only paired executions; it does
     * not remove other open tokens or settle publication, descendants or resources.
     *
     * <p>{@code entries} counts every entry the run covers — its completed pairs plus the still-open
     * tail token. {@code transform} carries the run's ordered queue effect; the counts alone cannot,
     * which is why this method no longer derives the pending depth from them.</p>
     */
    synchronized void observedCompletedRun(Object task,Object scheduler,Object parent,
        long entries,long completed,long submissions,QueueTransform transform) {
        if(entries<0||completed<0||submissions<0||completed>entries)
            throw new IllegalArgumentException("invalid observed run counts");
        Node target=node(task);if(target==null)return;
        target.entries=Math.addExact(target.entries,entries);target.exits=Math.addExact(target.exits,completed);
        target.submissions=Math.addExact(target.submissions,submissions);
        target.pendingSubmissions=transform.apply(target.pendingSubmissions);
        if(submissions>0)rememberScheduler(target,scheduler);
        if(parent!=null&&parent!=task){Node owner=node(parent);if(owner==null)target.lost=true;else relate(owner,target);}
        touch(target);
    }
    /**
     * Registers a still-open execution token whose entry was already counted by a merged run. It
     * never counts an entry again and never rewrites the run's last-event reading.
     */
    synchronized void registerOpenExecution(Object task,long execution,Object parent) {
        if(execution<1)throw new IllegalArgumentException("invalid execution token");
        Node target=node(task);if(target==null)return;
        if(target.active.size()>=MAX_EXECUTIONS||!target.active.add(execution)){target.lost=true;lost();}
        if(parent!=null&&parent!=task){Node owner=node(parent);if(owner==null)target.lost=true;else relate(owner,target);}
        touch(target);
    }
    private void relate(Node parent,Node child) {
        if(parent.children.contains(child.registration))return;
        if(parent.children.size()>=MAX_RELATIONS||child.parents.size()>=MAX_RELATIONS){parent.lost=child.lost=true;lost();return;}
        parent.children.add(child.registration);child.parents.add(parent.registration);touch(parent);
    }
    private void rememberScheduler(Node node,Object scheduler) {
        if(scheduler==null){node.missingScheduler=true;return;}
        var identity=new Identity(scheduler);if(node.schedulers.contains(identity))return;
        if(node.schedulers.size()>=MAX_RELATIONS){node.lost=true;lost();return;}
        node.schedulers.add(identity);
    }
    /** All observed scheduler identities, not merely the most recent submission's receiver. */
    public record Schedulers(List<Object> values,int observed,boolean complete) {
        public Schedulers {values=List.copyOf(values);}
    }
    public synchronized Schedulers schedulers(UUID registration) {
        var node=registrations.get(registration);if(node==null)return new Schedulers(List.of(),0,false);
        var values=node.schedulers.stream().map(Identity::get).filter(Objects::nonNull).toList();
        return new Schedulers(values,node.schedulers.size(),!node.lost&&!node.missingScheduler&&values.size()==node.schedulers.size());
    }
    /** A bounded observed graph snapshot. Even true observed exits do not prove queues or future submissions closed. */
    public record Descendants(List<View> nodes,boolean complete,boolean observedExecutionsExited,long revision) {
        public Descendants {nodes=List.copyOf(nodes);}
    }
    public synchronized Descendants descendants(UUID registration,int limit) {
        if(limit<1||limit>64)throw new IllegalArgumentException("invalid descendant snapshot budget");
        var views=new ArrayList<View>();var colors=new HashMap<UUID,Integer>();
        boolean complete=walkChildren(registration,limit,colors,views);
        return new Descendants(views,complete,complete&&!views.isEmpty()&&views.stream().allMatch(View::executionsExited),revision);
    }
    /**
     * True only when every observed successor of this registration is settled: the subtree snapshot
     * is complete, no node was lost, scheduler evidence is complete, and each node has either entered
     * and exited an execution completely, or had its queued epoch explicitly precluded by an accepted
     * cancel. A merely queued node does NOT count — it may still run.
     * An unobserved submission, a queued or executing node, an unknown scheduler, or any truncation
     * leaves this false: a parent exit never discharges its children's obligations.
     */
    public synchronized boolean successorsClosed(UUID registration,int limit) {
        // A registration this graph never observed has no subtree to discharge.
        if(registration==null||!registrations.containsKey(registration))return false;
        var snapshot=descendants(registration,limit);
        if(!snapshot.complete()||losses!=0)return false;
        for(var node:snapshot.nodes()) {
            if(node.evidenceLost())return false;
            // Scheduler evidence is demanded before any settled reading, not only for queued nodes.
            // A submission whose scheduler was never seen cannot be trusted to stay unreachable, and
            // SUCCESSORS_CLOSED is a monotone fact that cannot be withdrawn once recorded, so an
            // observed exit does not make unknown scheduler evidence acceptable. A node with no
            // submission at all is unaffected: it has no missing scheduler and its set is complete.
            if(!schedulers(node.registration()).complete())return false;
            // Only an observed exit or an explicit cancel settlement closes a node. A merely queued
            // successor does NOT: it can still run, so admitting it would make this predicate read
            // true -> false -> true, and SUCCESSORS_CLOSED is a monotone durable fact that cannot be
            // withdrawn once recorded. "Not yet run" is not "closed".
            if(node.executionsExited())continue;
            // PRECLUDED can settle a node only when no execution epoch is or ever was left
            // outstanding. In the periodic-overlap case a later queued epoch may be precluded while
            // the current epoch is still running; closing on the preclusion alone would persist a
            // monotone SUCCESSORS_CLOSED before that execution actually exits.
            if(node.queuedEpochPrecluded()&&node.entries()==0&&node.activeExecutions()==0&&node.pendingSubmissions()==0)continue;
            return false;
        }
        return true;
    }
    private boolean walkChildren(UUID id,int limit,Map<UUID,Integer> colors,List<View> views) {
        Integer color=colors.get(id);if(color!=null)return color==2;
        var node=registrations.get(id);if(node==null||node.lost||views.size()>=limit)return false;
        colors.put(id,1);views.add(view(node));
        for(UUID child:node.children)if(!walkChildren(child,limit,colors,views))return false;
        colors.put(id,2);return true;
    }
    /** Records the executor's acknowledged attempt; it never grants an action permit. */
    synchronized void cancellationRequested(UUID registration,UUID acknowledgedAttempt) {
        Node target=registrations.get(registration);if(target==null){lost();return;}
        Objects.requireNonNull(acknowledgedAttempt);
        if(target.cancellationResults.containsKey(acknowledgedAttempt))return;
        if(target.cancellationResults.size()>=MAX_RELATIONS){target.lost=true;lost();return;}
        target.cancellationResults.put(acknowledgedAttempt,null);target.cancelRequested=true;touch(target);
    }
    /**
     * Records the executor attempt result only. Queue settlement is produced by the observed CANCEL
     * event that shares the FutureTask CAS gate with ENTER/SUBMIT publication; this method must not
     * infer PRECLUDED from a later Graph snapshot.
     */
    synchronized void cancellationReturned(UUID registration,UUID acknowledgedAttempt,boolean accepted) {
        Node target=registrations.get(registration);
        if(target==null||!target.cancellationResults.containsKey(acknowledgedAttempt)){lost();return;}
        Boolean old=target.cancellationResults.get(acknowledgedAttempt);
        if(old!=null){if(old!=accepted){target.lost=true;lost();}return;}
        target.cancellationResults.put(acknowledgedAttempt,accepted);target.cancelReturned=true;target.cancelAccepted|=accepted;
        touch(target);
    }
    /**
     * Settles only queue epochs actually observed as precluded by a successful FutureTask state CAS.
     * The CANCEL event is ordered with ENTER/SUBMIT under BootstrapTaskHooks' per-task gate. A running
     * epoch remains independent and still has to contribute its EXIT before successor closure.
     */
    synchronized void queuePrecluded(Object task) {
        Node target=task==null?null:tasks.get(new Identity(task));
        if(target==null){lost();return;}
        if(target.pendingSubmissions==0)return;
        target.precludedSubmissions=Math.addExact(target.precludedSubmissions,target.pendingSubmissions);
        target.pendingSubmissions=0;touch(target);
    }
    synchronized void evidenceLost(){lost();for(Node node:registrations.values()){node.lost=true;touch(node);}}
    synchronized void sourceUsed(Object task,SourceUse source) {
        Node node=node(task);if(node==null)return;
        if(node.sources.contains(source))return;
        var prior=node.sources.stream().filter(value->value.qualification.equals(source.qualification)&&value.locator.equals(source.locator)
            &&value.sourceVersion==source.sourceVersion).findFirst().orElse(null);
        if(prior!=null){if(prior.copyDepth>=source.copyDepth)return;node.sources.remove(prior);}
        if(node.sources.size()>=MAX_RELATIONS){node.lost=true;lost();return;}
        node.sources.add(source);touch(node);
    }
    public synchronized View view(UUID registration) {
        Node node=registrations.get(registration);return node==null?null:view(node);
    }
    public synchronized View view(Object task){Node node=task==null?null:tasks.get(new Identity(task));return node==null?null:view(node);}
    public synchronized List<View> page(int offset,int limit) {
        if(offset<0||limit<1||limit>64)throw new IllegalArgumentException("invalid task page");
        return registrations.values().stream().skip(offset).limit(limit).map(this::view).toList();
    }
    private View view(Node node) {
        State state=node.lost?State.UNKNOWN:!node.active.isEmpty()?State.EXECUTING:node.cancelReturned?State.CANCEL_RETURNED
            :node.cancelRequested?State.CANCEL_REQUESTED:node.entries>0?State.EXIT_OBSERVED:State.SUBMISSION_UNCONFIRMED;
        return new View(node.registration,state,node.submissions,node.entries,node.exits,node.active.size(),node.pendingSubmissions,node.precludedSubmissions,node.parents,node.children,
            node.cancelReturned,node.cancelAccepted,node.task.get()!=null,node.lost,node.revision,node.sources);
    }
    public synchronized Object exactTask(UUID registration){Node node=registrations.get(registration);return node==null?null:node.task.get();}
    public synchronized long losses(){return losses;}
    public synchronized int size(){return registrations.size();}
    public record Ancestry(boolean complete,boolean relatedParent,long revision) {}
    /** Bounded traversal of actual parent edges. A lost/missing/cyclic ancestry cannot authorize a shallow task action. */
    public synchronized Ancestry ancestry(UUID registration,SourceUse source) {
        Node node=registrations.get(registration);
        if(node==null||node.lost)return new Ancestry(false,false,revision);
        var colors=new HashMap<UUID,Integer>();boolean[] related={false};int[] remaining={64};
        boolean complete=walkParents(node,source,colors,related,remaining);
        return new Ancestry(complete,related[0],revision);
    }
    private boolean walkParents(Node node,SourceUse source,Map<UUID,Integer> colors,boolean[] related,int[] remaining) {
        Integer color=colors.get(node.registration);if(color!=null)return color==2;
        if(remaining[0]--==0||node.lost)return false;colors.put(node.registration,1);
        for(UUID id:node.parents) {
            Node parent=registrations.get(id);if(parent==null)return false;
            if(parent.sources.stream().anyMatch(value->value.qualification.equals(source.qualification)&&value.locator.equals(source.locator)
                &&value.sourceVersion==source.sourceVersion))related[0]=true;
            if(!walkParents(parent,source,colors,related,remaining))return false;
        }
        colors.put(node.registration,2);return true;
    }
    /** Reclaims unrelated, collected metadata only; this is never an exit or resource-disposal receipt. */
    synchronized void retireUnboundCollected(int allowance) {
        if(allowance<0||allowance>64)throw new IllegalArgumentException("invalid task retirement allowance");
        int count=Math.min(allowance,retirementScan.size());
        for(int i=0;i<count;i++) {
            UUID id=retirementScan.removeFirst();Node node=registrations.get(id);if(node==null)continue;
            if(node.task.get()==null&&node.sources.isEmpty()&&node.parents.isEmpty()&&node.children.isEmpty()
                &&node.active.isEmpty()&&!node.cancelRequested) {tasks.remove(node.task);registrations.remove(id);}
            else retirementScan.addLast(id);
        }
    }
    private void touch(Node node){node.revision=revision=Math.addExact(revision,1);}
    private void lost(){losses=Math.addExact(losses,1);}
}
