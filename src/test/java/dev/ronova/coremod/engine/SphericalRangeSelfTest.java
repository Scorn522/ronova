package dev.ronova.coremod.engine;

/** Ensures normal queries and topology recovery share one exact 64-block sphere. */
public final class SphericalRangeSelfTest {
    private SphericalRangeSelfTest() {}

    public static void main(String[] args) {
        require(SphericalRange.contains(0, 0, 0, 64, 0, 0, 64),
            "the sphere boundary must be inclusive");
        require(!SphericalRange.contains(0, 0, 0, 64, 0, 1, 64),
            "a point outside the sphere must not pass through the AABB corner");
        require(SphericalRange.contains(-8, 12, 4, -8, 12, 4, 0),
            "zero radius must contain only the center");
        require(!SphericalRange.contains(0, 0, 0, 0, 0, 0, -1),
            "negative radii must be rejected");
        require(!SphericalRange.contains(0, 0, 0, Double.NaN, 0, 0, 64),
            "non-finite coordinates must be rejected");
        System.out.println("SphericalRangeSelfTest: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
