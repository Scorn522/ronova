package dev.ronova.coremod.root.api;

public final class StorageCapacitySelfTest {
    public static void main(String[] args) {
        long limit=64L*1024L*1024L, payload=2L*1024L*1024L, growth=1024L;
        StorageCapacity normal=StorageCapacity.evaluate(limit,44L,payload,growth,payload,1L<<30);
        require(normal.permits(true) && normal.requiredFreeBytes() > payload*2L+growth,
            "new write did not budget replacement, future receipt and recovery reserve");
        require(!StorageCapacity.evaluate(limit,44L,payload,growth,payload,normal.requiredFreeBytes()-1).permits(true),
            "insufficient free space was accepted");
        require(StorageCapacity.evaluate(limit,44L,payload,growth,payload,normal.requiredFreeBytes()).permits(true),
            "exact free-space boundary was rejected");
        StorageCapacity format=StorageCapacity.evaluate(limit,44L,limit-1024L,1025L,0L,1L<<30);
        require(format.state()==StorageCapacity.State.FORMAT_LIMIT && !format.permits(false),
            "reserved future record growth exceeded readable format");
        require(!StorageCapacity.evaluate(limit,44L,payload,growth,payload,-1L).permits(false),
            "unobserved storage was treated as unlimited");
        StorageCapacity soft=StorageCapacity.evaluate(limit,44L,payload,growth,StorageCapacity.SOFT_QUOTA_BYTES,1L<<30);
        require(!soft.permits(true) && soft.permits(false), "soft quota did not distinguish new acceptance from recovery");
        StorageCapacity hard=StorageCapacity.evaluate(limit,44L,payload,growth,StorageCapacity.HARD_QUOTA_BYTES,1L<<30);
        require(hard.state()==StorageCapacity.State.HARD_QUOTA_LIMIT && !hard.permits(false), "recovery ignored owned hard quota");
        StorageCapacity reused=StorageCapacity.evaluate(limit,44L,limit,0L,2L*(limit+44L),limit+44L,1L<<30);
        require(reused.permits(false) && reused.peakOwnedBytes()==3L*(limit+44L),
            "recovery double-charged a reusable staging file and deadlocked at the readable format limit");
        System.out.println("StorageCapacitySelfTest: PASS (format growth reserve, replacement/free-space bounds, unknown capacity, recovery priority, hard quota)");
    }
    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
}
