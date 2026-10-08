package net.minecraft.nbt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/** Compile/test NBT stream stub. It is never packaged into Ronova. */
public final class NbtIo {
    private NbtIo() {}

    public static void write(CompoundTag root, DataOutput output) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream objects = new ObjectOutputStream(bytes)) {
            objects.writeObject(root);
        }
        byte[] payload = bytes.toByteArray();
        output.writeInt(payload.length);
        output.write(payload);
    }

    public static CompoundTag read(DataInput input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > 64 * 1024 * 1024) {
            throw new IOException("stub NBT payload length is invalid");
        }
        byte[] payload = new byte[length];
        input.readFully(payload);
        try (ObjectInputStream objects = new ObjectInputStream(
            new ByteArrayInputStream(payload)
        )) {
            Object value = objects.readObject();
            if (!(value instanceof CompoundTag compound)) {
                throw new IOException("stub NBT payload is not a CompoundTag");
            }
            return compound;
        } catch (ClassNotFoundException failure) {
            throw new IOException("stub NBT class is unavailable", failure);
        }
    }
}
