package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.*;
import dev.ronova.coremod.policy.*;
import dev.ronova.coremod.root.api.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;

/** Complete file-backed batch conflict and original-receipt recovery scenarios. */
final class QualificationPreconditionScenarios {
    static void verify() throws Exception {
        Path world = Files.createTempDirectory("ronova-batch-preconditions-").toAbsolutePath().normalize();
        try {
            LocalDominionStore store = LocalDominionStore.openWorld(world);
            RealmId realm = store.realmId();
            UUID a = new UUID(0L, 591L), b = new UUID(0L, 592L);
            SubjectKey ka = new SubjectKey(realm, a), kb = new SubjectKey(realm, b);
            Path rootFile = world.resolve("ronova/durable-root-v1.bin");
            JudgmentBatch batch = store.qualify(batch(store.nextDecisionEpoch(), a, b));
            QualificationPreconditions expected = batch.preconditions();
            byte[] initial = Files.readAllBytes(rootFile);
            for (String mode : List.of("one-subject", "policy", "realm")) {
                RealmId selectedRealm = mode.equals("realm") ? new RealmId(new UUID(0L, 999L)) : realm;
                List<SubjectQualification> subjects = List.of(
                    QualificationPreconditions.baseline(new SubjectKey(selectedRealm, a)),
                    new SubjectQualification(new TargetEpoch(new SubjectKey(selectedRealm, b),
                        mode.equals("one-subject") ? 1L : 0L), mode.equals("one-subject")));
                JudgmentBatch conflicting = batch(store.nextDecisionEpoch(), a, b).withPreconditions(
                    new QualificationPreconditions(selectedRealm,
                        expected.policyRevision() + (mode.equals("policy") ? 1L : 0L), subjects));
                CommitReceipt refused = store.commit(conflicting);
                require(refused.rejected() && store.commitIndex() == 0L
                    && store.targetEpoch(ka) == null && store.targetEpoch(kb) == null
                    && store.recover(conflicting.commandId()) == null
                    && Arrays.equals(initial, Files.readAllBytes(rootFile)),
                    "conflict partially mutated or persisted a batch: " + mode);
            }
            // Two callers read the same acknowledged head. The loser must not refresh its expectation.
            JudgmentBatch competing = store.qualify(batch(store.nextDecisionEpoch(), a, b));
            CommitReceipt first = store.commit(batch);
            require(first.committed() && first.qualifications().size() == 2 && first.commitIndex() == 1L,
                "guarded batch did not commit exactly once");
            require(store.commit(competing).rejected() && store.qualify(competing) == competing,
                "stale caller renewed its preconditions");
            store.commit(new DominionCommand.ClearTombstone(a));
            long beforeConflict = store.commitIndex();
            byte[] beforeBytes = Files.readAllBytes(rootFile);
            // Correct global epoch is insufficient when just one subject expectation is stale.
            JudgmentBatch staleAtFreshGlobal = batch(store.nextDecisionEpoch(), a, b).withPreconditions(expected);
            require(store.commit(staleAtFreshGlobal).rejected() && store.commitIndex() == beforeConflict
                && !store.hasTombstone(a) && store.hasTombstone(b)
                && Arrays.equals(beforeBytes, Files.readAllBytes(rootFile)), "per-subject conflict used only global epoch");

            JudgmentBatch stalePolicy = store.qualify(batch(store.nextDecisionEpoch(), a));
            store.commit(new DominionCommand.ConfigureStrengthPolicy(new StrengthPolicyConfiguration(
                new PolicyRevision(2L), StrengthProfile.COMPATIBLE, Map.of())));
            JudgmentBatch policyAtFreshGlobal = batch(store.nextDecisionEpoch(), a).withPreconditions(stalePolicy.preconditions());
            require(store.commit(policyAtFreshGlobal).rejected(), "policy revision was not enforced");

            JudgmentBatch next = store.qualify(batch(store.nextDecisionEpoch(), a));
            store.shadow().armCommitFailpoint(RootCommitFailpoint.AFTER_APPLY_BEFORE_ACK);
            require(store.commit(next).unknown(), "guarded ACK-loss fixture did not become unknown");
            CommitReceipt recovered = store.recover(next.commandId(), next.canonicalPayloadHash());
            require(recovered.committed() && recovered.targetEpoch(ka).generation() == 3L,
                "ACK recovery issued a replacement qualification");
            long index = store.commitIndex();
            LocalDominionStore reopened = LocalDominionStore.openWorld(world);
            CommitReceipt original = reopened.recover(batch.commandId(), batch.canonicalPayloadHash());
            require(original.command().equals(first.command()) && original.qualifications().equals(first.qualifications())
                && reopened.commit(next).qualifications().equals(recovered.qualifications())
                && reopened.commitIndex() == index, "restart changed original preconditions/hash/receipt");

            CompoundTag image = DurableRootFileBackend.at(rootFile).readIfPresent();
            for (String mode : List.of("missing", "wrong-type", "schema", "policy", "subject")) {
                CompoundTag corrupt = image.copy();
                CompoundTag command = corrupt.getList("command_journal_v1", Tag.TAG_COMPOUND)
                    .getCompound(0).getCompound("command");
                require(command.contains("qualification_preconditions", Tag.TAG_COMPOUND), "fixture lost persisted preconditions");
                switch (mode) {
                    case "missing" -> command.remove("qualification_preconditions");
                    case "wrong-type" -> command.putString("qualification_preconditions", "invalid");
                    case "schema" -> command.getCompound("qualification_preconditions").putInt("schema", 999);
                    case "policy" -> command.getCompound("qualification_preconditions").putLong("policy_revision", 8L);
                    case "subject" -> command.getCompound("qualification_preconditions").getCompound("qualifications")
                        .getList("subjects", Tag.TAG_COMPOUND).getCompound(0).putLong("target_generation", 90L);
                }
                require(ShadowDurableRootStore.load(corrupt, realm).readOnly(), "tampered request was accepted: " + mode);
            }
            System.out.println("QualificationPreconditionScenarios: PASS (whole-batch conflicts, policy/Realm, original ID, ACK loss, restart, tampering)");
        } finally {
            try (var files = Files.walk(world)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) {
                    if (!path.toAbsolutePath().normalize().startsWith(world)) throw new IllegalStateException("unsafe test cleanup");
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static JudgmentBatch batch(DominionEpoch epoch, UUID... ids) {
        return new JudgmentBatch(UUID.randomUUID(), Arrays.stream(ids).map(id -> new DominionDecision.Judgment(
            new SubjectIdentity(id, "test:entity", "test.Entity", "minecraft:overworld", 0D, 0D, 0D, false),
            epoch, true, DominionDecision.LineageScope.IDENTITY, 0D, "precondition-test")).toList());
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
