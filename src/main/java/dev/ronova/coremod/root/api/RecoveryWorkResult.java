package dev.ronova.coremod.root.api;

import java.util.Objects;

/** Monotone facts about one work intent. Recording a fact does not close the recovery chain. */
public record RecoveryWorkResult(EffectRecord record,RecoveryWorkDescriptor descriptor,long facts) {
    public enum Fact {
        CANCEL_REQUESTED(1),CANCEL_ACCEPTED(2),CANCEL_REJECTED(4),EXECUTION_EXITED(8),SUCCESSORS_CLOSED(16),
        MEMORY_CHANGED(32),SERIALIZED(64),FILE_COMMITTED(128),READBACK_VERIFIED(256),PRESERVATION_VERIFIED(512),
        RESOURCES_RELEASED(1024),NOT_CURRENT(2048),NOT_EXECUTED(4096),EFFECT_UNKNOWN(8192);
        public final long bit;Fact(long bit){this.bit=bit;}
    }
    public RecoveryWorkResult {
        Objects.requireNonNull(record);Objects.requireNonNull(descriptor);
        if(!descriptor.matches(record.key())||facts!=facts(record))throw new IllegalArgumentException("work result identity/facts mismatch");
        descriptor.validateFacts(record);
    }
    public boolean has(Fact fact){return (facts&fact.bit)!=0;}
    public static long facts(EffectRecord record) {
        if(!RecoveryWorkDescriptor.OPERATION.equals(record.key().semanticOperationId()))throw new IllegalArgumentException("not a recovery work result");
        String evidence=record.evidenceRef();
        if(record.state()==EffectRecord.State.PREPARED&&evidence.equals("recovery-work-intent"))return 0;
        if(record.state()==EffectRecord.State.EFFECT_UNKNOWN) {
            if(evidence.equals("restart-prepared-effect-unknown")||evidence.equals("prepared-without-live-attempt"))return Fact.EFFECT_UNKNOWN.bit;
            if(evidence.matches("work-facts/[1-9][0-9]{0,4}")) {
                long facts=Long.parseLong(evidence.substring(11));if(facts<16384)return facts;
            }
        }
        throw new IllegalArgumentException("invalid recovery work evidence/state");
    }
    public static String observe(EffectRecord record,Fact fact){return "work-facts/"+(facts(record)|Objects.requireNonNull(fact).bit);}
}
