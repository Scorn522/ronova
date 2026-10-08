package dev.ronova.coremod.observation;

import java.lang.ref.WeakReference;
import java.util.*;

/** Actual writer traversal; published entries retain neither holders nor their object graphs. */
public final class NbtWriteReferences {
    public record Entry(WeakReference<Object> value,List<NbtStoragePreservation.Step> path) {
        public Entry {path=List.copyOf(path);}
    }
    public record Snapshot(boolean complete,List<Entry> entries) {
        public Snapshot {entries=List.copyOf(entries);}
    }
    private static final class Frame {Object value;int index;Frame(Object value){this.value=value;}}
    private final ArrayList<Frame> frames=new ArrayList<>();
    private final ArrayList<NbtStoragePreservation.Step> path=new ArrayList<>();
    private final ArrayList<Entry> entries=new ArrayList<>();
    private boolean valid=true;private int nodes;
    public NbtWriteReferences(Object root){frames.add(new Frame(root));}
    public AutoCloseable enter(Object parent,Object child,String key,boolean compound,boolean supported) {
        if(!valid)return null;
        if(!supported||frames.get(frames.size()-1).value!=parent||path.size()>=64||++nodes>4096
            ||key!=null&&key.length()>512){valid=false;return null;}
        for(var frame:frames)if(frame.value==child){valid=false;return null;}
        var step=key==null?new NbtStoragePreservation.Index(frames.get(frames.size()-1).index++):new NbtStoragePreservation.Key(key);
        path.add(step);var frame=new Frame(child);frames.add(frame);
        if(compound) {
            if(entries.size()>=64)valid=false;
            else entries.add(new Entry(new WeakReference<>(child),path));
        }
        return ()->{
            if(frames.isEmpty())return;
            if(frames.get(frames.size()-1)!=frame){valid=false;return;}
            frames.remove(frames.size()-1);path.remove(path.size()-1);frame.value=null;
        };
    }
    public Snapshot snapshot(){return valid&&frames.size()==1?new Snapshot(true,entries):new Snapshot(false,List.of());}
    /** Drop owned traversal references even if a caller retains its already-closed stream or scope. */
    public void release(){valid=false;for(var frame:frames)frame.value=null;frames.clear();path.clear();entries.clear();}
}
