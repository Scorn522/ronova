package dev.ronova.coremod.root.api;

import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.SubjectIdentity;
import dev.ronova.coremod.policy.StrengthPolicyConfiguration;
import dev.ronova.coremod.policy.StrengthPolicySnapshot;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/** Loader-neutral mutation vocabulary. Every durable write enters through this boundary. */
public sealed interface DominionCommand permits DominionCommand.BindOwner,
    DominionCommand.UnbindOwner, DominionCommand.CommitJudgment,
    DominionCommand.CommitJudgmentBatch, DominionCommand.ClearTombstones,
    DominionCommand.ClearTombstone, DominionCommand.ConfigureStrengthPolicy, DominionCommand.TransitionSubjects {

    record TransitionSubjects(SubjectTransitionBatch batch) implements DominionCommand {
        public TransitionSubjects { Objects.requireNonNull(batch, "batch"); }
    }

    record BindOwner(UUID commandId, UUID owner) implements DominionCommand {
        public BindOwner {
            Objects.requireNonNull(commandId, "commandId");
            Objects.requireNonNull(owner, "owner");
        }

        public BindOwner(UUID owner) {
            this(UUID.randomUUID(), owner);
        }
    }

    record UnbindOwner(UUID commandId, UUID owner) implements DominionCommand {
        public UnbindOwner {
            Objects.requireNonNull(commandId, "commandId");
            Objects.requireNonNull(owner, "owner");
        }

        public UnbindOwner(UUID owner) {
            this(UUID.randomUUID(), owner);
        }
    }

    record CommitJudgment(
        UUID commandId,
        SubjectIdentity subject,
        DominionDecision.Judgment decision
    ) implements DominionCommand {
        public CommitJudgment {
            Objects.requireNonNull(commandId, "commandId");
            Objects.requireNonNull(subject, "subject");
            Objects.requireNonNull(decision, "decision");
            if (!subject.equals(decision.subject())) {
                throw new IllegalArgumentException("judgment subject mismatch");
            }
            if (!decision.committed()) {
                throw new IllegalArgumentException("committed judgment is required");
            }
        }

        public CommitJudgment(
            SubjectIdentity subject,
            DominionDecision.Judgment decision
        ) {
            this(UUID.randomUUID(), subject, decision);
        }
    }

    /** One action-level atomic judgment mutation. */
    record CommitJudgmentBatch(JudgmentBatch batch) implements DominionCommand {
        public CommitJudgmentBatch {
            Objects.requireNonNull(batch, "batch");
        }
    }

    record ClearTombstones(UUID commandId) implements DominionCommand {
        public ClearTombstones {
            Objects.requireNonNull(commandId, "commandId");
        }

        public ClearTombstones() {
            this(UUID.randomUUID());
        }
    }

    record ClearTombstone(UUID commandId, UUID subject) implements DominionCommand {
        public ClearTombstone {
            Objects.requireNonNull(commandId, "commandId");
            Objects.requireNonNull(subject, "subject");
        }

        public ClearTombstone(UUID subject) {
            this(UUID.randomUUID(), subject);
        }
    }

    /** Persists one complete requested strength image; activation waits for restart. */
    record ConfigureStrengthPolicy(
        UUID commandId, StrengthPolicyConfiguration configuration
    ) implements DominionCommand {
        public ConfigureStrengthPolicy {
            Objects.requireNonNull(commandId, "commandId");
            Objects.requireNonNull(configuration, "configuration");
        }

        public ConfigureStrengthPolicy(StrengthPolicyConfiguration configuration) {
            this(UUID.randomUUID(), configuration);
        }
    }

    /** Returns the durable id that callers must retain across retry and recovery. */
    static UUID commandId(DominionCommand command) {
        Objects.requireNonNull(command, "command");
        if (command instanceof TransitionSubjects transitions) return transitions.batch().commandId();
        if (command instanceof BindOwner bind) {
            return bind.commandId();
        }
        if (command instanceof UnbindOwner unbind) {
            return unbind.commandId();
        }
        if (command instanceof CommitJudgment judgment) {
            return judgment.commandId();
        }
        if (command instanceof CommitJudgmentBatch batch) {
            return batch.batch().commandId();
        }
        if (command instanceof ClearTombstones clear) {
            return clear.commandId();
        }
        if (command instanceof ClearTombstone clear) {
            return clear.commandId();
        }
        if (command instanceof ConfigureStrengthPolicy configure) {
            return configure.commandId();
        }
        throw new IllegalArgumentException("unsupported command: " + command);
    }

    /** Returns the stable payload hash used for idempotency and conflict checks. */
    static String canonicalPayloadHash(DominionCommand command) {
        return canonicalPayloadHash(command, command instanceof ConfigureStrengthPolicy configure
            ? configure.configuration().sourceSchema() : StrengthPolicySnapshot.SCHEMA_VERSION);
    }

    /** Schema-aware hash used only while migrating durable strength commands. */
    static String canonicalPayloadHash(
        DominionCommand command,
        int strengthPolicySchema
    ) {
        Objects.requireNonNull(command, "command");
        if (command instanceof CommitJudgmentBatch batch) {
            return batch.batch().canonicalPayloadHash();
        }
        return sha256(canonicalPayload(command, strengthPolicySchema));
    }

    /** Legacy strength hashes are accepted only on decoded legacy payloads. */
    static boolean matchesCanonicalPayloadHash(DominionCommand command, String candidate) {
        Objects.requireNonNull(command, "command");
        String supplied = Objects.toString(candidate, "").trim();
        if (supplied.isEmpty()) {
            return false;
        }
        if (canonicalPayloadHash(command).equalsIgnoreCase(supplied)) {
            return true;
        }
        if (command instanceof ConfigureStrengthPolicy) return false;
        return supportsLegacyEncoding(command)
            && sha256(legacyCanonicalPayload(command)).equalsIgnoreCase(supplied);
    }

    /** Stable textual encoding. Command identity is deliberately not part of the payload. */
    static String canonicalPayload(DominionCommand command) {
        return canonicalPayload(command, command instanceof ConfigureStrengthPolicy configure
            ? configure.configuration().sourceSchema() : StrengthPolicySnapshot.SCHEMA_VERSION);
    }

    private static String canonicalPayload(
        DominionCommand command,
        int strengthPolicySchema
    ) {
        Objects.requireNonNull(command, "command");
        StringBuilder canonical = new StringBuilder();
        if (command instanceof TransitionSubjects transitions) {
            appendField(canonical, "transition-subjects");
            appendField(canonical, transitions.batch().canonicalKey());
        } else if (command instanceof BindOwner bind) {
            appendField(canonical, "bind-owner");
            appendField(canonical, bind.owner());
        } else if (command instanceof UnbindOwner unbind) {
            appendField(canonical, "unbind-owner");
            appendField(canonical, unbind.owner());
        } else if (command instanceof ClearTombstones) {
            appendField(canonical, "clear-tombstones");
        } else if (command instanceof ClearTombstone clear) {
            appendField(canonical, "clear-tombstone");
            appendField(canonical, clear.subject());
        } else if (command instanceof CommitJudgment judgment) {
            appendField(canonical, "commit-judgment");
            appendField(canonical, canonicalJudgment(judgment.decision()));
        } else if (command instanceof CommitJudgmentBatch batch) {
            appendField(canonical, "commit-judgment-batch");
            appendField(canonical, batch.batch().canonicalPayloadHash());
        } else if (command instanceof ConfigureStrengthPolicy configure) {
            appendField(canonical, "configure-strength-policy");
            appendField(
                canonical,
                configure.configuration().canonicalKeyForSchema(strengthPolicySchema)
            );
        } else {
            throw new IllegalArgumentException("unsupported command: " + command);
        }
        return canonical.toString();
    }

    /** Full canonical Judgment encoding shared by action ordering and durable hashing. */
    static String canonicalJudgment(DominionDecision.Judgment judgment) {
        Objects.requireNonNull(judgment, "judgment");
        SubjectIdentity subject = judgment.subject();
        StringBuilder canonical = new StringBuilder();
        appendField(canonical, subject.uuid());
        appendField(canonical, subject.registryType());
        appendField(canonical, subject.runtimeClass());
        appendField(canonical, subject.dimension());
        appendField(canonical, Double.toHexString(subject.x()));
        appendField(canonical, Double.toHexString(subject.y()));
        appendField(canonical, Double.toHexString(subject.z()));
        appendField(canonical, subject.player());
        appendField(canonical, judgment.epoch().generation());
        appendField(canonical, judgment.epoch().sequence());
        appendField(canonical, judgment.committed());
        appendField(canonical, judgment.scope());
        appendField(canonical, Double.toHexString(judgment.radius()));
        appendField(canonical, judgment.reason());
        return canonical.toString();
    }

    private static void appendField(StringBuilder target, Object value) {
        String encoded = Objects.toString(value, "");
        target.append(encoded.length()).append(':').append(encoded).append(';');
    }

    private static String legacyCanonicalPayload(DominionCommand command) {
        if (command instanceof BindOwner bind) {
            return "bind-owner|" + bind.owner();
        }
        if (command instanceof UnbindOwner unbind) {
            return "unbind-owner|" + unbind.owner();
        }
        if (command instanceof ClearTombstones) {
            return "clear-tombstones";
        }
        if (command instanceof ClearTombstone clear) {
            return "clear-tombstone|" + clear.subject();
        }
        if (command instanceof CommitJudgment judgment) {
            return "commit-judgment|" + judgment.decision().subject().canonicalKey()
                + "|" + judgment.decision().epoch().generation()
                + ":" + judgment.decision().epoch().sequence();
        }
        throw new IllegalArgumentException("unsupported legacy command: " + command);
    }

    private static boolean supportsLegacyEncoding(DominionCommand command) {
        return command instanceof BindOwner
            || command instanceof UnbindOwner
            || command instanceof ClearTombstones
            || command instanceof ClearTombstone
            || command instanceof CommitJudgment;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
