package dev.ronova.coremod.client.tooltip;

import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** Semantic control row with a stable key column and an optional second description row. */
public record RonovaTooltipControlData(
    Component input,
    Component action,
    Component description,
    boolean descriptionBelow
) implements TooltipComponent {
    public RonovaTooltipControlData {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(description, "description");
    }
}
