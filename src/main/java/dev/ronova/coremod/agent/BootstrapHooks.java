package dev.ronova.coremod.agent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.BiPredicate;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Bootstrap-class-loader bridge shared by ModLauncher classes and the attached
 * Agent. It deliberately depends on JDK classes only.
 */
public final class BootstrapHooks {
    private static volatile Predicate<Object> protectedPredicate = ignored -> false;
    private static volatile Predicate<Object> quarantinePredicate = ignored -> false;
    private static volatile Predicate<Object> entityPredicate = ignored -> false;
    private static volatile BiPredicate<Object, Object> removalPredicate =
        (entity, reason) -> safeProtected(entity);
    private static volatile Consumer<Object> protectedViolationListener = ignored -> {};
    private static volatile BooleanSupplier externalControlSuppression = () -> false;
    private static volatile BiConsumer<Object, Object> nbtCopyObserver = (source, result) -> {};
    private static volatile BiPredicate<Object, Object> serializedSpawnPredicate = (level, tag) -> false;

    private BootstrapHooks() {}

    public static void installPredicate(Predicate<Object> predicate) {
        protectedPredicate = Objects.requireNonNull(predicate, "predicate");
    }

    public static void installQuarantinePredicate(Predicate<Object> predicate) {
        quarantinePredicate = Objects.requireNonNull(predicate, "predicate");
    }

    public static void installEntityPredicate(Predicate<Object> predicate) {
        entityPredicate = Objects.requireNonNull(predicate, "predicate");
    }

    public static void installRemovalPredicate(BiPredicate<Object, Object> predicate) {
        removalPredicate = Objects.requireNonNull(predicate, "predicate");
    }

    public static void installProtectedViolationListener(Consumer<Object> listener) {
        protectedViolationListener = Objects.requireNonNull(listener, "listener");
    }

    public static void installExternalControlSuppression(BooleanSupplier suppression) {
        externalControlSuppression = Objects.requireNonNull(suppression, "suppression");
    }

    public static void installNbtCopyObserver(BiConsumer<Object, Object> observer) {
        nbtCopyObserver = Objects.requireNonNull(observer, "observer");
    }
    public static void installSerializedSpawnPredicate(BiPredicate<Object, Object> predicate) {
        serializedSpawnPredicate = Objects.requireNonNull(predicate, "predicate");
    }
    public static boolean rejectSerializedSpawn(Object level, Object tag) {
        try { return serializedSpawnPredicate.test(level, tag); }
        catch (RuntimeException unavailable) { return false; }
    }

    /** Observation preserves the original return identity and exceptions of the copy operation. */
    public static Object observeNbtCopy(Object source, Object result) {
        try { nbtCopyObserver.accept(source, result); }
        catch (RuntimeException ignored) { /* Missing evidence never grants source disposal permission. */ }
        return result;
    }

    public static boolean shouldBlockRemoval(Object entity, Object reason) {
        return rejectsRemoval(entity, reason);
    }

    public static boolean shouldBlockRevive(Object entity) {
        return safeQuarantined(entity);
    }

    public static boolean shouldBlockDamage(Object entity) {
        return rejectsProtected(entity);
    }

    public static boolean shouldBlockDeath(Object entity) {
        return rejectsProtected(entity);
    }

    public static boolean shouldBlockHealth(Object entity, float health) {
        return rejectsTransition(entity, health <= 0.0F);
    }

    public static boolean shouldSuppressRender(Object entity) {
        if (safeQuarantined(entity)) {
            return true;
        }
        return !safeEntity(entity) && shouldSuppressExternalVisual();
    }

    public static boolean shouldSuppressExternalVisual() {
        return shouldSuppressExternalControl();
    }

    public static boolean shouldSuppressExternalControl() {
        try {
            return externalControlSuppression.getAsBoolean();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Filters query results after every participating transformer has run. */
    public static Object filterQuarantinedResult(Object result) {
        if (!(result instanceof Iterable<?> iterable)) {
            return result;
        }
        if (result instanceof List<?> source) {
            return filterList(source);
        }
        if (result instanceof Collection<?> source) {
            boolean quarantined = false;
            for (Object value : source) {
                if (safeQuarantined(value)) {
                    quarantined = true;
                    break;
                }
            }
            if (!quarantined) {
                return result;
            }
            Collection<Object> filtered = result instanceof Set<?>
                ? new LinkedHashSet<>(Math.max(16, source.size()))
                : new ArrayList<>(source.size());
            for (Object value : source) {
                if (!safeQuarantined(value)) {
                    filtered.add(value);
                }
            }
            return filtered;
        }

        // Unknown Iterable implementations can be one-shot, so buffer and
        // return the single pass even when no quarantine entry was found.
        List<Object> buffered = new ArrayList<>();
        for (Object value : iterable) {
            if (!safeQuarantined(value)) {
                buffered.add(value);
            }
        }
        return buffered;
    }

    private static Object filterList(List<?> source) {
        List<Object> filtered = null;
        int index = 0;
        for (Object value : source) {
            boolean quarantined = safeQuarantined(value);
            if (quarantined) {
                if (filtered == null) {
                    filtered = new ArrayList<>(source.size() - 1);
                    filtered.addAll(source.subList(0, index));
                }
            } else if (filtered != null) {
                filtered.add(value);
            }
            index++;
        }
        return filtered == null ? source : filtered;
    }

    public static Object filterQuarantinedEntity(Object entity) {
        return safeQuarantined(entity) ? null : entity;
    }

    /*
     * External classes sometimes bypass lifecycle methods and write Forge's
     * public fields directly. The transformer replaces only those external
     * PUTFIELD instructions with these bootstrap-safe gates.
     */
    public static void writeRemovalReason(Object entity, Object reason) {
        if (reason == null ? !safeQuarantined(entity) : !rejectsRemoval(entity, reason)) {
            writeField(entity, reason, "removalReason", "f_146795_");
        }
    }

    public static void writeAddedToWorld(Object entity, boolean value) {
        if (value ? !safeQuarantined(entity) : !rejectsRemoval(entity, null)) {
            writeField(entity, value, "isAddedToWorld");
        }
    }

    public static void writeDead(Object entity, boolean value) {
        if (!rejectsTransition(entity, value)) {
            writeField(entity, value, "dead", "f_20890_");
        }
    }

    public static void writeDeathTime(Object entity, int value) {
        if (!rejectsTransition(entity, value > 0)) {
            writeField(entity, value, "deathTime", "f_20919_");
        }
    }

    /** Terminal writes are rejected for protected owners; live writes are rejected for quarantine. */
    private static boolean rejectsTransition(Object entity, boolean terminalState) {
        return terminalState ? rejectsProtected(entity) : safeQuarantined(entity);
    }

    private static boolean rejectsProtected(Object value) {
        if (!safeProtected(value)) {
            return false;
        }
        try {
            protectedViolationListener.accept(value);
        } catch (Throwable ignored) {
            // Lifecycle blocking remains authoritative even if observation fails.
        }
        return true;
    }

    private static boolean rejectsRemoval(Object entity, Object reason) {
        boolean rejected;
        try {
            rejected = removalPredicate.test(entity, reason);
        } catch (Throwable ignored) {
            rejected = safeProtected(entity);
        }
        if (!rejected) {
            return false;
        }
        try {
            protectedViolationListener.accept(entity);
        } catch (Throwable ignored) {
            // Removal blocking remains authoritative if observation fails.
        }
        return true;
    }

    private static boolean safeProtected(Object value) {
        try {
            return protectedPredicate.test(value);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean safeQuarantined(Object value) {
        try {
            return quarantinePredicate.test(value);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean safeEntity(Object value) {
        try {
            return entityPredicate.test(value);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void writeField(Object target, Object value, String... names) {
        if (target == null) {
            return;
        }
        Class<?> type = target.getClass();
        while (type != null) {
            for (String name : names) {
                try {
                    Field field = type.getDeclaredField(name);
                    if (!field.canAccess(target)) {
                        field.setAccessible(true);
                    }
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException ignored) {
                    // Try the next mapped alias or superclass.
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    return;
                }
            }
            type = type.getSuperclass();
        }
    }
}
