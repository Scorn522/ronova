package dev.ronova.coremod.agent;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public record StartupPolicy(ProtectionMode protectionMode, boolean containExternalRetransform, int reassertMillis) {
    public enum ProtectionMode { MIXIN, DUAL_AGENT }

    public static Path defaultPath() {
        return Path.of(System.getProperty("user.dir", "."), "config", "ronova-startup.properties");
    }

    public static StartupPolicy load() {
        return load(defaultPath());
    }

    public static StartupPolicy load(Path path) {
        Properties properties = new Properties();
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                properties.load(reader);
            } catch (IOException ignored) {
                // Defaults are deliberately safe when startup configuration is unreadable.
            }
        }
        ProtectionMode mode;
        try {
            mode = ProtectionMode.valueOf(properties.getProperty("protectionMode", "DUAL_AGENT")
                .trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            mode = ProtectionMode.DUAL_AGENT;
        }
        boolean contain = Boolean.parseBoolean(properties.getProperty("containExternalRetransform", "true"));
        int interval;
        try {
            interval = Integer.parseInt(properties.getProperty("reassertMillis", "30000"));
        } catch (NumberFormatException ignored) {
            interval = 30000;
        }
        // Retransforming Entity/LivingEntity is expensive on a modded client.
        // The ClassFileLoadHook already reapplies Ronova whenever another agent
        // retransforms a class; this interval is only a low-frequency fallback.
        return new StartupPolicy(mode, contain, Math.max(15_000, Math.min(60_000, interval)));
    }

    public void save(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Properties properties = new Properties();
        properties.setProperty("protectionMode", protectionMode.name());
        properties.setProperty("containExternalRetransform", Boolean.toString(containExternalRetransform));
        properties.setProperty("reassertMillis", Integer.toString(reassertMillis));
        try (Writer writer = Files.newBufferedWriter(path)) {
            properties.store(writer, "Ronova early-start policy; restart required");
        }
    }
}
