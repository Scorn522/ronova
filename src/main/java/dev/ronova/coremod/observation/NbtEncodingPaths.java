package dev.ronova.coremod.observation;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.nbt.*;

/** Bounded observation of references in the actual writer input; never infers provenance from NBT values. */
public final class NbtEncodingPaths {
    public record Result(boolean complete,List<List<NbtStoragePreservation.Step>> paths) {
        public Result {paths=paths.stream().map(List::copyOf).toList();}
    }
    private static final java.lang.reflect.Field COMPOUND=backing(CompoundTag.class,Map.class);
    private static final java.lang.reflect.Field LIST=backing(ListTag.class,List.class);
    private static java.lang.reflect.Field backing(Class<?> owner,Class<?> type) {
        java.lang.reflect.Field found=null;
        try {
            for(var field:owner.getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())&&field.getType()==type) {
                if(found!=null)return null;found=field;
            }
            return found!=null&&found.trySetAccessible()?found:null;
        } catch(RuntimeException unavailable){return null;}
    }
    private static Object read(java.lang.reflect.Field field,Object owner) {
        try {return field==null?null:field.get(owner);}catch(IllegalAccessException unavailable){return null;}
    }
    private NbtEncodingPaths() {}
    public static Result find(CompoundTag image,Predicate<CompoundTag> matches,BooleanSupplier allowance) {
        var scan=new Scan(matches,allowance);
        try {
            if(image==null||!scan.visit(image,new ArrayList<>()))return new Result(false,List.of());
            return new Result(true,scan.paths);
        } catch(RuntimeException unstable){return new Result(false,List.of());}
    }
    private static final class Scan {
        final Predicate<CompoundTag> matches;final BooleanSupplier allowance;
        final IdentityHashMap<Tag,Boolean> active=new IdentityHashMap<>();
        final List<List<NbtStoragePreservation.Step>> paths=new ArrayList<>();int nodes;
        Scan(Predicate<CompoundTag> matches,BooleanSupplier allowance){this.matches=Objects.requireNonNull(matches);this.allowance=Objects.requireNonNull(allowance);}
        boolean visit(Tag tag,List<NbtStoragePreservation.Step> path) {
            if(tag==null||path.size()>64||++nodes>4096||!allowance.getAsBoolean()||active.put(tag,true)!=null)return false;
            try {
                Class<?> type=tag.getClass();
                if(type==CompoundTag.class) {
                    var value=(CompoundTag)tag;
                    if(matches.test(value)) {
                        if(path.isEmpty()||paths.size()>=64)return false;
                        paths.add(List.copyOf(path));
                    }
                    Object backing=read(COMPOUND,value);
                    if(backing==null||(backing.getClass()!=HashMap.class&&backing.getClass()!=LinkedHashMap.class))return false;
                    for(var entry:((Map<?,?>)backing).entrySet()) {
                        if(!(entry.getKey() instanceof String key)||key.length()>512||!(entry.getValue() instanceof Tag child))return false;
                        path.add(new NbtStoragePreservation.Key(key));
                        boolean complete=visit(child,path);path.remove(path.size()-1);
                        if(!complete)return false;
                    }
                    return true;
                }
                if(type==ListTag.class) {
                    var value=(ListTag)tag;
                    Object backing=read(LIST,value);
                    if(backing==null||backing.getClass()!=ArrayList.class)return false;
                    var entries=(ArrayList<?>)backing;
                    for(int i=0;i<entries.size();i++) {
                        if(!(entries.get(i) instanceof Tag child))return false;
                        path.add(new NbtStoragePreservation.Index(i));
                        boolean complete=visit(child,path);path.remove(path.size()-1);
                        if(!complete)return false;
                    }
                    return true;
                }
                return type==ByteTag.class||type==ShortTag.class||type==IntTag.class||type==LongTag.class
                    ||type==FloatTag.class||type==DoubleTag.class||type==StringTag.class||type==ByteArrayTag.class
                    ||type==IntArrayTag.class||type==LongArrayTag.class;
            } finally {active.remove(tag);}
        }
    }
}
