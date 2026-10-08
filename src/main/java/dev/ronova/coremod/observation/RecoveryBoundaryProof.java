package dev.ronova.coremod.observation;

import dev.ronova.coremod.agent.BootstrapHooks;
import dev.ronova.coremod.engine.spawn.SpawnAdmissionRuntime;
import java.lang.StackWalker.StackFrame;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;

/** Actual registered caller frames; a direct call to the public observer is not a boundary event. */
final class RecoveryBoundaryProof {
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static volatile String reconstructionFrames="";
    private RecoveryBoundaryProof() {}
    static boolean copy() {
        List<StackFrame> frames = WALKER.walk(stream -> stream.limit(7).toList());
        return copyFrames(frames);
    }
    /** Classes on the actual copy stack, never a supplied class name or global loader scan. */
    static List<Class<?>> copyCallers() {
        List<StackFrame> frames = WALKER.walk(stream -> stream.limit(16).toList());
        if (!copyFrames(frames)) return List.of();
        return frames.stream().skip(5).<Class<?>>map(StackFrame::getDeclaringClass).distinct().limit(6).toList();
    }
    static List<Class<?>> asyncCopyCallers() {
        List<StackFrame> frames=WALKER.walk(stream->stream.limit(16).toList());
        if(frames.size()<5||!frame(frames.get(1),RecoveryTaskObserver.class,"copied")||!callback(frames.get(2))
            ||!frame(frames.get(3),BootstrapHooks.class,"observeNbtCopy")||frames.get(4).getDeclaringClass()!=CompoundTag.class
            ||!Set.of("copy","m_6426_").contains(frames.get(4).getMethodName()))return List.of();
        return frames.stream().skip(5).<Class<?>>map(StackFrame::getDeclaringClass).distinct().limit(6).toList();
    }
    private static boolean copyFrames(List<StackFrame> frames) {
        return frames.size() >= 5 && frame(frames.get(1), RecoverySourceTracker.class, "copied")
            && callback(frames.get(2)) && frame(frames.get(3), BootstrapHooks.class, "observeNbtCopy")
            && frames.get(4).getDeclaringClass() == CompoundTag.class && Set.of("copy", "m_6426_").contains(frames.get(4).getMethodName());
    }
    static boolean reconstruction() {
        List<StackFrame> frames = WALKER.walk(stream -> stream.limit(8).toList());
        if(Boolean.getBoolean("ronova.validation.asyncCopyObserver"))reconstructionFrames=frames.stream()
            .map(frame->frame.getClassName()+"."+frame.getMethodName()).collect(java.util.stream.Collectors.joining(" -> "));
        return frames.size() >= 6 && frame(frames.get(1), RecoverySourceTracker.class, "reconstructing")
            && frame(frames.get(2), SpawnAdmissionRuntime.class, "rejectNbt") && callback(frames.get(3))
            && frame(frames.get(4), BootstrapHooks.class, "rejectSerializedSpawn")
            && frames.get(5).getDeclaringClass() == EntityType.class && Set.of("create", "m_20642_").contains(frames.get(5).getMethodName());
    }
    static String reconstructionFrames(){return reconstructionFrames;}
    private static boolean frame(StackFrame frame, Class<?> owner, String name) {
        return frame.getDeclaringClass() == owner && frame.getMethodName().equals(name);
    }
    private static boolean callback(StackFrame frame) {
        return frame.getDeclaringClass() == RecoveryObservationScope.class && frame.getMethodName().startsWith("lambda$installHook$");
    }
}
