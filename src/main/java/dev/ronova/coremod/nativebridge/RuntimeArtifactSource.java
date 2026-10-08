package dev.ronova.coremod.nativebridge;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Resolves runtime-critical embedded resources from one coherent artifact.
 *
 * <p>Packaged installs always read their own class-path resources.  An exploded ForgeGradle
 * development run may, after a clean production build, read runtime resources from that one
 * sealed production JAR.  Development classes themselves remain on the Mojmap dev classpath;
 * reobfuscated registered code images are therefore never enabled there.  This is a development
 * parity aid only; it never grants authority and never makes a missing or malformed artifact look
 * verified.</p>
 */
final class RuntimeArtifactSource {
    static final String NATIVE = "native/windows-x86_64/ronova.dll";
    static final String NATIVE_ROOT = "native/windows-x86_64/ronova_root.dll";
    static final String TASK_HOOK = "META-INF/ronova/task-hook-support.jar";
    static final String SEAL_ID = "META-INF/ronova/code-seal-id.txt";
    static final String SEAL_MANIFEST = "META-INF/ronova/code-seal-manifest.json";
    static final String MODS_TOML = "META-INF/mods.toml";
    private static final String TASK_HOOK_CLASS =
        "dev/ronova/coremod/agent/BootstrapTaskHooks.class";
    private static final String PROBE_IMAGE =
        "dev/ronova/coremod/integrity/CodeSealProbe";
    private static final String SOURCE_IMAGE =
        "dev/ronova/coremod/nativebridge/RuntimeArtifactSource";

    enum Origin {
        EXPLODED_DEVELOPMENT,
        PACKAGED_JAR
    }

    private final Path sealedArtifact;
    private final boolean development;

    private RuntimeArtifactSource(Path sealedArtifact, boolean development) {
        this.sealedArtifact = sealedArtifact;
        this.development = development;
    }

    static RuntimeArtifactSource resolve(Path gameDirectory) {
        return resolve(gameDirectory, detectOrigin());
    }

    /**
     * Resolves with an explicit runtime origin.  Production uses {@link #resolve(Path)}; this
     * overload exists so regression tests can exercise both namespace branches without making
     * their expected result depend on whether Gradle launched the test from a classes directory
     * or from the packaged self-test classpath.
     */
    static RuntimeArtifactSource resolve(Path gameDirectory, Origin origin) {
        Objects.requireNonNull(origin, "origin");
        if (origin == Origin.PACKAGED_JAR) return new RuntimeArtifactSource(null, false);
        Path root = developmentProjectRoot(gameDirectory);
        if (root == null) {
            System.err.println("RONOVA_DEV_ARTIFACT_UNAVAILABLE development project root not found; using exploded classpath resources");
            return new RuntimeArtifactSource(null, true);
        }
        Path artifact = selectProductionArtifact(root.resolve("build").resolve("libs"));
        if (artifact != null) {
            System.out.println("RONOVA_DEV_ARTIFACT_READY " + artifact.toAbsolutePath().normalize());
            return new RuntimeArtifactSource(artifact, true);
        }
        System.err.println("RONOVA_DEV_ARTIFACT_UNAVAILABLE clean-build production JAR not found; using exploded classpath resources");
        return new RuntimeArtifactSource(null, true);
    }

    boolean development() { return development; }
    Path sealedArtifact() { return sealedArtifact; }

    /**
     * Registered code-image replacement is only valid when the loaded Ronova classes use the
     * same namespace as the sealed artifact.  ForgeGradle development runs execute Mojmap
     * classes while the clean production JAR is reobfuscated; its embedded Native/code-seal
     * images may therefore be used as resources, but never as replacement class bytes.
     */
    boolean codeSealNamespaceCompatible() { return !development; }

    InputStream open(String resourceName) throws IOException {
        String entry = normalize(resourceName);
        if (sealedArtifact != null) {
            try (ZipFile zip = new ZipFile(sealedArtifact.toFile())) {
                ZipEntry found = zip.getEntry(entry);
                if (found == null || found.isDirectory())
                    throw new IOException("sealed development artifact missing resource: " + entry);
                return new ByteArrayInputStream(zip.getInputStream(found).readAllBytes());
            }
        }
        InputStream input = RuntimeArtifactSource.class.getResourceAsStream('/' + entry);
        if (input == null) throw new IOException("missing native resource: /" + entry);
        return input;
    }

    private static String normalize(String resourceName) {
        Objects.requireNonNull(resourceName, "resourceName");
        String value = resourceName.startsWith("/") ? resourceName.substring(1) : resourceName;
        if (value.isBlank() || value.contains("..") || value.indexOf('\\') >= 0)
            throw new IllegalArgumentException("invalid embedded resource path");
        return value;
    }

    private static Origin detectOrigin() {
        try {
            var source = RuntimeArtifactSource.class.getProtectionDomain().getCodeSource();
            if (source != null && source.getLocation() != null) {
                Path current = Path.of(source.getLocation().toURI()).toAbsolutePath().normalize();
                return Files.isDirectory(current) ? Origin.EXPLODED_DEVELOPMENT : Origin.PACKAGED_JAR;
            }
        } catch (RuntimeException | URISyntaxException ignored) { }
        // Unknown provenance must not enable the development bridge.
        return Origin.PACKAGED_JAR;
    }

    private static Path developmentProjectRoot(Path gameDirectory) {
        try {
            var source = RuntimeArtifactSource.class.getProtectionDomain().getCodeSource();
            if (source != null && source.getLocation() != null) {
                Path current = Path.of(source.getLocation().toURI()).toAbsolutePath().normalize();
                if (Files.isDirectory(current)) {
                    for (int depth = 0; current != null && depth < 8; depth++, current = current.getParent()) {
                        if (current.getFileName() != null && current.getFileName().toString().equals("build"))
                            return current.getParent();
                    }
                }
            }
        } catch (RuntimeException | URISyntaxException ignored) { }
        if (gameDirectory != null) {
            Path game = gameDirectory.toAbsolutePath().normalize();
            Path parent = game.getParent();
            if (game.getFileName() != null && game.getFileName().toString().equalsIgnoreCase("run")
                && parent != null
                && Files.isDirectory(parent.resolve("build").resolve("classes").resolve("java").resolve("main"))
                && Files.isDirectory(parent.resolve("build").resolve("libs"))) return parent;
        }
        return null;
    }

    static Path selectProductionArtifact(Path libraries) {
        if (libraries == null || !Files.isDirectory(libraries)) return null;
        List<Path> candidates = new ArrayList<>();
        try (var stream = Files.list(libraries)) {
            stream.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                .forEach(candidates::add);
        } catch (IOException unavailable) {
            return null;
        }
        candidates.sort(Comparator
            .comparing(RuntimeArtifactSource::lastModified).reversed()
            .thenComparing(path -> path.getFileName().toString()));
        for (Path candidate : candidates) if (validProductionArtifact(candidate)) return candidate;
        return null;
    }

    private static FileTime lastModified(Path path) {
        try { return Files.getLastModifiedTime(path); }
        catch (IOException ignored) { return FileTime.fromMillis(0L); }
    }

    static boolean validProductionArtifact(Path artifact) {
        if (artifact == null || !Files.isRegularFile(artifact)) return false;
        try (ZipFile zip = new ZipFile(artifact.toFile())) {
            for (String required : List.of(NATIVE, NATIVE_ROOT, TASK_HOOK, SEAL_ID, SEAL_MANIFEST, MODS_TOML)) {
                ZipEntry entry = zip.getEntry(required);
                if (entry == null || entry.isDirectory()) return false;
            }
            byte[] nativeBytes = zip.getInputStream(zip.getEntry(NATIVE)).readAllBytes();
            byte[] rootBytes = zip.getInputStream(zip.getEntry(NATIVE_ROOT)).readAllBytes();
            if (!peImage(nativeBytes) || !peImage(rootBytes)) return false;

            String sealId = new String(zip.getInputStream(zip.getEntry(SEAL_ID)).readAllBytes(), StandardCharsets.US_ASCII).trim();
            if (!sealId.matches("[0-9a-f]{64}")) return false;
            String manifest = new String(zip.getInputStream(zip.getEntry(SEAL_MANIFEST)).readAllBytes(), StandardCharsets.UTF_8);
            if (!manifest.contains(sealId) || !manifest.contains(PROBE_IMAGE) || !manifest.contains(SOURCE_IMAGE)) return false;

            byte[] hooks = zip.getInputStream(zip.getEntry(TASK_HOOK)).readAllBytes();
            if (!nestedContains(hooks, TASK_HOOK_CLASS)) return false;
            String mods = new String(zip.getInputStream(zip.getEntry(MODS_TOML)).readAllBytes(), StandardCharsets.UTF_8);
            return mods.contains("modId=\"ronova\"") || mods.contains("modId = \"ronova\"");
        } catch (IOException malformed) {
            return false;
        }
    }

    private static boolean peImage(byte[] image) {
        return image.length > 64 * 1024 && image[0] == 'M' && image[1] == 'Z';
    }

    private static boolean nestedContains(byte[] archive, String name) throws IOException {
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(archive))) {
            for (ZipEntry entry = input.getNextEntry(); entry != null; entry = input.getNextEntry())
                if (name.equals(entry.getName())) return true;
        }
        return false;
    }
}
