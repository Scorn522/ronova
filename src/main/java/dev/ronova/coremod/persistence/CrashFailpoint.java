package dev.ronova.coremod.persistence;

/** One-shot shadow-store crash-injection points used by local recovery tests. */
public enum CrashFailpoint {
    NONE,
    BEFORE_APPLY,
    AFTER_APPLY_BEFORE_DIRTY,
    AFTER_DIRTY_BEFORE_ACK
}
