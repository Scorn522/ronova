package dev.ronova.coremod.root.api;

import dev.ronova.coremod.contract.SubjectKey;
import dev.ronova.coremod.surface.RuntimeSurface;
import java.util.*;

/** Derived by the existing Root writer from one retained, committed qualification receipt. */
public record TerminalOperationPlan(UUID planId, long planRevision, UUID commandId, String authorityHash,
    long authorityCommitIndex, SubjectQualification qualification, long activePolicyRevision,
    long configuredPolicyRevision, String ownerIdentity, String completionContract,
    List<Step> orderedSteps, Set<RuntimeSurface> requiredSurfaces, Set<RuntimeSurface> optionalSurfaces,
    long reservedHeapBytes, String recoveryPolicy) {
    public enum Step { ADMISSION_FENCE, SERVER_PRESENCE, SOURCE_QUALIFICATION, TERMINAL_LIFECYCLE, TERMINAL_STATE, WORLD_RECORDS, NETWORK_PROJECTION }
    public TerminalOperationPlan {
        Objects.requireNonNull(planId); Objects.requireNonNull(commandId); Objects.requireNonNull(qualification);
        authorityHash = EffectRecord.checkedHash(authorityHash);
        if (planRevision < authorityCommitIndex || !planId.equals(commandId) || authorityCommitIndex < 1 || !qualification.terminal()
            || qualification.revisions() == null || qualification.revisions().activeIncarnation() == null
            || activePolicyRevision < 1 || configuredPolicyRevision < 1 || reservedHeapBytes != 16384)
            throw new IllegalArgumentException("invalid terminal operation plan");
        orderedSteps = List.copyOf(orderedSteps); requiredSurfaces = Set.copyOf(requiredSurfaces); optionalSurfaces = Set.copyOf(optionalSurfaces);
        if (!orderedSteps.equals(List.of(Step.values()))
            || !requiredSurfaces.equals(Set.of(RuntimeSurface.REGISTRY, RuntimeSurface.TEMPORAL, RuntimeSurface.DATA))
            || !optionalSurfaces.equals(Set.of(RuntimeSurface.NETWORK, RuntimeSurface.PRESENTATION))
            || !Objects.equals(ownerIdentity, "root/" + qualification.subjectKey().realmId().canonical())
            || !Objects.equals(completionContract, "ronova:terminal-server/v4")
            || !Objects.equals(recoveryPolicy, "CONTROLLED_REASSERT_OPAQUE_NO_REPLAY"))
            throw new IllegalArgumentException("unregistered plan contract");
    }
    public static TerminalOperationPlan terminal(UUID command, String hash, long index, long planRevision,
        SubjectQualification qualification, long activePolicy, long configuredPolicy) {
        return new TerminalOperationPlan(command, planRevision, command, hash, index, qualification, activePolicy, configuredPolicy,
            "root/" + qualification.subjectKey().realmId().canonical(), "ronova:terminal-server/v4", List.of(Step.values()),
            Set.of(RuntimeSurface.REGISTRY, RuntimeSurface.TEMPORAL, RuntimeSurface.DATA),
            Set.of(RuntimeSurface.NETWORK, RuntimeSurface.PRESENTATION), 16384, "CONTROLLED_REASSERT_OPAQUE_NO_REPLAY");
    }
    public SubjectKey subjectKey() { return qualification.subjectKey(); }
    public UUID physicalNonce() { return qualification.revisions().activeIncarnation().nonce(); }
    public EffectKey effectKey() { return new EffectKey(commandId, qualification.targetEpoch(), EffectRecord.TERMINAL_LIFECYCLE, EffectRecord.ONCE_SLOT); }
    public String causalityKey() { return commandId + "/" + subjectKey().canonical() + ":" + qualification.targetEpoch().generation() + "/" + planRevision; }
}
