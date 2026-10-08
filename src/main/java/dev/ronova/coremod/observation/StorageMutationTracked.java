package dev.ronova.coremod.observation;

/** Implemented by the registered DimensionDataStorage mixin. Missing instrumentation is unknown. */
public interface StorageMutationTracked {
    long ronova$cacheObservationVersion();
    /** Registered vanilla data-folder resolution only; absence never permits a guessed path. */
    default java.nio.file.Path ronova$observedDataFile(String name){return null;}
}
