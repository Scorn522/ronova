package dev.ronova.coremod.network;

import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.engine.JudgmentMark;
import java.util.*;

/** One owned outgoing transaction. Parts are constructed only when transport budget is available. */
public final class ProjectionTransmission {
    public static final long MAX_RETAINED_BYTES = 64L * 1024 * 1024;
    private final UUID realm, stream, identity;
    private final long context, revision, base;
    private final DominionEpoch epoch;
    private final ProjectionFrame.Mode mode;
    private final List<JudgmentMark> marks;
    private final int parts;
    private final long retainedBytes;
    private int cursor;
    public ProjectionTransmission(UUID realm, UUID stream, long context, long revision, long base,
        DominionEpoch epoch, ProjectionFrame.Mode mode, List<JudgmentMark> marks, UUID identity) {
        long bytes = estimateRetainedBytes(marks);
        if (bytes > MAX_RETAINED_BYTES) throw new IllegalArgumentException("outgoing projection byte budget");
        this.realm = Objects.requireNonNull(realm); this.stream = Objects.requireNonNull(stream);
        this.context = context; this.revision = revision; this.base = base; this.epoch = Objects.requireNonNull(epoch);
        this.mode = Objects.requireNonNull(mode); this.identity = identity; this.marks = List.copyOf(marks);
        this.parts = Math.max(1, (marks.size() + ProjectionFrame.MARKS_PER_PART - 1) / ProjectionFrame.MARKS_PER_PART);
        this.retainedBytes = bytes;
        // Validate metadata before publishing any part; each remaining mark is bounded by the same wire contract.
        for (int part = 0; part < parts; part++) frame(part);
    }
    public static long estimateRetainedBytes(List<JudgmentMark> marks) {
        if (marks.size() > ProjectionFrame.MAX_MARKS) throw new IllegalArgumentException("outgoing projection capacity");
        long bytes = 256;
        for (var mark : marks) bytes += 400L + 2L * (mark.type().length() + mark.dimension().length());
        return bytes;
    }
    public long retainedBytes() { return retainedBytes; }
    public long revision() { return revision; }
    public boolean complete() { return cursor == parts; }
    /** UTF-8 upper bound plus primitive fields and packet framing; not a measured Netty allocation. */
    public long nextWireBytes() {
        if (complete()) return 0;
        long bytes = 160;
        for (int i = cursor * ProjectionFrame.MARKS_PER_PART; i < Math.min(marks.size(), (cursor + 1) * ProjectionFrame.MARKS_PER_PART); i++) {
            var mark = marks.get(i); bytes += 96L + 3L * (mark.type().length() + mark.dimension().length());
        }
        return bytes;
    }
    public ProjectionFrame next() {
        if (complete()) throw new IllegalStateException("projection transmission already complete");
        return frame(cursor);
    }
    /** Advance only after the transport accepted the part. */
    public void sent() {
        if (complete()) throw new IllegalStateException("projection transmission already complete");
        cursor++;
    }
    private ProjectionFrame frame(int part) {
        int start = part * ProjectionFrame.MARKS_PER_PART, end = Math.min(marks.size(), start + ProjectionFrame.MARKS_PER_PART);
        return new ProjectionFrame(realm, stream, context, revision, base, epoch, mode, part, parts, marks.subList(start, end), identity);
    }
}
