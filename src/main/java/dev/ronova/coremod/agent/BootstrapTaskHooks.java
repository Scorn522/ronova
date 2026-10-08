package dev.ronova.coremod.agent;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** JDK-only bootstrap bridge. Events describe execution, never grant disposal permission. */
public final class BootstrapTaskHooks {
    public static final int ENTER=1,EXIT=2,SUBMIT=3,OVERFLOW=4,CANCEL=5;
    private static final int MAX_DEPTH=128,MAX_GATES=4096;
    private static final ThreadLocal<Frame> CURRENT=new ThreadLocal<>();
    private static final ThreadLocal<Boolean> DELIVERING=new ThreadLocal<>();
    private static final ThreadLocal<Integer> OVERFLOW_DEPTH=new ThreadLocal<>();
    private static final Object OVERFLOW_TOKEN=new Object(),PRECLUDED_TOKEN=new Object();
    private static final AtomicLong DROPPED=new AtomicLong();
    private static final StackWalker WALKER=StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static volatile Consumer<Object[]> observer;
    private static final GateRegistry GATES=new GateRegistry(MAX_GATES);
    private BootstrapTaskHooks() {}
    private static native long nativeEnter(Object task);
    private static native void nativeExit(long token);
    private static native boolean nativeCancelPermit(Object handle,Object task,int expected,int update);
    private static native void nativeCancelCommitted(Object task,boolean linearized);

    /**
     * Keeps ordinary Future cancellation unchanged. For a Ronova-armed cancellation the same per-task
     * gate also linearizes the FutureTask state CAS against ENTER/SUBMIT publication: a hook that wins
     * the gate publishes before the CAS, while a successful cancel marks the task before any later
     * outer ENTER/SUBMIT can publish. If no bounded gate can be obtained the native attempt is reported
     * uncertain rather than pretending the observer stream was linearized.
     */
    public static boolean cancelState(java.lang.invoke.VarHandle handle,Object task,int expected,int update) {
        if(!nativeCancelPermit(handle,task,expected,update))return false;
        Gate gate=GATES.acquire(task);
        if(gate==null) {
            boolean changed=handle.compareAndSet(task,expected,update);
            if(changed) {
                // We could not linearize cancellation against ENTER/SUBMIT publication. Preserve the
                // real FutureTask state transition, but invalidate task evidence rather than leaving a
                // queued obligation silently stale.
                lost();
                nativeCancelCommitted(task,false);
            }
            return changed;
        }
        boolean changed;
        synchronized(gate) {
            changed=handle.compareAndSet(task,expected,update);
            if(changed) {
                gate.cancelled=true;
                // Cancellation itself is an observed queue event. Publishing it while holding the same
                // per-task gate makes every earlier ENTER/SUBMIT visible before this PRECLUDED marker,
                // and prevents a later outer ENTER/SUBMIT from creating a new executable obligation.
                // Reentrant observer delivery cannot safely enqueue another event; make that a visible
                // evidence gap instead of silently losing the queue settlement.
                if(observer!=null&&Boolean.TRUE.equals(DELIVERING.get()))lost();
                else emit(CANCEL,task,null,null,null);
            }
        }
        // The gate no longer needs to be held after the CAS: cancelled is already visible and every
        // later outer ENTER/SUBMIT will be suppressed. Keep lease cleanup outside the hook lock.
        if(changed)nativeCancelCommitted(task,true);
        return changed;
    }
    public static void install(Consumer<Object[]> value){observer=Objects.requireNonNull(value);}
    public static boolean observerMatches(Object expected){return observer==expected;}
    public static boolean deliveringTo(Object expected){return observer==expected&&Boolean.TRUE.equals(DELIVERING.get());}

    /** Opaque token is returned only at a registered JDK method entry and must be closed in its finally. */
    public static Object enter(Object task) {
        try{return enterFrame(task);}catch(Throwable unavailable){lost();return null;}
    }
    private static Object enterFrame(Object task) {
        if(observer==null||task==null||Boolean.TRUE.equals(DELIVERING.get())||!boundary(false))return null;
        Frame parent=CURRENT.get();
        Integer overflow=OVERFLOW_DEPTH.get();
        if(overflow!=null){OVERFLOW_DEPTH.set(Math.addExact(overflow,1));return OVERFLOW_TOKEN;}
        if(parent!=null&&parent.depth>=MAX_DEPTH){OVERFLOW_DEPTH.set(1);emit(OVERFLOW,task,null,parent.task,null);return OVERFLOW_TOKEN;}
        Gate gate=GATES.acquire(task);
        if(gate==null)return enterPublished(task,parent);
        synchronized(gate) {
            // A successful FutureTask cancellation precludes later outer run()/runAndReset() entries.
            // Nested boundaries belonging to the already-running epoch remain paired with their exit.
            if(gate.cancelled&&(parent==null||parent.task!=task))return PRECLUDED_TOKEN;
            return enterPublished(task,parent);
        }
    }
    private static Object enterPublished(Object task,Frame parent) {
        Frame frame=new Frame(task,parent,nativeEnter(task));
        CURRENT.set(frame);
        if(parent==null||parent.task!=task)emit(ENTER,task,null,parent==null?null:parent.task,frame);
        return frame;
    }
    public static void exit(Object token) {
        try{exitFrame(token);}catch(Throwable unavailable){lost();
            if(token instanceof Frame frame&&frame.thread==Thread.currentThread()&&CURRENT.get()==frame) {
                if(frame.previous==null)CURRENT.remove();else CURRENT.set(frame.previous);
            }
        }
    }
    private static void exitFrame(Object token) {
        if(token==PRECLUDED_TOKEN)return;
        if(token==OVERFLOW_TOKEN&&boundary(false)) {
            Integer depth=OVERFLOW_DEPTH.get();if(depth!=null){if(depth==1)OVERFLOW_DEPTH.remove();else OVERFLOW_DEPTH.set(depth-1);}return;
        }
        if(!(token instanceof Frame frame)||frame.thread!=Thread.currentThread()||CURRENT.get()!=frame||!boundary(false))return;
        // Keep the executing parent context during exit delivery, but never permit event-handler recursion.
        try {if(frame.previous==null||frame.previous.task!=frame.task)emit(EXIT,frame.task,null,
            frame.previous==null?null:frame.previous.task,frame);}
        finally {nativeExit(frame.nativeToken);if(frame.previous==null)CURRENT.remove();else CURRENT.set(frame.previous);}
    }
    public static void submitted(Object scheduler,Object task) {
        try{submit(scheduler,task);}catch(Throwable unavailable){lost();}
    }
    private static void submit(Object scheduler,Object task) {
        if(observer==null||task==null||Boolean.TRUE.equals(DELIVERING.get())||!boundary(true))return;
        if(scheduler==task&&task instanceof java.util.concurrent.ForkJoinTask<?>) {
            scheduler=java.util.concurrent.ForkJoinTask.getPool();if(scheduler==null)scheduler=java.util.concurrent.ForkJoinPool.commonPool();
        }
        Frame frame=CURRENT.get();
        Gate gate=GATES.acquire(task);
        if(gate==null){emit(SUBMIT,task,scheduler,frame==null?null:frame.task,frame);return;}
        synchronized(gate) {
            // Re-submitting an already-cancelled FutureTask can only enqueue a no-op wrapper. Do not
            // create a fresh executable queue obligation after the cancellation linearization point.
            if(gate.cancelled)return;
            emit(SUBMIT,task,scheduler,frame==null?null:frame.task,frame);
        }
    }
    public static Object currentTask(){Frame frame=CURRENT.get();return OVERFLOW_DEPTH.get()!=null||Boolean.TRUE.equals(DELIVERING.get())||frame==null?null:frame.task;}
    public static Object currentExecution(){return OVERFLOW_DEPTH.get()!=null||Boolean.TRUE.equals(DELIVERING.get())?null:CURRENT.get();}
    public static long droppedEvents(){return DROPPED.get();}
    private static void lost(){for(;;){long n=DROPPED.get();if(n==Long.MAX_VALUE||DROPPED.compareAndSet(n,n+1))return;}}
    private static void emit(int kind,Object task,Object scheduler,Object parent,Object execution) {
        Consumer<Object[]> sink=observer;
        if(sink==null||Boolean.TRUE.equals(DELIVERING.get()))return;
        DELIVERING.set(true);
        try {sink.accept(new Object[]{kind,task,scheduler,parent,execution,Thread.currentThread(),System.nanoTime(),DROPPED.get()});}
        catch(Throwable unavailable){lost();}
        finally {DELIVERING.remove();}
    }
    private static boolean boundary(boolean submission) {
        return WALKER.walk(frames->frames.dropWhile(frame->frame.getDeclaringClass()==BootstrapTaskHooks.class).findFirst().map(frame->{
            Class<?> owner=frame.getDeclaringClass();
            if(owner.getClassLoader()!=null||!"java.base".equals(owner.getModule().getName()))return false;
            String name=owner.getName(),method=frame.getMethodName();
            if(submission)return name.equals("java.util.concurrent.ThreadPoolExecutor")&&method.equals("execute")
                ||name.equals("java.util.concurrent.ScheduledThreadPoolExecutor")&&(method.equals("delayedExecute")||method.equals("reExecutePeriodic"))
                ||name.equals("java.util.concurrent.ForkJoinPool")&&method.equals("externalSubmit")
                ||name.equals("java.util.concurrent.ForkJoinTask")&&method.equals("fork");
            return name.equals("java.util.concurrent.FutureTask")&&(method.equals("run")||method.equals("runAndReset"))
                ||name.equals("java.util.concurrent.ThreadPoolExecutor")&&method.equals("runWorker")
                ||name.equals("java.util.concurrent.ThreadPoolExecutor$CallerRunsPolicy")&&method.equals("rejectedExecution")
                ||name.equals("java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask")&&method.equals("run")
                ||name.equals("java.util.concurrent.ForkJoinTask")&&method.equals("doExec");
        }).orElse(false));
    }
    private static final class Frame {
        final Object task;
        final Frame previous;
        final long nativeToken;
        final Thread thread=Thread.currentThread();
        final int depth;
        Frame(Object task,Frame previous,long nativeToken){this.task=task;this.previous=previous;this.nativeToken=nativeToken;depth=previous==null?1:previous.depth+1;}
    }
    static final class Gate { boolean cancelled; }
    static final class GateRegistry {
        private final int capacity;
        private final ReferenceQueue<Object> collected=new ReferenceQueue<>();
        private final Map<Identity,Gate> gates=new HashMap<>();
        GateRegistry(int capacity){if(capacity<1)throw new IllegalArgumentException("invalid gate capacity");this.capacity=capacity;}
        synchronized Gate acquire(Object task) {
            if(task==null)return null;drain();
            Identity probe=new Identity(task);Gate found=gates.get(probe);if(found!=null)return found;
            if(gates.size()>=capacity)return null;
            Identity stored=new Identity(task,collected);Gate gate=new Gate();gates.put(stored,gate);return gate;
        }
        synchronized int size(){drain();return gates.size();}
        private void drain(){for(Identity dead;(dead=(Identity)collected.poll())!=null;)gates.remove(dead);}
    }
    private static final class Identity extends WeakReference<Object> {
        final int hash;
        Identity(Object value){super(Objects.requireNonNull(value));hash=System.identityHashCode(value);}
        Identity(Object value,ReferenceQueue<Object> queue){super(Objects.requireNonNull(value),queue);hash=System.identityHashCode(value);}
        @Override public int hashCode(){return hash;}
        @Override public boolean equals(Object value){return this==value||value instanceof Identity other&&get()!=null&&get()==other.get();}
    }
}
