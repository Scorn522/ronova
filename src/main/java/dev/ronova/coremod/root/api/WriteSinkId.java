package dev.ronova.coremod.root.api;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Stable identifier for an Authority-bearing final write boundary. */
public record WriteSinkId(String canonical) {
    private static final Pattern VALUE =
        Pattern.compile("[a-z0-9][a-z0-9_.:/-]{0,191}");

    public WriteSinkId {
        canonical = Objects.toString(canonical, "").trim().toLowerCase(Locale.ROOT);
        if (!VALUE.matcher(canonical).matches()) {
            throw new IllegalArgumentException("invalid write sink id: " + canonical);
        }
    }
}
