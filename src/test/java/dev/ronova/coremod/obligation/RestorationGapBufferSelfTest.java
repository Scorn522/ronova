package dev.ronova.coremod.obligation;

import dev.ronova.coremod.root.api.CheckpointReceipt;
import java.util.List;

public final class RestorationGapBufferSelfTest {
    public static void main(String[] args) {
        var buffer=new RestorationGapBuffer();
        require(buffer.prepare(ObligationCheckpoint.EMPTY,0)==null,"empty buffer fabricated a checkpoint");
        buffer.missed();buffer.missed();
        var first=buffer.prepare(ObligationCheckpoint.EMPTY,4);
        buffer.missed();
        require(buffer.prepare(ObligationCheckpoint.EMPTY,100)==first&&buffer.expectedRevision()==0,
            "unknown retry changed its payload or base revision");
        require(!buffer.acknowledge(new CheckpointReceipt(CheckpointReceipt.Status.UNKNOWN,ObligationCheckpoint.EMPTY))
            &&buffer.count()==3,"unknown result consumed observations");
        var unrelated=new ObligationCheckpoint(1,5,first.entries(),first.coverage(),first.restrictions(),first.overflow());
        require(!buffer.acknowledge(new CheckpointReceipt(CheckpointReceipt.Status.COMMITTED,unrelated)),
            "an unrelated receipt consumed a pending batch");
        var rootReasons=new CandidateRestrictions(1,0,CandidateRestrictions.EMPTY.retiredDigest(),List.of());
        var acknowledged=new ObligationCheckpoint(1,4,first.entries(),first.coverage(),rootReasons,first.overflow());
        require(buffer.acknowledge(new CheckpointReceipt(CheckpointReceipt.Status.UNCHANGED,acknowledged))
            &&buffer.count()==1,"Root-derived restrictions or later observations broke ACK accounting");
        var second=buffer.prepare(acknowledged,10);
        require(second.coverage().overflowCount()==3&&second.overflow().unscopedCount()==3&&buffer.expectedRevision()==1,
            "retry counted the first batch twice or lost the second batch");
        require(!buffer.acknowledge(new CheckpointReceipt(CheckpointReceipt.Status.CONFLICT,acknowledged))
            &&buffer.prepare(acknowledged,20)==second,"conflict silently rebased an ambiguous increment");
        require(buffer.acknowledge(new CheckpointReceipt(CheckpointReceipt.Status.COMMITTED,second))&&!buffer.pending(),
            "completed gap batch remained pending");
        buffer.missed();buffer.clear();require(!buffer.pending(),"shutdown retained the buffer");
        System.out.println("RestorationGapBufferSelfTest PASS");
    }
    private static void require(boolean value,String message) { if(!value)throw new AssertionError(message); }
}
