package dev.ronova.coremod.engine;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.lang.reflect.Field;

/** Registered fastutil layout; views are transient and never retain an iterator across mutations. */
final class RegisteredIntIndexSlots {
    private static final Field KEYS=field("key",int[].class), VALUES=field("value",Object[].class), CAPACITY=field("n",int.class),
        MASK=field("mask",int.class), SIZE=field("size",int.class);
    private static Field field(String name,Class<?> type) {
        try {
            var field=Int2ObjectLinkedOpenHashMap.class.getDeclaredField(name);
            if(field.getType()!=type||!field.trySetAccessible())throw new IllegalStateException("unavailable index layout");
            return field;
        } catch(ReflectiveOperationException failure) {throw new IllegalStateException("unavailable index layout",failure);}
    }
    record View(Int2ObjectMap<?> map,int[] keys,Object[] values) {
        boolean bound() {
            try {return KEYS.get(map)==keys&&VALUES.get(map)==values&&CAPACITY.getInt(map)==keys.length-1
                &&MASK.getInt(map)==keys.length-2&&SIZE.getInt(map)>=0&&SIZE.getInt(map)<keys.length-1;}
            catch(IllegalAccessException|RuntimeException unavailable){return false;}
        }
        boolean removeIfSame(int position,Object expected) {
            if(!bound()||position<0||position>=values.length||values[position]!=expected)return false;
            int key=keys[position];
            if(map.get(key)!=expected||!bound()||keys[position]!=key||values[position]!=expected)return false;
            map.remove(key);return true;
        }
    }
    static View read(Int2ObjectMap<?> map) {
        if(map==null||map.getClass()!=Int2ObjectLinkedOpenHashMap.class)return null;
        try {
            int[] keys=(int[])KEYS.get(map);Object[] values=(Object[])VALUES.get(map);int n=CAPACITY.getInt(map);
            int size=SIZE.getInt(map);
            return keys!=null&&values!=null&&n>0&&(n&(n-1))==0&&keys.length==n+1&&values.length==keys.length
                &&MASK.getInt(map)==n-1&&size>=0&&size<n
                ?new View(map,keys,values):null;
        } catch(IllegalAccessException|RuntimeException unavailable){return null;}
    }
    private RegisteredIntIndexSlots() {}
}
