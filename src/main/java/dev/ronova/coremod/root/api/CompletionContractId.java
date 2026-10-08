package dev.ronova.coremod.root.api;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Stable identity for a non-CLEARED operation's finality contract. */
public record CompletionContractId(String canonical) {
    private static final Pattern VALUE =
        Pattern.compile("[a-z0-9][a-z0-9_.:/-]{0,191}");

    public CompletionContractId {
        canonical = Objects.toString(canonical, "").trim().toLowerCase(Locale.ROOT);
        if (!VALUE.matcher(canonical).matches()) {
            throw new IllegalArgumentException("invalid completion contract id: " + canonical);
        }
    }
}
