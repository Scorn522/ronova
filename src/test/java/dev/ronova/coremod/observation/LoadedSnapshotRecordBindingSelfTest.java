package dev.ronova.coremod.observation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class LoadedSnapshotRecordBindingSelfTest {
    static final class Storage { Map<String,Object> cache = new HashMap<>(); }
    static final class Owner {
        Map<UUID,Object> records = new ConcurrentHashMap<>();
        @Override public boolean equals(Object value) { throw new AssertionError("owner equality invoked"); }
        @Override public int hashCode() { throw new AssertionError("owner hash invoked"); }
    }
    public static void main(String[] args) throws Exception {
        var storage = new Storage(); var owner = new Owner(); storage.cache.put("key", owner);
        var cacheField = Storage.class.getDeclaredField("cache"); cacheField.setAccessible(true);
        var mapField = Owner.class.getDeclaredField("records"); mapField.setAccessible(true);
        var id = UUID.randomUUID(); var otherId = UUID.randomUUID(); Object image = new Object(), other = new Object();
        var slot = new AtomicReference<>(image); var records = owner.records;
        records.put(id, slot); records.put(otherId, other);
        StorageBindingObservation.Read binding = () -> LoadedSnapshotRecordBinding.matches(storage, cacheField, "key", owner,
            mapField, records, id, slot, slot, image, false);
        require(binding.matches(), "direct loaded snapshot not bound");
        slot.set(new Object()); require(!binding.matches(), "snapshot replacement retained permission"); slot.set(image);
        storage.cache.put("key", new Owner()); require(!binding.matches(), "cache owner replacement retained permission");
        storage.cache.put("key", owner);
        owner.records = new ConcurrentHashMap<>(records); require(!binding.matches(), "map replacement retained permission");
        owner.records = records;
        require(!LoadedSnapshotRecordBinding.matches(storage, cacheField, "key", owner, mapField, records, id,
            slot, new AtomicReference<>(image), image, false), "nested/mixed record obtained whole-record permission");
        require(AtomicSnapshotRecord.remove(records, id, slot, () -> {
            try { return binding.matches(); } catch (ReflectiveOperationException error) { return false; }
        }) == AtomicSnapshotRecord.Result.REMOVED && records.get(otherId) == other,
            "direct record operation failed or touched an unrelated record");
        require(LoadedSnapshotRecordBinding.matches(storage, cacheField, "key", owner, mapField, records, id,
            slot, slot, image, true), "published removal was not observable in original owner");
        records.put(id, new Object());
        require(!LoadedSnapshotRecordBinding.matches(storage, cacheField, "key", owner, mapField, records, id,
            slot, slot, image, true), "replacement entry was reported absent");
        owner.records = new ConcurrentHashMap<>() {
            @Override public Object get(Object key) { throw new AssertionError("custom map called"); }
        };
        require(!LoadedSnapshotRecordBinding.matches(storage, cacheField, "key", owner, mapField, owner.records, id,
            slot, slot, image, false), "custom container obtained write binding");
        System.out.println("LoadedSnapshotRecordBindingSelfTest PASS");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
