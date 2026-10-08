package dev.ronova.coremod.observation;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Finds a live holder of the exact snapshot being copied, without calling record getters. */
public final class StaticSnapshotLocator {
    private static final int FIELD_LIMIT = 64;
    public record Binding(Class<?> owner, Field mapField, Map<?, ?> map, UUID subject,
                          Object record, Field snapshotField, Object slot, Object snapshot) {
        public WeakBinding weaken() { return new WeakBinding(this); }
        public boolean stillBound() {
            if (snapshot == null || !safeMap(map) || mapField.getDeclaringClass() != owner
                || !Modifier.isStatic(mapField.getModifiers())) return false;
            try {
                return mapField.get(null) == map && map.get(subject) == record
                    && (snapshotField == null ? record : snapshotField.get(record)) == slot
                    && AtomicSnapshotReference.bound(slot, snapshot);
            } catch (IllegalAccessException | RuntimeException | LinkageError unavailable) { return false; }
        }
    }
    /** A retained source must not keep its manager's defining loader alive through reflection Fields. */
    public static final class WeakBinding {
        private final WeakReference<Class<?>> owner, imageOwner;
        private final WeakReference<Map<?, ?>> map;
        private final WeakReference<Object> record, slot, snapshot;
        private final String mapName, imageName;
        private final UUID subject;
        private WeakBinding(Binding binding) {
            owner = new WeakReference<>(binding.owner());
            imageOwner = new WeakReference<>(binding.snapshotField() == null ? null : binding.snapshotField().getDeclaringClass());
            map = new WeakReference<>(binding.map()); record = new WeakReference<>(binding.record());
            slot = new WeakReference<>(binding.slot()); snapshot = new WeakReference<>(binding.snapshot());
            mapName = binding.mapField().getName();
            imageName = binding.snapshotField() == null ? null : binding.snapshotField().getName();
            subject = binding.subject();
        }
        public boolean stillBound() {
            return bindingObservation() == StorageBindingObservation.BOUND;
        }
        public StorageBindingObservation bindingObservation() {
            Object expected = snapshot.get();
            return expected == null ? StorageBindingObservation.DETACHED : inspectContents(expected);
        }
        public boolean emptyAtomicSlot() { return inspectContents(null) == StorageBindingObservation.BOUND; }
        public boolean directRecord() { return imageName == null; }
        public boolean recordAbsent() {
            var type = owner.get(); var container = map.get();
            if (!directRecord() || type == null || container == null || container.getClass() != ConcurrentHashMap.class) return false;
            try {
                Field field = type.getDeclaredField(mapName);
                return Modifier.isStatic(field.getModifiers()) && field.trySetAccessible()
                    && field.get(null) == container && !container.containsKey(subject);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return false; }
        }
        private StorageBindingObservation inspectContents(Object expectedSnapshot) {
            var type = owner.get(); var container = map.get(); var value = record.get();
            var expectedSlot = slot.get();
            if (type == null || container == null || value == null || expectedSlot == null) return StorageBindingObservation.DETACHED;
            if (!safeMap(container)) return StorageBindingObservation.UNAVAILABLE;
            try {
                Field field = type.getDeclaredField(mapName);
                if (!Modifier.isStatic(field.getModifiers())) return StorageBindingObservation.DETACHED;
                if (!field.trySetAccessible()) return StorageBindingObservation.UNAVAILABLE;
                if (field.get(null) != container || container.get(subject) != value) return StorageBindingObservation.DETACHED;
                Object currentSlot = value;
                if (imageName != null) {
                    var recordType = imageOwner.get();
                    if (recordType == null) return StorageBindingObservation.DETACHED;
                    Field image = recordType.getDeclaredField(imageName);
                    if (Modifier.isStatic(image.getModifiers())) return StorageBindingObservation.DETACHED;
                    if (!image.trySetAccessible()) return StorageBindingObservation.UNAVAILABLE;
                    currentSlot = image.get(value);
                }
                return currentSlot == expectedSlot
                    && (expectedSnapshot != null ? AtomicSnapshotReference.bound(currentSlot, expectedSnapshot)
                        : currentSlot.getClass() == AtomicReference.class && ((AtomicReference<?>) currentSlot).get() == null)
                    ? StorageBindingObservation.BOUND : StorageBindingObservation.DETACHED;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return StorageBindingObservation.UNAVAILABLE; }
        }
    }
    public record Search(Binding binding, boolean incomplete) {}
    /** Discovery position only: never a witness for a copy that happened earlier. */
    public static final class Cursor {
        private WeakReference<Class<?>> caller = new WeakReference<>(null);
        private UUID subject;
        private int outer, inner;
        private WeakReference<Object> map = new WeakReference<>(null);
        private WeakReference<Object> record = new WeakReference<>(null);
        private boolean inside;
        private String callerLayout, recordLayout;
        public void clear() {
            caller.clear(); subject = null; outer = inner = 0; map.clear(); record.clear(); inside = false;
            callerLayout = recordLayout = null;
        }
        private void advance() { outer++; inner = 0; map.clear(); record.clear(); inside = false; }
    }
    private StaticSnapshotLocator() {}

    /** Callers must independently establish the real copy event, Realm, qualification and code evidence. */
    public static Search find(Class<?> caller, UUID subject, Object snapshot, BooleanSupplier budget) {
        return find(caller, subject, snapshot, budget, new Cursor());
    }

    /** Resume structure discovery, but re-read every container and validate this event's exact image. */
    public static Search find(Class<?> caller, UUID subject, Object snapshot, BooleanSupplier budget, Cursor cursor) {
        Objects.requireNonNull(caller); Objects.requireNonNull(subject);
        Objects.requireNonNull(snapshot); Objects.requireNonNull(budget); Objects.requireNonNull(cursor);
        if (cursor.caller.get() != caller || !subject.equals(cursor.subject)) {
            cursor.clear(); cursor.caller = new WeakReference<>(caller); cursor.subject = subject;
        }
        int inspected = 0;
        boolean incomplete = false;
        try {
            String callerLayout = ReflectionFieldLayout.hierarchyShape(caller);
            if (!callerLayout.equals(cursor.callerLayout)) {
                cursor.outer = cursor.inner = 0; cursor.inside = false; cursor.map.clear(); cursor.record.clear();
                cursor.callerLayout = callerLayout; cursor.recordLayout = null;
            }
            int outer = 0;
            for (Class<?> type = caller; type != null && type != Object.class; type = type.getSuperclass()) {
                var fields = ReflectionFieldLayout.get(type);
                for (int fieldIndex = 0; fieldIndex < fields.size(); fieldIndex++) {
                    Field field = fields.field(fieldIndex);
                    if (outer++ < cursor.outer) continue;
                    // A resumed field slice includes its mandatory container revalidation.
                    boolean innerCredit = cursor.inside;
                    if (inspected++ >= FIELD_LIMIT || !budget.getAsBoolean()) return new Search(null, true);
                    if (!Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive() || field.isSynthetic()) {
                        cursor.advance(); continue;
                    }
                    Object candidate;
                    try {
                        if (!field.trySetAccessible()) { incomplete = true; cursor.advance(); continue; }
                        candidate = field.get(null);
                    } catch (IllegalAccessException | RuntimeException unavailable) { incomplete = true; cursor.advance(); continue; }
                    if (!safeMap(candidate)) { cursor.advance(); continue; }
                    Map<?, ?> map = (Map<?, ?>) candidate;
                    Object record = map.get(subject);
                    if (record == null) { cursor.advance(); continue; }
                    if (AtomicSnapshotReference.bound(record, snapshot)) {
                        var binding = new Binding(type, field, map, subject, record, null, record, snapshot);
                        if (binding.stillBound()) { cursor.advance(); return new Search(binding, incomplete); }
                        incomplete = true; cursor.advance(); continue;
                    }
                    // An atomic holder containing a different image is not a record to introspect.
                    if (record.getClass() == AtomicReference.class) { cursor.advance(); continue; }
                    String recordLayout = ReflectionFieldLayout.hierarchyShape(record.getClass());
                    if (cursor.map.get() != map || cursor.record.get() != record || !recordLayout.equals(cursor.recordLayout)) {
                        cursor.inner = 0; cursor.map = new WeakReference<>(map); cursor.record = new WeakReference<>(record);
                        cursor.recordLayout = recordLayout;
                    }
                    cursor.inside = true;
                    int inner = 0;
                    for (Class<?> recordType = record.getClass(); recordType != null && recordType != Object.class;
                         recordType = recordType.getSuperclass()) {
                        var images = ReflectionFieldLayout.get(recordType);
                        for (int imageIndex = 0; imageIndex < images.size(); imageIndex++) {
                            Field image = images.field(imageIndex);
                            if (inner++ < cursor.inner) continue;
                            if (!innerCredit && (inspected++ >= FIELD_LIMIT || !budget.getAsBoolean())) return new Search(null, true);
                            innerCredit = false;
                            cursor.inner++;
                            if (Modifier.isStatic(image.getModifiers()) || image.getType().isPrimitive() || image.isSynthetic()) continue;
                            try {
                                if (!image.trySetAccessible()) { incomplete = true; continue; }
                                Object slot = image.get(record);
                                if (!AtomicSnapshotReference.bound(slot, snapshot)) continue;
                                var binding = new Binding(type, field, map, subject, record, image, slot, snapshot);
                                if (binding.stillBound()) return new Search(binding, incomplete);
                                incomplete = true;
                            } catch (IllegalAccessException | RuntimeException unavailable) { incomplete = true; }
                        }
                    }
                    cursor.advance();
                }
            }
            cursor.clear();
        } catch (RuntimeException | LinkageError unavailable) { cursor.clear(); incomplete = true; }
        return new Search(null, incomplete);
    }
    private static boolean safeMap(Object value) {
        return value != null && (value.getClass() == HashMap.class || value.getClass() == LinkedHashMap.class
            || value.getClass() == ConcurrentHashMap.class);
    }
}
