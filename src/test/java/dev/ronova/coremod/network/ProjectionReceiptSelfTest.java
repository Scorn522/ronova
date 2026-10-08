package dev.ronova.coremod.network;

import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.engine.*;
import java.util.*;

public final class ProjectionReceiptSelfTest {
    public static void main(String[] args) {
        UUID realm=new UUID(717,1),stream=new UUID(717,2);var epoch=new DominionEpoch(1,3);
        var first=new JudgmentMark(new UUID(717,10),"test:a","minecraft:overworld",1,2,3,0,JudgmentScope.IDENTITY);
        var second=new JudgmentMark(new UUID(717,11),"test:b","minecraft:overworld",4,5,6,0,JudgmentScope.IDENTITY);
        String digest=ProjectionDigest.of(List.of(first,second));
        require(digest.equals(ProjectionDigest.of(List.of(second,first))),"projection order changed content digest");
        var state=new ProjectionReceiptState(realm,stream,1);state.expect(1,epoch,digest);
        require(!state.current(),"sending became a client receipt");
        var ack=new ProjectionAcknowledgement(realm,stream,1,1,epoch,digest);
        require(state.accept(ack,10)&&state.current(),"exact client view receipt rejected");
        require(state.accept(ack,20)&&state.receivedAt()==10,"duplicate reply renewed the evidence window");
        state.expect(2,epoch,digest);
        require(!state.current()&&!state.accept(ack,21),"old revision acknowledged a new publication");
        require(!state.accept(new ProjectionAcknowledgement(realm,stream,1,2,epoch,ProjectionDigest.of(List.of(first))),22),"partial image acknowledged full snapshot");
        require(!state.accept(new ProjectionAcknowledgement(realm,new UUID(717,9),1,2,epoch,digest),23),"old connection identity accepted");
        var fresh=new ProjectionReceiptState(realm,new UUID(717,3),2);fresh.expect(1,epoch,digest);
        require(!fresh.accept(ack,24)&&!fresh.current(),"receipt survived reconnect without new participation");
        System.out.println("ProjectionReceiptSelfTest: PASS (canonical image, no send-as-ack, exact revision/content, duplicate freshness, reconnect isolation)");
    }
    private static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
