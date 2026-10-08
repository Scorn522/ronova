package dev.ronova.coremod.network;

import dev.ronova.coremod.RonovaDominion;
import dev.ronova.coremod.authority.AuthorityView;
import dev.ronova.coremod.baseline.BaselineTelemetry;
import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.content.DominionContent;
import dev.ronova.coremod.contract.Capability;
import dev.ronova.coremod.engine.JudgmentEngine;
import dev.ronova.coremod.engine.JudgmentEngine.JudgmentReport;
import dev.ronova.coremod.engine.JudgmentMark;
import dev.ronova.coremod.engine.JudgmentScope;
import dev.ronova.coremod.engine.QuarantineView;
import dev.ronova.coremod.root.api.CommitReceipt;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Server-side action validation, idempotence, execution and receipts. */
public final class ServerActionService {
    private static final int JOURNALED_SESSIONS_PER_PLAYER = 4;
    private static final dev.ronova.coremod.util.WeakIdentityMap<ServerPlayer, EnumMap<RonovaAction, Integer>> LAST_ACTION =
        new dev.ronova.coremod.util.WeakIdentityMap<>();
    private static final dev.ronova.coremod.util.WeakIdentityMap<ServerPlayer, SessionJournal<Receipt>> RECEIPTS =
        new dev.ronova.coremod.util.WeakIdentityMap<>();
    private static final AtomicLong ACTION_SEQUENCE = new AtomicLong();
    private static final AtomicLong REJECTED_INGRESS_WORK = new AtomicLong();
    private static final AtomicLong DUPLICATE_INGRESS_RECEIPTS = new AtomicLong();
    private static final AtomicLong REJECTED_REALMS = new AtomicLong();
    private static final AtomicLong RESOURCE_BACKPRESSURE = new AtomicLong();

    private ServerActionService() {}

    /** Called at the Netty boundary. Never creates a runtime or reads client-supplied qualification claims. */
    public static Runnable guardQueued(ServerPlayer player, Runnable body) {
        if (player == null || player.server == null || player.connection == null) return () -> {};
        var runtime = ServerRuntimeRegistry.getIfPresent(player.server);
        if (runtime == null || runtime.durableStore() == null || runtime.durableStore().readOnly()) return () -> {};
        var connection = player.connection.connection;
        if(connection==null||!connection.isConnected()||runtime.authorityState().shuttingDown())return ()->{};
        var stamp = runtime.durableStore().authorityStamp(player.getUUID());
        if (!stamp.settled()) return () -> {};
        long created=System.nanoTime();
        var task = new dev.ronova.coremod.runtime.DeferredAuthorityTask(
            stamp, created, 5_000_000_000L);
        var work=runtime.ingressWork().enqueue(connection,() -> {
            boolean sameContext = ServerRuntimeRegistry.getIfPresent(player.server) == runtime
                && !runtime.authorityState().shuttingDown() && !runtime.durableStore().readOnly()
                && player.connection != null && player.connection.connection == connection
                && connection != null && connection.isConnected()
                && player.server.getPlayerList().getPlayer(player.getUUID()) == player;
            var result = task.run(sameContext ? runtime.durableStore().authorityStamp(player.getUUID()) : null,
                sameContext, System.nanoTime(), body);
            if (result != dev.ronova.coremod.runtime.DeferredAuthorityTask.Result.EXECUTED) REJECTED_INGRESS_WORK.incrementAndGet();
        },created,5_000_000_000L);
        if(work==null){RESOURCE_BACKPRESSURE.incrementAndGet();return ()->{};}
        return work;
    }
    /** The network future may complete/cancel without ever invoking its queued callback. */
    public static void releaseQueued(Runnable work) { if (work instanceof dev.ronova.coremod.runtime.IngressWorkRegistry.Work queued) queued.cancel(); }

    public static long rejectedIngressWork() { return REJECTED_INGRESS_WORK.get(); }
    public static long duplicateIngressReceipts() { return DUPLICATE_INGRESS_RECEIPTS.get(); }
    public static long rejectedRealmRequests() { return REJECTED_REALMS.get(); }
    public static long resourceBackpressure() { return RESOURCE_BACKPRESSURE.get(); }

    public static int execute(ServerPlayer player, RonovaAction action, String origin) {
        if (player == null || player.server == null) {
            return 0;
        }
        AuthorityView authorityView = ServerRuntimeRegistry.runtime(player.server).authorityView();
        if (!canExecute(player, action, authorityView) || !acquire(player, action)) {
            return 0;
        }
        long started = System.nanoTime();
        JudgmentReport report = executeReport(player, action, authorityView);
        if (!report.resolved()) {
            return 0;
        }
        publish(player, new Receipt(0L, 0L, action, report), origin, started);
        return report.judged();
    }

    public static int executeTargeted(
        ServerPlayer player,
        net.minecraft.world.entity.Entity target,
        String origin
    ) {
        if (player == null || player.server == null || target == null) {
            return 0;
        }
        AuthorityView authorityView = ServerRuntimeRegistry.runtime(player.server).authorityView();
        if (!hasBladeInHand(player)
            || !hasAuthority(player, Capability.PRIMARY_ACTION, authorityView)
            || !acquire(player, RonovaAction.PRIMARY)) {
            return 0;
        }
        long started = System.nanoTime();
        JudgmentReport report = JudgmentEngine.judgeReport(
            target, player, JudgmentScope.LOCAL_LINEAGE, 8.0D, authorityView
        );
        if (!report.resolved()) {
            return 0;
        }
        publish(player, new Receipt(0L, 0L, RonovaAction.PRIMARY, report), origin, started);
        return report.judged();
    }

    static void executeSequenced(ServerPlayer player, RonovaPackets.ActionRequest request) {
        if (player == null || player.server == null || request == null
            || request.realmId() == null || request.action() == null
            || request.sessionId() <= 0L
            || request.sequence() <= 0L) {
            return;
        }
        var runtime = ServerRuntimeRegistry.getIfPresent(player.server);
        if (runtime == null || runtime.durableStore() == null) return;
        if (!runtime.durableStore().realmId().value().equals(request.realmId())) { REJECTED_REALMS.incrementAndGet(); return; }
        SessionJournal<Receipt> journal = RECEIPTS.computeIfAbsent(
            player, ignored -> new SessionJournal<>(JOURNALED_SESSIONS_PER_PLAYER)
        );
        Receipt previous = journal.get(request.sessionId());
        if (previous != null && request.sequence() == previous.sequence) {
            if (request.action() == previous.action) {
                DUPLICATE_INGRESS_RECEIPTS.incrementAndGet();
                sendReceipt(player, previous);
            }
            // Same identity with a different action is a payload conflict, not
            // an older request that may receive a synthetic acknowledgement.
            return;
        }
        if (previous != null && request.sequence() < previous.sequence) {
            sendReceipt(player, new Receipt(
                request.sessionId(), request.sequence(), request.action(),
                new JudgmentReport(0, 0, List.of())
            ));
            return;
        }

        UUID commandId = ActionCommandIdentity.derive(
            runtime.durableStore().realmId(), player.getUUID(), request.sessionId(),
            request.sequence()
        );
        CommitReceipt recovered = runtime.commands().recover(commandId);
        if (recovered != null) {
            if (!ActionCommandIdentity.matches(recovered, request.action())) {
                return;
            }
            JudgmentReport recoveredReport = JudgmentEngine.reportFromReceipt(recovered);
            if (!recoveredReport.resolved()) {
                return;
            }
            Receipt receipt = new Receipt(
                request.sessionId(), request.sequence(), request.action(), recoveredReport
            );
            journal.put(request.sessionId(), receipt);
            sendReceipt(player, receipt);
            return;
        }

        long started = System.nanoTime();
        JudgmentReport report = new JudgmentReport(0, 0, List.of());
        AuthorityView authorityView = runtime.authorityView();
        if (canExecute(player, request.action(), authorityView)
            && acquire(player, request.action())) {
            report = executeReport(
                player, request.action(), authorityView, commandId,
                ActionCommandIdentity.commandDomain(request.action())
            );
        }
        if (!report.resolved()) {
            return;
        }
        Receipt receipt = new Receipt(
            request.sessionId(), request.sequence(), request.action(), report
        );
        journal.put(request.sessionId(), receipt);
        publish(player, receipt, "packet", started);
    }

    static void executeBlockBreak(ServerPlayer player, BlockPos position) {
        if (player == null || player.server == null || position == null) {
            return;
        }
        AuthorityView authorityView = ServerRuntimeRegistry.runtime(player.server).authorityView();
        if (!hasBladeInHand(player)
            || !hasAuthority(player, Capability.INSTANT_MINING, authorityView)) {
            return;
        }
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = player.serverLevel().clip(new ClipContext(
            eye, eye.add(player.getLookAngle().scale(JudgmentEngine.PRIMARY_RANGE)),
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(position)) {
            return;
        }
        BlockState state = player.serverLevel().getBlockState(position);
        if (state.isAir()) {
            return;
        }
        BlockEntity blockEntity = player.serverLevel().getBlockEntity(position);
        Block.dropResources(state, player.serverLevel(), position, blockEntity,
            player, player.getMainHandItem());
        player.serverLevel().removeBlockEntity(position);
        player.serverLevel().setBlock(position, Blocks.AIR.defaultBlockState(), 3);
        player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL,
            position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D,
            18, 0.4D, 0.4D, 0.4D, 0.02D);
    }

    public static void releasePlayer(ServerPlayer player) {
        if (player != null) {
            releaseConnection(player.server,player,player.connection==null?null:player.connection.connection);
        }
    }
    static void releaseConnection(net.minecraft.server.MinecraftServer server,ServerPlayer player,net.minecraft.network.Connection connection) {
        var runtime=server==null?null:ServerRuntimeRegistry.getIfPresent(server);
        if(runtime!=null&&connection!=null)runtime.ingressWork().cancelOwner(connection);
        if(player!=null&&(player.connection==null||player.connection.connection==connection)) {
            LAST_ACTION.remove(player);
            RECEIPTS.remove(player);
        }
    }

    public static void clearPlayers(net.minecraft.server.MinecraftServer server,Set<UUID> players) {
        if (players == null) {
            return;
        }
        LAST_ACTION.removeIf(player -> player.server==server&&players.contains(player.getUUID()));
        RECEIPTS.removeIf(player -> player.server==server&&players.contains(player.getUUID()));
    }

    private static boolean canExecute(
        ServerPlayer player,
        RonovaAction action,
        AuthorityView authorityView
    ) {
        return player != null && action != null && hasBladeInHand(player)
            && hasAuthority(player, capability(action), authorityView);
    }

    private static boolean hasAuthority(
        ServerPlayer player,
        Capability capability,
        AuthorityView authorityView
    ) {
        if (player == null || player.server == null || capability == null || authorityView == null) return false;
        var runtime = ServerRuntimeRegistry.getIfPresent(player.server);
        if (runtime == null || runtime.durableStore() == null) return false;
        var stamp = runtime.durableStore().authorityStamp(player.getUUID());
        return stamp.settled() && stamp.owner() && authorityView.hasCapability(player, capability);
    }

    private static JudgmentReport executeReport(
        ServerPlayer player,
        RonovaAction action,
        AuthorityView authorityView
    ) {
        return executeReport(player, action, authorityView, null, "");
    }

    private static JudgmentReport executeReport(
        ServerPlayer player,
        RonovaAction action,
        AuthorityView authorityView,
        UUID commandId,
        String commandDomain
    ) {
        return switch (action) {
            case PRIMARY -> JudgmentEngine.rayJudgmentReport(
                player, JudgmentEngine.PRIMARY_RANGE, authorityView,
                commandId, commandDomain
            );
            case AREA -> JudgmentEngine.areaJudgmentReport(
                player, JudgmentEngine.AREA_RADIUS, authorityView,
                commandId, commandDomain
            );
            case LOADED -> JudgmentEngine.loadedWorldJudgmentReport(
                player, authorityView, commandId, commandDomain
            );
        };
    }

    private static boolean hasBladeInHand(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.is(DominionContent.RONOVA_BLADE.get())
            || off.is(DominionContent.RONOVA_BLADE.get());
    }

    private static boolean acquire(ServerPlayer player, RonovaAction action) {
        int tick = player.server.getTickCount();
        EnumMap<RonovaAction, Integer> actions = LAST_ACTION.computeIfAbsent(
            player, ignored -> new EnumMap<>(RonovaAction.class)
        );
        Integer previous = actions.put(action, tick);
        return previous == null || previous != tick;
    }

    private static void publish(
        ServerPlayer player,
        Receipt receipt,
        String origin,
        long started
    ) {
        RonovaAction action = receipt.action;
        JudgmentReport report = receipt.report;
        double distance = action == RonovaAction.PRIMARY ? 8.0D : 1.2D;
        double x = player.getX() + player.getLookAngle().x * distance;
        double y = player.getEyeY() + player.getLookAngle().y * distance;
        double z = player.getZ() + player.getLookAngle().z * distance;
        player.serverLevel().sendParticles(ParticleTypes.END_ROD, x, y, z,
            Math.min(96, 12 + report.judged() * 4),
            0.65D, 0.65D, 0.65D, 0.02D);
        sendReceipt(player, receipt);
        List<JudgmentMark> marks = report.marks();
        QuarantineSyncService.broadcastMarks(player.server, marks);
        long micros = (System.nanoTime() - started) / 1_000L;
        var runtime = ServerRuntimeRegistry.runtime(player.server);
        int topologySize = runtime.observation().size();
        var watch = runtime.terminalWatch().metrics();
        int tombstoneCount = runtime.projection().admission().tombstoneCount();
        int quarantineObjects = QuarantineView.objectCount(player.server);
        BaselineTelemetry.recordAction(
            player.server, action, micros, report.candidates(), report.judged(),
            tombstoneCount, topologySize, quarantineObjects
        );
        RonovaDominion.LOGGER.info(
            "RONOVA_ACTION seq={} request_session={} request_seq={} player={} origin={} action={} candidates={} judged={} marks={} observation_mode={} topology={} tombstones={} quarantine_objects={} watch_mode={} watch_entries={} watch_scheduled={} watch_attempted={} watch_completed={} watch_notification_failures={} elapsed_us={}",
            ACTION_SEQUENCE.incrementAndGet(), receipt.sessionId, receipt.sequence,
            player.getGameProfile().getName(), origin,
            action, report.candidates(), report.judged(), marks.size(),
            runtime.rawObservationMode(), topologySize, tombstoneCount, quarantineObjects,
            watch.mode(), watch.activeEntries(), watch.scheduledNotifications(),
            watch.attemptNotifications(), watch.completionNotifications(),
            runtime.passiveWatchNotificationFailures(), micros
        );
    }

    private static void sendReceipt(ServerPlayer player, Receipt receipt) {
        JudgmentReport report = receipt.report;
        RonovaChannel.sendToPlayer(player, new RonovaPackets.JudgmentFeedback(
            receipt.sessionId, receipt.sequence, receipt.action,
            report.judged(), report.candidates()
        ));
    }

    private static Capability capability(RonovaAction action) {
        return switch (action) {
            case PRIMARY -> Capability.PRIMARY_ACTION;
            case AREA -> Capability.AREA_ACTION;
            case LOADED -> Capability.LOADED_ACTION;
        };
    }

    private record Receipt(
        long sessionId,
        long sequence,
        RonovaAction action,
        JudgmentReport report
    ) {}
}
