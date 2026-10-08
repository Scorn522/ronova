package dev.ronova.coremod.root.api;

/** Measured/preflighted headroom, not exclusive ownership of filesystem free space. */
public record StorageCapacity(State state, long payloadBytes, long reservedGrowthBytes,
    long ownedBytes, long usableBytes, long requiredFreeBytes, long peakOwnedBytes) {
    public static final long SOFT_QUOTA_BYTES = 192L * 1024L * 1024L;
    public static final long HARD_QUOTA_BYTES = 256L * 1024L * 1024L;
    public static final long MIN_RECOVERY_FREE_BYTES = 8L * 1024L * 1024L;

    public static StorageCapacity evaluate(long formatLimit, long headerBytes, long payload,
        long growth, long owned, long usable) {
        return evaluate(formatLimit, headerBytes, payload, growth, owned, 0L, usable);
    }

    public static StorageCapacity evaluate(long formatLimit, long headerBytes, long payload,
        long growth, long owned, long reusableStagingBytes, long usable) {
        if (formatLimit <= 0 || headerBytes < 0 || payload < 0 || growth < 0 || owned < 0) {
            throw new IllegalArgumentException("invalid storage budget input");
        }
        if (reusableStagingBytes < 0L || reusableStagingBytes > owned) throw new IllegalArgumentException("invalid staging accounting");
        if (payload > formatLimit || growth > formatLimit - payload) {
            return new StorageCapacity(State.FORMAT_LIMIT, payload, growth, owned, usable, 0L, 0L);
        }
        long next = Math.addExact(headerBytes, payload);
        long recoveryImage = Math.addExact(next, growth);
        long free = Math.addExact(Math.addExact(Math.max(0L, next - reusableStagingBytes), recoveryImage), MIN_RECOVERY_FREE_BYTES);
        long peak = Math.addExact(owned - reusableStagingBytes, Math.addExact(Math.max(next, reusableStagingBytes), recoveryImage));
        State state = usable < 0L ? State.UNOBSERVED : usable < free ? State.FREE_SPACE_LIMIT
            : peak > HARD_QUOTA_BYTES ? State.HARD_QUOTA_LIMIT
            : peak > SOFT_QUOTA_BYTES ? State.SOFT_QUOTA_LIMIT : State.AVAILABLE;
        return new StorageCapacity(state, payload, growth, owned, usable, free, peak);
    }

    public boolean permits(boolean acceptingNew) {
        return state == State.AVAILABLE || !acceptingNew && state == State.SOFT_QUOTA_LIMIT;
    }

    public enum State { AVAILABLE, SOFT_QUOTA_LIMIT, HARD_QUOTA_LIMIT, FREE_SPACE_LIMIT, FORMAT_LIMIT, ENCODING_REJECTED, UNOBSERVED }
}
