package dev.ronova.coremod.persistence;

/**
 * One-shot failure injection points for the 7.16 authoritative Java store.
 * They model the two ambiguity windows that command-id recovery must close.
 */
public enum RootCommitFailpoint {
    NONE,
    BEFORE_APPLY,
    AFTER_APPLY_BEFORE_ACK
}
