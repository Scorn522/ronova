package dev.ronova.coremod.observation;

import dev.ronova.coremod.engine.ExecutionTargetPolicy;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

/** Packaged-bytecode gates for 7.14A raw observation separation. */
public final class RawObservationSeparationSelfTest {
    private static final String PREFIX = "dev/ronova/coremod/";
    private static final String FLAGS = PREFIX + "observation/RawObservationFlags";
    private static final String GRAPH = PREFIX + "observation/TopologyGraph";
    private static final String CORE = PREFIX + "observation/WeakTopologyCore";
    private static final String RAW_SOURCE = PREFIX + "observation/RawObservationSource";
    private static final String RAW_REF = PREFIX + "observation/RawEntityRef";
    private static final String LEGACY = PREFIX + "observation/LegacyTopologyObservation";
    private static final String OBSERVATION = PREFIX + "observation/TopologyObservation";
    private static final String POLICY = PREFIX + "engine/ExecutionTargetPolicy";
    private static final String STATE = PREFIX + "engine/JudgmentState";
    private static final String TOPOLOGY = PREFIX + "engine/TopologyIndex";
    private static final String JUDGMENT = PREFIX + "engine/JudgmentEngine";
    private static final String DISCOVERY = PREFIX + "engine/TargetDiscovery";
    private static final String TERMINAL = PREFIX + "engine/TerminalLifecycle";
    private static final String RUNTIME = PREFIX + "runtime/DominionServerRuntime";
    private static final String COMPOSITION = PREFIX + "bootstrap/DominionCompositionRoot";
    private static final String REGISTRY = PREFIX + "bootstrap/ServerRuntimeRegistry";

    private RawObservationSeparationSelfTest() {}

    public static void main(String[] args) throws Exception {
        Map<String, ClassNode> classes = productionClasses();
        verifiesFlagResolution(classes);
        verifiesPolicySplit(classes);
        verifiesRawDependencyBoundary(classes);
        verifiesLegacyIsolation(classes);
        verifiesPerServerWiring(classes);
        verifiesObservationDominance(classes);
        System.out.println("RawObservationSeparationSelfTest: PASS");
    }

    private static void verifiesFlagResolution(Map<String, ClassNode> classes) {
        RawObservationFlags.Selection defaults = RawObservationFlags.resolve(
            ignored -> null, ignored -> null
        );
        require(!defaults.enabled() && defaults.source() == RawObservationFlags.Source.DEFAULT,
            "raw observation must default to false");

        RawObservationFlags.Selection environment = RawObservationFlags.resolve(
            ignored -> null, ignored -> " TrUe "
        );
        require(environment.enabled()
                && environment.source() == RawObservationFlags.Source.ENVIRONMENT,
            "valid environment flag was not selected");

        RawObservationFlags.Selection property = RawObservationFlags.resolve(
            ignored -> " false ", ignored -> "true"
        );
        require(!property.enabled()
                && property.source() == RawObservationFlags.Source.JVM_PROPERTY
                && property.hasWarning(),
            "property precedence/conflict warning is incorrect");

        RawObservationFlags.Selection invalidProperty = RawObservationFlags.resolve(
            ignored -> "enabled", ignored -> "true"
        );
        require(!invalidProperty.enabled()
                && invalidProperty.source()
                    == RawObservationFlags.Source.INVALID_JVM_PROPERTY,
            "invalid property incorrectly fell through to the environment");

        RawObservationFlags.Selection invalidEnvironment = RawObservationFlags.resolve(
            ignored -> null, ignored -> "1"
        );
        require(!invalidEnvironment.enabled()
                && invalidEnvironment.source()
                    == RawObservationFlags.Source.INVALID_ENVIRONMENT,
            "invalid environment flag did not fail closed");

        for (ClassNode node : classes.values()) {
            if (FLAGS.equals(node.name)) {
                continue;
            }
            for (MethodNode method : node.methods) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof LdcInsnNode constant
                        && constant.cst instanceof String value) {
                        require(!RawObservationFlags.PROPERTY.equals(value)
                                && !RawObservationFlags.ENVIRONMENT.equals(value),
                            "7.14A flag input leaked outside RawObservationFlags: " + node.name);
                    }
                }
            }
        }
        require(countCalls(classes.get(COMPOSITION), FLAGS, "resolve") == 1,
            "composition root must resolve the 7.14A mode exactly once");
        for (String name : List.of(
            PREFIX + "engine/", PREFIX + "capability/", PREFIX + "terminal/"
        )) {
            for (ClassNode node : classes.values()) {
                if (node.name.startsWith(name)) {
                    require(countCalls(node, RUNTIME, "rawObservationEnabled") == 0,
                        "hot-path mode branch leaked into " + node.name);
                }
            }
        }
        FieldNode selection = field(classes.get(RUNTIME), "rawObservationSelection");
        require((selection.access & Opcodes.ACC_FINAL) != 0,
            "runtime mode selection must be immutable");
    }

    private static void verifiesPolicySplit(Map<String, ClassNode> classes) {
        ClassNode policy = classes.get(POLICY);
        MethodNode shouldIndex = method(policy, "shouldIndex");
        MethodNode shouldJudge = method(policy, "shouldJudge");
        require(countCalls(shouldIndex, POLICY, "isCombatObject") == 1,
            "shouldIndex lost combat-object classification");
        require(countCalls(shouldIndex, STATE, "isJudged") == 0,
            "shouldIndex is contaminated by terminal state");
        require(countCalls(shouldJudge, POLICY, "isCombatObject") == 1
                && countCalls(shouldJudge, STATE, "isJudged") == 1,
            "shouldJudge does not preserve the wide non-terminal policy");
        require(countCalls(shouldJudge, POLICY, "shouldIndex") == 0,
            "shouldJudge was incorrectly narrowed to shouldIndex");
        require(countCalls(classes.get(JUDGMENT), POLICY, "shouldJudge") >= 1,
            "JudgmentEngine does not invoke shouldJudge");
        require(countCalls(classes.get(DISCOVERY), POLICY, "shouldJudge") >= 1,
            "TargetDiscovery does not invoke shouldJudge");
    }

    private static void verifiesRawDependencyBoundary(Map<String, ClassNode> classes) {
        List<String> rawClasses = List.of(GRAPH, CORE, RAW_SOURCE, RAW_REF);
        List<String> forbidden = List.of(
            STATE,
            TERMINAL,
            PREFIX + "engine/QuarantineView",
            PREFIX + "engine/ServerQuarantineView",
            PREFIX + "engine/ClientQuarantineView",
            PREFIX + "engine/LegacyQuarantineView",
            PREFIX + "terminal/",
            PREFIX + "presence/",
            PREFIX + "projection/PresenceProjection"
        );
        for (String name : rawClasses) {
            ClassNode node = classes.get(name);
            require(node != null, "missing raw production class: " + name);
            Set<String> references = references(node);
            for (String denied : forbidden) {
                require(references.stream().noneMatch(value -> value.startsWith(denied)),
                    name + " depends on forbidden raw-layer type " + denied);
            }
            require(countCalls(node, POLICY, "shouldJudge") == 0,
                name + " invokes Judgment eligibility from the raw layer");
        }
        require(countCalls(classes.get(GRAPH), POLICY, "shouldIndex") == 1,
            "TopologyGraph must invoke only shouldIndex for raw eligibility");

        Set<String> discoveryReferences = references(classes.get(DISCOVERY));
        for (String denied : List.of(
            PREFIX + "engine/QuarantineView",
            PREFIX + "engine/ServerQuarantineView",
            PREFIX + "engine/ClientQuarantineView",
            PREFIX + "engine/LegacyQuarantineView",
            PREFIX + "projection/PresenceProjection",
            PREFIX + "presence/"
        )) {
            require(discoveryReferences.stream().noneMatch(value -> value.startsWith(denied)),
                "TargetDiscovery depends on presence filtering: " + denied);
        }
    }

    private static void verifiesLegacyIsolation(Map<String, ClassNode> classes) {
        for (ClassNode node : classes.values()) {
            if (TOPOLOGY.equals(node.name)) {
                continue;
            }
            int calls = countCalls(node, TOPOLOGY, null);
            if (LEGACY.equals(node.name)) {
                require(calls >= 1, "legacy adapter does not bridge TopologyIndex");
            } else {
                require(calls == 0, "TopologyIndex caller escaped legacy adapter: " + node.name);
            }
        }
        require(countCalls(classes.get(TERMINAL), TOPOLOGY, "forget") == 0,
            "TerminalLifecycle still calls TopologyIndex.forget directly");
        require(countCalls(classes.get(LEGACY), TOPOLOGY, "forget") == 1,
            "legacy adapter must be the sole TopologyIndex.forget bridge");
        // 7.16+ durable terminal-watch takeover deliberately keeps raw observation
        // knowledge after terminal completion. The historical runtime dispatcher
        // remains as a deprecated no-op seam, but no production path may call it.
        for (ClassNode node : classes.values()) {
            int calls = countCalls(node, RUNTIME, "legacyForgetOnTerminal");
            require(calls == 0,
                "7.16+ production path reintroduced legacy terminal forget: " + node.name);
        }
    }

    private static void verifiesPerServerWiring(Map<String, ClassNode> classes) {
        ClassNode composition = classes.get(COMPOSITION);
        require(countNew(composition, RAW_SOURCE) == 1
                && countNew(composition, LEGACY) == 1,
            "composition root does not select both observation implementations");
        require(countNew(composition, PREFIX + "terminal/DurableTerminalWatchSet") == 1,
            "composition root does not create the durable terminal watch");
        require(method(classes.get(RUNTIME), "observation") != null,
            "DominionServerRuntime does not expose observation()");
        MethodNode release = method(classes.get(REGISTRY), "release");
        require(countCalls(release, REGISTRY, "releaseResources") == 1,
            "registry release does not invoke failure-isolated resource cleanup");
        require(countCalls(classes.get(REGISTRY), PREFIX + "terminal/TerminalWatchSet", "clear") == 1,
            "registry release does not clear the server watch");
        require(countCalls(classes.get(REGISTRY), OBSERVATION, "clear") == 1,
            "registry release does not clear the server observation graph");
    }

    private static void verifiesObservationDominance(Map<String, ClassNode> classes) {
        ClassNode judgment = classes.get(JUDGMENT);
        MethodNode publicEntry = method(judgment, "judgeReport");
        require(countCalls(publicEntry, JUDGMENT, "canJudge") == 0,
            "judgeReport filters the root before the unified observation traversal");
        require(countCalls(publicEntry, JUDGMENT, "judgeRootsReport") == 1,
            "judgeReport does not delegate non-null roots to the unified traversal");
        for (String methodName : List.of("judgeRootsReport")) {
            MethodNode method = method(judgment, methodName);
            List<AbstractInsnNode> instructions = Arrays.asList(method.instructions.toArray());
            List<Integer> observeCalls = new ArrayList<>();
            List<Integer> eligibilityCalls = new ArrayList<>();
            for (int index = 0; index < instructions.size(); index++) {
                AbstractInsnNode instruction = instructions.get(index);
                if (instruction instanceof MethodInsnNode call) {
                    if (OBSERVATION.equals(call.owner) && "observe".equals(call.name)) {
                        observeCalls.add(index);
                    }
                    if (JUDGMENT.equals(call.owner) && "canJudge".equals(call.name)) {
                        eligibilityCalls.add(index);
                    }
                }
            }
            require(!observeCalls.isEmpty() && !eligibilityCalls.isEmpty(),
                methodName + " lacks observation or eligibility bytecode");
            List<Set<Integer>> dominators = dominators(method);
            for (int eligibility : eligibilityCalls) {
                require(observeCalls.stream().anyMatch(dominators.get(eligibility)::contains),
                    "observation does not dominate eligibility in " + methodName);
            }
        }
    }

    private static List<Set<Integer>> dominators(MethodNode method) {
        AbstractInsnNode[] instructions = method.instructions.toArray();
        int count = instructions.length;
        Map<LabelNode, Integer> labels = new HashMap<>();
        for (int index = 0; index < count; index++) {
            if (instructions[index] instanceof LabelNode label) {
                labels.put(label, index);
            }
        }
        List<Set<Integer>> successors = new ArrayList<>(count);
        List<Set<Integer>> predecessors = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            successors.add(new HashSet<>());
            predecessors.add(new HashSet<>());
        }
        for (int index = 0; index < count; index++) {
            AbstractInsnNode instruction = instructions[index];
            int opcode = instruction.getOpcode();
            if (instruction instanceof JumpInsnNode jump) {
                addSuccessor(successors, index, labels.get(jump.label));
                if (opcode != Opcodes.GOTO && opcode != Opcodes.JSR && index + 1 < count) {
                    addSuccessor(successors, index, index + 1);
                }
            } else if (instruction instanceof TableSwitchInsnNode table) {
                addSuccessor(successors, index, labels.get(table.dflt));
                for (LabelNode label : table.labels) {
                    addSuccessor(successors, index, labels.get(label));
                }
            } else if (instruction instanceof LookupSwitchInsnNode lookup) {
                addSuccessor(successors, index, labels.get(lookup.dflt));
                for (LabelNode label : lookup.labels) {
                    addSuccessor(successors, index, labels.get(label));
                }
            } else if (!isTerminalOpcode(opcode) && index + 1 < count) {
                addSuccessor(successors, index, index + 1);
            }
        }
        for (int from = 0; from < count; from++) {
            for (int to : successors.get(from)) {
                predecessors.get(to).add(from);
            }
        }

        Set<Integer> reachable = new HashSet<>();
        ArrayDeque<Integer> pending = new ArrayDeque<>();
        if (count > 0) {
            pending.add(0);
        }
        while (!pending.isEmpty()) {
            int current = pending.removeFirst();
            if (reachable.add(current)) {
                pending.addAll(successors.get(current));
            }
        }

        List<Set<Integer>> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            if (!reachable.contains(index)) {
                result.add(new HashSet<>());
            } else if (index == 0) {
                result.add(new HashSet<>(Set.of(0)));
            } else {
                result.add(new HashSet<>(reachable));
            }
        }
        boolean changed;
        do {
            changed = false;
            for (int index = 1; index < count; index++) {
                if (!reachable.contains(index)) {
                    continue;
                }
                Set<Integer> next = null;
                for (int predecessor : predecessors.get(index)) {
                    if (!reachable.contains(predecessor)) {
                        continue;
                    }
                    if (next == null) {
                        next = new HashSet<>(result.get(predecessor));
                    } else {
                        next.retainAll(result.get(predecessor));
                    }
                }
                if (next == null) {
                    next = new HashSet<>();
                }
                next.add(index);
                if (!next.equals(result.get(index))) {
                    result.set(index, next);
                    changed = true;
                }
            }
        } while (changed);
        return result;
    }

    private static void addSuccessor(
        List<Set<Integer>> successors, int from, Integer to
    ) {
        if (to != null) {
            successors.get(from).add(to);
        }
    }

    private static boolean isTerminalOpcode(int opcode) {
        return opcode == Opcodes.IRETURN || opcode == Opcodes.LRETURN
            || opcode == Opcodes.FRETURN || opcode == Opcodes.DRETURN
            || opcode == Opcodes.ARETURN || opcode == Opcodes.RETURN
            || opcode == Opcodes.ATHROW || opcode == Opcodes.RET;
    }

    private static Map<String, ClassNode> productionClasses() throws Exception {
        Path location = Path.of(TopologyObservation.class.getProtectionDomain()
            .getCodeSource().getLocation().toURI());
        Map<String, ClassNode> classes = new LinkedHashMap<>();
        if (Files.isRegularFile(location)) {
            try (JarFile jar = new JarFile(location.toFile())) {
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().startsWith(PREFIX)
                        && entry.getName().endsWith(".class")) {
                        try (InputStream input = jar.getInputStream(entry)) {
                            ClassNode node = new ClassNode();
                            new org.objectweb.asm.ClassReader(input).accept(node, 0);
                            classes.put(node.name, node);
                        }
                    }
                }
            }
        } else {
            Path root = location.resolve(PREFIX.replace('/', java.io.File.separatorChar));
            try (var paths = Files.walk(root)) {
                for (Path path : paths.filter(value -> value.toString().endsWith(".class")).toList()) {
                    try (InputStream input = Files.newInputStream(path)) {
                        ClassNode node = new ClassNode();
                        new org.objectweb.asm.ClassReader(input).accept(node, 0);
                        classes.put(node.name, node);
                    }
                }
            }
        }
        require(classes.containsKey(OBSERVATION),
            "production artifact could not be enumerated: " + location);
        return classes;
    }

    private static Set<String> references(ClassNode node) {
        Set<String> result = new HashSet<>();
        addReference(result, node.superName);
        result.addAll(node.interfaces);
        for (FieldNode field : node.fields) {
            addDescriptor(result, field.desc);
        }
        for (MethodNode method : node.methods) {
            addDescriptor(result, method.desc);
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof TypeInsnNode type) {
                    addReference(result, type.desc);
                } else if (instruction instanceof FieldInsnNode field) {
                    addReference(result, field.owner);
                    addDescriptor(result, field.desc);
                } else if (instruction instanceof MethodInsnNode call) {
                    addReference(result, call.owner);
                    addDescriptor(result, call.desc);
                } else if (instruction instanceof MultiANewArrayInsnNode array) {
                    addDescriptor(result, array.desc);
                } else if (instruction instanceof LdcInsnNode constant
                    && constant.cst instanceof Type type) {
                    addType(result, type);
                } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                    addDescriptor(result, dynamic.desc);
                    addHandle(result, dynamic.bsm);
                    for (Object argument : dynamic.bsmArgs) {
                        if (argument instanceof Handle handle) {
                            addHandle(result, handle);
                        } else if (argument instanceof Type type) {
                            addType(result, type);
                        }
                    }
                }
            }
        }
        return result;
    }

    private static void addHandle(Set<String> values, Handle handle) {
        addReference(values, handle.getOwner());
        addDescriptor(values, handle.getDesc());
    }

    private static void addDescriptor(Set<String> values, String descriptor) {
        if (descriptor == null) {
            return;
        }
        Type type = Type.getType(descriptor);
        if (type.getSort() == Type.METHOD) {
            addType(values, type.getReturnType());
            for (Type argument : type.getArgumentTypes()) {
                addType(values, argument);
            }
        } else {
            addType(values, type);
        }
    }

    private static void addType(Set<String> values, Type type) {
        if (type.getSort() == Type.ARRAY) {
            addType(values, type.getElementType());
        } else if (type.getSort() == Type.OBJECT) {
            addReference(values, type.getInternalName());
        }
    }

    private static void addReference(Set<String> values, String value) {
        if (value != null) {
            values.add(value);
        }
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
                && owner.equals(call.owner) && (name == null || name.equals(call.name))) {
                count++;
            }
        }
        return count;
    }

    private static int countNew(ClassNode node, String type) {
        int count = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof TypeInsnNode typeInstruction
                    && instruction.getOpcode() == Opcodes.NEW
                    && type.equals(typeInstruction.desc)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static MethodNode method(ClassNode node, String name) {
        require(node != null, "missing class for method " + name);
        for (MethodNode method : node.methods) {
            if (name.equals(method.name)) {
                return method;
            }
        }
        throw new AssertionError("missing method " + node.name + "." + name);
    }

    private static FieldNode field(ClassNode node, String name) {
        require(node != null, "missing class for field " + name);
        for (FieldNode field : node.fields) {
            if (name.equals(field.name)) {
                return field;
            }
        }
        throw new AssertionError("missing field " + node.name + "." + name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
