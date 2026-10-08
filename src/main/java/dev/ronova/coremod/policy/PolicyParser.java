package dev.ronova.coremod.policy;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Strict parser for the small 7.16 policy status/configuration vocabulary. */
public final class PolicyParser {
    private PolicyParser() {}

    public static PolicyState parse(Map<String, String> values) {
        Map<String, String> input = values == null ? Map.of() : values;
        StrengthProfile profile = parseEnum(
            input.get("profile"), StrengthProfile.class, StrengthProfile.COMPATIBLE
        );
        PolicyRevision revision = parseRevision(input.get("revision"));
        EnumMap<PlaneId, MechanismLevel> planes = new EnumMap<>(PlaneId.class);
        for (PlaneId plane : PlaneId.values()) {
            String raw = input.get(plane.name().toLowerCase(Locale.ROOT));
            if (raw != null && !raw.isBlank()) {
                planes.put(plane, parseEnum(raw, MechanismLevel.class, null));
            }
        }
        return new PolicyState(revision, profile, planes, java.util.List.of());
    }

    public static PolicyState parse(String value) {
        if (value == null || value.isBlank()) {
            return PolicyState.defaults();
        }
        java.util.LinkedHashMap<String, String> values = new java.util.LinkedHashMap<>();
        for (String part : value.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2 || pair[0].isBlank()) {
                throw new IllegalArgumentException("invalid policy entry: " + part);
            }
            values.put(pair[0].trim().toLowerCase(Locale.ROOT), pair[1].trim());
        }
        return parse(values);
    }

    public static StrengthProfile parseProfile(String value) {
        return parseEnum(value, StrengthProfile.class, null);
    }

    public static MechanismLevel parseMechanismLevel(String value) {
        return parseEnum(value, MechanismLevel.class, null);
    }

    private static PolicyRevision parseRevision(String value) {
        if (value == null || value.isBlank()) {
            return PolicyRevision.INITIAL;
        }
        try {
            return new PolicyRevision(Long.parseLong(value.trim()));
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("invalid policy revision: " + value, failure);
        }
    }

    private static <T extends Enum<T>> T parseEnum(
        String value, Class<T> type, T fallback
    ) {
        if (value == null || value.isBlank()) {
            if (fallback != null) {
                return fallback;
            }
            throw new IllegalArgumentException("missing " + type.getSimpleName());
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf(type, normalized);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(
                "invalid " + type.getSimpleName() + ": " + value, failure
            );
        }
    }
}
