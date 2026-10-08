package dev.ronova.coremod.observation;

/** Bounded advisory transcript of the vanilla save boundary. Never a durable commit receipt. */
public final class SavedDataSaveTrace {
    public record EncodedWrite(long generation,String hash,long bytes,NbtWriteReferences.Snapshot references,String fileIdentity) {
        public EncodedWrite(long generation,String hash,long bytes,NbtWriteReferences.Snapshot references) {
            this(generation,hash,bytes,references,null);
        }
    }
    private EncodedWrite encodedWrite;
    synchronized void invalidateEncoded(long expectedGeneration) {
        if(generation==expectedGeneration)encodedWrite=null;
    }
    synchronized EncodedWrite encoded(long expectedGeneration,String hash,long bytes,NbtWriteReferences.Snapshot references) {
        return encoded(expectedGeneration,hash,bytes,references,null);
    }
    synchronized EncodedWrite encoded(long expectedGeneration,String hash,long bytes,NbtWriteReferences.Snapshot references,String fileIdentity) {
        if(generation==expectedGeneration&&active==1&&!failed&&!ambiguous&&encodedWrite==null)
            return encodedWrite=new EncodedWrite(generation,hash,bytes,references,fileIdentity);
        return null;
    }
    public synchronized EncodedWrite encodedWrite() {
        return active==0&&returned&&!failed&&!ambiguous&&writeReturns==1?encodedWrite:null;
    }
    public record Snapshot(java.util.UUID traceId, long generation, int active, int serialized, int writeAttempts, int writeReturns,
                           boolean failed, boolean ambiguous, boolean returned, String observedPath) {
        /** One new observed call on this same trace; a missing/overridden boundary cannot reuse old success. */
        public boolean observedWriteReturnAfter(Snapshot before) {
            return before != null && traceId.equals(before.traceId) && before.active == 0
                && before.generation >= 0 && before.generation < Long.MAX_VALUE && generation == before.generation + 1
                && active == 0 && serialized == 1 && writeAttempts == 1
                && writeReturns == 1 && !failed && !ambiguous && returned;
        }
    }
    private final java.util.UUID traceId = java.util.UUID.randomUUID();
    private long generation,encodingTurn;
    private String observedPath;
    private int active, serialized, writeAttempts, writeReturns;
    private boolean failed, ambiguous, returned, poisoned;
    synchronized int nextEncodingStart(int size) {
        if(size<1)throw new IllegalArgumentException("empty source group");
        int start=(int)(encodingTurn%size);encodingTurn=encodingTurn==Long.MAX_VALUE?0:encodingTurn+1;return start;
    }
    public synchronized void begin() {
        if (active == 0) {
            if (generation == Long.MAX_VALUE) poisoned = true; else generation++;
            serialized = writeAttempts = writeReturns = 0;
            failed = returned = false; ambiguous = poisoned;observedPath=null;encodedWrite=null;
        } else ambiguous = true;
        if (active == Integer.MAX_VALUE) poisoned = ambiguous = true; else active++;
    }
    public synchronized void serialized() {
        if (active != 1 || serialized != 0) ambiguous = true;
        if (serialized < 2) serialized++;
    }
    public void writing() { writing(null); }
    public synchronized void writing(String path) {
        observedPath=path!=null&&path.length()<=4096?path:null;
        if (active != 1 || serialized != 1 || writeAttempts != 0) ambiguous = true;
        if (writeAttempts < 2) writeAttempts++;
    }
    public synchronized void wrote() {
        if (active != 1 || writeAttempts != 1 || writeReturns != 0) ambiguous = true;
        if (writeReturns < 2) writeReturns++;
    }
    /** IOException may be swallowed by vanilla; retain failure even when the outer method returns. */
    public synchronized void failed() { failed = true; }
    public synchronized void end(boolean normally) {
        if (active <= 0) { poisoned = ambiguous = true; return; }
        if (!normally) failed = true;
        active--;
        if (active == 0) returned = normally;
    }
    public synchronized Snapshot snapshot() {
        return new Snapshot(traceId, generation, active, serialized, writeAttempts, writeReturns, failed, ambiguous, returned, observedPath);
    }
}
