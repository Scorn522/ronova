package dev.ronova.coremod.authority;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/** Resolves the 7.14B Authority split mode once for each server runtime. */
public final class AuthoritySplitFlags {
    public static final String PROPERTY = "ronova.714b.authoritySplit.enabled";
    public static final String ENVIRONMENT = "RONOVA_714B_AUTHORITY_SPLIT_ENABLED";

    private AuthoritySplitFlags() {}

    public enum Source {
        DEFAULT,
        JVM_PROPERTY,
        ENVIRONMENT,
        INVALID_JVM_PROPERTY,
        INVALID_ENVIRONMENT
    }

    public record Selection(boolean enabled, Source source, String warning) {
        public Selection {
            Objects.requireNonNull(source, "source");
        }

        public boolean hasWarning() {
            return warning != null && !warning.isBlank();
        }
    }

    public static Selection resolve() {
        return resolve(System::getProperty, System::getenv);
    }

    static Selection resolve(
        Function<String, String> propertyReader,
        Function<String, String> environmentReader
    ) {
        Objects.requireNonNull(propertyReader, "propertyReader");
        Objects.requireNonNull(environmentReader, "environmentReader");

        String property = propertyReader.apply(PROPERTY);
        String environment = environmentReader.apply(ENVIRONMENT);
        if (property != null) {
            Boolean selected = parse(property);
            if (selected == null) {
                return new Selection(
                    false,
                    Source.INVALID_JVM_PROPERTY,
                    "Invalid " + PROPERTY + " value; 7.14B Authority split uses LEGACY rollback mode."
                );
            }
            Boolean environmentValue = parse(environment);
            String warning = environmentValue != null && environmentValue != selected
                ? PROPERTY + " overrides conflicting " + ENVIRONMENT + "."
                : null;
            return new Selection(selected, Source.JVM_PROPERTY, warning);
        }

        if (environment != null) {
            Boolean selected = parse(environment);
            if (selected == null) {
                return new Selection(
                    false,
                    Source.INVALID_ENVIRONMENT,
                    "Invalid " + ENVIRONMENT + " value; 7.14B Authority split uses LEGACY rollback mode."
                );
            }
            return new Selection(selected, Source.ENVIRONMENT, null);
        }
        return new Selection(true, Source.DEFAULT, null);
    }

    private static Boolean parse(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true" -> Boolean.TRUE;
            case "false" -> Boolean.FALSE;
            default -> null;
        };
    }
}
