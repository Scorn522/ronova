package dev.ronova.coremod.engine;

import dev.ronova.coremod.adapter.MinecraftSubjectAdapter;
import dev.ronova.coremod.authority.AuthorityView;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.SubjectIdentity;
import dev.ronova.coremod.presence.OwnerImmunity;
import dev.ronova.coremod.presence.PresencePolicy;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

/**
 * 7.14C client-side half of the quarantine view.
 *
 * <p>The client has no projection bundle, so it holds its own lineage index and its
 * own presence policy. The policy is backed by the client owner cache rather than a
 * server projection; when that cache cannot answer, {@link OwnerImmunity} fails
 * static to visible, which is the safe direction for a render cancellation.</p>
 */
final class ClientQuarantineView {
    private static final LineageIndex<JudgmentMark> MARKS = new LineageIndex<>();
    private static final PresencePolicy PRESENCE = new PresencePolicy(
        OwnerImmunity.of(uuid -> AuthorityView.client().owns(uuid))
    );

    private ClientQuarantineView() {}

    static void acceptMarks(List<JudgmentMark> marks) {
        if (marks == null || marks.isEmpty()) {
            return;
        }
        synchronized (MARKS) {
            for (JudgmentMark mark : marks) {
                if (mark != null) {
                    MARKS.put(mark);
                }
            }
        }
    }

    /** Applies a complete server snapshot without exposing a partially rebuilt index. */
    static void replaceMarks(List<JudgmentMark> marks) {
        synchronized (MARKS) {
            MARKS.replaceAll(marks);
        }
    }

    static void removeMark(UUID uuid) {
        if (uuid == null) {
            return;
        }
        synchronized (MARKS) {
            MARKS.remove(uuid);
        }
    }

    static void clearMarks() {
        MARKS.clear();
    }

    static int markCount() {
        return MARKS.size();
    }

    /** Resolves the raw client quarantine flag from the replicated lineage index. */
    static boolean quarantined(Entity entity) {
        synchronized (MARKS) {
            String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            String dimension = entity.level().dimension().location().toString();
            boolean directOrSpatial = MARKS.matches(
                entity.getUUID(), type, dimension,
                entity.getX(), entity.getY(), entity.getZ()
            );
            return directOrSpatial || (!entity.isAddedToWorld()
                && MARKS.matchesDetachedLineage(type, dimension));
        }
    }

    /** Adjudicates client-side presence through the client-owned presence policy. */
    static boolean matches(Entity entity) {
        SubjectIdentity subject = MinecraftSubjectAdapter.from(entity);
        return !PRESENCE.decide(
            subject, DominionEpoch.ZERO, quarantined(entity)
        ).present();
    }

    /** Client presence counters, exposed for the 7.14C self-test only. */
    static PresencePolicy presence() {
        return PRESENCE;
    }
}
