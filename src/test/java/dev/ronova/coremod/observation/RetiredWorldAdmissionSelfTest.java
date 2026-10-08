package dev.ronova.coremod.observation;

import java.util.concurrent.atomic.AtomicInteger;

public final class RetiredWorldAdmissionSelfTest {
    private static final class World {
        @Override public boolean equals(Object other) { throw new AssertionError("world equality callback"); }
        @Override public int hashCode() { throw new AssertionError("world hash callback"); }
    }
    public static void main(String[] args) {
        var held = new AtomicInteger();
        var gate = new RetiredWorldAdmission<World>(2, () -> { held.incrementAndGet(); return held::decrementAndGet; });
        var old = new World(); var replacement = new World(); var third = new World();
        require(gate.allows(old) && gate.allows(replacement), "initial worlds were blocked");
        gate.retire(old); gate.retire(old);
        require(!gate.allows(old) && gate.allows(replacement) && held.get() == 1,
            "retirement used logical equality, duplicated reservation or blocked a new physical world");
        gate.retire(replacement);
        require(held.get() == 2 && !gate.allows(old), "second retirement evicted the first world");
        gate.retire(third);
        require(!gate.allows(third) && !gate.allows(old) && !gate.allows(new World()) && held.get() == 2,
            "capacity loss reopened an unrecorded retired world");
        gate.close(); gate.close();
        require(held.get() == 0 && !gate.allows(new World()), "closed gate admitted work or leaked reservations");
        var denied = new RetiredWorldAdmission<World>(2, () -> null);
        denied.retire(old);
        require(!denied.allows(old) && !denied.allows(replacement), "budget loss was interpreted as complete retirement coverage");
        var failed = new RetiredWorldAdmission<World>(2, () -> { throw new IllegalStateException("fixture accounting error"); });
        failed.retire(old);
        require(!failed.allows(old), "retirement registration exception reopened the source world");
        System.out.println("RetiredWorldAdmissionSelfTest PASS");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
