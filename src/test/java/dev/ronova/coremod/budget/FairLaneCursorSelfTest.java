package dev.ronova.coremod.budget;

public final class FairLaneCursorSelfTest {
    public static void main(String[] args) {
        var cursor=new FairLaneCursor(4);int[] served=new int[4];
        for(int i=0;i<100000;i++) {
            int selected=cursor.peek(15);
            require(cursor.peek(15)==selected,"budget denial consumed a turn");
            cursor.consumed(selected);served[selected]++;
        }
        for(int count:served)require(count==25000,"hot traffic starved a cold or seed lane");
        cursor.reset();require(cursor.peek(0)==-1,"empty lanes produced work");
        require(cursor.peek(9)==0,"first ready lane skipped");cursor.consumed(0);
        require(cursor.peek(9)==3,"seed lane starved behind a continuously hot lane");
        System.out.println("FairLaneCursorSelfTest: PASS (100k saturated selections, equal service, reservation failure, empty lanes and seed progress)");
    }
    private static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
