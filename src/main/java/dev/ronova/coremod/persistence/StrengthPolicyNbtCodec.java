package dev.ronova.coremod.persistence;

import dev.ronova.coremod.policy.MechanismLevel;
import dev.ronova.coremod.policy.PolicyRevision;
import dev.ronova.coremod.policy.ProfilePresetMatrix;
import dev.ronova.coremod.policy.StrengthOptionKey;
import dev.ronova.coremod.policy.StrengthPolicyConfiguration;
import dev.ronova.coremod.policy.StrengthPolicySnapshot;
import dev.ronova.coremod.policy.StrengthProfile;
import dev.ronova.coremod.policy.AttackAction;
import dev.ronova.coremod.policy.AttackActionLevel;
import dev.ronova.coremod.policy.AttackActionPolicy;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Strict bounded NBT codec for the Root-owned strength request. */
final class StrengthPolicyNbtCodec {
    private static final int MAX_ROWS = StrengthOptionKey.values().length;

    private StrengthPolicyNbtCodec() {}

    static CompoundTag encode(StrengthPolicyConfiguration configuration) {
        return encodeForSchema(configuration, StrengthPolicySnapshot.SCHEMA_VERSION);
    }

    /** Keep historical command payload semantics; only the live policy migrates. */
    static CompoundTag encodeCommand(StrengthPolicyConfiguration configuration) {
        return encodeForSchema(configuration, configuration.sourceSchema());
    }

    private static CompoundTag encodeForSchema(StrengthPolicyConfiguration configuration, int schema) {
        if (configuration == null) {
            throw new IllegalArgumentException("strength policy configuration is required");
        }
        CompoundTag root = new CompoundTag();
        root.putInt("schema", schema);
        root.putLong("revision", configuration.revision().value());
        if (schema >= 2) root.putInt("profile_matrix_revision", configuration.profileMatrixRevision());
        root.putString("profile", configuration.profile().name());
        ListTag rows = new ListTag();
        for (StrengthOptionKey option : StrengthOptionKey.values()) {
            CompoundTag row = new CompoundTag();
            row.putString("key", option.canonical());
            row.putString("level", configuration.levels().get(option).name());
            rows.add(row);
        }
        root.put("options", rows);
        if (schema >= 3) {
            root.putInt("action_matrix_revision", configuration.attacks().matrixRevision());
            ListTag actions = new ListTag();
            for (AttackAction action : AttackAction.values()) {
                CompoundTag row = new CompoundTag();
                row.putString("key", action.canonical());
                row.putString("level", configuration.attacks().levels().get(action).name());
                actions.add(row);
            }
            root.put("attack_actions", actions);
        }
        return root;
    }

    static StrengthPolicyConfiguration decode(CompoundTag root) {
        return decodeWithMetadata(root).configuration();
    }

    static Decoded decodeWithMetadata(CompoundTag root) {
        if (root == null
            || !root.contains("schema", Tag.TAG_INT)
            || !root.contains("revision", Tag.TAG_LONG)
            || !root.contains("profile", Tag.TAG_STRING)
            || !root.contains("options", Tag.TAG_LIST)) {
            throw new IllegalArgumentException("malformed strength policy payload");
        }
        int schema = root.getInt("schema");
        if (schema < 1 || schema > StrengthPolicySnapshot.SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported strength policy schema: " + schema);
        }
        int matrixRevision;
        if (schema >= 2) {
            if (!root.contains("profile_matrix_revision", Tag.TAG_INT)) {
                throw new IllegalArgumentException(
                    "strength policy is missing its profile matrix revision"
                );
            }
            matrixRevision = root.getInt("profile_matrix_revision");
        } else {
            matrixRevision = ProfilePresetMatrix.CURRENT_REVISION;
        }
        if (!ProfilePresetMatrix.supports(matrixRevision)) {
            throw new IllegalArgumentException(
                "unsupported profile preset matrix revision: " + matrixRevision
            );
        }
        PolicyRevision revision = new PolicyRevision(root.getLong("revision"));
        StrengthProfile profile = StrengthProfile.valueOf(root.getString("profile"));
        ListTag rows = root.getList("options", Tag.TAG_COMPOUND);
        if (rows.size() != MAX_ROWS) {
            throw new IllegalArgumentException(
                "strength policy must contain exactly " + MAX_ROWS + " option rows"
            );
        }
        EnumMap<StrengthOptionKey, MechanismLevel> levels =
            new EnumMap<>(StrengthOptionKey.class);
        for (Tag raw : rows) {
            CompoundTag row = (CompoundTag) raw;
            if (!row.contains("key", Tag.TAG_STRING)
                || !row.contains("level", Tag.TAG_STRING)) {
                throw new IllegalArgumentException("malformed strength policy option");
            }
            StrengthOptionKey option = StrengthOptionKey.fromCanonical(row.getString("key"));
            MechanismLevel level = MechanismLevel.valueOf(row.getString("level"));
            if (levels.put(option, level) != null) {
                throw new IllegalArgumentException(
                    "duplicate strength policy option: " + option.canonical()
                );
            }
        }
        AttackActionPolicy attacks = AttackActionPolicy.legacyDefaults(profile);
        if (schema >= 3) {
            if (!root.contains("action_matrix_revision", Tag.TAG_INT)
                || !root.contains("attack_actions", Tag.TAG_LIST)) {
                throw new IllegalArgumentException("missing attack action policy");
            }
            ListTag actionRows = root.getList("attack_actions", Tag.TAG_COMPOUND);
            if (actionRows.size() != AttackAction.values().length) {
                throw new IllegalArgumentException("attack policy requires exactly 14 action rows");
            }
            EnumMap<AttackAction, AttackActionLevel> values = new EnumMap<>(AttackAction.class);
            for (Tag raw : actionRows) {
                CompoundTag row = (CompoundTag) raw;
                if (!row.contains("key", Tag.TAG_STRING) || !row.contains("level", Tag.TAG_STRING)) {
                    throw new IllegalArgumentException("malformed attack action");
                }
                AttackAction action = AttackAction.fromCanonical(row.getString("key"));
                if (values.put(action, AttackActionLevel.valueOf(row.getString("level"))) != null) {
                    throw new IllegalArgumentException("duplicate attack action: " + action);
                }
            }
            attacks = new AttackActionPolicy(root.getInt("action_matrix_revision"), values);
        } else if (root.contains("attack_actions") || root.contains("action_matrix_revision")) {
            throw new IllegalArgumentException("legacy schema cannot carry unverified action fields");
        }
        StrengthPolicyConfiguration configuration = new StrengthPolicyConfiguration(
            revision, matrixRevision, profile, Map.copyOf(levels), attacks, schema);
        return new Decoded(
            configuration, schema, schema < StrengthPolicySnapshot.SCHEMA_VERSION
        );
    }

    record Decoded(
        StrengthPolicyConfiguration configuration,
        int sourceSchema,
        boolean migrated
    ) {}
}
