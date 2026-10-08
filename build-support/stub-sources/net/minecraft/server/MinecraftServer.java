package net.minecraft.server;

import java.nio.file.Path;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

/** Compile-only offline ABI stub; never packaged. */
public class MinecraftServer {
    private final ServerLevel overworld = new ServerLevel(this);

    public ServerLevel overworld() {
        return overworld;
    }

    public int getTickCount() {
        return 0;
    }

    public ServerLevel getLevel(ResourceKey<Level> dimension) {
        return overworld;
    }

    public Path getWorldPath(LevelResource resource) {
        return Path.of(".");
    }
}
