package dev.ronova.coremod.root.api;

import dev.ronova.coremod.obligation.ObligationCheckpoint;
import java.util.Objects;

public record CheckpointReceipt(Status status, ObligationCheckpoint acknowledged) {
    public enum Status { COMMITTED, UNCHANGED, UNKNOWN, CONFLICT, REJECTED }
    public CheckpointReceipt {Objects.requireNonNull(status);Objects.requireNonNull(acknowledged);}
    public boolean acknowledgedWrite() {return status==Status.COMMITTED||status==Status.UNCHANGED;}
}
