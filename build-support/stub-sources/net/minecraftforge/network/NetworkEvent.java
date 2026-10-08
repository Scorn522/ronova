package net.minecraftforge.network;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerPlayer;

public final class NetworkEvent {
    private NetworkEvent() {}

    public static class Context {
        public ServerPlayer getSender() {
            return null;
        }

        public CompletableFuture<Void> enqueueWork(Runnable work) {
            return CompletableFuture.completedFuture(null);
        }

        public void setPacketHandled(boolean handled) {}
    }
}
