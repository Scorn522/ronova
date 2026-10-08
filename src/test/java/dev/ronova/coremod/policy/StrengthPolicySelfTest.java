package dev.ronova.coremod.policy;

import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.contract.SubjectIdentity;
import dev.ronova.coremod.extension.VersionedExtensionEnvelope;
import dev.ronova.coremod.extension.VersionedExtensionId;
import dev.ronova.coremod.extension.VersionedExtensionRegistry;
import dev.ronova.coremod.root.api.CommitReceipt;
import dev.ronova.coremod.root.api.CommittedJudgmentGate;
import dev.ronova.coremod.root.api.DominionCommand;
import dev.ronova.coremod.root.api.JudgmentBatch;
import dev.ronova.coremod.root.api.TargetEpoch;
import dev.ronova.coremod.root.api.TargetEpochExtension;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Loader-neutral 7.16 policy, extension and qualification receipt contracts. */
public final class StrengthPolicySelfTest {
    private StrengthPolicySelfTest() {}

    public static void main(String[] args) {
        stableVocabulary();
        profilePresetMatrix();
        configuredSupportedEffectiveSeparation();
        coherentRestartBoundary();
        versionedExtensionRegistry();
        qualificationReceipt();
        ordinaryAttackPolicy();
        System.out.println(
            "StrengthPolicySelfTest: PASS "
                + "(54-key versioned preset matrix, safety floors, ceilings, "
                + "14 action controls, pending restart, legacy schema, extension seam, TargetEpoch, commit gate)"
        );
    }

    private static void profilePresetMatrix() {
        require(ProfilePresetMatrix.CURRENT_REVISION == 1,
            "unexpected initial profile preset matrix revision");
        Set<StrengthOptionKey> compatibleMonitorKeys = Set.of(
            StrengthOptionKey.ATTACK_NON_ENTITY,
            StrengthOptionKey.ATTACK_LOGIC_SOURCE,
            StrengthOptionKey.ATTACK_RECOVERY_SOURCE,
            StrengthOptionKey.ATTACK_TASK_CHAIN,
            StrengthOptionKey.ATTACK_CALLBACK_CHAIN,
            StrengthOptionKey.ATTACK_DYNAMIC_CODE,
            StrengthOptionKey.ATTACK_SOURCE_TRACE,
            StrengthOptionKey.RUNTIME_INSTRUMENTATION,
            StrengthOptionKey.RUNTIME_JVMTI,
            StrengthOptionKey.RUNTIME_DYNAMIC_CODE_WATCH,
            StrengthOptionKey.RUNTIME_NATIVE_MODULE_WATCH,
            StrengthOptionKey.RUNTIME_ANOMALOUS_CALL_LOG
        );
        int compatibleMonitor = 0;
        int compatibleProtection = 0;
        for (StrengthOptionKey option : StrengthOptionKey.values()) {
            MechanismLevel compatible = ProfilePresetMatrix.presetLevel(
                ProfilePresetMatrix.CURRENT_REVISION,
                StrengthProfile.COMPATIBLE,
                option
            );
            require(compatible != MechanismLevel.FOLLOW_PROFILE,
                "compatible matrix contains configuration syntax");
            MechanismLevel expectedCompatible = compatibleMonitorKeys.contains(option)
                ? MechanismLevel.MONITOR : MechanismLevel.PROTECT_RONOVA;
            require(compatible == expectedCompatible,
                "compatible matrix row drifted for " + option);
            if (compatible == MechanismLevel.MONITOR) {
                compatibleMonitor++;
            } else if (compatible == MechanismLevel.PROTECT_RONOVA) {
                compatibleProtection++;
            } else {
                throw new AssertionError(
                    "compatible matrix has an unexpected level for " + option
                );
            }
            require(ProfilePresetMatrix.presetLevel(
                ProfilePresetMatrix.CURRENT_REVISION,
                StrengthProfile.ENHANCED,
                option
            ) == MechanismLevel.RELATED_PATHS,
                "enhanced matrix must use related-path policy for " + option);
            require(ProfilePresetMatrix.presetLevel(
                ProfilePresetMatrix.CURRENT_REVISION,
                StrengthProfile.EXTREME,
                option
            ) == MechanismLevel.STRICT_ALL,
                "extreme matrix must use strict policy for " + option);
            require(ProfilePresetMatrix.presetLevel(
                ProfilePresetMatrix.CURRENT_REVISION,
                StrengthProfile.CUSTOM,
                option
            ) == MechanismLevel.MONITOR,
                "custom FOLLOW_PROFILE fallback must be monitor for " + option);
        }
        require(compatibleMonitor == 12 && compatibleProtection == 42,
            "compatible per-key matrix distribution drifted");
        require(ProfilePresetMatrix.presetLevel(
            ProfilePresetMatrix.CURRENT_REVISION,
            StrengthProfile.COMPATIBLE,
            StrengthOptionKey.ATTACK_DYNAMIC_CODE
        ) == MechanismLevel.MONITOR,
            "compatible dynamic-code attack must remain observational");
        require(ProfilePresetMatrix.presetLevel(
            ProfilePresetMatrix.CURRENT_REVISION,
            StrengthProfile.COMPATIBLE,
            StrengthOptionKey.ATTACK_STATE_SOURCE
        ) == MechanismLevel.PROTECT_RONOVA,
            "compatible state-source attack must retain the stable target path");
        require(ProfilePresetMatrix.presetLevel(
            ProfilePresetMatrix.CURRENT_REVISION,
            StrengthProfile.COMPATIBLE,
            StrengthOptionKey.RUNTIME_DYNAMIC_CODE_WATCH
        ) == MechanismLevel.MONITOR,
            "compatible dynamic-code watch must not become broad enforcement");
        require(ProfilePresetMatrix.presetLevel(
            ProfilePresetMatrix.CURRENT_REVISION,
            StrengthProfile.COMPATIBLE,
            StrengthOptionKey.RUNTIME_JAVA_AGENT
        ) == MechanismLevel.PROTECT_RONOVA,
            "compatible Java Agent must protect Ronova rather than only log");

        ProfilePresetMatrix.Resolution disabled = ProfilePresetMatrix.resolve(
            ProfilePresetMatrix.CURRENT_REVISION,
            StrengthProfile.CUSTOM,
            StrengthOptionKey.ATTACK_DYNAMIC_CODE,
            MechanismLevel.OFF
        );
        require(disabled.requestedLevel() == MechanismLevel.OFF
                && !disabled.minimumFloorApplied(),
            "ordinary custom OFF must remain disabled");
        ProfilePresetMatrix.Resolution safetyFloor = ProfilePresetMatrix.resolve(
            ProfilePresetMatrix.CURRENT_REVISION,
            StrengthProfile.CUSTOM,
            StrengthOptionKey.PROTECT_LOOP_GUARD,
            MechanismLevel.OFF
        );
        require(safetyFloor.requestedLevel() == MechanismLevel.PROTECT_RONOVA
                && safetyFloor.minimumFloorApplied(),
            "loop guard must not fall below the mandatory safety floor");

        boolean rejected = false;
        try {
            ProfilePresetMatrix.presetLevel(
                ProfilePresetMatrix.CURRENT_REVISION + 1,
                StrengthProfile.EXTREME,
                StrengthOptionKey.INTERCEPT_MIXIN
            );
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        require(rejected, "unknown profile matrix revision was accepted");

        Map<StrengthOptionKey, MechanismLevel> materialized =
            ProfilePresetMatrix.materialize(
                ProfilePresetMatrix.CURRENT_REVISION,
                StrengthProfile.COMPATIBLE,
                Map.of()
            );
        require(materialized.size() == StrengthOptionKey.values().length
                && materialized.get(StrengthOptionKey.ATTACK_DYNAMIC_CODE)
                    == MechanismLevel.MONITOR
                && materialized.get(StrengthOptionKey.INTERCEPT_MIXIN)
                    == MechanismLevel.PROTECT_RONOVA,
            "custom transition did not freeze the complete current profile image");
    }

    private static void stableVocabulary() {
        require(StrengthOptionKey.values().length == 54,
            "7.16 strength option vocabulary drifted");
        HashSet<String> keys = new HashSet<>();
        for (StrengthOptionKey option : StrengthOptionKey.values()) {
            require(keys.add(option.canonical()), "duplicate strength option key");
            require(StrengthOptionKey.fromCanonical(option.canonical()) == option,
                "strength option lookup must round-trip");
        }
        require(MechanismLevel.lowerOf(
            MechanismLevel.STRICT_ALL, MechanismLevel.RELATED_PATHS
        ) == MechanismLevel.RELATED_PATHS, "supported ceiling must cap configured level");

        StrengthPolicyConfiguration configuration =
            new StrengthPolicyConfiguration(
                PolicyRevision.INITIAL,
                StrengthProfile.ENHANCED,
                Map.of()
            );
        require(configuration.canonicalKeyForSchema(1)
                .startsWith("1|1|ENHANCED|")
                && configuration.canonicalKeyForSchema(2)
                    .startsWith("2|1|1|ENHANCED|"),
            "legacy/current policy canonical encodings lost matrix versioning");
        boolean rejectedSchema = false;
        try {
            configuration.canonicalKeyForSchema(StrengthPolicySnapshot.SCHEMA_VERSION + 1);
        } catch (IllegalArgumentException expected) {
            rejectedSchema = true;
        }
        require(rejectedSchema, "unknown strength-policy schema was accepted");
    }

    private static void configuredSupportedEffectiveSeparation() {
        StrengthPolicySnapshot compatible = StrengthPolicySnapshot.defaults()
            .withCapability(
                StrengthOptionKey.INTERCEPT_MIXIN,
                MechanismLevel.STRICT_ALL,
                EvidenceState.VERIFIED,
                ActivationState.ACTIVE
            );
        StrengthResolutionReceipt receipt = compatible.resolve(
            StrengthOptionKey.INTERCEPT_MIXIN
        );
        require(receipt.configuredLevel() == MechanismLevel.FOLLOW_PROFILE,
            "profile-following syntax must remain visible");
        require(receipt.configuredMatrixRevision()
                == ProfilePresetMatrix.CURRENT_REVISION
                && receipt.activeMatrixRevision()
                    == ProfilePresetMatrix.CURRENT_REVISION,
            "resolution receipt must bind both matrix revisions");
        require(receipt.presetLevel() == MechanismLevel.PROTECT_RONOVA
                && receipt.requestedLevel() == MechanismLevel.PROTECT_RONOVA,
            "compatible interception did not resolve through its matrix row");
        require(receipt.supportedCeiling() == MechanismLevel.STRICT_ALL,
            "supported ceiling must remain independent");
        require(receipt.evidenceCeiling() == MechanismLevel.STRICT_ALL,
            "verified evidence ceiling must remain independent");
        require(receipt.effectiveLevel() == MechanismLevel.PROTECT_RONOVA,
            "compatible interception must protect Ronova only");
        require(receipt.activationState() == ActivationState.ACTIVE,
            "verified capability should activate");

        StrengthPolicySnapshot limitedExtreme = compatible
            .configure(StrengthProfile.EXTREME, Map.of())
            .activateConfigured()
            .withCapability(
                StrengthOptionKey.INTERCEPT_MIXIN,
                MechanismLevel.RELATED_PATHS,
                MechanismLevel.PROTECT_RONOVA,
                EvidenceState.DEGRADED,
                ActivationState.ACTIVE
            );
        StrengthResolutionReceipt limited = limitedExtreme.resolve(
            StrengthOptionKey.INTERCEPT_MIXIN
        );
        require(limited.supportedCeiling() == MechanismLevel.RELATED_PATHS
                && limited.evidenceCeiling() == MechanismLevel.PROTECT_RONOVA
                && limited.effectiveLevel() == MechanismLevel.PROTECT_RONOVA,
            "EXTREME must not overclaim beyond runtime or evidence support");
        require(limited.presetLevel() == MechanismLevel.STRICT_ALL
                && limited.requestedLevel() == MechanismLevel.STRICT_ALL,
            "EXTREME receipt lost its strict matrix request");
        require(limited.evidenceState() == EvidenceState.DEGRADED,
            "evidence state must survive policy resolution");

        StrengthResolutionReceipt disabled = compatible
            .configure(
                StrengthProfile.CUSTOM,
                Map.of(StrengthOptionKey.ATTACK_DYNAMIC_CODE, MechanismLevel.OFF)
            )
            .activateConfigured()
            .withCapability(
                StrengthOptionKey.ATTACK_DYNAMIC_CODE,
                MechanismLevel.STRICT_ALL,
                EvidenceState.VERIFIED,
                ActivationState.ACTIVE
            )
            .resolve(StrengthOptionKey.ATTACK_DYNAMIC_CODE);
        require(disabled.activationState() == ActivationState.INACTIVE
                && disabled.effectiveLevel() == MechanismLevel.OFF,
            "an explicitly disabled ordinary option must be INACTIVE");
        StrengthPolicySnapshot customImage = compatible.configure(
            StrengthProfile.CUSTOM,
            Map.of(StrengthOptionKey.ATTACK_DYNAMIC_CODE, MechanismLevel.OFF)
        );
        require(customImage.configuredLevels().get(
                    StrengthOptionKey.INTERCEPT_MIXIN
                ) == MechanismLevel.PROTECT_RONOVA
                && customImage.configuredLevels().values().stream().noneMatch(
                    value -> value == MechanismLevel.FOLLOW_PROFILE
                ),
            "partial Custom edit did not freeze every unedited profile value");

        StrengthResolutionReceipt mandatory = compatible
            .configure(
                StrengthProfile.CUSTOM,
                Map.of(StrengthOptionKey.PROTECT_LOOP_GUARD, MechanismLevel.OFF)
            )
            .activateConfigured()
            .withCapability(
                StrengthOptionKey.PROTECT_LOOP_GUARD,
                MechanismLevel.STRICT_ALL,
                EvidenceState.VERIFIED,
                ActivationState.ACTIVE
            )
            .resolve(StrengthOptionKey.PROTECT_LOOP_GUARD);
        require(mandatory.minimumFloorApplied()
                && mandatory.requestedLevel() == MechanismLevel.PROTECT_RONOVA
                && mandatory.effectiveLevel() == MechanismLevel.PROTECT_RONOVA,
            "mandatory loop-guard floor was not reflected by the receipt");
    }

    private static void coherentRestartBoundary() {
        StrengthPolicySnapshot active = StrengthPolicySnapshot.defaults()
            .withCapability(
                StrengthOptionKey.PROTECT_LIFE,
                MechanismLevel.STRICT_ALL,
                EvidenceState.VERIFIED,
                ActivationState.ACTIVE
            );
        StrengthPolicySnapshot configured = active.configure(
            StrengthProfile.EXTREME, Map.of()
        );
        require(configured.pendingRestart(), "profile change must require restart");
        require(configured.resolve(StrengthOptionKey.PROTECT_LIFE).pendingRestart(),
            "even hot-capable options must wait for the coherent profile boundary");
        require(configured.resolve(StrengthOptionKey.INTERCEPT_MIXIN).pendingRestart(),
            "startup interception must report pending restart");

        StrengthPolicySnapshot equalEffectiveButNewImage = active.configure(
            StrengthProfile.COMPATIBLE,
            Map.of(StrengthOptionKey.PROTECT_LIFE, MechanismLevel.PROTECT_RONOVA)
        );
        require(equalEffectiveButNewImage.pendingRestart(),
            "equal effective strength with different syntax is still a new image");
        require(equalEffectiveButNewImage.resolve(
            StrengthOptionKey.INTERCEPT_MIXIN
        ).pendingRestart(),
            "one changed option must hold the whole profile at the restart boundary");

        StrengthPolicySnapshot restarted = configured.activateConfigured();
        require(!restarted.pendingRestart(), "clean bootstrap must activate saved profile");
        require(restarted.configuration().profile() == StrengthProfile.EXTREME,
            "durable configuration must preserve profile");
        require(restarted.configuration().revision().value() == 2L,
            "policy revision must be monotonic");
    }

    private static void versionedExtensionRegistry() {
        TargetEpochCodec codec = new TargetEpochCodec();
        VersionedExtensionRegistry registry = new VersionedExtensionRegistry();
        registry.register(codec);
        registry.freeze();
        TargetEpoch value = new TargetEpoch(
            new dev.ronova.coremod.contract.SubjectKey(
                new dev.ronova.coremod.contract.RealmId(new UUID(0L, 1L)),
                UUID.fromString("00000000-0000-0000-0000-000000000031")), 31L
        );
        VersionedExtensionEnvelope envelope = codec.encode(value, 7L);
        byte[] payload = envelope.payload();
        payload[0] ^= 1;
        require(!Arrays.equals(payload, envelope.payload()),
            "extension payload accessor must be defensive");
        VersionedExtensionRegistry.Resolution resolution = registry.resolve(envelope);
        require(resolution.resolved() && value.equals(resolution.value()),
            "registered extension must resolve deterministically");
        require(registry.resolve(VersionedExtensionEnvelope.utf8(
            new VersionedExtensionId("ronova", "unknown"), 1, 0, "x"
        )).status() == VersionedExtensionRegistry.Status.UNKNOWN_EXTENSION,
            "unknown extensions must remain non-authoritative opaque data");
    }

    private static void qualificationReceipt() {
        UUID target = UUID.fromString("00000000-0000-0000-0000-000000000052");
        DominionEpoch before = DominionEpoch.LOCAL_INITIAL;
        DominionEpoch after = before.nextSequence();
        SubjectIdentity subject = new SubjectIdentity(
            target, "minecraft:zombie", "test.Zombie", "minecraft:overworld",
            1.0D, 2.0D, 3.0D, false
        );
        DominionDecision.Judgment judgment = new DominionDecision.Judgment(
            subject, after, true, DominionDecision.LineageScope.IDENTITY,
            0.0D, "strength-policy-self-test"
        );
        JudgmentBatch batch = new JudgmentBatch(UUID.randomUUID(), List.of(judgment));
        DominionCommand command = new DominionCommand.CommitJudgmentBatch(batch);
        CommitReceipt receipt = new CommitReceipt(
            command, before, after,
            new DominionSnapshot(after, List.of(judgment)),
            batch.commandId(), batch.canonicalPayloadHash(), 1L,
            CommitReceipt.Status.COMMITTED
        );
        require(receipt.targetEpoch(new dev.ronova.coremod.contract.SubjectKey(
            new dev.ronova.coremod.contract.RealmId(new UUID(0L, 1L)), target)) == null,
            "legacy receipt must not fabricate a per-subject qualification");
        require(receipt.admissionRevision().equals(after),
            "Admission revision must share the committed Root revision");
        require(receipt.revokeThroughEpoch().equals(before),
            "old qualification must be revoked through the pre-commit epoch");
        int[] effects = {0};
        require(CommittedJudgmentGate.from(receipt).run(judgment, () -> effects[0]++),
            "committed batch member did not pass the physical execution gate");
        for (CommitReceipt.Status status : CommitReceipt.Status.values()) {
            if (status == CommitReceipt.Status.COMMITTED) continue;
            CommitReceipt failed = new CommitReceipt(command, before, before,
                new DominionSnapshot(before, List.of()), batch.commandId(), batch.canonicalPayloadHash(), 1L, status);
            require(!CommittedJudgmentGate.from(failed).run(judgment, () -> effects[0]++),
                "noncommitted receipt invoked a physical action: " + status);
        }
        require(!CommittedJudgmentGate.from(null).run(judgment, () -> effects[0]++),
            "missing receipt invoked a physical action");
        DominionDecision.Judgment stale = new DominionDecision.Judgment(
            subject, before, true, DominionDecision.LineageScope.IDENTITY, 0.0D, "stale");
        require(!CommittedJudgmentGate.from(receipt).run(stale, () -> effects[0]++),
            "uncommitted/stale decision invoked a physical action");
        DominionDecision.Judgment unrelated = new DominionDecision.Judgment(
            new SubjectIdentity(UUID.randomUUID(), "minecraft:zombie", "test.Zombie",
                "minecraft:overworld", 1.0D, 2.0D, 3.0D, false),
            after, true, DominionDecision.LineageScope.IDENTITY, 0.0D, "unrelated");
        require(!CommittedJudgmentGate.from(receipt).run(unrelated, () -> effects[0]++),
            "same-epoch unrelated target invoked a physical action");
        DominionCommand unrelatedCommand = new DominionCommand.BindOwner(UUID.randomUUID());
        require(!CommittedJudgmentGate.from(new CommitReceipt(unrelatedCommand,
            before, after, new DominionSnapshot(after, List.of())))
            .run(judgment, () -> effects[0]++), "non-judgment receipt authorized clearing");
        require(effects[0] == 1, "failed/unknown receipt leaked physical effects");
    }

    private static void ordinaryAttackPolicy() {
        require(AttackAction.values().length == 14, "ordinary action vocabulary drifted");
        AttackActionPolicy defaults = AttackActionPolicy.followingProfile();
        long enabled = Arrays.stream(AttackAction.values()).filter(action ->
            defaults.resolve(StrengthProfile.COMPATIBLE, action) != AttackActionLevel.DISABLED
        ).count();
        require(enabled == 9, "compatible ordinary-action preset drifted");
        for (AttackAction action : AttackAction.values()) {
            require(AttackAction.fromCanonical(action.canonical()) == action, "action lookup failed");
            require(defaults.resolve(StrengthProfile.ENHANCED, action) == AttackActionLevel.ENHANCED,
                "enhanced ordinary-action preset drifted");
            require(defaults.resolve(StrengthProfile.EXTREME, action) == AttackActionLevel.EXTREME,
                "extreme ordinary-action preset drifted");
            require(AttackActionPolicy.legacyDefaults(StrengthProfile.CUSTOM)
                .resolve(StrengthProfile.CUSTOM, action) == AttackActionLevel.DISABLED,
                "legacy custom silently gained an ordinary action");
        }
        StrengthPolicySnapshot base = StrengthPolicySnapshot.defaults()
            .configure(StrengthProfile.EXTREME, Map.of()).activateConfigured()
            .withCapability(StrengthOptionKey.ATTACK_DISPATCH, MechanismLevel.STRICT_ALL,
                EvidenceState.VERIFIED, ActivationState.ACTIVE);
        require(base.resolveAttack(Set.of(AttackAction.ALL_RETURN), Set.of()).permitted(),
            "verified supported action was rejected");
        require(!base.configuration().permitsBaselineAction(AttackAction.ALL_RETURN),
            "allReturn was authorized through the legacy baseline gate");
        StrengthPolicySnapshot pending = base.configure(StrengthProfile.CUSTOM, Map.of(),
            Map.of(AttackAction.ALL_RETURN, AttackActionLevel.DISABLED));
        StrengthPolicySnapshot custom = base.configure(StrengthProfile.CUSTOM, Map.of()).activateConfigured();
        StrengthPolicySnapshot actionOnly = custom.configure(StrengthProfile.CUSTOM, Map.of(),
            Map.of(AttackAction.ALL_RETURN, AttackActionLevel.DISABLED));
        require(actionOnly.configuration().levels().equals(custom.configuration().levels())
            && actionOnly.activeProfile() == actionOnly.configuredProfile()
            && actionOnly.resolve(StrengthOptionKey.INTERCEPT_MIXIN).pendingRestart(),
            "ordinary-only edit failed to hold the entire image behind restart");
        require(pending.pendingRestart()
                && pending.activeActions().resolve(pending.activeProfile(), AttackAction.ALL_RETURN)
                    == AttackActionLevel.EXTREME,
            "action-only change bypassed the restart boundary");
        StrengthPolicySnapshot disabled = pending.activateConfigured();
        require(!disabled.resolveAttack(Set.of(AttackAction.ALL_RETURN), Set.of()).permitted(),
            "advanced dispatch re-enabled disabled allReturn");
        require(!disabled.resolveAttack(
            Set.of(AttackAction.ALL_RETURN, AttackAction.STRIP_CODE_DEFENSE), Set.of()).permitted(),
            "compound action bypassed its disabled member");
        StrengthPolicySnapshot monitor = base.configure(StrengthProfile.CUSTOM,
            Map.of(StrengthOptionKey.ATTACK_DISPATCH, MechanismLevel.MONITOR)).activateConfigured();
        require(!monitor.resolveAttack(Set.of(AttackAction.ALL_RETURN), Set.of()).permitted(),
            "MONITOR dependency authorized an attack");
        require(!base.resolveAttack(Set.of(AttackAction.ALL_RETURN),
            Set.of(StrengthOptionKey.ATTACK_DYNAMIC_CODE)).permitted(),
            "bytecode branch bypassed its additional dependency");
        StrengthPolicySnapshot limited = base.withCapability(StrengthOptionKey.ATTACK_DISPATCH,
            MechanismLevel.RELATED_PATHS, EvidenceState.VERIFIED, ActivationState.ACTIVE);
        require(limited.resolveAttack(Set.of(AttackAction.ALL_RETURN), Set.of()).effectiveDepth()
            == MechanismLevel.RELATED_PATHS, "ordinary request exceeded executor ceiling");
        require(!StrengthPolicySnapshot.defaults().configure(StrengthProfile.EXTREME, Map.of())
                .activateConfigured().resolveAttack(Set.of(AttackAction.ALL_RETURN), Set.of()).permitted(),
            "unimplemented executor was reported available");
        StrengthPolicyConfiguration configured = disabled.configuration();
        StrengthPolicyConfiguration next = configured.next(StrengthProfile.CUSTOM,
            Map.of(StrengthOptionKey.INTERCEPT_MIXIN, MechanismLevel.MONITOR));
        require(next.attacks().equals(configured.attacks()), "editing a key reset ordinary actions");
        require(next.next(StrengthProfile.EXTREME, Map.of()).attacks().equals(defaults),
            "selecting a preset did not reset action overrides");
        UUID id = UUID.randomUUID();
        DominionCommand command = new DominionCommand.ConfigureStrengthPolicy(id, configured);
        require(!DominionCommand.matchesCanonicalPayloadHash(command,
            DominionCommand.canonicalPayloadHash(command, 2)),
            "legacy hash authenticated new ordinary-action permissions");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class TargetEpochCodec implements TargetEpochExtension {
        private static final VersionedExtensionId ID =
            new VersionedExtensionId("ronova", "target_epoch");

        @Override public VersionedExtensionId id() { return ID; }
        @Override public int schemaVersion() { return 2; }
        @Override public Class<TargetEpoch> valueType() { return TargetEpoch.class; }

        @Override
        public VersionedExtensionEnvelope encode(TargetEpoch value, long revision) {
            String payload = value.subjectKey().realmId().canonical() + "|"
                + value.subjectKey().subjectId() + "|" + value.generation();
            return VersionedExtensionEnvelope.create(
                ID, schemaVersion(), revision, payload.getBytes(StandardCharsets.UTF_8)
            );
        }

        @Override
        public TargetEpoch decode(VersionedExtensionEnvelope envelope) {
            if (!ID.equals(envelope.id()) || envelope.schemaVersion() != schemaVersion()) {
                throw new IllegalArgumentException("wrong TargetEpoch extension envelope");
            }
            String[] parts = new String(
                envelope.payload(), StandardCharsets.UTF_8
            ).split("\\|", -1);
            if (parts.length != 3) {
                throw new IllegalArgumentException("malformed TargetEpoch payload");
            }
            return new TargetEpoch(
                new dev.ronova.coremod.contract.SubjectKey(
                    dev.ronova.coremod.contract.RealmId.fromString(parts[0]), UUID.fromString(parts[1])),
                Long.parseLong(parts[2])
            );
        }
    }
}
