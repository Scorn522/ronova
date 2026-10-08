package dev.ronova.coremod.root.api;

import java.util.Objects;

/** Conditional registered storage commit. The descriptor binds baseline, expected image and the supported adapter. */
public record RecoveryStoragePlan(RecoverySnapshotPlan source,RecoveryWorkDescriptor descriptor) {
    public RecoveryStoragePlan {
        Objects.requireNonNull(source);Objects.requireNonNull(descriptor);
        if(descriptor.kind()!=RecoveryWorkDescriptor.Kind.STORAGE_COMMIT||!RecoveryTaskPlan.matches(source,descriptor))throw new IllegalArgumentException("storage plan source mismatch");
    }
    public EffectKey effectKey(){return descriptor.effectKey(source.terminal());}
}
