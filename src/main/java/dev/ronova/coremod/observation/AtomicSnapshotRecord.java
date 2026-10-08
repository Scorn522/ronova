package dev.ronova.coremod.observation;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Exact JDK map entry mutation without invoking a foreign record's equals or callbacks. */
public final class AtomicSnapshotRecord {
    public enum Result { REMOVED, NOT_CURRENT, FENCED, UNSUPPORTED }
    /** The map operation returned before cleanup failed; this is not a verified disposition receipt. */
    public static final class CompletionFailure extends RuntimeException {
        private final Result completedResult;
        private CompletionFailure(Result completedResult, Throwable cause) {
            super("record operation completed but permit cleanup failed", cause);
            this.completedResult = completedResult;
        }
        public Result completedResult() { return completedResult; }
    }
    public interface Permit extends AutoCloseable {
        boolean valid();
        @Override void close();
    }
    private AtomicSnapshotRecord() {}
    /** Keep the exact key unchanged while a registered final commit runs. Acquire outer authority inside action. */
    @SuppressWarnings("unchecked")
    public static boolean guardSlot(Object container,UUID subject,Object expected,Runnable action) {
        if(container==null||container.getClass()!=ConcurrentHashMap.class||subject==null||action==null)return false;
        boolean[] entered={false};
        ((ConcurrentHashMap<UUID,Object>)container).compute(subject,(key,value)->{
            if(value==expected){action.run();entered[0]=true;}
            return value;
        });
        return entered[0];
    }
    /** permitted must only read registered authority/bindings, never mutate this map or call foreign code. */
    @SuppressWarnings("unchecked")
    public static Result remove(Object container, UUID subject, Object expected, BooleanSupplier permitted) {
        return removeWithPermit(container, subject, expected, () -> new Permit() {
            public boolean valid() { return permitted.getAsBoolean(); }
            public void close() {}
        });
    }
    /** Acquire authority only after the container lock; retain it through actual entry publication. */
    @SuppressWarnings("unchecked")
    public static Result removeWithPermit(Object container, UUID subject, Object expected, Supplier<Permit> acquire) {
        if (container == null || container.getClass() != ConcurrentHashMap.class || subject == null || expected == null)
            return Result.UNSUPPORTED;
        var result = new Result[] { Result.NOT_CURRENT };
        var acquired = new Permit[1];
        Result completed = null;
        Throwable operationFailure = null;
        // remove(key, value) can call a foreign value's equals after a replacement.
        // The registered remapping body instead uses identity under the map's entry lock.
        try {
        ((ConcurrentHashMap<UUID, Object>) container).computeIfPresent(subject, (key, current) -> {
            if (current != expected) return current;
            var permit = acquired[0] = acquire.get();
            if (permit == null || !permit.valid()) { result[0] = Result.FENCED; return current; }
            result[0] = Result.REMOVED;
            return null;
        });
        completed = result[0];
        return completed;
        } catch (RuntimeException | Error failure) {
            operationFailure = failure; throw failure;
        } finally {
            if (acquired[0] != null) try { acquired[0].close(); }
            catch (RuntimeException | Error cleanupFailure) {
                if (operationFailure != null) {
                    if (operationFailure != cleanupFailure) operationFailure.addSuppressed(cleanupFailure);
                } else if (completed != null) throw new CompletionFailure(completed, cleanupFailure);
                else throw cleanupFailure;
            }
        }
    }
}
