package net.minecraft.resources;

/** Compile-only offline ABI stub; never packaged. */
public record ResourceLocation(String value) {
    public ResourceLocation(String namespace, String path) {
        this((namespace == null ? "" : namespace) + ":" + (path == null ? "" : path));
    }

    public static ResourceLocation fromNamespaceAndPath(
        String namespace,
        String path
    ) {
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation tryParse(String value) {
        return value == null || value.isBlank() ? null : new ResourceLocation(value);
    }

    @Override public String toString() { return value == null ? "unknown:unknown" : value; }
}
