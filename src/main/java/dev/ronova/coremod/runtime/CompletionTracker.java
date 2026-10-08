package dev.ronova.coremod.runtime;

import dev.ronova.coremod.root.api.SubjectQualification;
import dev.ronova.coremod.root.api.TerminalOperationPlan;
import java.util.*;

/** Current evidence for the frozen server-result contract. Never changes a qualification or resolves an effect. */
public final class CompletionTracker {
    public static final long STABLE_TICKS = 20, MAX_SAMPLE_GAP = 10;
    public enum Condition {
        BODY_DISABLED, WORLD_RECORDS, OLD_EPOCH_REJECTED, RECOVERY_TASKS,
        REBUILD_ENTRYPOINTS, SAVE_QUALIFICATION, TRACKING, SERVER_PRESENCE, NO_PENDING_WORK
    }
    public enum State { MISSING, VERIFIED, FAILED }
    public record Scope(UUID commandId, String authorityHash, SubjectQualification qualification,
                        long activePolicy, long configuredPolicy, String contract) {
        public Scope(UUID commandId,String authorityHash,SubjectQualification qualification,long activePolicy,long configuredPolicy) {
            this(commandId,authorityHash,qualification,activePolicy,configuredPolicy,"ronova:terminal-server/v4");
        }
        public Scope {
            Objects.requireNonNull(commandId); Objects.requireNonNull(authorityHash); Objects.requireNonNull(qualification);
            Objects.requireNonNull(contract);
            if(contract.isBlank()||contract.length()>128)throw new IllegalArgumentException("invalid completion contract");
            if (!qualification.terminal() || activePolicy < 1 || configuredPolicy < 1)
                throw new IllegalArgumentException("invalid completion scope");
        }
        public static Scope of(TerminalOperationPlan plan) {
            return new Scope(plan.commandId(), plan.authorityHash(), plan.qualification(),
                plan.activePolicyRevision(), plan.configuredPolicyRevision(),plan.completionContract());
        }
    }
    public record Evidence(State state, long dependencyRevision, String reference) {
        public Evidence {
            Objects.requireNonNull(state); Objects.requireNonNull(reference);
            if (dependencyRevision < 0 || reference.isBlank() || reference.length() > 128
                || state == State.VERIFIED && dependencyRevision == 0)
                throw new IllegalArgumentException("invalid completion evidence");
        }
    }
    public record Evaluation(Scope scope, long revision, boolean complete, boolean fenced,
                             Set<Condition> missing, long stableTicks, boolean unresolvedEffect) {
        public Evaluation { missing = Set.copyOf(missing); }
    }
    private Scope scope;
    private Map<Condition, Evidence> previous = Map.of();
    private long sampledAt = -1, stableSince = -1, revision;
    private Evaluation current, lastSatisfied;

    public synchronized Evaluation sample(Scope next, long now, Map<Condition, Evidence> evidence,
                                          boolean authorityCurrent, boolean effectsResolved) {
        Objects.requireNonNull(next); Objects.requireNonNull(evidence);
        if (now < 0) throw new IllegalArgumentException("negative observation time");
        EnumMap<Condition, Evidence> snapshot = new EnumMap<>(Condition.class);
        evidence.forEach((key, value) -> snapshot.put(Objects.requireNonNull(key), Objects.requireNonNull(value)));
        EnumSet<Condition> missing = EnumSet.allOf(Condition.class);
        snapshot.forEach((condition, proof) -> { if (proof.state() == State.VERIFIED) missing.remove(condition); });
        boolean fenced = !authorityCurrent || snapshot.values().stream().anyMatch(e -> e.state() == State.FAILED);
        boolean changed = !next.equals(scope) || !snapshot.equals(previous);
        boolean gap = sampledAt < 0 || now < sampledAt || now - sampledAt > MAX_SAMPLE_GAP;
        boolean eligible = !fenced && missing.isEmpty() && effectsResolved;
        if (!eligible) stableSince = -1;
        else if (changed || gap || stableSince < 0) stableSince = now;
        long stable = stableSince < 0 ? 0 : now - stableSince;
        boolean complete = eligible && stable >= STABLE_TICKS;
        if (current == null || changed || gap || current.complete() != complete || current.fenced() != fenced
            || current.unresolvedEffect() == effectsResolved || !current.missing().equals(missing)) revision = Math.addExact(revision, 1);
        scope = next; previous = Map.copyOf(snapshot); sampledAt = now;
        current = new Evaluation(scope, revision, complete, fenced, missing, stable, !effectsResolved);
        if (complete) lastSatisfied = current;
        return current;
    }

    /** A silent observer cannot leave a previously valid certificate displayed forever. */
    public synchronized Evaluation current(long now) {
        if (current == null) return null;
        if (now < sampledAt || now - sampledAt > MAX_SAMPLE_GAP) {
            stableSince = -1;
            if (!current.fenced() || current.complete() || !current.missing().equals(Set.of(Condition.values()))) {
                revision = Math.addExact(revision, 1);
                current = new Evaluation(scope, revision, false, true, Set.of(Condition.values()), 0, current.unresolvedEffect());
            }
            // Invalidate the dependency snapshot too; renewed observations start a fresh window.
            previous = Map.of();
        }
        return current;
    }
    public synchronized Evaluation lastSatisfied() { return lastSatisfied; }
}
