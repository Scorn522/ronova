package dev.ronova.coremod.engine;

import dev.ronova.coremod.adapter.MinecraftSubjectAdapter;
import dev.ronova.coremod.authority.AuthorityView;
import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionRequest;
import dev.ronova.coremod.contract.SubjectIdentity;
import dev.ronova.coremod.core.LocalDominionCore;
import dev.ronova.coremod.root.api.CommitReceipt;
import dev.ronova.coremod.root.api.CommittedJudgmentGate;
import dev.ronova.coremod.root.api.JudgmentBatch;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Executes an immutable Judgment decision; it never discovers or selects targets. */
public final class JudgmentExecutor {
    private JudgmentExecutor() {}

    /** Compatibility entry point; the owner server supplies the read view. */
    public static SpawnFirewall.ExecutionResult execute(
        Entity entity,
        ServerPlayer owner,
        JudgmentScope requestedScope,
        double requestedRadius
    ) {
        if (owner == null || owner.server == null) {
            return new SpawnFirewall.ExecutionResult(false, null);
        }
        return execute(
            entity, owner, requestedScope, requestedRadius,
            ServerRuntimeRegistry.runtime(owner.server).authorityView()
        );
    }

    public static SpawnFirewall.ExecutionResult execute(
        Entity entity,
        ServerPlayer owner,
        JudgmentScope requestedScope,
        double requestedRadius,
        AuthorityView authorityView
    ) {
        if (owner == null || owner.server == null) {
            return new SpawnFirewall.ExecutionResult(false, null);
        }
        if (entity != null && JudgmentState.isJudged(entity)) {
            SpawnFirewall.reassert(entity);
            return new SpawnFirewall.ExecutionResult(false, null);
        }
        var runtime = ServerRuntimeRegistry.runtime(owner.server);
        PreparedJudgment prepared = prepare(
            entity, owner, requestedScope, requestedRadius,
            runtime.commands().nextDecisionEpoch(), authorityView
        );
        if (prepared == null) {
            return new SpawnFirewall.ExecutionResult(false, null);
        }
        JudgmentBatch batch = new JudgmentBatch(
            UUID.randomUUID(), List.of(prepared.decision())
        );
        CommitReceipt receipt = runtime.commands().commit(runtime.commands().qualify(batch)
            .withIncarnations(java.util.Map.of(prepared.subject().uuid(), runtime.incarnationNonce(prepared.entity()))));
        return receipt.committed()
            ? applyCommitted(prepared, owner, authorityView, CommittedJudgmentGate.from(receipt))
            : new SpawnFirewall.ExecutionResult(false, null);
    }

    /**
     * Builds an immutable decision for a shared action epoch. This method has no
     * physical side effects and never writes Root, so a whole closure can be
     * validated before its single atomic batch commit.
     */
    static PreparedJudgment prepare(
        Entity entity,
        ServerPlayer owner,
        JudgmentScope requestedScope,
        double requestedRadius,
        DominionEpoch actionEpoch,
        AuthorityView authorityView
    ) {
        if (entity == null || owner == null || owner.server == null
            || actionEpoch == null || authorityView == null
            || !ExecutionTargetPolicy.isCombatObject(entity)
            || authorityView.isProtected(entity)) {
            return null;
        }
        boolean alreadyTerminal = JudgmentState.isJudged(entity);
        if (alreadyTerminal) {
            return null;
        }
        SubjectIdentity subject = MinecraftSubjectAdapter.from(entity);
        DominionDecision.Judgment decision = (DominionDecision.Judgment)
            LocalDominionCore.decide(new DominionRequest.Judgment(
                subject, actionEpoch,
                authorityView.isProtected(owner), alreadyTerminal,
                toContractScope(requestedScope), Math.max(0.0D, requestedRadius)
            ));
        if (!decision.committed()) {
            return null;
        }

        JudgmentScope scope = toEngineScope(decision.scope());
        return new PreparedJudgment(
            entity, subject, decision,
            JudgmentMark.from(entity, scope, decision.radius())
        );
    }

    /** Applies only a decision proven committed by the caller's Root receipt. */
    static SpawnFirewall.ExecutionResult applyCommitted(
        PreparedJudgment prepared,
        ServerPlayer owner,
        AuthorityView authorityView,
        CommittedJudgmentGate committed
    ) {
        if (prepared == null || owner == null || owner.server == null
            || authorityView == null || committed == null
            || authorityView.isProtected(prepared.entity())) {
            return new SpawnFirewall.ExecutionResult(false, null);
        }
        Entity entity = prepared.entity();
        var runtime = ServerRuntimeRegistry.runtime(owner.server);
        boolean[] entered = {false};
        boolean qualified = committed.runQualified(prepared.decision(), runtime.incarnationNonce(entity),
            runtime.commands()::subjectQualification, () -> {
            entered[0] = runtime.terminalPlans().execute(entity, authorityView);
            committed.runQualified(prepared.decision(), runtime.incarnationNonce(entity), runtime.commands()::subjectQualification,
                () -> runtime.terminalWatch().track(prepared.mark(), entity));
        });
        boolean applied = qualified && entered[0] && committed.runQualified(prepared.decision(), runtime.incarnationNonce(entity),
            runtime.commands()::subjectQualification, () -> {});
        return new SpawnFirewall.ExecutionResult(applied, applied ? prepared.mark() : null);
    }

    record PreparedJudgment(
        Entity entity,
        SubjectIdentity subject,
        DominionDecision.Judgment decision,
        JudgmentMark mark
    ) {}

    private static DominionDecision.LineageScope toContractScope(JudgmentScope scope) {
        return switch (scope) {
            case IDENTITY -> DominionDecision.LineageScope.IDENTITY;
            case LOCAL_LINEAGE -> DominionDecision.LineageScope.LOCAL_LINEAGE;
            case GLOBAL_LINEAGE -> DominionDecision.LineageScope.GLOBAL_LINEAGE;
        };
    }

    private static JudgmentScope toEngineScope(DominionDecision.LineageScope scope) {
        return switch (scope) {
            case IDENTITY -> JudgmentScope.IDENTITY;
            case LOCAL_LINEAGE -> JudgmentScope.LOCAL_LINEAGE;
            case GLOBAL_LINEAGE -> JudgmentScope.GLOBAL_LINEAGE;
        };
    }
}
