package dev.ronova.coremod.runtime;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/** Owns only Ronova-enqueued callbacks, with exact connection identity and bounded reservations. */
public final class IngressWorkRegistry<K> implements AutoCloseable {
    public enum State { QUEUED, RUNNING, CANCEL_REQUESTED, FINISHING, COMPLETED, CANCELLED, FAILED }
    private final int maximumOwners,maximumTasks;
    private final IntFunction<Runnable> reserve;
    private final ReferenceQueue<K> collected=new ReferenceQueue<>();
    private final Map<Key<K>,Owner> owners=new HashMap<>();
    private final LinkedHashSet<Work> tasks=new LinkedHashSet<>();
    private boolean closed;
    private long denied,cancelled,completed,failed;
    private static final class Owner {
        boolean closed;
        Runnable release;
        Owner(Runnable release){this.release=release;}
    }
    public IngressWorkRegistry(int maximumOwners,int maximumTasks,IntFunction<Runnable> reserve) {
        if(maximumOwners<1||maximumTasks<1)throw new IllegalArgumentException("invalid ingress limits");
        this.maximumOwners=maximumOwners;this.maximumTasks=maximumTasks;this.reserve=Objects.requireNonNull(reserve);
    }
    public synchronized Work enqueue(K owner,Runnable body,long now,long lifetime) {
        Objects.requireNonNull(owner);Objects.requireNonNull(body);
        if(lifetime<=0||lifetime>5_000_000_000L)throw new IllegalArgumentException("invalid ingress lifetime");
        reap();
        if(closed){denied=next(denied);return null;}
        var key=new Key<>(owner,null);var scope=owners.get(key);
        if(scope==null) {
            if(owners.size()>=maximumOwners){denied=next(denied);return null;}
            Runnable permit=reserve.apply(256);
            if(permit==null){denied=next(denied);return null;}
            key=new Key<>(owner,collected);scope=new Owner(permit);owners.put(key,scope);
        }
        if(scope.closed||tasks.size()>=maximumTasks){denied=next(denied);return null;}
        Runnable permit=reserve.apply(4096);
        if(permit==null){denied=next(denied);return null;}
        var work=new Work(this,key,scope,body,permit,now,lifetime);tasks.add(work);return work;
    }
    public void cancelOwner(K owner) {
        if(owner==null)return;
        List<Work> pending;
        synchronized(this) {
            var scope=owners.get(new Key<>(owner,null));if(scope==null)return;
            scope.closed=true;pending=tasks.stream().filter(work->work.scope==scope).toList();
        }
        cancelAll(pending);
    }
    /** Fair bounded inspection; executing callbacks are never interrupted or reported cancelled. */
    public void maintain(long now,int maximum,Predicate<K> connected) {
        Objects.requireNonNull(connected);
        if(maximum<1)throw new IllegalArgumentException("invalid maintenance allowance");
        List<Work> selected=new ArrayList<>();
        synchronized(this) {
            reap();int count=Math.min(maximum,tasks.size());
            for(int i=0;i<count;i++){var work=tasks.iterator().next();tasks.remove(work);tasks.add(work);selected.add(work);}
        }
        for(var work:selected) {
            @SuppressWarnings("unchecked") K owner=(K)work.owner.get();
            if(owner==null){work.cancel();continue;}
            if(!connected.test(owner)){cancelOwner(owner);continue;}
            long age=now-work.created;
            if(age<0||age>=work.lifetime)work.cancel();
        }
    }
    public synchronized Map<String,Long> counters() {
        return Map.of("ingress_connections",(long)owners.size(),"ingress_pending",(long)tasks.size(),
            "ingress_denied",denied,"ingress_cancelled",cancelled,"ingress_completed",completed,"ingress_failed",failed);
    }
    @SuppressWarnings("unchecked") private void reap() {
        for(int i=0;i<16;i++) {
            Key<K> key=(Key<K>)collected.poll();if(key==null)return;
            Owner scope=owners.remove(key);if(scope!=null){scope.closed=true;releaseOwner(scope);}
        }
    }
    private static void releaseOwner(Owner owner) {Runnable release=owner.release;owner.release=null;if(release!=null)release.run();}
    @Override public void close() {
        List<Work> pending;List<Owner> scopes;
        synchronized(this) {
            if(closed)return;closed=true;pending=List.copyOf(tasks);scopes=List.copyOf(owners.values());
            scopes.forEach(scope->scope.closed=true);owners.clear();
        }
        Throwable failure=null;
        try{cancelAll(pending);}catch(RuntimeException|Error error){failure=error;}
        for(var scope:scopes)try{releaseOwner(scope);}catch(RuntimeException|Error error){if(failure==null)failure=error;else if(failure!=error)failure.addSuppressed(error);}
        rethrow(failure);
    }
    private static void cancelAll(List<Work> pending) {
        Throwable failure=null;
        for(var work:pending)try{work.cancel();}catch(RuntimeException|Error error){if(failure==null)failure=error;else if(failure!=error)failure.addSuppressed(error);}
        rethrow(failure);
    }
    private static void rethrow(Throwable failure) {if(failure instanceof RuntimeException error)throw error;if(failure instanceof Error error)throw error;
        if(failure!=null)throw new IllegalStateException("ingress callback failed",failure);}
    private static long next(long value){return value==Long.MAX_VALUE?value:value+1;}

    public static final class Work implements Runnable {
        private final IngressWorkRegistry<?> registry;
        private final Key<?> owner;
        private final Owner scope;
        private final long created,lifetime;
        private Runnable body,release;
        private State state=State.QUEUED;
        private Work(IngressWorkRegistry<?> registry,Key<?> owner,Owner scope,Runnable body,Runnable release,long created,long lifetime) {
            this.registry=registry;this.owner=owner;this.scope=scope;this.body=body;this.release=release;this.created=created;this.lifetime=lifetime;
        }
        public State state(){synchronized(registry){return state;}}
        @Override public void run() {
            Runnable action;
            synchronized(registry){if(state!=State.QUEUED)return;
                if(registry.closed||scope.closed||owner.get()==null)action=null;
                else{state=State.RUNNING;action=body;body=null;}}
            if(action==null){cancel();return;}
            Throwable failure=null;
            try{action.run();}catch(Throwable error){failure=error;rethrow(error);}
            finally{finish(failure);}
        }
        private void finish(Throwable prior) {
            Runnable permit;
            synchronized(registry){state=State.FINISHING;permit=release;release=null;}
            Throwable cleanup=null;
            try{if(permit!=null)permit.run();}catch(RuntimeException|Error error){cleanup=error;}
            finally{synchronized(registry){registry.tasks.remove(this);state=prior==null&&cleanup==null?State.COMPLETED:State.FAILED;
                if(state==State.COMPLETED)registry.completed=next(registry.completed);else registry.failed=next(registry.failed);}}
            if(cleanup!=null){if(prior!=null&&prior!=cleanup)prior.addSuppressed(cleanup);else if(prior==null)rethrow(cleanup);}
        }
        public boolean cancel() {
            Runnable permit;
            synchronized(registry) {
                if(state==State.RUNNING||state==State.CANCEL_REQUESTED){state=State.CANCEL_REQUESTED;return false;}
                if(state!=State.QUEUED)return false;
                body=null;permit=release;release=null;state=State.FINISHING;
            }
            boolean released=false;
            try{if(permit!=null)permit.run();released=true;}
            finally{synchronized(registry){registry.tasks.remove(this);state=released?State.CANCELLED:State.FAILED;
                if(released)registry.cancelled=next(registry.cancelled);else registry.failed=next(registry.failed);}}
            return true;
        }
    }
    private static final class Key<K> extends WeakReference<K> {
        private final int hash;
        Key(K owner,ReferenceQueue<K> queue){super(owner,queue);hash=System.identityHashCode(owner);}
        @Override public int hashCode(){return hash;}
        @Override public boolean equals(Object other){return this==other||other instanceof Key<?> key&&get()!=null&&get()==key.get();}
    }
}
