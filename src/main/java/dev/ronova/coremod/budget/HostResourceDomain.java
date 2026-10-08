package dev.ronova.coremod.budget;

/** Shared safety inventory; observed host pressure is not attribution to an actor. */
public enum HostResourceDomain {
    JAVA_HEAP(Unit.BYTES), DIRECT_BUFFER(Unit.BYTES), MAPPED_BUFFER(Unit.BYTES),
    METASPACE(Unit.BYTES), CODE_CACHE(Unit.BYTES), THREAD(Unit.COUNT), THREAD_STACK(Unit.BYTES),
    CLASS(Unit.COUNT), CLASSLOADER(Unit.COUNT), JNI_JNA_NATIVE(Unit.BYTES), JVMTI_OWNED(Unit.BYTES),
    FILE_DESCRIPTOR(Unit.COUNT), PLATFORM_HANDLE(Unit.COUNT), GPU_UPLOAD(Unit.BYTES), RENDER_BUFFER(Unit.BYTES);
    public enum Unit { BYTES, COUNT }
    private final Unit unit;
    HostResourceDomain(Unit unit) { this.unit = unit; }
    public Unit unit() { return unit; }
}
