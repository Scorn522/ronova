package net.minecraft.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

public final class Commands {
    private Commands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return new LiteralArgumentBuilder<>();
    }

    public static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(
        String name,
        ArgumentType<T> type
    ) {
        return new RequiredArgumentBuilder<>();
    }
}
