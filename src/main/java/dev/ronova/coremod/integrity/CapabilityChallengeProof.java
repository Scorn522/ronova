package dev.ronova.coremod.integrity;

/**
 * Bounded behavior-challenge evidence for a selected capability scope.
 * This is evidence only: it is not a Root lease, execution permit, or durable-effect receipt.
 */
public record CapabilityChallengeProof(
    boolean supported,
    boolean current,
    long generation,
    long attempts,
    long successes,
    long failures
) {
    public static final CapabilityChallengeProof UNAVAILABLE =
        new CapabilityChallengeProof(false, false, 0L, 0L, 0L, 0L);

    public CapabilityChallengeProof {
        if (generation < 0L || attempts < 0L || successes < 0L || failures < 0L
            || successes > attempts || failures > attempts) {
            throw new IllegalArgumentException("invalid capability challenge evidence");
        }
        current = supported && current && successes > 0L;
    }
}
