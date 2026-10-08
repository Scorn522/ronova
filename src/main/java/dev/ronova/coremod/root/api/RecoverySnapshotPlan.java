package dev.ronova.coremod.root.api;

import java.util.Objects;

/** Permission for one observed atomic snapshot, not a durable disposition receipt. */
public record RecoverySnapshotPlan(TerminalOperationPlan terminal, String sourceLocator,
                                   long sourceVersion, long witnessRevision, int copyDepth, StorageScope storageScope) {
    public enum StorageScope { STATIC_MANAGER, LOADED_SAVED_DATA }
    public static final String CONTRACT = "ronova:static-atomic-snapshot-clear/v1";
    public static final String LOADED_CONTRACT = "ronova:loaded-atomic-snapshot-clear/v1";
    public RecoverySnapshotPlan(TerminalOperationPlan terminal, String sourceLocator,
        long sourceVersion, long witnessRevision, int copyDepth) {
        this(terminal, sourceLocator, sourceVersion, witnessRevision, copyDepth, StorageScope.STATIC_MANAGER);
    }
    public RecoverySnapshotPlan {
        Objects.requireNonNull(terminal);
        Objects.requireNonNull(storageScope);
        sourceLocator = EffectRecord.checkedHash(sourceLocator);
        if (sourceVersion < 1 || witnessRevision < sourceVersion || copyDepth < 0)
            throw new IllegalArgumentException("invalid snapshot source/witness version");
    }
}
