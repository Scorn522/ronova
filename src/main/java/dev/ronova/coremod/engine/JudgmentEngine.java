package dev.ronova.coremod.engine;

import dev.ronova.coremod.authority.AuthorityView;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.observation.TopologyObservation;
import dev.ronova.coremod.runtime.DominionServerRuntime;
import dev.ronova.coremod.root.api.CommitReceipt;
import dev.ronova.coremod.root.api.JudgmentBatch;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.entity.PartEntity;

public final class JudgmentEngine {
    public static final double PRIMARY_RANGE = 64.0D;
    public static final double AREA_RADIUS = 64.0D;
    private JudgmentEngine() {}

    public static int judge(Entity target, ServerPlayer owner) {
        return judgeReport(target, owner, JudgmentScope.LOCAL_LINEAGE, 8.0D).judged();
    }

    public static int rayJudgment(ServerPlayer owner, double range) {
        return rayJudgmentReport(owner, range).judged();
    }

    public static JudgmentReport rayJudgmentReport(ServerPlayer owner, double range) {
        if (owner == null || owner.server == null) {
            return new JudgmentReport(0, 0, List.of());
        }
        return rayJudgmentReport(
            owner, range, ServerRuntimeRegistry.runtime(owner.server).authorityView()
        );
    }

    public static JudgmentReport rayJudgmentReport(
        ServerPlayer owner,
        double range,
        AuthorityView authorityView
    ) {
        return rayJudgmentReport(owner, range, authorityView, null, "");
    }

    /** Sequenced ingress variant whose Root command identity survives restart. */
    public static JudgmentReport rayJudgmentReport(
        ServerPlayer owner,
        double range,
        AuthorityView authorityView,
        UUID commandId
    ) {
        return rayJudgmentReport(owner, range, authorityView, commandId, "");
    }

    public static JudgmentReport rayJudgmentReport(
        ServerPlayer owner,
        double range,
        AuthorityView authorityView,
        UUID commandId,
        String commandDomain
    ) {
        TargetDiscovery.Discovery discovery = TargetDiscovery.ray(owner, range, authorityView);
        return judgeRootsReport(
            discovery.roots(), owner, JudgmentScope.LOCAL_LINEAGE, 8.0D,
            authorityView, commandId, commandDomain
        ).withCandidates(discovery.candidates());
    }

    public static int areaJudgment(ServerPlayer owner, double radius) {
        return areaJudgmentReport(owner, radius).judged();
    }

    public static JudgmentReport areaJudgmentReport(ServerPlayer owner, double radius) {
        if (owner == null || owner.server == null) {
            return new JudgmentReport(0, 0, List.of());
        }
        return areaJudgmentReport(
            owner, radius, ServerRuntimeRegistry.runtime(owner.server).authorityView()
        );
    }

    public static JudgmentReport areaJudgmentReport(
        ServerPlayer owner,
        double radius,
        AuthorityView authorityView
    ) {
        return areaJudgmentReport(owner, radius, authorityView, null, "");
    }

    /** Sequenced ingress variant whose Root command identity survives restart. */
    public static JudgmentReport areaJudgmentReport(
        ServerPlayer owner,
        double radius,
        AuthorityView authorityView,
        UUID commandId
    ) {
        return areaJudgmentReport(owner, radius, authorityView, commandId, "");
    }

    public static JudgmentReport areaJudgmentReport(
        ServerPlayer owner,
        double radius,
        AuthorityView authorityView,
        UUID commandId,
        String commandDomain
    ) {
        double boundedRadius = Math.min(AREA_RADIUS, Math.max(0.0D, radius));
        TargetDiscovery.Discovery discovery = TargetDiscovery.area(
            owner, boundedRadius, authorityView
        );
        return judgeRootsReport(
            discovery.roots(), owner, JudgmentScope.LOCAL_LINEAGE, boundedRadius,
            authorityView, commandId, commandDomain
        ).withCandidates(discovery.candidates());
    }

    public static int loadedWorldJudgment(ServerPlayer owner) {
        return loadedWorldJudgmentReport(owner).judged();
    }

    public static JudgmentReport loadedWorldJudgmentReport(ServerPlayer owner) {
        if (owner == null || owner.server == null) {
            return new JudgmentReport(0, 0, List.of());
        }
        return loadedWorldJudgmentReport(
            owner, ServerRuntimeRegistry.runtime(owner.server).authorityView()
        );
    }

    public static JudgmentReport loadedWorldJudgmentReport(
        ServerPlayer owner,
        AuthorityView authorityView
    ) {
        return loadedWorldJudgmentReport(owner, authorityView, null, "");
    }

    /** Sequenced ingress variant whose Root command identity survives restart. */
    public static JudgmentReport loadedWorldJudgmentReport(
        ServerPlayer owner,
        AuthorityView authorityView,
        UUID commandId
    ) {
        return loadedWorldJudgmentReport(owner, authorityView, commandId, "");
    }

    public static JudgmentReport loadedWorldJudgmentReport(
        ServerPlayer owner,
        AuthorityView authorityView,
        UUID commandId,
        String commandDomain
    ) {
        TargetDiscovery.Discovery discovery = TargetDiscovery.loaded(owner, authorityView);
        return judgeRootsReport(
            discovery.roots(), owner, JudgmentScope.GLOBAL_LINEAGE, 0.0D,
            authorityView, commandId, commandDomain
        ).withCandidates(discovery.candidates());
    }

    public static int judge(Entity root, ServerPlayer owner, JudgmentScope scope, double radius) {
        return judgeReport(root, owner, scope, radius).judged();
    }

    public static JudgmentReport judgeReport(Entity root, ServerPlayer owner, JudgmentScope scope, double radius) {
        if (root == null || owner == null || owner.server == null) {
            return new JudgmentReport(root == null ? 0 : 1, 0, List.of());
        }
        return judgeRootsReport(
            List.of(root), owner, scope, radius,
            ServerRuntimeRegistry.runtime(owner.server).authorityView(), null, ""
        );
    }

    public static JudgmentReport judgeReport(
        Entity root,
        ServerPlayer owner,
        JudgmentScope scope,
        double radius,
        AuthorityView authorityView
    ) {
        if (root == null || owner == null || owner.server == null || authorityView == null) {
            return new JudgmentReport(root == null ? 0 : 1, 0, List.of());
        }
        return judgeRootsReport(
            List.of(root), owner, scope, radius, authorityView, null, ""
        );
    }

    private static JudgmentReport judgeRootsReport(
        Iterable<? extends Entity> roots,
        ServerPlayer owner,
        JudgmentScope scope,
        double radius,
        AuthorityView authorityView,
        UUID commandId,
        String commandDomain
    ) {
        if (roots == null || owner == null || authorityView == null) {
            return new JudgmentReport(0, 0, List.of());
        }

        DominionServerRuntime runtime = ServerRuntimeRegistry.runtime(owner.server);
        TopologyObservation observation = runtime.observation();
        Set<Entity> encountered = identitySet();
        Set<Entity> closure = identitySet();
        ArrayDeque<Entity> pending = new ArrayDeque<>();
        for (Entity root : roots) {
            if (root != null) {
                pending.add(root);
            }
        }
        while (!pending.isEmpty()) {
            Entity current = pending.removeFirst();
            if (current == null || !encountered.add(current)) {
                continue;
            }
            observation.observe(current);
            pending.addAll(current.getPassengers());
            if (current.getVehicle() != null) {
                pending.add(current.getVehicle());
            }
            if (current instanceof PartEntity<?> part && part.getParent() != null) {
                pending.add(part.getParent());
            }
            if (current.isMultipartEntity() && current.getParts() != null) {
                Collections.addAll(pending, current.getParts());
            }
            pending.addAll(observation.related(current));
            if (current == owner || !canJudge(current, authorityView)) {
                continue;
            }
            closure.add(current);
        }

        int count = 0;
        List<JudgmentMark> marks = new ArrayList<>();
        try {
            DominionEpoch actionEpoch = runtime.commands().nextDecisionEpoch();
            LinkedHashMap<UUID, JudgmentExecutor.PreparedJudgment> prepared =
                new LinkedHashMap<>();
            for (Entity entity : closure) {
                if (authorityView.isProtected(entity)) {
                    continue;
                }
                if (JudgmentState.isJudged(entity)) {
                    SpawnFirewall.reassert(entity);
                    continue;
                }
                JudgmentExecutor.PreparedJudgment value = JudgmentExecutor.prepare(
                    entity, owner, scope, radius, actionEpoch, authorityView
                );
                if (value != null && value.subject().uuid() != null) {
                    JudgmentExecutor.PreparedJudgment duplicate = prepared.putIfAbsent(value.subject().uuid(), value);
                    if (duplicate != null && duplicate.entity() != value.entity()) {
                        return new JudgmentReport(closure.size(), 0, List.of(), false);
                    }
                }
            }
            if (prepared.isEmpty()) {
                if (commandId != null) {
                    CommitReceipt emptyReceipt = runtime.commands().commit(
                        runtime.commands().qualify(JudgmentBatch.empty(commandId, commandDomain))
                    );
                    if (emptyReceipt.unknown()) {
                        return new JudgmentReport(closure.size(), 0, List.of(), false);
                    }
                    if (!emptyReceipt.committed()) {
                        return new JudgmentReport(closure.size(), 0, List.of());
                    }
                }
                return new JudgmentReport(closure.size(), 0, List.of());
            }
            JudgmentBatch batch = new JudgmentBatch(
                commandId == null ? UUID.randomUUID() : commandId,
                commandId == null ? "" : commandDomain,
                prepared.values().stream().map(
                    JudgmentExecutor.PreparedJudgment::decision
                ).toList()
            );
            java.util.Map<UUID, UUID> incarnations = new java.util.LinkedHashMap<>();
            prepared.forEach((id, value) -> incarnations.put(id, runtime.incarnationNonce(value.entity())));
            CommitReceipt receipt = runtime.commands().commit(runtime.commands().qualify(batch).withIncarnations(incarnations));
            if (receipt.unknown()) {
                return new JudgmentReport(closure.size(), 0, List.of(), false);
            }
            if (!receipt.committed()) {
                return new JudgmentReport(closure.size(), 0, List.of());
            }
            var committed = dev.ronova.coremod.root.api.CommittedJudgmentGate.from(receipt);
            for (JudgmentExecutor.PreparedJudgment value : prepared.values()) {
                SpawnFirewall.ExecutionResult result = JudgmentExecutor.applyCommitted(
                    value, owner, authorityView, committed
                );
                if (result.firstExecution()) {
                    marks.add(result.mark());
                    count++;
                }
            }
        } finally {
            SpawnFirewall.invalidateAdmissionPasses(owner.server);
        }
        return new JudgmentReport(closure.size(), count, List.copyOf(marks));
    }

    /** Reconstructs the authoritative result of a durable ingress replay. */
    public static JudgmentReport reportFromReceipt(CommitReceipt receipt) {
        if (receipt == null || receipt.unknown()) {
            return new JudgmentReport(0, 0, List.of(), false);
        }
        if (!receipt.committed()) {
            return new JudgmentReport(0, 0, List.of());
        }

        List<dev.ronova.coremod.contract.DominionDecision.Judgment> judgments;
        if (receipt.command()
            instanceof dev.ronova.coremod.root.api.DominionCommand.CommitJudgmentBatch batch) {
            judgments = batch.batch().judgments();
        } else if (receipt.command()
            instanceof dev.ronova.coremod.root.api.DominionCommand.CommitJudgment judgment) {
            judgments = List.of(judgment.decision());
        } else {
            return new JudgmentReport(0, 0, List.of());
        }
        List<JudgmentMark> marks = judgments.stream().map(value -> new JudgmentMark(
            value.subject().uuid(), value.subject().registryType(),
            value.subject().dimension(), value.subject().x(), value.subject().y(),
            value.subject().z(), Math.max(0.0D, value.radius()),
            toEngineScope(value.scope())
        )).toList();
        return new JudgmentReport(judgments.size(), judgments.size(), marks);
    }

    /** Compatibility name: this now reports durable watch records, not retries. */
    public static int pendingRetries() {
        return ServerRuntimeRegistry.terminalWatchEntries();
    }

    public static int pendingRetries(MinecraftServer server) {
        return server == null ? 0
            : ServerRuntimeRegistry.runtime(server).terminalWatch().size();
    }

    public static void clearServer(MinecraftServer server) {
        var runtime = ServerRuntimeRegistry.getIfPresent(server);
        if (runtime != null) runtime.terminalWatch().clear();
    }

    private static boolean canJudge(Entity entity, AuthorityView authorityView) {
        return ExecutionTargetPolicy.shouldJudge(entity)
            && !authorityView.isProtected(entity);
    }

    private static Set<Entity> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    public record JudgmentReport(
        int candidates,
        int judged,
        List<JudgmentMark> marks,
        boolean resolved
    ) {
        public JudgmentReport {
            marks = marks == null ? List.of() : List.copyOf(marks);
        }

        public JudgmentReport(int candidates, int judged, List<JudgmentMark> marks) {
            this(candidates, judged, marks, true);
        }

        private JudgmentReport withCandidates(int value) {
            return new JudgmentReport(value, judged, marks, resolved);
        }
    }

    private static JudgmentScope toEngineScope(
        dev.ronova.coremod.contract.DominionDecision.LineageScope scope
    ) {
        return switch (scope) {
            case IDENTITY -> JudgmentScope.IDENTITY;
            case LOCAL_LINEAGE -> JudgmentScope.LOCAL_LINEAGE;
            case GLOBAL_LINEAGE -> JudgmentScope.GLOBAL_LINEAGE;
        };
    }

}
