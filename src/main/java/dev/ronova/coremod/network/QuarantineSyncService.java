package dev.ronova.coremod.network;

import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.engine.JudgmentMark;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Root-sourced, per-connection projection transactions. No partial snapshot is a committed client view. */
public final class QuarantineSyncService {
    private static final Map<MinecraftServer,Map<UUID,Session>> PLAYERS=Collections.synchronizedMap(new WeakHashMap<>());
    private static final AtomicLong CONTEXTS=new AtomicLong();
    private static final AtomicLong RESYNCS=new AtomicLong();
    private static final long SERVER_QUEUE_BYTES = 128L * 1024 * 1024;
    private static final long SERVER_TICK_BYTES = 2L * 1024 * 1024, CONNECTION_TICK_BYTES = 512L * 1024;
    private static final int SERVER_TICK_PARTS = 16, CONNECTION_TICK_PARTS = 4;
    private static final Map<MinecraftServer,QueueBudget> QUEUES=Collections.synchronizedMap(new WeakHashMap<>());
    private static final class QueueBudget {
        long retained,denied,parts,bytes; int cursor;
        final ProjectionTickBudget tick=new ProjectionTickBudget(SERVER_TICK_PARTS,SERVER_TICK_BYTES);
    }
    private static QueueBudget queue(MinecraftServer server) { synchronized(QUEUES){return QUEUES.computeIfAbsent(server,ignored->new QueueBudget());} }
    public static Map<String,Long> queueCounters(MinecraftServer server) {
        var queue=queue(server);synchronized(queue){return Map.of("projection_queued_bytes",queue.retained,
            "projection_queue_denials",queue.denied,"projection_parts_sent",queue.parts,"projection_wire_estimate_sent",queue.bytes);}
    }
    private static void discard(MinecraftServer server,Session state) {
        if(state.outgoing==null)return;
        var queue=queue(server);synchronized(queue){queue.retained-=state.outgoing.retainedBytes();}
        state.outgoing=null;
        if(state.reservation!=null){state.reservation.close();state.reservation=null;}
    }
    private static void close(MinecraftServer server,Session state) {
        state.closed=true;state.dirty=false;state.receipt=null;state.delivery.close();discard(server,state);
        ServerActionService.releaseConnection(server,state.player.get(),state.connection.get());
    }
    private static Session find(MinecraftServer server,UUID player) {
        synchronized(PLAYERS){var map=PLAYERS.get(server);return map==null?null:map.get(player);}
    }
    public static String deliverySummary(ServerPlayer player) {
        if(player==null||player.server==null)return "本连接投影：未连接";
        var state=find(player.server,player.getUUID());if(state==null)return "本连接投影：尚未登记";
        synchronized(state){return state.current(player)?state.delivery.summary(Integer.toUnsignedLong(player.server.getTickCount()),state.dirty):"本连接投影：连接已失效";}
    }
    private record DigestImage(dev.ronova.coremod.contract.DominionEpoch epoch,String digest) {}
    private static final Map<MinecraftServer,DigestImage> DIGESTS=Collections.synchronizedMap(new WeakHashMap<>());
    public record StreamStatus(UUID realmId,UUID streamId,long contextRevision,long revision) {}
    public static StreamStatus streamStatus(ServerPlayer player) {
        if(player==null||player.server==null)return null;
        Session state;synchronized(PLAYERS){var map=PLAYERS.get(player.server);state=map==null?null:map.get(player.getUUID());}
        if(state==null)return null;
        synchronized(state){if(!state.current(player))return null;var runtime=ServerRuntimeRegistry.getIfPresent(player.server);return runtime==null||runtime.durableStore()==null?null:
            new StreamStatus(runtime.durableStore().realmId().value(),state.stream,state.context,state.revision);}
    }
    public static long resyncCount(){return RESYNCS.get();}
    public static java.util.List<dev.ronova.coremod.root.api.ClientParticipationContract.Endpoint> participationEndpoints(MinecraftServer server) {
        var result=new java.util.ArrayList<dev.ronova.coremod.root.api.ClientParticipationContract.Endpoint>();
        for(var state:sessions(server))synchronized(state){var player=state.player.get();if(state.current(player)&&player.connection.connection.isConnected())
            result.add(new dev.ronova.coremod.root.api.ClientParticipationContract.Endpoint(player.getUUID(),state.stream,state.context,Math.addExact(state.revision,1)));}
        return java.util.List.copyOf(result);
    }
    public static boolean confirmsParticipation(MinecraftServer server,dev.ronova.coremod.root.api.ClientParticipationContract contract,
        dev.ronova.coremod.root.api.ClientParticipationContract.Member member) {
        var endpoint=member.endpoint();
        var runtime=ServerRuntimeRegistry.getIfPresent(server);
        if(runtime==null||runtime.durableStore()==null)return false;
        var authority=runtime.durableStore().authorityStamp(endpoint.client());
        if(authority==null||!authority.settled()||authority.ownerAuthorizationRevision()!=member.ownerRevision()
            ||authority.owner()!=member.required())return false;
        var state=find(server,endpoint.client());if(state==null)return false;
        synchronized(state){
            var player=state.player.get();return state.current(player)&&player.connection.connection.isConnected()
                &&state.stream.equals(endpoint.stream())&&state.context==endpoint.context()&&state.receipt!=null
                &&state.outgoing==null&&!state.dirty&&state.sentRevision==state.revision
                &&state.receipt.confirms(endpoint.minimumRevision(),contract.epoch(),contract.imageDigest());}
    }
    private static final class Session {
        final WeakReference<ServerPlayer> player;
        final WeakReference<net.minecraft.network.Connection> connection;
        final UUID stream=UUID.randomUUID(); final long context;
        long revision,lastResyncTick=Long.MIN_VALUE,lastParticipationSendTick=Long.MIN_VALUE;
        ProjectionReceiptState receipt;
        ProjectionTransmission outgoing;
        boolean dirty;
        long sentRevision, sentAtTick=-1;
        boolean closed;
        final ProjectionTickBudget tick=new ProjectionTickBudget(CONNECTION_TICK_PARTS,CONNECTION_TICK_BYTES);
        final ProjectionDeliveryState delivery=new ProjectionDeliveryState();
        dev.ronova.coremod.budget.HostResourceGovernor.Reservation reservation;
        Session(ServerPlayer player) {
            this.player=new WeakReference<>(player);connection=new WeakReference<>(player.connection.connection);
            context=CONTEXTS.updateAndGet(value->Math.addExact(value,1));
        }
        boolean current(ServerPlayer value) {return !closed&&value!=null&&player.get()==value&&value.connection!=null&&connection.get()==value.connection.connection;}
    }
    private QuarantineSyncService() {}
    /** Coalesce refreshed contract images per connection; pending ACKs do not cause repeated publication. */
    public static void ensureParticipationImage(MinecraftServer server,dev.ronova.coremod.root.api.ClientParticipationContract contract) {
        long now=Integer.toUnsignedLong(server.getTickCount());
        for(var member:contract.members()) {
            Session state;
            synchronized(PLAYERS){var map=PLAYERS.get(server);state=map==null?null:map.get(member.endpoint().client());}
            if(state==null)continue;
            synchronized(state) {
                var player=state.player.get();var endpoint=member.endpoint();
                if(player==null||!state.current(player)||!state.stream.equals(endpoint.stream())||state.context!=endpoint.context())continue;
                if(state.receipt!=null&&state.receipt.expects(endpoint.minimumRevision(),contract.epoch(),contract.imageDigest()))continue;
                if(state.lastParticipationSendTick!=Long.MIN_VALUE&&now>=state.lastParticipationSendTick&&now-state.lastParticipationSendTick<20)continue;
                state.lastParticipationSendTick=now;sendSnapshot(player,state);
            }
        }
    }
    private static Session session(ServerPlayer player) {
        if(player==null||player.server==null||player.connection==null||!player.server.isSameThread())return null;
        synchronized(PLAYERS) {
            var players=PLAYERS.computeIfAbsent(player.server,ignored->new LinkedHashMap<>());
            Session prior=players.get(player.getUUID());
            if(prior!=null)synchronized(prior){if(prior.current(player))return prior;close(player.server,prior);}
            Session created=new Session(player);players.put(player.getUUID(),created);return created;
        }
    }
    public static void registerPlayer(ServerPlayer player) {
        if(player==null||player.server==null||!player.server.isSameThread())return;
        ServerRuntimeRegistry.runtime(player.server); // Login/clone runs on the server thread and initializes the authoritative view.
        Session state=session(player);if(state==null)return;
        synchronized(state){sendOwner(player,state);sendSnapshot(player,state);}
    }
    public static void sendOwnerState(ServerPlayer player) {
        Session state=session(player);if(state==null)return;
        synchronized(state){sendOwner(player,state);}
    }
    private static void sendOwner(ServerPlayer player,Session state) {
        if(!state.current(player)||!player.server.isSameThread())return;
        var runtime=ServerRuntimeRegistry.getIfPresent(player.server);if(runtime==null||runtime.durableStore()==null)return;
        var stamp=runtime.durableStore().authorityStamp(player.getUUID());if(!stamp.settled())return;
        RonovaChannel.sendToPlayer(player,new RonovaPackets.OwnerState(player.getUUID(),stamp.owner(),stamp.subjectKey().realmId().value(),
            state.stream,state.context,stamp.ownerAuthorizationRevision()));
    }
    public static void releasePlayer(ServerPlayer player) {
        if(player==null)return;
        synchronized(PLAYERS){var players=PLAYERS.get(player.server);if(players==null)return;
            var state=players.get(player.getUUID());if(state!=null)synchronized(state){
                if(state.current(player)){close(player.server,state);players.remove(player.getUUID());}}
            if(players.isEmpty())PLAYERS.remove(player.server);
        }
    }
    public static Set<UUID> clearServerState(MinecraftServer server) {
        DIGESTS.remove(server);
        synchronized(PLAYERS){var old=PLAYERS.remove(server);
            if(old!=null)for(var state:old.values())synchronized(state){close(server,state);}
            QUEUES.remove(server);return old==null?Set.of():Set.copyOf(old.keySet());}
    }
    public static void broadcastRemoval(MinecraftServer server,UUID uuid) {
        if(server==null||uuid==null)return;
        var runtime=ServerRuntimeRegistry.getIfPresent(server);if(runtime==null)return;
        if(runtime.projection().admission().hasTombstone(uuid)){broadcastSnapshot(server);return;}
        publish(server,ProjectionFrame.Mode.REMOVE,List.of(),uuid);
    }
    public static void broadcastClear(MinecraftServer server) {
        if(server==null)return;
        var runtime=ServerRuntimeRegistry.getIfPresent(server);if(runtime==null)return;
        if(runtime.projection().admission().tombstoneCount()!=0){broadcastSnapshot(server);return;}
        // A clear supersedes every unacknowledged old image. REPLACE may bridge revisions the client never committed.
        for(var state:sessions(server))synchronized(state){var player=state.player.get();if(!state.current(player))continue;
            discard(server,state);state.receipt=null;state.sentRevision=0;state.dirty=true;state.delivery.queued();
            send(player,state,ProjectionFrame.Mode.REPLACE,List.of(),null);
        }
    }
    public static void broadcastMarks(MinecraftServer server,List<JudgmentMark> marks) {
        if(server==null||marks==null||marks.isEmpty())return;
        var runtime=ServerRuntimeRegistry.getIfPresent(server);if(runtime==null)return;
        List<JudgmentMark> current=new ArrayList<>();Set<UUID> seen=new HashSet<>();
        for(var mark:marks){var actual=runtime.projection().admission().mark(mark.uuid());
            if(actual==null){broadcastSnapshot(server);return;}if(seen.add(actual.uuid()))current.add(actual);}
        publish(server,ProjectionFrame.Mode.APPEND,current,null);
    }
    private static List<Session> sessions(MinecraftServer server) {
        synchronized(PLAYERS){var map=PLAYERS.get(server);return map==null?List.of():List.copyOf(map.values());}
    }
    public static void broadcastSnapshot(MinecraftServer server) {
        for(var state:sessions(server))synchronized(state){var player=state.player.get();if(state.current(player))sendSnapshot(player,state);}
    }
    private static void publish(MinecraftServer server,ProjectionFrame.Mode mode,List<JudgmentMark> marks,UUID identity) {
        for(var state:sessions(server))synchronized(state){var player=state.player.get();if(state.current(player))send(player,state,mode,marks,identity);}
    }
    private static void sendSnapshot(ServerPlayer player,Session state) {
        if(!state.current(player)||!player.server.isSameThread())return;
        if(Integer.toUnsignedLong(player.server.getTickCount())<state.delivery.retryAt()){state.dirty=true;return;}
        var runtime=ServerRuntimeRegistry.getIfPresent(player.server);if(runtime==null)return;
        sendOwner(player,state);send(player,state,ProjectionFrame.Mode.REPLACE,runtime.projection().admission().marks(),null);
    }
    private static void send(ServerPlayer player,Session state,ProjectionFrame.Mode mode,List<JudgmentMark> marks,UUID identity) {
        var runtime=ServerRuntimeRegistry.getIfPresent(player.server);
        if(runtime==null||!player.server.isSameThread()||!state.current(player)||!player.connection.connection.isConnected())return;
        long now=Integer.toUnsignedLong(player.server.getTickCount());
        if(now<state.delivery.retryAt()){state.dirty=true;return;}
        if(marks.size()>ProjectionFrame.MAX_MARKS){state.dirty=true;state.delivery.failed(ProjectionDeliveryState.Stage.CAPACITY,now);return;}
        var epoch=runtime.projection().snapshot().epoch();
        if(runtime.durableStore()==null||epoch.compareTo(runtime.durableStore().epoch())<0||runtime.durableStore().persistencePending()){state.dirty=true;state.delivery.failed(ProjectionDeliveryState.Stage.ROOT_WAIT,now);return;}
        DigestImage image=DIGESTS.get(player.server);
        if(image==null||!image.epoch().equals(epoch)) {
            var fullImage=runtime.projection().admission().marks();
            if(fullImage.size()>ProjectionFrame.MAX_MARKS){state.dirty=true;state.delivery.failed(ProjectionDeliveryState.Stage.CAPACITY,now);return;}
            image=new DigestImage(epoch,ProjectionDigest.of(fullImage));DIGESTS.put(player.server,image);
        }
        // Keep one bounded image per connection. Later changes coalesce into a fresh Root snapshot after its ACK.
        if(state.outgoing!=null||state.receipt!=null&&!state.receipt.current()){state.dirty=true;return;}
        var queue=queue(player.server);
        long retained=ProjectionTransmission.estimateRetainedBytes(marks);
        synchronized(queue){if(retained>ProjectionTransmission.MAX_RETAINED_BYTES||retained>SERVER_QUEUE_BYTES-queue.retained){
            queue.denied++;state.dirty=true;state.delivery.failed(ProjectionDeliveryState.Stage.QUEUE_BUDGET,now);return;}}
        long base=state.revision,next=Math.addExact(base,1);
        var permit=runtime.resources().reserve(Map.of(dev.ronova.coremod.budget.HostResourceDomain.JAVA_HEAP,retained),
            dev.ronova.coremod.budget.HostResourceGovernor.Priority.NORMAL);
        if(!permit.granted()){synchronized(queue){queue.denied++;}state.dirty=true;state.delivery.failed(ProjectionDeliveryState.Stage.HEAP_BUDGET,now);return;}
        ProjectionTransmission outgoing;
        try {outgoing=new ProjectionTransmission(runtime.durableStore().realmId().value(),state.stream,state.context,next,base,epoch,mode,marks,identity);}
        catch(RuntimeException failure){permit.reservation().close();throw failure;}
        state.reservation=permit.reservation();
        synchronized(queue){queue.retained+=outgoing.retainedBytes();}
        state.outgoing=outgoing;state.revision=next;state.dirty=false;state.delivery.queued();
        if(state.receipt==null)state.receipt=new ProjectionReceiptState(runtime.durableStore().realmId().value(),state.stream,state.context);
        state.receipt.expect(next,epoch,image.digest());
    }
    /** Server-tick fair transport pump. No packet burst is emitted from an action callback. */
    public static void pulse(MinecraftServer server) {
        if(server==null||!server.isSameThread())return;
        var states=sessions(server);if(states.isEmpty())return;
        var queue=queue(server);long now=Integer.toUnsignedLong(server.getTickCount());int start;
        synchronized(queue){queue.tick.begin(now);if(queue.tick.exhausted())return;start=Math.floorMod(queue.cursor,states.size());}
        int nextStart=start;
        boolean serverFull=false;
        for(int n=0;n<states.size()&&!serverFull;n++) {
            int index=(start+n)%states.size();var state=states.get(index);nextStart=(index+1)%states.size();
            synchronized(state) {
                var player=state.player.get();state.tick.begin(now);
                if(player==null||!state.current(player)||!player.connection.connection.isConnected()){close(server,state);continue;}
                if(state.outgoing==null&&state.receipt!=null&&!state.receipt.current()&&state.sentAtTick>=0
                    &&(now<state.sentAtTick||now-state.sentAtTick>=100)){
                    state.receipt=null;state.dirty=true;state.delivery.failed(ProjectionDeliveryState.Stage.ACK_TIMEOUT,now);
                }
                if(state.outgoing==null&&state.dirty&&now>=state.delivery.retryAt()&&(state.receipt==null||state.receipt.current()))sendSnapshot(player,state);
                while(state.outgoing!=null) {
                    long cost=state.outgoing.nextWireBytes();if(!state.tick.fits(cost))break;
                    synchronized(queue){
                        if(!queue.tick.fits(cost)){serverFull=true;nextStart=index;break;}
                        // Failed sends still consume this tick's allowance, bounding repeated failing callbacks.
                        queue.tick.charge(cost);state.tick.charge(cost);
                    }
                    try { RonovaChannel.sendToPlayer(player,new RonovaPackets.QuarantineSync(state.outgoing.next())); }
                    catch(RuntimeException unavailable){discard(server,state);state.receipt=null;state.dirty=true;
                        state.delivery.failed(ProjectionDeliveryState.Stage.TRANSPORT,now);break;}
                    state.outgoing.sent();synchronized(queue){queue.parts++;queue.bytes+=cost;}
                    if(state.outgoing.complete()){state.sentRevision=state.outgoing.revision();state.sentAtTick=now;state.delivery.sent();discard(server,state);}
                }
            }
        }
        synchronized(queue){queue.cursor=nextStart;}
        // Remove only the exact closed endpoint; reconnects with the same UUID keep their new session.
        synchronized(PLAYERS){var map=PLAYERS.get(server);if(map!=null)map.values().removeIf(state->{synchronized(state){return state.closed;}});}
    }

    public static void resync(ServerPlayer player,RonovaPackets.ProjectionResyncRequest request) {
        if(player==null||player.server==null)return;
        Session state; synchronized(PLAYERS){var map=PLAYERS.get(player.server);state=map==null?null:map.get(player.getUUID());}
        if(state==null||!player.server.isSameThread())return;
        synchronized(state) {
            if(!state.current(player))return;
            var runtime=ServerRuntimeRegistry.getIfPresent(player.server);long tick=Integer.toUnsignedLong(player.server.getTickCount());
            if(runtime==null||!state.stream.equals(request.streamId())||state.context!=request.contextRevision()
                ||!runtime.durableStore().realmId().value().equals(request.realmId())
                ||state.lastResyncTick!=Long.MIN_VALUE&&tick-state.lastResyncTick>=0&&tick-state.lastResyncTick<20)return;
            state.lastResyncTick=tick;RESYNCS.incrementAndGet();
            discard(player.server,state);state.receipt=null;state.dirty=false;sendSnapshot(player,state);
        }
    }
    public static void acknowledge(ServerPlayer player,ProjectionAcknowledgement acknowledgement) {
        if(player==null||player.server==null||!player.server.isSameThread())return;
        var state=find(player.server,player.getUUID());if(state==null)return;
        boolean mismatch;
        synchronized(state) {
            if(!state.current(player)||state.receipt==null||state.outgoing!=null||state.sentRevision!=acknowledgement.revision())return;
            boolean accepted=state.receipt.accept(acknowledgement,Integer.toUnsignedLong(player.server.getTickCount()));
            if(accepted)state.delivery.confirmed();
            mismatch=!accepted&&state.receipt.matchesPublication(acknowledgement);
        }
        // Resync looks up the endpoint again without holding its monitor; session replacement takes the reverse lookup order.
        if(mismatch)resync(player,new RonovaPackets.ProjectionResyncRequest(acknowledgement.realm(),acknowledgement.stream(),acknowledgement.context(),acknowledgement.revision()));
    }
    public static boolean projectionAcknowledged(ServerPlayer player) {
        if(player==null||player.server==null)return false;
        var state=find(player.server,player.getUUID());if(state==null)return false;
        synchronized(state){return state.current(player)&&player.connection.connection.isConnected()&&state.receipt!=null
            &&state.outgoing==null&&!state.dirty&&state.sentRevision==state.revision&&state.receipt.current();}
    }
}
