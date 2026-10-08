package dev.ronova.coremod.capability.recovery;

import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.integrity.RawAccessorDispatch;
import dev.ronova.coremod.mixin.EntityStateAccessor;
import dev.ronova.coremod.policy.*;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Restores identity cells from an existing physical anchor and current Root Owner qualification. */
public final class RegisteredOwnerIdentity {
    private static final Set<StrengthOptionKey> KEYS=Set.of(StrengthOptionKey.PROTECT_IDENTITY_RESPAWN,
        StrengthOptionKey.PROTECT_AUTO_REPAIR,StrengthOptionKey.RUNTIME_NATIVE,StrengthOptionKey.RUNTIME_JVMTI,
        StrengthOptionKey.PROTECT_CODE_INTEGRITY,StrengthOptionKey.RUNTIME_CRITICAL_CLASS_WATCH);
    private RegisteredOwnerIdentity() {}
    public static boolean repair(ServerPlayer player,UUID expected) {
        if(player==null||expected==null||player.server==null||!player.server.isSameThread())return false;
        var runtime=ServerRuntimeRegistry.getIfPresent(player.server);
        if(runtime==null||runtime.durableStore()==null||runtime.authorityState().shuttingDown())return false;
        var anchors=runtime.authorityState();
        if(anchors.anchor(expected)!=player||!expected.equals(anchors.anchorId(player)))return false;
        var policy=runtime.strengthPolicy();if(policy.pendingRestart())return false;
        for(var key:KEYS)if(ProfilePresetMatrix.resolve(policy.activeMatrixRevision(),policy.activeProfile(),key,
            policy.activeLevels().get(key)).requestedLevel().rank()<MechanismLevel.PROTECT_RONOVA.rank())return false;
        var connection=player.connection;
        if(connection==null||connection.player!=player)return false;
        var listed=player.server.getPlayerList().getPlayer(expected);
        if(listed!=null&&listed!=player)return false;
        var store=runtime.durableStore();var stamp=store.authorityStamp(expected);
        if(stamp==null||!stamp.owner()||!stamp.settled())return false;
        try(var observation=runtime.enforcement().beginObservation(1)) {
            if(observation==null||!RawAccessorDispatch.matches(player,Entity.class,EntityStateAccessor.class))return false;
            var state=(EntityStateAccessor)player;var level=state.ronova$getLevel();
            if(!(level instanceof ServerLevel current)||current.getServer()!=player.server
                ||player.server.getLevel(current.dimension())!=level)return false;
            String text=expected.toString();
            try(var lease=store.acquireOwnerWrite(stamp)) {
                if(lease==null||!lease.valid(stamp)||!observation.stillCurrent()||anchors.shuttingDown()
                    ||anchors.anchor(expected)!=player||!expected.equals(anchors.anchorId(player))
                    ||player.connection!=connection||connection.player!=player||state.ronova$getLevel()!=level)return false;
                if(!expected.equals(state.ronova$getUuid()))state.ronova$setUuid(expected);
                if(!lease.valid(stamp)||!observation.stillCurrent()||anchors.shuttingDown()
                    ||anchors.anchor(expected)!=player||player.connection!=connection||connection.player!=player)return false;
                if(!text.equals(state.ronova$getStringUuid()))state.ronova$setStringUuid(text);
                return expected.equals(state.ronova$getUuid())&&text.equals(state.ronova$getStringUuid())
                    &&state.ronova$getLevel()==level&&anchors.anchor(expected)==player&&player.connection==connection
                    &&connection.player==player&&lease.valid(stamp)&&!anchors.shuttingDown()&&observation.finish();
            }
        }
    }
}
