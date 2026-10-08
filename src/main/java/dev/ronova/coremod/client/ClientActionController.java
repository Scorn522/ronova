package dev.ronova.coremod.client;

import dev.ronova.coremod.network.RonovaAction;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Reliable client action outbox and feedback state, independent of input and recovery. */
public final class ClientActionController {
    private static final long RETRY_NANOS = 300_000_000L;
    private static final long RETRY_MAX_NANOS = 3_000_000_000L;

    private final Deque<PendingAction> outbox = new ArrayDeque<>();
    private final ActionSender sender;
    private UUID realmId;
    private UUID playerId;
    private boolean suspended = true;
    private long sessionId = newSessionId();
    private long nextSequence = 1L;
    private long lastFeedbackGameTime = Long.MIN_VALUE;
    private int feedbackTicks;
    private int lastCount;
    private RonovaAction lastAction = RonovaAction.PRIMARY;

    public ClientActionController(ActionSender sender) {
        this.sender = sender;
    }

    public synchronized void pulse(long gameTime) {
        if (lastFeedbackGameTime != gameTime && feedbackTicks > 0) {
            feedbackTicks--;
        }
        lastFeedbackGameTime = gameTime;
        flush();
    }

    public synchronized void queue(RonovaAction action) {
        if (action == null) {
            return;
        }
        outbox.addLast(new PendingAction(sessionId, nextSequence++, action));
        flush();
    }

    /**
     * Accepts the server's durable Realm identity before sending queued work.
     * A reconnect to the same Realm resumes the original session/sequence; a
     * different Realm discards the old world's pending input before it can be
     * replayed as a new command.
     */
    public synchronized void acceptContext(
        UUID acceptedRealm,
        UUID acceptedPlayer
    ) {
        UUID nextRealm = Objects.requireNonNull(acceptedRealm, "acceptedRealm");
        UUID nextPlayer = Objects.requireNonNull(acceptedPlayer, "acceptedPlayer");
        if (realmId != null && (!realmId.equals(nextRealm)
            || playerId == null || !playerId.equals(nextPlayer))) {
            replaceSession();
        }
        realmId = nextRealm;
        playerId = nextPlayer;
        suspended = false;
        flush();
    }

    /** Retains only an unacknowledged action across a transient disconnect. */
    public synchronized void suspend() {
        suspended = true;
        if (outbox.isEmpty()) {
            replaceSession();
            realmId = null;
            playerId = null;
        }
    }

    public synchronized void acceptFeedback(
        long acceptedSessionId,
        long sequence,
        RonovaAction action,
        int count
    ) {
        if (acceptedSessionId != 0L && acceptedSessionId != sessionId) {
            return;
        }
        PendingAction head = outbox.peekFirst();
        if (head != null && acceptedSessionId == head.sessionId
            && sequence == head.sequence) {
            outbox.removeFirst();
            flush();
        }
        lastAction = action;
        lastCount = Math.max(0, count);
        feedbackTicks = 32;
    }

    public synchronized void flush() {
        PendingAction head = outbox.peekFirst();
        if (head == null || suspended || realmId == null || playerId == null) {
            return;
        }
        long now = System.nanoTime();
        if (head.lastSendNanos != 0L && now - head.lastSendNanos < head.retryDelayNanos()) {
            return;
        }
        head.lastSendNanos = now;
        head.sendAttempts++;
        try {
            sender.send(realmId, head.sessionId, head.sequence, head.action);
        } catch (RuntimeException ignored) {
            // The same action remains queued until the connection becomes writable.
        }
    }

    public synchronized void reset() {
        replaceSession();
        realmId = null;
        playerId = null;
        suspended = true;
        lastFeedbackGameTime = Long.MIN_VALUE;
    }

    public synchronized int feedbackTicks() {
        return feedbackTicks;
    }

    public synchronized int lastCount() {
        return lastCount;
    }

    public synchronized RonovaAction lastAction() {
        return lastAction;
    }

    private static long newSessionId() {
        return ThreadLocalRandom.current().nextLong(1L, Long.MAX_VALUE);
    }

    private void replaceSession() {
        outbox.clear();
        sessionId = newSessionId();
        nextSequence = 1L;
    }

    @FunctionalInterface
    public interface ActionSender {
        void send(UUID realmId, long sessionId, long sequence, RonovaAction action);
    }

    private static final class PendingAction {
        private final long sessionId;
        private final long sequence;
        private final RonovaAction action;
        private long lastSendNanos;
        private int sendAttempts;

        private PendingAction(long sessionId, long sequence, RonovaAction action) {
            this.sessionId = sessionId;
            this.sequence = sequence;
            this.action = action;
        }

        private long retryDelayNanos() {
            int shift = Math.min(4, Math.max(0, sendAttempts - 1));
            return Math.min(RETRY_MAX_NANOS, RETRY_NANOS << shift);
        }
    }
}
