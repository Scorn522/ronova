package dev.ronova.coremod.nativebridge;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Bounded artifact-selection regression for ForgeGradle exploded run parity. */
public final class RuntimeArtifactSourceSelfTest {
    private RuntimeArtifactSourceSelfTest() {}

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("ronova-runtime-artifact-");
        try {
            Path libs = Files.createDirectories(root.resolve("build/libs"));
            Path good = libs.resolve("ronova-good.jar");
            writeArtifact(good, true, true);
            Files.setLastModifiedTime(good, FileTime.fromMillis(1_000L));

            Path newerBroken = libs.resolve("ronova-newer-broken.jar");
            writeArtifact(newerBroken, false, true);
            Files.setLastModifiedTime(newerBroken, FileTime.fromMillis(2_000L));

            require(RuntimeArtifactSource.validProductionArtifact(good), "valid sealed artifact rejected");
            require(!RuntimeArtifactSource.validProductionArtifact(newerBroken), "missing task hook accepted");
            require(good.equals(RuntimeArtifactSource.selectProductionArtifact(libs)), "selector preferred malformed newer artifact");
            Files.createDirectories(root.resolve("build/classes/java/main"));
            Path run = Files.createDirectories(root.resolve("run"));
            RuntimeArtifactSource resolved = RuntimeArtifactSource.resolve(
                run, RuntimeArtifactSource.Origin.EXPLODED_DEVELOPMENT
            );
            require(resolved.development() && good.equals(resolved.sealedArtifact()), "explicit dev origin did not bind the coherent sealed artifact");
            require(!resolved.codeSealNamespaceCompatible(), "dev run must not enable reobf code-image replacement against Mojmap classes");
            try (var seal = resolved.open('/' + RuntimeArtifactSource.SEAL_ID)) {
                require(seal.readAllBytes().length == 64, "dev artifact seal id was not read from the selected JAR");
            }

            RuntimeArtifactSource packaged = RuntimeArtifactSource.resolve(
                run, RuntimeArtifactSource.Origin.PACKAGED_JAR
            );
            require(!packaged.development(), "explicit packaged origin entered development mode");
            require(packaged.sealedArtifact() == null, "packaged origin borrowed a development artifact");
            require(packaged.codeSealNamespaceCompatible(), "packaged origin incorrectly fenced production code seals");

            Path badSeal = libs.resolve("ronova-bad-seal.jar");
            writeArtifact(badSeal, true, false);
            require(!RuntimeArtifactSource.validProductionArtifact(badSeal), "mismatched seal manifest accepted");
        } finally {
            try (var walk = Files.walk(root)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (Exception ignored) { }
                });
            }
        }
        System.out.println("RuntimeArtifactSourceSelfTest PASS (explicit origin injection, single sealed dev artifact, packaged/dev namespace fence)");
    }

    private static void writeArtifact(Path output, boolean includeHook, boolean correctSeal) throws Exception {
        String id = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        byte[] pe = new byte[70 * 1024]; pe[0] = 'M'; pe[1] = 'Z';
        byte[] hooks = nestedHook();
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
            put(zip, RuntimeArtifactSource.NATIVE, pe);
            put(zip, RuntimeArtifactSource.NATIVE_ROOT, pe);
            if (includeHook) put(zip, RuntimeArtifactSource.TASK_HOOK, hooks);
            put(zip, RuntimeArtifactSource.SEAL_ID, id.getBytes(StandardCharsets.US_ASCII));
            String manifest = "{\"build_id\":\"" + (correctSeal ? id : "f".repeat(64)) + "\",\"classes\":{"
                + "\"dev/ronova/coremod/integrity/CodeSealProbe\":\"x\","
                + "\"dev/ronova/coremod/nativebridge/RuntimeArtifactSource\":\"y\"}}";
            put(zip, RuntimeArtifactSource.SEAL_MANIFEST, manifest.getBytes(StandardCharsets.UTF_8));
            put(zip, RuntimeArtifactSource.MODS_TOML, "modId=\"ronova\"\n".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static byte[] nestedHook() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream nested = new ZipOutputStream(bytes)) {
            put(nested, "dev/ronova/coremod/agent/BootstrapTaskHooks.class", new byte[]{1,2,3});
        }
        return bytes.toByteArray();
    }

    private static void put(ZipOutputStream zip, String name, byte[] value) throws Exception {
        zip.putNextEntry(new ZipEntry(name)); zip.write(value); zip.closeEntry();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
