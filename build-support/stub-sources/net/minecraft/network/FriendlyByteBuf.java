package net.minecraft.network;

import java.util.Collection;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;

public class FriendlyByteBuf extends ByteBuf {
    @FunctionalInterface
    public interface Reader<T> extends Function<FriendlyByteBuf, T> {}

    @FunctionalInterface
    public interface Writer<T> extends BiConsumer<FriendlyByteBuf, T> {}

    public FriendlyByteBuf writeVarLong(long value) { return this; }
    public long readVarLong() { return 0L; }
    public FriendlyByteBuf writeVarInt(int value) { return this; }
    public int readVarInt() { return 0; }
    public <E extends Enum<E>> FriendlyByteBuf writeEnum(E value) { return this; }
    public <E extends Enum<E>> E readEnum(Class<E> type) { return null; }
    public FriendlyByteBuf writeUUID(UUID value) { return this; }
    public UUID readUUID() { return null; }
    public ByteBuf writeBoolean(boolean value) { return this; }
    public boolean readBoolean() { return false; }
    public FriendlyByteBuf writeUtf(String value) { return this; }
    public String readUtf() { return null; }
    public ByteBuf writeDouble(double value) { return this; }
    public double readDouble() { return 0.0D; }
    public FriendlyByteBuf writeBlockPos(BlockPos value) { return this; }
    public BlockPos readBlockPos() { return null; }

    public <T> void writeCollection(Collection<T> values, Writer<T> writer) {}

    public <T, C extends Collection<T>> C readCollection(
        IntFunction<C> factory,
        Reader<T> reader
    ) {
        return null;
    }
}
