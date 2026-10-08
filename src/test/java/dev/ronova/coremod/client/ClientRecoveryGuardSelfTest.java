package dev.ronova.coremod.client;

/** Regression check for the setScreen -> guard -> repair recursion that crashed alpha.6. */
public final class ClientRecoveryGuardSelfTest {
    private ClientRecoveryGuardSelfTest() {}

    public static void main(String[] args) {
        ClientRecoveryGuard guard = new ClientRecoveryGuard();
        require(guard.enterRepair(), "first repair must enter");
        require(!guard.enterRepair(), "nested repair must be rejected");
        guard.exitRepair();
        require(guard.enterRepair(), "repair must reopen after exit");
        guard.exitRepair();

        require(guard.enterSurfaceMutation(), "first internal UI mutation must enter");
        require(guard.isInternalSurfaceMutation(), "screen boundary must see internal mutation");
        require(!guard.enterSurfaceMutation(), "nested UI mutation must not recurse");
        guard.exitSurfaceMutation();
        require(!guard.isInternalSurfaceMutation(), "UI mutation marker must clear");
        System.out.println("ClientRecoveryGuardSelfTest: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
