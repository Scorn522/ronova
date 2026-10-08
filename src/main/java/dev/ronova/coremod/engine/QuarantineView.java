package dev.ronova.coremod.engine;

import dev.ronova.coremod.presence.PresenceSplitFlags;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Generic quarantine facade shared by world queries, render cancellation and the
 * execution engine. It keys by object identity, UUID, registry type, dimension and
 * declared lineage; it contains no knowledge of another mod's names.
 *
 * <p>Since 7.14C the decision itself lives in two side-specific halves, selected by
 * {@link PresenceSplitFlags}:</p>
 *
 * <ul>
 *   <li>{@link ServerQuarantineView} reads the published projection bundle, where the
 *       presence policy owns owner and Dominion-self immunity.</li>
 *   <li>{@link ClientQuarantineView} reads the replicated lineage index and the client
 *       owner cache.</li>
 *   <li>{@link LegacyQuarantineView} reproduces the former 7.14B caller-short-circuit
 *       behaviour when the split is explicitly disabled.</li>
 * </ul>
 *
 * <p>Registry mutation stays on the half that owns the state, so this facade holds no
 * quarantine state of its own.</p>
 */
public final class QuarantineView {
    private static final PresenceSplitFlags.Selection SPLIT = PresenceSplitFlags.resolve();

    private QuarantineView() {}

    /** True when the 7.14C presence split is active rather than the rollback path. */
    public static boolean splitEnabled() {
        return SPLIT.enabled();
    }

    /** Resolved presence split mode, surfaced by diagnostics and the composition root. */
    public static PresenceSplitFlags.Selection splitSelection() {
        return SPLIT;
    }

    public static void register(MinecraftServer server, Entity entity) {
        ServerQuarantineView.register(server, entity);
    }
    public static boolean registered(MinecraftServer server, Entity entity) { return ServerQuarantineView.registered(server, entity); }

    public static void acceptClientMarks(List<JudgmentMark> marks) {
        ClientQuarantineView.acceptMarks(marks);
    }

    /** Applies a complete server snapshot without exposing a partially rebuilt index. */
    public static void replaceClientMarks(List<JudgmentMark> marks) {
        ClientQuarantineView.replaceMarks(marks);
    }

    public static void removeClientMark(UUID uuid) {
        ClientQuarantineView.removeMark(uuid);
    }

    public static boolean matchesObject(Object value) {
        return value instanceof Entity entity && matches(entity);
    }

    public static boolean matches(Entity entity) {
        if (!ExecutionTargetPolicy.isCombatObject(entity)) {
            return false;
        }
        if (!SPLIT.enabled()) {
            return LegacyQuarantineView.matches(entity);
        }
        return entity.level() instanceof ServerLevel serverLevel
            ? ServerQuarantineView.matches(entity, serverLevel)
            : ClientQuarantineView.matches(entity);
    }

    public static List<Entity> filter(List<Entity> source) {
        if (source == null || source.isEmpty()) {
            return source;
        }
        List<Entity> filtered = null;
        for (int index = 0; index < source.size(); index++) {
            Entity entity = source.get(index);
            if (matches(entity)) {
                if (filtered == null) {
                    filtered = new ArrayList<>(source.subList(0, index));
                }
            } else if (filtered != null) {
                filtered.add(entity);
            }
        }
        return filtered == null ? source : filtered;
    }

    public static int objectCount(MinecraftServer server) {
        return ServerQuarantineView.objectCount(server);
    }

    public static void clearServer(MinecraftServer server) {
        ServerQuarantineView.clearServer(server);
    }

    public static void clearIdentity(MinecraftServer server, UUID uuid) {
        ServerQuarantineView.clearIdentity(server, uuid);
    }

    public static void clearClientMarks() {
        ClientQuarantineView.clearMarks();
    }

    public static int clientMarkCount() {
        return ClientQuarantineView.markCount();
    }
}
