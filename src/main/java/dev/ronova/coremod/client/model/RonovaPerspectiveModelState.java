package dev.ronova.coremod.client.model;

import com.mojang.math.Transformation;
import java.util.Map;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.world.item.ItemDisplayContext;

/** Immutable per-display-context transform table used by the wrapped item model. */
public final class RonovaPerspectiveModelState implements ModelState {
    public static final RonovaPerspectiveModelState IDENTITY =
        new RonovaPerspectiveModelState(Map.of());

    private final Map<ItemDisplayContext, Transformation> transforms;
    private final boolean uvLocked;

    public RonovaPerspectiveModelState(Map<ItemDisplayContext, Transformation> transforms) {
        this(transforms, false);
    }

    public RonovaPerspectiveModelState(
        Map<ItemDisplayContext, Transformation> transforms,
        boolean uvLocked
    ) {
        this.transforms = Map.copyOf(transforms);
        this.uvLocked = uvLocked;
    }

    public Transformation getTransform(ItemDisplayContext context) {
        return transforms.getOrDefault(context, Transformation.identity());
    }

    @Override
    public boolean isUvLocked() {
        return uvLocked;
    }
}
