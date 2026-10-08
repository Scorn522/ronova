package dev.ronova.coremod.authority;

import dev.ronova.coremod.adapter.MinecraftSubjectAdapter;
import dev.ronova.coremod.budget.BudgetTelemetrySnapshot;
import dev.ronova.coremod.budget.FrameBudgetGovernor;
import dev.ronova.coremod.contract.Capability;
import dev.ronova.coremod.contract.CapabilitySet;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.SubjectIdentity;
import dev.ronova.coremod.projection.ProjectionBundle;
import dev.ronova.coremod.projection.ProjectionCoordinator;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Immutable local Authority query boundary.
 *
 * <p>This type is deliberately read-only: it never binds an owner, writes an
 * entity tag, clears a projection, touches an anchor, or submits a Root command.
 * Server instances read the latest atomically published projection; the shared
 * client instance reads only the client-side recovery cache and marker.</p>
 */
public class AuthorityView {
    public static final String BOUND_TAG = "ronova.bound";

    private static final AuthorityView CLIENT = new AuthorityView();
    private static final Map<MinecraftServer, AuthorityView> SERVERS =
        Collections.synchronizedMap(new WeakHashMap<>());

    protected final MinecraftServer server;
    private final Supplier<ProjectionBundle> projections;
    private final FrameBudgetGovernor frameBudgetGovernor;

    /** Creates a server-bound view backed by the supplied projection accessor. */
    public AuthorityView(
        MinecraftServer server,
        Supplier<ProjectionBundle> projections
    ) {
        this(server, projections, null);
    }

    /** Creates a server-bound view with measurement-only 7.14D telemetry. */
    public AuthorityView(
        MinecraftServer server,
        Supplier<ProjectionBundle> projections,
        FrameBudgetGovernor frameBudgetGovernor
    ) {
        this.server = server;
        this.projections = Objects.requireNonNull(projections, "projections");
        this.frameBudgetGovernor = frameBudgetGovernor;
    }

    /** Creates a projection-only view, useful for loader-neutral focused tests. */
    public AuthorityView(Supplier<ProjectionBundle> projections) {
        this(null, projections, null);
    }

    /** Creates a projection-only view with measurement-only 7.14D telemetry. */
    public AuthorityView(
        Supplier<ProjectionBundle> projections,
        FrameBudgetGovernor frameBudgetGovernor
    ) {
        this(null, projections, frameBudgetGovernor);
    }

    private AuthorityView() {
        this.server = null;
        this.projections = null;
        this.frameBudgetGovernor = null;
    }

    /** Returns a read-only view for one server's current projection. */
    public static AuthorityView forServer(MinecraftServer server) {
        if (server == null) {
            throw new IllegalArgumentException("server is required");
        }
        synchronized (SERVERS) {
            return SERVERS.computeIfAbsent(server, value -> {
                AuthoritySplitFlags.Selection selection = AuthoritySplitFlags.resolve();
                return selection.enabled()
                    ? new AuthorityView(value, () -> ProjectionCoordinator.current(value))
                    : new LegacyAuthorityView(value, () -> ProjectionCoordinator.current(value));
            });
        }
    }

    /** Attaches the selected view to an already-created projection coordinator. */
    public static AuthorityView attach(
        MinecraftServer server,
        ProjectionCoordinator coordinator,
        AuthoritySplitFlags.Selection selection
    ) {
        if (coordinator == null) {
            throw new IllegalArgumentException("coordinator is required");
        }
        return attach(server, coordinator, selection, coordinator.frameBudgetGovernor());
    }

    /** Attaches the selected view with optional measurement-only 7.14D telemetry. */
    public static AuthorityView attach(
        MinecraftServer server,
        ProjectionCoordinator coordinator,
        AuthoritySplitFlags.Selection selection,
        FrameBudgetGovernor frameBudgetGovernor
    ) {
        if (server == null || coordinator == null || selection == null) {
            throw new IllegalArgumentException(
                "server, coordinator, and Authority selection are required"
            );
        }
        synchronized (SERVERS) {
            AuthorityView view = selection.enabled()
                ? new AuthorityView(server, coordinator::published, frameBudgetGovernor)
                : new LegacyAuthorityView(
                    server, coordinator::published, frameBudgetGovernor
                );
            SERVERS.put(server, view);
            return view;
        }
    }

    /** Attaches the default 7.14B pure-query view. */
    public static AuthorityView attach(
        MinecraftServer server,
        ProjectionCoordinator coordinator
    ) {
        return attach(
            server,
            coordinator,
            new AuthoritySplitFlags.Selection(
                true, AuthoritySplitFlags.Source.DEFAULT, null
            )
        );
    }

    /** Selects the server projection or the client recovery view for a player. */
    public static AuthorityView forPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.server != null) {
            return forServer(serverPlayer.server);
        }
        return client();
    }

    /** Selects the server projection or the client recovery view for an entity. */
    public static AuthorityView forEntity(Entity entity) {
        return entity instanceof Player player ? forPlayer(player) : client();
    }

    /** Releases the cached view for a stopped server. */
    public static void release(MinecraftServer server) {
        if (server != null) {
            synchronized (SERVERS) {
                SERVERS.remove(server);
            }
        }
    }

    /** Returns the read-only client fallback view. */
    public static AuthorityView client() {
        return CLIENT;
    }

    /** Returns the optional measurement-only 7.14D telemetry sink. */
    public FrameBudgetGovernor frameBudgetGovernor() {
        return frameBudgetGovernor;
    }

    /** Resolves a Minecraft player without performing any binding side effect. */
    public DominionDecision.Authority resolve(Player player) {
        if (player == null) {
            return null;
        }
        return resolve(player, MinecraftSubjectAdapter.from(player));
    }

    /**
     * Resolves an already adapted subject. Keeping this overload explicit lets
     * adapter callers avoid a second conversion while preserving one query
     * implementation.
     */
    public DominionDecision.Authority resolve(
        Player player,
        SubjectIdentity subject
    ) {
        long startedNanos = queryStartedNanos();
        try {
            return resolvePlayer(player, subject);
        } finally {
            recordQueryTelemetry(startedNanos);
        }
    }

    private DominionDecision.Authority resolvePlayer(
        Player player,
        SubjectIdentity subject
    ) {
        if (player == null || subject == null) {
            return null;
        }
        if (server != null) {
            if (!(player instanceof ServerPlayer serverPlayer)
                || serverPlayer.server != server) {
                return denied(subject, DominionEpoch.ZERO, "authority-server-mismatch");
            }
            ProjectionBundle bundle = projections.get();
            return bundle == null
                ? denied(subject, DominionEpoch.ZERO, "authority-projection-unavailable")
                : bundle.authorityTopology().resolve(subject);
        }
        if (projections != null) {
            ProjectionBundle bundle = projections.get();
            return bundle == null
                ? denied(subject, DominionEpoch.ZERO, "authority-projection-unavailable")
                : bundle.authorityTopology().resolve(subject);
        }

        boolean recordedOwner = subject.uuid() != null
            && AuthorityIdentity.contains(subject.uuid());
        if (!recordedOwner) {
            recordedOwner = hasBoundTag(player);
        }
        return new DominionDecision.Authority(
            subject,
            DominionEpoch.ZERO,
            recordedOwner,
            recordedOwner ? CapabilitySet.OWNER_DEFAULT : CapabilitySet.NONE,
            recordedOwner ? "client-owner-projection" : "client-not-owner-projection"
        );
    }

    /** Resolves a loader-neutral subject from this projection without mutation. */
    public DominionDecision.Authority resolve(SubjectIdentity subject) {
        long startedNanos = queryStartedNanos();
        try {
            if (subject == null) {
                return null;
            }
            if (projections == null) {
                return denied(subject, DominionEpoch.ZERO, "client-player-required");
            }
            ProjectionBundle bundle = projections.get();
            return bundle == null
                ? denied(subject, DominionEpoch.ZERO, "authority-projection-unavailable")
                : bundle.authorityTopology().resolve(subject);
        } finally {
            recordQueryTelemetry(startedNanos);
        }
    }

    /** Returns whether the entity is an authorized player in this view. */
    public boolean isProtected(Entity entity) {
        return entity instanceof Player player && isProtected(player);
    }

    /** Pure player authorization query. */
    public boolean isProtected(Player player) {
        DominionDecision.Authority authority = resolve(player);
        return authority != null && authority.authorized();
    }

    /** Pure capability query for a player. */
    public boolean hasCapability(Player player, Capability capability) {
        if (player == null || capability == null) {
            return false;
        }
        DominionDecision.Authority authority = resolve(player);
        return authority != null && authority.authorized()
            && authority.capabilities().contains(capability);
    }

    /** Pure capability query for a UUID in the current projection or cache. */
    public boolean hasCapability(UUID uuid, Capability capability) {
        return capability != null && capabilities(uuid).contains(capability);
    }

    /** Returns the projected capability mask without mutation. */
    public CapabilitySet capabilities(UUID uuid) {
        if (uuid == null) {
            return CapabilitySet.NONE;
        }
        if (projections != null) {
            ProjectionBundle bundle = projections.get();
            DominionDecision.Authority authority = bundle == null
                ? null : bundle.authorityTopology().authority(uuid);
            return authority == null || !authority.authorized()
                ? CapabilitySet.NONE : authority.capabilities();
        }
        return AuthorityIdentity.contains(uuid)
            ? CapabilitySet.OWNER_DEFAULT : CapabilitySet.NONE;
    }

    /** Returns whether the current projection owns the UUID. */
    public boolean owns(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        if (projections != null) {
            ProjectionBundle bundle = projections.get();
            return bundle != null && bundle.authorityTopology().owns(uuid);
        }
        return AuthorityIdentity.contains(uuid);
    }

    /** Returns the epoch carried by the current local projection. */
    public DominionEpoch epoch() {
        if (projections == null) {
            return DominionEpoch.ZERO;
        }
        ProjectionBundle bundle = projections.get();
        return bundle == null ? DominionEpoch.ZERO : bundle.snapshot().epoch();
    }

    private long queryStartedNanos() {
        return frameBudgetGovernor == null ? 0L : System.nanoTime();
    }

    private void recordQueryTelemetry(long startedNanos) {
        if (frameBudgetGovernor == null || startedNanos == 0L) {
            return;
        }
        long elapsedUs = Math.max(0L, System.nanoTime() - startedNanos) / 1_000L;
        try {
            frameBudgetGovernor.recordSynchronousQuery(
                BudgetTelemetrySnapshot.QueryKind.AUTHORITY_QUERY, elapsedUs
            );
        } catch (RuntimeException ignored) {
            // Telemetry failure must never alter the synchronous Authority answer.
        }
    }

    private static boolean hasBoundTag(Player player) {
        try {
            var data = player == null ? null : player.getPersistentData();
            return data != null && data.getBoolean(BOUND_TAG);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static DominionDecision.Authority denied(
        SubjectIdentity subject,
        DominionEpoch epoch,
        String reason
    ) {
        return new DominionDecision.Authority(
            subject, epoch, false, CapabilitySet.NONE, reason
        );
    }
}
