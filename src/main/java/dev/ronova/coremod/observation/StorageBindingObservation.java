package dev.ronova.coremod.observation;

import java.util.function.LongSupplier;

/** An unstable read blocks execution but does not prove the previously observed holder disappeared. */
public enum StorageBindingObservation {
    BOUND, DETACHED, UNAVAILABLE;
    @FunctionalInterface public interface Read { boolean matches() throws ReflectiveOperationException; }
    public static StorageBindingObservation read(LongSupplier version, Read binding) {
        try {
            long before = version.getAsLong();
            if (before < 0) return UNAVAILABLE;
            boolean matches = binding.matches();
            if (version.getAsLong() != before) return UNAVAILABLE;
            return matches ? BOUND : DETACHED;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return UNAVAILABLE; }
    }
}
