package dev.ronova.coremod.observation;

/** Per-source retry cadence; it does not withdraw qualification or suppress other permitted paths. */
final class RecordRetrySchedule {
    private int nextTick, delay=1;
    private boolean waiting;
    boolean ready(int tick) { return !waiting||tick-nextTick>=0; }
    void failed(int tick) {
        waiting=true;nextTick=tick+delay;delay=Math.min(20,delay*2);
    }
}
