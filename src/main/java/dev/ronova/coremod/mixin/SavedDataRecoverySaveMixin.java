package dev.ronova.coremod.mixin;

import dev.ronova.coremod.observation.SavedDataSaveTrace;
import dev.ronova.coremod.observation.SavedDataSaveTracked;
import java.io.File;
import java.io.IOException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.saveddata.SavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe the actual vanilla serializer/write calls without changing its exception or dirty semantics. */
@Mixin(SavedData.class)
public abstract class SavedDataRecoverySaveMixin implements SavedDataSaveTracked {
    @Unique private final java.util.concurrent.atomic.AtomicReference<SavedDataSaveTrace> ronova$saveTrace = new java.util.concurrent.atomic.AtomicReference<>();
    @Override public final SavedDataSaveTrace ronova$saveTrace() {
        var trace = ronova$saveTrace.get();
        if (trace == null) {
            var created = new SavedDataSaveTrace();
            trace = ronova$saveTrace.compareAndSet(null, created) ? created : ronova$saveTrace.get();
        }
        return trace;
    }
    @Inject(method = "save(Ljava/io/File;)V", at = @At("HEAD"))
    private void ronova$beginSave(File file, CallbackInfo callback) {
        var trace = ronova$saveTrace.get(); if (trace != null) trace.begin();
    }
    @Inject(method = "save(Ljava/io/File;)V", at = @At("RETURN"))
    private void ronova$endSave(File file, CallbackInfo callback) {
        var trace = ronova$saveTrace.get(); if (trace != null) trace.end(true);
    }
    @Redirect(method = "save(Ljava/io/File;)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/saveddata/SavedData;save(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;"))
    private CompoundTag ronova$serialize(SavedData owner, CompoundTag input) {
        var trace = ronova$saveTrace.get();
        try {
            CompoundTag result = owner.save(input);
            if (trace != null) trace.serialized();
            return result;
        } catch (RuntimeException | Error failure) {
            if (trace != null) trace.end(false);
            throw failure;
        }
    }
    @Unique private static String ronova$observedPath(File file) {
        if(file==null||file.getClass()!=File.class)return null;
        try {return file.toPath().toAbsolutePath().normalize().toString();}
        catch(RuntimeException unavailable){return null;}
    }
    @Redirect(method = "save(Ljava/io/File;)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/nbt/NbtIo;writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/io/File;)V"))
    private void ronova$write(CompoundTag wrapper, File file) throws IOException {
        var trace = ronova$saveTrace.get();
        if (trace != null) {
            trace.writing(ronova$observedPath(file));
            try {dev.ronova.coremod.bootstrap.ServerRuntimeRegistry.savedDataEncoding((SavedData)(Object)this,wrapper);}
            catch(RuntimeException|LinkageError unavailable) {
                // A capture failure must not suppress the real save or replay its serializer.
            }
        }
        try (var encoded=dev.ronova.coremod.observation.SavedDataEncodedWrite.open(trace,wrapper,this)) {
            NbtIo.writeCompressed(wrapper, file);
            if(encoded!=null)encoded.succeeded();
            if (trace != null) trace.wrote();
        } catch (IOException failure) {
            if (trace != null) trace.failed();
            throw failure;
        } catch (RuntimeException | Error failure) {
            if (trace != null) trace.end(false);
            throw failure;
        }
    }
}
