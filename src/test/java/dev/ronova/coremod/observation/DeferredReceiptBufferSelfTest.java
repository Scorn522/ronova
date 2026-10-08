package dev.ronova.coremod.observation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class DeferredReceiptBufferSelfTest {
    public static void main(String[] args) {
        var retained = new AtomicInteger();
        var buffer = new DeferredReceiptBuffer<String, String>(3, () -> {
            retained.incrementAndGet(); return retained::decrementAndGet;
        });
        var waiting = buffer.reserve("running-worker");
        var first = buffer.reserve("first"); var second = buffer.reserve("second");
        require(buffer.reserve("overflow") == null && retained.get() == 3, "capacity rejection allocated a reservation");
        first.complete("write-observed"); second.complete("not-current");
        var seen = new ArrayList<String>();
        require(buffer.flush(2, () -> false, (key, value) -> { throw new AssertionError("zero budget published a receipt"); }) == 0
            && buffer.size() == 3, "zero budget lost a receipt");
        require(buffer.flush(1, () -> true, (key, value) -> { seen.add(key); return false; }) == 1,
            "first failed receipt was not attempted");
        require(buffer.flush(1, () -> true, (key, value) -> { seen.add(key); return false; }) == 1
            && seen.size() == 2 && !seen.get(0).equals(seen.get(1)), "failed receipt starved another ready receipt");
        require(buffer.readyCount() == 2 && retained.get() == 3 && buffer.anyKey("running-worker"::equals),
            "failure released its reservation or lost the uncompleted task");
        seen.clear();
        buffer.flush(3, () -> true, (key, value) -> {
            seen.add(key + ":" + value);
            if (key.equals("first")) throw new IllegalStateException("fixture persistence error");
            return true;
        });
        require(seen.containsAll(List.of("first:write-observed", "second:not-current"))
            && buffer.size() == 2 && retained.get() == 2, "one publishing exception lost or blocked unrelated results");
        buffer.flush(3, () -> true, (key, value) -> key.equals("first") && value.equals("write-observed"));
        require(buffer.size() == 1 && retained.get() == 1, "retry did not preserve the exact result or release once");
        waiting.complete("cancelled-after-exit");
        require(buffer.readyCount() == 1, "worker exit did not make its receipt publishable");
        buffer.close(); buffer.close(); waiting.complete("late-shutdown");
        require(retained.get() == 0 && buffer.reserve("late") == null && buffer.size() == 0,
            "close leaked reservations or admitted late work");
        var denied = new DeferredReceiptBuffer<String, String>(1, () -> null);
        require(denied.reserve("denied") == null && denied.size() == 0, "resource denial created a ticket");
        var releases = new AtomicInteger();
        var broken = new DeferredReceiptBuffer<String, String>(2, () -> () -> {
            if (releases.incrementAndGet() == 1) throw new IllegalStateException("fixture release error");
        });
        broken.reserve("one"); broken.reserve("two");
        boolean threw = false;
        try { broken.close(); } catch (IllegalStateException expected) { threw = true; }
        require(threw && releases.get() == 2 && broken.size() == 0, "release error stranded later reservations");
        shutdownBatch();
        System.out.println("DeferredReceiptBufferSelfTest PASS");
    }
    private static void shutdownBatch() {
        var releases = new AtomicInteger();
        var buffer = new DeferredReceiptBuffer<String, String>(3, () -> releases::incrementAndGet);
        var running = buffer.reserve("running"); var done = buffer.reserve("done"); var failed = buffer.reserve("failed");
        done.complete("observed"); failed.complete("unknown");
        var attempted = new ArrayList<List<java.util.Map.Entry<String, String>>>();
        require(!buffer.flushBatch(batch -> { attempted.add(batch); return false; })
            && releases.get() == 0 && buffer.size() == 3, "batch rejection released unacknowledged receipts");
        require(buffer.flushBatch(batch -> { attempted.add(batch); return true; })
            && releases.get() == 2 && buffer.size() == 1 && buffer.readyCount() == 0,
            "batch ACK released running work or retained completed receipts");
        require(attempted.size() == 2 && attempted.get(0).equals(attempted.get(1))
            && attempted.get(0).stream().noneMatch(entry -> entry.getKey().equals("running")),
            "shutdown changed results between retries or invented a running task outcome");
        require(buffer.flushBatch(batch -> { throw new AssertionError("empty batch performed I/O"); }),
            "no-ready-result batch failed");
        running.complete("late-exit");
        require(!buffer.flushBatch(batch -> { throw new IllegalStateException("fixture storage failure"); })
            && buffer.readyCount() == 1, "batch exception discarded late result");
        buffer.close(); require(releases.get() == 3, "shutdown reservation release count differs");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
