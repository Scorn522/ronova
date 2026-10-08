package dev.ronova.coremod.integrity;

/** Low-frequency boundary implemented by the Native integrity adapter. */
@FunctionalInterface
public interface EnforcementProofSource {
    EnforcementProof read();
    default CodeIntegrityProof codeIntegrity(){return CodeIntegrityProof.UNAVAILABLE;}
    default CapabilityChallengeProof challengeProof(){return CapabilityChallengeProof.UNAVAILABLE;}
}
