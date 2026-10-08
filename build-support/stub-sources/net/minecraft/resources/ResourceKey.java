package net.minecraft.resources;

/** Compile-only offline ABI stub; never packaged. */
public final class ResourceKey<T> {
    private final ResourceLocation location;

    public ResourceKey(ResourceLocation location) {
        this.location = location;
    }

    private ResourceKey(ResourceLocation registry, ResourceLocation location) {
        this(location);
    }

    public ResourceLocation location() { return location; }

    public static <T> ResourceKey<T> create(Object registry, ResourceLocation location) {
        return new ResourceKey<>(location);
    }
}
