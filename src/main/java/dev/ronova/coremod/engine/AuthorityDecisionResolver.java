package dev.ronova.coremod.engine;

import dev.ronova.coremod.authority.AuthorityView;
import dev.ronova.coremod.adapter.MinecraftSubjectAdapter;
import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.contract.DominionDecision;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Compatibility adapter for the former engine resolver.
 *
 * <p>The actual read boundary now lives in {@link AuthorityView}; this class
 * performs no binding, tag write, anchor update, projection cleanup, or Root
 * mutation and remains only for source/ABI compatibility with older callers.</p>
 */
final class AuthorityDecisionResolver {
    private AuthorityDecisionResolver() {}

    static DominionDecision.Authority resolve(Player player) {
        if (player == null) {
            return null;
        }
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.server != null) {
            return ServerRuntimeRegistry.runtime(serverPlayer.server).authorityView().resolve(
                player, MinecraftSubjectAdapter.from(player)
            );
        }
        return AuthorityView.client().resolve(player, MinecraftSubjectAdapter.from(player));
    }
}
