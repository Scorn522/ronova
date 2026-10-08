package dev.ronova.coremod.client;

/** Thread-local reentrancy gate for state recovery and internal UI mutation. */
final class ClientRecoveryGuard {
    private final ThreadLocal<Integer> repairDepth = ThreadLocal.withInitial(() -> 0);
    private final ThreadLocal<Integer> surfaceDepth = ThreadLocal.withInitial(() -> 0);

    boolean enterRepair() {
        if (repairDepth.get() != 0) {
            return false;
        }
        repairDepth.set(1);
        return true;
    }

    void exitRepair() {
        repairDepth.remove();
    }

    boolean enterSurfaceMutation() {
        if (surfaceDepth.get() != 0) {
            return false;
        }
        surfaceDepth.set(1);
        return true;
    }

    void exitSurfaceMutation() {
        surfaceDepth.remove();
    }

    boolean isInternalSurfaceMutation() {
        return surfaceDepth.get() != 0;
    }

    void reset() {
        repairDepth.remove();
        surfaceDepth.remove();
    }
}
