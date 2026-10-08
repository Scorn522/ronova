package dev.ronova.coremod.engine;

/** Shared finite-radius semantics for every entity discovery source. */
public final class SphericalRange {
    private SphericalRange() {}

    public static boolean contains(
        double centerX,
        double centerY,
        double centerZ,
        double targetX,
        double targetY,
        double targetZ,
        double radius
    ) {
        if (!Double.isFinite(centerX) || !Double.isFinite(centerY) || !Double.isFinite(centerZ)
            || !Double.isFinite(targetX) || !Double.isFinite(targetY)
            || !Double.isFinite(targetZ) || !Double.isFinite(radius) || radius < 0.0D) {
            return false;
        }
        double dx = targetX - centerX;
        double dy = targetY - centerY;
        double dz = targetZ - centerZ;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }
}
