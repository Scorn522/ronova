package dev.ronova.coremod.client;

import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.engine.*;
import dev.ronova.coremod.network.ProjectionFrame;
import java.util.*;

public final class ProjectionStreamSelfTest {
    private static final UUID REALM=new UUID(66,1),STREAM=new UUID(66,2);
    private static final DominionEpoch EPOCH=new DominionEpoch(1,10);
    public static void main(String[] args) {
        var state=new ProjectionStreamState();require(state.bind(REALM,STREAM,1),"context rejected");
        require(state.acceptOwner(3,true)&&!state.acceptOwner(2,false)&&!state.acceptOwner(3,false),"Owner revision rolled back/conflicted");
        var a=mark(1);var b=mark(2);
        require(state.accept(frame(1,0,ProjectionFrame.Mode.REPLACE,1,2,List.of(b),null),0)==ProjectionStreamState.Result.STAGED
            &&state.snapshot().isEmpty(),"partial snapshot published");
        require(state.accept(frame(1,0,ProjectionFrame.Mode.REPLACE,1,2,List.of(b),null),1)==ProjectionStreamState.Result.STAGED,"duplicate part rejected");
        require(state.accept(frame(1,0,ProjectionFrame.Mode.REPLACE,0,2,List.of(a),null),2)==ProjectionStreamState.Result.COMMITTED
            &&state.snapshot().size()==2,"out-of-order parts did not assemble atomically");
        require(state.accept(frame(2,1,ProjectionFrame.Mode.CLEAR,0,1,List.of(),null),3)==ProjectionStreamState.Result.COMMITTED,"clear failed");
        require(state.accept(frame(1,0,ProjectionFrame.Mode.APPEND,0,1,List.of(a),null),4)==ProjectionStreamState.Result.STALE
            &&state.snapshot().isEmpty(),"delayed old mark restored cleared state");
        require(state.accept(frame(4,3,ProjectionFrame.Mode.APPEND,0,1,List.of(a),null),5)==ProjectionStreamState.Result.RESYNC_REQUIRED,
            "missing base silently applied a delta");
        require(state.accept(frame(5,4,ProjectionFrame.Mode.REPLACE,0,1,List.of(a),null),6)==ProjectionStreamState.Result.COMMITTED
            &&!state.needsResync(6),"full resync could not bridge revision gap");
        state.accept(frame(6,5,ProjectionFrame.Mode.REPLACE,0,2,List.of(a),null),7);
        require(state.needsResync(3000)&&state.snapshot().equals(List.of(a)),"incomplete snapshot lost last committed view or never timed out");
        state.accept(frame(7,6,ProjectionFrame.Mode.REPLACE,0,2,List.of(a),null),3001);
        require(state.accept(frame(7,6,ProjectionFrame.Mode.REPLACE,0,2,List.of(b),null),3002)==ProjectionStreamState.Result.RESYNC_REQUIRED,
            "conflicting duplicate part accepted");
        require(!state.bind(REALM,new UUID(66,3),1),"same context revision replaced stream identity");
        require(state.bind(REALM,new UUID(66,3),2)&&state.snapshot().isEmpty(),"new connection context did not reset mirror");
        require(state.accept(frame(8,7,ProjectionFrame.Mode.REPLACE,0,1,List.of(a),null),3003)==ProjectionStreamState.Result.WRONG_CONTEXT,
            "old stream restored a new context");
        require(!state.bind(REALM,STREAM,1),"old context hello reset a newer session");
        state.reset();require(state.snapshot().isEmpty()&&state.revision()==0,"disconnect retained projection");
        state.bind(REALM,STREAM,1);state.accept(frame(1,0,ProjectionFrame.Mode.REPLACE,0,1,List.of(a,b),null),0);
        require(state.accept(frame(2,1,ProjectionFrame.Mode.REMOVE,0,1,List.of(),a.uuid()),1)==ProjectionStreamState.Result.COMMITTED
            &&state.snapshot().equals(List.of(b)),"identity removal changed another subject");
        var bounded = new ProjectionStreamState(2048);
        bounded.bind(REALM, STREAM, 1);
        require(bounded.accept(frame(1,0,ProjectionFrame.Mode.REPLACE,0,1,List.of(a),null),0)==ProjectionStreamState.Result.COMMITTED,
            "small bounded image rejected");
        long retained = bounded.memoryUsage().retainedBytes();
        require(bounded.accept(frame(2,1,ProjectionFrame.Mode.REPLACE,0,2,List.of(b),null),1)==ProjectionStreamState.Result.STAGED,
            "bounded partial image rejected");
        long staged = bounded.memoryUsage().retainedBytes();
        bounded.accept(frame(2,1,ProjectionFrame.Mode.REPLACE,0,2,List.of(b),null),2);
        require(bounded.memoryUsage().retainedBytes()==staged,"duplicate packet allocated twice");
        require(bounded.accept(frame(2,1,ProjectionFrame.Mode.REPLACE,1,2,List.of(mark(3),mark(4)),null),3)
            ==ProjectionStreamState.Result.RESYNC_REQUIRED && bounded.snapshot().equals(List.of(a))
            &&bounded.memoryUsage().retainedBytes()==retained && bounded.memoryUsage().denials()==1,
            "pressure replaced committed image or retained failed staging");
        require(bounded.accept(frame(3,2,ProjectionFrame.Mode.REPLACE,0,1,List.of(b),null),4)==ProjectionStreamState.Result.COMMITTED,
            "budget refusal prevented subsequent bounded resync");
        bounded.reset();
        require(bounded.memoryUsage().retainedBytes()==0 && bounded.memoryUsage().peakBytes()<=2048,
            "disconnect leaked owned projection budget");
        var large = new ProjectionStreamState(); large.bind(REALM, STREAM, 1);
        for (int part=0; part<ProjectionFrame.MAX_PARTS; part++) {
            List<JudgmentMark> marks = new ArrayList<>();
            for (int i=0; i<ProjectionFrame.MARKS_PER_PART; i++) marks.add(mark(1L+part*ProjectionFrame.MARKS_PER_PART+i));
            var result = large.accept(frame(1,0,ProjectionFrame.Mode.REPLACE,part,ProjectionFrame.MAX_PARTS,marks,null),part);
            require(result==(part==ProjectionFrame.MAX_PARTS-1?ProjectionStreamState.Result.COMMITTED:ProjectionStreamState.Result.STAGED),
                "capacity image failed at part "+part);
        }
        require(large.snapshot().size()==ProjectionFrame.MAX_MARKS && large.memoryUsage().peakBytes()<=large.memoryUsage().budgetBytes(),
            "capacity boundary or temporary allocation budget broken");
        large.reset();require(large.memoryUsage().retainedBytes()==0,"large image budget not released");
        var paced=new ProjectionStreamState();paced.bind(REALM,STREAM,1);
        for(int part=0;part<8;part++) {
            long now=part*1000L;
            require(!paced.needsResync(now),"active paced transfer expired from original start time");
            var result=paced.accept(frame(1,0,ProjectionFrame.Mode.REPLACE,part,8,List.of(mark(part+1)),null),now);
            require(result==(part==7?ProjectionStreamState.Result.COMMITTED:ProjectionStreamState.Result.STAGED),"paced frame failed");
        }
        paced.accept(frame(2,1,ProjectionFrame.Mode.REPLACE,0,2,List.of(a),null),8000);
        paced.accept(frame(2,1,ProjectionFrame.Mode.REPLACE,0,2,List.of(a),null),9500);
        require(paced.needsResync(10001),"duplicate packet indefinitely renewed a stalled transaction");
        var cancelled=new ProjectionStreamState();cancelled.bind(REALM,STREAM,1);
        cancelled.accept(frame(1,0,ProjectionFrame.Mode.REPLACE,0,1,List.of(a),null),0);
        cancelled.accept(frame(2,1,ProjectionFrame.Mode.REPLACE,0,2,List.of(a),null),1);
        require(cancelled.accept(frame(3,2,ProjectionFrame.Mode.REPLACE,0,1,List.of(),null),2)==ProjectionStreamState.Result.COMMITTED
            &&cancelled.snapshot().isEmpty(),"clear replacement waited for an unfinished old transaction");
        require(cancelled.accept(frame(2,1,ProjectionFrame.Mode.REPLACE,1,2,List.of(b),null),3)==ProjectionStreamState.Result.STALE
            &&cancelled.snapshot().isEmpty()&&!cancelled.needsResync(3),"cancelled image resurrected marks after clear");
        System.out.println("ProjectionStreamSelfTest: PASS (atomic chunks, duplicate/conflict, Owner versions, clear/replay, gap/timeout resync, stream/context reset)");
    }
    private static ProjectionFrame frame(long revision,long base,ProjectionFrame.Mode mode,int part,int parts,List<JudgmentMark> marks,UUID identity) {
        return new ProjectionFrame(REALM,STREAM,1,revision,base,EPOCH,mode,part,parts,marks,identity);
    }
    private static JudgmentMark mark(long id){return new JudgmentMark(new UUID(66,id),"test:entity","minecraft:overworld",0,0,0,0,JudgmentScope.IDENTITY);}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
