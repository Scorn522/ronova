package dev.ronova.coremod.capability.recovery;

import dev.ronova.coremod.capability.CapabilityContext;
import dev.ronova.coremod.capability.ServerCapabilityModule;
import dev.ronova.coremod.contract.Capability;
import java.util.ArrayList;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

public final class NegativeEffectCleanseModule implements ServerCapabilityModule {
    @Override public Capability capability() { return Capability.NEGATIVE_EFFECT_CLEANSE; }

    @Override public void repair(CapabilityContext context) {
        var player = context.player();
        context.attempt(() -> {
            for (Object value : new ArrayList<>(player.getActiveEffects())) {
                if (value instanceof MobEffectInstance effect
                    && effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                    player.removeEffect(effect.getEffect());
                }
            }
        });
    }
}
