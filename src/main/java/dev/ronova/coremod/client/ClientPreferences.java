package dev.ronova.coremod.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record ClientPreferences(VisualMode visualMode) {
    public static ClientPreferences load(Path path) {
        Properties properties = new Properties();
        if (Files.isRegularFile(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException ignored) {
                return defaults();
            }
        }
        try {
            return new ClientPreferences(VisualMode.valueOf(
                properties.getProperty("visualMode", VisualMode.ALL.name())));
        } catch (IllegalArgumentException ignored) {
            return defaults();
        }
    }

    public void save(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Properties properties = new Properties();
        properties.setProperty("visualMode", visualMode.name());
        try (OutputStream output = Files.newOutputStream(path)) {
            properties.store(output, "Ronova client visuals");
        }
    }

    public ClientPreferences nextVisualMode() {
        return new ClientPreferences(switch (visualMode) {
            case ALL -> VisualMode.HUD_ONLY;
            case HUD_ONLY -> VisualMode.OFF;
            case OFF -> VisualMode.ALL;
        });
    }

    public static ClientPreferences defaults() {
        return new ClientPreferences(VisualMode.ALL);
    }

    public enum VisualMode {
        ALL,
        HUD_ONLY,
        OFF
    }
}
