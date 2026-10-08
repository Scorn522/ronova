package dev.ronova.coremod.obligation;

import dev.ronova.coremod.root.api.CheckpointReceipt;
import java.util.Objects;

/** One immutable metadata retry while live obligation detail cannot be allocated. */
public final class RestorationGapBuffer {
    private long waiting, captured, expectedRevision;
    private ObligationCheckpoint attempt;

    public synchronized void missed() { waiting=add(waiting,1); }
    public synchronized boolean pending() { return waiting>0||attempt!=null; }
    public synchronized long count() { return add(waiting,captured); }
    public synchronized long expectedRevision() {
        if(attempt==null)throw new IllegalStateException("no staged gap checkpoint");
        return expectedRevision;
    }
    public synchronized ObligationCheckpoint prepare(ObligationCheckpoint acknowledged,long clock) {
        Objects.requireNonNull(acknowledged);
        if(attempt!=null)return attempt;
        if(waiting==0)return null;
        var coverage=acknowledged.coverage();var overflow=acknowledged.overflow();
        var next=new ObligationCheckpoint(Math.addExact(acknowledged.revision(),1),Math.max(clock,acknowledged.clock()),
            acknowledged.entries(),new ObligationRegistry.Coverage(add(coverage.overflowCount(),waiting),true,coverage.authorityAnchor()),
            acknowledged.restrictions(),new OverflowCheckpoint(overflow.retryEntries(),overflow.scopes(),add(overflow.unscopedCount(),waiting)));
        // New observations go into another scalar while this exact attempt awaits ACK.
        expectedRevision=acknowledged.revision();captured=waiting;waiting=0;attempt=next;
        return next;
    }
    public synchronized boolean acknowledge(CheckpointReceipt receipt) {
        Objects.requireNonNull(receipt);
        if(attempt==null||!receipt.acknowledgedWrite()||receipt.acknowledged().revision()!=attempt.revision()
            ||!attempt.sameObservationContent(receipt.acknowledged()))return false;
        attempt=null;captured=0;return true;
    }
    public synchronized void clear() { waiting=0;captured=0;attempt=null;expectedRevision=0; }
    private static long add(long left,long right) { return left>Long.MAX_VALUE-right?Long.MAX_VALUE:left+right; }
}
