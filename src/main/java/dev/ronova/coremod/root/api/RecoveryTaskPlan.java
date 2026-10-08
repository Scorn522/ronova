package dev.ronova.coremod.root.api;

import java.util.Objects;

/** Conditional task-state action. Queue references, actual exit and descendants are separate obligations. */
public record RecoveryTaskPlan(RecoverySnapshotPlan source,RecoveryWorkDescriptor descriptor) {
    public RecoveryTaskPlan {
        Objects.requireNonNull(source);Objects.requireNonNull(descriptor);
        if(descriptor.kind()!=RecoveryWorkDescriptor.Kind.TASK_CANCEL||!matches(source,descriptor))throw new IllegalArgumentException("task plan source mismatch");
    }
    static boolean matches(RecoverySnapshotPlan source,RecoveryWorkDescriptor description) {
        return source.sourceLocator().equals(description.location().locatorHash())&&source.storageScope()==description.location().scope()
            &&source.sourceVersion()==description.sourceVersion()&&source.copyDepth()==description.copyDepth();
    }
    public EffectKey effectKey(){return descriptor.effectKey(source.terminal());}
}
