package dev.ronova.coremod.observation;

import dev.ronova.coremod.root.api.RecoverySnapshotPlan;
import dev.ronova.coremod.root.api.RecoverySourceLocation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/** Resolve hints against live objects only. Results never prove recovery causality or authorize a write. */
public final class RecoveryLocationResolver {
    public enum State { PRESENT, ENTRY_ABSENT, SNAPSHOT_EMPTY, UNAVAILABLE, BUDGET_EXHAUSTED }
    public record View(Object owner, Field mapField, Map<?, ?> container, Object record,
        Field snapshotField, Object slot, Object snapshot) {}
    public record Result(State state, View view) {}
    private RecoveryLocationResolver() {}
    public static Result loaded(Object owner, RecoverySourceLocation location, UUID subject, BooleanSupplier budget) {
        if (owner == null || location == null || location.scope() != RecoverySnapshotPlan.StorageScope.LOADED_SAVED_DATA)
            return result(State.UNAVAILABLE);
        return resolve(owner, owner.getClass(), false, location, subject, budget);
    }
    /** Caller must supply a class already executing in an observed initialized caller chain, never a class-name lookup. */
    public static Result fromObservedClass(Class<?> caller, RecoverySourceLocation location, UUID subject, BooleanSupplier budget) {
        if (caller == null || location == null || location.scope() != RecoverySnapshotPlan.StorageScope.STATIC_MANAGER)
            return result(State.UNAVAILABLE);
        return resolve(caller, caller, true, location, subject, budget);
    }
    private static Result resolve(Object owner, Class<?> type, boolean statik, RecoverySourceLocation location,
        UUID subject, BooleanSupplier budget) {
        if (subject == null || budget == null) return result(State.UNAVAILABLE);
        try {
            Class<?> declaring = uniqueOwner(type, location.mapOwner(), budget);
            if (declaring == null) return result(State.UNAVAILABLE);
            Field mapField = declaring.getDeclaredField(location.mapField());
            if (Modifier.isStatic(mapField.getModifiers()) != statik || !mapField.trySetAccessible()) return result(State.UNAVAILABLE);
            Object holder = statik ? null : owner;
            Object rawMap = mapField.get(holder);
            if (!safeMap(rawMap)) return result(State.UNAVAILABLE);
            Map<?, ?> map = (Map<?, ?>) rawMap;
            Object record = map.get(subject);
            if (record == null) {
                boolean placeholder = map.containsKey(subject);
                if (!budget.getAsBoolean()) return result(State.BUDGET_EXHAUSTED);
                if (mapField.get(holder) != map || map.get(subject) != null || map.containsKey(subject) != placeholder)
                    return result(State.UNAVAILABLE);
                return result(placeholder ? State.SNAPSHOT_EMPTY : State.ENTRY_ABSENT);
            }
            Field snapshotField = null; Object slot = record;
            if (!location.directRecord()) {
                Class<?> imageOwner = uniqueOwner(record.getClass(), location.snapshotOwner(), budget);
                if (imageOwner == null) return result(State.UNAVAILABLE);
                snapshotField = imageOwner.getDeclaredField(location.snapshotField());
                if (Modifier.isStatic(snapshotField.getModifiers()) || !snapshotField.trySetAccessible()) return result(State.UNAVAILABLE);
                slot = snapshotField.get(record);
            }
            if (!budget.getAsBoolean()) return result(State.BUDGET_EXHAUSTED);
            Object image = AtomicSnapshotReference.unwrap(slot);
            // Re-read the same locations before returning even a transient lookup result.
            if (mapField.get(holder) != map || map.get(subject) != record
                || (snapshotField == null ? record : snapshotField.get(record)) != slot
                || AtomicSnapshotReference.unwrap(slot) != image) return result(State.UNAVAILABLE);
            return new Result(image == null ? State.SNAPSHOT_EMPTY : State.PRESENT,
                new View(owner, mapField, map, record, snapshotField, slot, image));
        } catch (BudgetExhausted exhausted) { return result(State.BUDGET_EXHAUSTED); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return result(State.UNAVAILABLE); }
    }
    private static Class<?> uniqueOwner(Class<?> type, String name, BooleanSupplier budget) {
        Class<?> matched = null;
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (!budget.getAsBoolean()) throw new BudgetExhausted();
            if (current.getName().equals(name)) {
                if (matched != null) return null; // Same binary name in distinct loaders is ambiguous.
                matched = current;
            }
        }
        return matched;
    }
    private static final class BudgetExhausted extends RuntimeException {}
    private static boolean safeMap(Object value) {
        return value != null && (value.getClass() == HashMap.class || value.getClass() == LinkedHashMap.class
            || value.getClass() == ConcurrentHashMap.class);
    }
    private static Result result(State state) { return new Result(state, null); }
}
