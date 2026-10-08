package dev.ronova.coremod.engine.spawn;

import dev.ronova.coremod.budget.BudgetCategory;
import dev.ronova.coremod.budget.BudgetReceipt;
import dev.ronova.coremod.budget.FrameBudgetGovernor;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Regression checks for bounded retry ordering, accounting and serialization. */
public final class BoundedRetryQueueSelfTest {
    private static final long WAIT_SECONDS = 5L;

    private BoundedRetryQueueSelfTest() {}

    public static void main(String[] args) throws Exception {
        verifiesDefaultPolicyAndAdmissionDedupWiring();
        verifiesSnapshotRotationAndNewEntries();
        verifiesBudgetExhaustionCarriesWork();
        verifiesPermanentFailureIsDeadLettered();
        verifiesConcurrentDrainsAreSerialized();
        verifiesInputValidationAndClear();
        verifiesDisposalLifecycle();
        System.out.println("BoundedRetryQueueSelfTest: PASS");
    }

    private static void verifiesDisposalLifecycle() {
        var released = new java.util.HashMap<Integer, Integer>();
        BoundedRetryQueue<Integer> queue = new BoundedRetryQueue<>(2, 1, value -> released.merge(value, 1, Integer::sum));
        queue.addLast(1); queue.addLast(2); queue.drain(value -> false);
        require(released.isEmpty(), "retry released a still-retained allocation");
        queue.drain(value -> false);
        require(released.get(1) == 1 && !released.containsKey(2), "dead-letter eviction/retenion release is wrong");
        queue.addLast(3); queue.drain(value -> true); queue.removeIf(value -> value == 2);
        queue.addLast(4);
        try { queue.drain(value -> { throw new IllegalStateException("fixture"); }); }
        catch (IllegalStateException expected) { }
        require(queue.size() == 1 && !released.containsKey(4), "throwing processor lost retained work");
        queue.clear(); queue.clear();
        require(released.size() == 4 && released.values().stream().allMatch(count -> count == 1), "queue disposal leaked or double-released");
    }

    private static void verifiesDefaultPolicyAndAdmissionDedupWiring() throws Exception {
        BoundedRetryQueue<Integer> queue = new BoundedRetryQueue<>();
        require(queue.maxAttempts() == 8,
            "production retry policy no longer uses eight attempts per entry");
        require(queue.deadLetterCapacity() == 1_024,
            "production dead-letter diagnostics no longer use the bounded capacity");

        String runtimeName =
            "dev/ronova/coremod/engine/spawn/SpawnAdmissionRuntime";
        ClassNode runtime = new ClassNode();
        try (InputStream input = BoundedRetryQueueSelfTest.class.getClassLoader()
            .getResourceAsStream(runtimeName + ".class")) {
            require(input != null, "missing SpawnAdmissionRuntime.class");
            new ClassReader(input).accept(runtime, 0);
        }

        int dedupCalls = 0;
        int enqueueCalls = 0;
        int dedupOrder = -1;
        int enqueueOrder = -1;
        for (MethodNode method : runtime.methods) {
            if (!"observeConstruction".equals(method.name)) {
                continue;
            }
            int order = 0;
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode call)) {
                    continue;
                }
                if ("dev/ronova/coremod/util/WeakIdentitySet".equals(call.owner)
                    && "addBounded".equals(call.name)) {
                    dedupCalls++;
                    dedupOrder = order;
                }
                if ("dev/ronova/coremod/engine/spawn/BoundedRetryQueue".equals(call.owner)
                    && "addLast".equals(call.name)) {
                    enqueueCalls++;
                    enqueueOrder = order;
                }
                order++;
            }
        }
        require(dedupCalls == 1 && enqueueCalls == 1 && dedupOrder < enqueueOrder,
            "admission construction work is not identity-deduplicated before enqueue");
    }

    private static void verifiesSnapshotRotationAndNewEntries() {
        BoundedRetryQueue<Integer> queue = new BoundedRetryQueue<>();
        queue.addLast(1);
        queue.addLast(2);
        queue.addLast(3);
        List<Integer> firstPass = new ArrayList<>();

        queue.drain(value -> {
            firstPass.add(value);
            if (value == 2) {
                queue.addLast(4);
            }
            return value != 1;
        });

        require(firstPass.equals(List.of(1, 2, 3)),
            "ungated drain did not process exactly its entry snapshot");
        require(queue.size() == 2,
            "failed work or a concurrently added entry was dropped or over-drained");

        List<Integer> retryPass = new ArrayList<>();
        queue.drain(value -> {
            retryPass.add(value);
            return true;
        });
        require(retryPass.equals(List.of(1, 4)) && queue.size() == 0,
            "failed work was not rotated behind the snapshot for the next drain");
    }

    private static void verifiesBudgetExhaustionCarriesWork() {
        BoundedRetryQueue<Integer> queue = new BoundedRetryQueue<>();
        queue.addLast(1);
        queue.addLast(2);
        queue.addLast(3);
        List<Integer> firstPass = new ArrayList<>();

        FrameBudgetGovernor bounded = new FrameBudgetGovernor(true);
        bounded.beginTick(1L);
        BudgetReceipt first = queue.drain(
            bounded.budget(BudgetCategory.TERMINAL),
            value -> {
                firstPass.add(value);
                if (value == 1) {
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError("budget fixture was interrupted", interrupted);
                    }
                }
                return true;
            }
        );

        require(first.deferred() >= 1L && first.overBudget(),
            "budget exhaustion did not defer a queued entry");
        require(first.attempted() == first.processed() + first.deferred()
                && first.processed() == firstPass.size()
                && first.carryIn() == 3L
                && first.carryOut() == queue.size()
                && first.carryOut() > 0L,
            "budgeted drain returned inconsistent carry accounting");

        List<Integer> recoveryPass = new ArrayList<>();
        FrameBudgetGovernor recovery = new FrameBudgetGovernor(false);
        recovery.beginTick(2L);
        BudgetReceipt recovered = queue.drain(
            recovery.budget(BudgetCategory.TERMINAL),
            value -> {
                recoveryPass.add(value);
                return true;
            }
        );
        List<Integer> allValues = new ArrayList<>(firstPass);
        allValues.addAll(recoveryPass);
        require(allValues.equals(List.of(1, 2, 3)) && queue.size() == 0,
            "deferred retry work did not recover exactly once on a later tick");
        require(recovered.processed() == recoveryPass.size()
                && recovered.deferred() == 0L
                && recovered.carryIn() == first.carryOut()
                && recovered.carryOut() == 0L,
            "recovery drain did not close the carried backlog");
    }

    private static void verifiesPermanentFailureIsDeadLettered() {
        BoundedRetryQueue<String> queue = new BoundedRetryQueue<>(3, 2);
        queue.addLast("permanent");
        queue.addLast("later-success");
        AtomicInteger permanentAttempts = new AtomicInteger();
        AtomicInteger successfulLaterEntries = new AtomicInteger();
        BudgetReceipt finalReceipt = null;

        for (int tick = 1; tick <= 100 && queue.size() > 0; tick++) {
            FrameBudgetGovernor governor = new FrameBudgetGovernor(false);
            governor.beginTick(tick);
            finalReceipt = queue.drain(
                governor.budget(BudgetCategory.ADMISSION),
                value -> {
                    if ("permanent".equals(value)) {
                        permanentAttempts.incrementAndGet();
                        return false;
                    }
                    successfulLaterEntries.incrementAndGet();
                    return true;
                }
            );
        }

        require(successfulLaterEntries.get() == 1,
            "a permanently failing head blocked a later successful entry");
        require(permanentAttempts.get() == 3 && queue.size() == 0,
            "permanent work did not leave the active backlog at its retry limit");
        require(queue.deadLetterCount() == 1 && queue.deadLetteredTotal() == 1L,
            "exhausted work was not retained as a dead-letter diagnostic");
        require(queue.deadLetters().get(0).value().equals("permanent")
                && queue.deadLetters().get(0).attempts() == 3,
            "dead-letter diagnostics lost the failed value or attempt count");
        require(finalReceipt != null && finalReceipt.carryOut() == 0L
                && finalReceipt.deferred() == 0L,
            "dead-letter transition left retry carry in the final receipt");

        for (String value : List.of("second", "third", "fourth")) {
            queue.addLast(value);
            for (int attempt = 0; attempt < 3; attempt++) {
                queue.drain(ignored -> false);
            }
        }
        require(queue.size() == 0 && queue.deadLetterCount() == 2
                && queue.deadLetteredTotal() == 4L,
            "dead-letter retention was not bounded while total diagnostics advanced");
        require(queue.deadLetters().stream().map(BoundedRetryQueue.DeadLetter::value)
                .toList().equals(List.of("third", "fourth")),
            "dead-letter capacity did not evict the oldest diagnostic first");
    }

    private static void verifiesConcurrentDrainsAreSerialized() throws Exception {
        BoundedRetryQueue<Integer> queue = new BoundedRetryQueue<>();
        queue.addLast(1);
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        List<Integer> processed = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger activeProcessors = new AtomicInteger();
        AtomicInteger maximumActiveProcessors = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread first = new Thread(() -> {
            try {
                queue.drain(value -> {
                    int active = activeProcessors.incrementAndGet();
                    maximumActiveProcessors.updateAndGet(
                        previous -> Math.max(previous, active)
                    );
                    firstEntered.countDown();
                    try {
                        if (!releaseFirst.await(WAIT_SECONDS, TimeUnit.SECONDS)) {
                            throw new AssertionError("first drain release timed out");
                        }
                        processed.add(value);
                        return true;
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError("first drain was interrupted", interrupted);
                    } finally {
                        activeProcessors.decrementAndGet();
                    }
                });
            } catch (Throwable thrown) {
                failure.compareAndSet(null, thrown);
                releaseFirst.countDown();
            }
        }, "bounded-retry-first");

        Thread second = new Thread(() -> {
            try {
                secondStarted.countDown();
                queue.drain(value -> {
                    int active = activeProcessors.incrementAndGet();
                    maximumActiveProcessors.updateAndGet(
                        previous -> Math.max(previous, active)
                    );
                    try {
                        processed.add(value);
                        return true;
                    } finally {
                        activeProcessors.decrementAndGet();
                    }
                });
            } catch (Throwable thrown) {
                failure.compareAndSet(null, thrown);
            }
        }, "bounded-retry-second");

        first.start();
        require(firstEntered.await(WAIT_SECONDS, TimeUnit.SECONDS),
            "first drain did not enter its processor");
        queue.addLast(2);
        second.start();
        require(secondStarted.await(WAIT_SECONDS, TimeUnit.SECONDS),
            "second drain did not start");
        releaseFirst.countDown();
        first.join(TimeUnit.SECONDS.toMillis(WAIT_SECONDS));
        second.join(TimeUnit.SECONDS.toMillis(WAIT_SECONDS));

        require(!first.isAlive() && !second.isAlive(),
            "serialized drain threads did not finish");
        if (failure.get() != null) {
            throw new AssertionError("concurrent drain failed", failure.get());
        }
        require(maximumActiveProcessors.get() == 1,
            "two drain processors ran concurrently");
        require(processed.equals(List.of(1, 2)) && queue.size() == 0,
            "serialized drains lost or duplicated queued work");
    }

    private static void verifiesInputValidationAndClear() {
        BoundedRetryQueue<Integer> queue = new BoundedRetryQueue<>();
        boolean nullValueRejected = false;
        try {
            queue.addLast(null);
        } catch (NullPointerException expected) {
            nullValueRejected = true;
        }
        require(nullValueRejected, "null retry work was accepted");

        boolean nullProcessorRejected = false;
        try {
            queue.drain((BoundedRetryQueue.Processor<Integer>) null);
        } catch (NullPointerException expected) {
            nullProcessorRejected = true;
        }
        require(nullProcessorRejected, "null ungated processor was accepted");

        boolean nullBudgetRejected = false;
        try {
            queue.drain(null, value -> true);
        } catch (NullPointerException expected) {
            nullBudgetRejected = true;
        }
        require(nullBudgetRejected, "null work budget was accepted");

        queue.addLast(7);
        queue.addLast(8);
        queue.clear();
        require(queue.size() == 0, "clear did not remove all retry work");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
