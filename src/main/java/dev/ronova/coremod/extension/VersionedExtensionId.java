package dev.ronova.coremod.extension;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Stable loader-neutral identity for one future contract extension. */
public record VersionedExtensionId(String namespace, String name)
    implements Comparable<VersionedExtensionId> {
    private static final Pattern TOKEN = Pattern.compile("[a-z0-9][a-z0-9_.-]{0,127}");

    public VersionedExtensionId {
        namespace = normalize(namespace, "namespace");
        name = normalize(name, "name");
    }

    public String canonical() {
        return namespace + ":" + name;
    }

    public static VersionedExtensionId parse(String value) {
        String normalized = Objects.toString(value, "").trim().toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf(':');
        if (separator <= 0 || separator == normalized.length() - 1
            || normalized.indexOf(':', separator + 1) >= 0) {
            throw new IllegalArgumentException("extension id must be namespace:name");
        }
        return new VersionedExtensionId(
            normalized.substring(0, separator), normalized.substring(separator + 1)
        );
    }

    @Override
    public int compareTo(VersionedExtensionId other) {
        return other == null ? 1 : canonical().compareTo(other.canonical());
    }

    private static String normalize(String value, String label) {
        String normalized = Objects.toString(value, "").trim().toLowerCase(Locale.ROOT);
        if (!TOKEN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("invalid extension " + label + ": " + value);
        }
        return normalized;
    }
}
