package net.minecraft.server.level;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.Level;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Compile-only offline ABI stub; never packaged. */
public class ServerLevel extends Level {
    private final MinecraftServer server;
    private final DimensionDataStorage storage = new DimensionDataStorage();

    public ServerLevel(MinecraftServer server) {
        this.server = server;
    }

    public DimensionDataStorage getDataStorage() {
        return storage;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public Entity getEntity(UUID uuid) {
        return null;
    }

    public BlockHitResult clip(ClipContext context) { return new BlockHitResult(); }

    public BlockState getBlockState(BlockPos position) { return new BlockState(); }

    public BlockEntity getBlockEntity(BlockPos position) { return null; }

    public void removeBlockEntity(BlockPos position) {}

    public boolean setBlock(BlockPos position, BlockState state, int flags) { return true; }

    public <T extends ParticleOptions> int sendParticles(
        T particle,
        double x,
        double y,
        double z,
        int count,
        double offsetX,
        double offsetY,
        double offsetZ,
        double speed
    ) { return count; }
}
