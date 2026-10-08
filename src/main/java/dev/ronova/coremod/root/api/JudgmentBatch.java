package dev.ronova.coremod.root.api;

import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.SubjectIdentity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.UUID;

/**
 * Canonical action-level collection of judgments. One batch has one command id,
 * one decision epoch and one logical durable mutation.
 */
public record JudgmentBatch(
    UUID commandId,
    String commandDomain,
    List<DominionDecision.Judgment> judgments,
    String canonicalPayloadHash,
    QualificationPreconditions preconditions,
    java.util.Map<UUID, UUID> incarnationNonces
) {
    private static final Comparator<DominionDecision.Judgment> ORDER =
        Comparator.comparing(DominionCommand::canonicalJudgment);
    private static final Comparator<DominionDecision.Judgment> LEGACY_ORDER =
        Comparator.comparing((DominionDecision.Judgment value) -> value.subject().canonicalKey())
            .thenComparing(value -> value.scope().name())
            .thenComparing(value -> Double.toString(value.radius()))
            .thenComparing(value -> value.reason());

    public JudgmentBatch {
        incarnationNonces = java.util.Collections.unmodifiableMap(new java.util.TreeMap<>(
            Objects.requireNonNull(incarnationNonces, "incarnationNonces")));
        commandId = Objects.requireNonNull(commandId, "commandId");
        commandDomain = normalizeDomain(commandDomain);
        List<DominionDecision.Judgment> copy = new ArrayList<>();
        if (judgments != null) {
            for (DominionDecision.Judgment judgment : judgments) {
                copy.add(Objects.requireNonNull(judgment, "judgment"));
            }
        }
        copy.sort(ORDER);
        LinkedHashSet<SubjectIdentity> subjects = new LinkedHashSet<>();
        Set<UUID> subjectIds = new HashSet<>();
        DominionEpoch batchEpoch = null;
        for (DominionDecision.Judgment judgment : copy) {
            if (!judgment.committed()) {
                throw new IllegalArgumentException("batch contains an uncommitted judgment");
            }
            if (!subjects.add(judgment.subject())) {
                throw new IllegalArgumentException(
                    "batch contains duplicate subject: " + judgment.subject().canonicalKey()
                );
            }
            UUID subjectId = judgment.subject().uuid();
            if (subjectId == null || !subjectIds.add(subjectId)) {
                throw new IllegalArgumentException(
                    "batch contains duplicate or missing subject UUID"
                );
            }
            if (batchEpoch == null) {
                batchEpoch = judgment.epoch();
            } else if (!batchEpoch.equals(judgment.epoch())) {
                throw new IllegalArgumentException(
                    "batch judgments must share one decision epoch"
                );
            }
        }
        judgments = List.copyOf(copy);
        if (preconditions != null) {
            Set<UUID> expectedSubjects = new HashSet<>();
            for (SubjectQualification expected : preconditions.subjects()) {
                expectedSubjects.add(expected.subjectKey().subjectId());
            }
            if (!subjectIds.equals(expectedSubjects)) {
                throw new IllegalArgumentException("qualification preconditions must cover the exact batch");
            }
        }
        if (!incarnationNonces.isEmpty() && (preconditions == null || !subjectIds.equals(incarnationNonces.keySet())
            || incarnationNonces.values().stream().anyMatch(Objects::isNull))) {
            throw new IllegalArgumentException("physical bindings require the exact qualified batch");
        }
        String computed = hash(commandDomain, judgments, preconditions, incarnationNonces);
        String supplied = Objects.toString(canonicalPayloadHash, "").trim();
        if (!supplied.isEmpty()
            && !computed.equalsIgnoreCase(supplied)
            && !(preconditions == null && commandDomain.isEmpty()
                && legacyHash(judgments).equalsIgnoreCase(supplied))) {
            throw new IllegalArgumentException("judgment batch canonical hash mismatch");
        }
        canonicalPayloadHash = computed;
    }

    public JudgmentBatch(UUID commandId, String commandDomain, List<DominionDecision.Judgment> judgments,
        String canonicalPayloadHash, QualificationPreconditions preconditions) {
        this(commandId, commandDomain, judgments, canonicalPayloadHash, preconditions, java.util.Map.of());
    }

    public JudgmentBatch withIncarnations(java.util.Map<UUID, UUID> nonces) {
        if (!incarnationNonces.isEmpty() && !incarnationNonces.equals(nonces)) {
            throw new IllegalStateException("cannot replace physical objects in an existing request");
        }
        return new JudgmentBatch(commandId, commandDomain, judgments, null, preconditions, nonces);
    }

    /** Historical batches keep their exact canonical hash and have no added expectations. */
    public JudgmentBatch(UUID commandId, String commandDomain,
        List<DominionDecision.Judgment> judgments, String canonicalPayloadHash) {
        this(commandId, commandDomain, judgments, canonicalPayloadHash, null);
    }

    public JudgmentBatch withPreconditions(QualificationPreconditions expected) {
        Objects.requireNonNull(expected, "expected");
        if (preconditions != null && !preconditions.equals(expected)) {
            throw new IllegalStateException("cannot renew qualification under the same command identity");
        }
        return new JudgmentBatch(commandId, commandDomain, judgments, null, expected, incarnationNonces);
    }

    public JudgmentBatch(UUID commandId, List<DominionDecision.Judgment> judgments) {
        this(commandId, "", judgments, null);
    }

    /** Compatibility constructor for the format-1 journal shape. */
    public JudgmentBatch(
        UUID commandId,
        List<DominionDecision.Judgment> judgments,
        String canonicalPayloadHash
    ) {
        this(commandId, "", judgments, canonicalPayloadHash);
    }

    public JudgmentBatch(
        UUID commandId,
        String commandDomain,
        List<DominionDecision.Judgment> judgments
    ) {
        this(commandId, commandDomain, judgments, null);
    }

    public static JudgmentBatch of(UUID commandId, DominionDecision.Judgment judgment) {
        return new JudgmentBatch(commandId, List.of(Objects.requireNonNull(judgment, "judgment")));
    }

    public static JudgmentBatch empty(UUID commandId) {
        return new JudgmentBatch(commandId, List.of());
    }

    public static JudgmentBatch empty(UUID commandId, String commandDomain) {
        return new JudgmentBatch(commandId, commandDomain, List.of());
    }

    public boolean empty() {
        return judgments.isEmpty();
    }

    public int size() {
        return judgments.size();
    }

    /** The decision epoch requested by all entries, or null for an empty batch. */
    public DominionEpoch epoch() {
        return judgments.isEmpty() ? null : judgments.get(0).epoch();
    }

    public List<SubjectIdentity> subjects() {
        return judgments.stream().map(DominionDecision.Judgment::subject).toList();
    }

    public String canonicalKey() {
        return commandId + ":" + canonicalPayloadHash;
    }

    private static String hash(
        String commandDomain,
        List<DominionDecision.Judgment> values,
        QualificationPreconditions preconditions,
        java.util.Map<UUID, UUID> incarnations
    ) {
        StringBuilder canonical = new StringBuilder();
        if (!commandDomain.isEmpty()) {
            canonical.append("domain:").append(commandDomain.length()).append(':')
                .append(commandDomain).append(';');
        }
        for (DominionDecision.Judgment judgment : values) {
            String encoded = DominionCommand.canonicalJudgment(judgment);
            canonical.append(encoded.length()).append(':').append(encoded).append(';');
        }
        if (preconditions != null) {
            String encoded = preconditions.canonicalKey();
            canonical.append("expected:").append(encoded.length()).append(':').append(encoded).append(';');
        }
        if (!incarnations.isEmpty()) {
            canonical.append("physical-incarnations-v1:");
            incarnations.forEach((subject, nonce) -> canonical.append(subject).append(':').append(nonce).append(';'));
        }
        return sha256(canonical.toString());
    }

    private static String legacyHash(List<DominionDecision.Judgment> values) {
        List<DominionDecision.Judgment> legacy = new ArrayList<>(values);
        legacy.sort(LEGACY_ORDER);
        StringBuilder canonical = new StringBuilder();
        for (DominionDecision.Judgment judgment : legacy) {
            SubjectIdentity subject = judgment.subject();
            canonical.append(subject.canonicalKey()).append('|')
                .append(judgment.epoch().generation()).append(':')
                .append(judgment.epoch().sequence()).append('|')
                .append(judgment.committed()).append('|')
                .append(judgment.scope()).append('|')
                .append(Double.toString(judgment.radius())).append('|')
                .append(judgment.reason()).append('\n');
        }
        return sha256(canonical.toString());
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String normalizeDomain(String value) {
        String normalized = Objects.toString(value, "").trim();
        if (normalized.length() > 64) {
            throw new IllegalArgumentException("judgment command domain is too long");
        }
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (!(current >= 'a' && current <= 'z')
                && !(current >= '0' && current <= '9')
                && current != '.' && current != '_' && current != '-') {
                throw new IllegalArgumentException(
                    "judgment command domain is not canonical"
                );
            }
        }
        return normalized;
    }
}
