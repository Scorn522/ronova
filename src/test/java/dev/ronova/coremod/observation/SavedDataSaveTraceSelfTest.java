package dev.ronova.coremod.observation;

public final class SavedDataSaveTraceSelfTest {
    public static void main(String[] args) {
        var trace=new SavedDataSaveTrace();
        var before=trace.snapshot();
        require(!trace.snapshot().observedWriteReturnAfter(before),"empty transcript fabricated a write");
        before=trace.snapshot();trace.begin();trace.end(true);
        require(!trace.snapshot().observedWriteReturnAfter(before),"clean early return fabricated a write");
        before=trace.snapshot();trace.begin();trace.serialized();trace.writing();trace.failed();trace.end(true);
        require(trace.snapshot().failed()&&!trace.snapshot().observedWriteReturnAfter(before),"swallowed IOException became success");
        before=trace.snapshot();trace.begin();trace.end(false);
        require(trace.snapshot().active()==0&&!trace.snapshot().observedWriteReturnAfter(before),"serializer exception left an accepted result");
        before=trace.snapshot();trace.begin();trace.serialized();trace.writing();trace.wrote();trace.end(true);
        require(trace.snapshot().observedWriteReturnAfter(before),"real write return was lost");
        var prior=trace.snapshot();var priorBefore=before;
        require(!prior.observedWriteReturnAfter(prior),"an unobserved overriding save reused old success");
        var foreign=new SavedDataSaveTrace();
        for(long i=0;i<priorBefore.generation();i++){foreign.begin();foreign.end(true);}
        require(foreign.snapshot().generation()==priorBefore.generation(),"foreign generation control mismatched");
        require(!prior.observedWriteReturnAfter(foreign.snapshot()),"different holder traces were mixed");
        var multiple=new SavedDataSaveTrace();var multipleBefore=multiple.snapshot();
        for(int i=0;i<2;i++){multiple.begin();multiple.serialized();multiple.writing();multiple.wrote();multiple.end(true);}
        require(!multiple.snapshot().observedWriteReturnAfter(multipleBefore),"multiple intervening saves became one known call");
        before=trace.snapshot();trace.begin();trace.serialized();trace.begin();trace.serialized();trace.writing();trace.wrote();trace.end(true);trace.end(true);
        require(trace.snapshot().ambiguous()&&!trace.snapshot().observedWriteReturnAfter(before),"overlapping saves merged into one accepted write");
        require(prior.observedWriteReturnAfter(priorBefore),"new attempts mutated a prior immutable transcript");
        before=trace.snapshot();trace.begin();trace.serialized();trace.writing();trace.wrote();trace.end(true);
        require(trace.snapshot().observedWriteReturnAfter(before),"later independent attempt could not be observed");
        before=trace.snapshot();trace.end(true);trace.begin();trace.serialized();trace.writing();trace.wrote();trace.end(true);
        require(!trace.snapshot().observedWriteReturnAfter(before),"unmatched boundary silently recovered");
        System.out.println("SavedDataSaveTraceSelfTest PASS (advisory write return only; no durable receipt)");
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
