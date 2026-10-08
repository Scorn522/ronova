package dev.ronova.coremod.engine;

import dev.ronova.coremod.adapter.MinecraftSubjectAdapter;
import dev.ronova.coremod.authority.AuthorityView;
import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.SubjectIdentity;
import dev.ronova.coremod.projection.PresenceProjection;
import dev.ronova.coremod.projection.ProjectionBundle;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Emergency 7.14C rollback path.
 *
 * <p>This class intentionally reproduces the 7.14B behaviour: presence merely echoes
 * the caller-supplied quarantine flag through {@link PresenceProjection}, and owner
 * immunity depends on a caller short-circuit against {@link AuthorityView} instead of
 * being owned by the presence policy. It is selected only when the 7.14C presence
 * split flag is explicitly false; evidence produced in this mode cannot satisfy the
 * 7.14C gate, because a caller that skips the short-circuit loses immunity.</p>
 */
final class LegacyQuarantineView {
    private static final PresenceProjection CLIENT_PRESENCE = new PresenceProjection();
    private static final PresenceProjection SERVER_PRESENCE = new PresenceProjection();

    private LegacyQuarantineView() {}

    static boolean matches(Entity entity) {
        boolean quarantined;
        DominionEpoch epoch;
        PresenceProjection presence;
        if (entity.level() instanceof ServerLevel serverLevel) {
            MinecraftServer server = serverLevel.getServer();
            if (ServerRuntimeRegistry.runtime(server).authorityView().isProtected(entity)) {
                return false;
            }
            ProjectionBundle projections =
                ServerRuntimeRegistry.runtime(server).projection();
            quarantined = ServerQuarantineView.quarantined(
                server, entity, MinecraftSubjectAdapter.from(entity), projections
            );
            epoch = projections.snapshot().epoch();
            presence = SERVER_PRESENCE;
        } else {
            if (AuthorityView.client().isProtected(entity)) {
                return false;
            }
            quarantined = ClientQuarantineView.quarantined(entity);
            epoch = DominionEpoch.ZERO;
            presence = CLIENT_PRESENCE;
        }
        SubjectIdentity subject = MinecraftSubjectAdapter.from(entity);
        return !presence.decide(subject, epoch, quarantined).present();
    }
}
