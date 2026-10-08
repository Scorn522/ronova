package dev.ronova.coremod.runtime;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class IngressWorkRegistrySelfTest {
    public static void main(String[] args) throws Exception {
        AtomicLong bytes=new AtomicLong();AtomicInteger calls=new AtomicInteger();
        var registry=new IngressWorkRegistry<Object>(8,4,size->{bytes.addAndGet(size);return ()->bytes.addAndGet(-size);});
        Object a=new String("same"),b=new String("same");
        var cancelled=registry.enqueue(a,calls::incrementAndGet,0,100);
        var kept=registry.enqueue(b,calls::incrementAndGet,0,100);
        registry.cancelOwner(a);cancelled.run();kept.run();kept.run();
        require(calls.get()==1&&cancelled.state()==IngressWorkRegistry.State.CANCELLED
            &&kept.state()==IngressWorkRegistry.State.COMPLETED&&bytes.get()==512,
            "identity cancellation replayed work, cancelled a peer, or retained a callback charge");
        require(registry.enqueue(a,calls::incrementAndGet,0,100)==null,"late ingress reopened a closed connection");
        var expired=registry.enqueue(b,calls::incrementAndGet,0,100);
        registry.maintain(100,4,ignored->true);expired.run();
        require(expired.state()==IngressWorkRegistry.State.CANCELLED&&calls.get()==1,"expired queued callback ran");
        var failed=registry.enqueue(b,()->{throw new IllegalStateException("fixture");},0,100);
        try{failed.run();throw new AssertionError("failure swallowed");}catch(IllegalStateException expected){}
        require(failed.state()==IngressWorkRegistry.State.FAILED&&bytes.get()==512,"failed callback leaked its charge");
        registry.close();registry.close();require(bytes.get()==0,"shutdown double-released or retained connection charges");
        java.lang.ref.Reference.reachabilityFence(a);java.lang.ref.Reference.reachabilityFence(b);

        var running=new IngressWorkRegistry<Object>(1,1,size->{bytes.addAndGet(size);return ()->bytes.addAndGet(-size);});
        var entered=new CountDownLatch(1);var proceed=new CountDownLatch(1);Object owner=new Object();
        var active=running.enqueue(owner,()->{
            entered.countDown();try{if(!proceed.await(2,TimeUnit.SECONDS))throw new AssertionError("fixture timed out");}
            catch(InterruptedException error){throw new AssertionError(error);}calls.incrementAndGet();
        },0,100);
        Thread worker=new Thread(active,"ronova-ingress-running");worker.setDaemon(true);worker.start();
        try {
            require(entered.await(2,TimeUnit.SECONDS),"work did not enter");
            running.cancelOwner(owner);running.close();
            require(active.state()==IngressWorkRegistry.State.CANCEL_REQUESTED&&bytes.get()==4096,
                "running callback was falsely cancelled or released before it finished");
        } finally {proceed.countDown();worker.join(2000);}
        require(!worker.isAlive()&&active.state()==IngressWorkRegistry.State.COMPLETED&&bytes.get()==0,
            "running callback failed to release after actual completion");

        var denied=new IngressWorkRegistry<Object>(1,1,size->size==256?()->{}:null);
        require(denied.enqueue(owner,()->{},0,100)==null&&denied.counters().get("ingress_pending")==0,
            "unreserved work entered the queue");denied.close();
        System.out.println("IngressWorkRegistrySelfTest PASS");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
