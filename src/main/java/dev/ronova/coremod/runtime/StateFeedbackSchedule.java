package dev.ronova.coremod.runtime;

import dev.ronova.coremod.policy.MechanismLevel;
import java.util.Objects;

/** Runtime response cadence, independent of Authority and of opaque lifecycle attempts. */
public final class StateFeedbackSchedule {
    private int started;
    private int horizon;
    private MechanismLevel depth;
    private int nextTick, stableSamples;
    private boolean finished;
    public StateFeedbackSchedule(int tick,MechanismLevel requestedDepth) {
        updateDepth(tick,requestedDepth);
    }
    /** Unrelated policy revisions preserve cadence; an actual depth change starts its new horizon. */
    public void updateDepth(int tick,MechanismLevel requestedDepth) {
        Objects.requireNonNull(requestedDepth);
        if(requestedDepth.rank()<MechanismLevel.PROTECT_RONOVA.rank())throw new IllegalArgumentException("write depth required");
        if(depth==requestedDepth)return;
        depth=requestedDepth;stableSamples=0;finished=false;
        started=tick;nextTick=tick+1;
        horizon=requestedDepth==MechanismLevel.STRICT_ALL?0:requestedDepth==MechanismLevel.RELATED_PATHS?100:20;
    }
    public boolean due(int tick){return !finished&&tick-nextTick>=0;}
    public void observed(int tick,boolean rebounded) {
        if(!due(tick))throw new IllegalStateException("feedback is not due");
        stableSamples=rebounded?0:Math.min(5,stableSamples+1);
        int interval=rebounded?1:Math.min(20,1<<stableSamples);
        if(horizon!=0) {
            long age=Integer.toUnsignedLong(tick-started);
            if(age>=horizon){finished=true;return;}
            interval=(int)Math.min(interval,horizon-age);
        }
        nextTick=tick+interval;
    }
    public void unavailable(int tick){if(!finished)nextTick=tick+1;}
    public boolean finished(){return finished;}
    public boolean continuous(){return horizon==0;}
}
