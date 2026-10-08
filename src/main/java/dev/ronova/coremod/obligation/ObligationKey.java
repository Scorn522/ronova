package dev.ronova.coremod.obligation;

import dev.ronova.coremod.root.api.TargetEpoch;
import java.util.Objects;

/** Unknown producers coalesce into their known result outlet, never an arbitrary reported class or task name. */
public record ObligationKey(TargetEpoch targetEpoch, Source source, Edge edge, Result result) {
    // SAVED_RECORD is the retained serialized-snapshot source category, including static holders.
    // It does not assert that the holder is SavedData or that any file has been written.
    public enum Source { ADMISSION_GATE, TERMINAL_PLAN, ROOT_EFFECT_JOURNAL, SAVED_RECORD }
    public enum Edge { ADMISSION, EXECUTION, OPAQUE_EFFECT, RECONSTRUCTION }
    public enum Result { CURRENT_QUALIFICATION, TERMINAL_SERVER_RESULT, EFFECT_RESOLUTION, RECOVERY_SOURCE_DISPOSAL }
    public ObligationKey { Objects.requireNonNull(targetEpoch); Objects.requireNonNull(source); Objects.requireNonNull(edge); Objects.requireNonNull(result); }
}
