package dev.ronova.coremod.client;

import dev.ronova.coremod.network.RonovaAction;

public final class BladeBlockGestureSelfTest {
    public static void main(String[] args) {
        var gesture=new BladeBlockGesture();require(gesture.release(true)==null,"unarmed release triggered");
        gesture.begin(false);require(gesture.armed(),"block did not arm");
        gesture.begin(true);require(gesture.release(true)==RonovaAction.AREA,"holding changed the selected action");
        require(gesture.release(true)==null,"mouse callback and polling duplicated release");
        gesture.begin(true);gesture.begin(false);require(gesture.release(true)==RonovaAction.LOADED,"releasing Shift first changed Shift-right action");
        gesture.begin(false);require(gesture.release(false)==null,"invalid/switched-item block triggered");
        gesture.begin(true);gesture.cancel();require(gesture.release(true)==null,"GUI/disconnect cancellation triggered");
        System.out.println("BladeBlockGestureSelfTest: PASS (release only, mode latch, duplicate suppression and cancellation)");
    }
    private static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
