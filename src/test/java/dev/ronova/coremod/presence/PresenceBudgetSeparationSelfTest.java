package dev.ronova.coremod.presence;

import dev.ronova.coremod.authority.AuthorityView;
import dev.ronova.coremod.budget.BudgetCategory;
import dev.ronova.coremod.budget.BudgetTelemetrySnapshot;
import dev.ronova.coremod.budget.FrameBudgetGovernor;
import dev.ronova.coremod.contract.CapabilitySet;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.contract.SubjectIdentity;
import dev.ronova.coremod.projection.ProjectionCoordinator;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** 7.14D proof that synchronous Authority/Presence queries stay synchronous. */
public final class PresenceBudgetSeparationSelfTest {
    private static final String WORK_BUDGET = "dev/ronova/coremod/budget/WorkBudget";
    private static final String RUNTIME_COORDINATOR = "dev/ronova/coremod/runtime/RuntimeCoordinator";

    private PresenceBudgetSeparationSelfTest() {}

    public static void main(String[] args) throws Exception {
        verifiesSynchronousAnswersSurviveExhaustion();
        verifiesTelemetryIsMeasurementOnly();
        verifiesNoDeferredSchedulerDependency();
        System.out.println("PresenceBudgetSeparationSelfTest: PASS");
    }

    private static void verifiesSynchronousAnswersSurviveExhaustion() {
        UUID ownerUuid = UUID.randomUUID();
        SubjectIdentity owner = subject(ownerUuid, "minecraft:player", true);
        DominionEpoch epoch = new DominionEpoch(2L, 1L);
        DominionDecision.Authority authority = new DominionDecision.Authority(
            owner, epoch, true, CapabilitySet.OWNER_DEFAULT, "test-owner"
        );
        ProjectionCoordinator coordinator = new ProjectionCoordinator();
        coordinator.publish(new DominionSnapshot(epoch, List.of(authority)));

        AuthorityView baselineView = new AuthorityView(coordinator::published);
        FrameBudgetGovernor governor = new FrameBudgetGovernor(true);
        governor.beginTick(1L);
        exhaustTerminalBudget(governor);
        AuthorityView exhaustedView = new AuthorityView(coordinator::published, governor);

        DominionDecision.Authority baselineAuthority = baselineView.resolve(owner);
        DominionDecision.Authority exhaustedAuthority = exhaustedView.resolve(owner);
        require(baselineAuthority.equals(exhaustedAuthority)
                && exhaustedAuthority.authorized(),
            "Authority answer changed after deferred-work budget exhaustion");

        OwnerImmunity immunity = OwnerImmunity.ofOwners(Set.of(ownerUuid));
        PresencePolicy baselinePolicy = new PresencePolicy(immunity);
        PresencePolicy exhaustedPolicy = new PresencePolicy(immunity, governor);
        SubjectIdentity stranger = subject(UUID.randomUUID(), "minecraft:zombie", false);
        require(baselinePolicy.decide(owner, epoch, true)
                .equals(exhaustedPolicy.decide(owner, epoch, true)),
            "owner Presence answer changed after budget exhaustion");
        require(baselinePolicy.decide(stranger, epoch, true)
                .equals(exhaustedPolicy.decide(stranger, epoch, true)),
            "quarantined stranger Presence answer changed after budget exhaustion");
        require(!exhaustedPolicy.decide(stranger, epoch, true).present(),
            "budget exhaustion incorrectly made a quarantined stranger visible");
    }

    private static void verifiesTelemetryIsMeasurementOnly() {
        FrameBudgetGovernor governor = new FrameBudgetGovernor(true);
        governor.beginTick(7L);
        exhaustTerminalBudget(governor);
        PresencePolicy policy = new PresencePolicy(
            OwnerImmunity.ofOwners(Set.of()), governor
        );
        SubjectIdentity stranger = subject(UUID.randomUUID(), "minecraft:zombie", false);
        DominionDecision.Presence decision = policy.decide(
            stranger, new DominionEpoch(7L, 1L), false
        );
        require(decision.present(), "measurement test changed a visible Presence answer");
        BudgetTelemetrySnapshot snapshot = governor.snapshot();
        require(snapshot.synchronousQueries()
                .get(BudgetTelemetrySnapshot.QueryKind.PRESENCE_QUERY).callCount() == 1L,
            "Presence query was not counted as synchronous telemetry");
        require(snapshot.categoryReceipts().get(BudgetCategory.TERMINAL).deferred() == 1L,
            "test did not actually exhaust the deferred-work budget");
    }

    private static void verifiesNoDeferredSchedulerDependency() throws Exception {
        for (String internalName : List.of(
            "dev/ronova/coremod/authority/AuthorityView",
            "dev/ronova/coremod/presence/PresencePolicy"
        )) {
            ClassNode node = read(internalName);
            for (MethodNode method : node.methods) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode call) {
                        require(!WORK_BUDGET.equals(call.owner)
                                && !RUNTIME_COORDINATOR.equals(call.owner),
                            internalName + " depends on a deferred scheduler path");
                    }
                }
            }
        }
    }

    private static void exhaustTerminalBudget(FrameBudgetGovernor governor) {
        var budget = governor.budget(BudgetCategory.TERMINAL);
        long reserved = 0L;
        while (budget.tryConsumeCount(1)) {
            reserved++;
        }
        budget.deferCount(1);
        budget.finish(reserved, 1L, reserved, 1L);
        require(budget.receipt().deferred() == 1L,
            "terminal fixture did not record its deferred unit");
    }

    private static SubjectIdentity subject(UUID uuid, String type, boolean player) {
        return new SubjectIdentity(
            uuid, type, "test." + type.substring(type.indexOf(':') + 1),
            "minecraft:overworld", 1.0D, 2.0D, 3.0D, player
        );
    }

    private static ClassNode read(String internalName) throws Exception {
        try (InputStream input = PresenceBudgetSeparationSelfTest.class.getClassLoader()
            .getResourceAsStream(internalName + ".class")) {
            require(input != null, "missing production class: " + internalName);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
