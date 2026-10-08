package dev.ronova.coremod.observation;

import dev.ronova.coremod.integrity.EnforcementProof;
import dev.ronova.coremod.integrity.EnforcementProofSource;
import dev.ronova.coremod.integrity.EnforcementRuntime;
import java.util.concurrent.atomic.AtomicReference;

/** Regression: runtime construction may occur on a mod-loading worker, but server ownership binds later. */
final class RecoveryTaskObserverLaneSelfTest {
    private RecoveryTaskObserverLaneSelfTest() {}
    static void run() throws Exception {
        AtomicReference<RecoveryTaskObserver> created=new AtomicReference<>();
        Thread constructor=new Thread(()->created.set(new RecoveryTaskObserver(()->{},
            new EnforcementRuntime((EnforcementProofSource)()->EnforcementProof.UNAVAILABLE),copy->{})),
            "ronova-observer-constructor-fixture");
        constructor.start();constructor.join();
        RecoveryTaskObserver observer=created.get();require(observer!=null,"observer fixture was not constructed");
        observer.bindServerLane(); // The test thread represents Minecraft's verified server lane.
        AtomicReference<Throwable> foreignFailure=new AtomicReference<>();
        Thread foreign=new Thread(()->{try{observer.bindServerLane();}catch(Throwable failure){foreignFailure.set(failure);}},
            "ronova-observer-wrong-lane-fixture");
        foreign.start();foreign.join();
        require(foreignFailure.get() instanceof IllegalStateException,"foreign lane rebound task evidence ownership");
        observer.close();
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
