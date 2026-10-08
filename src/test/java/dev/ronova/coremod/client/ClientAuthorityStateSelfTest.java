package dev.ronova.coremod.client;

/** Deterministic regression check for lag-independent client recovery timing. */
public final class ClientAuthorityStateSelfTest {
    private ClientAuthorityStateSelfTest() {}

    public static void main(String[] args) {
        long[] now = {1_000L};
        ClientAuthorityState state = new ClientAuthorityState(() -> now[0]);
        require(!state.isActive(), "fresh state must be inactive");
        state.arm();
        require(state.isActive(), "arm must activate recovery immediately");
        now[0] += 4_000_000_000L;
        state.arm();
        now[0] += 4_000_000_000L;
        require(state.isActive(), "a repeated violation must extend the window");
        now[0] += 2_000_000_000L;
        require(!state.isActive(), "recovery must expire by monotonic time without ticks");
        state.arm();
        state.reset();
        require(!state.isActive(), "session reset must clear recovery");

        now[0] = Long.MIN_VALUE + 1_000L;
        state.arm();
        require(state.isActive(), "a negative nanoTime origin must still activate recovery");
        now[0] += 5_000_000_001L;
        require(!state.isActive(), "negative-origin recovery must expire normally");
        System.out.println("ClientAuthorityStateSelfTest: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
