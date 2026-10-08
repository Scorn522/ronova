package dev.ronova.coremod.engine.spawn;

import dev.ronova.coremod.budget.BudgetReceipt;
import dev.ronova.coremod.budget.WorkBudget;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Thread-safe FIFO drain with finite per-entry retries and bounded dead letters. */
final class BoundedRetryQueue<T> {
    static final int DEFAULT_MAX_ATTEMPTS = 8;
    static final int DEFAULT_DEAD_LETTER_CAPACITY = 1_024;

    private final Object queueLock = new Object();
    private final Object drainLock = new Object();
    private final ArrayDeque<Entry<T>> queue = new ArrayDeque<>();
    private final ArrayDeque<DeadLetter<T>> deadLetters = new ArrayDeque<>();
    private final int maxAttempts;
    private final int deadLetterCapacity;
    private final java.util.function.Consumer<T> dispose;
    private long deadLetteredTotal;

    BoundedRetryQueue() {
        this(DEFAULT_MAX_ATTEMPTS, DEFAULT_DEAD_LETTER_CAPACITY);
    }

    BoundedRetryQueue(int maxAttempts, int deadLetterCapacity) {
        this(maxAttempts, deadLetterCapacity, ignored -> {});
    }

    BoundedRetryQueue(int maxAttempts, int deadLetterCapacity, java.util.function.Consumer<T> dispose) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        if (deadLetterCapacity <= 0) {
            throw new IllegalArgumentException("deadLetterCapacity must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.deadLetterCapacity = deadLetterCapacity;
        this.dispose = Objects.requireNonNull(dispose, "dispose");
    }

    void addLast(T value) {
        addLast(new Entry<>(Objects.requireNonNull(value, "value"), 0));
    }

    /** Runs one ungated snapshot; a failed head cannot starve later entries. */
    void drain(Processor<? super T> processor) {
        Objects.requireNonNull(processor, "processor");
        synchronized (drainLock) {
            long attemptLimit = size();
            long attempted = 0L;
            while (attempted < attemptLimit) {
                Entry<T> entry = pollFirst();
                if (entry == null) {
                    break;
                }
                attempted++;
                if (!process(entry, processor)) {
                    retryOrDeadLetter(entry);
                } else dispose.accept(entry.value());
            }
        }
    }

    /** Runs at most one entry-snapshot under the supplied count/time budget. */
    BudgetReceipt drain(WorkBudget budget, Processor<? super T> processor) {
        Objects.requireNonNull(budget, "budget");
        Objects.requireNonNull(processor, "processor");
        synchronized (drainLock) {
            long carryIn = size();
            long attempted = 0L;
            long processed = 0L;
            long deferred = 0L;
            while (attempted < carryIn) {
                Entry<T> candidate = peekFirst();
                if (candidate == null) {
                    break;
                }
                if (!budget.tryConsumeCount(1)) {
                    budget.deferCount(1);
                    deferred++;
                    break;
                }
                Entry<T> claimed = pollFirst(candidate);
                if (claimed == null) {
                    budget.refundCount(1);
                    continue;
                }
                attempted++;
                if (process(claimed, processor)) {
                    dispose.accept(claimed.value());
                    processed++;
                } else if (retryOrDeadLetter(claimed)) {
                    deferred++;
                } else {
                    // Moving an exhausted entry to dead letters completes this
                    // active-queue work unit; it is no longer carry-out work.
                    processed++;
                }
            }
            return budget.finish(processed, deferred, carryIn, size());
        }
    }

    int size() {
        synchronized (queueLock) {
            return queue.size();
        }
    }

    int maxAttempts() {
        return maxAttempts;
    }

    int deadLetterCapacity() {
        return deadLetterCapacity;
    }

    int deadLetterCount() {
        synchronized (queueLock) {
            return deadLetters.size();
        }
    }

    long deadLetteredTotal() {
        synchronized (queueLock) {
            return deadLetteredTotal;
        }
    }

    List<DeadLetter<T>> deadLetters() {
        synchronized (queueLock) {
            return List.copyOf(deadLetters);
        }
    }

    void removeIf(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        synchronized (drainLock) {
            synchronized (queueLock) {
                for (var iterator = queue.iterator(); iterator.hasNext();) {
                    Entry<T> entry = iterator.next();
                    if (predicate.test(entry.value())) { iterator.remove(); dispose.accept(entry.value()); }
                }
                for (var iterator = deadLetters.iterator(); iterator.hasNext();) {
                    DeadLetter<T> entry = iterator.next();
                    if (predicate.test(entry.value())) { iterator.remove(); dispose.accept(entry.value()); }
                }
            }
        }
    }

    void clear() {
        synchronized (drainLock) {
            synchronized (queueLock) {
                queue.forEach(entry -> dispose.accept(entry.value())); queue.clear();
                deadLetters.forEach(entry -> dispose.accept(entry.value())); deadLetters.clear();
                deadLetteredTotal = 0L;
            }
        }
    }

    /** Returns true when the entry remains active, false when it is quarantined. */
    private boolean retryOrDeadLetter(Entry<T> entry) {
        int attempts = entry.failedAttempts() + 1;
        if (attempts < maxAttempts) {
            addLast(new Entry<>(entry.value(), attempts));
            return true;
        }
        synchronized (queueLock) {
            if (deadLetters.size() >= deadLetterCapacity) {
                dispose.accept(deadLetters.removeFirst().value());
            }
            deadLetters.addLast(new DeadLetter<>(entry.value(), attempts));
            if (deadLetteredTotal != Long.MAX_VALUE) {
                deadLetteredTotal++;
            }
        }
        return false;
    }

    private void addLast(Entry<T> entry) {
        synchronized (queueLock) {
            queue.addLast(entry);
        }
    }

    private boolean process(Entry<T> entry, Processor<? super T> processor) {
        try { return processor.process(entry.value()); }
        catch (RuntimeException | Error failed) { addLast(entry); throw failed; }
    }

    private Entry<T> peekFirst() {
        synchronized (queueLock) {
            return queue.peekFirst();
        }
    }

    private Entry<T> pollFirst() {
        synchronized (queueLock) {
            return queue.pollFirst();
        }
    }

    private Entry<T> pollFirst(Entry<T> expected) {
        synchronized (queueLock) {
            if (queue.peekFirst() != expected) {
                return null;
            }
            return queue.pollFirst();
        }
    }

    record DeadLetter<T>(T value, int attempts) {
        DeadLetter {
            Objects.requireNonNull(value, "value");
            if (attempts <= 0) {
                throw new IllegalArgumentException("attempts must be positive");
            }
        }
    }

    private record Entry<T>(T value, int failedAttempts) {
        private Entry {
            Objects.requireNonNull(value, "value");
            if (failedAttempts < 0) {
                throw new IllegalArgumentException("failedAttempts must be non-negative");
            }
        }
    }

    @FunctionalInterface
    interface Processor<T> {
        boolean process(T value);
    }
}
