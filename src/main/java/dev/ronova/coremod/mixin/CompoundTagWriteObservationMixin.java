package dev.ronova.coremod.mixin;

import java.io.*;
import net.minecraft.nbt.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(CompoundTag.class)
public abstract class CompoundTagWriteObservationMixin {
    @Shadow private static void writeNamedTag(String name,Tag tag,DataOutput output)throws IOException {throw new AssertionError();}
    @Redirect(method="write",at=@At(value="INVOKE",target="Lnet/minecraft/nbt/CompoundTag;writeNamedTag(Ljava/lang/String;Lnet/minecraft/nbt/Tag;Ljava/io/DataOutput;)V"))
    private void ronova$writeReference(String name,Tag child,DataOutput output)throws IOException {
        var scope=dev.ronova.coremod.observation.SavedDataEncodedWrite.enterReference(this,child,name);
        try {writeNamedTag(name,child,output);}finally {dev.ronova.coremod.observation.SavedDataEncodedWrite.leaveReference(scope);}
    }
}
