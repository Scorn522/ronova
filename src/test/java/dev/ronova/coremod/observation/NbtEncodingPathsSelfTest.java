package dev.ronova.coremod.observation;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.nbt.*;

public final class NbtEncodingPathsSelfTest {
    public static void main(String[] args) throws Exception {
        var seed=new CompoundTag();seed.putString("UUID","same-value");
        var root=new CompoundTag();root.put("direct",seed);root.put("lookalike",seed.copy());
        var list=new ListTag();list.add(seed);root.put("nested",list);
        var result=NbtEncodingPaths.find(root,value->value==seed,()->true);
        Set<List<NbtStoragePreservation.Step>> expected=Set.of(
            List.of(new NbtStoragePreservation.Key("direct")),
            List.of(new NbtStoragePreservation.Key("nested"),new NbtStoragePreservation.Index(0)));
        require(result.complete()&&Set.copyOf(result.paths()).equals(expected),"identity paths or shared aliases lost: "+result);
        require(NbtEncodingPaths.find(root,value->false,()->true).paths().isEmpty(),"NBT values fabricated provenance");
        var permits=new AtomicInteger();
        var limited=NbtEncodingPaths.find(root,value->value==seed,()->permits.incrementAndGet()<=2);
        require(!limited.complete()&&limited.paths().isEmpty(),"partial coverage published actionable paths");
        var cycle=new CompoundTag();cycle.put("cycle",cycle);
        require(!NbtEncodingPaths.find(cycle,value->false,()->true).complete(),"cycle became complete");
        require(!NbtEncodingPaths.find(seed,value->value==seed,()->true).complete(),"whole writer root became a removable record");
        boolean immutable=false;try{result.paths().get(0).add(new NbtStoragePreservation.Key("changed"));}
        catch(UnsupportedOperationException expectedFailure){immutable=true;}
        require(immutable,"captured path remained mutable");
        var hostile=new HostileMap();var custom=new CompoundTag();
        java.lang.reflect.Field field=Arrays.stream(CompoundTag.class.getDeclaredFields())
            .filter(value->!java.lang.reflect.Modifier.isStatic(value.getModifiers())&&value.getType()==Map.class).findFirst().orElseThrow();
        require(field.trySetAccessible(),"test backing field unavailable");field.set(custom,hostile);
        require(!NbtEncodingPaths.find(custom,value->false,()->true).complete()&&hostile.calls==0,"observer executed foreign map callbacks");
        var a=new SavedDataSaveTrace();var b=new SavedDataSaveTrace();
        for(int i=0;i<12;i++)require(a.nextEncodingStart(3)==i%3&&b.nextEncodingStart(2)==i%2,"holder-local encoding rotation lost fairness");
        System.out.println("NbtEncodingPathsSelfTest PASS (identity/aliases, nonassociation, budget, cycles, foreign backing, immutable paths, rotation)");
    }
    private static final class HostileMap extends HashMap<String,Tag> {
        int calls;
        @Override public Set<Map.Entry<String,Tag>> entrySet(){calls++;throw new AssertionError("foreign callback");}
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
