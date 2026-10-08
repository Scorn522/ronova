package dev.ronova.coremod.engine;

/** Single strength policy shared by every weapon action and target source. */
public final class ExecutionScopePolicy {
    private ExecutionScopePolicy() {}

    public static JudgmentScope normalize(boolean playerTarget, JudgmentScope requested) {
        return playerTarget ? JudgmentScope.IDENTITY : java.util.Objects.requireNonNull(requested, "requested");
    }
}
