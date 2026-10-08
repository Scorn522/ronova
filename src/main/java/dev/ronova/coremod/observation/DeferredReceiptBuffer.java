package dev.ronova.coremod.observation;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Reserve result retention before starting a mutation; retry only the receipt, never the mutation. */
final class DeferredReceiptBuffer<K, V> implements AutoCloseable {
    private final int capacity;
    private final Supplier<Runnable> reserve;
    private final ArrayDeque<Ticket> rotation = new ArrayDeque<>();
    private boolean closed;
    DeferredReceiptBuffer(int capacity, Supplier<Runnable> reserve) {
        if (capacity < 1) throw new IllegalArgumentException("invalid receipt capacity");
        this.capacity = capacity; this.reserve = Objects.requireNonNull(reserve);
    }
    final class Ticket implements AutoCloseable {
        final K key;
        private final Runnable release;
        private V result;
        private boolean released;
        private Ticket(K key, Runnable release) { this.key = key; this.release = release; }
        void complete(V result) {
            synchronized (DeferredReceiptBuffer.this) {
                if (released) return; // Shutdown preserves the pre-existing durable unknown intent.
                if (this.result != null) throw new IllegalStateException("receipt already completed");
                this.result = Objects.requireNonNull(result);
            }
        }
        @Override public void close() {
            synchronized (DeferredReceiptBuffer.this) {
                if (released) return;
                released = true; rotation.remove(this); release.run();
            }
        }
    }
    synchronized Ticket reserve(K key) {
        Objects.requireNonNull(key);
        if (closed || rotation.size() >= capacity) return null;
        Runnable release = reserve.get();
        if (release == null) return null;
        var ticket = new Ticket(key, release); rotation.addLast(ticket); return ticket;
    }
    synchronized int flush(int limit, BooleanSupplier budget, BiPredicate<K, V> publish) {
        if (closed || limit < 1) return 0;
        int visited = rotation.size(), attempted = 0;
        while (visited-- > 0 && attempted < limit && !rotation.isEmpty()) {
            var ticket = rotation.removeFirst(); rotation.addLast(ticket);
            if (ticket.result == null) continue;
            if (!budget.getAsBoolean()) break;
            attempted++;
            boolean accepted;
            try { accepted = publish.test(ticket.key, ticket.result); }
            catch (RuntimeException | LinkageError unavailable) { accepted = false; }
            if (accepted) ticket.close();
        }
        return attempted;
    }
    synchronized int size() { return rotation.size(); }
    /** A shutdown snapshot includes only already-completed work. Failure retains every ticket. */
    synchronized boolean flushBatch(java.util.function.Predicate<java.util.List<java.util.Map.Entry<K, V>>> publish) {
        if (closed) return false;
        var ready = rotation.stream().filter(ticket -> ticket.result != null).toList();
        if (ready.isEmpty()) return true;
        var values = ready.stream().map(ticket -> java.util.Map.entry(ticket.key, ticket.result)).toList();
        boolean accepted;
        try { accepted = publish.test(values); }
        catch (RuntimeException | LinkageError unavailable) { accepted = false; }
        if (!accepted) return false;
        RuntimeException failure = null;
        for (var ticket : ready) try { ticket.close(); }
        catch (RuntimeException error) {
            if (failure == null) failure = error; else if (failure != error) failure.addSuppressed(error);
        }
        if (failure != null) throw failure;
        return true;
    }
    synchronized int readyCount() { return (int) rotation.stream().filter(ticket -> ticket.result != null).count(); }
    synchronized boolean anyKey(java.util.function.Predicate<K> predicate) {
        return rotation.stream().anyMatch(ticket -> predicate.test(ticket.key));
    }
    @Override public synchronized void close() {
        if (closed) return; closed = true;
        RuntimeException failure = null;
        while (!rotation.isEmpty()) {
            try { rotation.peekFirst().close(); }
            catch (RuntimeException error) {
                if (failure == null) failure = error; else if (failure != error) failure.addSuppressed(error);
            }
        }
        if (failure != null) throw failure;
    }
}
