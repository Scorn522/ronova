package dev.ronova.coremod.engine;

import java.util.UUID;

/** Minimal identity and spatial lineage contract shared by durable and client state. */
public interface LineageRecord {
    UUID uuid();

    String type();

    String dimension();

    double x();

    double y();

    double z();

    double radius();

    JudgmentScope scope();
}
