package dev.ronova.coremod.root.api;

import java.util.Objects;

/** Permission to remove one directly bound snapshot record; not a task or persistence receipt. */
public record RecoveryRecordPlan(RecoverySnapshotPlan source) {
    public static final String CONTRACT = "ronova:static-direct-snapshot-record-remove/v1";
    public static final String LOADED_CONTRACT = "ronova:loaded-direct-snapshot-record-remove/v1";
    public RecoveryRecordPlan {
        Objects.requireNonNull(source);
    }
}
