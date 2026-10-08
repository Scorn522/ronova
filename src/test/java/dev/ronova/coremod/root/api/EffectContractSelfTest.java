package dev.ronova.coremod.root.api;

import dev.ronova.coremod.contract.RealmId;
import dev.ronova.coremod.contract.SubjectKey;
import java.util.UUID;

public final class EffectContractSelfTest {
    public static void main(String[] args) {
        EffectKey key = new EffectKey(new UUID(0L, 7L), new TargetEpoch(new SubjectKey(
            new RealmId(new UUID(0L, 8L)), new UUID(0L, 9L)), 1L), EffectRecord.PUBLIC_DEATH, EffectRecord.ONCE_SLOT);
        EffectRecord prepared = EffectRecord.publicDeath(key, EffectRecord.hash("authority"), 1L, 0L);
        EffectRecord unknown = prepared.unknown("callback-threw-after-partial-effect");
        require(unknown.state() == EffectRecord.State.EFFECT_UNKNOWN && unknown.unresolved()
            && unknown.key().equals(prepared.key()) && unknown.attemptId().equals(prepared.attemptId()),
            "recovery changed effect identity/attempt or guessed completion");
        require(unknown.effectPayloadHash().equals(prepared.effectPayloadHash())
            && unknown.revision() == prepared.revision() + 1L, "recovery rewrote semantic payload");
        reject(() -> new EffectClaim(EffectClaim.Status.GRANTED, unknown));
        reject(() -> withState(prepared, EffectRecord.State.EFFECT_CONFIRMED));
        reject(() -> withState(prepared, EffectRecord.State.NO_EFFECT_CONFIRMED));
        reject(() -> prepared.unknown("x".repeat(257)));
        reject(() -> prepared.unknown("fake\nrecord"));
        reject(() -> EffectRecord.publicDeath(new EffectKey(key.canonicalCommandId(), key.targetEpoch(),
            EffectRecord.PUBLIC_DEATH, "random-retry-slot"), prepared.authorityPayloadHash(), 1L, 0L));
        reject(() -> EffectRecord.publicDeath(key, "not-a-hash", 1L, 0L));
        EffectRecord replanned = new EffectRecord(prepared.key(), prepared.authorityPayloadHash(), prepared.authorityCommitIndex(),
            prepared.effectPayloadHash(), prepared.policyRevision(), UUID.randomUUID(), 2L, "different-step",
            prepared.attemptId(), prepared.sideEffectClass(), prepared.recoveryCapability(), prepared.state(), 2L, "replan-metadata");
        require(replanned.key().equals(prepared.key()) && replanned.effectPayloadHash().equals(prepared.effectPayloadHash()),
            "plan metadata escaped semantic effect deduplication");
        System.out.println("EffectContractSelfTest: PASS (opaque unknown, no fabricated confirmation, stable key/attempt, bounded evidence, slot contract)");
    }
    private static EffectRecord withState(EffectRecord r, EffectRecord.State state) {
        return new EffectRecord(r.key(), r.authorityPayloadHash(), r.authorityCommitIndex(), r.effectPayloadHash(),
            r.policyRevision(), r.planId(), r.planRevision(), r.stepId(), r.attemptId(), r.sideEffectClass(),
            r.recoveryCapability(), state, r.revision()+1L, "returned-normally");
    }
    private static void reject(Runnable body) {
        try { body.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("unsafe effect contract was accepted");
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
