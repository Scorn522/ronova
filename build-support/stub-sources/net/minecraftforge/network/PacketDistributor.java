package net.minecraftforge.network;

import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;

/** Compile-only offline ABI stub; never packaged. */
public final class PacketDistributor<T> {
    public static final PacketDistributor<ServerPlayer> PLAYER =
        new PacketDistributor<>();

    private PacketDistributor() {}

    public PacketTarget with(Supplier<T> value) {
        return null;
    }

    public interface PacketTarget {}
}
