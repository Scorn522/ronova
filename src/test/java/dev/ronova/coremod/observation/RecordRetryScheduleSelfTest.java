package dev.ronova.coremod.observation;

public final class RecordRetryScheduleSelfTest {
    public static void main(String[] args) {
        var schedule=new RecordRetrySchedule();require(schedule.ready(0),"initial attempt delayed");
        schedule.failed(0);require(!schedule.ready(0)&&schedule.ready(1),"first retry was not next tick");
        schedule.failed(1);require(!schedule.ready(2)&&schedule.ready(3),"repeated failure spun each tick");
        int tick=3;
        for(int i=0;i<20;i++) {
            schedule.failed(tick);require(!schedule.ready(tick)&&schedule.ready(tick+20),"retry stalled beyond bounded delay");
            tick+=20;
        }
        schedule.failed(Integer.MAX_VALUE);
        require(!schedule.ready(Integer.MIN_VALUE)&&schedule.ready(Integer.MIN_VALUE+19),"tick wrap broke retry cadence");
        require(new RecordRetrySchedule().ready(tick),"new source inherited another source's failure delay");
        System.out.println("RecordRetryScheduleSelfTest PASS");
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
