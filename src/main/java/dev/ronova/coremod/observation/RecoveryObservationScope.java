package dev.ronova.coremod.observation;

import dev.ronova.coremod.agent.BootstrapHooks;

/** The copy hook observes the currently executing server lane, never a global Realm guess. */
public final class RecoveryObservationScope implements AutoCloseable {
    private static final ThreadLocal<RecoveryObservationScope> CURRENT = new ThreadLocal<>();
    private final Thread thread = Thread.currentThread();
    private final RecoveryObservationScope previous;
    private final java.lang.ref.WeakReference<RecoverySourceTracker> installed;
    private final java.lang.ref.WeakReference<net.minecraft.server.MinecraftServer> server;
    private boolean closed;

    public static void installHook() {
        RecoveryTaskObserver.install();
        BootstrapHooks.installSerializedSpawnPredicate((level, tag) -> {
            if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)
                || !(tag instanceof net.minecraft.nbt.CompoundTag compound)) return false;
            if (serverLevel.getServer().getLevel(serverLevel.dimension()) != serverLevel) return false;
            var runtime = dev.ronova.coremod.bootstrap.ServerRuntimeRegistry.getIfPresent(serverLevel.getServer());
            return runtime != null && runtime.spawnAdmission().rejectNbt(serverLevel, compound);
        });
        BootstrapHooks.installNbtCopyObserver((source, result) -> {
            RecoveryObservationScope scope = CURRENT.get();
            RecoverySourceTracker tracker = scope == null ? null : scope.installed.get();
            if (tracker != null) tracker.copied(source, result, scope.server.get());
            else RecoveryTaskObserver.copied(source,result);
        });
    }
    public RecoveryObservationScope(RecoverySourceTracker tracker, net.minecraft.server.MinecraftServer server) {
        installed = new java.lang.ref.WeakReference<>(java.util.Objects.requireNonNull(tracker));
        this.server = new java.lang.ref.WeakReference<>(java.util.Objects.requireNonNull(server));
        if (!server.isSameThread()) throw new IllegalStateException("recovery scope requires its server lane");
        previous = CURRENT.get(); CURRENT.set(this);
    }
    @Override public void close() {
        if (closed) return;
        if (Thread.currentThread() != thread || CURRENT.get() != this)
            throw new IllegalStateException("recovery scope closed outside its owning lane");
        closed = true;
        if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
    }
    public boolean ownedByCurrentThread() { return Thread.currentThread() == thread; }
    static RecoveryTaskObserver taskObserver() {
        RecoveryObservationScope scope=CURRENT.get();
        RecoverySourceTracker tracker=scope==null?null:scope.installed.get();
        return tracker==null?null:tracker.taskObserver();
    }
}
