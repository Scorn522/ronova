package dev.ronova.coremod.policy;

/** Frozen 7.16 authority modes; stronger modes retain the weaker core. */
public enum AuthorityMode {
    OBSERVATIONAL,
    NON_BLOCKING,
    HARDENED;

    public boolean includes(AuthorityMode required) {
        return required == null || ordinal() >= required.ordinal();
    }

    public boolean isStricterThan(AuthorityMode other) {
        return other != null && ordinal() > other.ordinal();
    }
}
