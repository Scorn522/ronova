package dev.ronova.coremod.client.tooltip;

import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** One explicitly laid-out tooltip row; converted to a client component on the MOD bus. */
public record RonovaTooltipLineData(
    Component text,
    Kind kind,
    int width,
    int height,
    int decorationArgb
) implements TooltipComponent {
    public RonovaTooltipLineData {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(kind, "kind");
        if (width < 0) throw new IllegalArgumentException("width must be non-negative");
        if (height < 1) throw new IllegalArgumentException("height must be positive");
    }

    public static RonovaTooltipLineData text(Component text, int height) {
        return new RonovaTooltipLineData(text, Kind.TEXT, 0, height, 0);
    }

    public static RonovaTooltipLineData spacer(int height) {
        return new RonovaTooltipLineData(Component.empty(), Kind.SPACER, 0, height, 0);
    }

    public static RonovaTooltipLineData divider(int width) {
        return new RonovaTooltipLineData(
            Component.empty(), Kind.DIVIDER, width, 7, 0xA06B3040);
    }

    public enum Kind {
        TEXT,
        SPACER,
        DIVIDER
    }
}
