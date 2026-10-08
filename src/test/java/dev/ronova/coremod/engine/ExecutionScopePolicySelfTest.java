package dev.ronova.coremod.engine;

/** Requested scope is a ceiling; identity requests must not widen to all entities of a type. */
public final class ExecutionScopePolicySelfTest {
    private ExecutionScopePolicySelfTest() {}

    public static void main(String[] args) {
        for (JudgmentScope requested : JudgmentScope.values()) {
            require(ExecutionScopePolicy.normalize(false, requested) == requested,
                "non-player request silently changed its scope");
            require(ExecutionScopePolicy.normalize(true, requested) == JudgmentScope.IDENTITY,
                "player cleanup must remain UUID-exact");
        }
        System.out.println("ExecutionScopePolicySelfTest: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
