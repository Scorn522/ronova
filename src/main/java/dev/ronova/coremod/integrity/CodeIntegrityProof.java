package dev.ronova.coremod.integrity;

/** Evidence for registered class images only; it does not attest arbitrary memory or JIT targets. */
public record CodeIntegrityProof(boolean supported,long registered,long loaded,long verified,long replacements) {
    public static final CodeIntegrityProof UNAVAILABLE=new CodeIntegrityProof(false,0,0,0,0);
    public CodeIntegrityProof {if(registered<0||loaded<0||verified<0||verified>loaded||loaded>registered||replacements<0)throw new IllegalArgumentException("invalid code evidence");}
    public boolean current(){return supported&&loaded>0&&loaded==verified;}
}
