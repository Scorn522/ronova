package dev.ronova.coremod.agent;

import java.lang.instrument.Instrumentation;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.jar.JarFile;

public final class RonovaAgent {
    private static final String INSTALLED_PROPERTY = "ronova.dominion.agent.installed";
    private static final String EARLY_PROPERTY = "ronova.dominion.agent.early";
    private static final String LATE_PROPERTY = "ronova.dominion.agent.late";
    private static volatile Instrumentation instrumentation;
    private static volatile boolean earlyActive;
    private static volatile boolean lateActive;
    private static volatile boolean watchdogStarted;
    private static volatile JarFile bootstrapSupport;
    private static volatile JarFile hookSupport;
    private static StartupPolicy policy = StartupPolicy.load();

    private RonovaAgent() {}

    public static void premain(String arguments, Instrumentation value) {
        install(value, true);
    }

    public static void agentmain(String arguments, Instrumentation value) {
        install(value, false);
    }

    private static synchronized void install(Instrumentation value, boolean early) {
        instrumentation = value;
        System.setProperty(INSTALLED_PROPERTY, "true");
        policy = StartupPolicy.load();
        if (policy.protectionMode() == StartupPolicy.ProtectionMode.DUAL_AGENT) {
            installBootstrapSupport(value);
            value.addTransformer(new RonovaTransformer("early"), value.isRetransformClassesSupported());
            earlyActive = early;
            System.setProperty(EARLY_PROPERTY, Boolean.toString(early));
        }
    }

    private static void installBootstrapSupport(Instrumentation value) {
        if (bootstrapSupport != null) {
            return;
        }
        try (InputStream source = RonovaAgent.class.getResourceAsStream("/META-INF/ronova/asm-support.jar");
             InputStream hooks = RonovaAgent.class.getResourceAsStream("/META-INF/ronova/hook-support.jar")) {
            if (source == null) {
                throw new IllegalStateException("Missing embedded Agent ASM support");
            }
            if (hooks == null) {
                throw new IllegalStateException("Missing embedded bootstrap hook support");
            }
            Path extracted = Files.createTempFile("ronova-agent-asm-", ".jar");
            Files.copy(source, extracted, StandardCopyOption.REPLACE_EXISTING);
            extracted.toFile().deleteOnExit();
            JarFile support = new JarFile(extracted.toFile());
            value.appendToSystemClassLoaderSearch(support);
            Class.forName("org.objectweb.asm.ClassVisitor", false, ClassLoader.getSystemClassLoader());
            bootstrapSupport = support;

            Path extractedHooks = Files.createTempFile("ronova-agent-hooks-", ".jar");
            Files.copy(hooks, extractedHooks, StandardCopyOption.REPLACE_EXISTING);
            extractedHooks.toFile().deleteOnExit();
            JarFile sharedHooks = new JarFile(extractedHooks.toFile());
            value.appendToBootstrapClassLoaderSearch(sharedHooks);
            Class.forName("dev.ronova.coremod.agent.BootstrapHooks", false, null);
            hookSupport = sharedHooks;
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot initialize embedded Agent ASM support", exception);
        }
    }

    public static synchronized void armLateGuard() {
        Instrumentation value = instrumentation;
        if (value == null || lateActive || policy.protectionMode() != StartupPolicy.ProtectionMode.DUAL_AGENT) {
            return;
        }
        value.addTransformer(new RonovaTransformer("late"), value.isRetransformClassesSupported());
        lateActive = true;
        System.setProperty(LATE_PROPERTY, "true");
    }

    public static synchronized void markRuntimeReady() {
        if (!lateActive) {
            return;
        }
        startWatchdog();
    }

    private static void startWatchdog() {
        if (watchdogStarted || instrumentation == null || !policy.containExternalRetransform()) {
            return;
        }
        watchdogStarted = true;
        Thread watchdog = new Thread(() -> {
            while (instrumentation != null && policy.containExternalRetransform()) {
                try {
                    Thread.sleep(policy.reassertMillis());
                    reassertTargets();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable ignored) {
                    // Runtime containment is best effort; Forge must remain bootable.
                }
            }
        }, "Ronova-Integrity-Reassert");
        watchdog.setDaemon(true);
        watchdog.setPriority(Thread.NORM_PRIORITY - 1);
        watchdog.start();
    }

    private static void reassertTargets() {
        Instrumentation value = instrumentation;
        if (value == null || !value.isRetransformClassesSupported()) {
            return;
        }
        Class<?>[] targets = Arrays.stream(value.getAllLoadedClasses())
            .filter(value::isModifiableClass)
            .filter(type -> RonovaTransformer.isTarget(type.getName().replace('.', '/')))
            .toArray(Class<?>[]::new);
        if (targets.length > 0) {
            try {
                value.retransformClasses(targets);
            } catch (Throwable ignored) {
                // A hostile or limited runtime can reject retransformation.
            }
        }
    }

    public static boolean isEarlyActive() {
        return earlyActive || Boolean.getBoolean(EARLY_PROPERTY);
    }

    public static boolean isLateActive() {
        return lateActive || Boolean.getBoolean(LATE_PROPERTY);
    }

    public static boolean isInstalled() {
        return instrumentation != null || Boolean.getBoolean(INSTALLED_PROPERTY);
    }
}
