package dev.ronova.coremod.presence;

import dev.ronova.coremod.RonovaDominion;
import dev.ronova.coremod.contract.SubjectIdentity;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 7.14C owner and Dominion-self immunity.
 *
 * <p>Immunity is evaluated here rather than at a caller, so a future caller
 * cannot bypass it by skipping a short-circuit. Two identity sources exist and
 * neither depends on a successful authority resolution:</p>
 *
 * <ul>
 *   <li><b>owner</b>: the durable owner UUID set supplied by the containing
 *       projection. When that lookup is unavailable or throws, the subject is
 *       treated as immune (fail-static to {@code present=true}).</li>
 *   <li><b>Dominion-self</b>: a stateless predicate over the subject's own
 *       registry namespace. It references only this mod's namespace and never a
 *       third-party mod ID, class name or entity name.</li>
 * </ul>
 */
public final class OwnerImmunity {
    /** Immunity verdict, kept explicit so the reason can be surfaced in decisions. */
    public enum Kind {
        NONE("not-immune"),
        OWNER("owner-immune"),
        DOMINION_SELF("dominion-self-immune"),
        UNRESOLVED("owner-lookup-unavailable");

        private final String reason;

        Kind(String reason) {
            this.reason = reason;
        }

        public String reason() {
            return reason;
        }

        public boolean immune() {
            return this != NONE;
        }
    }

    /** Namespace prefix of every EntityType this mod registers itself. */
    private static final String SELF_NAMESPACE = RonovaDominion.MOD_ID + ":";

    private final Predicate<UUID> owners;

    private OwnerImmunity(Predicate<UUID> owners) {
        this.owners = Objects.requireNonNull(owners, "owners");
    }

    /** Immunity backed by a durable owner-registry lookup. */
    public static OwnerImmunity of(Predicate<UUID> owners) {
        return new OwnerImmunity(owners);
    }

    /** Immunity backed by a fixed owner set, used by focused tests and the client. */
    public static OwnerImmunity ofOwners(Set<UUID> owners) {
        Set<UUID> copy = owners == null ? Set.of() : Set.copyOf(owners);
        return new OwnerImmunity(copy::contains);
    }

    /**
     * Immunity with no reachable owner registry. Dominion-self immunity still
     * applies; owner immunity cannot be decided and therefore fails static.
     */
    public static OwnerImmunity unresolved() {
        return new OwnerImmunity(uuid -> {
            throw new IllegalStateException("owner registry is unavailable");
        });
    }

    /** True when the subject's registry type belongs to this mod's own namespace. */
    public static boolean dominionSelf(SubjectIdentity subject) {
        return subject != null && subject.registryType().startsWith(SELF_NAMESPACE);
    }

    /**
     * Classifies the subject. A failing owner lookup yields {@link Kind#UNRESOLVED}
     * rather than propagating: immunity must not depend on authority resolution
     * succeeding.
     */
    public Kind classify(SubjectIdentity subject) {
        if (subject == null) {
            return Kind.UNRESOLVED;
        }
        if (dominionSelf(subject)) {
            return Kind.DOMINION_SELF;
        }
        UUID uuid = subject.uuid();
        if (uuid == null) {
            return Kind.NONE;
        }
        try {
            return owners.test(uuid) ? Kind.OWNER : Kind.NONE;
        } catch (RuntimeException | LinkageError failure) {
            return Kind.UNRESOLVED;
        }
    }

    public boolean immune(SubjectIdentity subject) {
        return classify(subject).immune();
    }
}
