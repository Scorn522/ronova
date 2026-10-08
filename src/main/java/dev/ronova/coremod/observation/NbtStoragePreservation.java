package dev.ronova.coremod.observation;

import java.util.*;
import net.minecraft.nbt.*;

/** Compare private decoded images, never mutable world-owned tags. Positions require separate production provenance; no write authority is granted. */
public final class NbtStoragePreservation {
    public sealed interface Step permits Key,Index {}
    public record Key(String value) implements Step {
        public Key {Objects.requireNonNull(value);if(value.length()>512)throw new IllegalArgumentException("key too large");}
    }
    public record Index(int value) implements Step {
        public Index {if(value<0)throw new IllegalArgumentException("negative index");}
    }
    public enum Result { PRESERVED, UNRELATED_CHANGE, TARGET_PRESENT, INVALID_TARGET, UNSUPPORTED_OR_LIMIT }
    private static final class Node {boolean remove;final Map<Step,Node> children=new HashMap<>();}
    private static final Node EMPTY=new Node();
    private NbtStoragePreservation() {}

    public static Result verify(CompoundTag before,CompoundTag proposed,List<List<Step>> provenRemovals,
                                int nodeLimit,long byteLimit) {
        if(before==null||proposed==null||provenRemovals==null||provenRemovals.isEmpty()||provenRemovals.size()>64
            ||nodeLimit<1||nodeLimit>65536||byteLimit<1||byteLimit>64L*1024*1024)return Result.UNSUPPORTED_OR_LIMIT;
        var root=new Node();
        for(var path:provenRemovals) {
            if(path==null||path.isEmpty()||path.size()>64)return Result.INVALID_TARGET;
            Node at=root;
            for(var step:path) {
                if(step==null||at.remove)return Result.INVALID_TARGET;
                at=at.children.computeIfAbsent(step,ignored->new Node());
            }
            if(at.remove||!at.children.isEmpty())return Result.INVALID_TARGET;
            at.remove=true;
        }
        try {return new Compare(nodeLimit,byteLimit).visit(before,proposed,root,0);}
        catch(ConcurrentModificationException|IndexOutOfBoundsException|NullPointerException unstable) {return Result.UNSUPPORTED_OR_LIMIT;}
    }
    private static final class Compare {
        int nodes;long bytes;
        final IdentityHashMap<Tag,Boolean> beforeStack=new IdentityHashMap<>(),afterStack=new IdentityHashMap<>();
        Compare(int nodes,long bytes){this.nodes=nodes;this.bytes=bytes;}
        Result visit(Tag before,Tag after,Node scope,int depth) {
            if(--nodes<0||depth>64)return Result.UNSUPPORTED_OR_LIMIT;
            if(before==null)return Result.INVALID_TARGET;
            if(scope.remove)return after==null?Result.PRESERVED:Result.TARGET_PRESENT;
            if(after==null)return Result.UNRELATED_CHANGE;
            if(before.getClass()!=after.getClass())return Result.UNRELATED_CHANGE;
            if(beforeStack.containsKey(before)||afterStack.containsKey(after))return Result.UNSUPPORTED_OR_LIMIT;
            beforeStack.put(before,true);afterStack.put(after,true);
            try {
                if(before.getClass()==CompoundTag.class) {
                    var a=(CompoundTag)before;var b=(CompoundTag)after;
                    for(var step:scope.children.keySet())if(!(step instanceof Key key)||!a.contains(key.value()))return Result.INVALID_TARGET;
                    for(String key:a.getAllKeys()) {
                        if(key==null||key.length()>512||!charge(2L*key.length()))return Result.UNSUPPORTED_OR_LIMIT;
                        Node child=scope.children.getOrDefault(new Key(key),EMPTY);
                        if(child.remove&&b.contains(key))return Result.TARGET_PRESENT;
                        var result=visit(a.get(key),b.get(key),child,depth+1);
                        if(result!=Result.PRESERVED)return result;
                    }
                    for(String key:b.getAllKeys())if(!a.contains(key))return Result.UNRELATED_CHANGE;
                    return Result.PRESERVED;
                }
                if(before.getClass()==ListTag.class) {
                    var a=(ListTag)before;var b=(ListTag)after;
                    for(var step:scope.children.keySet())if(!(step instanceof Index index)||index.value()>=a.size())return Result.INVALID_TARGET;
                    if(a.getElementType()!=b.getElementType()&&!(b.isEmpty()&&b.getElementType()==0&&!scope.children.isEmpty()))return Result.UNRELATED_CHANGE;
                    int next=0;
                    for(int i=0;i<a.size();i++) {
                        Node child=scope.children.getOrDefault(new Index(i),EMPTY);
                        Tag value=child.remove?null:next<b.size()?b.get(next++):null;
                        var result=visit(a.get(i),value,child,depth+1);
                        if(result!=Result.PRESERVED)return result;
                    }
                    return next==b.size()?Result.PRESERVED:Result.UNRELATED_CHANGE;
                }
                if(!scope.children.isEmpty())return Result.INVALID_TARGET;
                long cost=scalarCost(before)+scalarCost(after);
                if(cost<0||!charge(cost))return Result.UNSUPPORTED_OR_LIMIT;
                boolean same=before.getClass()==FloatTag.class
                    ?Float.floatToRawIntBits(((FloatTag)before).getAsFloat())==Float.floatToRawIntBits(((FloatTag)after).getAsFloat())
                    :before.getClass()==DoubleTag.class
                        ?Double.doubleToRawLongBits(((DoubleTag)before).getAsDouble())==Double.doubleToRawLongBits(((DoubleTag)after).getAsDouble())
                        :before.equals(after);
                return same?Result.PRESERVED:Result.UNRELATED_CHANGE;
            } finally {beforeStack.remove(before);afterStack.remove(after);}
        }
        boolean charge(long count){if(count<0||count>bytes)return false;bytes-=count;return true;}
        long scalarCost(Tag tag) {
            Class<?> type=tag.getClass();
            if(type==ByteTag.class||type==ShortTag.class||type==IntTag.class||type==LongTag.class
                ||type==FloatTag.class||type==DoubleTag.class)return 8;
            if(type==StringTag.class)return 2L*((StringTag)tag).getAsString().length();
            if(type==ByteArrayTag.class)return ((ByteArrayTag)tag).getAsByteArray().length;
            if(type==IntArrayTag.class)return 4L*((IntArrayTag)tag).getAsIntArray().length;
            if(type==LongArrayTag.class)return 8L*((LongArrayTag)tag).getAsLongArray().length;
            return -1;
        }
    }
}
