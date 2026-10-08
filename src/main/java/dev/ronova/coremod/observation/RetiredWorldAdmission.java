package dev.ronova.coremod.observation;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.function.Supplier;

/** Weak physical world retirement, with no LRU eviction that could reopen an unloading world. */
final class RetiredWorldAdmission<W> implements AutoCloseable {
    private record Entry<W>(WeakReference<W> world, Runnable release) {}
    private final int limit;
    private final Supplier<Runnable> reserve;
    private final ArrayList<Entry<W>> retired = new ArrayList<>();
    private boolean coverageLost, closed;
    RetiredWorldAdmission(int limit, Supplier<Runnable> reserve) {
        if (limit < 1) throw new IllegalArgumentException("invalid world retirement limit");
        this.limit = limit; this.reserve = java.util.Objects.requireNonNull(reserve);
    }
    private void reap() {
        for (var iterator = retired.iterator(); iterator.hasNext();) {
            var entry = iterator.next();
            if (entry.world.get() == null) { iterator.remove(); entry.release.run(); }
        }
    }
    synchronized boolean allows(W world) {
        if (world == null || closed || coverageLost) return false;
        reap();
        return retired.stream().noneMatch(entry -> entry.world.get() == world);
    }
    synchronized void retire(W world) {
        if (world == null || closed || coverageLost) return;
        try {
            reap();
            if (retired.stream().anyMatch(entry -> entry.world.get() == world)) return;
            if (retired.size() >= limit) { coverageLost = true; return; }
            Runnable release = reserve.get();
            if (release == null) { coverageLost = true; return; }
            retired.add(new Entry<>(new WeakReference<>(world), release));
        } catch (RuntimeException | LinkageError unavailable) { coverageLost = true; }
    }
    @Override public synchronized void close() {
        if (closed) return; closed = true;
        var releasing = new ArrayList<>(retired); retired.clear();
        RuntimeException failure = null;
        for (var entry : releasing) try { entry.release.run(); }
        catch (RuntimeException error) {
            if (failure == null) failure = error; else if (failure != error) failure.addSuppressed(error);
        }
        if (failure != null) throw failure;
    }
}
