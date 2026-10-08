package net.minecraft.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;

/** Compile-only offline ABI stub; never packaged. */
public final class Minecraft {
    public Level level;
    public LocalPlayer player;
    public Screen screen;

    public static Minecraft getInstance() { return null; }
    public void setScreen(Screen value) { screen = value; }
}
