package dev.ronova.coremod.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Pure-Java check of the bootstrap per-task cancellation gate; no Native library is required. */
public final class BootstrapTaskGateSelfTest {
    public static void main(String[] args) throws Exception {
        require(BootstrapTaskHooks.CANCEL==5,"bootstrap cancellation event id drifted");
        var registry=new BootstrapTaskHooks.GateRegistry(4);
        Object task=new Object();
        var first=registry.acquire(task);var second=registry.acquire(task);
        require(first!=null&&first==second,"same task did not reuse its cancellation gate");
        Object equalA=new EqualTask(7),equalB=new EqualTask(7);
        require(registry.acquire(equalA)!=registry.acquire(equalB),"task gates used foreign equals instead of identity");

        var order=Collections.synchronizedList(new ArrayList<String>());
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        Thread publisher=new Thread(()->{
            synchronized(first) {
                order.add("hook-acquired");entered.countDown();
                try {require(release.await(5,TimeUnit.SECONDS),"publisher release timed out");}
                catch(InterruptedException interrupted){throw new AssertionError(interrupted);}
                order.add("hook-published");
            }
        },"gate-publisher");
        Thread canceller=new Thread(()->{
            try {require(entered.await(5,TimeUnit.SECONDS),"cancel wait timed out");}
            catch(InterruptedException interrupted){throw new AssertionError(interrupted);}
            synchronized(first) {order.add("cancel-cas");first.cancelled=true;}
        },"gate-canceller");
        publisher.start();canceller.start();
        require(entered.await(5,TimeUnit.SECONDS),"publisher did not acquire gate");
        Thread.sleep(10);release.countDown();publisher.join(5000);canceller.join(5000);
        require(!publisher.isAlive()&&!canceller.isAlive(),"gate probe threads did not finish");
        require(order.equals(java.util.List.of("hook-acquired","hook-published","cancel-cas")),
            "cancel crossed a hook publication holding the same task gate: "+order);
        synchronized(first){require(first.cancelled,"successful cancel state was not retained for later entries");}
        var committed=BootstrapTaskHooks.class.getDeclaredMethod("nativeCancelCommitted",Object.class,boolean.class);
        require(java.lang.reflect.Modifier.isNative(committed.getModifiers())&&java.lang.reflect.Modifier.isStatic(committed.getModifiers()),
            "bootstrap/native cancel-commit ABI drifted");

        var full=new BootstrapTaskHooks.GateRegistry(1);require(full.acquire(new Object())!=null,"first gate unavailable");
        require(full.acquire(new Object())==null,"bounded gate registry exceeded capacity");
        System.out.println("BootstrapTaskGateSelfTest PASS");
    }
    private record EqualTask(int value) {}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
