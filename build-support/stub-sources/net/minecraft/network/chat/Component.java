package net.minecraft.network.chat;

public interface Component {
    static MutableComponent literal(String value) {
        return null;
    }

    static MutableComponent translatable(String key, Object... arguments) {
        return null;
    }
}
