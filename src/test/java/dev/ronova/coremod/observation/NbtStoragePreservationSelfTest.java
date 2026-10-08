package dev.ronova.coremod.observation;

import java.util.List;
import net.minecraft.nbt.*;

public final class NbtStoragePreservationSelfTest {
    public static void main(String[] args) {
        var before=new CompoundTag();var records=new ListTag();
        var target=new CompoundTag();target.putString("UUID","target");
        var other=new CompoundTag();other.putString("UUID","other");other.putLongArray("unknown",new long[]{1,2,3});
        records.add(target);records.add(other);before.put("records",records);before.putString("unknown_extension","preserve");
        before.putDouble("nan_payload",Double.longBitsToDouble(0x7ff8000000000001L));before.putFloat("nan",Float.NaN);
        List<List<NbtStoragePreservation.Step>> removal=List.of(List.of(new NbtStoragePreservation.Key("records"),new NbtStoragePreservation.Index(0)));
        var after=before.copy();after.getList("records",Tag.TAG_COMPOUND).remove(0);
        require(check(before,after,removal)==NbtStoragePreservation.Result.PRESERVED,"target removal did not preserve other fields");
        var changed=after.copy();changed.remove("unknown_extension");
        require(check(before,changed,removal)==NbtStoragePreservation.Result.UNRELATED_CHANGE,"unknown field loss accepted");
        changed=after.copy();changed.putDouble("nan_payload",Double.longBitsToDouble(0x7ff8000000000002L));
        require(check(before,changed,removal)==NbtStoragePreservation.Result.UNRELATED_CHANGE,"floating-point bit change accepted");
        changed=after.copy();changed.getList("records",Tag.TAG_COMPOUND).getCompound(0).putLong("added",1);
        require(check(before,changed,removal)==NbtStoragePreservation.Result.UNRELATED_CHANGE,"unrelated record mutation accepted");
        require(check(before,before.copy(),removal)!=NbtStoragePreservation.Result.PRESERVED,"target still present accepted");
        require(check(before,after,List.of(List.of(new NbtStoragePreservation.Key("missing"))))==NbtStoragePreservation.Result.INVALID_TARGET,"missing baseline target accepted");
        require(check(before,after,List.of(List.of(new NbtStoragePreservation.Key("records")),removal.get(0)))==NbtStoragePreservation.Result.INVALID_TARGET,"overlapping positions accepted");
        require(check(before,after,List.of(removal.get(0),removal.get(0)))==NbtStoragePreservation.Result.INVALID_TARGET,"duplicate position accepted");
        require(NbtStoragePreservation.verify(before,after,removal,1,1024)==NbtStoragePreservation.Result.UNSUPPORTED_OR_LIMIT,"node limit ignored");
        require(NbtStoragePreservation.verify(before,after,removal,1024,1)==NbtStoragePreservation.Result.UNSUPPORTED_OR_LIMIT,"byte limit ignored");
        var cyclic=new CompoundTag();cyclic.put("cycle",cyclic);cyclic.put("target",target);
        var cyclicAfter=new CompoundTag();cyclicAfter.put("cycle",cyclicAfter);
        require(check(cyclic,cyclicAfter,List.of(List.of(new NbtStoragePreservation.Key("target"))))==NbtStoragePreservation.Result.UNSUPPORTED_OR_LIMIT,"cycle accepted as a persisted tree");
        var nested=new CompoundTag();nested.put("target",target);nested.put("other",other);
        var nestedAfter=nested.copy();nestedAfter.remove("target");
        require(check(nested,nestedAfter,List.of(List.of(new NbtStoragePreservation.Key("target"))))==NbtStoragePreservation.Result.PRESERVED,"compound target removal rejected");
        var nullTarget=nested.copy();
        try {nullTarget.put("target",null);throw new AssertionError("NBT accepted null value");}
        catch(IllegalArgumentException expected) { }
        require(check(nested,nullTarget,List.of(List.of(new NbtStoragePreservation.Key("target"))))==NbtStoragePreservation.Result.TARGET_PRESENT,"null target value was mistaken for absent key");
        require(before.getList("records",Tag.TAG_COMPOUND).size()==2&&before.contains("unknown_extension"),"verifier mutated baseline");
        System.out.println("NbtStoragePreservationSelfTest PASS (positions supplied by test; no ownership or write authority)");
    }
    private static NbtStoragePreservation.Result check(CompoundTag before,CompoundTag after,List<List<NbtStoragePreservation.Step>> paths) {
        return NbtStoragePreservation.verify(before,after,paths,4096,1024*1024);
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
