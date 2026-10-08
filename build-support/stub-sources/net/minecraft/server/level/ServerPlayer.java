package net.minecraft.server.level;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Compile-only offline ABI stub; never packaged. */
public class ServerPlayer extends Player {
    public MinecraftServer server;

    public Inventory getInventory() { return new Inventory(); }
    public Vec3 getEyePosition() { return new Vec3(0.0D, 0.0D, 0.0D); }
    public Vec3 getLookAngle() { return new Vec3(0.0D, 0.0D, 1.0D); }
    public ServerLevel serverLevel() { return server == null ? null : server.overworld(); }
    public ItemStack getMainHandItem() { return new ItemStack(); }
    public ItemStack getOffhandItem() { return new ItemStack(); }
    public double getEyeY() { return getY(); }
    public GameProfile getGameProfile() { return new GameProfile(); }
}
