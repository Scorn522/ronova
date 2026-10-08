package dev.ronova.coremod.network;

import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.engine.*;
import java.util.*;

public final class ProjectionTransmissionSelfTest {
    public static void main(String[] args) {
        var marks=new ArrayList<JudgmentMark>();
        for(int i=0;i<257;i++)marks.add(new JudgmentMark(new UUID(717,i+1),"test:entity","minecraft:overworld",0,0,0,0,JudgmentScope.IDENTITY));
        var transfer=new ProjectionTransmission(new UUID(717,500),new UUID(717,501),1,1,0,new DominionEpoch(1,1),ProjectionFrame.Mode.REPLACE,marks,null);
        marks.clear();
        require(transfer.next().marks().size()==128,"outgoing snapshot retained mutable caller list");
        require(transfer.next().equals(transfer.next()),"reading an unsent part advanced the transaction");
        int count=0,total=0;
        while(!transfer.complete()) {
            var part=transfer.next();
            require(part.partIndex()==count&&part.partCount()==3,"part sequence changed");
            require(transfer.nextWireBytes()>0&&transfer.nextWireBytes()<=512L*1024,"part does not fit one connection tick budget");
            total+=part.marks().size();count++;transfer.sent();
        }
        require(total==257&&transfer.nextWireBytes()==0,"outgoing image was truncated or held phantom work");
        try {transfer.next();throw new AssertionError("completed transmission replayed a part");}catch(IllegalStateException expected){}
        var oversized=new ArrayList<JudgmentMark>();
        var longMark=new JudgmentMark(new UUID(717,1),"x".repeat(256),"y".repeat(256),0,0,0,0,JudgmentScope.IDENTITY);
        for(int i=0;i<ProjectionFrame.MAX_MARKS;i++)oversized.add(longMark);
        try {new ProjectionTransmission(new UUID(717,500),new UUID(717,501),1,1,0,new DominionEpoch(1,1),ProjectionFrame.Mode.REPLACE,oversized,null);
            throw new AssertionError("oversized retained projection accepted");}catch(IllegalArgumentException expected){}
        var budget=new ProjectionTickBudget(4,1000);
        budget.begin(8);budget.charge(600);budget.begin(8);
        require(!budget.fits(401)&&budget.bytes()==600,"repeated pump replenished one tick's byte allowance");
        budget.charge(400);require(budget.exhausted(),"exact byte boundary did not exhaust allowance");
        budget.begin(9);for(int i=0;i<4;i++)budget.charge(1);
        require(!budget.fits(1)&&budget.parts()==4,"small packets bypassed the part allowance");
        budget.begin(10);require(budget.fits(1000),"new tick never released its allowance");
        var delivery=new ProjectionDeliveryState();
        delivery.failed(ProjectionDeliveryState.Stage.HEAP_BUDGET,0);
        require(delivery.retryAt()==20&&delivery.summary(0,true).contains("堆预算不足"),"resource refusal lost its reason or backoff");
        delivery.queued();delivery.sent();delivery.failed(ProjectionDeliveryState.Stage.ACK_TIMEOUT,120);
        require(delivery.retryAt()==160&&delivery.failures()==2,"sending without an ACK erased consecutive failures");
        delivery.confirmed();delivery.failed(ProjectionDeliveryState.Stage.TRANSPORT,200);
        require(delivery.retryAt()==220,"confirmed receipt did not reset retry delay");
        for(int i=0;i<10;i++)delivery.failed(ProjectionDeliveryState.Stage.TRANSPORT,300);
        require(delivery.retryAt()==500,"persistent failures exceeded bounded backoff");
        delivery.close();require(delivery.stage()==ProjectionDeliveryState.Stage.CLOSED&&!delivery.summary(0,false).contains("后重试"),"closed endpoint offered a retry");
        System.out.println("ProjectionTransmissionSelfTest: PASS (owned immutable image, bounded parts, explicit send progress, retained byte refusal)");
    }
    private static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
