package dev.ronova.coremod.persistence;

import java.nio.file.Path;

/**
 * Test-only bridge that exposes the package-private {@code LocalDominionStore.openWorld(Path)}
 * factory to regression tests living in other packages (for example
 * {@code dev.ronova.coremod.observation.RecoveryTaskDispositionSelfTest}).
 *
 * <p>It exists so an out-of-package test can build a real store fixture without widening the
 * production API surface: {@code openWorld} stays package-private, and nothing outside the test
 * source set can reach this class.</p>
 */
public final class LocalStoreTestAccess {
    private LocalStoreTestAccess() {}

    /** Opens the same durable world store {@code openWorld} builds, from another package. */
    public static LocalDominionStore openWorld(Path world) {
        return LocalDominionStore.openWorld(world);
    }
}
