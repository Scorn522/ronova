package dev.ronova.coremod.presence;

import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.SubjectIdentity;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * 7.14C presence split gate.
 *
 * <p>The one invariant this test exists for: immunity belongs to
 * {@link PresencePolicy}, not to a caller short-circuit. Every assertion that calls
 * {@code decide} calls it directly, exactly as a future caller that forgot the old
 * {@code isProtected} guard would.</p>
 */
public final class PresenceSplitSelfTest {
    private static final String PREFIX = "dev/ronova/coremod/";
    private static final String POLICY = PREFIX + "presence/PresencePolicy";
    private static final String FLAGS = PREFIX + "presence/PresenceSplitFlags";
    private static final String LEGACY_PRESENCE = PREFIX + "projection/PresenceProjection";
    private static final String FACADE = PREFIX + "engine/QuarantineView";
    private static final String SERVER = PREFIX + "engine/ServerQuarantineView";
    private static final String CLIENT = PREFIX + "engine/ClientQuarantineView";
    private static final String LEGACY_VIEW = PREFIX + "engine/LegacyQuarantineView";
    private static final String AUTHORITY_VIEW = PREFIX + "authority/AuthorityView";
    private static final String BUNDLE = PREFIX + "projection/ProjectionBundle";

    private PresenceSplitSelfTest() {}

    public static void main(String[] args) throws Exception {
        verifiesFlagResolution();
        verifiesPolicyOwnedOwnerImmunity();
        verifiesDominionSelfImmunity();
        verifiesFailStaticOnUnresolvedOwners();
        verifiesQuarantineStillApplies();
        verifiesMetrics();
        verifiesBundleCarriesPolicy();
        verifiesViewSplit();
        verifiesLegacyRollbackIsolation();
        System.out.println("PresenceSplitSelfTest: PASS");
    }

    private static void verifiesFlagResolution() {
        PresenceSplitFlags.Selection defaults = PresenceSplitFlags.resolve(
            ignored -> null, ignored -> null
        );
        require(defaults.enabled() && defaults.source() == PresenceSplitFlags.Source.DEFAULT,
            "7.14C presence split must default to enabled");

        PresenceSplitFlags.Selection environment = PresenceSplitFlags.resolve(
            ignored -> null, ignored -> " TrUe "
        );
        require(environment.enabled()
                && environment.source() == PresenceSplitFlags.Source.ENVIRONMENT,
            "valid environment presence flag was not selected");

        PresenceSplitFlags.Selection property = PresenceSplitFlags.resolve(
            ignored -> " false ", ignored -> "true"
        );
        require(!property.enabled()
                && property.source() == PresenceSplitFlags.Source.JVM_PROPERTY
                && property.hasWarning(),
            "JVM property must override a conflicting environment presence flag");

        PresenceSplitFlags.Selection invalid = PresenceSplitFlags.resolve(
            ignored -> "enabled", ignored -> "true"
        );
        require(!invalid.enabled()
                && invalid.source() == PresenceSplitFlags.Source.INVALID_JVM_PROPERTY,
            "invalid presence property must select the explicit legacy rollback");
    }

    private static void verifiesPolicyOwnedOwnerImmunity() {
        UUID ownerUuid = UUID.randomUUID();
        PresencePolicy policy = new PresencePolicy(OwnerImmunity.ofOwners(Set.of(ownerUuid)));
        DominionDecision.Presence decision = policy.decide(
            subject(ownerUuid, "minecraft:player", true), epoch(), true
        );
        require(decision.present(),
            "a quarantined owner must stay present without any caller short-circuit");
        require(OwnerImmunity.Kind.OWNER.reason().equals(decision.reason()),
            "owner immunity must be attributed to the policy, got " + decision.reason());
    }

    private static void verifiesDominionSelfImmunity() {
        PresencePolicy policy = new PresencePolicy(OwnerImmunity.ofOwners(Set.of()));
        DominionDecision.Presence decision = policy.decide(
            subject(UUID.randomUUID(), "ronova:blade_projection", false), epoch(), true
        );
        require(decision.present(),
            "this mod's own entities must never be filtered out by presence");
        require(OwnerImmunity.Kind.DOMINION_SELF.reason().equals(decision.reason()),
            "Dominion-self immunity must carry its own reason, got " + decision.reason());
    }

    private static void verifiesFailStaticOnUnresolvedOwners() {
        PresencePolicy policy = new PresencePolicy(OwnerImmunity.unresolved());
        DominionDecision.Presence decision = policy.decide(
            subject(UUID.randomUUID(), "minecraft:player", true), epoch(), true
        );
        require(decision.present(),
            "an unavailable owner registry must fail static to present");
        require(OwnerImmunity.Kind.UNRESOLVED.reason().equals(decision.reason()),
            "an unresolved owner lookup must be reported, got " + decision.reason());
        require(policy.unresolvedImmunities() == 1L,
            "unresolved owner lookups must be counted for diagnostics");
    }

    private static void verifiesQuarantineStillApplies() {
        PresencePolicy policy = new PresencePolicy(OwnerImmunity.ofOwners(Set.of()));
        SubjectIdentity stranger = subject(UUID.randomUUID(), "minecraft:zombie", false);
        require(!policy.decide(stranger, epoch(), true).present(),
            "presence must still hide a quarantined non-owner subject");
        require(policy.decide(stranger, epoch(), false).present(),
            "presence must not hide an unquarantined subject");
    }

    private static void verifiesMetrics() {
        UUID ownerUuid = UUID.randomUUID();
        PresencePolicy policy = new PresencePolicy(OwnerImmunity.ofOwners(Set.of(ownerUuid)));
        policy.decide(subject(ownerUuid, "minecraft:player", true), epoch(), true);
        policy.decide(subject(UUID.randomUUID(), "minecraft:zombie", false), epoch(), true);
        require(policy.evaluations() == 2L, "every decision must be counted");
        require(policy.immuneDecisions() == 1L, "immune decisions must be counted separately");
        require(policy.unresolvedImmunities() == 0L,
            "a healthy owner registry must not report unresolved lookups");
    }

    private static void verifiesBundleCarriesPolicy() throws Exception {
        ClassNode bundle = read(BUNDLE);
        require(bundle.methods.stream().anyMatch(method -> "presence".equals(method.name)
                && ("()L" + POLICY + ";").equals(method.desc)),
            "ProjectionBundle must publish the 7.14C PresencePolicy, not the legacy echo");
        ClassNode coordinator = read(PREFIX + "projection/ProjectionCoordinator");
        require(countNew(coordinator, POLICY) == 1,
            "the coordinator must build exactly one presence policy per projection");
        require(countCalls(coordinator, LEGACY_PRESENCE, null) == 0,
            "the coordinator must not retain the legacy presence projection");
    }

    private static void verifiesViewSplit() throws Exception {
        ClassNode facade = read(FACADE);
        require(countCalls(facade, POLICY, "decide") == 0,
            "the facade must delegate the decision rather than adjudicate it");
        require(countCalls(facade, SERVER, "matches") == 1
                && countCalls(facade, CLIENT, "matches") == 1,
            "the facade must dispatch to exactly one server half and one client half");
        require(countCalls(facade, FLAGS, "resolve") == 1,
            "the presence split mode must be resolved once by the facade");

        ClassNode server = read(SERVER);
        require(countCalls(server, POLICY, "decide") == 1,
            "the server half must own exactly one presence decision");
        require(countCalls(server, AUTHORITY_VIEW, null) == 0,
            "the server half must not short-circuit immunity through AuthorityView");
        require(countCalls(server, LEGACY_PRESENCE, null) == 0,
            "the server half must not reach the legacy presence projection");

        ClassNode client = read(CLIENT);
        require(countCalls(client, POLICY, "decide") == 1,
            "the client half must own exactly one presence decision");
        require(countCalls(client, LEGACY_PRESENCE, null) == 0,
            "the client half must not reach the legacy presence projection");
    }

    private static void verifiesLegacyRollbackIsolation() throws Exception {
        ClassNode legacy = read(LEGACY_VIEW);
        require(countCalls(legacy, LEGACY_PRESENCE, "decide") == 1,
            "the rollback path must isolate the legacy presence echo in one place");
        require(countCalls(legacy, AUTHORITY_VIEW, "isProtected") >= 1,
            "the rollback path must reproduce the former caller short-circuit");
        ClassNode facade = read(FACADE);
        require(countCalls(facade, LEGACY_VIEW, "matches") == 1,
            "the facade must reach the rollback path from exactly one branch");
        for (String owner : Set.of(FACADE, SERVER, CLIENT)) {
            require(countCalls(read(owner), LEGACY_PRESENCE, null) == 0,
                owner + " must not depend on the legacy presence projection");
        }
    }

    private static DominionEpoch epoch() {
        return new DominionEpoch(7L, 14L);
    }

    private static SubjectIdentity subject(UUID uuid, String type, boolean player) {
        return new SubjectIdentity(
            uuid, type, "test." + type.substring(type.indexOf(':') + 1),
            "minecraft:overworld", 1.0D, 2.0D, 3.0D, player
        );
    }

    private static ClassNode read(String internalName) throws Exception {
        try (InputStream input = PresenceSplitSelfTest.class.getClassLoader()
            .getResourceAsStream(internalName + ".class")) {
            require(input != null, "missing production class: " + internalName);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static int countCalls(ClassNode node, String owner, String name) {
        int count = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                    && owner.equals(call.owner)
                    && (name == null || name.equals(call.name))) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countNew(ClassNode node, String owner) {
        int count = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                    && owner.equals(call.owner)
                    && "<init>".equals(call.name)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
