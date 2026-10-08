package dev.ronova.coremod.agent;

import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Bootstrap-safe bridge called by transformed Minecraft methods. */
public final class AgentHooks {
    private AgentHooks() {}

    public static void installPredicate(Predicate<Object> predicate) {
        BootstrapHooks.installPredicate(predicate);
        invokeSystemAgent("armLateGuard");
    }

    public static void installQuarantinePredicate(Predicate<Object> predicate) {
        BootstrapHooks.installQuarantinePredicate(predicate);
    }

    public static void installEntityPredicate(Predicate<Object> predicate) {
        BootstrapHooks.installEntityPredicate(predicate);
    }

    public static void installRemovalPredicate(BiPredicate<Object, Object> predicate) {
        BootstrapHooks.installRemovalPredicate(predicate);
    }

    public static void installProtectedViolationListener(Consumer<Object> listener) {
        BootstrapHooks.installProtectedViolationListener(listener);
    }

    public static void installExternalControlSuppression(BooleanSupplier suppression) {
        BootstrapHooks.installExternalControlSuppression(suppression);
    }

    public static boolean shouldBlockRemoval(Object entity, Object reason) {
        return BootstrapHooks.shouldBlockRemoval(entity, reason);
    }

    public static boolean shouldBlockDamage(Object entity) {
        return BootstrapHooks.shouldBlockDamage(entity);
    }

    public static boolean shouldBlockDeath(Object entity) {
        return BootstrapHooks.shouldBlockDeath(entity);
    }

    public static boolean shouldBlockHealth(Object entity, float health) {
        return BootstrapHooks.shouldBlockHealth(entity, health);
    }

    public static boolean isAgentActive() {
        return Boolean.getBoolean("ronova.dominion.agent.early");
    }

    public static boolean isLateGuardActive() {
        return Boolean.getBoolean("ronova.dominion.agent.late");
    }

    public static void markRuntimeReady() {
        invokeSystemAgent("markRuntimeReady");
    }

    private static void invokeSystemAgent(String methodName) {
        try {
            Class<?> agent = Class.forName(RonovaAgent.class.getName(), true, ClassLoader.getSystemClassLoader());
            Method method = agent.getMethod(methodName);
            method.invoke(null);
        } catch (ReflectiveOperationException ignored) {
            // Mixin/event protection remains active if Instrumentation is unavailable.
        }
    }
}
