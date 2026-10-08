package dev.ronova.coremod.observation;

import dev.ronova.coremod.budget.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public final class StorageImageTaskSelfTest {
    public static void main(String[] args) throws Exception {
        var request=new StorageImageTask.Request(Path.of("unused-fixture"),List.of(List.of(new NbtStoragePreservation.Key("target"))),1024,1024,64);
        var governor=new HostResourceGovernor();var entered=new CountDownLatch(2);var release=new CountDownLatch(1);
        java.util.function.Function<java.util.function.BooleanSupplier,StorageImageTask.Prepared> blocked=active->{
            require(Thread.currentThread().getContextClassLoader()==null,"worker retained caller loader");entered.countDown();
            try{if(!release.await(5,TimeUnit.SECONDS))throw new AssertionError("fixture timeout");}
            catch(InterruptedException failure){throw new AssertionError("worker interrupted",failure);}
            return empty();
        };
        var first=StorageImageTask.startOperation(request,governor,blocked);
        var second=StorageImageTask.startOperation(request,governor,blocked);
        require(first!=null&&second!=null&&entered.await(5,TimeUnit.SECONDS),"workers did not start");
        try {
            require(StorageImageTask.startOperation(request,governor,ignored->empty())==null,"worker cap exceeded");
            first.close();second.close();
            require(first.state()==StorageImageTask.State.CANCEL_PENDING&&!first.done()&&first.take()==null&&used(governor)>0,"cancel fabricated exit or released active work");
        } finally {release.countDown();}
        await(first);await(second);require(used(governor)==0,"cancelled work leaked reservations");
        var success=StorageImageTask.startOperation(request,governor,ignored->empty());require(success!=null,"capacity did not recover");await(success);
        var output=success.take();require(output!=null&&success.take()==null,"result transfer was not single-use");
        success.close();require(success.state()==StorageImageTask.State.FINISHED&&used(governor)>0,"task close released transferred result");
        output.close();require(used(governor)==0,"result reservation not released");
        var failed=StorageImageTask.startOperation(request,governor,ignored->{throw new IllegalStateException("fixture failure");});await(failed);
        require(failed.state()==StorageImageTask.State.FAILED&&failed.take()==null&&used(governor)==0,"failed work leaked a result or reservation");failed.close();
        System.out.println("StorageImageTaskSelfTest PASS (bounded workers, cancellation/exit, result transfer, cleanup, failure)");
    }
    private static StorageImageTask.Prepared empty(){return new StorageImageTask.Prepared(new RecoveryFileReadback.Result(RecoveryFileReadback.Status.UNAVAILABLE,RecoveryFileReadback.Reason.PRECONDITION_UNPROVEN,0,null),null);}
    private static long used(HostResourceGovernor governor){return governor.snapshot().stream().filter(row->row.ownedUsage()!=null).mapToLong(row->row.ownedUsage()).sum();}
    private static void await(StorageImageTask task) throws Exception {long end=System.nanoTime()+5_000_000_000L;while(!task.done()&&System.nanoTime()<end)Thread.sleep(1);require(task.done(),"worker did not exit");}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
