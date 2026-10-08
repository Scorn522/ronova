package dev.ronova.coremod.client.tooltip;

import com.mojang.datafixers.util.Either;
import dev.ronova.coremod.RonovaDominion;
import dev.ronova.coremod.content.DominionContent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge-only tooltip integration; no GuiGraphics mixin is required. */
@Mod.EventBusSubscriber(
    modid = RonovaDominion.MOD_ID,
    value = Dist.CLIENT,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class RonovaTooltipEvents {
    private static GuiGraphics currentGraphics;

    private RonovaTooltipEvents() {}

    @SubscribeEvent
    public static void gather(RenderTooltipEvent.GatherComponents event) {
        if (!isBlade(event.getItemStack())) return;
        var elements = event.getTooltipElements();
        elements.clear();
        boolean expanded = Screen.hasShiftDown();
        for (TooltipComponent line : RonovaTooltipText.build(expanded)) {
            elements.add(Either.<FormattedText, TooltipComponent>right(line));
        }
        event.setMaxWidth(RonovaTooltipText.targetWidth(expanded));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void pre(RenderTooltipEvent.Pre event) {
        currentGraphics = isBlade(event.getItemStack()) ? event.getGraphics() : null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void color(RenderTooltipEvent.Color event) {
        if (!isBlade(event.getItemStack())) return;

        event.setBackgroundStart(0);
        event.setBackgroundEnd(0);
        event.setBorderStart(0);
        event.setBorderEnd(0);

        GuiGraphics graphics = currentGraphics;
        currentGraphics = null;
        if (graphics != null) {
            RonovaTooltipRenderer.render(
                graphics,
                event.getComponents(),
                event.getX(),
                event.getY(),
                event.getFont(),
                Screen.hasShiftDown()
            );
        }
    }

    private static boolean isBlade(ItemStack stack) {
        return !stack.isEmpty() && stack.is(DominionContent.RONOVA_BLADE.get());
    }
}
