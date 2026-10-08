package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.*;
import dev.ronova.coremod.root.api.*;
import java.util.*;
import net.minecraft.nbt.*;

final class SubjectTransitionNbtCodec {
    static CompoundTag encode(SubjectTransitionBatch batch) {
        CompoundTag tag = new CompoundTag(); tag.putInt("schema", 1);
        tag.putUUID("actor", batch.actor());
        tag.putLong("expected_generation", batch.expectedEpoch().generation());
        tag.putLong("expected_sequence", batch.expectedEpoch().sequence());
        tag.put("qualification_preconditions", RootCommandJournal.preconditionsTag(batch.preconditions()));
        ListTag rows = new ListTag();
        for (var t : batch.transitions()) {
            CompoundTag row = new CompoundTag(); row.putUUID("subject", t.subjectKey().subjectId());
            row.putString("kind", t.kind().name());
            if (t.incarnationNonce() != null) row.putUUID("nonce", t.incarnationNonce());
            if (t.domain() != null) row.putString("domain", t.domain().value());
            if (t.owner() != null) row.putUUID("owner", t.owner().subjectId());
            rows.add(row);
        }
        tag.put("transitions", rows); return tag;
    }
    static SubjectTransitionBatch decode(UUID id, CompoundTag tag) {
        if (!tag.contains("schema", Tag.TAG_INT) || tag.getInt("schema") != 1 || !tag.hasUUID("actor")
            || !tag.contains("expected_generation", Tag.TAG_LONG) || !tag.contains("expected_sequence", Tag.TAG_LONG)
            || !(tag.get("transitions") instanceof ListTag)) throw new IllegalArgumentException("malformed subject transition batch");
        var expected = Objects.requireNonNull(RootCommandJournal.readPreconditions(tag), "preconditions");
        ListTag rows = tag.getList("transitions", Tag.TAG_COMPOUND);
        if (((ListTag) tag.get("transitions")).size() != rows.size() || rows.size() > 1_000_000) {
            throw new IllegalArgumentException("invalid transition rows");
        }
        List<SubjectTransitionBatch.Transition> changes = new ArrayList<>();
        for (Tag raw : rows) {
            CompoundTag row = (CompoundTag) raw;
            if (!row.hasUUID("subject") || !row.contains("kind", Tag.TAG_STRING)
                || row.contains("nonce") && !row.hasUUID("nonce")
                || row.contains("domain") && !row.contains("domain", Tag.TAG_STRING)
                || row.contains("owner") && !row.hasUUID("owner")) throw new IllegalArgumentException("invalid transition row");
            changes.add(new SubjectTransitionBatch.Transition(new SubjectKey(expected.realmId(), row.getUUID("subject")),
                SubjectTransitionBatch.Kind.valueOf(row.getString("kind")), row.hasUUID("nonce") ? row.getUUID("nonce") : null,
                row.contains("domain") ? new StateDomainId(row.getString("domain")) : null,
                row.hasUUID("owner") ? new SubjectKey(expected.realmId(), row.getUUID("owner")) : null));
        }
        return new SubjectTransitionBatch(id, tag.getUUID("actor"), new DominionEpoch(tag.getLong("expected_generation"),
            tag.getLong("expected_sequence")), expected, changes);
    }
}
