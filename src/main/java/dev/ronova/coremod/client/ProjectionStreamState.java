package dev.ronova.coremod.client;

import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.engine.JudgmentMark;
import dev.ronova.coremod.network.ProjectionFrame;
import java.util.*;

/** Atomic client mirror. This class can never write server Truth or mint an execution qualification. */
public final class ProjectionStreamState {
    public enum Result { COMMITTED, STAGED, STALE, WRONG_CONTEXT, RESYNC_REQUIRED }
    private UUID realm,stream;
    private long contextRevision,revision,ownerRevision=-1;
    private Boolean ownerState;
    private DominionEpoch epoch=DominionEpoch.ZERO;
    private Map<UUID,JudgmentMark> active=Map.of();
    private ProjectionFrame pending;
    private List<JudgmentMark>[] parts;
    private int received,totalMarks;
    private long pendingSince;
    private boolean resync;
    public static final long DEFAULT_BYTE_BUDGET = 64L * 1024 * 1024;
    private final long byteBudget;
    private long activeBytes, pendingBytes, peakBytes, budgetDenials;
    public ProjectionStreamState() { this(DEFAULT_BYTE_BUDGET); }
    public ProjectionStreamState(long byteBudget) {
        if (byteBudget < 1024 || byteBudget > 256L * 1024 * 1024) throw new IllegalArgumentException("invalid projection byte budget");
        this.byteBudget = byteBudget;
    }
    /** Conservative owned estimate, including strings, indexes and staging; not whole-JVM heap usage. */
    public record MemoryUsage(long retainedBytes, long peakBytes, long budgetBytes, long denials) {}
    public synchronized MemoryUsage memoryUsage() { return new MemoryUsage(activeBytes + pendingBytes, peakBytes, byteBudget, budgetDenials); }
    private static long markBytes(JudgmentMark mark) { return 384L + 2L * (mark.type().length() + mark.dimension().length()); }
    private boolean fits(long bytes) {
        if (bytes > byteBudget) { if (budgetDenials != Long.MAX_VALUE) budgetDenials++; return false; }
        peakBytes = Math.max(peakBytes, bytes); return true;
    }
    public synchronized boolean bind(UUID realmId,UUID streamId,long generation) {
        Objects.requireNonNull(realmId);Objects.requireNonNull(streamId);
        if(generation<1||generation<contextRevision)return false;
        if(generation==contextRevision)return realmId.equals(realm)&&streamId.equals(stream);
        reset();realm=realmId;stream=streamId;contextRevision=generation;return true;
    }
    public synchronized boolean acceptOwner(long ownerRevision,boolean state) {
        if(realm==null||ownerRevision<0||ownerRevision<this.ownerRevision)return false;
        if(ownerRevision==this.ownerRevision)return Objects.equals(ownerState,state);
        this.ownerRevision=ownerRevision;ownerState=state;return true;
    }
    @SuppressWarnings("unchecked")
    public synchronized Result accept(ProjectionFrame frame,long nowMillis) {
        Objects.requireNonNull(frame);
        if(!frame.realmId().equals(realm)||!frame.streamId().equals(stream)||frame.contextRevision()!=contextRevision)return Result.WRONG_CONTEXT;
        if(frame.revision()<=revision||frame.authorityEpoch().compareTo(epoch)<0)return Result.STALE;
        if(pending!=null&&frame.revision()<pending.revision())return Result.STALE;
        if(frame.mode()!=ProjectionFrame.Mode.REPLACE&&(revision==0||frame.baseRevision()!=revision||frame.revision()!=revision+1))return fail();
        if(pending==null||frame.revision()>pending.revision()) {
            clearPending();
            long headers = 256L + 16L * frame.partCount();
            if (!fits(activeBytes + headers)) return fail();
            pendingBytes = headers;
            pending=frame;parts=(List<JudgmentMark>[])new List<?>[frame.partCount()];received=0;totalMarks=0;pendingSince=nowMillis;
        }else if(!pending.sameTransaction(frame))return fail();
        var prior=parts[frame.partIndex()];
        if(prior!=null)return prior.equals(frame.marks())?Result.STAGED:fail();
        long incomingBytes = 32;
        for (var mark : frame.marks()) incomingBytes += markBytes(mark);
        if (!fits(activeBytes + pendingBytes + incomingBytes)) return fail();
        pendingBytes += incomingBytes;
        parts[frame.partIndex()]=frame.marks();received++;totalMarks+=frame.marks().size();pendingSince=nowMillis;
        if(totalMarks>ProjectionFrame.MAX_MARKS)return fail();
        if(received<parts.length)return Result.STAGED;
        // Account for both the old committed image and temporary indexes before allocating them.
        boolean emptyBase = frame.mode()==ProjectionFrame.Mode.REPLACE || frame.mode()==ProjectionFrame.Mode.CLEAR;
        if (!fits(activeBytes + pendingBytes + 128L * ((emptyBase ? 0 : active.size()) + totalMarks))) return fail();
        Map<UUID,JudgmentMark> next=emptyBase?new LinkedHashMap<>():new LinkedHashMap<>(active);
        Set<UUID> seen=new HashSet<>();
        for(var part:parts)for(var mark:part){if(!seen.add(mark.uuid()))return fail();next.put(mark.uuid(),mark);}
        if(frame.mode()==ProjectionFrame.Mode.REMOVE)next.remove(frame.identity());
        if(frame.mode()==ProjectionFrame.Mode.CLEAR)next.clear();
        if(next.size()>ProjectionFrame.MAX_MARKS)return fail();
        long nextBytes = 0;
        for (var mark : next.values()) nextBytes += markBytes(mark);
        if (!fits(nextBytes)) return fail();
        active=Collections.unmodifiableMap(next);activeBytes=nextBytes;revision=frame.revision();epoch=frame.authorityEpoch();clearPending();resync=false;
        return Result.COMMITTED;
    }
    private Result fail(){clearPending();resync=true;return Result.RESYNC_REQUIRED;}
    private void clearPending(){pending=null;parts=null;received=0;totalMarks=0;pendingBytes=0;}
    public synchronized boolean needsResync(long nowMillis){if(pending!=null&&(nowMillis<pendingSince||nowMillis-pendingSince>2000))fail();return resync;}
    public synchronized int size(){return active.size();}
    public synchronized List<JudgmentMark> snapshot(){return List.copyOf(active.values());}
    public synchronized UUID realmId(){return realm;}
    public synchronized UUID streamId(){return stream;}
    public synchronized long contextRevision(){return contextRevision;}
    public synchronized long revision(){return revision;}
    public synchronized DominionEpoch authorityEpoch(){return epoch;}
    public synchronized boolean contains(UUID identity){return active.containsKey(identity);}
    public synchronized Boolean ownerState(){return ownerState;}
    public synchronized void reset(){realm=null;stream=null;contextRevision=0;revision=0;ownerRevision=-1;ownerState=null;epoch=DominionEpoch.ZERO;active=Map.of();activeBytes=0;clearPending();resync=false;}
}
