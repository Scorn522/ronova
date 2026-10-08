package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.*;
import dev.ronova.coremod.root.api.*;
import java.util.*;
import net.minecraft.nbt.*;

final class SubjectRevisionNbtCodec {
    static CompoundTag encode(SubjectRevisions value) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("binding", value.bindingRevision());
        tag.putLong("judgment", value.judgmentRevision());
        tag.putLong("admission", value.admissionTokenRevision());
        tag.putLong("revoked_through", value.revokedThroughEpoch());
        if (value.activeIncarnation() != null) {
            tag.putLong("incarnation_sequence", value.activeIncarnation().sequence());
            tag.putUUID("incarnation_nonce", value.activeIncarnation().nonce());
        }
        ListTag owners = new ListTag();
        value.ownership().forEach((domain, binding) -> {
            CompoundTag row = new CompoundTag();
            row.putString("domain", domain.value()); row.putLong("revision", binding.revision());
            if (binding.owner() != null) row.putUUID("owner", binding.owner().subjectId());
            owners.add(row);
        });
        tag.put("ownership", owners);
        return tag;
    }
    static SubjectRevisions decode(SubjectKey subject, CompoundTag tag) {
        for (String field : List.of("binding", "judgment", "admission", "revoked_through")) {
            if (!tag.contains(field, Tag.TAG_LONG)) throw new IllegalArgumentException("missing subject revision: " + field);
        }
        PhysicalIncarnationId incarnation = null;
        if (tag.contains("incarnation_sequence") || tag.contains("incarnation_nonce")) {
            if (!tag.contains("incarnation_sequence", Tag.TAG_LONG) || !tag.hasUUID("incarnation_nonce")) {
                throw new IllegalArgumentException("incomplete incarnation");
            }
            incarnation = new PhysicalIncarnationId(subject, tag.getLong("incarnation_sequence"), tag.getUUID("incarnation_nonce"));
        }
        if (!(tag.get("ownership") instanceof ListTag original)) throw new IllegalArgumentException("missing domain ownership");
        ListTag rows = tag.getList("ownership", Tag.TAG_COMPOUND);
        if (rows.size() != original.size() || rows.size() > 128) throw new IllegalArgumentException("invalid domain ownership rows");
        Map<StateDomainId, OwnershipBinding> ownership = new TreeMap<>();
        for (Tag raw : rows) {
            CompoundTag row = (CompoundTag) raw;
            if (!row.contains("domain", Tag.TAG_STRING) || !row.contains("revision", Tag.TAG_LONG)
                || row.contains("owner") && !row.hasUUID("owner")) throw new IllegalArgumentException("malformed domain owner");
            StateDomainId domain = new StateDomainId(row.getString("domain"));
            OwnershipBinding binding = new OwnershipBinding(row.getLong("revision"), row.hasUUID("owner")
                ? new SubjectKey(subject.realmId(), row.getUUID("owner")) : null);
            if (ownership.putIfAbsent(domain, binding) != null) throw new IllegalArgumentException("duplicate ownership domain");
        }
        return new SubjectRevisions(tag.getLong("binding"), incarnation, tag.getLong("judgment"),
            tag.getLong("admission"), tag.getLong("revoked_through"), ownership);
    }
}
