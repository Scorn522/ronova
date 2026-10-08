package dev.ronova.coremod.mixin;

import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.observation.StorageMutationSequence;
import dev.ronova.coremod.observation.StorageMutationTracked;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Unique;

/** Invalidate old observed bindings before the registered storage replacement entry runs. */
@Mixin(DimensionDataStorage.class)
public abstract class DimensionDataStorageRecoveryMixin implements StorageMutationTracked {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private java.io.File dataFolder;
    @Override public final java.nio.file.Path ronova$observedDataFile(String name) {
        if(dataFolder==null||dataFolder.getClass()!=java.io.File.class)return null;
        if(name==null||name.isEmpty()||name.length()>512||name.indexOf('/')>=0||name.indexOf('\\')>=0||name.indexOf(':')>=0
            ||name.chars().anyMatch(Character::isISOControl))return null;
        var folder=dataFolder.toPath().toAbsolutePath().normalize();var file=folder.resolve(name+".dat").normalize();
        return file.getParent().equals(folder)?file:null;
    }
    @Unique private final StorageMutationSequence ronova$cacheMutations = new StorageMutationSequence();
    @Override public final long ronova$cacheObservationVersion() {
        return ronova$cacheMutations == null ? -1 : ronova$cacheMutations.observationVersion();
    }
    @Redirect(method = {
        "set(Ljava/lang/String;Lnet/minecraft/world/level/saveddata/SavedData;)V",
        "get(Ljava/util/function/Function;Ljava/lang/String;)Lnet/minecraft/world/level/saveddata/SavedData;"
    }, at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object ronova$replacingSavedData(java.util.Map<Object, Object> cache, Object key, Object value) {
        ronova$cacheMutations.begin();
        try {
            if (key instanceof String name) ServerRuntimeRegistry.savedDataReplacing((DimensionDataStorage) (Object) this, name);
            return cache.put(key, value);
        } finally { ronova$cacheMutations.end(); }
    }
}
