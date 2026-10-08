package dev.ronova.coremod.root.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/** Durable execution fact. It neither issues Judgment nor proves arbitrary external effects. */
public record EffectRecord(
    EffectKey key, String authorityPayloadHash, long authorityCommitIndex,
    String effectPayloadHash, long policyRevision, UUID planId, long planRevision,
    String stepId, UUID attemptId, SideEffectClass sideEffectClass,
    RecoveryCapability recoveryCapability, State state, long revision, String evidenceRef
) {
    public static final String PUBLIC_DEATH = "ronova:public-death/v1";
    public static final String TERMINAL_LIFECYCLE = "ronova:terminal-lifecycle/v1";
    public static final String SOURCE_QUALIFICATION = "ronova:source-qualification/v1";
    public static final String ONCE_SLOT = "once";
    public static final int MAX_EVIDENCE_BYTES = 256;
    // Upper bound on future status/evidence growth for each accepted unresolved record.
    public static final long RECOVERY_GROWTH_BYTES = 1024L;

    public EffectRecord {
        Objects.requireNonNull(key, "key");
        authorityPayloadHash = checkedHash(authorityPayloadHash);
        effectPayloadHash = checkedHash(effectPayloadHash);
        if (authorityCommitIndex < 1 || policyRevision < 0 || planRevision < 1 || revision < 1) {
            throw new IllegalArgumentException("invalid effect revisions");
        }
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(attemptId, "attemptId");
        Objects.requireNonNull(sideEffectClass, "sideEffectClass");
        Objects.requireNonNull(recoveryCapability, "recoveryCapability");
        Objects.requireNonNull(state, "state");
        stepId = boundedAscii(stepId, 128);
        evidenceRef = boundedAscii(evidenceRef, MAX_EVIDENCE_BYTES);
        if (recoveryCapability == RecoveryCapability.OPAQUE && state.confirmed()) {
            throw new IllegalArgumentException("opaque effect cannot assert confirmed completion or absence");
        }
    }

    public static EffectRecord publicDeath(EffectKey key, String authorityHash, long commitIndex,
        long policyRevision) {
        if (!PUBLIC_DEATH.equals(key.semanticOperationId()) || !ONCE_SLOT.equals(key.semanticSlotId())) {
            throw new IllegalArgumentException("unregistered public-death semantic slot");
        }
        return new EffectRecord(key, authorityHash, commitIndex, publicDeathPayloadHash(key, authorityHash),
            policyRevision, key.canonicalCommandId(), 1L, "public-death", UUID.randomUUID(),
            SideEffectClass.EXTERNAL_OPAQUE, RecoveryCapability.OPAQUE, State.PREPARED, 1L, "intent-prepared");
    }

    public static String publicDeathPayloadHash(EffectKey key, String authorityHash) {
        return hash("public-death-v1\nminecraft:out_of_world\n" + key.canonical() + "\n" + checkedHash(authorityHash));
    }

    public static EffectRecord terminalLifecycle(EffectKey key, String authorityHash, long commitIndex, long policyRevision, long planRevision) {
        if (!TERMINAL_LIFECYCLE.equals(key.semanticOperationId()) || !ONCE_SLOT.equals(key.semanticSlotId()))
            throw new IllegalArgumentException("unregistered terminal lifecycle slot");
        return new EffectRecord(key, authorityHash, commitIndex, terminalPayloadHash(key, authorityHash), policyRevision,
            key.canonicalCommandId(), planRevision, "terminal-lifecycle", UUID.randomUUID(), SideEffectClass.EXTERNAL_OPAQUE,
            RecoveryCapability.OPAQUE, State.PREPARED, 1, "terminal-intent-prepared");
    }
    public static String terminalPayloadHash(EffectKey key, String authorityHash) {
        return hash("terminal-lifecycle-v1\nprepare/effects/death/kill/discard/remove/raw-callbacks\n" + key.canonical() + "\n" + checkedHash(authorityHash));
    }

    public EffectRecord unknown(String evidence) {
        if (state.confirmed()) throw new IllegalStateException("confirmed effect cannot be rewritten as unknown");
        return new EffectRecord(key, authorityPayloadHash, authorityCommitIndex, effectPayloadHash,
            policyRevision, planId, planRevision, stepId, attemptId, sideEffectClass, recoveryCapability,
            State.EFFECT_UNKNOWN, Math.addExact(revision, 1L), evidence);
    }

    /** The confirmed effect is ONLY the Root-owned qualification, never an external record deletion. */
    public static EffectRecord sourceQualification(RecoverySourcePlan plan) {
        var terminal = plan.terminal(); var key = plan.effectKey();
        return new EffectRecord(key, terminal.authorityHash(), terminal.authorityCommitIndex(),
            sourceQualificationPayloadHash(key, terminal.authorityHash()), terminal.activePolicyRevision(),
            terminal.planId(), terminal.planRevision(), "source-qualification", UUID.randomUUID(),
            SideEffectClass.RONOVA_CONTROLLED, RecoveryCapability.IDEMPOTENT, State.EFFECT_CONFIRMED, 1,
            "root-source-qualification-only;external-disposition-unproven");
    }
    public static String sourceQualificationPayloadHash(EffectKey key, String authorityHash) {
        if (!SOURCE_QUALIFICATION.equals(key.semanticOperationId())
            || !key.semanticSlotId().matches("source/[0-9a-f]{64}"))
            throw new IllegalArgumentException("unregistered source qualification slot");
        return hash(RecoverySourcePlan.CONTRACT + "\n" + key.canonical() + "\n" + checkedHash(authorityHash));
    }

    public boolean unresolved() { return !state.confirmed(); }

    public String canonical() {
        return key.canonical() + "\n" + authorityPayloadHash + "\n" + authorityCommitIndex + "\n"
            + effectPayloadHash + "\n" + policyRevision + "\n" + planId + "\n" + planRevision + "\n"
            + stepId + "\n" + attemptId + "\n" + sideEffectClass + "\n" + recoveryCapability + "\n"
            + state + "\n" + revision + "\n" + evidenceRef;
    }

    public static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    public static String checkedHash(String value) {
        Objects.requireNonNull(value, "hash");
        if (!value.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("non-canonical SHA-256");
        return value;
    }

    private static String boundedAscii(String value, int maximum) {
        Objects.requireNonNull(value, "bounded text");
        if (value.isEmpty() || value.length() > maximum) throw new IllegalArgumentException("effect text length exceeds bound");
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) < 32 || value.charAt(i) > 126) throw new IllegalArgumentException("effect text must be bounded printable ASCII");
        }
        return value;
    }

    public enum State {
        PREPARED, EFFECT_UNKNOWN, EFFECT_CONFIRMED, NO_EFFECT_CONFIRMED;
        public boolean confirmed() { return this == EFFECT_CONFIRMED || this == NO_EFFECT_CONFIRMED; }
    }
    public enum RecoveryCapability { IDEMPOTENT, QUERYABLE, COMPENSATABLE, OPAQUE }
    public enum SideEffectClass { RONOVA_CONTROLLED, REVERSIBLE, COMPENSATABLE, EXTERNAL_OPAQUE }
}
