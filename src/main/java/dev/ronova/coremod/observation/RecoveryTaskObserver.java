package dev.ronova.coremod.observation;

import dev.ronova.coremod.nativebridge.NativeBridge;
import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Bounded worker capture; only the server lane merges task evidence. No world or Root call occurs in accept. */
public final class RecoveryTaskObserver implements AutoCloseable {
    private static final int ROUTE_LIMIT=4096,INBOX_LIMIT=512;
    private static final Map<Identity,Route> ROUTES=new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<Identity> ROUTE_SCAN=new ConcurrentLinkedQueue<>();
    private static final Map<Identity,Long> EXECUTIONS=new ConcurrentHashMap<>();
    private static final ReferenceQueue<Object> COLLECTED_EXECUTIONS=new ReferenceQueue<>();
    private static final Semaphore ROUTE_SLOTS=new Semaphore(ROUTE_LIMIT),EXECUTION_SLOTS=new Semaphore(ROUTE_LIMIT);
    private static final AtomicLong LOST=new AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLongArray LOSS_REASONS=new java.util.concurrent.atomic.AtomicLongArray(5);
    private static final Consumer<Object[]> SINK=RecoveryTaskObserver::accept;
    private static volatile Method delivering,currentTask,currentExecution,observerMatches;
    private static final AtomicLong SERIAL=new AtomicLong();
    private static final class Identity extends WeakReference<Object> {
        final int hash;
        Identity(Object value){super(Objects.requireNonNull(value));hash=System.identityHashCode(value);}
        Identity(Object value,ReferenceQueue<Object> queue){super(Objects.requireNonNull(value),queue);hash=System.identityHashCode(value);}
        public int hashCode(){return hash;}
        public boolean equals(Object value){return this==value||value instanceof Identity other&&get()!=null&&get()==other.get();}
    }
    private record Route(WeakReference<RecoveryTaskObserver> owner) {}
    private interface Captured {}
    // The bounded inbox owns these references until merge; GC must not turn a witnessed exit into a missing event.
    private record Event(int kind,Object task,Object scheduler,Object parent,long execution,long changes,long dropped) implements Captured {}
    private record Run(TaskEventRun value) implements Captured {}
    record Origin(RecoverySourceTracker.SourceKey source,dev.ronova.coremod.root.api.SubjectQualification qualification,
                  int depth,boolean verified,long publishedAt) {}
    record Copy(Object task,Object source,Object result,Origin origin,List<Class<?>> callers,boolean verified,long capturedAt,long taskChanges) implements Captured {}
    private final AtomicReference<Thread> owner=new AtomicReference<>();
    private final TaskExecutionGraph graph=new TaskExecutionGraph(1024);
    private final Semaphore permits=new Semaphore(INBOX_LIMIT);
    private final ConcurrentLinkedQueue<Captured> inbox=new ConcurrentLinkedQueue<>();
    private final Object inboxLock=new Object();
    private Run tailRun;
    private final AtomicLong compactedEvents=new AtomicLong();
    private final AtomicLong localInboxDrops=new AtomicLong(),capturedEvents=new AtomicLong();
    private final Map<Identity,Origin> origins=new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Identity> originScan=new ConcurrentLinkedQueue<>();
    private final Semaphore originSlots=new Semaphore(2048);
    private final dev.ronova.coremod.integrity.EnforcementRuntime enforcement;
    private final Consumer<Copy> copyMerge;
    private volatile boolean closed;
    private final Runnable release;
    private long observedLosses=LOST.get(),observedDrops,merged;
    RecoveryTaskObserver(Runnable release,dev.ronova.coremod.integrity.EnforcementRuntime enforcement,Consumer<Copy> copyMerge){
        this.release=release;this.enforcement=Objects.requireNonNull(enforcement);this.copyMerge=Objects.requireNonNull(copyMerge);closed=release==null;
    }

    public static synchronized boolean install() {
        Class<?> hooks=NativeBridge.taskHookClass();if(hooks==null)return false;
        try {
            Method matches=hooks.getMethod("observerMatches",Object.class);
            if(observerMatches!=null&&Boolean.TRUE.equals(matches.invoke(null,SINK)))return true;
            // Replacement is evidence loss, including a fixture/agent replacing the callback.
            if(observerMatches!=null)lost();
            delivering=hooks.getMethod("deliveringTo",Object.class);
            currentTask=hooks.getMethod("currentTask");currentExecution=hooks.getMethod("currentExecution");observerMatches=matches;
            hooks.getMethod("install",Consumer.class).invoke(null,SINK);return true;
        } catch(ReflectiveOperationException|LinkageError unavailable){lost();return false;}
    }
    private static void accept(Object[] row) {
        try {
            if(delivering==null||!Boolean.TRUE.equals(delivering.invoke(null,SINK))||row==null||row.length!=8
                ||row[5]!=Thread.currentThread()||!(row[0] instanceof Integer kind)||kind<1||kind>5
                ||row[1]==null||!(row[7] instanceof Long dropped)) {lost();return;}
                RecoveryTaskObserver scoped=RecoveryObservationScope.taskObserver();
                Route route=ROUTES.get(new Identity(row[1]));
                // CANCEL is a settlement for an already-observed task; it must never start a new
                // observation route merely because some unrelated cancellation happened inside a
                // scoped server callback. SUBMIT/ENTER remain the only route-establishing events.
                if(kind==5&&route==null)return;
                RecoveryTaskObserver destination=route==null?null:route.owner.get();
                Route inherited=row[3]==null?null:ROUTES.get(new Identity(row[3]));
                RecoveryTaskObserver parent=inherited==null?null:inherited.owner.get();
                if(destination==null)destination=parent==null?scoped:parent;
                if(destination==null||destination.closed)return;
                var proof=NativeBridge.taskObservers();
                if(!proof.covers(63)){lost(0);return;}
                if((kind==1||kind==2)&&!NativeBridge.currentTaskReceiver(row[1])){lost(1);return;}
                if(scoped!=null&&scoped!=destination||parent!=null&&parent!=destination){lost();return;}
                if(route==null) {
                    if(!ROUTE_SLOTS.tryAcquire()){lost();return;}
                    Identity identity=new Identity(row[1]);Route added=new Route(new WeakReference<>(destination));
                    Route raced=ROUTES.putIfAbsent(identity,added);
                    if(raced==null)ROUTE_SCAN.offer(identity);
                    else {ROUTE_SLOTS.release();if(raced.owner.get()!=destination){lost();return;}}
                }
                long execution=0;
                if(kind==1) {
                    if(row[4]==null||!EXECUTION_SLOTS.tryAcquire()){lost();return;}
                    Identity token=new Identity(row[4],COLLECTED_EXECUTIONS);
                    execution=SERIAL.incrementAndGet();
                    if(execution<1||EXECUTIONS.putIfAbsent(token,execution)!=null){EXECUTION_SLOTS.release();lost();return;}
                } else if(kind==2) {
                    Long token=row[4]==null?null:EXECUTIONS.remove(new Identity(row[4]));
                    if(token==null){lost();return;}EXECUTION_SLOTS.release();execution=token;
                } else if(kind==4){lost();return;}
                var after=NativeBridge.taskObservers();
                if(!after.covers(63)||after.changes()!=proof.changes()){lost(2);return;}
                destination.enqueue(new Event(kind,row[1],row[2],row[3],execution,proof.changes(),dropped));
        } catch(Throwable unavailable){lost(4);}
    }
    /** Read-only capture identity; it is not a task cancellation permit. */
    static Object executingTask() {
        try {Object task=currentTask==null?null:currentTask.invoke(null);return task!=null&&NativeBridge.currentTaskReceiver(task)?task:null;}
        catch(ReflectiveOperationException|LinkageError unavailable){lost();return null;}
    }
    /** Server-lane copies can run inside a real JDK task (for example CallerRunsPolicy).
     * Use the same Native receiver and runtime route as worker capture, never the thread name. */
    void synchronousSourceUsed(TaskExecutionGraph.SourceUse source) {
        requireOwner();if(closed)return;
        var before=NativeBridge.taskObservers();Object task=executingTask();if(task==null)return;
        Route route=ROUTES.get(new Identity(task));
        var after=NativeBridge.taskObservers();
        if(route==null||route.owner.get()!=this||!before.covers(63)||!after.covers(63)||before.changes()!=after.changes()) {
            lost();return;
        }
        synchronized(inboxLock){tailRun=null;} // Inline source consumption is an ordering barrier too.
        graph.sourceUsed(task,Objects.requireNonNull(source));
    }
    static void copied(Object source,Object result) {
        if(source==null||result==null||source==result)return;
        var taskProof=NativeBridge.taskObservers();
        Object task=executingTask();if(task==null)return;
        Route route=ROUTES.get(new Identity(task));RecoveryTaskObserver observer=route==null?null:route.owner.get();
        if(observer==null||observer.closed)return;
        List<Class<?>> callers=RecoveryBoundaryProof.asyncCopyCallers();if(callers.isEmpty()){lost();return;}
        try(var proof=observer.enforcement.beginObservation(0x6000)) {
            Origin origin=observer.origins.get(new Identity(source));long now=System.nanoTime();
            boolean fresh=origin!=null&&now-origin.publishedAt>=0&&now-origin.publishedAt<=dev.ronova.coremod.root.api.RecoverySourceEvidence.MAX_AGE_NANOS;
            var after=NativeBridge.taskObservers();
            boolean verified=fresh&&origin.verified&&proof!=null&&proof.finish()&&taskProof.covers(63)&&after.covers(63)&&taskProof.changes()==after.changes();
            if(!observer.enqueue(new Copy(task,source,result,origin,callers,verified,now,taskProof.changes())))return;
            // This is a capture ticket only. The server still requires the preceding edge,
            // matching original qualification/version and live evidence before accepting it.
            if(verified&&origin.depth<Integer.MAX_VALUE)observer.publishCaptured(result,new Origin(origin.source,origin.qualification,origin.depth+1,true,now));
        }
    }
    private void publishCaptured(Object result,Origin origin) {
        if(closed)return;
        Identity key=new Identity(result);Origin existing=origins.get(key);
        if(existing!=null){
            if(!existing.source.equals(origin.source)||!existing.qualification.equals(origin.qualification)||existing.depth!=origin.depth)lost();
            return; // A server-merged ticket may already have a later observation timestamp.
        }
        if(!originSlots.tryAcquire()){lost();return;}
        if(origins.putIfAbsent(key,origin)==null)originScan.offer(key);else {originSlots.release();lost();}
    }
    void publish(Object value,Origin origin) {
        requireOwner();if(closed||value==null)return;
        Identity key=new Identity(value);
        if(origin==null){if(origins.remove(key)!=null)originSlots.release();return;}
        if(origins.replace(key,origin)!=null)return;
        if(!originSlots.tryAcquire()){lost();return;}
        if(origins.putIfAbsent(key,origin)==null)originScan.offer(key);else originSlots.release();
    }
    public int merge(int allowance) {
        requireOwner();if(allowance<0||allowance>64)throw new IllegalArgumentException("invalid task merge allowance");
        if(closed)return 0;
        for(int i=0;i<16;i++) {
            Identity collected=(Identity)COLLECTED_EXECUTIONS.poll();if(collected==null)break;
            if(EXECUTIONS.remove(collected)!=null)EXECUTION_SLOTS.release();
        }
        if(!install()){graph.evidenceLost();return 0;}
        long lost=LOST.get();if(lost!=observedLosses){graph.evidenceLost();observedLosses=lost;}
        int count=0;Captured captured;
        while(count<allowance&&(captured=poll())!=null) {
            count++;
            if(captured instanceof Copy copy){copyMerge.accept(copy);merged++;continue;}
            if(captured instanceof Run run) {
                var proof=NativeBridge.taskObservers();var value=run.value;
                if(!proof.covers(63)||proof.changes()!=value.changes||value.dropped!=observedDrops) {
                    observedDrops=value.dropped;graph.evidenceLost();continue;
                }
                value.mergeInto(graph);merged=Math.addExact(merged,value.events);continue;
            }
            Event event=(Event)captured;var proof=NativeBridge.taskObservers();
            Object task=event.task;
            if(task==null||!proof.covers(63)||proof.changes()!=event.changes||event.dropped!=observedDrops) {
                observedDrops=event.dropped;graph.evidenceLost();continue;
            }
            if(event.kind==1)graph.entered(task,event.execution,event.parent);
            else if(event.kind==2)graph.exited(task,event.execution);
            else if(event.kind==3)graph.submitted(task,event.scheduler,event.parent);
            else if(event.kind==5)graph.queuePrecluded(task);
            else {graph.evidenceLost();continue;}
            merged++;
        }
        for(int i=0;i<16;i++) {
            Identity key=originScan.poll();if(key==null)break;
            if(key.get()==null){if(origins.remove(key)!=null)originSlots.release();}else originScan.offer(key);
        }
        graph.retireUnboundCollected(16);
            for(int i=0;i<16;i++) {
                Identity key=ROUTE_SCAN.poll();if(key==null)break;Route route=ROUTES.get(key);
                RecoveryTaskObserver owner=route==null?null:route.owner.get();
                if(key.get()==null||owner==null){if(route!=null&&ROUTES.remove(key,route))ROUTE_SLOTS.release();}else ROUTE_SCAN.offer(key);
            }
        return count;
    }
    /**
     * Server-lane cancellation barrier. RecoveryTaskDisposition enters only while the inbox is empty.
     * BootstrapTaskHooks holds the exact task gate through ENTER/SUBMIT publication and the FutureTask
     * state CAS, so every event that linearized before an accepted cancel is already in this bounded
     * FIFO when the native call returns. Consuming at most the entire inbox capacity therefore passes
     * every pre-CAS event even if unrelated post-CAS producers keep appending behind it.
     *
     * <p>This is observation synchronization only; it grants no cancellation authority. Any proof loss
     * while crossing the barrier makes the settlement unavailable.</p>
     */
    boolean synchronizeCancellationBoundary() {
        requireOwner();if(closed)return false;
        // The empty-inbox fast path must still validate the callback and capture-loss counters.
        // A pre-CAS ENTER can fail before enqueue (for example because the observer route/proof was
        // unavailable); without this check an empty queue would look synchronized and could mint
        // NOT_EXECUTED from a stale Graph. install() also detects callback replacement.
        if(!install()) {graph.evidenceLost();return false;}
        long capturedLosses=LOST.get();
        if(capturedLosses!=observedLosses) {
            graph.evidenceLost();observedLosses=capturedLosses;return false;
        }
        long lossesBefore=graph.losses();int remaining=INBOX_LIMIT;
        while(remaining>0) {
            int queued=queued();
            if(queued==0) {
                long after=LOST.get();
                if(after!=capturedLosses) {graph.evidenceLost();observedLosses=after;return false;}
                return graph.losses()==lossesBefore;
            }
            int step=Math.min(64,Math.min(queued,remaining));
            int consumed=merge(step);if(consumed<=0)return false;remaining-=consumed;
            if(LOST.get()!=capturedLosses||graph.losses()!=lossesBefore)return false;
        }
        // Consuming a full inbox capacity is sufficient: at the CAS there could not have been more
        // than INBOX_LIMIT queued capture objects, so any objects still present were appended later.
        return LOST.get()==capturedLosses&&graph.losses()==lossesBefore;
    }
    public TaskExecutionGraph graph(){return graph;}
    public long merged(){return merged;}
    public long lostEvents(){return LOST.get();}
    public Map<String,Long> lossReasons(){return Map.of("proof_unavailable",LOSS_REASONS.get(0),"receiver_unproven",LOSS_REASONS.get(1),
        "proof_changed",LOSS_REASONS.get(2),"inbox_full",LOSS_REASONS.get(3),"capture_exception",LOSS_REASONS.get(4));}
    public Map<String,Long> captureDiagnostics(){return Map.of("local_inbox_drops",localInboxDrops.get(),"captured_events",capturedEvents.get(),
        "queued_objects",(long)inbox.size(),"reserved_slots",(long)queued(),"compacted_events",compactedEvents.get());}
    public int queued(){return INBOX_LIMIT-permits.availablePermits();}
    private static void lost(){LOST.updateAndGet(value->value==Long.MAX_VALUE?value:value+1);}
    private static void lost(int reason){LOSS_REASONS.updateAndGet(reason,value->value==Long.MAX_VALUE?value:value+1);lost();}
    /** Bind the observer to the verified server lane, never to the runtime-construction worker. */
    void bindServerLane() {
        Thread current=Thread.currentThread(),bound=owner.get();
        if(bound==null){owner.compareAndSet(null,current);bound=owner.get();}
        if(bound!=current)throw new IllegalStateException("task evidence server lane already bound to another thread");
    }
    private void requireOwner(){Thread bound=owner.get();if(bound==null||Thread.currentThread()!=bound)throw new IllegalStateException("task evidence merge requires its server lane");}
    private boolean enqueue(Captured captured) {
        synchronized(inboxLock) {
            if(closed)return false;
            if(captured instanceof Event event&&tailRun!=null&&tailRun.value.append(event.kind,event.task,event.scheduler,
                event.parent,event.execution,event.changes,event.dropped)) {
                capturedEvents.incrementAndGet();compactedEvents.incrementAndGet();return true;
            }
            tailRun=null; // Different events (especially copies) may never be reordered into an older run.
            if(!permits.tryAcquire()){localInboxDrops.incrementAndGet();lost(3);return false;}
            boolean queued=false;
            try {
                if(captured instanceof Event event&&event.kind==1)
                    captured=tailRun=new Run(new TaskEventRun(event.task,event.parent,event.execution,event.changes,event.dropped));
                inbox.offer(captured);queued=true;
                if(!(captured instanceof Copy))capturedEvents.incrementAndGet();
                return true;
            } finally {if(!queued){tailRun=null;permits.release();}}
        }
    }
    private Captured poll() {
        synchronized(inboxLock) {
            Captured captured=inbox.poll();if(captured==null)return null;
            if(captured==tailRun)tailRun=null;
            permits.release();return captured;
        }
    }
    private void discardClosed(){for(int i=0;i<INBOX_LIMIT;i++)if(poll()==null)break;}
    @Override public void close() {
        // A runtime may be torn down before its first server tick. In that one case there is no
        // previously verified lane to preserve, so close may bind the cleanup caller exactly once.
        if(owner.get()==null)bindServerLane();
        requireOwner();if(closed)return;closed=true;
        discardClosed();
        origins.clear();originScan.clear();
        // Routes keep only weak owners and objects; late events cannot enter a replacement runtime.
        graph.evidenceLost();
        release.run();
    }
}
