package dev.ronova.coremod.runtime;

import dev.ronova.coremod.policy.MechanismLevel;

public final class StateFeedbackScheduleSelfTest {
    public static void main(String[] args) {
        var extreme=new StateFeedbackSchedule(0,MechanismLevel.STRICT_ALL);
        require(!extreme.due(0)&&extreme.due(1),"feedback re-read the same write tick");
        extreme.observed(1,false);
        extreme.updateDepth(2,MechanismLevel.STRICT_ALL);
        require(!extreme.due(2)&&extreme.due(3),"stable state did not back off");
        extreme.observed(3,true);
        require(extreme.due(4),"a rebound did not request a next-tick check");
        extreme.unavailable(4);
        require(!extreme.due(4)&&extreme.due(5),"unavailable observation was treated as stable success");
        for(int tick=5;tick<2000;tick++)if(extreme.due(tick))extreme.observed(tick,false);
        require(extreme.continuous()&&!extreme.finished(),"extreme silently dropped its surviving watch");
        extreme.updateDepth(2000,MechanismLevel.PROTECT_RONOVA);
        require(!extreme.continuous()&&!extreme.due(2000)&&extreme.due(2001),"depth reduction did not apply its new cadence");
        for(int tick=2001;tick<=2020;tick++)if(extreme.due(tick))extreme.observed(tick,false);
        require(extreme.finished(),"reduced depth kept an infinite horizon");
        extreme.updateDepth(2021,MechanismLevel.STRICT_ALL);
        require(!extreme.finished()&&extreme.continuous()&&extreme.due(2022),"depth increase did not restart continuous supervision");
        for(var depth:new MechanismLevel[]{MechanismLevel.PROTECT_RONOVA,MechanismLevel.RELATED_PATHS}) {
            var finite=new StateFeedbackSchedule(0,depth);
            int horizon=depth==MechanismLevel.PROTECT_RONOVA?20:100;
            for(int tick=0;tick<horizon;tick++) {
                if(finite.due(tick))finite.observed(tick,false);
                require(!finite.finished(),"finite watch ended before its final observation");
            }
            require(finite.due(horizon),"finite watch skipped its final horizon");
            finite.observed(horizon,false);require(finite.finished(),"finite watch did not retire");
        }
        var wrapped=new StateFeedbackSchedule(Integer.MAX_VALUE,MechanismLevel.STRICT_ALL);
        require(wrapped.due(Integer.MIN_VALUE),"tick wrap prevented feedback");
        wrapped.observed(Integer.MIN_VALUE,true);
        require(wrapped.due(Integer.MIN_VALUE+1),"tick wrap prevented follow-up repair");
        System.out.println("StateFeedbackScheduleSelfTest: PASS (next-tick response, adaptive cadence, sustained extreme, finite horizons, wrap)");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
