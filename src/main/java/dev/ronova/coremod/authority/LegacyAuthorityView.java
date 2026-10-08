package dev.ronova.coremod.authority;

import dev.ronova.coremod.budget.FrameBudgetGovernor;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.projection.ProjectionBundle;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Emergency 7.14B rollback adapter.
 *
 * <p>Unlike the default {@link AuthorityView}, this adapter intentionally
 * reproduces the former query-time synchronization of runtime markers and
 * anchors. It is selected only when the 7.14B split flag is explicitly false;
 * evidence produced in this mode cannot satisfy the 7.14B gate.</p>
 */
final class LegacyAuthorityView extends AuthorityView {
    LegacyAuthorityView(
        MinecraftServer server,
        Supplier<ProjectionBundle> projections
    ) {
        this(server, projections, null);
    }

    LegacyAuthorityView(
        MinecraftServer server,
        Supplier<ProjectionBundle> projections,
        FrameBudgetGovernor frameBudgetGovernor
    ) {
        super(server, projections, frameBudgetGovernor);
    }

    @Override
    public DominionDecision.Authority resolve(
        Player player,
        dev.ronova.coremod.contract.SubjectIdentity subject
    ) {
        DominionDecision.Authority authority = super.resolve(player, subject);
        synchronize(player, authority);
        return authority;
    }

    private void synchronize(
        Player player,
        DominionDecision.Authority authority
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)
            || serverPlayer.server != server) {
            return;
        }
        if (authority != null && authority.authorized()) {
            AuthorityBinder.restoreLegacyProjection(serverPlayer, authority);
        } else {
            AuthorityBinder.clearProjection(serverPlayer, false);
        }
    }
}
