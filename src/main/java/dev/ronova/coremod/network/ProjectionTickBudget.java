package dev.ronova.coremod.network;

/** Transport attempts consume the same allowance for the whole tick, including repeated pump calls. */
public final class ProjectionTickBudget {
    private final int maximumParts;
    private final long maximumBytes;
    private long tick = Long.MIN_VALUE, bytes;
    private int parts;
    public ProjectionTickBudget(int maximumParts, long maximumBytes) {
        if (maximumParts < 1 || maximumBytes < 1) throw new IllegalArgumentException("invalid projection tick budget");
        this.maximumParts = maximumParts; this.maximumBytes = maximumBytes;
    }
    public void begin(long now) {
        if (now != tick) { tick = now; bytes = 0; parts = 0; }
    }
    public boolean fits(long cost) { return cost > 0 && parts < maximumParts && cost <= maximumBytes - bytes; }
    public boolean exhausted() { return parts >= maximumParts || bytes >= maximumBytes; }
    public void charge(long cost) {
        if (!fits(cost)) throw new IllegalStateException("projection tick allowance exhausted");
        parts++; bytes += cost;
    }
    public int parts() { return parts; }
    public long bytes() { return bytes; }
}
