package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.RealmId;
import dev.ronova.coremod.contract.SubjectKey;
import dev.ronova.coremod.root.api.SubjectQualification;
import dev.ronova.coremod.root.api.TargetEpoch;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.HashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Versioned exact extension in the existing Root image and command receipt. */
final class QualificationNbtCodec {
    private static final int MAX_ROWS = 1_000_000;
    private QualificationNbtCodec() {}

    static CompoundTag encode(Collection<SubjectQualification> values) {
        List<SubjectQualification> sorted = new ArrayList<>(values);
        if (sorted.size() > MAX_ROWS) throw new IllegalArgumentException("qualification row limit exceeded");
        sorted.sort(Comparator.comparing(SubjectQualification::subjectKey));
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema", sorted.stream().anyMatch(value -> value.revisions() != null) ? 2 : 1);
        ListTag rows = new ListTag();
        for (SubjectQualification value : sorted) {
            CompoundTag row = new CompoundTag();
            row.putString("realm_id", value.subjectKey().realmId().canonical());
            row.putUUID("subject_id", value.subjectKey().subjectId());
            row.putLong("target_generation", value.targetEpoch().generation());
            row.putBoolean("terminal", value.terminal());
            if (value.revisions() != null) row.put("revisions", SubjectRevisionNbtCodec.encode(value.revisions()));
            rows.add(row);
        }
        tag.put("subjects", rows);
        tag.putString("hash", hash(sorted));
        return tag;
    }

    static List<SubjectQualification> decode(CompoundTag tag) {
        if (!tag.contains("schema", Tag.TAG_INT) || tag.getInt("schema") < 1 || tag.getInt("schema") > 2
            || !tag.contains("subjects", Tag.TAG_LIST) || !tag.contains("hash", Tag.TAG_STRING)) {
            throw new IllegalArgumentException("malformed qualification extension");
        }
        ListTag rows = tag.getList("subjects", Tag.TAG_COMPOUND);
        if (rows.size() > MAX_ROWS) throw new IllegalArgumentException("invalid qualification rows");
        // getList(type) can silently return an empty list on a wrong element type.
        if (!(tag.get("subjects") instanceof ListTag original) || original.size() != rows.size()) {
            throw new IllegalArgumentException("wrong qualification row type");
        }
        List<SubjectQualification> result = new ArrayList<>();
        HashSet<SubjectKey> keys = new HashSet<>();
        for (Tag raw : rows) {
            CompoundTag row = (CompoundTag) raw;
            if (!row.contains("realm_id", Tag.TAG_STRING) || !row.hasUUID("subject_id")
                || !row.contains("target_generation", Tag.TAG_LONG) || !row.contains("terminal", Tag.TAG_BYTE)) {
                throw new IllegalArgumentException("incomplete qualification row");
            }
            if (row.getByte("terminal") != 0 && row.getByte("terminal") != 1) {
                throw new IllegalArgumentException("non-canonical qualification terminal flag");
            }
            SubjectKey key = new SubjectKey(RealmId.fromString(row.getString("realm_id")), row.getUUID("subject_id"));
            if (!keys.add(key)) throw new IllegalArgumentException("duplicate qualification subject");
            if (row.contains("revisions") && (tag.getInt("schema") != 2 || !row.contains("revisions", Tag.TAG_COMPOUND))) {
                throw new IllegalArgumentException("invalid subject revision extension");
            }
            result.add(new SubjectQualification(new TargetEpoch(key, row.getLong("target_generation")), row.getBoolean("terminal"),
                row.contains("revisions") ? SubjectRevisionNbtCodec.decode(key, row.getCompound("revisions")) : null));
        }
        result.sort(Comparator.comparing(SubjectQualification::subjectKey));
        if ((result.stream().anyMatch(value -> value.revisions() != null) ? 2 : 1) != tag.getInt("schema")) {
            throw new IllegalArgumentException("non-canonical qualification schema");
        }
        if (!hash(result).equals(tag.getString("hash"))) throw new IllegalArgumentException("qualification hash mismatch");
        return List.copyOf(result);
    }

    private static String hash(List<SubjectQualification> values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (SubjectQualification value : values) {
                String row = value.subjectKey().canonical() + ":" + value.targetEpoch().generation()
                    + ":" + value.terminal()
                    + (value.revisions() == null ? "" : ":revisions-v1:" + value.revisions().canonical()) + "\n";
                digest.update(row.getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
