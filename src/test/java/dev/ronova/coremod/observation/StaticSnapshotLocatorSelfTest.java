package dev.ronova.coremod.observation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class StaticSnapshotLocatorSelfTest {
    private static final class Direct { private static Map<UUID,Object> records = new HashMap<>(); }
    private static final class Atomic { private static Map<UUID,Object> records = new ConcurrentHashMap<>(); }
    private static final class Holder { private static Map<UUID,Object> records = new LinkedHashMap<>(); }
    private static class Parent { private static Map<UUID,Object> records = new HashMap<>(); }
    private static final class Child extends Parent {}
    private static final class InstanceOnly { private final Map<UUID,Object> records = new HashMap<>(); }
    private static final class Wide {
        private static int a00,a01,a02,a03,a04,a05,a06,a07,a08,a09,a10,a11,a12,a13,a14,a15,
            a16,a17,a18,a19,a20,a21,a22,a23,a24,a25,a26,a27,a28,a29,a30,a31,
            a32,a33,a34,a35,a36,a37,a38,a39,a40,a41,a42,a43,a44,a45,a46,a47,
            a48,a49,a50,a51,a52,a53,a54,a55,a56,a57,a58,a59,a60,a61,a62,a63;
        private static Map<UUID,Object> zRecords = new HashMap<>();
    }
    private static final class WideRecord {
        private int a00,a01,a02,a03,a04,a05,a06,a07,a08,a09,a10,a11,a12,a13,a14,a15,
            a16,a17,a18,a19,a20,a21,a22,a23,a24,a25,a26,a27,a28,a29,a30,a31,
            a32,a33,a34,a35,a36,a37,a38,a39,a40,a41,a42,a43,a44,a45,a46,a47,
            a48,a49,a50,a51,a52,a53,a54,a55,a56,a57,a58,a59,a60,a61,a62,a63;
        private final Object zSnapshot;
        WideRecord(Object snapshot) { zSnapshot = snapshot; }
    }
    private static final class Record {
        private Object snapshot;
        Record(Object snapshot) { this.snapshot = snapshot; }
        public Object getSnapshot() { throw new AssertionError("record getter invoked"); }
        @Override public boolean equals(Object other) { throw new AssertionError("record equality invoked"); }
        @Override public int hashCode() { throw new AssertionError("record hash invoked"); }
    }
    private static final class MapOverride extends HashMap<UUID,Object> {
        @Override public Object get(Object key) { throw new AssertionError("custom map getter invoked"); }
    }
    public static void main(String[] args) {
        UUID subject = UUID.randomUUID();
        Object seed = new String("snapshot"), equalSeed = new String("snapshot");
        try {
            bindingReadStatus(subject, seed);
            Wide.zRecords.put(subject, seed);
            var scan = new StaticSnapshotLocator.Cursor();
            var first = StaticSnapshotLocator.find(Wide.class, subject, seed, () -> true, scan);
            require(first.binding() == null && first.incomplete(), "wide source did not retain its unfinished tail");
            var tail = StaticSnapshotLocator.find(Wide.class, subject, seed, () -> true, scan);
            require(tail.binding() != null && tail.binding().stillBound(), "source beyond 64 fields was never reached");
            Holder.records.put(subject, new WideRecord(seed));
            scan.clear();
            StaticSnapshotLocator.Search resumed = null;
            for (int i = 0; i < 70; i++) {
                var one = new AtomicInteger(1);
                resumed = StaticSnapshotLocator.find(Holder.class, subject, seed, () -> one.getAndDecrement() > 0, scan);
                if (resumed.binding() != null) break;
            }
            require(resumed != null && resumed.binding() != null, "one-field slices starved nested record discovery");
            scan.clear();
            var one = new AtomicInteger(1);
            StaticSnapshotLocator.find(Holder.class, subject, seed, () -> one.getAndDecrement() > 0, scan);
            require(StaticSnapshotLocator.find(Holder.class, subject, seed, () -> false, scan).binding() == null,
                "resume ignored a denied budget");
            Holder.records.put(subject, new Record(equalSeed));
            require(StaticSnapshotLocator.find(Holder.class, subject, seed, () -> true, scan).binding() == null,
                "replacement record inherited a historical image witness");
            require(StaticSnapshotLocator.find(Holder.class, subject, equalSeed, () -> true, scan).binding() != null,
                "replacement record could not be freshly observed");
            Direct.records.put(subject, seed);
            var direct = StaticSnapshotLocator.find(Direct.class, subject, seed, () -> true).binding();
            require(direct != null && direct.stillBound(), "direct static snapshot was not bound");
            var weakDirect = direct.weaken();
            require(weakDirect.stillBound(), "weak source could not resolve the current static field");
            require(StaticSnapshotLocator.find(Direct.class, UUID.randomUUID(), seed, () -> true).binding() == null,
                "different subject inherited a static record");
            require(StaticSnapshotLocator.find(Direct.class, subject, equalSeed, () -> true).binding() == null,
                "equal snapshot content created a relation");
            Direct.records = new HashMap<>(Direct.records);
            require(!direct.stillBound(), "replaced map retained the old binding");
            require(!weakDirect.stillBound(), "weak source accepted a replacement static map");

            var slot = new AtomicReference<>(seed);
            Atomic.records.put(subject, slot);
            var atomic = StaticSnapshotLocator.find(Atomic.class, subject, seed, () -> true).binding();
            require(atomic != null && atomic.stillBound(), "direct atomic snapshot was not bound");
            var entryBinding = atomic.weaken();
            UUID unrelated = UUID.randomUUID(); Object otherEntry = new Object(); Atomic.records.put(unrelated, otherEntry);
            require(AtomicSnapshotRecord.remove(Atomic.records, subject, slot, () -> false) == AtomicSnapshotRecord.Result.FENCED
                && Atomic.records.get(subject) == slot, "denied record action removed its entry");
            require(AtomicSnapshotRecord.remove(Atomic.records, subject, slot, entryBinding::stillBound)
                == AtomicSnapshotRecord.Result.REMOVED && entryBinding.recordAbsent()
                && Atomic.records.get(unrelated) == otherEntry && slot.get() == seed,
                "direct record removal changed another entry or mutated the retained snapshot");
            require(AtomicSnapshotRecord.remove(Atomic.records, subject, slot, () -> true) == AtomicSnapshotRecord.Result.NOT_CURRENT,
                "missing record was reported as a second removal");
            Object replacedRecord = new Record(seed); Atomic.records.put(subject, replacedRecord);
            require(AtomicSnapshotRecord.remove(Atomic.records, subject, new Record(seed),
                () -> { throw new AssertionError("replacement reached the write gate"); }) == AtomicSnapshotRecord.Result.NOT_CURRENT
                && Atomic.records.get(subject) == replacedRecord && !entryBinding.recordAbsent(),
                "record identity check dispatched equals or removed a replacement");
            require(AtomicSnapshotRecord.remove(new HashMap<>(), subject, slot, () -> true)
                == AtomicSnapshotRecord.Result.UNSUPPORTED, "non-atomic map accepted record mutation");
            require(AtomicSnapshotRecord.remove(new ConcurrentHashMap<UUID,Object>() {}, subject, slot, () -> true)
                == AtomicSnapshotRecord.Result.UNSUPPORTED, "custom map accepted record mutation");
            Atomic.records.put(subject, slot);
            slot.set(equalSeed);
            require(!atomic.stillBound(), "replaced atomic image retained the old binding");
            slot.set(seed);
            Atomic.records.put(subject, new AtomicReference<>(seed));
            require(!atomic.stillBound(), "replacement atomic holder retained the old binding");
            var writable = StaticSnapshotLocator.find(Atomic.class, subject, seed, () -> true).binding();
            var weakWritable = writable.weaken();
            Object writableSlot = Atomic.records.get(subject);
            require(AtomicSnapshotReference.clear(writableSlot, seed, () -> false) == AtomicSnapshotReference.ClearResult.FENCED
                && AtomicSnapshotReference.bound(writableSlot, seed), "fenced snapshot write changed the image");
            require(AtomicSnapshotReference.clear(writableSlot, seed, weakWritable::stillBound) == AtomicSnapshotReference.ClearResult.CLEARED
                && weakWritable.emptyAtomicSlot() && !weakWritable.stillBound(), "exact snapshot cell was not cleared and observed");
            require(AtomicSnapshotReference.clear(writableSlot, seed, () -> true) == AtomicSnapshotReference.ClearResult.NOT_CURRENT,
                "retry reported a second snapshot mutation");
            var raced = new AtomicReference<>(seed);
            require(AtomicSnapshotReference.clear(raced, seed, () -> { raced.set(equalSeed); return true; })
                == AtomicSnapshotReference.ClearResult.NOT_CURRENT && raced.get() == equalSeed,
                "CAS cleared a replacement image installed after the gate");
            require(AtomicSnapshotReference.clear(new AtomicReference<Object>(seed) {}, seed, () -> true)
                == AtomicSnapshotReference.ClearResult.UNSUPPORTED, "unregistered atomic subclass was mutated");

            Record record = new Record(new AtomicReference<>(seed));
            Holder.records.put(subject, record);
            var nested = StaticSnapshotLocator.find(Holder.class, subject, seed, () -> true).binding();
            require(nested != null && nested.snapshotField() != null && nested.stillBound(),
                "record field was not read by exact reference");
            var weakNested = nested.weaken();
            require(weakNested.stillBound(), "weak source could not resolve the current record field");
            record.snapshot = new AtomicReference<>(seed);
            require(!nested.stillBound(), "replacement snapshot field retained the old binding");
            require(!weakNested.stillBound(), "weak source accepted a replacement snapshot slot");
            var refreshed = StaticSnapshotLocator.find(Holder.class, subject, seed, () -> true).binding();
            Holder.records.put(subject, new Record(seed));
            require(refreshed != null && !refreshed.stillBound(), "replacement record retained the old binding");

            Parent.records.put(subject, seed);
            var inherited = StaticSnapshotLocator.find(Child.class, subject, seed, () -> true).binding();
            require(inherited != null && inherited.owner() == Parent.class && inherited.stillBound(),
                "inherited static storage was not bound to its declaring class");
            var instance = new InstanceOnly(); instance.records.put(subject, seed);
            require(StaticSnapshotLocator.find(InstanceOnly.class, subject, seed, () -> true).binding() == null,
                "an instance field was treated as static storage");

            var custom = new MapOverride(); custom.put(subject, seed); Direct.records = custom;
            require(StaticSnapshotLocator.find(Direct.class, subject, seed, () -> true).binding() == null,
                "custom map implementation was accepted");
            Direct.records = new HashMap<>(); Direct.records.put(subject, seed);
            AtomicInteger attempted = new AtomicInteger();
            var denied = StaticSnapshotLocator.find(Direct.class, subject, seed, () -> { attempted.incrementAndGet(); return false; });
            require(denied.binding() == null && denied.incomplete() && attempted.get() == 1,
                "exhausted budget produced a binding or continued inspection");

            Holder.records = new HashMap<>(); Holder.records.put(subject, new Record(seed));
            AtomicInteger allowance = new AtomicInteger(1);
            var partial = StaticSnapshotLocator.find(Holder.class, subject, seed, () -> allowance.getAndDecrement() > 0);
            require(partial.binding() == null && partial.incomplete(), "unread record field produced a complete binding");
            AtomicInteger steps = new AtomicInteger();
            var replaced = StaticSnapshotLocator.find(Holder.class, subject, seed, () -> {
                if (steps.incrementAndGet() == 2) Holder.records = new HashMap<>(Holder.records);
                return true;
            });
            require(replaced.binding() == null && replaced.incomplete(), "map replacement during discovery was not rechecked");
        } finally {
            Direct.records = new HashMap<>(); Atomic.records.clear(); Holder.records.clear(); Parent.records.clear(); Wide.zRecords.clear();
        }
        System.out.println("StaticSnapshotLocatorSelfTest PASS");
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void bindingReadStatus(UUID subject, Object seed) {
        try {
            Direct.records = new HashMap<>(); Direct.records.put(subject, seed);
            var field = Direct.class.getDeclaredField("records");
            var binding = new StaticSnapshotLocator.Binding(Direct.class, field, Direct.records, subject,
                seed, null, seed, seed).weaken();
            require(binding.bindingObservation() == StorageBindingObservation.BOUND, "stable static binding unavailable");
            Direct.records.put(subject, new Object());
            require(binding.bindingObservation() == StorageBindingObservation.DETACHED, "replaced static entry retained its binding");
            var unavailable = new StaticSnapshotLocator.Binding(String.class, field, Direct.records, subject,
                seed, null, seed, seed).weaken();
            require(unavailable.bindingObservation() == StorageBindingObservation.UNAVAILABLE
                && !unavailable.stillBound(), "missing static metadata was classified as detached or writable");
            var custom = new StaticSnapshotLocator.Binding(Direct.class, field, new MapOverride(), subject,
                seed, null, seed, seed).weaken();
            require(custom.bindingObservation() == StorageBindingObservation.UNAVAILABLE,
                "unsupported container invoked foreign callbacks or proved source removal");
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
}
