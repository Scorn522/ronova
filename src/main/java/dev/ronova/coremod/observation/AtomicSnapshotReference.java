package dev.ronova.coremod.observation;

import java.util.concurrent.atomic.AtomicReference;

/** Only the exact JDK reference container; no arbitrary supplier/getter invocation. */
public final class AtomicSnapshotReference {
    public enum ClearResult { CLEARED, NOT_CURRENT, FENCED, UNSUPPORTED }
    private AtomicSnapshotReference() {}
    public static Object unwrap(Object slot) {
        return slot!=null&&slot.getClass()==AtomicReference.class?((AtomicReference<?>)slot).get():slot;
    }
    public static boolean bound(Object slot,Object expectedSnapshot) {
        return expectedSnapshot!=null&&unwrap(slot)==expectedSnapshot;
    }
    /** No getter/setter callback; CAS cannot clear a replacement image installed after the gate. */
    @SuppressWarnings("unchecked")
    public static ClearResult clear(Object slot, Object expectedSnapshot, java.util.function.BooleanSupplier permitted) {
        java.util.Objects.requireNonNull(permitted);
        if (slot == null || slot.getClass() != AtomicReference.class || expectedSnapshot == null) return ClearResult.UNSUPPORTED;
        if (!permitted.getAsBoolean()) return ClearResult.FENCED;
        return ((AtomicReference<Object>) slot).compareAndSet(expectedSnapshot, null) ? ClearResult.CLEARED : ClearResult.NOT_CURRENT;
    }
}
