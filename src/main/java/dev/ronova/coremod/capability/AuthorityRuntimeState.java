package dev.ronova.coremod.capability;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.lang.ref.WeakReference;
import dev.ronova.coremod.util.WeakIdentityMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Per-server ephemeral Authority anchors, connections and maintenance bookkeeping. */
public final class AuthorityRuntimeState {
    private final MinecraftServer server;
    private final Map<UUID, ServerPlayer> anchors = new ConcurrentHashMap<>();
    private record ConnectionBinding(UUID owner, WeakReference<ServerPlayer> player) {}
    private final WeakIdentityMap<Connection, ConnectionBinding> protectedConnections = new WeakIdentityMap<>();
    private final WeakIdentityMap<Player, Integer> lastRoutineRepair = new WeakIdentityMap<>();
    private volatile boolean shuttingDown;

    public AuthorityRuntimeState(MinecraftServer server) {
        if (server == null) {
            throw new IllegalArgumentException("server is required");
        }
        this.server = server;
    }

    public MinecraftServer server() { return server; }

    public ServerPlayer bindAnchor(ServerPlayer player) {
        if (shuttingDown || player == null || player.server != server || !server.isSameThread()) return null;
        UUID uuid = player.getUUID();
        synchronized(this) {
            if (shuttingDown) return null;
            for(var entry:anchors.entrySet()) {
                if(entry.getValue()==player&&!entry.getKey().equals(uuid))return player;
            }
            return anchors.put(uuid, player);
        }
    }

    public ServerPlayer anchor(UUID uuid) { return anchors.get(uuid); }
    /** Existing physical binding only; never infer a new Owner from a damaged current UUID. */
    public synchronized UUID anchorId(ServerPlayer player) {
        if(shuttingDown||player==null)return null;
        UUID found=null;
        for(var entry:anchors.entrySet())if(entry.getValue()==player) {
            if(found!=null&&!found.equals(entry.getKey()))return null;
            found=entry.getKey();
        }
        return found;
    }

    public java.util.List<ServerPlayer> anchors() {
        return new ArrayList<>(anchors.values());
    }

    public void bindConnection(ServerPlayer player) {
        if (shuttingDown || player == null || player.server != server || !server.isSameThread()) return;
        try {
            if (player != null && player.connection != null && player.connection.connection != null) {
                var listener=player.connection;var connection=listener.connection;
                UUID uuid = player.getUUID();
                synchronized(this) {
                    if (shuttingDown||player.connection!=listener||listener.connection!=connection) return;
                    protectedConnections.put(connection, new ConnectionBinding(uuid,new WeakReference<>(player)));
                }
            }
        } catch (Throwable ignored) {
            // Counted module recovery is independent; weak keys keep this adapter recoverable.
        }
    }

    public synchronized void releaseConnection(ServerPlayer player) {
        try {
            if (player != null && player.connection != null && player.connection.connection != null) {
                var connection=player.connection.connection;
                var binding=protectedConnections.get(connection);
                if(binding!=null&&binding.player().get()==player)protectedConnections.remove(connection);
            }
        } catch (Throwable ignored) {
            // Weak keys guarantee eventual cleanup at this compatibility boundary.
        }
    }

    public synchronized UUID connectionOwner(Connection connection) {
        var binding=connection==null?null:protectedConnections.get(connection);
        var player=binding==null?null:binding.player().get();
        return !shuttingDown&&player!=null&&anchors.get(binding.owner())==player?binding.owner():null;
    }
    public synchronized ServerPlayer protectedPlayer(Connection connection) {
        var binding=connection==null?null:protectedConnections.get(connection);
        var player=binding==null?null:binding.player().get();
        return !shuttingDown&&player!=null&&anchors.get(binding.owner())==player?player:null;
    }

    public synchronized void releaseObject(ServerPlayer player) {
        if (player == null) {
            return;
        }
        // Use physical identity even if UUID changed or Player overrides equality.
        for(UUID key:anchors.keySet())anchors.computeIfPresent(key,(ignored,current)->current==player?null:current);
        releaseConnection(player);
        lastRoutineRepair.remove(player);
    }

    public void clearProjection(ServerPlayer player) {
        releaseObject(player);
    }

    public synchronized void markRepaired(Player player, int tick) {
        if (shuttingDown || player == null) return;
        lastRoutineRepair.put(player, tick);
    }

    public synchronized boolean claimRoutineRepair(Player player, int tick) {
        if (shuttingDown || player == null) return false;
        synchronized (lastRoutineRepair) {
            Integer previous = lastRoutineRepair.get(player);
            lastRoutineRepair.put(player, tick);
            return previous == null || previous != tick;
        }
    }

    public boolean shuttingDown() { return shuttingDown; }

    public synchronized void beginShutdown() { shuttingDown = true; }

    public int anchorCount() { return anchors.size(); }

    public int connectionCount() { return protectedConnections.size(); }

    public synchronized void release() {
        shuttingDown = true;
        anchors.clear();
        protectedConnections.clear();
        lastRoutineRepair.clear();
    }
}
