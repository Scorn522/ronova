package dev.ronova.coremod.client;

import dev.ronova.coremod.authority.AuthorityIdentity;
import dev.ronova.coremod.mixin.ClientLevelAccessor;
import dev.ronova.coremod.mixin.EntityStateAccessor;
import dev.ronova.coremod.mixin.LivingEntityStateAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;

/** Client lifecycle repair and authority-surface ownership. */
public final class ClientRecoveryController {
    private final ClientAuthorityState authority = new ClientAuthorityState();
    private final ClientRecoveryGuard guard = new ClientRecoveryGuard();

    public boolean recoverIfInvalid(Minecraft minecraft) {
        if (!hasProtectedLocalPlayer(minecraft)) {
            return false;
        }
        LocalPlayer player = minecraft.player;
        boolean invalid = isInvalid(player);
        if (invalid) {
            authority.arm();
            if (guard.enterRepair()) {
                try {
                    repairState(minecraft, player);
                } finally {
                    guard.exitRepair();
                }
            }
        }
        return invalid;
    }

    public boolean shouldRejectScreen(Minecraft minecraft, Screen requested) {
        if (guard.isInternalSurfaceMutation() || !hasProtectedLocalPlayer(minecraft)) {
            return false;
        }
        if (requested instanceof DeathScreen) {
            authority.arm();
            return true;
        }
        return requested != null && requested != minecraft.screen
            && !(requested instanceof RonovaControlScreen) && !(requested instanceof RonovaStrengthScreen) && authority.isActive();
    }

    public boolean shouldRejectOverlay(Minecraft minecraft, Overlay requested) {
        return !guard.isInternalSurfaceMutation() && hasProtectedLocalPlayer(minecraft)
            && requested != null && authority.isActive();
    }

    public boolean isAuthorityRecoveryActive() {
        Minecraft minecraft = Minecraft.getInstance();
        return hasProtectedLocalPlayer(minecraft) && authority.isActive()
            && !guard.isInternalSurfaceMutation();
    }

    public void maintainAuthoritySurface(Minecraft minecraft) {
        if (!hasProtectedLocalPlayer(minecraft) || !authority.isActive()
            || !guard.enterSurfaceMutation()) {
            return;
        }
        try {
            boolean changed = false;
            if (minecraft.screen != null && !(minecraft.screen instanceof RonovaControlScreen) && !(minecraft.screen instanceof RonovaStrengthScreen)) {
                minecraft.setScreen(null);
                changed = true;
            }
            if (minecraft.getOverlay() != null) {
                minecraft.setOverlay(null);
                changed = true;
            }
            if (changed) {
                KeyMapping.setAll();
            }
            if (minecraft.screen == null && !minecraft.mouseHandler.isMouseGrabbed()) {
                minecraft.mouseHandler.grabMouse();
            }
        } finally {
            guard.exitSurfaceMutation();
        }
    }

    public void observeProtectedViolation(Object value) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.player != null && value == minecraft.player
            && AuthorityIdentity.contains(minecraft.player.getUUID())) {
            authority.arm();
        }
    }

    public void reset() {
        authority.reset();
        guard.reset();
    }

    private static void repairState(Minecraft minecraft, LocalPlayer player) {
        boolean invalid = isInvalid(player);
        restoreClientLifecycle(player);
        restoreClientVitals(player);
        restoreClientAbilities(player);
        if (invalid) {
            restoreClientTopology(minecraft, player);
        }
    }

    private static void restoreClientLifecycle(LocalPlayer player) {
        try {
            if (player.isRemoved()) {
                player.revive();
            }
        } catch (Throwable ignored) {
            // Raw state is restored independently.
        }
        try {
            EntityStateAccessor state = (EntityStateAccessor) player;
            state.ronova$setRemovalReason(null);
            state.ronova$setAddedToWorld(true);
        } catch (Throwable ignored) {
            // Client topology recovery remains independent.
        }
        try {
            ((LivingEntityStateAccessor) player).ronova$setDead(false);
            player.deathTime = 0;
            player.hurtTime = 0;
        } catch (Throwable ignored) {
            // Vitals and topology recovery continue.
        }
    }

    private static void restoreClientVitals(LocalPlayer player) {
        float health;
        try {
            health = Math.max(20.0F, player.getMaxHealth());
        } catch (Throwable ignored) {
            health = 20.0F;
        }
        try {
            player.setHealth(health);
        } catch (Throwable ignored) {
            // Synchronized state is written independently.
        }
        try {
            player.getEntityData().set(
                LivingEntityStateAccessor.ronova$getHealthDataAccessor(), health
            );
        } catch (Throwable ignored) {
            // Frame/tick recovery retries without blocking controls.
        }
    }

    private static void restoreClientAbilities(LocalPlayer player) {
        try {
            Abilities abilities = player.getAbilities();
            abilities.invulnerable = true;
            abilities.mayfly = true;
            abilities.flying = true;
        } catch (Throwable ignored) {
            // Movement recovery is retried on tick and frame lanes.
        }
    }

    private static void restoreClientTopology(Minecraft minecraft, LocalPlayer player) {
        if (minecraft.level == null) {
            return;
        }
        try {
            if (minecraft.level.getEntity(player.getId()) != player) {
                minecraft.level.addPlayer(player.getId(), player);
            }
        } catch (Throwable ignored) {
            // The entity-manager sink below remains available.
        }
        try {
            if (minecraft.level.getEntity(player.getId()) != player) {
                ((ClientLevelAccessor) minecraft.level).ronova$getEntityStorage().addEntity(player);
                player.onAddedToWorld();
            }
        } catch (Throwable ignored) {
            // Repeated frame/tick recovery retries this bounded operation.
        }
    }

    private static boolean hasProtectedLocalPlayer(Minecraft minecraft) {
        return minecraft != null && minecraft.player != null
            && AuthorityIdentity.contains(minecraft.player.getUUID());
    }

    private static boolean isInvalid(LocalPlayer player) {
        return player.isRemoved() || !player.isAddedToWorld()
            || player.isDeadOrDying() || player.getHealth() <= 0.0F;
    }
}
