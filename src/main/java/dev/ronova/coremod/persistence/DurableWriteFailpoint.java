package dev.ronova.coremod.persistence;

/** Package-local one-shot fault injection; never selected by gameplay/configuration. */
enum DurableWriteFailpoint {
    NONE,
    BEFORE_WRITE,
    AFTER_REPLACE_BEFORE_ACK
}
