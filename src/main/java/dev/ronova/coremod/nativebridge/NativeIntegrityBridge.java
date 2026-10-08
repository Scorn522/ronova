package dev.ronova.coremod.nativebridge;

import dev.ronova.coremod.integrity.EnforcementProof;
import dev.ronova.coremod.integrity.EnforcementProofSource;

/** JNI adapter for the process-wide semantic enforcement lease. */
public final class NativeIntegrityBridge implements EnforcementProofSource {
    public static final NativeIntegrityBridge INSTANCE = new NativeIntegrityBridge();

    private NativeIntegrityBridge() {}
    @Override public dev.ronova.coremod.integrity.CodeIntegrityProof codeIntegrity() {
        var state=NativeBridge.codeSeals();return new dev.ronova.coremod.integrity.CodeIntegrityProof(
            state.supported(),state.registered(),state.loaded(),state.verified(),state.replacements());
    }

    @Override public dev.ronova.coremod.integrity.CapabilityChallengeProof challengeProof() {
        var state=NativeBridge.codeSealChallenge();
        return new dev.ronova.coremod.integrity.CapabilityChallengeProof(
            state.supported(),state.current(),state.generation(),state.attempts(),state.successes(),state.failures());
    }

    @Override
    public EnforcementProof read() {
        if (!NativeBridge.canAttestEnforcement()) return EnforcementProof.UNAVAILABLE;
        try {
            long[] values = state0();
            if (values == null || values.length < 11) return EnforcementProof.UNAVAILABLE;
            long flags = values[0];
            return new EnforcementProof(
                true,
                (flags & 1L) != 0L,
                (flags & 2L) != 0L,
                mask(values[1]),
                mask(values[2]),
                values[3], values[4], values[5], values[6], values[7], values[8],
                values[9], values[10]
            );
        } catch (Throwable ignored) {
            return EnforcementProof.UNAVAILABLE;
        }
    }

    private static int mask(long value) {
        return value < 0L || value > Integer.MAX_VALUE ? 0 : (int) value;
    }

    private static native long[] state0();
}
