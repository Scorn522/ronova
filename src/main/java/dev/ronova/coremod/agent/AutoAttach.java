package dev.ronova.coremod.agent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class AutoAttach {
    private AutoAttach() {}

    public static Result ensureAttached(Path modJar) {
        if (RonovaAgent.isInstalled()) {
            return Result.ALREADY_ACTIVE;
        }
        if (StartupPolicy.load().protectionMode() != StartupPolicy.ProtectionMode.DUAL_AGENT) {
            return Result.DISABLED_BY_POLICY;
        }
        if (modJar == null || !Files.isRegularFile(modJar)) {
            return Result.DEVELOPMENT_ENVIRONMENT;
        }

        String executable = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
            ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executable);
        try {
            Process helper = new ProcessBuilder(
                java.toString(),
                "--add-modules", "jdk.attach",
                "-cp", modJar.toAbsolutePath().toString(),
                AttachHelper.class.getName(),
                Long.toString(ProcessHandle.current().pid()),
                modJar.toAbsolutePath().toString()
            ).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.INHERIT).start();

            if (!helper.waitFor(15, TimeUnit.SECONDS)) {
                helper.destroy();
                return Result.TIMED_OUT;
            }
            return helper.exitValue() == 0 && RonovaAgent.isInstalled() ? Result.ATTACHED : Result.UNAVAILABLE;
        } catch (Exception ignored) {
            return Result.UNAVAILABLE;
        }
    }

    public enum Result {
        ATTACHED,
        ALREADY_ACTIVE,
        NATIVE_ACTIVE,
        DISABLED_BY_POLICY,
        DEVELOPMENT_ENVIRONMENT,
        TIMED_OUT,
        UNAVAILABLE
    }
}
