package dev.ronova.coremod.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import dev.ronova.coremod.root.api.StorageCapacity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * World-local, checksummed and atomically replaced image for the 7.16 Root.
 * SavedData remains a downgrade mirror; a COMMITTED receipt is never based on
 * setDirty() alone.
 */
public final class DurableRootFileBackend {
    static final int MAX_PAYLOAD_BYTES = 64 * 1024 * 1024;
    private static final int MAGIC = 0x524F4E52; // RONR
    private static final int FORMAT_VERSION = 1;
    private static final int DIGEST_BYTES = 32;
    private static final int HEADER_BYTES = Integer.BYTES * 3 + DIGEST_BYTES;
    private static final String DIRECTORY = "ronova";
    private static final String FILE_NAME = "durable-root-v1.bin";
    private final Path target;
    private DurableWriteFailpoint failpoint = DurableWriteFailpoint.NONE;
    private StorageCapacity capacity = StorageCapacity.evaluate(MAX_PAYLOAD_BYTES, HEADER_BYTES, 0L, 0L, 0L, -1L);
    private Long usableBytesForTest;
    private dev.ronova.coremod.budget.HostResourceGovernor resources;
    public synchronized void attachRuntimeResources(dev.ronova.coremod.budget.HostResourceGovernor governor) {
        resources=Objects.requireNonNull(governor);
        resources.declareAccounting(dev.ronova.coremod.budget.HostResourceDomain.FILE_DESCRIPTOR,
            dev.ronova.coremod.budget.HostResourceGovernor.Accounting.PARTIAL_ESTIMATE,"root-file-channel/owned-handle-permit");
        if(isWindows())resources.declareAccounting(dev.ronova.coremod.budget.HostResourceDomain.PLATFORM_HANDLE,
            dev.ronova.coremod.budget.HostResourceGovernor.Accounting.PARTIAL_ESTIMATE,"root-file-channel/owned-handle-permit");
    }
    private dev.ronova.coremod.budget.HostResourceGovernor.Reservation filePermit() {
        if(resources==null)return null; // Bootstrap precedes runtime composition; do not claim historical accounting.
        var charges=new java.util.EnumMap<dev.ronova.coremod.budget.HostResourceDomain,Long>(dev.ronova.coremod.budget.HostResourceDomain.class);
        charges.put(dev.ronova.coremod.budget.HostResourceDomain.FILE_DESCRIPTOR,1L);
        if(isWindows())charges.put(dev.ronova.coremod.budget.HostResourceDomain.PLATFORM_HANDLE,1L);
        var permit=resources.reserve(charges,dev.ronova.coremod.budget.HostResourceGovernor.Priority.RECOVERY);
        if(!permit.granted())throw new DurableRootPersistenceException("Root file handle budget exhausted: "+permit.denial());
        return permit.reservation();
    }
    private static boolean isWindows(){return System.getProperty("os.name","").startsWith("Windows");}

    private DurableRootFileBackend(Path target) {
        this.target = Objects.requireNonNull(target, "target").toAbsolutePath().normalize();
    }

    static DurableRootFileBackend forServer(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        return new DurableRootFileBackend(worldRoot.resolve(DIRECTORY).resolve(FILE_NAME));
    }

    static DurableRootFileBackend at(Path target) {
        return new DurableRootFileBackend(target);
    }

    public synchronized boolean exists() {
        return Files.isRegularFile(target);
    }

    public synchronized CompoundTag readIfPresent() {
        if (!exists()) {
            if (!Files.notExists(target)) throw new DurableRootPersistenceException("durable Root path exists but is not a readable regular file");
            return null;
        }
        final long fileSize;
        try {
            fileSize = Files.size(target);
            if (fileSize < HEADER_BYTES
                || fileSize > HEADER_BYTES + (long) MAX_PAYLOAD_BYTES) {
                throw new DurableRootPersistenceException(
                    "durable Root file size is outside the bounded format"
                );
            }
        } catch (IOException failure) {
            throw failure("cannot read durable Root file", failure);
        }

        try (var permit=filePermit(); DataInputStream input = new DataInputStream(
            new BufferedInputStream(Files.newInputStream(target))
        )) {
            int magic = input.readInt();
            int version = input.readInt();
            int payloadLength = input.readInt();
            if (magic != MAGIC) {
                throw new DurableRootPersistenceException("durable Root magic mismatch");
            }
            if (version != FORMAT_VERSION) {
                throw new DurableRootPersistenceException(
                    "unsupported durable Root file version: " + version
                );
            }
            if (payloadLength < 0 || payloadLength > MAX_PAYLOAD_BYTES
                || fileSize != HEADER_BYTES + (long) payloadLength) {
                throw new DurableRootPersistenceException(
                    "durable Root payload length is invalid"
                );
            }
            byte[] expectedDigest = new byte[DIGEST_BYTES];
            input.readFully(expectedDigest);
            byte[] payload = new byte[payloadLength];
            input.readFully(payload);
            if (!MessageDigest.isEqual(expectedDigest, digest(payload))) {
                throw new DurableRootPersistenceException(
                    "durable Root payload checksum mismatch"
                );
            }
            CompoundTag root = NbtIo.read(new DataInputStream(
                new ByteArrayInputStream(payload)
            ));
            if (root == null) {
                throw new DurableRootPersistenceException("durable Root NBT is empty");
            }
            return root;
        } catch (DurableRootPersistenceException failure) {
            throw failure;
        } catch (IOException | RuntimeException failure) {
            throw failure("cannot decode durable Root file", failure);
        }
    }

    public synchronized void write(CompoundTag root) {
        write(prepare(root, 0L, false));
    }

    /** Serializes and checks capacity without changing the filesystem or accepting a new intent. */
    synchronized PreparedWrite prepare(CompoundTag root, long reservedGrowthBytes, boolean acceptingNew) {
        Objects.requireNonNull(root, "root");
        byte[] payload;
        try { payload = encode(root); }
        catch (DurableRootPersistenceException rejected) {
            capacity = new StorageCapacity(StorageCapacity.State.ENCODING_REJECTED, -1L,
                reservedGrowthBytes, capacity.ownedBytes(), capacity.usableBytes(), 0L, 0L);
            throw rejected;
        }
        long owned = 0L, reusableStaging = 0L, usable = -1L;
        try {
            if (Files.exists(target)) owned = Files.size(target);
            if (Files.exists(temporaryPath())) {
                long stagingSize = Files.size(temporaryPath());
                owned = Math.addExact(owned, stagingSize);
                if (Files.isRegularFile(temporaryPath())) reusableStaging = stagingSize;
            }
            Path existing = target.getParent();
            while (existing != null && !Files.exists(existing)) existing = existing.getParent();
            if (existing != null) usable = Files.getFileStore(existing).getUsableSpace();
        } catch (IOException | RuntimeException ignored) {
            usable = -1L;
        }
        if (usableBytesForTest != null) usable = usableBytesForTest;
        capacity = StorageCapacity.evaluate(MAX_PAYLOAD_BYTES, HEADER_BYTES, payload.length, reservedGrowthBytes, owned, reusableStaging, usable);
        if (!capacity.permits(acceptingNew)) throw new StorageCapacityException(capacity);
        return new PreparedWrite(this, payload, digest(payload));
    }

    synchronized void write(PreparedWrite prepared) {
        if (prepared.backend != this) throw new IllegalArgumentException("prepared image belongs to another backend");
        if (prepared.used) throw new IllegalStateException("prepared image is already consumed");
        prepared.used = true;
        byte[] payload = prepared.payload;
        byte[] checksum = prepared.checksum;
        Path parent = target.getParent();
        if (parent == null) {
            throw new DurableRootPersistenceException(
                "durable Root path has no parent directory"
            );
        }
        Path temporary = temporaryPath();
        try {
            failAt(DurableWriteFailpoint.BEFORE_WRITE);
            Files.createDirectories(parent);
            try (var permit=filePermit(); FileChannel channel = FileChannel.open(
                temporary,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            )) {
                ByteBuffer header = ByteBuffer.allocate(HEADER_BYTES);
                header.putInt(MAGIC);
                header.putInt(FORMAT_VERSION);
                header.putInt(payload.length);
                header.put(checksum);
                header.flip();
                writeFully(channel, header);
                writeFully(channel, ByteBuffer.wrap(payload));
                channel.force(true);
            }
            replace(temporary, target);
            forceDirectoryBestEffort(parent);
            failAt(DurableWriteFailpoint.AFTER_REPLACE_BEFORE_ACK);
        } catch (IOException failure) {
            throw failure("cannot replace durable Root file", failure);
        }
    }

    synchronized void armWriteFailpoint(DurableWriteFailpoint point) {
        failpoint = Objects.requireNonNull(point, "point");
    }

    synchronized void setUsableBytesForTest(Long value) { usableBytesForTest = value; }
    public synchronized StorageCapacity capacity() { return capacity; }

    static final class PreparedWrite {
        private final DurableRootFileBackend backend;
        private final byte[] payload;
        private final byte[] checksum;
        private boolean used;
        private PreparedWrite(DurableRootFileBackend backend, byte[] payload, byte[] checksum) {
            this.backend = backend;
            this.payload = payload;
            this.checksum = checksum;
        }
    }

    private void failAt(DurableWriteFailpoint point) throws IOException {
        if (failpoint == point) {
            failpoint = DurableWriteFailpoint.NONE;
            throw new IOException("injected durable write failure: " + point);
        }
    }

    Path target() {
        return target;
    }

    Path temporaryPath() {
        return target.resolveSibling(target.getFileName() + ".next");
    }

    private static byte[] encode(CompoundTag root) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(
            new BoundedOutputStream(bytes, MAX_PAYLOAD_BYTES)
        )) {
            NbtIo.write(root, output);
        } catch (IOException | RuntimeException failure) {
            throw failure("cannot encode durable Root NBT", failure);
        }
        return bytes.toByteArray();
    }

    private static byte[] digest(byte[] payload) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(payload);
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("SHA-256 is required by the JVM", impossible);
        }
    }

    private static void writeFully(FileChannel channel, ByteBuffer buffer)
        throws IOException {
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    private static void replace(Path temporary, Path target) throws IOException {
        // Atomic replacement is part of the commit contract. An unsupported
        // filesystem therefore yields COMMIT_UNKNOWN instead of a weaker move.
        Files.move(
            temporary, target,
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING
        );
    }

    /**
     * POSIX filesystems can force the directory entry. Some Windows providers
     * reject opening directories; the file itself has already been forced and
     * atomically replaced when this best-effort hardening is unavailable.
     */
    private void forceDirectoryBestEffort(Path directory) {
        try (var permit=filePermit(); FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (IOException | UnsupportedOperationException ignored) {
            // Cross-platform optional hardening; never weaken file force + replace.
        }
    }

    private static DurableRootPersistenceException failure(
        String message, Throwable cause
    ) {
        return new DurableRootPersistenceException(message + ": " + targetMessage(cause), cause);
    }

    private static String targetMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
            ? failure.getClass().getSimpleName() : message;
    }

    private static final class BoundedOutputStream extends FilterOutputStream {
        private final long limit;
        private long count;

        private BoundedOutputStream(OutputStream output, long limit) {
            super(output);
            this.limit = limit;
        }

        @Override
        public void write(int value) throws IOException {
            requireCapacity(1L);
            out.write(value);
            count++;
        }

        @Override
        public void write(byte[] values, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, values.length);
            requireCapacity(length);
            out.write(values, offset, length);
            count += length;
        }

        private void requireCapacity(long additional) throws IOException {
            if (additional < 0L || count > limit - additional) {
                throw new IOException("durable Root payload exceeds bounded size");
            }
        }
    }
}
