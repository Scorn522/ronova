package net.minecraft.commands.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;

public class UuidArgument implements ArgumentType<UUID> {
    public static UuidArgument uuid() {
        return new UuidArgument();
    }

    public static UUID getUuid(CommandContext<CommandSourceStack> context, String name) {
        return null;
    }
}
