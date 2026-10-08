package dev.ronova.coremod.client;

import dev.ronova.coremod.agent.AgentHooks;
import dev.ronova.coremod.authority.AuthorityIdentity;
import dev.ronova.coremod.network.RonovaAction;
import dev.ronova.coremod.network.RonovaNetwork;
import java.util.UUID;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;

/** Compatibility facade for composed client input, action and recovery controllers. */
public final class ClientRuntime {
    private static final ClientActionController ACTIONS =
        new ClientActionController(RonovaNetwork::sendAction);
    private static final ClientInputController INPUT =
        new ClientInputController(ACTIONS, RonovaNetwork::requestBreak);
    private static final ClientRecoveryController RECOVERY = new ClientRecoveryController();
    private static final ProjectionStreamState PROJECTION = new ProjectionStreamState();
    private static final StrengthControlState STRENGTH = new StrengthControlState();
    private static long lastResyncMillis=Long.MIN_VALUE;
    private static long projectionRejects;

    static {
        AgentHooks.installProtectedViolationListener(RECOVERY::observeProtectedViolation);
        AgentHooks.installExternalControlSuppression(RECOVERY::isAuthorityRecoveryActive);
    }

    private ClientRuntime() {}

    public static void pulse(Minecraft minecraft) {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            INPUT.reset();
            return;
        }
        ACTIONS.pulse(minecraft.level.getGameTime());
        long now=System.nanoTime()/1_000_000L;
        if(PROJECTION.needsResync(now)&&PROJECTION.realmId()!=null&&(lastResyncMillis==Long.MIN_VALUE||now-lastResyncMillis>=1000)) {
            lastResyncMillis=now;
            dev.ronova.coremod.network.RonovaChannel.sendToServer(new dev.ronova.coremod.network.RonovaPackets.ProjectionResyncRequest(
                PROJECTION.realmId(),PROJECTION.streamId(),PROJECTION.contextRevision(),PROJECTION.revision()));
        }
        RECOVERY.recoverIfInvalid(minecraft);
        RECOVERY.maintainAuthoritySurface(minecraft);
        INPUT.pulse(minecraft);
    }

    public static void framePulse(Minecraft minecraft) {
        ACTIONS.flush();
        if (minecraft != null && minecraft.player != null) {
            RECOVERY.recoverIfInvalid(minecraft);
            RECOVERY.maintainAuthoritySurface(minecraft);
        }
    }

    public static void onMouseButton(long window, int button, int action, int modifiers) {
        INPUT.onMouseButton(window, button, action, modifiers);
    }
    public static void beginBladeBlock(net.minecraft.world.entity.player.Player player,net.minecraft.world.item.ItemStack stack,net.minecraft.world.InteractionHand hand) {
        INPUT.beginBlock(player,stack,hand);
    }
    public static void bladeBlockReleased(net.minecraft.world.entity.player.Player player,net.minecraft.world.item.ItemStack stack){INPUT.blockReleased(player,stack);}

    public static void queueAction(RonovaAction action) {
        ACTIONS.queue(action);
    }

    public static void acceptFeedback(
        long sessionId,
        long sequence,
        RonovaAction action,
        int count
    ) {
        ACTIONS.acceptFeedback(sessionId, sequence, action, count);
    }

    public static void acceptOwnerState(
        UUID uuid,
        boolean protectedState,
        UUID realmId
    ) {
        AuthorityIdentity.replace(uuid, protectedState);
        ACTIONS.acceptContext(realmId, uuid);
    }
    public static boolean connectionCurrent(net.minecraft.network.Connection connection) {
        var minecraft=Minecraft.getInstance();
        return connection!=null&&connection.isConnected()&&minecraft!=null&&minecraft.getConnection()!=null
            &&minecraft.getConnection().getConnection()==connection;
    }
    public static void acceptVersionedOwner(dev.ronova.coremod.network.RonovaPackets.OwnerState packet,
        net.minecraft.network.Connection connection) {
        if(!connectionCurrent(connection))return;
        long prior=PROJECTION.contextRevision();
        if(!PROJECTION.bind(packet.realmId(),packet.streamId(),packet.contextRevision())){projectionRejects++;return;}
        if(prior!=PROJECTION.contextRevision()){
            dev.ronova.coremod.engine.QuarantineView.clearClientMarks();
            STRENGTH.bind(new dev.ronova.coremod.network.StrengthControl.Context(packet.realmId(),packet.streamId(),packet.contextRevision()));
        }
        if(PROJECTION.acceptOwner(packet.ownerRevision(),packet.protectedState()))acceptOwnerState(packet.uuid(),packet.protectedState(),packet.realmId());
        else projectionRejects++;
    }
    public static void acceptProjection(dev.ronova.coremod.network.ProjectionFrame frame,net.minecraft.network.Connection connection) {
        if(!connectionCurrent(connection))return;
        var result=PROJECTION.accept(frame,System.nanoTime()/1_000_000L);
        if(result==ProjectionStreamState.Result.COMMITTED){
            var committed=PROJECTION.snapshot();dev.ronova.coremod.engine.QuarantineView.replaceClientMarks(committed);
            dev.ronova.coremod.network.RonovaChannel.sendToServer(new dev.ronova.coremod.network.ProjectionAcknowledgement(
                PROJECTION.realmId(),PROJECTION.streamId(),PROJECTION.contextRevision(),PROJECTION.revision(),PROJECTION.authorityEpoch(),
                dev.ronova.coremod.network.ProjectionDigest.of(committed)));
        }
        else if(result!=ProjectionStreamState.Result.STAGED)projectionRejects++;
    }
    public static long projectionRevision(){return PROJECTION.revision();}
    public static long projectionRejects(){return projectionRejects;}
    public static int projectionMarkCount(){return PROJECTION.size();}
    public static boolean projectionContains(UUID identity){return PROJECTION.contains(identity);}
    public static Boolean projectionOwnerState(){return PROJECTION.ownerState();}
    public static StrengthControlState strengthControls(){return STRENGTH;}
    public static boolean requestStrength(dev.ronova.coremod.network.StrengthControl.Operation operation,int key,int value) {
        var mc=Minecraft.getInstance();if(mc==null||mc.getConnection()==null||PROJECTION.realmId()==null)return false;
        STRENGTH.bind(new dev.ronova.coremod.network.StrengthControl.Context(PROJECTION.realmId(),PROJECTION.streamId(),PROJECTION.contextRevision()));
        var request=STRENGTH.begin(operation,key,value,System.nanoTime()/1_000_000L);
        if(request==null)return false;
        dev.ronova.coremod.network.RonovaChannel.sendToServer(new dev.ronova.coremod.network.StrengthControlPackets.RequestPacket(request));return true;
    }
    public static void acceptStrengthSnapshot(dev.ronova.coremod.network.StrengthControl.Snapshot snapshot,net.minecraft.network.Connection connection) {
        if(connectionCurrent(connection)&&Objects.equals(snapshot.context().realm(),PROJECTION.realmId())
            &&Objects.equals(snapshot.context().stream(),PROJECTION.streamId())&&snapshot.context().generation()==PROJECTION.contextRevision())STRENGTH.accept(snapshot,System.nanoTime()/1_000_000L);
    }

    /** Preserves an unacknowledged action until a same-Realm reconnect. */
    public static void suspendSession() {
        STRENGTH.reset();
        PROJECTION.reset();dev.ronova.coremod.engine.QuarantineView.clearClientMarks();lastResyncMillis=Long.MIN_VALUE;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.player != null) {
            AuthorityIdentity.unbind(minecraft.player.getUUID());
        }
        ACTIONS.suspend();
        INPUT.reset();
        RECOVERY.reset();
    }

    public static void resetSession() {
        STRENGTH.reset();
        PROJECTION.reset();dev.ronova.coremod.engine.QuarantineView.clearClientMarks();lastResyncMillis=Long.MIN_VALUE;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.player != null) {
            AuthorityIdentity.unbind(minecraft.player.getUUID());
        }
        ACTIONS.reset();
        INPUT.reset();
        RECOVERY.reset();
    }

    public static int feedbackTicks() {
        return ACTIONS.feedbackTicks();
    }
    public static ProjectionStreamState.MemoryUsage projectionMemoryUsage() { return PROJECTION.memoryUsage(); }

    public static int lastCount() {
        return ACTIONS.lastCount();
    }

    public static RonovaAction lastAction() {
        return ACTIONS.lastAction();
    }

    public static boolean recoverIfInvalid(Minecraft minecraft) {
        return RECOVERY.recoverIfInvalid(minecraft);
    }

    public static boolean shouldRejectScreen(Minecraft minecraft, Screen requested) {
        return RECOVERY.shouldRejectScreen(minecraft, requested);
    }

    public static boolean shouldRejectOverlay(Minecraft minecraft, Overlay requested) {
        return RECOVERY.shouldRejectOverlay(minecraft, requested);
    }

    public static boolean isAuthorityRecoveryActive() {
        return RECOVERY.isAuthorityRecoveryActive();
    }
}
