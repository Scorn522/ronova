package dev.ronova.coremod.observation;

import java.util.List;

public final class DiscoveryCursorSelfTest {
    public static void main(String[] args) {
        var cursor=new DiscoveryCursor<Integer>();
        var entries=List.of(1,2,3).iterator();cursor.set(entries,0);
        require(cursor.get(0).next()==1,"first entry missing");
        cursor.touch(DiscoveryCursor.MAX_IDLE_NANOS-1);
        require(cursor.get(DiscoveryCursor.MAX_IDLE_NANOS).next()==2,"active scan restarted at the first entry");
        require(cursor.expire(2*DiscoveryCursor.MAX_IDLE_NANOS)&&cursor.get(2*DiscoveryCursor.MAX_IDLE_NANOS)==null,
            "inactive scan retained its borrowed iterator");
        cursor.touch(2*DiscoveryCursor.MAX_IDLE_NANOS+1);
        require(cursor.get(2*DiscoveryCursor.MAX_IDLE_NANOS+1)==null,"renewal revived an expired cursor");
        cursor.set(List.of(4).iterator(),100);require(cursor.get(99)==null,"backwards clock retained the old lease");
        cursor.set(List.of(5).iterator(),100);cursor.clear();cursor.clear();
        require(cursor.get(101)==null,"world exit retained a discovery handle");
        System.out.println("DiscoveryCursorSelfTest PASS");
    }
    private static void require(boolean value,String message) { if(!value)throw new AssertionError(message); }
}
