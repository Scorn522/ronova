package dev.ronova.coremod.budget;

/** Bounded round-robin selection; a failed budget reservation must not consume a lane's turn. */
public final class FairLaneCursor {
    private final int lanes;
    private int next;
    public FairLaneCursor(int lanes){if(lanes<1||lanes>30)throw new IllegalArgumentException("invalid lane count");this.lanes=lanes;}
    public int peek(int readyMask){for(int i=0;i<lanes;i++){int lane=(next+i)%lanes;if((readyMask&(1<<lane))!=0)return lane;}return -1;}
    public void consumed(int lane){if(lane<0||lane>=lanes)throw new IllegalArgumentException("invalid lane");next=(lane+1)%lanes;}
    public void reset(){next=0;}
}
