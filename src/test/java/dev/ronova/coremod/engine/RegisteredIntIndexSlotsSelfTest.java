package dev.ronova.coremod.engine;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;

public final class RegisteredIntIndexSlotsSelfTest {
    private static final class Value {
        @Override public boolean equals(Object other){throw new AssertionError("foreign equality invoked");}
        @Override public int hashCode(){throw new AssertionError("foreign hash invoked");}
    }
    public static void main(String[] args) throws Exception {
        var map=new Int2ObjectLinkedOpenHashMap<Object>();var target=new Value();var other=new Value();
        for(int i=1;i<=200;i++)map.put(i,other);
        map.put(0,target);map.put(-71,target);map.put(100001,target);
        var before=RegisteredIntIndexSlots.read(map);
        require(before!=null&&before.bound(),"registered layout unavailable");
        int selected=find(before,target);int key=before.keys()[selected];map.put(key,other);
        require(!before.removeIfSame(selected,target)&&map.get(key)==other,"replacement entry was removed");
        map.put(key,target);
        for (int added = 10000; added < 20000; added++) map.put(added, new Object());
        require(!before.bound()&&!before.removeIfSame(selected,target),"old array view survived rehash");
        int removed=0;
        for(int attempt=0;attempt<10;attempt++) {
            var view=RegisteredIntIndexSlots.read(map);int position=find(view,target);
            if(position<0)break;
            require(view.removeIfSame(position,target),"exact alias removal failed");removed++;
        }
        require(removed==3&&map.size()==10200&&map.get(1)==other&&map.get(200)==other
            &&map.containsKey(10000)&&map.containsKey(19999),
            "numeric aliases remained or unrelated entries were removed");
        require(RegisteredIntIndexSlots.read(new Int2ObjectLinkedOpenHashMap<Object>(){})==null,
            "custom container inherited registered layout permission");
        var current=RegisteredIntIndexSlots.read(map);
        var mask=Int2ObjectLinkedOpenHashMap.class.getDeclaredField("mask");mask.setAccessible(true);
        int originalMask=mask.getInt(map);
        try {
            mask.setInt(map,0);
            require(RegisteredIntIndexSlots.read(map)==null&&!current.bound(),"damaged mask retained a writable view");
        } finally {mask.setInt(map,originalMask);}
        var size=Int2ObjectLinkedOpenHashMap.class.getDeclaredField("size");size.setAccessible(true);
        int originalSize=size.getInt(map);
        try {
            size.setInt(map,-1);
            require(RegisteredIntIndexSlots.read(map)==null&&!current.bound(),"negative size retained a writable view");
        } finally {size.setInt(map,originalSize);}
        System.out.println("RegisteredIntIndexSlotsSelfTest PASS");
    }
    private static int find(RegisteredIntIndexSlots.View view,Object target) {
        for(int i=0;i<view.values().length;i++)if(view.values()[i]==target)return i;
        return -1;
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
