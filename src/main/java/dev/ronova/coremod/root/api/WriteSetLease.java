package dev.ronova.coremod.root.api;

import dev.ronova.coremod.policy.PolicyRevision;
import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * Ephemeral narrowing of an already-valid Root write permission.
 *
 * <p>This object is never an authority source. A sink is usable only when the supplied upstream
 * permission remains current <em>and</em> the sink was declared in this lease. Consuming a sink
 * records bounded evidence so the caller can compare the declared and actually visited final
 * write boundaries before it reports success.</p>
 */
public final class WriteSetLease implements AutoCloseable {
    public record Assessment(Set<WriteSinkId> declared, Set<WriteSinkId> consumed,
        Set<WriteSinkId> rejected, boolean upstreamCurrent, boolean closed) {
        public Assessment {
            declared=Set.copyOf(declared);consumed=Set.copyOf(consumed);rejected=Set.copyOf(rejected);
        }
        public Set<WriteSinkId> missing() {
            var missing=new LinkedHashSet<>(declared);missing.removeAll(consumed);return Set.copyOf(missing);
        }
        public boolean exact() {
            return upstreamCurrent&&!closed&&rejected.isEmpty()&&missing().isEmpty()&&declared.containsAll(consumed);
        }
        public String summary() {
            return "declared="+declared.size()+";consumed="+consumed.size()+";rejected="+rejected.size()
                +";missing="+missing().size()+";upstream="+upstreamCurrent;
        }
    }

    private final UUID operationId;
    private final TargetEpoch targetEpoch;
    private final PolicyRevision policyRevision;
    private final BooleanSupplier upstream;
    private final Set<WriteSinkId> declared;
    private final LinkedHashMap<WriteSinkId,WriteSinkEvidence> evidence=new LinkedHashMap<>();
    private final LinkedHashSet<WriteSinkId> rejected=new LinkedHashSet<>();
    private boolean closed;

    public WriteSetLease(UUID operationId,TargetEpoch targetEpoch,PolicyRevision policyRevision,
        BooleanSupplier upstream,Collection<WriteSinkId> declared) {
        this.operationId=Objects.requireNonNull(operationId,"operationId");
        this.targetEpoch=Objects.requireNonNull(targetEpoch,"targetEpoch");
        this.policyRevision=Objects.requireNonNull(policyRevision,"policyRevision");
        this.upstream=Objects.requireNonNull(upstream,"upstream");
        var copy=new LinkedHashSet<WriteSinkId>();
        for(var sink:Objects.requireNonNull(declared,"declared"))copy.add(Objects.requireNonNull(sink,"sink"));
        if(copy.isEmpty())throw new IllegalArgumentException("write set must declare at least one sink");
        this.declared=Set.copyOf(copy);
    }

    public static WriteSetLease forPlan(TerminalOperationPlan plan,BooleanSupplier upstream,WriteSinkId... sinks) {
        Objects.requireNonNull(plan,"plan");
        return new WriteSetLease(plan.planId(),plan.qualification().targetEpoch(),
            new PolicyRevision(plan.activePolicyRevision()),upstream,List.of(sinks));
    }

    /**
     * Consume one final write boundary. Returning true means the upstream permission was current and
     * this sink was declared. Returning false never widens permission and records bounded rejection
     * evidence for diagnostics.
     */
    public synchronized boolean consume(WriteSinkId sink) {
        Objects.requireNonNull(sink,"sink");
        if(closed)return false;
        boolean current=safeCurrent();
        if(!declared.contains(sink)) {
            rejected.add(sink);
            evidence.putIfAbsent(sink,evidence(sink,WriteSinkEvidence.Outcome.REJECTED_POLICY,"undeclared"));
            return false;
        }
        if(!current) {
            rejected.add(sink);
            evidence.putIfAbsent(sink,evidence(sink,WriteSinkEvidence.Outcome.FENCED_UNVERIFIED,"upstream-revoked"));
            return false;
        }
        evidence.putIfAbsent(sink,evidence(sink,WriteSinkEvidence.Outcome.ACCEPTED,"declared-final-write"));
        return true;
    }

    public synchronized Assessment assessment() {
        var consumed=new LinkedHashSet<WriteSinkId>();
        evidence.forEach((sink,value)->{if(value.outcome()==WriteSinkEvidence.Outcome.ACCEPTED)consumed.add(sink);});
        return new Assessment(declared,consumed,rejected,safeCurrent(),closed);
    }

    public synchronized List<WriteSinkEvidence> evidence() { return List.copyOf(evidence.values()); }
    public synchronized WriteSetReceipt receipt() {
        var assessment=assessment();
        var hashes=evidence.values().stream().map(WriteSinkEvidence::evidenceHash).sorted().toList();
        String hash=EffectRecord.hash("ronova:write-set-receipt/v1\n"+operationId+"\n"+targetEpoch.subjectKey().canonical()
            +"\n"+targetEpoch.generation()+"\n"+policyRevision.value()+"\n"+String.join("\n",hashes));
        return new WriteSetReceipt(operationId,targetEpoch,policyRevision,assessment.declared(),assessment.consumed(),
            assessment.rejected(),assessment.upstreamCurrent()&&!assessment.closed(),hash);
    }
    public synchronized boolean current() { return !closed&&safeCurrent(); }

    private boolean safeCurrent() {
        try{return upstream.getAsBoolean();}catch(RuntimeException|LinkageError unavailable){return false;}
    }
    private WriteSinkEvidence evidence(WriteSinkId sink,WriteSinkEvidence.Outcome outcome,String detail) {
        String hash=EffectRecord.hash("ronova:write-set-evidence/v1\n"+operationId+"\n"+targetEpoch.subjectKey().canonical()
            +"\n"+targetEpoch.generation()+"\n"+policyRevision.value()+"\n"+sink.canonical()+"\n"+outcome.name()+"\n"+detail);
        return new WriteSinkEvidence(operationId,sink,targetEpoch,policyRevision,outcome,hash);
    }

    @Override public synchronized void close(){closed=true;}
}
