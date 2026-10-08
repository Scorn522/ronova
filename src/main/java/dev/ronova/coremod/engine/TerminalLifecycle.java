package dev.ronova.coremod.engine;

import dev.ronova.coremod.authority.AuthorityView;
import dev.ronova.coremod.baseline.BaselineTelemetry;
import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.mixin.EntityStateAccessor;
import dev.ronova.coremod.mixin.LivingEntityStateAccessor;
import dev.ronova.coremod.runtime.DominionServerRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.phys.Vec3;

/**
 * Ordered, failure-isolated terminal lifecycle. Public lifecycle calls perform
 * normal death integration once; raw state is then asserted independently so
 * one hostile override cannot abort the remaining lanes.
 */
public final class TerminalLifecycle {
    private TerminalLifecycle() {}

    /** Only registered raw fields; it does not assert index absence or whole-operation completion. */
    public static boolean controlledStatePresent(Entity entity) {
        return entity != null && TerminalStateSink.matches(entity);
    }

    /** Compatibility entry point; server entities obtain the composed read view. */
    public static void enforce(Entity entity, boolean processDeathOnce) {
        AuthorityView view=AuthorityView.client();
        if(entity!=null&&entity.level() instanceof ServerLevel level) {
            var runtime=ServerRuntimeRegistry.getIfPresent(level.getServer());
            if(runtime==null)return;
            view=runtime.authorityView();
        }
        enforce(entity, processDeathOnce, view);
    }

    public static void enforce(
        Entity entity,
        boolean processDeathOnce,
        AuthorityView authorityView
    ) {
        if (entity != null && entity.level() instanceof ServerLevel level) {
            var runtime = ServerRuntimeRegistry.getIfPresent(level.getServer());
            if (runtime != null && runtime.durableStore() != null) {
                var current = runtime.durableStore().subjectQualification(new dev.ronova.coremod.contract.SubjectKey(runtime.durableStore().realmId(), entity.getUUID()));
                // A changed/unjudged UUID must not fall through into the legacy physical writer.
                if (current == null || !current.terminal()) return;
                if (current.revisions() != null) {
                    runtime.terminalPlans().execute(entity, authorityView); return;
                }
            }
            // Server mutations always need a current Root plan. Missing runtime or
            // legacy unbound qualifications cannot re-enable the old unrestricted writer.
            return;
        }
        enforceLegacyBody(entity, processDeathOnce, authorityView, false, () -> true);
    }

    static void runPlannedIntegration(Entity entity, AuthorityView view, java.util.function.BooleanSupplier current) {
        enforceLegacyBody(entity, true, view, true, current);
    }

    private static void enforceLegacyBody(Entity entity, boolean processDeathOnce, AuthorityView authorityView, boolean planned,
        java.util.function.BooleanSupplier current) {
        if (entity == null || authorityView == null || authorityView.isProtected(entity)) {
            return;
        }
        if (!current.getAsBoolean()) return;
        BaselineTelemetry.recordTerminalInvocation(entity);
        boolean firstExecution = JudgmentState.mark(entity);
        if (firstExecution && !planned) {
            prepare(entity, current);
        }
        if (entity instanceof LivingEntity living) {
            if (!current.getAsBoolean()) return;
            if (firstExecution && !planned) {
                clearEffects(living);
            }
            if (processDeathOnce && firstExecution) {
                if (!current.getAsBoolean()) return;
                if (planned) {
                    try { invokePublicDeath(living, living.damageSources().fellOutOfWorld(), current, false); }
                    catch (RuntimeException failed) { /* The encompassing Root effect remains unknown. */ }
                } else integrateDeathOnce(living, entity);
            }
            if (!current.getAsBoolean()) return;
            if (!planned) writeLivingTerminal(living, current);
        }
        if (firstExecution) {
            // The planned path has one death entry and one removal entry. kill() can
            // dispatch hurt/die again; discard() can dispatch remove again.
            if (!planned) {
                if (!current.getAsBoolean()) return;
                invokeKill(entity);
                if (!current.getAsBoolean()) return;
                invokeDiscard(entity);
            }
            if (!current.getAsBoolean()) return;
            invokeRemove(entity);
        }
        if (!current.getAsBoolean()) return;
        if (!planned) writeEntityTerminal(entity, current);
    }

    /**
     * Completes a Root-committed terminal transition.  The durable lifecycle
     * claim is written before public death integration. Recovery never replays
     * that opaque death entry for an existing or uncertain claim. This does not
     * prove its effect completed, nor cover the other legacy physical lanes.
     */
    public static void completeCommitted(Entity entity, AuthorityView authorityView) {
        enforce(entity, true, authorityView);
    }

    private static void prepare(Entity entity, java.util.function.BooleanSupplier current) {
        try {
            if (!current.getAsBoolean()) return;
            entity.setInvulnerable(false);
            if (!current.getAsBoolean()) return;
            entity.stopRiding();
            if (!current.getAsBoolean()) return;
            entity.ejectPassengers();
            if (!current.getAsBoolean()) return;
            entity.setDeltaMovement(Vec3.ZERO);
        } catch (Throwable ignored) {
            // Terminal raw-state lanes below do not depend on preparation.
        }
    }

    private static void clearEffects(LivingEntity living) {
        try {
            living.removeAllEffects();
        } catch (Throwable ignored) {
            // Effects cannot veto terminal entity state.
        }
    }

    private static String physicalIdentity(Entity entity) {
        return entity.getUUID() + "#" + entity.getId() + "@"
            + Integer.toHexString(System.identityHashCode(entity));
    }

    private static void integrateDeathOnce(LivingEntity living, Entity entity) {
        DamageSource source;
        try {
            source = living.damageSources().fellOutOfWorld();
        } catch (Throwable ignored) {
            return;
        }
        if (entity.level() instanceof ServerLevel level) {
            try {
                DominionServerRuntime runtime = ServerRuntimeRegistry.runtime(level.getServer());
                runtime.integrateLifecycleOnce(entity.getUUID(), physicalIdentity(entity),
                    () -> invokePublicDeath(living, source, () -> true));
            } catch (Throwable ignored) {
                // No acknowledged intent/entry means no callback; raw terminal maintenance continues.
            }
        } else {
            invokePublicDeath(living, source, () -> true);
        }
    }

    private static void invokePublicDeath(LivingEntity living, DamageSource source, java.util.function.BooleanSupplier current) {
        invokePublicDeath(living,source,current,true);
    }
    private static void invokePublicDeath(LivingEntity living, DamageSource source, java.util.function.BooleanSupplier current,boolean prime) {
        if (!current.getAsBoolean()) return;
        if(prime)primeDeathIntegration(living, current);
        if (!current.getAsBoolean()) return;
        try {
            // Exactly one public death entry avoids duplicate loot when a
            // hostile hurt override performs side effects but returns false.
            living.die(source);
        } catch (Throwable ignored) {
            // Raw terminal state remains authoritative.
        }
        if (living instanceof ServerPlayer player) {
            try {
                if (!current.getAsBoolean()) return;
                player.connection.send(new ClientboundPlayerCombatKillPacket(
                    player.getId(),
                    Component.translatable("death.attack.generic", player.getDisplayName())
                ));
            } catch (Throwable ignored) {
                // Visual notification is not part of execution authority.
            }
        }
    }

    private static void primeDeathIntegration(LivingEntity living, java.util.function.BooleanSupplier current) {
        try {
            LivingEntityStateAccessor state = (LivingEntityStateAccessor) living;
            if (!current.getAsBoolean()) return;
            state.ronova$setDead(false);
            if (!current.getAsBoolean()) return;
            living.deathTime = 0;
            if (!current.getAsBoolean()) return;
            living.getEntityData().set(
                LivingEntityStateAccessor.ronova$getHealthDataAccessor(), 1.0F
            );
        } catch (Throwable ignored) {
            // The public hurt/die call may still integrate without priming.
        }
    }

    private static void writeLivingTerminal(LivingEntity living, java.util.function.BooleanSupplier current) {
        try {
            if (!current.getAsBoolean()) return;
            living.setHealth(0.0F);
        } catch (Throwable ignored) {
            // Synchronized and direct fields are asserted separately.
        }
        try {
            LivingEntityStateAccessor state = (LivingEntityStateAccessor) living;
            if (!current.getAsBoolean()) return;
            state.ronova$setDead(true);
            if (!current.getAsBoolean()) return;
            living.deathTime = Math.max(20, living.deathTime);
            if (!current.getAsBoolean()) return;
            living.getEntityData().set(
                LivingEntityStateAccessor.ronova$getHealthDataAccessor(), 0.0F
            );
        } catch (Throwable ignored) {
            // Entity removal is an independent terminal lane.
        }
    }

    private static void invokeKill(Entity entity) {
        try {
            entity.kill();
        } catch (Throwable ignored) {
            // Continue with discard/remove and raw state.
        }
    }

    private static void invokeDiscard(Entity entity) {
        try {
            entity.discard();
        } catch (Throwable ignored) {
            // Continue with remove and raw state.
        }
    }

    private static void invokeRemove(Entity entity) {
        try {
            entity.remove(RemovalReason.KILLED);
        } catch (Throwable ignored) {
            // Raw state remains authoritative.
        }
    }

    private static void writeEntityTerminal(Entity entity, java.util.function.BooleanSupplier current) {
        notifyRemoval(entity, current);
        try {
            EntityStateAccessor state = (EntityStateAccessor) entity;
            if (!current.getAsBoolean()) return;
            state.ronova$setRemovalReason(RemovalReason.KILLED);
            if (!current.getAsBoolean()) return;
            state.ronova$setAddedToWorld(false);
            if (!current.getAsBoolean()) return;
            state.ronova$setLevelCallback(EntityInLevelCallback.NULL);
        } catch (Throwable ignored) {
            // Previous public lifecycle lanes may already have finalized it.
        }
    }

    private static void notifyRemoval(Entity entity, java.util.function.BooleanSupplier current) {
        try {
            EntityStateAccessor state = (EntityStateAccessor) entity;
            if (!current.getAsBoolean()) return;
            EntityInLevelCallback callback = state.ronova$getLevelCallback();
            if (callback != null && callback != EntityInLevelCallback.NULL) {
                try {
                    callback.onRemove(RemovalReason.KILLED);
                } catch (Throwable ignored) {
                    // Direct state below remains authoritative.
                }
            }
        } catch (Throwable ignored) {
            // The separate field sink can converge state without replaying this callback.
        }
    }
}
