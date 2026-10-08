package dev.ronova.coremod.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.ronova.coremod.authority.AuthorityBinder;
import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.content.DominionContent;
import dev.ronova.coremod.engine.JudgmentEngine;
import dev.ronova.coremod.projection.ProjectionBundle;
import dev.ronova.coremod.engine.QuarantineView;
import dev.ronova.coremod.engine.SpawnFirewall;
import dev.ronova.coremod.network.RonovaNetwork;
import dev.ronova.coremod.nativebridge.NativeDominionRoot;
import dev.ronova.coremod.policy.MechanismLevel;
import dev.ronova.coremod.policy.ProfilePresetMatrix;
import dev.ronova.coremod.policy.StrengthProfile;
import dev.ronova.coremod.policy.AttackAction;
import dev.ronova.coremod.policy.AttackActionLevel;
import dev.ronova.coremod.policy.StrengthOptionKey;
import dev.ronova.coremod.policy.StrengthPolicyConfiguration;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ronova.coremod.runtime.RuntimeCoordinator;
import dev.ronova.coremod.root.api.DominionCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public final class DominionCommands {
    private DominionCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ronova")
            .then(Commands.literal("give").requires(source -> source.hasPermission(2)).executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                player.getInventory().add(DominionContent.RONOVA_BLADE.get().getDefaultInstance());
                AuthorityBinder.grant(player);
                context.getSource().sendSuccess(() -> Component.literal("Ronova Blade granted and owner bound."), false);
                return 1;
            }))
            .then(Commands.literal("bind").requires(source -> source.hasPermission(2)).executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                AuthorityBinder.grant(player);
                context.getSource().sendSuccess(() -> Component.literal("Owner continuity bound."), false);
                return 1;
            }))
            .then(Commands.literal("unbind").requires(source -> source.hasPermission(2)).executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                AuthorityBinder.unbind(player);
                context.getSource().sendSuccess(() -> Component.literal("Owner continuity unbound."), false);
                return 1;
            }))
            .then(Commands.literal("status").executes(context -> {
                var server = context.getSource().getServer();
                var runtime = ServerRuntimeRegistry.runtime(server);
                ProjectionBundle projections = runtime.projection();
                NativeDominionRoot.Status nativeRoot = NativeDominionRoot.status();
                var enforcement = runtime.enforcement().published();
                var challenge = runtime.enforcement().challengeProof();
                var watch = runtime.terminalWatch().metrics();
                var strength = runtime.strengthPolicy();
                String contractEpoch = projections.snapshot().epoch().generation()
                    + ":" + projections.snapshot().epoch().sequence();
                context.getSource().sendSuccess(() -> Component.literal(
                    "contract=" + (nativeRoot.ready() && enforcement.current()
                        ? "R4_L5A_SEMANTIC_NATIVE_ROOT"
                        : nativeRoot.ready()
                            ? "R4_L5A_DEGRADED_NATIVE_ROOT"
                            : "R4_L5A_JAVA_FALLBACK")
                        + ", epoch=" + contractEpoch
                        + ", root=" + (nativeRoot.ready() ? "ACTIVE" : "UNAVAILABLE")
                        + ", native_epoch=" + nativeRoot.generation() + ":" + nativeRoot.sequence()
                        + ", native_hash=" + Long.toUnsignedString(
                            nativeRoot.fingerprint(), 16
                        )
                        + ", projection_current=" + nativeRoot.projectionCurrent()
                        + ", owners=" + projections.authorityTopology().ownerCount()
                        + ", anchors=" + runtime.authorityState().anchorCount()
                        + ", connections=" + runtime.authorityState().connectionCount()
                        + ", tombstones=" + projections.admission().tombstoneCount()
                        + ", observation_mode=" + runtime.rawObservationMode()
                        + ", observation_flag_source=" + runtime.rawObservationFlagSource()
                        + ", observed=" + runtime.observation().size()
                        + ", watch_mode=" + watch.mode()
                        + ", watch_entries=" + watch.activeEntries()
                        + ", watch_scheduled=" + watch.scheduledNotifications()
                        + ", watch_attempted=" + watch.attemptNotifications()
                        + ", watch_completed=" + watch.completionNotifications()
                        + ", watch_notification_failures="
                        + runtime.passiveWatchNotificationFailures()
                        + ", pulses=" + RuntimeCoordinator.serverPulses()
                        + ", rejected=" + SpawnFirewall.rejectedAdmissions(server)
                        + ", rejected_nbt=" + SpawnFirewall.nbtRejections(server)
                        + ", cached_pass=" + SpawnFirewall.passedObjects()
                        + ", admission_retry_pending="
                        + runtime.spawnAdmission().pendingConstructionRetries()
                        + ", admission_quarantined_failures="
                        + runtime.spawnAdmission().quarantinedConstructionFailures()
                        + ", admission_dead_letter_total="
                        + runtime.spawnAdmission().deadLetteredConstructionFailures()
                        + ", admission_dead_letter_latest="
                        + runtime.spawnAdmission().latestQuarantinedConstructionUuid()
                        + ", admission_retry_limit="
                        + runtime.spawnAdmission().constructionRetryLimit()
                        + ", retries=" + JudgmentEngine.pendingRetries()
                        + ", quarantined=" + QuarantineView.objectCount(server)
                        + ", projections=" + runtime.projectionCoordinator().publications()
                        + ", stale=" + runtime.projectionCoordinator().staleRejections()
                        + ", native_recoveries=" + runtime.projectionCoordinator().nativeRecoveries()
                        + ", module_failures=" + runtime.capabilities().diagnostics().totalFailures()
                        + ", root_pending=" + runtime.commands().hasPendingPublication()
                        + ", root_publish_failures=" + runtime.commands().blockedPublications()
                        + ", durable_authoritative="
                        + (runtime.durableStore() != null
                            && runtime.durableStore().authoritative())
                        + ", durable_commit_index="
                        + (runtime.durableStore() == null
                            ? 0L : runtime.durableStore().commitIndex())
                        + ", durable_file_present="
                        + (runtime.durableStore() != null
                            && runtime.durableStore().durableFilePresent())
                        + ", durable_persistence_pending="
                        + (runtime.durableStore() != null
                            && runtime.durableStore().persistencePending())
                        + ", durable_persistence_failure="
                        + (runtime.durableStore() == null
                            ? "" : runtime.durableStore().persistenceFailure())
                        + ", lifecycle_unverified_claims="
                        + (runtime.durableStore() == null
                            ? 0 : runtime.durableStore().lifecycleClaimCount())
                        + ", effect_records=" + (runtime.durableStore() == null ? 0 : runtime.durableStore().effectRecordCount())
                        + ", effect_unresolved=" + (runtime.durableStore() == null ? 0 : runtime.durableStore().effectUnresolvedCount())
                        + ", effect_reserved_growth_bytes=" + (runtime.durableStore() == null ? 0 : runtime.durableStore().effectReservedGrowthBytes())
                        + ", storage_rejections=" + (runtime.durableStore() == null ? 0 : runtime.durableStore().storageRejections())
                        + ", storage_pressure=" + (runtime.durableStore() == null || runtime.durableStore().storageCapacity() == null
                            ? "UNOBSERVED" : runtime.durableStore().storageCapacity().state())
                        + ", strength_revision=" + strength.revision().value()
                        + ", strength_configured_matrix="
                        + strength.configuredMatrixRevision()
                        + ", strength_active_matrix="
                        + strength.activeMatrixRevision()
                        + ", strength_configured=" + strength.configuredProfile()
                        + ", strength_active=" + strength.activeProfile()
                        + ", strength_pending_restart=" + strength.pendingRestart()
                        + ", policy_generation=" + nativeRoot.policyGeneration()
                        + ", admission_generation=" + nativeRoot.admissionGeneration()
                        + ", reconcile=" + nativeRoot.reconciliationClock()
                        + ", drift=" + nativeRoot.driftDetections()
                        + ", native_decisions=" + nativeRoot.nativeDecisions()
                        + ", degraded_decisions=" + nativeRoot.degradedDecisions()
                        + ", integrity=" + enforcement.state()
                        + ", integrity_required=0x" + Integer.toHexString(
                            enforcement.requiredMask()
                        )
                        + ", integrity_verified=0x" + Integer.toHexString(
                            enforcement.verifiedMask()
                        )
                        + ", integrity_generation=" + enforcement.generation()
                        + ", integrity_age_ms=" + enforcement.ageMillis()
                        + ", integrity_failures=" + enforcement.failedLeases()
                        + ", integrity_semantic_failures=" + enforcement.semanticFailures()
                        + ", integrity_read_failures=" + runtime.enforcement().readFailures()
                        + ", code_challenge=" + (challenge.current()?"VERIFIED":challenge.supported()?"DEGRADED":"UNAVAILABLE")
                        + ", code_challenge_generation=" + challenge.generation()
                        + ", code_challenge_attempts=" + challenge.attempts()
                        + ", code_challenge_successes=" + challenge.successes()
                        + ", code_challenge_failures=" + challenge.failures()
                        + ", code_challenge_read_failures=" + runtime.enforcement().challengeReadFailures()
                ), false);
                return projections.admission().tombstoneCount();
            }))
            .then(Commands.literal("diagnostics").requires(source -> source.hasPermission(2)).executes(context -> {
                var runtime = ServerRuntimeRegistry.runtime(context.getSource().getServer());
                runtime.diagnostics().sampleIfDue(System.nanoTime());
                context.getSource().sendSuccess(() -> Component.literal("obligation_details="+runtime.obligations().registry().size()
                    +" checkpoint_revision="+runtime.obligations().checkpointRevision()+" checkpoint_failures="+runtime.obligations().checkpointFailures()
                    +" reviews="+runtime.obligations().reviews()+" overflow="+runtime.obligations().registry().coverage()
                    +" enforcement=EXISTING_ROOT_GATES_ONLY"),false);
                for (var row : runtime.diagnostics().surfaces()) {
                    context.getSource().sendSuccess(() -> Component.literal("surface=" + row.surface()
                        + " supported=" + row.supported() + " evidence=" + row.evidence() + " state=" + row.state()
                        + " registry_revision=" + row.registryRevision() + " sample=" + row.sampledRevision()
                        + " scope=" + (row.lastTrustedObservation() == null ? "UNOBSERVED" : row.lastTrustedObservation().scope())
                        + " detail=" + row.diagnostic()), false);
                }
                for (var row : runtime.diagnostics().resources()) {
                    context.getSource().sendSuccess(() -> Component.literal("resource=" + row.domain() + " unit=" + row.domain().unit()
                        + " accounting=" + row.accounting() + " owned=" + row.ownedUsage() + " peak=" + row.ownedPeak()
                        + " host_used=" + row.host().usage() + " host_limit=" + row.host().limit() + " host_pressure=" + row.host().pressure()
                        + " soft=" + row.budget().soft() + " hard=" + row.budget().hard() + " reserve=" + row.budget().recoveryReserve()
                        + " throttle=" + row.throttleState() + " recovery_headroom_used=" + row.recoveryHeadroomInUse()
                        + " allocation_rate=" + row.allocationRatePerSecond() + " last_release_revision=" + row.lastReleaseRevision()
                        + " last_denial=" + row.lastDenial() + " denied=" + row.denied() + " released=" + row.releasedTotal()), false);
                }
                return runtime.diagnostics().surfaces().size();
            }))
            .then(Commands.literal("strength")
                .then(attackCommands())
                .then(optionCommands())
                .then(Commands.literal("status").executes(context ->
                    reportStrength(context.getSource())
                ))
                .then(Commands.literal("compatible")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> setStrengthProfile(
                        context.getSource(), StrengthProfile.COMPATIBLE
                    )))
                .then(Commands.literal("enhanced")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> setStrengthProfile(
                        context.getSource(), StrengthProfile.ENHANCED
                    )))
                .then(Commands.literal("extreme")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> setStrengthProfile(
                        context.getSource(), StrengthProfile.EXTREME
                    )))
                .then(Commands.literal("custom")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> setStrengthProfile(
                        context.getSource(), StrengthProfile.CUSTOM
                    )))
            )
            .then(Commands.literal("judge_loaded").requires(source -> source.hasPermission(2)).executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                int count = JudgmentEngine.loadedWorldJudgment(player);
                context.getSource().sendSuccess(() -> Component.literal("Judged loaded entity objects: " + count), true);
                return count;
            }))
            .then(Commands.literal("diagnose_uuid").requires(source -> source.hasPermission(2))
                .then(Commands.argument("uuid", UuidArgument.uuid()).executes(context ->
                    diagnoseUuid(
                        context.getSource(), UuidArgument.getUuid(context, "uuid")
                    )
                )))
            .then(Commands.literal("clear_tombstones").requires(source -> source.hasPermission(2)).executes(context -> {
                var server = context.getSource().getServer();
                var runtime = ServerRuntimeRegistry.runtime(server);
                int count = runtime.projection().admission().tombstoneCount();
                var receipt=runtime.commands().commit(new DominionCommand.ClearTombstones());
                if(!receipt.committed()) {
                    context.getSource().sendFailure(Component.literal("Root 尚未确认解除；当前限制状态待确认。"));
                    return 0;
                }
                SpawnFirewall.onTombstonesCleared(context.getSource().getServer());
                RonovaNetwork.broadcastQuarantineClear(context.getSource().getServer());
                context.getSource().sendSuccess(() -> Component.literal("已解除裁决：" + count
                    + "。仍有独立存档、管理器或任务恢复来源的目标可能重新出现。"), true);
                return count;
            }))
            .then(Commands.literal("clear_uuid").requires(source -> source.hasPermission(2))
                .then(Commands.argument("uuid", UuidArgument.uuid()).executes(context -> {
                    java.util.UUID uuid = UuidArgument.getUuid(context, "uuid");
                    var runtime = ServerRuntimeRegistry.runtime(context.getSource().getServer());
                    var receipt=runtime.commands().commit(new DominionCommand.ClearTombstone(uuid));
                    if(!receipt.committed()) {
                        context.getSource().sendFailure(Component.literal("Root 尚未确认解除；当前限制状态待确认。"));
                        return 0;
                    }
                    boolean removed=receipt.changed();
                    SpawnFirewall.clearIdentity(context.getSource().getServer(), uuid);
                    RonovaNetwork.broadcastQuarantineRemoval(context.getSource().getServer(), uuid);
                    context.getSource().sendSuccess(() -> Component.literal(
                        removed ? "已解除裁决：" + uuid + "。独立恢复来源仍可能重建此目标。" : "未找到该 UUID 的裁决：" + uuid
                    ), true);
                    return removed ? 1 : 0;
                })))
        );
    }

    private static int reportStrength(CommandSourceStack source) {
        var policy = ServerRuntimeRegistry.runtime(source.getServer()).strengthPolicy();
        source.sendSuccess(() -> Component.literal(
            "strength_revision=" + policy.revision().value()
                + ", configured_matrix=" + policy.configuredMatrixRevision()
                + ", active_matrix=" + policy.activeMatrixRevision()
                + ", configured=" + policy.configuredProfile()
                + ", active=" + policy.activeProfile()
                + ", pending_restart=" + policy.pendingRestart()
        ), false);
        for (AttackAction action : AttackAction.values()) {
            var resolution = policy.resolveAttack(java.util.Set.of(action), java.util.Set.of());
            source.sendSuccess(() -> Component.literal(
                action.displayName() + ": 保存=" + policy.configuredActions().levels().get(action)
                    + ", 当前=" + policy.activeActions().resolve(policy.activeProfile(), action)
                    + ", 接线=" + (action == AttackAction.TERMINALIZE
                        ? "新终结提交门" : "未接线（仅保存请求）")
                    + ", 高级解析=" + resolution.reason()
            ), false);
        }
        source.sendSuccess(() -> Component.literal(
            "关闭终结不会解除已有裁决。其余普通项尚不改变旧终态物理链。"
        ), false);
        return policy.pendingRestart() ? 0 : 1;
    }

    private static int setStrengthProfile(
        CommandSourceStack source,
        StrengthProfile profile
    ) {
        var runtime = ServerRuntimeRegistry.runtime(source.getServer());
        var current = runtime.strengthPolicy().configuration();
        boolean alreadySelected = current.profileMatrixRevision()
                == ProfilePresetMatrix.CURRENT_REVISION
            && current.profile() == profile
            && (profile == StrengthProfile.CUSTOM
                ? current.levels().values().stream().noneMatch(
                    level -> level == MechanismLevel.FOLLOW_PROFILE
                )
                : current.levels().values().stream().allMatch(
                    level -> level == MechanismLevel.FOLLOW_PROFILE
                ))
            && (profile == StrengthProfile.CUSTOM
                ? current.attacks().levels().values().stream().noneMatch(
                    level -> level == AttackActionLevel.FOLLOW_PROFILE)
                : current.attacks().levels().values().stream().allMatch(
                    level -> level == AttackActionLevel.FOLLOW_PROFILE));
        if (alreadySelected) {
            source.sendSuccess(() -> Component.literal(
                "Strength preset is already " + profile + "."
            ), false);
            return 0;
        }
        var next = current.next(profile, java.util.Map.of());
        return saveStrength(source, next);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> attackCommands() {
        var group = Commands.literal("attack").requires(source -> source.hasPermission(2));
        for (AttackAction action : AttackAction.values()) {
            var entry = Commands.literal(action.canonical());
            for (AttackActionLevel level : AttackActionLevel.values()) {
                if (level == AttackActionLevel.FOLLOW_PROFILE) continue;
                String name = level == AttackActionLevel.DISABLED ? "off"
                    : level.name().toLowerCase(java.util.Locale.ROOT);
                entry.then(Commands.literal(name).executes(context -> {
                    var current = ServerRuntimeRegistry.runtime(context.getSource().getServer())
                        .strengthPolicy().configuration();
                    return saveStrength(context.getSource(), current.next(
                        StrengthProfile.CUSTOM, java.util.Map.of(), java.util.Map.of(action, level)));
                }));
            }
            group.then(entry);
        }
        return group;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> optionCommands() {
        var group = Commands.literal("option").requires(source -> source.hasPermission(2));
        for (StrengthOptionKey option : StrengthOptionKey.values()) {
            var entry = Commands.literal(option.canonical());
            for (MechanismLevel level : MechanismLevel.values()) {
                if (level == MechanismLevel.FOLLOW_PROFILE) continue;
                entry.then(Commands.literal(level.name().toLowerCase(java.util.Locale.ROOT))
                    .executes(context -> {
                        var current = ServerRuntimeRegistry.runtime(context.getSource().getServer())
                            .strengthPolicy().configuration();
                        return saveStrength(context.getSource(), current.next(
                            StrengthProfile.CUSTOM, java.util.Map.of(option, level)));
                    }));
            }
            group.then(entry);
        }
        return group;
    }

    private static int saveStrength(CommandSourceStack source, StrengthPolicyConfiguration next) {
        var runtime = ServerRuntimeRegistry.runtime(source.getServer());
        var receipt = runtime.commands().commit(
            new DominionCommand.ConfigureStrengthPolicy(next)
        );
        if (!receipt.committed()) {
            source.sendFailure(Component.literal(
                "Strength preset was not committed: " + receipt.status()
            ));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
            "Strength policy saved as " + next.profile()
                + " (matrix " + next.profileMatrixRevision() + ")"
                + "; restart required before the whole profile becomes active."
        ), true);
        source.sendSuccess(() -> Component.literal(
            "r54：未接线的普通项只保存请求；用 /ronova strength status 查看接线范围。"
        ), false);
        return 1;
    }

    private static int diagnoseUuid(CommandSourceStack source, UUID uuid) {
        var runtime = ServerRuntimeRegistry.runtime(source.getServer());
        var watch = runtime.terminalWatch().snapshot(uuid);
        long scheduled = 0L;
        long attempted = 0L;
        String completion = "NONE";
        for (var entry : watch.entries().entrySet()) {
            scheduled += entry.getValue().schedules().size();
            attempted += entry.getValue().attempts().size();
            for (var value : entry.getValue().completions()) {
                completion = value.name();
            }
        }
        int rawCount = runtime.observation().snapshot(uuid).size();
        String finalCompletion = completion;
        String line = "RONOVA_714A_DIAG uuid=" + uuid
            + " mode=" + runtime.rawObservationMode()
            + " raw_count=" + rawCount
            + " watch_mode=" + watch.mode()
            + " scheduled=" + scheduled
            + " attempted=" + attempted
            + " completion=" + finalCompletion
            + " notification_failures=" + runtime.passiveWatchNotificationFailures();
        source.sendSuccess(() -> Component.literal(line), false);
        return rawCount;
    }
}
