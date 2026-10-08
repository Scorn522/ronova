package dev.ronova.coremod.network;

import dev.ronova.coremod.policy.*;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class StrengthControlPackets {
    private StrengthControlPackets() {}
    private static void context(FriendlyByteBuf b,StrengthControl.Context c){b.writeUUID(c.realm());b.writeUUID(c.stream());b.writeVarLong(c.generation());}
    private static StrengthControl.Context context(FriendlyByteBuf b){return new StrengthControl.Context(b.readUUID(),b.readUUID(),b.readVarLong());}
    public record RequestPacket(StrengthControl.Request request) {
        static void encode(RequestPacket p,FriendlyByteBuf b){var r=p.request();context(b,r.context());b.writeUUID(r.requestId());b.writeVarLong(r.expectedRevision());b.writeEnum(r.operation());b.writeVarInt(r.key());b.writeVarInt(r.value());}
        static RequestPacket decode(FriendlyByteBuf b){return new RequestPacket(new StrengthControl.Request(context(b),b.readUUID(),b.readVarLong(),b.readEnum(StrengthControl.Operation.class),b.readVarInt(),b.readVarInt()));}
        static void handle(RequestPacket p,Supplier<NetworkEvent.Context> supplier) {
            var ctx=supplier.get();var player=ctx.getSender();
            Runnable work=ServerActionService.guardQueued(player,()->StrengthControlService.execute(player,p.request()));
            try{ctx.enqueueWork(work).whenComplete((ignored,failed)->ServerActionService.releaseQueued(work));}
            catch(RuntimeException|Error failure){ServerActionService.releaseQueued(work);throw failure;}
            ctx.setPacketHandled(true);
        }
    }
    public record SnapshotPacket(StrengthControl.Snapshot snapshot) {
        static void encode(SnapshotPacket p,FriendlyByteBuf b) {
            var s=p.snapshot();context(b,s.context());b.writeUUID(s.requestId());b.writeVarLong(s.revision());b.writeEnum(s.configured());b.writeEnum(s.active());
            b.writeBoolean(s.editable());b.writeBoolean(s.pendingRestart());b.writeEnum(s.outcome());
            for(var o:s.options()){b.writeEnum(o.configured());b.writeEnum(o.effective());b.writeEnum(o.activation());}
            for(var a:s.actions()){b.writeEnum(a.configured());b.writeEnum(a.effective());}
            b.writeEnum(s.attackOutcome());b.writeEnum(s.execution());b.writeBoolean(s.completionSatisfied());
            b.writeVarLong(s.completionRevision());b.writeUtf(s.completionDetail(),256);
        }
        static SnapshotPacket decode(FriendlyByteBuf b) {
            var context=context(b);var id=b.readUUID();long revision=b.readVarLong();var configured=b.readEnum(StrengthProfile.class);var active=b.readEnum(StrengthProfile.class);
            boolean editable=b.readBoolean(),pending=b.readBoolean();var outcome=b.readEnum(StrengthControl.Outcome.class);
            List<StrengthControl.Option> options=new ArrayList<>();List<StrengthControl.Action> actions=new ArrayList<>();
            for(var key:StrengthOptionKey.values())options.add(new StrengthControl.Option(key,b.readEnum(MechanismLevel.class),b.readEnum(MechanismLevel.class),b.readEnum(ActivationState.class)));
            for(var key:AttackAction.values())actions.add(new StrengthControl.Action(key,b.readEnum(AttackActionLevel.class),b.readEnum(MechanismLevel.class)));
            return new SnapshotPacket(new StrengthControl.Snapshot(context,id,revision,configured,active,editable,pending,outcome,options,actions,
                b.readEnum(StrengthControl.AttackOutcome.class),b.readEnum(StrengthControl.Execution.class),b.readBoolean(),b.readVarLong(),b.readUtf(256)));
        }
        static void handle(SnapshotPacket p,Supplier<NetworkEvent.Context> supplier) {
            var ctx=supplier.get();var connection=ctx.getNetworkManager();
            ctx.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->dev.ronova.coremod.client.ClientRuntime.acceptStrengthSnapshot(p.snapshot(),connection)));
            ctx.setPacketHandled(true);
        }
    }
}
