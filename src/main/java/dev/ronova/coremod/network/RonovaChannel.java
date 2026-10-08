package dev.ronova.coremod.network;

import dev.ronova.coremod.RonovaDominion;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Versioned channel ownership, packet registration and transport only. */
public final class RonovaChannel {
    public static final String PROTOCOL = "14";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(ResourceLocation.fromNamespaceAndPath(
            RonovaDominion.MOD_ID, "authority"
        ))
        .networkProtocolVersion(() -> PROTOCOL)
        .clientAcceptedVersions(PROTOCOL::equals)
        .serverAcceptedVersions(PROTOCOL::equals)
        .simpleChannel();
    private static int discriminator;
    private static boolean initialized;

    private RonovaChannel() {}

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        CHANNEL.registerMessage(discriminator++, RonovaPackets.ActionRequest.class,
            RonovaPackets.ActionRequest::encode, RonovaPackets.ActionRequest::decode,
            RonovaPackets.ActionRequest::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(discriminator++, RonovaPackets.OwnerState.class,
            RonovaPackets.OwnerState::encode, RonovaPackets.OwnerState::decode,
            RonovaPackets.OwnerState::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(discriminator++, RonovaPackets.JudgmentFeedback.class,
            RonovaPackets.JudgmentFeedback::encode, RonovaPackets.JudgmentFeedback::decode,
            RonovaPackets.JudgmentFeedback::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(discriminator++, RonovaPackets.QuarantineSync.class,
            RonovaPackets.QuarantineSync::encode, RonovaPackets.QuarantineSync::decode,
            RonovaPackets.QuarantineSync::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(discriminator++, RonovaPackets.BlockBreakRequest.class,
            RonovaPackets.BlockBreakRequest::encode, RonovaPackets.BlockBreakRequest::decode,
            RonovaPackets.BlockBreakRequest::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(discriminator++, RonovaPackets.ProjectionResyncRequest.class,
            RonovaPackets.ProjectionResyncRequest::encode, RonovaPackets.ProjectionResyncRequest::decode,
            RonovaPackets.ProjectionResyncRequest::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(discriminator++, StrengthControlPackets.RequestPacket.class,
            StrengthControlPackets.RequestPacket::encode, StrengthControlPackets.RequestPacket::decode,
            StrengthControlPackets.RequestPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(discriminator++, StrengthControlPackets.SnapshotPacket.class,
            StrengthControlPackets.SnapshotPacket::encode, StrengthControlPackets.SnapshotPacket::decode,
            StrengthControlPackets.SnapshotPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(discriminator++, ProjectionAcknowledgement.class,
            ProjectionAcknowledgement::encode, ProjectionAcknowledgement::decode,
            ProjectionAcknowledgement::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        if (player != null && packet != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
}
