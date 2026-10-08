package net.minecraft.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Compile-only offline ABI stub; never packaged. */
public class CommandSourceStack {
    public boolean hasPermission(int level) { return true; }

    public ServerPlayer getPlayerOrException() throws CommandSyntaxException {
        return new ServerPlayer();
    }

    public MinecraftServer getServer() { return new MinecraftServer(); }

    public void sendSuccess(Supplier<Component> message, boolean broadcast) {}

    public void sendFailure(Component message) {}
}
