package dev.ronova.coremod.mixin;

import dev.ronova.coremod.observation.SavedDataEncodedWrite;
import java.io.*;
import net.minecraft.nbt.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(NbtIo.class)
public abstract class NbtCompressedWriteObservationMixin {
    @Redirect(method="writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/io/File;)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/nbt/NbtIo;writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/io/OutputStream;)V"))
    private static void ronova$observeCompressedBytes(CompoundTag image,OutputStream output)throws IOException {
        NbtIo.writeCompressed(image,SavedDataEncodedWrite.observe(image,output));
    }
}
