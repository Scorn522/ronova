package net.minecraft.core.registries;

import net.minecraft.resources.ResourceLocation;

/** Compile-only offline ABI stub; never packaged. */
public final class BuiltInRegistries {
    public static final Registry ENTITY_TYPE = new Registry();

    private BuiltInRegistries() {}

    public static final class Registry {
        public ResourceLocation getKey(Object value) {
            return new ResourceLocation("minecraft:stub");
        }
    }
}
