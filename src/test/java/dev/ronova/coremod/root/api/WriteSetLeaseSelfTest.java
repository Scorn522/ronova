package dev.ronova.coremod.root.api;

import dev.ronova.coremod.contract.RealmId;
import dev.ronova.coremod.contract.SubjectKey;
import dev.ronova.coremod.policy.PolicyRevision;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WriteSetLeaseSelfTest {
    public static void main(String[] args) {
        var subject=new SubjectKey(RealmId.random(),UUID.randomUUID());
        var epoch=new TargetEpoch(subject,7);
        var current=new AtomicBoolean(true);
        var life=new WriteSinkId("ronova:terminal-state/life");
        var removal=new WriteSinkId("ronova:terminal-state/removal");
        var lease=new WriteSetLease(UUID.randomUUID(),epoch,new PolicyRevision(3),current::get,List.of(life));
        require(lease.consume(life),"declared sink fenced");
        require(lease.assessment().exact(),"declared sink did not produce exact write set");
        require(lease.evidence().size()==1&&lease.evidence().get(0).outcome()==WriteSinkEvidence.Outcome.ACCEPTED,
            "accepted sink evidence missing");
        var lifeReceipt=lease.receipt();
        require(lifeReceipt.exact()&&lifeReceipt.declared().equals(Set.of(life))
            &&lifeReceipt.consumed().equals(Set.of(life)),"typed write-set receipt lost exact life sink");
        require(!lease.consume(removal),"undeclared sink escaped write set");
        require(!lease.assessment().exact()&&lease.assessment().rejected().contains(removal),"undeclared sink not retained");

        var revoked=new WriteSetLease(UUID.randomUUID(),epoch,new PolicyRevision(3),current::get,List.of(removal));
        current.set(false);
        require(!revoked.consume(removal),"revoked upstream permission granted write");
        require(revoked.evidence().get(0).outcome()==WriteSinkEvidence.Outcome.FENCED_UNVERIFIED,
            "revoked upstream evidence misclassified");

        current.set(true);
        var missing=new WriteSetLease(UUID.randomUUID(),epoch,new PolicyRevision(3),current::get,List.of(life,removal));
        require(missing.consume(life),"declared life sink unavailable");
        require(!missing.assessment().exact()&&missing.assessment().missing().equals(Set.of(removal)),
            "missing declared sink not detected");
        require(!missing.receipt().exact(),"incomplete declaration produced an exact typed receipt");
        var removalLease=new WriteSetLease(lifeReceipt.operationId(),epoch,new PolicyRevision(3),current::get,List.of(removal));
        require(removalLease.consume(removal),"declared removal sink unavailable");
        var combined=WriteSetReceipt.combine(List.of(lifeReceipt,removalLease.receipt()));
        require(combined!=null&&combined.exact()&&combined.consumed().equals(Set.of(life,removal)),
            "exact typed receipts did not combine within one operation scope");
        missing.close();
        require(!missing.current()&&!missing.consume(removal),"closed write set remained usable");
        System.out.println("WriteSetLeaseSelfTest: PASS (upstream narrowing, exact set, undeclared rejection, revocation)");
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
