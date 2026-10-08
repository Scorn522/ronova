package dev.ronova.coremod.observation;

import dev.ronova.coremod.root.api.*;
import java.util.Objects;
import java.util.function.Function;

/** Query one acknowledged original intent. No new intent, write permission or completion fact is created. */
public final class RecoveryStorageReadbackQuery {
    private RecoveryStorageReadbackQuery() {}
    public static RecoveryFileReadback.Result query(EffectKey key,
        Function<EffectKey,RecoveryWorkResult> lookup,
        Function<RecoveryWorkDescriptor,RecoveryFileReadback.Result> readCurrentBinding) {
        Objects.requireNonNull(lookup);Objects.requireNonNull(readCurrentBinding);
        if(key==null)return unavailable();
        var original=lookup.apply(key);
        if(original==null||!key.equals(original.record().key())
            ||original.descriptor().kind()!=RecoveryWorkDescriptor.Kind.STORAGE_COMMIT)return unavailable();
        var observed=readCurrentBinding.apply(original.descriptor());
        // ACK loss, replacement of the original record, or a concurrent result update requires a fresh query.
        if(observed==null||!original.equals(lookup.apply(key)))return unavailable();
        if(observed.status()==RecoveryFileReadback.Status.OBSERVED_MATCH
            &&(!original.descriptor().expectedHash().equals(observed.observedHash())
                ||observed.reason()!=RecoveryFileReadback.Reason.NONE||observed.bytes()<0
                ||observed.fileIdentity()==null||observed.fileIdentity().isEmpty()
                ||!original.descriptor().fileIdentity().isEmpty()&&!original.descriptor().fileIdentity().equals(observed.fileIdentity())))return unavailable();
        return observed;
    }
    private static RecoveryFileReadback.Result unavailable() {
        return new RecoveryFileReadback.Result(RecoveryFileReadback.Status.UNAVAILABLE,
            RecoveryFileReadback.Reason.PRECONDITION_UNPROVEN,0,null);
    }
}
