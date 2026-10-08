package dev.ronova.coremod.authority;

import dev.ronova.coremod.contract.Capability;
import dev.ronova.coremod.contract.CapabilitySet;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.contract.SubjectIdentity;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/** 7.14B Authority read/write separation and per-server wiring gate. */
public final class AuthoritySplitSelfTest {
    private static final String PREFIX = "dev/ronova/coremod/";
    private static final String VIEW = PREFIX + "authority/AuthorityView";
    private static final String BINDER = PREFIX + "authority/AuthorityBinder";
    private static final String GUARD = PREFIX + "engine/AuthorityGuard";
    private static final String ENFORCEMENT = PREFIX + "engine/AuthorityEnforcement";
    private static final String REGISTRY = PREFIX + "bootstrap/ServerRuntimeRegistry";
    private static final String RUNTIME = PREFIX + "runtime/DominionServerRuntime";
    private static final String COMPOSITION = PREFIX + "bootstrap/DominionCompositionRoot";

    private AuthoritySplitSelfTest() {}

    public static void main(String[] args) throws Exception {
        verifiesFlagResolution();
        verifiesPureProjectionQuery();
        verifiesCompositionWiring();
        verifiesHotPathBoundaries();
        verifiesLegacyRollbackIsolation();
        System.out.println("AuthoritySplitSelfTest: PASS");
    }

    private static void verifiesFlagResolution() {
        AuthoritySplitFlags.Selection defaults = AuthoritySplitFlags.resolve(
            ignored -> null, ignored -> null
        );
        require(defaults.enabled() && defaults.source() == AuthoritySplitFlags.Source.DEFAULT,
            "7.14B Authority split must default to enabled");

        AuthoritySplitFlags.Selection environment = AuthoritySplitFlags.resolve(
            ignored -> null, ignored -> " TrUe "
        );
        require(environment.enabled()
                && environment.source() == AuthoritySplitFlags.Source.ENVIRONMENT,
            "valid environment Authority flag was not selected");

        AuthoritySplitFlags.Selection property = AuthoritySplitFlags.resolve(
            ignored -> " false ", ignored -> "true"
        );
        require(!property.enabled()
                && property.source() == AuthoritySplitFlags.Source.JVM_PROPERTY
                && property.hasWarning(),
            "JVM property must override a conflicting environment Authority flag");

        AuthoritySplitFlags.Selection invalid = AuthoritySplitFlags.resolve(
            ignored -> "enabled", ignored -> "true"
        );
        require(!invalid.enabled()
                && invalid.source() == AuthoritySplitFlags.Source.INVALID_JVM_PROPERTY,
            "invalid Authority property must select the explicit legacy rollback");
    }

    private static void verifiesPureProjectionQuery() {
        UUID ownerUuid = UUID.randomUUID();
        DominionEpoch epoch = new DominionEpoch(3L, 4L);
        SubjectIdentity owner = subject(ownerUuid, "minecraft:player", true);
        DominionDecision.Authority authority = new DominionDecision.Authority(
            owner, epoch, true, CapabilitySet.OWNER_DEFAULT, "test-owner"
        );
        dev.ronova.coremod.projection.ProjectionCoordinator coordinator =
            new dev.ronova.coremod.projection.ProjectionCoordinator();
        coordinator.publish(new DominionSnapshot(epoch, List.of(authority)));
        AuthorityView view = new AuthorityView(coordinator::published);
        require(view.hasCapability(ownerUuid, Capability.FLIGHT),
            "projection view lost projected owner capability");
        require(view.hasCapability(ownerUuid, Capability.INSTANT_MINING),
            "projection view lost projected mining capability");
        require(view.owns(ownerUuid), "projection view lost owner membership");
        require(view.epoch().equals(epoch), "projection view lost snapshot epoch");
    }

    private static void verifiesCompositionWiring() throws Exception {
        ClassNode composition = read(COMPOSITION);
        require(countNew(composition, ENFORCEMENT) == 1,
            "composition root must create one AuthorityEnforcement");
        require(countCalls(composition, VIEW, "attach") == 1,
            "composition root must attach one selected AuthorityView");
        ClassNode runtime = read(RUNTIME);
        require(countCalls(runtime, VIEW, null) == 0,
            "runtime must retain the supplied view rather than recreate it");
        require(hasMethod(runtime, "authorityView"),
            "server runtime must expose the composed AuthorityView");
        require(hasMethod(runtime, "authorityEnforcement"),
            "server runtime must expose the composed AuthorityEnforcement");
    }

    private static void verifiesHotPathBoundaries() throws Exception {
        for (String owner : Set.of(
            PREFIX + "engine/TargetDiscovery",
            PREFIX + "engine/JudgmentEngine",
            PREFIX + "engine/JudgmentExecutor",
            PREFIX + "engine/TerminalLifecycle",
            PREFIX + "engine/QuarantineView",
            PREFIX + "engine/ServerQuarantineView",
            PREFIX + "engine/ClientQuarantineView",
            PREFIX + "engine/spawn/SpawnAdmissionRuntime",
            PREFIX + "network/ServerActionService",
            PREFIX + "mixin/LivingEntityProtectionMixin",
            PREFIX + "mixin/EntityProtectionMixin",
            PREFIX + "mixin/ConnectionProtectionMixin",
            PREFIX + "mixin/ServerConnectionProtectionMixin"
        )) {
            ClassNode node = read(owner);
            require(countCalls(node, BINDER, null) == 0,
                owner + " leaked AuthorityBinder into a read/enforcement path");
            require(countCalls(node, GUARD, null) == 0,
                owner + " leaked the compatibility AuthorityGuard facade");
        }
        ClassNode events = read(PREFIX + "event/DominionEvents");
        require(countCalls(events, GUARD, null) == 0,
            "Forge events must not re-enter the compatibility AuthorityGuard facade");
        require(countCalls(events, BINDER, "restore") >= 2
                && countCalls(events, BINDER, "releaseObject") == 1,
            "Forge events must keep restore/release as explicit lifecycle mutations");

        ClassNode action = read(PREFIX + "network/ServerActionService");
        require(countCalls(action, VIEW, "hasCapability") >= 1,
            "action service must gate capabilities through AuthorityView");
        ClassNode discovery = read(PREFIX + "engine/TargetDiscovery");
        require(countCalls(discovery, VIEW, "forEntity") == 0
                && countCalls(discovery, VIEW, "forPlayer") == 0,
            "TargetDiscovery must consume an explicit server view");
        ClassNode judgment = read(PREFIX + "engine/JudgmentEngine");
        require(countCalls(judgment, VIEW, "forEntity") == 0
                && countCalls(judgment, VIEW, "forPlayer") == 0,
            "JudgmentEngine must consume an explicit server view");
    }

    private static void verifiesLegacyRollbackIsolation() throws Exception {
        ClassNode legacy = read(PREFIX + "authority/LegacyAuthorityView");
        require(countCalls(legacy, BINDER, "restoreLegacyProjection") == 1,
            "legacy mode must isolate its compatibility synchronization in LegacyAuthorityView");
        ClassNode view = read(VIEW);
        require(countCalls(view, BINDER, null) == 0,
            "AuthorityView must never call AuthorityBinder");
        ClassNode enforcement = read(ENFORCEMENT);
        require(countCalls(enforcement, VIEW, "resolve") >= 1,
            "AuthorityEnforcement must derive decisions from AuthorityView");
        require(countCalls(enforcement, BINDER, "releaseObject") == 1,
            "only explicit closing cleanup may release an Authority object");
    }

    private static SubjectIdentity subject(UUID uuid, String type, boolean player) {
        return new SubjectIdentity(
            uuid, type, "test." + type.substring(type.indexOf(':') + 1),
            "minecraft:overworld", 1.0D, 2.0D, 3.0D, player
        );
    }

    private static ClassNode read(String internalName) throws Exception {
        try (InputStream input = AuthoritySplitSelfTest.class.getClassLoader()
            .getResourceAsStream(internalName + ".class")) {
            require(input != null, "missing production class: " + internalName);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static int countNew(ClassNode node, String descriptor) {
        int count = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof TypeInsnNode type
                    && type.getOpcode() == org.objectweb.asm.Opcodes.NEW
                    && descriptor.equals(type.desc)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countCalls(ClassNode node, String owner, String name) {
        int count = 0;
        for (MethodNode method : node.methods) {
            count += countCalls(method, owner, name);
        }
        return count;
    }

    private static int countCalls(MethodNode method, String owner, String name) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                && owner.equals(call.owner)
                && (name == null || name.equals(call.name))) {
                count++;
            }
        }
        return count;
    }

    private static boolean hasMethod(ClassNode node, String name) {
        return node.methods.stream().anyMatch(method -> name.equals(method.name));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
