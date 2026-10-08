package dev.ronova.coremod.observation;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Exact loaded-cache/owner/direct-record relationship; caller supplies the current storage observation window. */
public final class LoadedSnapshotRecordBinding {
    private LoadedSnapshotRecordBinding() {}
    public static boolean matches(Object storage, Field cacheField, String name, Object owner, Field mapField,
        Object container, UUID subject, Object expectedRecord, Object slot, Object snapshot, boolean absent)
        throws IllegalAccessException {
        if (storage == null || cacheField == null || name == null || owner == null || mapField == null
            || container == null || container.getClass() != ConcurrentHashMap.class || subject == null) return false;
        if (java.lang.reflect.Modifier.isStatic(cacheField.getModifiers())
            || java.lang.reflect.Modifier.isStatic(mapField.getModifiers())) return false;
        Object cache = cacheField.get(storage);
        if (cache == null || !(cache.getClass() == HashMap.class || cache.getClass() == LinkedHashMap.class
            || cache.getClass() == ConcurrentHashMap.class)) return false;
        if (((Map<?, ?>) cache).get(name) != owner || mapField.get(owner) != container) return false;
        var records = (ConcurrentHashMap<?, ?>) container;
        if (absent) return !records.containsKey(subject);
        return expectedRecord != null && slot == expectedRecord && records.get(subject) == expectedRecord
            && AtomicSnapshotReference.bound(slot, snapshot);
    }
}
