package dev.ronova.coremod.root.api;

import java.util.Objects;
import java.util.UUID;

/** One physical source incarnation's mutation intent. It is never a persistence-completion receipt. */
public record RecoveryMutationIntent(RecoverySnapshotPlan source, UUID incarnation, Kind kind) {
    public enum Kind {
        STATIC_RECORD(0), STATIC_SNAPSHOT(1), LOADED_SNAPSHOT(2), LOADED_RECORD(3);
        final int wireId;
        Kind(int wireId) { this.wireId = wireId; }
        public boolean loaded() { return this == LOADED_SNAPSHOT || this == LOADED_RECORD; }
        public boolean record() { return this == STATIC_RECORD || this == LOADED_RECORD; }
    }
    public enum Observation {
        WRITE_OBSERVED(1), WRITE_UNVERIFIED(2), NOT_CURRENT(4), NOT_EXECUTED(8);
        final int bit;
        Observation(int bit) { this.bit = bit; }
    }
    public static final String OPERATION = "ronova:recovery-memory-mutation/v1";
    public record Receipt(RecoveryMutationIntent intent, Observation observation) {
        public Receipt { Objects.requireNonNull(intent); Objects.requireNonNull(observation); }
    }
    public RecoveryMutationIntent {
        Objects.requireNonNull(source); Objects.requireNonNull(incarnation); Objects.requireNonNull(kind);
        boolean loaded = source.storageScope() == RecoverySnapshotPlan.StorageScope.LOADED_SAVED_DATA;
        if (loaded != kind.loaded()) throw new IllegalArgumentException("mutation storage scope mismatch");
    }
    public EffectKey effectKey() {
        var terminal = source.terminal();
        return new EffectKey(terminal.commandId(), terminal.qualification().targetEpoch(), OPERATION,
            "source/" + source.sourceLocator() + "/" + incarnation.toString().replace("-", "")
                + "/" + kind.wireId + "/" + source.copyDepth());
    }
    public EffectRecord prepared() {
        var terminal = source.terminal(); var key = effectKey();
        return new EffectRecord(key, terminal.authorityHash(), terminal.authorityCommitIndex(),
            payloadHash(key, terminal.authorityHash()), terminal.activePolicyRevision(), terminal.planId(),
            terminal.planRevision(), "recovery-memory-mutation", UUID.randomUUID(),
            EffectRecord.SideEffectClass.EXTERNAL_OPAQUE, EffectRecord.RecoveryCapability.QUERYABLE,
            EffectRecord.State.PREPARED, 1, "memory-mutation-intent;task-and-save-unproven");
    }
    public static int depth(EffectKey key) {
        if (!OPERATION.equals(key.semanticOperationId())
            || !key.semanticSlotId().matches("source/[0-9a-f]{64}/[0-9a-f]{32}/[0-3]/(0|[1-9][0-9]{0,9})"))
            throw new IllegalArgumentException("unregistered recovery mutation slot");
        return Integer.parseInt(key.semanticSlotId().substring(key.semanticSlotId().lastIndexOf('/') + 1));
    }
    public static String payloadHash(EffectKey key, String authorityHash) {
        depth(key);
        return EffectRecord.hash(OPERATION + "\nexact-registered-memory-cell-only\n" + key.canonical()
            + "\n" + EffectRecord.checkedHash(authorityHash));
    }
    /** Accumulate observed outcomes; a later rejected retry must not erase an earlier write. */
    public static String observations(EffectRecord prior, Observation observation) {
        String prefix = "memory-observations/", suffix = ";task-and-save-unproven";
        int mask = observationMask(prior);
        return prefix + (mask | observation.bit) + suffix;
    }
    /** Validate persisted evidence before interpreting it; arbitrary text is not an empty observation set. */
    public static int observationMask(EffectRecord record) {
        if (!OPERATION.equals(record.key().semanticOperationId()))
            throw new IllegalArgumentException("not a recovery mutation record");
        String evidence = record.evidenceRef();
        if (record.state() == EffectRecord.State.PREPARED
            && evidence.equals("memory-mutation-intent;task-and-save-unproven")) return 0;
        if (record.state() == EffectRecord.State.EFFECT_UNKNOWN) {
            if (evidence.equals("restart-prepared-effect-unknown") || evidence.equals("prepared-without-live-attempt")) return 0;
            if (evidence.matches("memory-observations/([1-9]|1[0-5]);task-and-save-unproven"))
                return Integer.parseInt(evidence.substring("memory-observations/".length(), evidence.indexOf(';')));
        }
        throw new IllegalArgumentException("invalid memory mutation evidence/state");
    }
    /** An uncertain write is not cleared by a later observed bit. This covers memory only. */
    public boolean acknowledgedWrite(EffectRecord record) {
        if(record==null||!effectKey().equals(record.key())
            ||!source.terminal().authorityHash().equals(record.authorityPayloadHash())
            ||!payloadHash(effectKey(),source.terminal().authorityHash()).equals(record.effectPayloadHash()))return false;
        int mask=observationMask(record);
        return (mask&Observation.WRITE_OBSERVED.bit)!=0&&(mask&Observation.WRITE_UNVERIFIED.bit)==0;
    }
}
