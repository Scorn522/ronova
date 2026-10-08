package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.policy.MechanismLevel;
import dev.ronova.coremod.policy.PolicyRevision;
import dev.ronova.coremod.policy.ProfilePresetMatrix;
import dev.ronova.coremod.policy.StrengthOptionKey;
import dev.ronova.coremod.policy.StrengthPolicyConfiguration;
import dev.ronova.coremod.policy.StrengthProfile;
import dev.ronova.coremod.policy.AttackAction;
import dev.ronova.coremod.policy.AttackActionLevel;
import dev.ronova.coremod.root.api.CommitReceipt;
import dev.ronova.coremod.root.api.DominionCommand;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Codec/journal regression; offline runs require the explicit NBT stub label. */
public final class StrengthPolicyPersistenceSelfTest {
    private StrengthPolicyPersistenceSelfTest() {}

    public static void main(String[] args) {
        codecRoundTripAndMigration();
        malformedMatricesFailClosed();
        journalMigrationPreservesHistoricalEncoding();
        actionPoliciesAndTamper();
        System.out.println(
            "StrengthPolicyPersistenceSelfTest: PASS "
                + "(schema 3 round-trip, schema 1/2 migration, strict 54+14 rows, "
                + "historical journal tag/hash preservation, action tamper)"
        );
    }

    private static void codecRoundTripAndMigration() {
        StrengthPolicyConfiguration configuration = configuration();
        CompoundTag current = StrengthPolicyNbtCodec.encode(configuration);
        require(current.getInt("schema") == 3
                && current.getInt("profile_matrix_revision")
                    == ProfilePresetMatrix.CURRENT_REVISION
                && StrengthPolicyNbtCodec.decode(current).equals(configuration),
            "schema-3 strength policy did not round-trip");

        CompoundTag legacy = current.copy();
        legacy.putInt("schema", 1);
        legacy.remove("profile_matrix_revision");
        legacy.remove("attack_actions");
        legacy.remove("action_matrix_revision");
        StrengthPolicyNbtCodec.Decoded decoded =
            StrengthPolicyNbtCodec.decodeWithMetadata(legacy);
        require(decoded.migrated()
                && decoded.sourceSchema() == 1
                && decoded.configuration().equals(configuration),
            "schema-1 strength policy did not migrate deterministically");
    }

    private static void malformedMatricesFailClosed() {
        CompoundTag incomplete = StrengthPolicyNbtCodec.encode(configuration());
        incomplete.getList("options", Tag.TAG_COMPOUND).remove(0);
        expectRejected(incomplete, "incomplete matrix");

        CompoundTag duplicate = StrengthPolicyNbtCodec.encode(configuration());
        ListTag duplicateRows = duplicate.getList("options", Tag.TAG_COMPOUND);
        duplicateRows.set(
            duplicateRows.size() - 1,
            ((CompoundTag) duplicateRows.get(0)).copy()
        );
        expectRejected(duplicate, "duplicate matrix key");

        CompoundTag unknownRevision = StrengthPolicyNbtCodec.encode(configuration());
        unknownRevision.putInt("profile_matrix_revision", 2);
        expectRejected(unknownRevision, "unknown matrix revision");
    }

    private static void journalMigrationPreservesHistoricalEncoding() {
        StrengthPolicyConfiguration configuration = configuration();
        UUID commandId = UUID.fromString(
            "00000000-0000-0000-0000-000000000531"
        );
        DominionCommand.ConfigureStrengthPolicy command =
            new DominionCommand.ConfigureStrengthPolicy(commandId, configuration);
        DominionEpoch before = DominionEpoch.ZERO;
        DominionEpoch after = before.nextSequence();
        DominionSnapshot snapshot = new DominionSnapshot(after, List.of());

        RootCommandJournal source = new RootCommandJournal();
        source.append(command, before, snapshot, CommitReceipt.Status.COMMITTED);
        CompoundTag legacy = new CompoundTag();
        source.save(legacy);
        CompoundTag row = (CompoundTag) legacy.getList(
            "command_journal_v1", Tag.TAG_COMPOUND
        ).get(0);
        CompoundTag encodedConfiguration = row.getCompound("command")
            .getCompound("configuration");
        encodedConfiguration.putInt("schema", 1);
        encodedConfiguration.remove("profile_matrix_revision");
        encodedConfiguration.remove("attack_actions");
        encodedConfiguration.remove("action_matrix_revision");
        String legacyHash = DominionCommand.canonicalPayloadHash(command, 1);
        row.putString("payload_hash", legacyHash);

        RootCommandJournal migrated = new RootCommandJournal();
        migrated.restore(legacy, snapshot);
        CommitReceipt recovered = migrated.find(commandId, legacyHash);
        require(recovered != null && recovered.committed(),
            "legacy journal receipt was not recovered");

        CompoundTag rewritten = new CompoundTag();
        migrated.save(rewritten);
        CompoundTag rewrittenRow = (CompoundTag) rewritten.getList(
            "command_journal_v1", Tag.TAG_COMPOUND
        ).get(0);
        require(rewrittenRow.getString("payload_hash").equals(
                legacyHash
            ) && rewrittenRow.getCompound("command")
                .getCompound("configuration").getInt("schema") == 1,
            "legacy journal command was reinterpreted under new permissions");
        RootCommandJournal reloaded = new RootCommandJournal();
        reloaded.restore(rewritten, snapshot);
        require(reloaded.find(commandId, legacyHash) != null,
            "legacy replay failed after a save/reload cycle");
    }

    private static void actionPoliciesAndTamper() {
        for (int schema : new int[] {1, 2}) {
            for (StrengthProfile profile : StrengthProfile.values()) {
                CompoundTag legacy = StrengthPolicyNbtCodec.encode(
                    new StrengthPolicyConfiguration(new PolicyRevision(8L), profile, Map.of()));
                legacy.putInt("schema", schema);
                if (schema == 1) legacy.remove("profile_matrix_revision");
                legacy.remove("attack_actions");
                legacy.remove("action_matrix_revision");
                StrengthPolicyConfiguration migrated = StrengthPolicyNbtCodec.decode(legacy);
                for (AttackAction action : AttackAction.values()) {
                    require(migrated.attacks().levels().get(action) ==
                        (profile == StrengthProfile.CUSTOM ? AttackActionLevel.DISABLED
                            : AttackActionLevel.FOLLOW_PROFILE), "legacy action migration escalated permissions");
                }
                require(StrengthPolicyNbtCodec.decode(StrengthPolicyNbtCodec.encode(migrated))
                    .equals(migrated), "migrated policy failed schema-3 reload");
            }
        }
        StrengthPolicyConfiguration policy = configuration().next(StrengthProfile.CUSTOM, Map.of(),
            Map.of(AttackAction.ALL_RETURN, AttackActionLevel.DISABLED));
        require(StrengthPolicyNbtCodec.decode(StrengthPolicyNbtCodec.encode(policy)).equals(policy),
            "custom action override did not round-trip");
        CompoundTag missing = StrengthPolicyNbtCodec.encode(policy);
        missing.getList("attack_actions", Tag.TAG_COMPOUND).remove(0);
        expectRejected(missing, "missing ordinary action");
        CompoundTag duplicate = StrengthPolicyNbtCodec.encode(policy);
        ListTag rows = duplicate.getList("attack_actions", Tag.TAG_COMPOUND);
        rows.set(1, ((CompoundTag) rows.get(0)).copy());
        expectRejected(duplicate, "duplicate ordinary action");
        CompoundTag unknown = StrengthPolicyNbtCodec.encode(policy);
        unknown.putInt("action_matrix_revision", 2);
        expectRejected(unknown, "unknown action matrix");
        CompoundTag downgrade = StrengthPolicyNbtCodec.encode(policy);
        downgrade.putInt("schema", 2);
        expectRejected(downgrade, "unhashed action fields in legacy schema");
        CompoundTag tampered = StrengthPolicyNbtCodec.encode(policy);
        for (Tag raw : tampered.getList("attack_actions", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) raw;
            if (row.getString("key").equals(AttackAction.ALL_RETURN.canonical())) {
                row.putString("level", AttackActionLevel.EXTREME.name());
            }
        }
        UUID id = UUID.randomUUID();
        DominionCommand original = new DominionCommand.ConfigureStrengthPolicy(id, policy);
        DominionCommand changed = new DominionCommand.ConfigureStrengthPolicy(id,
            StrengthPolicyNbtCodec.decode(tampered));
        require(!DominionCommand.matchesCanonicalPayloadHash(changed,
            DominionCommand.canonicalPayloadHash(original)), "action tamper preserved payload hash");
    }

    private static StrengthPolicyConfiguration configuration() {
        return new StrengthPolicyConfiguration(
            new PolicyRevision(7L),
            ProfilePresetMatrix.CURRENT_REVISION,
            StrengthProfile.ENHANCED,
            Map.of(
                StrengthOptionKey.ATTACK_DYNAMIC_CODE, MechanismLevel.MONITOR,
                StrengthOptionKey.PROTECT_LOOP_GUARD, MechanismLevel.OFF
            )
        );
    }

    private static void expectRejected(CompoundTag value, String message) {
        boolean rejected = false;
        try {
            StrengthPolicyNbtCodec.decode(value);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        require(rejected, message + " was accepted");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
