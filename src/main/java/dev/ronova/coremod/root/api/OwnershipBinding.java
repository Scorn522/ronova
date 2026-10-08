package dev.ronova.coremod.root.api;

import dev.ronova.coremod.contract.SubjectKey;

/** Null owner is a retained revocation record, never an implicit default owner. */
public record OwnershipBinding(long revision, SubjectKey owner) {
    public OwnershipBinding {
        if (revision <= 0L) throw new IllegalArgumentException("ownership revision must be positive");
    }
}
