package dev.ronova.coremod.client;

import dev.ronova.coremod.network.RonovaAction;

/** One actual blade-use gesture. Holding and duplicate release observations never submit another action. */
public final class BladeBlockGesture {
    private RonovaAction armed;
    public void begin(boolean shifted){if(armed==null)armed=shifted?RonovaAction.LOADED:RonovaAction.AREA;}
    public boolean armed(){return armed!=null;}
    public RonovaAction release(boolean valid){var action=valid?armed:null;armed=null;return action;}
    public void cancel(){armed=null;}
}
