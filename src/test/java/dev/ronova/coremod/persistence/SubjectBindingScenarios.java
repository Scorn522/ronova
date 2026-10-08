package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.*;
import dev.ronova.coremod.root.api.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;

/** Real durable files, independent scope counters, rebind/revoke and effect-identity preservation. */
final class SubjectBindingScenarios {
    static void verify() throws Exception {
        Path world = Files.createTempDirectory("ronova-subject-binding-").toAbsolutePath().normalize();
        try {
            LocalDominionStore store = LocalDominionStore.openWorld(world);
            UUID actor = new UUID(60, 1), otherOwner = new UUID(60, 2);
            store.commit(new DominionCommand.BindOwner(actor)); store.commit(new DominionCommand.BindOwner(otherOwner));
            RealmId realm = store.realmId();
            SubjectKey a = new SubjectKey(realm, new UUID(60, 3)), b = new SubjectKey(realm, new UUID(60, 4));
            UUID na = new UUID(60, 30), nb = new UUID(60, 40);
            JudgmentBatch initial = store.qualify(batch(store.nextDecisionEpoch(), a, b))
                .withIncarnations(Map.of(a.subjectId(), na, b.subjectId(), nb));
            long before = store.commitIndex();
            CommitReceipt receipt = store.commit(initial);
            SubjectQualification qa = store.subjectQualification(a), qb = store.subjectQualification(b);
            require(store.snapshot().judgments().stream().noneMatch(d -> d.subject().player()),
                "identity-scoped non-player was reconstructed as a player");
            require(receipt.committed() && store.commitIndex() == before + 1 && receipt.qualifications().size() == 2,
                "physical binding split the authority batch");
            require(qa.revisions().bindingRevision() == 1 && qa.targetEpoch().generation() == 1
                && qa.revisions().judgmentRevision() == 1 && qa.revisions().admissionTokenRevision() == 1
                && qa.revisions().activeIncarnation().nonce().equals(na), "initial binding vector is incomplete");
            var decision = initial.judgments().stream().filter(d -> d.subject().uuid().equals(a.subjectId())).findFirst().orElseThrow();
            var gate = CommittedJudgmentGate.from(receipt); int[] physical = {0};
            require(gate.runQualified(decision, na, store::subjectQualification, () -> physical[0]++)
                && !gate.runQualified(decision, nb, store::subjectQualification, () -> physical[0]++)
                && physical[0] == 1, "same logical UUID authorized a different physical object");

            EffectClaim claim = store.prepareLifecycleEffect(a.subjectId(), "bound-object-a");
            require(claim.granted(), "bound judgment did not receive its first effect intent");
            StateDomainId data = new StateDomainId("core.data"), network = new StateDomainId("core.network");
            SubjectKey ownerKey = new SubjectKey(realm, otherOwner);
            CommitReceipt own = transition(store, actor, new SubjectTransitionBatch.Transition(a,
                SubjectTransitionBatch.Kind.SET_OWNERSHIP, null, data, ownerKey));
            SubjectQualification owned = store.subjectQualification(a);
            require(own.committed() && owned.targetEpoch().equals(qa.targetEpoch())
                && owned.revisions().bindingRevision() == 1 && owned.revisions().judgmentRevision() == 1
                && owned.revisions().admissionTokenRevision() == 1
                && owned.revisions().ownership().get(data).revision() == 1,
                "domain ownership changed unrelated revision scopes");
            require(!gate.runQualified(decision, na, store::subjectQualification, () -> physical[0]++)
                && !store.enterEffect(claim), "ownership drift failed to revoke an old execution entry");
            store.finishEffectUnknown(claim);
            EffectClaim recoveredEffect = store.prepareLifecycleEffect(a.subjectId(), "another-object");
            require(!recoveredEffect.granted() && recoveredEffect.record() != null
                && recoveredEffect.record().key().equals(claim.record().key()) && store.effectRecordCount() == 1,
                "ownership update minted a second death-effect identity");

            var stale = store.prepareTransitions(UUID.randomUUID(), actor, List.of(new SubjectTransitionBatch.Transition(a,
                SubjectTransitionBatch.Kind.REACTIVATE, null, null, null)));
            transition(store, actor, new SubjectTransitionBatch.Transition(a,
                SubjectTransitionBatch.Kind.SET_OWNERSHIP, null, network, ownerKey));
            var staleFreshGlobal = new SubjectTransitionBatch(stale.commandId(), actor, store.epoch(), stale.preconditions(), stale.transitions());
            require(store.commit(new DominionCommand.TransitionSubjects(staleFreshGlobal)).rejected(),
                "correct global epoch bypassed a stale ownership vector");
            require(store.subjectQualification(a).revisions().ownership().get(data).revision() == 1
                && store.subjectQualification(a).revisions().ownership().get(network).revision() == 1,
                "one ownership domain incremented another domain");
            long oldIndex = store.commitIndex();
            CommitReceipt revokeOwner = store.commit(new DominionCommand.UnbindOwner(otherOwner));
            require(revokeOwner.committed() && store.commitIndex() == oldIndex + 1
                && store.subjectQualification(a).revisions().ownership().values().stream()
                    .allMatch(value -> value.owner() == null && value.revision() == 2)
                && store.subjectQualification(a).targetEpoch().equals(qa.targetEpoch()),
                "owner removal did not atomically revoke all owned domains");

            var unsafe = store.prepareTransitions(UUID.randomUUID(), actor, List.of(
                new SubjectTransitionBatch.Transition(a, SubjectTransitionBatch.Kind.REACTIVATE, null, null, null),
                new SubjectTransitionBatch.Transition(b, SubjectTransitionBatch.Kind.REBIND, UUID.randomUUID(), null, null)));
            oldIndex = store.commitIndex();
            require(store.commit(new DominionCommand.TransitionSubjects(unsafe)).rejected()
                && store.subjectQualification(a).terminal() && store.subjectQualification(b).equals(qb)
                && store.commitIndex() == oldIndex, "invalid rebind partially reactivated another subject");
            var reactivate = store.prepareTransitions(UUID.randomUUID(), actor, List.of(
                new SubjectTransitionBatch.Transition(a, SubjectTransitionBatch.Kind.REACTIVATE, null, null, null)));
            store.shadow().armCommitFailpoint(RootCommitFailpoint.AFTER_APPLY_BEFORE_ACK);
            require(store.commit(new DominionCommand.TransitionSubjects(reactivate)).unknown(), "reactivation ACK-loss fixture failed");
            CommitReceipt recovered = store.recover(reactivate.commandId());
            SubjectQualification active = store.subjectQualification(a);
            require(recovered.committed() && !store.hasTombstone(a.subjectId()) && !active.terminal()
                && active.targetEpoch().generation() == 2 && active.revisions().bindingRevision() == 1
                && active.revisions().judgmentRevision() == 2 && active.revisions().admissionTokenRevision() == 2,
                "reactivation lost its independent counters");
            UUID fresh = new UUID(60, 31);
            CommitReceipt rebind = transition(store, actor,
                new SubjectTransitionBatch.Transition(a, SubjectTransitionBatch.Kind.REBIND, fresh, null, null));
            SubjectQualification boundAgain = store.subjectQualification(a);
            require(rebind.committed() && boundAgain.targetEpoch().generation() == 3
                && boundAgain.revisions().bindingRevision() == 2 && boundAgain.revisions().judgmentRevision() == 2
                && boundAgain.revisions().admissionTokenRevision() == 3 && boundAgain.revisions().revokedThroughEpoch() == 2
                && boundAgain.revisions().activeIncarnation().sequence() == 2
                && boundAgain.revisions().activeIncarnation().nonce().equals(fresh), "rebind conflated independent revision scopes");
            oldIndex = store.commitIndex();
            require(!transition(store, actor, new SubjectTransitionBatch.Transition(a,
                SubjectTransitionBatch.Kind.REBIND, fresh, null, null)).changed() && store.commitIndex() == oldIndex,
                "same-incarnation no-op advanced a counter");
            require(transition(store, otherOwner, new SubjectTransitionBatch.Transition(a,
                SubjectTransitionBatch.Kind.REBIND, UUID.randomUUID(), null, null)).rejected(), "revoked actor rebound a subject");
            require(!gate.runQualified(decision, na, store::subjectQualification, () -> physical[0]++), "old incarnation regained authority");

            LocalDominionStore reopened = LocalDominionStore.openWorld(world);
            require(reopened.snapshot().judgments().stream().noneMatch(d -> d.subject().player())
                && LegacyMirrorFacts.equivalent(reopened.shadow(), reopened.legacyMirror()),
                "player metadata changed in the authoritative snapshot or mirror after restart");
            require(reopened.subjectQualification(a).equals(boundAgain) && reopened.subjectQualification(b).equals(qb)
                && reopened.recover(initial.commandId()).command().equals(receipt.command())
                && reopened.recover(reactivate.commandId()).qualifications().equals(recovered.qualifications())
                && reopened.effectRecordCount() == 1 && reopened.effectUnresolvedCount() == 1,
                "restart changed binding history, transition identity or unknown effect");
            CompoundTag image = DurableRootFileBackend.at(world.resolve("ronova/durable-root-v1.bin")).readIfPresent();
            require(image.getCompound("subject_qualifications").getInt("schema") == 2, "full revisions were not persisted");
            for (String field : List.of("binding", "judgment", "admission", "revoked_through")) {
                CompoundTag corrupt = image.copy();
                corrupt.getCompound("subject_qualifications").getList("subjects", Tag.TAG_COMPOUND).getCompound(0)
                    .getCompound("revisions").putLong(field, 999);
                require(ShadowDurableRootStore.load(corrupt, realm).readOnly(), "corrupt revision accepted: " + field);
            }
            System.out.println("SubjectBindingScenarios: PASS (atomic physical binding, scoped ownership/revoke, guarded reactivation/rebind, ACK recovery, stable effect identity, restart)");
        } finally {
            try (var paths = Files.walk(world)) {
                for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                    if (!p.toAbsolutePath().normalize().startsWith(world)) throw new IllegalStateException("unsafe test cleanup");
                    Files.deleteIfExists(p);
                }
            }
        }
    }
    private static CommitReceipt transition(LocalDominionStore store, UUID actor, SubjectTransitionBatch.Transition... transitions) {
        return store.commit(new DominionCommand.TransitionSubjects(store.prepareTransitions(UUID.randomUUID(), actor, List.of(transitions))));
    }
    private static JudgmentBatch batch(DominionEpoch epoch, SubjectKey... keys) {
        return new JudgmentBatch(UUID.randomUUID(), Arrays.stream(keys).map(key -> new DominionDecision.Judgment(
            new SubjectIdentity(key.subjectId(), "test:entity", "test.Entity", "minecraft:overworld", 0, 0, 0, false),
            epoch, true, DominionDecision.LineageScope.IDENTITY, 0D, "binding-test")).toList());
    }
    private static void require(boolean value, String why) { if (!value) throw new AssertionError(why); }
}
