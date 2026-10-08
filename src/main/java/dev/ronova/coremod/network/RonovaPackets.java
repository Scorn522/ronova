package dev.ronova.coremod.network;

import dev.ronova.coremod.client.ClientRuntime;
import dev.ronova.coremod.engine.JudgmentMark;
import dev.ronova.coremod.engine.JudgmentScope;
import dev.ronova.coremod.engine.QuarantineView;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Protocol packet schemas, codecs and thread-boundary dispatch. */
public final class RonovaPackets {
    private RonovaPackets() {}

    public record ActionRequest(
        UUID realmId,
        long sessionId,
        long sequence,
        RonovaAction action
    ) {
        static void encode(ActionRequest packet, FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.realmId);
            buffer.writeVarLong(packet.sessionId);
            buffer.writeVarLong(packet.sequence);
            buffer.writeEnum(packet.action);
        }

        static ActionRequest decode(FriendlyByteBuf buffer) {
            return new ActionRequest(
                buffer.readUUID(), buffer.readVarLong(), buffer.readVarLong(),
                buffer.readEnum(RonovaAction.class)
            );
        }

        static void handle(ActionRequest packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            Runnable work = ServerActionService.guardQueued(player, () -> ServerActionService.executeSequenced(player, packet));
            try { context.enqueueWork(work).whenComplete((ignored, failure) -> ServerActionService.releaseQueued(work)); }
            catch (RuntimeException | Error failure) { ServerActionService.releaseQueued(work); throw failure; }
            context.setPacketHandled(true);
        }
    }

    public record OwnerState(UUID uuid, boolean protectedState, UUID realmId, UUID streamId, long contextRevision, long ownerRevision) {
        static void encode(OwnerState packet, FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.uuid);
            buffer.writeBoolean(packet.protectedState);
            buffer.writeUUID(packet.realmId);
            buffer.writeUUID(packet.streamId);buffer.writeVarLong(packet.contextRevision);buffer.writeVarLong(packet.ownerRevision);
        }

        static OwnerState decode(FriendlyByteBuf buffer) {
            return new OwnerState(
                buffer.readUUID(), buffer.readBoolean(), buffer.readUUID(), buffer.readUUID(), buffer.readVarLong(), buffer.readVarLong()
            );
        }

        static void handle(OwnerState packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            var connection=context.getNetworkManager();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientRuntime.acceptVersionedOwner(packet,connection)));
            context.setPacketHandled(true);
        }
    }

    public record JudgmentFeedback(
        long sessionId,
        long sequence,
        RonovaAction action,
        int count,
        int candidates
    ) {
        static void encode(JudgmentFeedback packet, FriendlyByteBuf buffer) {
            buffer.writeVarLong(packet.sessionId);
            buffer.writeVarLong(packet.sequence);
            buffer.writeEnum(packet.action);
            buffer.writeVarInt(packet.count);
            buffer.writeVarInt(packet.candidates);
        }

        static JudgmentFeedback decode(FriendlyByteBuf buffer) {
            return new JudgmentFeedback(
                buffer.readVarLong(), buffer.readVarLong(), buffer.readEnum(RonovaAction.class),
                buffer.readVarInt(), buffer.readVarInt()
            );
        }

        static void handle(JudgmentFeedback packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            var connection=context.getNetworkManager();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if(ClientRuntime.connectionCurrent(connection))ClientRuntime.acceptFeedback(packet.sessionId,packet.sequence,packet.action,packet.count);
            }));
            context.setPacketHandled(true);
        }
    }

    public record QuarantineSync(ProjectionFrame frame) {
        public QuarantineSync { java.util.Objects.requireNonNull(frame); }
        static void encode(QuarantineSync packet,FriendlyByteBuf buffer) {
            var f=packet.frame;
            buffer.writeUUID(f.realmId());buffer.writeUUID(f.streamId());buffer.writeVarLong(f.contextRevision());
            buffer.writeVarLong(f.revision());buffer.writeVarLong(f.baseRevision());
            buffer.writeVarLong(f.authorityEpoch().generation());buffer.writeVarLong(f.authorityEpoch().sequence());
            buffer.writeEnum(f.mode());buffer.writeVarInt(f.partIndex());buffer.writeVarInt(f.partCount());
            buffer.writeCollection(f.marks(),RonovaPackets::writeMark);
            if(f.mode()==ProjectionFrame.Mode.REMOVE)buffer.writeUUID(f.identity());
        }
        static QuarantineSync decode(FriendlyByteBuf buffer) {
            UUID realm=buffer.readUUID(),stream=buffer.readUUID();long context=buffer.readVarLong(),revision=buffer.readVarLong(),base=buffer.readVarLong();
            var epoch=new dev.ronova.coremod.contract.DominionEpoch(buffer.readVarLong(),buffer.readVarLong());
            var mode=buffer.readEnum(ProjectionFrame.Mode.class);int part=buffer.readVarInt(),parts=buffer.readVarInt(),size=buffer.readVarInt();
            if(size<0||size>ProjectionFrame.MARKS_PER_PART)throw new IllegalArgumentException("projection part size exceeded");
            List<JudgmentMark> marks=new ArrayList<>(size);for(int i=0;i<size;i++)marks.add(readMark(buffer));
            UUID identity=mode==ProjectionFrame.Mode.REMOVE?buffer.readUUID():null;
            return new QuarantineSync(new ProjectionFrame(realm,stream,context,revision,base,epoch,mode,part,parts,marks,identity));
        }
        static void handle(QuarantineSync packet,Supplier<NetworkEvent.Context> supplier) {
            var context=supplier.get();var connection=context.getNetworkManager();
            context.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientRuntime.acceptProjection(packet.frame,connection)));
            context.setPacketHandled(true);
        }
    }
    public record ProjectionResyncRequest(UUID realmId,UUID streamId,long contextRevision,long revision) {
        static void encode(ProjectionResyncRequest packet,FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.realmId);buffer.writeUUID(packet.streamId);buffer.writeVarLong(packet.contextRevision);buffer.writeVarLong(packet.revision);
        }
        static ProjectionResyncRequest decode(FriendlyByteBuf buffer) {
            return new ProjectionResyncRequest(buffer.readUUID(),buffer.readUUID(),buffer.readVarLong(),buffer.readVarLong());
        }
        static void handle(ProjectionResyncRequest packet,Supplier<NetworkEvent.Context> supplier) {
            var context=supplier.get();var player=context.getSender();
            Runnable work=ServerActionService.guardQueued(player,()->QuarantineSyncService.resync(player,packet));
            try{context.enqueueWork(work).whenComplete((ignored,failure)->ServerActionService.releaseQueued(work));}
            catch(RuntimeException|Error failure){ServerActionService.releaseQueued(work);throw failure;}
            context.setPacketHandled(true);
        }
    }

    public record BlockBreakRequest(BlockPos position) {
        static void encode(BlockBreakRequest packet, FriendlyByteBuf buffer) {
            buffer.writeBlockPos(packet.position);
        }

        static BlockBreakRequest decode(FriendlyByteBuf buffer) {
            return new BlockBreakRequest(buffer.readBlockPos());
        }

        static void handle(BlockBreakRequest packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            Runnable work = ServerActionService.guardQueued(player, () -> ServerActionService.executeBlockBreak(player, packet.position));
            try { context.enqueueWork(work).whenComplete((ignored, failure) -> ServerActionService.releaseQueued(work)); }
            catch (RuntimeException | Error failure) { ServerActionService.releaseQueued(work); throw failure; }
            context.setPacketHandled(true);
        }
    }

    static void writeMark(FriendlyByteBuf buffer, JudgmentMark mark) {
        buffer.writeUUID(mark.uuid());
        buffer.writeUtf(mark.type());
        buffer.writeUtf(mark.dimension());
        buffer.writeDouble(mark.x());
        buffer.writeDouble(mark.y());
        buffer.writeDouble(mark.z());
        buffer.writeDouble(mark.radius());
        buffer.writeEnum(mark.scope());
    }

    static JudgmentMark readMark(FriendlyByteBuf buffer) {
        return new JudgmentMark(
            buffer.readUUID(), buffer.readUtf(256), buffer.readUtf(256),
            buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
            buffer.readEnum(JudgmentScope.class)
        );
    }
}
