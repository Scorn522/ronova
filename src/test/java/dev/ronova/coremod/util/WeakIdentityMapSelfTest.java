package dev.ronova.coremod.util;

import java.util.UUID;

public final class WeakIdentityMapSelfTest {
    private static final class Physical {
        UUID uuid;
        Physical(UUID uuid) { this.uuid=uuid; }
        @Override public boolean equals(Object other) { throw new AssertionError("foreign equality called"); }
        @Override public int hashCode() { throw new AssertionError("foreign hash called"); }
    }
    public static void main(String[] args) {
        var uuid=UUID.randomUUID(); var old=new Physical(uuid); var replacement=new Physical(uuid);
        var map=new WeakIdentityMap<Physical,Integer>();
        map.put(old,7);map.put(replacement,8);
        require(map.size()==2&&map.get(old)==7&&map.get(replacement)==8,"same UUID collapsed distinct physical objects");
        old.uuid=UUID.randomUUID();
        require(map.get(old)==7,"mutable logical identity lost the physical cache entry");
        map.remove(old);
        require(map.size()==1&&map.get(replacement)==8,"old object cleanup removed the replacement");
        require(map.computeIfAbsent(replacement,ignored->{throw new AssertionError("existing entry was recomputed");})==8,
            "existing physical entry was not reused");
        map.clear();require(map.size()==0,"clear retained an entry");
        System.out.println("WeakIdentityMapSelfTest PASS");
    }
    private static void require(boolean value,String message) { if(!value)throw new AssertionError(message); }
}
