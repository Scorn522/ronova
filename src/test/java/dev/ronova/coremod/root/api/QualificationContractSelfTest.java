package dev.ronova.coremod.root.api;

import dev.ronova.coremod.contract.*;
import java.util.List;
import java.util.UUID;

/** Scope, receipt-membership and stale-execution regressions against the production JAR. */
public final class QualificationContractSelfTest {
    public static void main(String[] args) {
        RealmId realm = new RealmId(new UUID(0L, 17L));
        SubjectKey key = new SubjectKey(realm, new UUID(0L, 1L));
        SubjectKey foreign = new SubjectKey(new RealmId(new UUID(0L, 18L)), key.subjectId());
        TargetEpoch first = new TargetEpoch(key, 1L);
        require(!first.accepts(new TargetEpoch(foreign, 1L)), "same UUID crossed Realm boundary");
        reject(() -> first.compareTo(new TargetEpoch(foreign, 1L)));
        reject(() -> new TargetEpoch(key, Long.MAX_VALUE).next());
        reject(() -> new EffectKey(UUID.randomUUID(), new TargetEpoch(key, 0L), "ronova:death", "once"));

        DominionEpoch before = new DominionEpoch(3L, 70L), after = before.nextSequence();
        SubjectIdentity metadata = new SubjectIdentity(key.subjectId(), "test:entity", "test.Entity",
            "test:world", 1D, 2D, 3D, false);
        DominionDecision.Judgment decision = new DominionDecision.Judgment(metadata, after, true,
            DominionDecision.LineageScope.IDENTITY, 0D, "qualification-test");
        JudgmentBatch batch = JudgmentBatch.of(UUID.randomUUID(), decision);
        SubjectQualification baseline = QualificationPreconditions.baseline(key);
        QualificationPreconditions expectations = new QualificationPreconditions(realm, 1L, List.of(baseline));
        JudgmentBatch guarded = batch.withPreconditions(expectations);
        require(!guarded.canonicalPayloadHash().equals(batch.canonicalPayloadHash())
            && guarded.commandId().equals(batch.commandId()), "expected qualifications were not bound to the payload");
        reject(() -> guarded.withPreconditions(new QualificationPreconditions(realm, 2L, List.of(baseline))));
        reject(() -> batch.withPreconditions(new QualificationPreconditions(realm, 1L, List.of())));
        reject(() -> new QualificationPreconditions(realm, 1L, List.of(baseline, baseline)));
        reject(() -> new QualificationPreconditions(realm, 1L, List.of(QualificationPreconditions.baseline(foreign))));
        reject(() -> new QualificationPreconditions(realm, 0L, List.of(baseline)));
        require(expectations.matches(realm, 1L, java.util.Map.of())
            && !expectations.matches(realm, 2L, java.util.Map.of())
            && !expectations.matches(realm, 1L, java.util.Map.of(key, baseline.transition(true))),
            "qualification or policy drift was accepted");
        DominionCommand command = new DominionCommand.CommitJudgmentBatch(batch);
        DominionSnapshot snapshot = new DominionSnapshot(after, List.of(decision));
        DominionCommand guardedCommand = new DominionCommand.CommitJudgmentBatch(guarded);
        reject(() -> new CommitReceipt(guardedCommand, before, after, snapshot));
        reject(() -> new CommitReceipt(guardedCommand, before, after, snapshot, guarded.commandId(),
            guarded.canonicalPayloadHash(), 71L, CommitReceipt.Status.COMMITTED,
            List.of(new SubjectQualification(first.next(), true))));
        CommitReceipt receipt = new CommitReceipt(command, before, after, snapshot, batch.commandId(),
            batch.canonicalPayloadHash(), 71L, CommitReceipt.Status.COMMITTED,
            List.of(new SubjectQualification(first, true)));
        require(receipt.targetEpoch(key).generation() == 1L && receipt.targetEpoch(foreign) == null,
            "receipt used the global sequence or accepted a foreign subject");
        int[] effects = {0};
        var gate = CommittedJudgmentGate.from(receipt);
        require(gate.run(decision, ignored -> first, () -> effects[0]++), "current receipt was rejected");
        require(!gate.run(decision, ignored -> first.next(), () -> effects[0]++), "cleared qualification reused old receipt");
        require(!gate.run(decision, ignored -> null, () -> effects[0]++), "unknown binding authorized execution");
        require(effects[0] == 1, "stale gate executed physical callback");
        CommitReceipt historical = new CommitReceipt(command, before, after, snapshot);
        require(!CommittedJudgmentGate.from(historical).run(decision, ignored -> first, () -> effects[0]++),
            "legacy receipt fabricated current qualification");
        reject(() -> new CommitReceipt(command, before, after, snapshot, batch.commandId(),
            batch.canonicalPayloadHash(), 71L, CommitReceipt.Status.COMMITTED,
            List.of(new SubjectQualification(new TargetEpoch(new SubjectKey(realm, new UUID(0L, 2L)), 1L), true))));

        EffectKey effect = new EffectKey(batch.commandId(), first, "ronova:lifecycle/death", "once");
        require(effect.belongsTo(receipt) && !effect.belongsTo(historical)
            && !effect.belongsTo(CommitReceipt.unknown(command, before, new DominionSnapshot(before, List.of()), 70L)),
            "effect identity bypassed committed qualified receipt");
        require(effect.equals(new EffectKey(batch.commandId(), first, "ronova:lifecycle/death", "once")),
            "semantic effect identity changed during replan");
        require(!effect.equals(new EffectKey(batch.commandId(), first.next(), "ronova:lifecycle/death", "once")),
            "new qualification reused an old effect identity");
        reject(() -> new EffectKey(batch.commandId(), first, "invalid name", "once"));
        var tracker = new dev.ronova.coremod.runtime.PhysicalIncarnationTracker();
        Object physicalA = new String("same-logical-value"), physicalB = new String("same-logical-value");
        UUID nonceA = tracker.nonce(physicalA);
        require(nonceA.equals(tracker.nonce(physicalA)) && !nonceA.equals(tracker.nonce(physicalB)),
            "physical nonce used object equality instead of identity");
        var full = baseline.judgmentTransition(nonceA);
        require(full.revisions().bindingRevision() == 1L && full.revisions().activeIncarnation().subjectKey().equals(key),
            "binding lost the qualified subject identity");
        reject(() -> new SubjectQualification(new TargetEpoch(foreign, 1), true, full.revisions()));
        try {
            new SubjectRevisions(Long.MAX_VALUE, full.revisions().activeIncarnation(), 1, 1, 0, java.util.Map.of())
                .bind(key, UUID.randomUUID());
            throw new AssertionError("binding revision wrapped on overflow");
        } catch (ArithmeticException expected) { }
        reject(() -> new StateDomainId("unbounded|injection"));
        System.out.println("QualificationContractSelfTest: PASS (Realm scope, per-subject generation, overflow, receipt membership, stale gate, stable effect identity)");
    }
    private static void reject(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException | IllegalStateException expected) { return; }
        throw new AssertionError("invalid qualification was accepted");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
