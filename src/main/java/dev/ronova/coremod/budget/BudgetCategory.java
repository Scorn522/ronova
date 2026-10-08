package dev.ronova.coremod.budget;

/** Stable 7.14D budget category identifiers and initial per-tick envelope. */
public enum BudgetCategory {
    TERMINAL("terminal", 500L, 256),
    CAPABILITY("capability", 400L, 64),
    DISCOVERY("discovery", 300L, Integer.MAX_VALUE),
    ADMISSION("admission", 400L, 128),
    OBSERVATION("observation", 200L, 256),
    MAINTENANCE("maintenance", 200L, 64);

    private final String id;
    private final long defaultBudgetUs;
    private final int defaultCount;

    BudgetCategory(String id, long defaultBudgetUs, int defaultCount) {
        this.id = id;
        this.defaultBudgetUs = defaultBudgetUs;
        this.defaultCount = defaultCount;
    }

    public String id() {
        return id;
    }

    public long defaultBudgetUs() {
        return defaultBudgetUs;
    }

    public int defaultCount() {
        return defaultCount;
    }
}
