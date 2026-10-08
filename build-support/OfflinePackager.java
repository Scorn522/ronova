package dev.ronova.coremod.build;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Deterministic offline release overlay used when ForgeGradle is unavailable. */
public final class OfflinePackager {
    private static final long ZIP_EPOCH = 315_532_800_000L;

    private OfflinePackager() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException(
                "usage: <baseline.jar> <reobf-classes> <resources> <gradle.properties> <output.jar>"
            );
        }
        Path baseline = Path.of(args[0]);
        Path classes = Path.of(args[1]);
        Path resources = Path.of(args[2]);
        Path propertiesPath = Path.of(args[3]);
        Path output = Path.of(args[4]);

        Map<String, byte[]> entries = readArchive(baseline);
        pruneReplacedNestMembers(entries, classes);
        overlayTree(entries, classes, "");
        overlayTree(entries, resources, "");

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(propertiesPath, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        String template = Files.readString(resources.resolve("META-INF/mods.toml"), StandardCharsets.UTF_8);
        entries.put("META-INF/mods.toml", expand(template, properties).getBytes(StandardCharsets.UTF_8));
        entries.put("META-INF/MANIFEST.MF", manifest(properties).getBytes(StandardCharsets.UTF_8));

        byte[] bootstrap = entries.get("dev/ronova/coremod/agent/BootstrapHooks.class");
        if (bootstrap == null) {
            throw new IllegalStateException("updated BootstrapHooks.class is missing");
        }
        entries.put("META-INF/ronova/hook-support.jar", hookArchive(bootstrap));
        Map<String, byte[]> taskHooks = new TreeMap<>();
        entries.forEach((name, content) -> {
            if (name.startsWith("dev/ronova/coremod/agent/BootstrapTaskHooks") && name.endsWith(".class")) {
                taskHooks.put(name, content);
            }
        });
        if (!taskHooks.containsKey("dev/ronova/coremod/agent/BootstrapTaskHooks.class")) {
            throw new IllegalStateException("updated BootstrapTaskHooks.class is missing");
        }
        entries.put("META-INF/ronova/task-hook-support.jar", hookArchive(taskHooks));

        entries.keySet().removeIf(name ->
            name.startsWith("META-INF/")
                && (name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".DSA"))
        );
        writeArchive(entries, output);
        System.out.println("packaged " + output + " (" + entries.size() + " files)");
    }

    /** Removes stale nested classes when an overlaid top-level class no longer owns them. */
    private static void pruneReplacedNestMembers(
        Map<String, byte[]> entries,
        Path classes
    ) throws IOException {
        Set<String> overlay;
        try (var paths = Files.walk(classes)) {
            overlay = paths.filter(Files::isRegularFile)
                .map(path -> classes.relativize(path).toString().replace('\\', '/'))
                .filter(name -> name.endsWith(".class"))
                .collect(java.util.stream.Collectors.toSet());
        }
        for (String topLevel : overlay.stream()
            .filter(name -> !name.substring(name.lastIndexOf('/') + 1).contains("$"))
            .toList()) {
            String stem = topLevel.substring(0, topLevel.length() - ".class".length());
            entries.keySet().removeIf(name -> name.startsWith(stem + "$")
                && name.endsWith(".class") && !overlay.contains(name));
        }
    }

    private static Map<String, byte[]> readArchive(Path archive) throws IOException {
        Map<String, byte[]> result = new TreeMap<>();
        try (InputStream input = Files.newInputStream(archive);
             ZipInputStream zip = new ZipInputStream(input)) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                if (!entry.isDirectory()) {
                    result.put(entry.getName(), zip.readAllBytes());
                }
            }
        }
        return result;
    }

    private static void overlayTree(Map<String, byte[]> entries, Path root, String prefix)
        throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                String relative = root.relativize(path).toString().replace('\\', '/');
                entries.put(prefix + relative, Files.readAllBytes(path));
            }
        }
    }

    private static String expand(String template, Properties properties) {
        String result = template;
        for (String name : properties.stringPropertyNames()) {
            result = result.replace("${" + name + "}", properties.getProperty(name));
        }
        if (result.contains("${")) {
            throw new IllegalStateException("unexpanded mods.toml property");
        }
        return result;
    }

    private static String manifest(Properties properties) {
        return String.join("\r\n",
            "Manifest-Version: 1.0",
            "Specification-Title: " + properties.getProperty("mod_id"),
            "Specification-Version: 1",
            "Implementation-Title: " + properties.getProperty("mod_name"),
            "Implementation-Version: " + properties.getProperty("mod_version"),
            "Implementation-Vendor: " + properties.getProperty("mod_authors"),
            "MixinConfigs: ronova.mixins.json",
            "Premain-Class: dev.ronova.coremod.agent.RonovaAgent",
            "Agent-Class: dev.ronova.coremod.agent.RonovaAgent",
            "Can-Redefine-Classes: true",
            "Can-Retransform-Classes: true",
            "",
            ""
        );
    }

    private static byte[] hookArchive(byte[] bootstrap) throws IOException {
        return hookArchive(Map.of("dev/ronova/coremod/agent/BootstrapHooks.class", bootstrap));
    }

    private static byte[] hookArchive(Map<String, byte[]> classes) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            put(zip, "META-INF/MANIFEST.MF",
                "Manifest-Version: 1.0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            for (Map.Entry<String, byte[]> entry : new TreeMap<>(classes).entrySet()) {
                put(zip, entry.getKey(), entry.getValue());
            }
        }
        return bytes.toByteArray();
    }

    private static void writeArchive(Map<String, byte[]> entries, Path output) throws IOException {
        Files.createDirectories(output.toAbsolutePath().getParent());
        try (OutputStream stream = Files.newOutputStream(output);
             ZipOutputStream zip = new ZipOutputStream(stream, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                put(zip, entry.getKey(), entry.getValue());
            }
        }
    }

    private static void put(ZipOutputStream zip, String name, byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(ZIP_EPOCH);
        zip.putNextEntry(entry);
        try (InputStream input = new ByteArrayInputStream(content)) {
            input.transferTo(zip);
        }
        zip.closeEntry();
    }
}
