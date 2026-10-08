package dev.ronova.coremod.mixin;

import java.util.List;
import java.util.Map;
import net.minecraft.util.ClassInstanceMultiMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClassInstanceMultiMap.class)
public interface ClassInstanceMultiMapAccessor {
    @Accessor("allInstances") List<?> ronova$getAllInstances();
    @Accessor("byClass") Map<Class<?>, List<?>> ronova$getByClass();
}
