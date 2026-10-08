package dev.ronova.coremod.integrity;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

/** Publishes a bounded-rate immutable proof projection for one server runtime. */
public final class EnforcementRuntime {
    public static final int DEFAULT_POLL_TICKS = 20;
    public static final long OBSERVATION_WINDOW_NANOS = 250_000_000L;

    private final EnforcementProofSource source;
    private final int pollTicks;
    private final LongSupplier observationClock;
    private final AtomicReference<EnforcementProof> published =
        new AtomicReference<>(EnforcementProof.UNAVAILABLE);
    private int lastPollTick;
    private boolean initialized;
    private long polls;
    private long readFailures;
    private long challengeReadFailures;
    private volatile CodeIntegrityProof codeIntegrity=CodeIntegrityProof.UNAVAILABLE;
    private volatile CapabilityChallengeProof challengeProof=CapabilityChallengeProof.UNAVAILABLE;

    public EnforcementRuntime(EnforcementProofSource source) {
        this(source, DEFAULT_POLL_TICKS);
    }

    EnforcementRuntime(EnforcementProofSource source, int pollTicks) {
        this(source, pollTicks, System::nanoTime);
    }

    EnforcementRuntime(EnforcementProofSource source, int pollTicks, LongSupplier observationClock) {
        this.source = Objects.requireNonNull(source, "source");
        this.observationClock = Objects.requireNonNull(observationClock, "observationClock");
        if (pollTicks <= 0) throw new IllegalArgumentException("pollTicks must be positive");
        this.pollTicks = pollTicks;
    }

    public void pulse(int serverTick) {
        if (initialized && Integer.toUnsignedLong(serverTick - lastPollTick) < pollTicks) {
            return;
        }
        initialized = true;
        lastPollTick = serverTick;
        polls++;
        try {
            EnforcementProof next = source.read();
            published.set(next == null ? EnforcementProof.UNAVAILABLE : next);
            CodeIntegrityProof code=source.codeIntegrity();codeIntegrity=code==null?CodeIntegrityProof.UNAVAILABLE:code;
            if (next == null) readFailures++;
        } catch (Throwable ignored) {
            readFailures++;
            published.set(EnforcementProof.UNAVAILABLE);
            codeIntegrity=CodeIntegrityProof.UNAVAILABLE;
        }
        try {
            CapabilityChallengeProof challenge=source.challengeProof();
            challengeProof=challenge==null?CapabilityChallengeProof.UNAVAILABLE:challenge;
            if(challenge==null)challengeReadFailures++;
        } catch(Throwable ignored) {
            challengeReadFailures++;
            challengeProof=CapabilityChallengeProof.UNAVAILABLE;
        }
    }

    public EnforcementProof published() { return published.get(); }
    public CodeIntegrityProof codeIntegrity(){return codeIntegrity;}
    public CapabilityChallengeProof challengeProof(){return challengeProof;}
    /** A fresh observation proof, not a write lease or a promise about later execution. */
    public boolean observesRegisteredSemantics(int requiredMask) {
        return capture(requiredMask) != null;
    }

    /**
     * Start one bounded observation of registered execution. Use stillCurrent before writes,
     * then finish after raw read-back and before publishing the observation result.
     * This does not reserve Native execution, grant Root authority, or stop a transformer
     * between the check and a write; mutations performed before invalidation are not rolled back.
     */
    public Observation beginObservation(int requiredMask) {
        long issued = observationClock.getAsLong();
        Evidence evidence = capture(requiredMask);
        long elapsed = observationClock.getAsLong() - issued;
        return evidence == null || elapsed < 0 || elapsed >= OBSERVATION_WINDOW_NANOS
            ? null : new Observation(requiredMask, evidence, issued);
    }

    /** No public constructor: an observation is bound to its issuing runtime and current thread. */
    public final class Observation implements AutoCloseable {
        public enum State { ACTIVE, CONFIRMED, EXPIRED, WRONG_THREAD, PROOF_UNAVAILABLE, PROOF_CHANGED, CLOSED }
        private final int requiredMask;
        private final Evidence initial;
        private final long issued;
        private final Thread owner = Thread.currentThread();
        private final AtomicReference<State> state = new AtomicReference<>(State.ACTIVE);

        private Observation(int requiredMask, Evidence initial, long issued) {
            this.requiredMask = requiredMask; this.initial = initial; this.issued = issued;
        }
        public State state() { return state.get(); }
        public boolean stillCurrent() {
            if (state.get() != State.ACTIVE) return false;
            if (Thread.currentThread() != owner) return invalidate(State.WRONG_THREAD);
            if (expired()) return invalidate(State.EXPIRED);
            Evidence current = capture(requiredMask);
            if (current == null) return invalidate(State.PROOF_UNAVAILABLE);
            if (!initial.equals(current)) return invalidate(State.PROOF_CHANGED);
            if (expired()) return invalidate(State.EXPIRED);
            return state.get() == State.ACTIVE;
        }
        /** Single-use evidence receipt only, never a terminal-completion or durable-effect receipt. */
        public boolean finish() {
            return stillCurrent() && state.compareAndSet(State.ACTIVE, State.CONFIRMED);
        }
        private boolean expired() {
            long elapsed = observationClock.getAsLong() - issued;
            return elapsed < 0 || elapsed >= OBSERVATION_WINDOW_NANOS;
        }
        private boolean invalidate(State reason) {
            state.compareAndSet(State.ACTIVE, reason); return false;
        }
        @Override public void close() { state.compareAndSet(State.ACTIVE, State.CLOSED); }
    }

    // Age advances normally. All counters, masks and code-image inventory must remain identical.
    private record SemanticStamp(boolean watchdogArmed, int required, int verified,
        long generation, long successes, long failures, long transformations,
        long semanticFailures, long reassertions, long consecutiveFailures) {
        static SemanticStamp of(EnforcementProof proof) {
            return new SemanticStamp(proof.watchdogArmed(), proof.requiredMask(), proof.verifiedMask(),
                proof.generation(), proof.successfulLeases(), proof.failedLeases(), proof.transformations(),
                proof.semanticFailures(), proof.reassertions(), proof.consecutiveFailures());
        }
    }
    private record Evidence(SemanticStamp semantic, CodeIntegrityProof code) {}
    private Evidence capture(int requiredMask) {
        if (requiredMask <= 0) return null;
        try {
            var before = source.read(); var codeBefore = source.codeIntegrity();
            var codeAfter = source.codeIntegrity(); var after = source.read();
            if (before == null || after == null || codeBefore == null || codeAfter == null
                || !codeBefore.current() || !codeAfter.current() || !codeBefore.equals(codeAfter)
                || !before.current() || !after.current()
                || (after.requiredMask() & requiredMask) != requiredMask
                || (after.verifiedMask() & requiredMask) != requiredMask) return null;
            SemanticStamp stamp = SemanticStamp.of(after);
            return stamp.equals(SemanticStamp.of(before)) ? new Evidence(stamp, codeAfter) : null;
        } catch (RuntimeException | LinkageError unavailable) { return null; }
    }

    public long polls() { return polls; }
    public long challengeReadFailures(){return challengeReadFailures;}

    public long readFailures() { return readFailures; }
}
