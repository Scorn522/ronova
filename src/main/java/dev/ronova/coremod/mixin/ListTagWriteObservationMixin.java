package dev.ronova.coremod.mixin;

import java.io.*;
import net.minecraft.nbt.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ListTag.class)
public abstract class ListTagWriteObservationMixin {
    @Redirect(method="write",at=@At(value="INVOKE",target="Lnet/minecraft/nbt/Tag;write(Ljava/io/DataOutput;)V"))
    private void ronova$writeReference(Tag child,DataOutput output)throws IOException {
        var scope=dev.ronova.coremod.observation.SavedDataEncodedWrite.enterReference(this,child,null);
        try {child.write(output);}finally {dev.ronova.coremod.observation.SavedDataEncodedWrite.leaveReference(scope);}
    }
}
