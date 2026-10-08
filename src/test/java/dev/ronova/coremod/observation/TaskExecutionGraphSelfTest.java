package dev.ronova.coremod.observation;

import java.util.UUID;

public final class TaskExecutionGraphSelfTest {
    public static void main(String[] args) {
        var graph=new TaskExecutionGraph(8);Object scheduler=new Object(),parent=new Object(),child=new Object();
        UUID p=graph.submitted(parent,scheduler,null);
        require(graph.view(p).state()==TaskExecutionGraph.State.SUBMISSION_UNCONFIRMED,"submission attempt was treated as publication");
        graph.entered(parent,1,null);graph.entered(parent,2,null);
        UUID attempt=UUID.randomUUID();graph.cancellationRequested(p,attempt);graph.queuePrecluded(parent);graph.cancellationReturned(p,attempt,true);
        require(graph.view(p).activeExecutions()==2&&!graph.view(p).executionsExited(),"cancel result discharged running executions");
        UUID c=graph.submitted(child,scheduler,parent);graph.entered(child,3,parent);
        graph.exited(parent,1);require(!graph.view(p).executionsExited(),"one wrapper exit discharged concurrent execution");
        graph.exited(parent,2);
        require(graph.view(p).executionsExited()&&!graph.view(c).executionsExited(),"parent exit discharged child");
        require(graph.view(p).children().contains(c)&&graph.view(c).parents().contains(p),"actual parent edge missing");
        require(graph.descendants(p,8).complete()&&!graph.descendants(p,8).observedExecutionsExited(),"parent exit discharged an active descendant");
        require(!graph.descendants(p,1).complete(),"truncated descendants reported full coverage");
        graph.exited(child,3);graph.submitted(parent,scheduler,null);graph.entered(parent,4,null);
        require(p.equals(graph.view(parent).registration())&&!graph.view(p).executionsExited(),"periodic publication inherited old exit");
        graph.exited(parent,4);graph.exited(parent,4);
        require(graph.view(p).evidenceLost()&&!graph.view(p).executionsExited(),"duplicate exit accepted");
        Object hostile=new Object(){public boolean equals(Object value){throw new AssertionError("foreign equals invoked");}public int hashCode(){throw new AssertionError("foreign hash invoked");}};
        UUID h=graph.submitted(hostile,scheduler,null);require(graph.exactTask(h)==hostile,"task identity lookup failed");
        var full=new TaskExecutionGraph(1);full.submitted(parent,scheduler,null);
        require(full.submitted(child,scheduler,null)==null&&full.losses()==1,"capacity loss was hidden");
        var realm=new dev.ronova.coremod.contract.RealmId(UUID.randomUUID());
        var subject=new dev.ronova.coremod.contract.SubjectKey(realm,UUID.randomUUID());
        var qualification=new dev.ronova.coremod.root.api.SubjectQualification(new dev.ronova.coremod.root.api.TargetEpoch(subject,1),true);
        String locator=dev.ronova.coremod.root.api.EffectRecord.hash("task-source");
        graph.sourceUsed(hostile,new TaskExecutionGraph.SourceUse(qualification,locator,1,1));
        graph.sourceUsed(hostile,new TaskExecutionGraph.SourceUse(qualification,locator,1,3));
        graph.sourceUsed(hostile,new TaskExecutionGraph.SourceUse(qualification,locator,1,1));
        require(graph.view(h).sources().size()==1&&graph.view(h).sources().iterator().next().copyDepth()==3,"deeper use became shared or inherited shallow task permission");
        var ancestryGraph=new TaskExecutionGraph(8);Object ancestor=new Object(),left=new Object(),right=new Object(),leaf=new Object();
        UUID a=ancestryGraph.submitted(ancestor,scheduler,null);
        var use=new TaskExecutionGraph.SourceUse(qualification,locator,1,1);ancestryGraph.sourceUsed(ancestor,use);
        ancestryGraph.submitted(left,scheduler,ancestor);ancestryGraph.submitted(right,scheduler,ancestor);
        UUID l=ancestryGraph.submitted(leaf,scheduler,left);ancestryGraph.submitted(leaf,scheduler,right);
        require(ancestryGraph.ancestry(l,use).complete()&&ancestryGraph.ancestry(l,use).relatedParent(),"diamond ancestry lost the qualified parent");
        require(!ancestryGraph.ancestry(a,use).relatedParent(),"task used itself as a parent");
        ancestryGraph.submitted(ancestor,scheduler,leaf);
        require(!ancestryGraph.ancestry(l,use).complete(),"cyclic ancestry granted a shallow action");
        require(!ancestryGraph.descendants(a,8).complete(),"cyclic descendants reported closure");
        Object secondScheduler=new Object();graph.submitted(hostile,secondScheduler,null);
        var schedulers=graph.schedulers(h);
        require(schedulers.complete()&&schedulers.observed()==2&&schedulers.values().get(0)==scheduler
            &&schedulers.values().get(1)==secondScheduler,"resubmission erased a shared scheduler obligation");
        graph.submitted(hostile,null,null);
        require(!graph.schedulers(h).complete()&&graph.schedulers(h).observed()==2,"unobserved scheduler became proof of release");
        graph.evidenceLost();require(!graph.view(c).executionsExited(),"coverage loss preserved stale completion evidence");
        var batchGraph=new TaskExecutionGraph(8);Object recurring=new Object();
        var batch=new TaskEventRun(recurring,null,1,7,0);
        for(int i=1;i<=10_000;i++) {
            if(i>1)require(batch.append(1,recurring,null,null,i,7,0),"next consecutive execution rejected");
            require(batch.append(3,recurring,scheduler,recurring,0,7,0),"periodic self-submission did not normalize");
            require(batch.append(2,recurring,null,null,i,7,0),"paired exit rejected");
        }
        require(!batch.append(1,new Object(),null,null,10_001,7,0),"different task joined run");
        require(!batch.append(1,recurring,null,new Object(),10_001,7,0),"different parent joined run");
        require(!batch.append(1,recurring,null,null,10_001,8,0),"new proof generation joined run");
        require(!batch.append(1,recurring,null,null,10_001,7,1),"dropped evidence joined run");
        require(!batch.append(1,recurring,null,null,10_000,7,0),"duplicate completed token joined run");
        require(!batch.append(3,recurring,new Object(),null,0,7,0),"different scheduler joined run");
        batch.mergeInto(batchGraph);var compact=batchGraph.view(recurring);
        // A periodic task re-queues its next epoch from inside reExecutePeriodic, before the current
        // run's exit is observed, so the observed run is NOT settled: one submission is still waiting
        // to enter. Treating submissions==completed as settled hid that queued epoch.
        require(compact.entries()==10_000&&compact.exits()==10_000&&compact.submissions()==10_000
            &&compact.pendingSubmissions()==1&&!compact.executionsExited(),
            "compaction changed observed counts or settled a re-queued epoch");
        batchGraph.entered(recurring,20_000,null);
        var partial=new TaskEventRun(recurring,null,20_001,7,0);
        require(!partial.append(2,recurring,null,null,999,7,0),"wrong token discharged run");
        require(partial.append(2,recurring,null,null,20_001,7,0),"valid completed pair rejected");
        require(partial.append(1,recurring,null,null,20_002,7,0),"open tail rejected");
        partial.mergeInto(batchGraph);
        require(batchGraph.view(recurring).activeExecutions()==2&&!batchGraph.view(recurring).executionsExited(),"batch discharged unrelated/open executions");
        batchGraph.exited(recurring,20_000);batchGraph.exited(recurring,20_002);
        require(batchGraph.view(recurring).executionsExited(),"open tail tokens were not preserved");
        // Queue disposition must stay separate from execution exit: a wrapper cancelled while still
        // queued is settled as NOT_EXECUTED and must never be reported as an exited execution.
        var queue=new TaskExecutionGraph(8);Object queued=new Object();
        UUID q=queue.submitted(queued,scheduler,null);
        require(queue.view(q).queuedNeverEntered(),"queued submission was not distinguished from an execution");
        require(!queue.view(q).executionsExited(),"queued submission claimed an execution exit");
        require(queue.view(q).queueState()==TaskExecutionGraph.QueueState.SUBMITTED_NEVER_ENTERED,"queue state misreported");
        queue.entered(queued,1,null);queue.exited(queued,1);
        require(queue.view(q).executionsExited()&&!queue.view(q).queuedNeverEntered(),"entered execution was reported as never executed");
        require(queue.view(q).queueState()==TaskExecutionGraph.QueueState.ENTERED,"entered task lost its queue state");
        // Periodic resubmission reopens the queue epoch. The reading must follow the epoch that is
        // open now, not the object's history: gating on historical entries==0 would deny a re-queued
        // task its queue settlement for the rest of its life.
        queue.submitted(queued,scheduler,null);
        require(queue.view(q).queuedNeverEntered(),"a re-queued epoch was hidden by the object's history");
        require(!queue.view(q).executionsExited(),"reopened queue epoch preserved a stale exit");
        // Accumulated entry headroom must not absorb a later submission. A node can legitimately
        // carry more entries than submissions (bare entered calls, merged runs), so a cumulative
        // count comparison would still read "settled" here; only the outstanding-submission count
        // can tell that this new queue epoch has not entered yet.
        var wide=new TaskExecutionGraph(8);Object wideTask=new Object();
        UUID wideId=wide.submitted(wideTask,scheduler,null);
        wide.entered(wideTask,1,null);wide.exited(wideTask,1);
        wide.entered(wideTask,2,null);wide.exited(wideTask,2);
        wide.entered(wideTask,3,null);wide.exited(wideTask,3);
        require(wide.view(wideId).entries()>wide.view(wideId).submissions()&&wide.view(wideId).executionsExited(),
            "a settled node with entry headroom was not reported exited");
        wide.submitted(wideTask,scheduler,null);
        require(!wide.view(wideId).executionsExited(),
            "a submission hid behind accumulated entry headroom");
        // A submission trailing the last exit leaves an epoch open; the same totals reached in the
        // opposite order are settled. Order is the only thing that separates them, so a merge that
        // compared counts alone reported the queued epoch as already exited.
        var epoch=new TaskExecutionGraph(8);Object epochTask=new Object();
        var trailing=new TaskEventRun(epochTask,null,1,7,0);
        require(trailing.append(2,epochTask,null,null,1,7,0),"ordered completed pair rejected");
        require(trailing.append(3,epochTask,scheduler,null,0,7,0),"ordered self-submission rejected");
        trailing.mergeInto(epoch);var epochView=epoch.view(epochTask);
        require(epochView.entries()==1&&epochView.exits()==1&&epochView.submissions()==1,
            "merged epoch counts changed");
        require(!epochView.executionsExited(),
            "a submission trailing the last exit was reported as already exited");
        // The mirror case: an entry merged inside a run must discharge a submission that was recorded
        // separately before it, or a fully executed task stays unsettled forever.
        var settled=new TaskExecutionGraph(8);Object settledTask=new Object();
        UUID settledId=settled.submitted(settledTask,scheduler,null);
        var pair=new TaskEventRun(settledTask,null,1,7,0);
        require(pair.append(2,settledTask,null,null,1,7,0),"paired completed run rejected");
        pair.mergeInto(settled);
        require(settled.view(settledId).entries()==1&&settled.view(settledId).exits()==1
            &&settled.view(settledId).executionsExited(),
            "an entry did not discharge a submission recorded before its run");
        // Control: a later submission must reopen the settled reading.
        settled.submitted(settledTask,scheduler,null);
        require(!settled.view(settledId).executionsExited(),"a later submission preserved a stale exit");
        // Historical entry headroom must not hide a queued epoch. Three bare entries leave the
        // cumulative totals reading "settled" (entries ahead of submissions), so any rule derived
        // from those totals reports an empty queue while two submissions are still waiting.
        var depth=new TaskExecutionGraph(8);Object depthTask=new Object();
        for(int i=1;i<=3;i++){depth.entered(depthTask,i,null);depth.exited(depthTask,i);}
        depth.submitted(depthTask,scheduler,null);depth.submitted(depthTask,scheduler,null);
        var oneMore=new TaskEventRun(depthTask,null,4,7,0);
        require(oneMore.append(2,depthTask,null,null,4,7,0),"headroom probe exit rejected");
        oneMore.mergeInto(depth);var depthView=depth.view(depthTask);
        require(depthView.pendingSubmissions()==1&&!depthView.executionsExited(),
            "historical entry headroom hid a queued submission");
        depth.entered(depthTask,5,null);depth.exited(depthTask,5);
        require(depth.view(depthTask).pendingSubmissions()==0&&depth.view(depthTask).executionsExited(),
            "the last queued submission did not settle the node");
        // A periodic task queues its next epoch from inside the run that is still executing, so at
        // cancel time an active execution and an unmatched submission coexist. An accepted cancel
        // precludes that queued epoch, and both settlements must coexist: the running epoch exits, the
        // precluded epoch settles, and nothing waits on an entry that can never be observed.
        var overlap=new TaskExecutionGraph(8);Object overlapTask=new Object();
        UUID overlapId=overlap.submitted(overlapTask,scheduler,null);
        overlap.entered(overlapTask,1,null);
        overlap.submitted(overlapTask,scheduler,null);
        require(overlap.view(overlapId).activeExecutions()==1&&overlap.view(overlapId).pendingSubmissions()==1,
            "overlap probe did not present a running epoch beside a queued one");
        require(overlap.view(overlapId).queuedEpochOutstanding(),
            "a queued epoch was hidden by the execution still running");
        UUID overlapAttempt=UUID.randomUUID();
        overlap.cancellationRequested(overlapId,overlapAttempt);
        overlap.queuePrecluded(overlapTask);overlap.cancellationReturned(overlapId,overlapAttempt,true);
        require(!overlap.successorsClosed(overlapId,64),
            "a precluded future epoch closed the node while the current epoch was still running");
        overlap.exited(overlapTask,1);
        require(overlap.successorsClosed(overlapId,64),
            "periodic overlap did not close after both the running and queued epochs settled");
        var overlapView=overlap.view(overlapId);
        require(overlapView.pendingSubmissions()==0&&overlapView.precludedSubmissions()==1
            &&overlapView.executionsExited()&&overlapView.queuedEpochPrecluded(),
            "an accepted cancel lost the queued epoch instead of settling it as precluded");
        // A pure queued cancel must settle on its own: the precluded epoch keeps its own state rather
        // than decaying into "never observed a submission", so closure no longer waits for an entry
        // that can never happen.
        var pureQueued=new TaskExecutionGraph(8);Object pureTask=new Object();
        UUID pureId=pureQueued.submitted(pureTask,scheduler,null);
        require(!pureQueued.successorsClosed(pureId,64),"a merely queued node was closed before any cancel");
        UUID pureAttempt=UUID.randomUUID();
        pureQueued.cancellationRequested(pureId,pureAttempt);
        pureQueued.queuePrecluded(pureTask);pureQueued.cancellationReturned(pureId,pureAttempt,true);
        var pureView=pureQueued.view(pureId);
        require(pureView.queueState()==TaskExecutionGraph.QueueState.PRECLUDED_NEVER_ENTERED
            &&pureView.queuedEpochPrecluded()&&pureQueued.successorsClosed(pureId,64),
            "a cancel-precluded queued epoch could not settle closure on its own");
        // Queue preclusion is an execution observation, not a Ronova-attempt privilege. An ordinary
        // successful FutureTask.cancel uses the same bootstrap CAS hook and must retire a queued epoch
        // even when no RecoveryTaskDisposition attempt exists, or the Graph would keep a permanent
        // outstanding submission for an externally-cancelled task.
        var externalCancel=new TaskExecutionGraph(8);Object externalTask=new Object();
        UUID externalId=externalCancel.submitted(externalTask,scheduler,null);
        externalCancel.queuePrecluded(externalTask);var externalView=externalCancel.view(externalId);
        require(externalView.pendingSubmissions()==0&&externalView.precludedSubmissions()==1
            &&!externalView.cancellationReturned()&&!externalView.cancellationAccepted()
            &&externalCancel.successorsClosed(externalId,64),
            "an observed external FutureTask cancellation left a permanent queue obligation");
        // An accepted state CAS without the task-event barrier is not enough to settle the queue:
        // the Graph may still be missing an ENTER captured before the CAS. Preserve the outstanding
        // epoch until the caller can synchronize the observer stream instead of minting NOT_EXECUTED.
        var unsynchronizedCancel=new TaskExecutionGraph(8);Object unsynchronizedTask=new Object();
        UUID unsynchronizedId=unsynchronizedCancel.submitted(unsynchronizedTask,scheduler,null);
        UUID unsynchronizedAttempt=UUID.randomUUID();
        unsynchronizedCancel.cancellationRequested(unsynchronizedId,unsynchronizedAttempt);
        unsynchronizedCancel.cancellationReturned(unsynchronizedId,unsynchronizedAttempt,true);
        var unsynchronizedView=unsynchronizedCancel.view(unsynchronizedId);
        require(unsynchronizedView.cancellationAccepted()&&unsynchronizedView.pendingSubmissions()==1
            &&unsynchronizedView.precludedSubmissions()==0&&!unsynchronizedView.queuedEpochPrecluded()
            &&!unsynchronizedCancel.successorsClosed(unsynchronizedId,64),
            "an unsynchronized cancel minted a queue settlement from a stale snapshot");
        // The closure retry must not be keyed on this node's own revision: a descendant settling
        // touches only itself, so a parent still owing SUCCESSORS_CLOSED would never be revisited.
        // This pins that premise — if a descendant ever starts bumping the parent revision, the
        // RecoveryTaskDisposition latch changes meaning and this assertion must be revisited.
        var revisit=new TaskExecutionGraph(8);Object revisitParent=new Object(),revisitChild=new Object();
        UUID revisitId=revisit.submitted(revisitParent,scheduler,null);
        revisit.submitted(revisitChild,scheduler,revisitParent);
        revisit.entered(revisitParent,1,null);revisit.exited(revisitParent,1);
        long parentRevision=revisit.view(revisitId).revision();
        revisit.entered(revisitChild,1,null);
        require(!revisit.successorsClosed(revisitId,64),"a running child did not block closure");
        revisit.exited(revisitChild,1);
        require(revisit.view(revisitId).revision()==parentRevision,
            "a descendant settlement bumped the parent revision; the closure retry key premise changed");
        require(revisit.successorsClosed(revisitId,64),"child settlement did not close the subtree");
        // A node observed only as a parent was never observed being submitted: it must not be
        // treated as a settled queue entry.
        var orphan=new TaskExecutionGraph(8);Object onlyParent=new Object(),dependent=new Object();
        orphan.submitted(dependent,scheduler,onlyParent);
        var parentView=orphan.view(onlyParent);
        require(parentView!=null&&parentView.queueState()==TaskExecutionGraph.QueueState.UNOBSERVED_SUBMISSION,
            "unobserved submission was treated as a settled queue entry");
        require(!parentView.queuedNeverEntered(),"unobserved submission became a queue settlement");
        queue.evidenceLost();
        require(queue.view(q).queueState()==TaskExecutionGraph.QueueState.EVIDENCE_LOST,
            "coverage loss did not revoke the queue reading");
        // Successor closure is its own proof: it must demand a complete subtree whose every node is
        // settled, and a parent exit must never discharge an unfinished child.
        var closure=new TaskExecutionGraph(8);Object owner=new Object();
        UUID ownerId=closure.submitted(owner,scheduler,null);
        // "Not yet run" is not "closed": a merely queued node may still run, and SUCCESSORS_CLOSED is
        // a monotone durable fact that cannot be withdrawn once recorded.
        require(!closure.successorsClosed(ownerId,64),"a merely queued leaf was reported closed");
        closure.entered(owner,1,null);closure.exited(owner,1);
        require(closure.successorsClosed(ownerId,64),"an exited leaf lost its closure");
        Object running=new Object();UUID runningId=closure.submitted(running,scheduler,owner);
        closure.entered(running,1,null);
        require(!closure.successorsClosed(ownerId,64),"a running successor was reported closed");
        closure.exited(running,1);
        require(closure.view(runningId).executionsExited()&&closure.successorsClosed(ownerId,64),
            "an exited successor did not settle the subtree");
        Object waiting=new Object();closure.submitted(waiting,scheduler,owner);
        require(!closure.successorsClosed(ownerId,64),"a merely queued successor was treated as settled");
        Object stranger=new Object();closure.submitted(stranger,null,owner);
        require(!closure.successorsClosed(ownerId,64),"an unobserved submission was treated as settled");
        closure.entered(stranger,2,null);
        require(!closure.successorsClosed(ownerId,64),"an executing unentered successor was reported closed");
        closure.exited(stranger,2);
        // A node whose submission never revealed a scheduler keeps the subtree open even after it has
        // run and exited: unknown provenance cannot be trusted to stay unreachable, and the closure
        // fact is monotone, so it must never be recorded on that guess.
        require(!closure.successorsClosed(ownerId,64),
            "a node with incomplete scheduler evidence was closed after it exited");
        // Positive control: a fully settled subtree with complete scheduler evidence still closes.
        var cleanClosure=new TaskExecutionGraph(8);Object cleanOwner=new Object();
        UUID cleanOwnerId=cleanClosure.submitted(cleanOwner,scheduler,null);
        Object cleanChild=new Object();cleanClosure.submitted(cleanChild,scheduler,cleanOwner);
        cleanClosure.entered(cleanChild,1,null);cleanClosure.exited(cleanChild,1);
        require(!cleanClosure.successorsClosed(cleanOwnerId,64),"a merely queued parent was reported closed");
        cleanClosure.entered(cleanOwner,1,null);cleanClosure.exited(cleanOwner,1);
        require(cleanClosure.successorsClosed(cleanOwnerId,64),
            "a fully settled subtree with complete scheduler evidence was not closed");
        closure.evidenceLost();
        require(!closure.successorsClosed(ownerId,64),"coverage loss preserved a successor closure");
        require(!closure.successorsClosed(UUID.randomUUID(),64),"an unknown registration reported closure");
        require(!ancestryGraph.successorsClosed(a,8),
            "a cyclic descendant graph reported successor closure");
        System.out.println("TaskExecutionGraphSelfTest PASS (cancel versus exit, concurrent entries, descendants, periodic reentry, identity isolation, capacity, queue-versus-exit separation, submission ordering, successor closure)");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
