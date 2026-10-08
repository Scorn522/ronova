package dev.ronova.coremod.network;

import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.engine.JudgmentMark;
import java.util.*;

/** One bounded part of a connection-bound, versioned projection transaction. */
public record ProjectionFrame(UUID realmId, UUID streamId, long contextRevision, long revision, long baseRevision,
    DominionEpoch authorityEpoch, Mode mode, int partIndex, int partCount, List<JudgmentMark> marks, UUID identity) {
    public static final int MARKS_PER_PART=128, MAX_PARTS=512, MAX_MARKS=65536;
    public enum Mode { APPEND, REPLACE, REMOVE, CLEAR }
    public ProjectionFrame {
        Objects.requireNonNull(realmId);Objects.requireNonNull(streamId);Objects.requireNonNull(authorityEpoch);Objects.requireNonNull(mode);
        marks=List.copyOf(marks);
        if(contextRevision<1||revision<1||baseRevision<0||baseRevision>=revision||partCount<1||partCount>MAX_PARTS
            ||partIndex<0||partIndex>=partCount||marks.size()>MARKS_PER_PART)
            throw new IllegalArgumentException("invalid projection frame bounds/revision");
        if((mode==Mode.REMOVE||mode==Mode.CLEAR)&&(partCount!=1||!marks.isEmpty())
            ||mode==Mode.REMOVE&&identity==null||mode!=Mode.REMOVE&&identity!=null)throw new IllegalArgumentException("invalid projection operation");
        for(var mark:marks) {
            if(mark.uuid()==null||mark.type()==null||mark.type().length()>256||mark.dimension()==null||mark.dimension().length()>256
                ||mark.scope()==null||!Double.isFinite(mark.x())||!Double.isFinite(mark.y())||!Double.isFinite(mark.z())
                ||!Double.isFinite(mark.radius())||mark.radius()<0)throw new IllegalArgumentException("invalid projection mark");
        }
    }
    public boolean sameTransaction(ProjectionFrame other) {
        return realmId.equals(other.realmId)&&streamId.equals(other.streamId)&&contextRevision==other.contextRevision
            &&revision==other.revision&&baseRevision==other.baseRevision&&authorityEpoch.equals(other.authorityEpoch)
            &&mode==other.mode&&partCount==other.partCount&&Objects.equals(identity,other.identity);
    }
}
