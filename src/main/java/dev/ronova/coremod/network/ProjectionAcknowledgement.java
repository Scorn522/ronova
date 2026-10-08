package dev.ronova.coremod.network;

import dev.ronova.coremod.contract.DominionEpoch;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Client-reported commit of the canonical projection model, bound to its actual connection. */
public record ProjectionAcknowledgement(UUID realm,UUID stream,long context,long revision,DominionEpoch epoch,String digest) {
    public ProjectionAcknowledgement {
        Objects.requireNonNull(realm);Objects.requireNonNull(stream);Objects.requireNonNull(epoch);Objects.requireNonNull(digest);
        if(context<1||revision<1||!digest.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("invalid projection acknowledgement");
    }
    static void encode(ProjectionAcknowledgement p,FriendlyByteBuf b) {
        b.writeUUID(p.realm);b.writeUUID(p.stream);b.writeVarLong(p.context);b.writeVarLong(p.revision);
        b.writeVarLong(p.epoch.generation());b.writeVarLong(p.epoch.sequence());b.writeUtf(p.digest,64);
    }
    static ProjectionAcknowledgement decode(FriendlyByteBuf b) {
        return new ProjectionAcknowledgement(b.readUUID(),b.readUUID(),b.readVarLong(),b.readVarLong(),new DominionEpoch(b.readVarLong(),b.readVarLong()),b.readUtf(64));
    }
    static void handle(ProjectionAcknowledgement packet,Supplier<NetworkEvent.Context> supplier) {
        var ctx=supplier.get();var player=ctx.getSender();
        Runnable work=ServerActionService.guardQueued(player,()->QuarantineSyncService.acknowledge(player,packet));
        try{ctx.enqueueWork(work).whenComplete((ignored,failed)->ServerActionService.releaseQueued(work));}
        catch(RuntimeException|Error failed){ServerActionService.releaseQueued(work);throw failed;}
        ctx.setPacketHandled(true);
    }
}
