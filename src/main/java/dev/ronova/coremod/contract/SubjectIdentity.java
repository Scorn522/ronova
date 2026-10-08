package dev.ronova.coremod.contract;

import java.util.Objects;
import java.util.UUID;

/**
 * Loader-neutral identity observed by a Minecraft adapter. It deliberately
 * contains no Entity, Level or Forge type so the same value can cross a Native
 * or External Root boundary later.
 */
public record SubjectIdentity(
    UUID uuid,
    String registryType,
    String runtimeClass,
    String dimension,
    double x,
    double y,
    double z,
    boolean player
) {
    public SubjectIdentity {
        registryType = normalize(registryType, "unknown:unknown");
        runtimeClass = normalize(runtimeClass, "unknown");
        dimension = normalize(dimension, "unknown:unknown");
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
    }

    public boolean identified() {
        return uuid != null;
    }

    /** Stable textual key for logs, snapshots and future IPC payloads. */
    public String canonicalKey() {
        return (uuid == null ? "unassigned" : uuid.toString()) + '|'
            + registryType + '|' + runtimeClass + '|' + dimension;
    }

    private static String normalize(String value, String fallback) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
