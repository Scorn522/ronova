package dev.ronova.coremod.persistence;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.GZIPInputStream;
import net.minecraft.nbt.*;

/** Bounded reads and preservation of disposable SavedData mirrors, never a Root writer. */
final class SavedDataMirrorFiles {
    static final String LEDGER = "ronova_ledger";
    static final String SHADOW = "ronova_durable_root_shadow";
    static final String MIRROR_ONLY = "durable_root_mirror";
    private static final long MAX_BYTES = 64L * 1024L * 1024L;
    private static final int MAX_BACKUPS = 128;
    private SavedDataMirrorFiles() {}

    static Path path(Path world, String name) { return world.resolve("data").resolve(name + ".dat"); }

    static CompoundTag read(Path path) throws IOException {
        if (Files.notExists(path)) return null;
        if (!Files.isRegularFile(path) || Files.size(path) > MAX_BYTES) throw new IOException("invalid mirror file: " + path);
        try (PushbackInputStream raw = new PushbackInputStream(Files.newInputStream(path), 2)) {
            byte[] prefix = raw.readNBytes(2);
            raw.unread(prefix);
            InputStream decoded = prefix.length == 2 && (prefix[0] & 255) == 31 && (prefix[1] & 255) == 139
                ? new GZIPInputStream(raw) : raw;
            try (DataInputStream input = new DataInputStream(decoded)) {
                CompoundTag wrapper = NbtIo.read(input, new NbtAccounter(MAX_BYTES));
                if (wrapper == null || !wrapper.contains("data", Tag.TAG_COMPOUND) || input.read() != -1) {
                    throw new IOException("invalid SavedData wrapper: " + path);
                }
                return wrapper.getCompound("data");
            }
        } catch (RuntimeException malformed) { throw new IOException("invalid SavedData mirror: " + path, malformed); }
    }

    /** Existing bytes are preserved before Minecraft can overwrite either mirror. */
    static void preserve(Path world, Path source) throws IOException {
        if (Files.notExists(source)) return;
        if (!Files.isRegularFile(source) || Files.size(source) > MAX_BYTES) throw new IOException("mirror cannot be safely backed up");
        Path backups = world.resolve("ronova").resolve("mirror-backups");
        byte[] bytes = Files.readAllBytes(source);
        String hash;
        try { hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        Path target = backups.resolve(source.getFileName() + "." + hash + ".bak");
        if (Files.exists(target)) {
            if (!java.util.Arrays.equals(bytes, Files.readAllBytes(target))) throw new IOException("mirror backup differs from its digest");
            return;
        }
        long used = 0L; int count = 0;
        if (Files.exists(backups)) {
            try (var files = Files.newDirectoryStream(backups)) {
                for (Path file : files) {
                    if (!Files.isRegularFile(file)) throw new IOException("unexpected mirror backup entry");
                    used = Math.addExact(used, Files.size(file));
                    count++;
                }
            }
        }
        if (count >= MAX_BACKUPS || bytes.length > MAX_BYTES - used) throw new IOException("mirror backup capacity reached");
        Files.createDirectories(backups);
        try (var channel = java.nio.channels.FileChannel.open(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
    }
}
