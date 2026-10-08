package dev.ronova.coremod.observation;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class StorageMutationSequenceSelfTest {
    public static void main(String[] args) throws Exception {
        var sequence = new StorageMutationSequence(); long original = sequence.observationVersion();
        sequence.begin(); require(sequence.observationVersion() < 0, "active replacement was observable as stable");
        sequence.begin(); sequence.end();
        require(sequence.observationVersion() < 0, "inner replacement ended its outer window");
        sequence.end(); long after = sequence.observationVersion();
        require(after >= 0 && after != original, "replacement reused an old stable observation version");
        try { sequence.begin(); throw new IllegalStateException("fixture cache put failure"); }
        catch (IllegalStateException expected) { }
        finally { sequence.end(); }
        require(sequence.observationVersion() >= 0 && sequence.observationVersion() != after,
            "exception exit lost the mutation generation or left a stuck window");
        var entered = new CountDownLatch(1); var exit = new CountDownLatch(1);
        var worker = new Thread(() -> {
            sequence.begin(); entered.countDown();
            try { if (!exit.await(5, TimeUnit.SECONDS)) throw new AssertionError("fixture timed out"); }
            catch (InterruptedException error) { throw new AssertionError(error); }
            finally { sequence.end(); }
        });
        worker.setDaemon(true); worker.start();
        try {
            require(entered.await(5, TimeUnit.SECONDS), "worker did not enter");
            require(sequence.observationVersion() < 0, "another thread did not observe active replacement");
        } finally { exit.countDown(); worker.join(5000); }
        require(!worker.isAlive() && sequence.observationVersion() >= 0, "worker replacement did not finish");
        sequence.end(); sequence.begin(); sequence.end();
        require(sequence.observationVersion() < 0, "unmatched completion silently restored trusted observations");
        var exhausted = new StorageMutationSequence();
        for (int index = 0; index <= 0xffff; index++) exhausted.begin();
        for (int index = 0; index <= 0xffff; index++) exhausted.end();
        require(exhausted.observationVersion() < 0, "nesting overflow wrapped back to stable evidence");
        bindingObservations();
        System.out.println("StorageMutationSequenceSelfTest PASS");
    }
    private static void bindingObservations() {
        var sequence = new StorageMutationSequence();
        require(StorageBindingObservation.read(sequence::observationVersion, () -> true) == StorageBindingObservation.BOUND,
            "stable binding was not usable");
        sequence.begin();
        require(StorageBindingObservation.read(sequence::observationVersion, () -> {
            throw new AssertionError("read during active cache mutation");
        }) == StorageBindingObservation.UNAVAILABLE, "in-flight mutation retired a binding");
        sequence.end();
        require(StorageBindingObservation.read(sequence::observationVersion, () -> {
            sequence.begin(); sequence.end(); return false;
        }) == StorageBindingObservation.UNAVAILABLE, "unstable mismatch was treated as confirmed detachment");
        require(StorageBindingObservation.read(sequence::observationVersion, () -> {
            sequence.begin(); sequence.end(); return true;
        }) == StorageBindingObservation.UNAVAILABLE, "ABA change retained old observation evidence");
        require(StorageBindingObservation.read(sequence::observationVersion, () -> true) == StorageBindingObservation.BOUND,
            "completed unrelated mutation permanently prevented binding observation");
        require(StorageBindingObservation.read(sequence::observationVersion, () -> false) == StorageBindingObservation.DETACHED,
            "stable holder mismatch did not retire the source");
        require(StorageBindingObservation.read(sequence::observationVersion, () -> {
            throw new IllegalAccessException("fixture inaccessible field");
        }) == StorageBindingObservation.UNAVAILABLE, "metadata failure falsely proved source removal");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
