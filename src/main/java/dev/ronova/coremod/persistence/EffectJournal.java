package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.RealmId;
import dev.ronova.coremod.contract.SubjectKey;
import dev.ronova.coremod.root.api.EffectKey;
import dev.ronova.coremod.root.api.EffectRecord;
import dev.ronova.coremod.root.api.TargetEpoch;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** A component of the one Root image. No independent file, scheduler or Authority writer. */
final class EffectJournal {
    private static final int MAX_ROWS = 1_000_000;
    private final Map<EffectKey, EffectRecord> records = new LinkedHashMap<>();
    private final Map<EffectKey, dev.ronova.coremod.root.api.RecoverySourceLocation> locations = new LinkedHashMap<>();
    private final Map<EffectKey,dev.ronova.coremod.root.api.RecoveryWorkDescriptor> work = new LinkedHashMap<>();
    private final Map<String,EffectKey> workAliases=new java.util.HashMap<>();
    private final List<EffectKey> order = new ArrayList<>();
    private long unresolved;
    private long sourceQualifications;
    private long recoveryMutations;

    EffectRecord get(EffectKey key) { return records.get(key); }
    int size() { return records.size(); }
    long unresolved() { return unresolved; }
    boolean hasSourceQualifications() { return sourceQualifications > 0; }
    boolean hasRecoveryMutations() { return recoveryMutations > 0; }
    boolean hasLocations() { return !locations.isEmpty(); }
    boolean hasRecoveryWork(){return !work.isEmpty();}
    boolean hasStorageFileIdentity(){return work.values().stream().anyMatch(value->!value.fileIdentity().isEmpty());}
    boolean hasStorageGeneration(){return work.values().stream().anyMatch(value->value.storageGeneration()!=null);}
    dev.ronova.coremod.root.api.RecoveryWorkDescriptor work(EffectKey key){return work.get(key);}
    private static String workAlias(EffectKey key,dev.ronova.coremod.root.api.RecoveryWorkDescriptor description) {
        return EffectRecord.hash(key.canonicalCommandId()+"\n"+key.targetEpoch()+"\n"+description.canonical());
    }
    EffectKey workAliasKey(EffectKey key,dev.ronova.coremod.root.api.RecoveryWorkDescriptor description){return workAliases.get(workAlias(key,description));}
    void describeWork(EffectKey key,dev.ronova.coremod.root.api.RecoveryWorkDescriptor description) {
        var record=records.get(key);
        if(record==null||!description.matches(key)||!description.payloadHash(key,record.authorityPayloadHash()).equals(record.effectPayloadHash()))
            throw new IllegalArgumentException("recovery work description mismatch");
        var prior=work.get(key);if(prior!=null&&!prior.equals(description))throw new IllegalArgumentException("recovery work description changed");
        description.validateFacts(record);
        String alias=workAlias(key,description);EffectKey existing=workAliases.get(alias);
        if(existing!=null&&!existing.equals(key))throw new IllegalArgumentException("duplicate recovery work semantic alias");
        work.put(key,description);
        workAliases.put(alias,key);
    }
    dev.ronova.coremod.root.api.RecoverySourceLocation location(EffectKey key) { return locations.get(key); }
    void locate(EffectKey key, dev.ronova.coremod.root.api.RecoverySourceLocation location) {
        dev.ronova.coremod.root.api.RecoveryMutationIntent.depth(key);
        String[] slot = key.semanticSlotId().split("/");
        boolean loaded = slot[3].equals("2") || slot[3].equals("3");
        if (!records.containsKey(key) || !location.locatorHash().equals(slot[1])
            || loaded != (location.scope() == dev.ronova.coremod.root.api.RecoverySnapshotPlan.StorageScope.LOADED_SAVED_DATA)
            || (slot[3].equals("0") || slot[3].equals("3")) && !location.directRecord())
            throw new IllegalArgumentException("source location does not match its mutation intent");
        var prior = locations.get(key);
        if (prior != null && !prior.equals(location)) throw new IllegalArgumentException("source location conflict");
        locations.put(key, location);
    }
    long reservedGrowthBytes() { return Math.multiplyExact(unresolved, EffectRecord.RECOVERY_GROWTH_BYTES); }
    List<EffectRecord> snapshot() { return List.copyOf(records.values()); }
    List<EffectRecord> page(int offset, int limit) {
        if(offset<0||limit<1||limit>64)throw new IllegalArgumentException("invalid effect page");
        List<EffectRecord> result=new ArrayList<>();
        for(int i=offset;i<order.size()&&result.size()<limit;i++)result.add(records.get(order.get(i)));
        return List.copyOf(result);
    }

    EffectJournal copy() {
        EffectJournal copy = new EffectJournal();
        copy.records.putAll(records);
        copy.locations.putAll(locations);
        copy.work.putAll(work);
        copy.workAliases.putAll(workAliases);
        copy.order.addAll(order);
        copy.unresolved = unresolved;
        copy.sourceQualifications = sourceQualifications;
        copy.recoveryMutations = recoveryMutations;
        return copy;
    }

    void put(EffectRecord record) {
        if(record.state()==EffectRecord.State.PREPARED&&record.revision()!=1
            ||record.state()==EffectRecord.State.EFFECT_UNKNOWN&&record.revision()<2)
            throw new IllegalArgumentException("effect state lacks its preparation revision");
        EffectRecord prior = records.get(record.key());
        if(dev.ronova.coremod.root.api.RecoveryWorkDescriptor.OPERATION.equals(record.key().semanticOperationId())) {
            long facts=dev.ronova.coremod.root.api.RecoveryWorkResult.facts(record);
            var description=work.get(record.key());if(description!=null)description.validateFacts(record);
            if(prior!=null&&(facts&dev.ronova.coremod.root.api.RecoveryWorkResult.facts(prior))!=dev.ronova.coremod.root.api.RecoveryWorkResult.facts(prior))
                throw new IllegalArgumentException("recovery work observation rollback");
        }
        if (dev.ronova.coremod.root.api.RecoveryMutationIntent.OPERATION.equals(record.key().semanticOperationId())) {
            int observed = dev.ronova.coremod.root.api.RecoveryMutationIntent.observationMask(record);
            if (prior != null) {
                int retained = dev.ronova.coremod.root.api.RecoveryMutationIntent.observationMask(prior);
                if ((observed & retained) != retained)
                    throw new IllegalArgumentException("memory mutation observation rollback");
            }
        }
        if (prior == null && records.size() >= MAX_ROWS) throw new IllegalStateException("effect journal row limit exceeded");
        if (prior != null && (!prior.effectPayloadHash().equals(record.effectPayloadHash())
            || !prior.authorityPayloadHash().equals(record.authorityPayloadHash())
            || prior.authorityCommitIndex() != record.authorityCommitIndex()
            || prior.policyRevision() != record.policyRevision()
            || !prior.planId().equals(record.planId())
            || prior.planRevision() != record.planRevision()
            || !prior.stepId().equals(record.stepId())
            || prior.recoveryCapability() != record.recoveryCapability()
            || prior.sideEffectClass() != record.sideEffectClass()
            || prior.state().confirmed()
            || record.state() == EffectRecord.State.PREPARED
            || !prior.attemptId().equals(record.attemptId()) || prior.revision()==Long.MAX_VALUE
            || record.revision() != prior.revision()+1)) {
            throw new IllegalArgumentException("effect identity/payload/attempt conflict or revision rollback");
        }
        if(prior==null)order.add(record.key());
        if(prior==null && EffectRecord.SOURCE_QUALIFICATION.equals(record.key().semanticOperationId())) sourceQualifications++;
        if(prior==null && dev.ronova.coremod.root.api.RecoveryMutationIntent.OPERATION.equals(record.key().semanticOperationId())) recoveryMutations++;
        records.put(record.key(), record);
        unresolved += (record.unresolved() ? 1 : 0) - (prior != null && prior.unresolved() ? 1 : 0);
    }

    CompoundTag encode() {
        List<EffectRecord> sorted = new ArrayList<>(records.values());
        sorted.sort(Comparator.comparing(value -> value.key().canonical()));
        CompoundTag result = new CompoundTag();
        result.putInt("schema", hasStorageGeneration()?5:hasStorageFileIdentity()?4:hasRecoveryWork()?3:hasLocations()?2:1);
        ListTag rows = new ListTag();
        StringBuilder canonical = new StringBuilder();
        for (EffectRecord record : sorted) {
            CompoundTag row = encodeRecord(record);
            rows.add(row);
            canonical.append(row.getString("record_hash")).append('\n');
            var location = locations.get(record.key());
            if (location != null) {
                row.putString("source_location", location.canonical());
                row.putString("source_location_hash", EffectRecord.hash(location.canonical()));
                canonical.append(row.getString("source_location_hash")).append('\n');
            }
            var description=work.get(record.key());
            if(description!=null) {
                row.putString("work_description",description.canonical());
                String hash=EffectRecord.hash(description.canonical());row.putString("work_description_hash",hash);canonical.append(hash).append('\n');
            } else if(dev.ronova.coremod.root.api.RecoveryWorkDescriptor.OPERATION.equals(record.key().semanticOperationId()))
                throw new IllegalStateException("recovery work is missing its description");
        }
        result.put("records", rows);
        result.putString("hash", EffectRecord.hash(canonical.toString()));
        return result;
    }

    static EffectJournal decode(CompoundTag tag, RealmId realm, RootCommandJournal commands) {
        if (!tag.contains("schema", Tag.TAG_INT) || tag.getInt("schema") < 1 || tag.getInt("schema") > 5
            || !tag.contains("records", Tag.TAG_LIST) || !tag.contains("hash", Tag.TAG_STRING)) {
            throw new IllegalArgumentException("malformed effect journal");
        }
        ListTag rows = tag.getList("records", Tag.TAG_COMPOUND);
        if (!(tag.get("records") instanceof ListTag original) || original.size() != rows.size() || rows.size() > MAX_ROWS) {
            throw new IllegalArgumentException("malformed effect journal rows");
        }
        EffectJournal result = new EffectJournal();
        StringBuilder canonical = new StringBuilder();
        String previous = null;
        java.util.Set<String> lifecycleSlots = new java.util.HashSet<>();
        for (Tag raw : rows) {
            EffectRecord record = decodeRecord((CompoundTag) raw);
            String key = record.key().canonical();
            if (previous != null && previous.compareTo(key) >= 0) throw new IllegalArgumentException("unordered/duplicate effect key");
            if (!record.key().targetEpoch().subjectKey().realmId().equals(realm) || !commands.ownsEffectAuthority(record)) {
                throw new IllegalArgumentException("effect record lacks its original committed qualification");
            }
            boolean publicDeath = EffectRecord.PUBLIC_DEATH.equals(record.key().semanticOperationId());
            boolean terminal = EffectRecord.TERMINAL_LIFECYCLE.equals(record.key().semanticOperationId());
            boolean sourceQualification = EffectRecord.SOURCE_QUALIFICATION.equals(record.key().semanticOperationId());
            boolean recoveryMutation = dev.ronova.coremod.root.api.RecoveryMutationIntent.OPERATION.equals(record.key().semanticOperationId());
            boolean recoveryWork=dev.ronova.coremod.root.api.RecoveryWorkDescriptor.OPERATION.equals(record.key().semanticOperationId());
            var row=(CompoundTag)raw;
            dev.ronova.coremod.root.api.RecoveryWorkDescriptor description=null;
            if(recoveryWork) {
                if(tag.getInt("schema")<3||!row.contains("work_description",Tag.TAG_STRING)||!row.contains("work_description_hash",Tag.TAG_STRING))
                    throw new IllegalArgumentException("recovery work schema/description missing");
                description=dev.ronova.coremod.root.api.RecoveryWorkDescriptor.parse(row.getString("work_description"));
                if(!description.matches(record.key())||!EffectRecord.hash(description.canonical()).equals(row.getString("work_description_hash"))
                    ||record.recoveryCapability()!=EffectRecord.RecoveryCapability.QUERYABLE||record.sideEffectClass()!=EffectRecord.SideEffectClass.EXTERNAL_OPAQUE
                    ||record.state().confirmed()||!record.planId().equals(record.key().canonicalCommandId())||!record.stepId().equals("recovery-work")
                    ||record.planRevision()<record.authorityCommitIndex()||record.planRevision()>commands.commitIndex()
                    ||!description.payloadHash(record.key(),record.authorityPayloadHash()).equals(record.effectPayloadHash()))
                    throw new IllegalArgumentException("invalid recovery work contract");
                var configuration=commands.policyAt(record.planRevision());
                var policy=dev.ronova.coremod.policy.StrengthPolicySnapshot.fromConfiguration(configuration);
                var decision=description.kind()==dev.ronova.coremod.root.api.RecoveryWorkDescriptor.Kind.STORAGE_COMMIT
                    ?dev.ronova.coremod.policy.RecoveryChainPolicy.registeredStorageCommit(policy,description.copyDepth(),true)
                    :dev.ronova.coremod.policy.RecoveryChainPolicy.registeredTaskDisposition(policy,description.permissionDepth(),true);
                if(configuration.revision().value()!=record.policyRevision()||!decision.executionPermitted())throw new IllegalArgumentException("recovery work lacks historical permission");
            } else if(row.contains("work_description")||row.contains("work_description_hash"))throw new IllegalArgumentException("description on a different semantic operation");
            if (recoveryMutation) {
                int depth = dev.ronova.coremod.root.api.RecoveryMutationIntent.depth(record.key());
                if (record.recoveryCapability() != EffectRecord.RecoveryCapability.QUERYABLE
                    || record.sideEffectClass() != EffectRecord.SideEffectClass.EXTERNAL_OPAQUE
                    || record.state().confirmed()
                    || !record.planId().equals(record.key().canonicalCommandId())
                    || !record.stepId().equals("recovery-memory-mutation")
                    || record.planRevision() < record.authorityCommitIndex() || record.planRevision() > commands.commitIndex()
                    || !record.effectPayloadHash().equals(dev.ronova.coremod.root.api.RecoveryMutationIntent.payloadHash(
                        record.key(), record.authorityPayloadHash())))
                    throw new IllegalArgumentException("recovery mutation contract is inconsistent");
                var policy = commands.policyAt(record.planRevision());
                if (policy.revision().value() != record.policyRevision()
                    || !dev.ronova.coremod.policy.RecoveryChainPolicy.registeredAtomicSnapshot(
                        dev.ronova.coremod.policy.StrengthPolicySnapshot.fromConfiguration(policy), depth, true).executionPermitted())
                    throw new IllegalArgumentException("recovery mutation lacks historical policy permission");
            } else if (sourceQualification) {
                if (record.recoveryCapability() != EffectRecord.RecoveryCapability.IDEMPOTENT
                    || record.sideEffectClass() != EffectRecord.SideEffectClass.RONOVA_CONTROLLED
                    || record.state() != EffectRecord.State.EFFECT_CONFIRMED
                    || record.revision() != 1
                    || !record.planId().equals(record.key().canonicalCommandId())
                    || !record.stepId().equals("source-qualification") || record.planRevision() < record.authorityCommitIndex()
                    || record.planRevision() > commands.commitIndex()
                    || !record.effectPayloadHash().equals(EffectRecord.sourceQualificationPayloadHash(record.key(), record.authorityPayloadHash())))
                    throw new IllegalArgumentException("source qualification contract is inconsistent");
                var policy = commands.policyAt(record.planRevision());
                if (policy.revision().value() != record.policyRevision()
                    || !dev.ronova.coremod.policy.RecoveryChainPolicy.registeredNbtOutlet(
                        dev.ronova.coremod.policy.StrengthPolicySnapshot.fromConfiguration(policy), 1, true).executionPermitted())
                    throw new IllegalArgumentException("source qualification lacks its historical policy permission");
            } else if (!recoveryWork&&((!publicDeath && !terminal)
                || !EffectRecord.ONCE_SLOT.equals(record.key().semanticSlotId())
                || record.recoveryCapability() != EffectRecord.RecoveryCapability.OPAQUE
                || record.sideEffectClass() != EffectRecord.SideEffectClass.EXTERNAL_OPAQUE
                || !record.effectPayloadHash().equals(publicDeath ? EffectRecord.publicDeathPayloadHash(record.key(), record.authorityPayloadHash())
                    : EffectRecord.terminalPayloadHash(record.key(), record.authorityPayloadHash())))) {
                throw new IllegalArgumentException("effect semantic contract is unsupported");
            }
            if (terminal && (!record.planId().equals(record.key().canonicalCommandId()) || !record.stepId().equals("terminal-lifecycle")
                || record.planRevision() < record.authorityCommitIndex() || record.planRevision() > commands.commitIndex()))
                throw new IllegalArgumentException("terminal effect plan provenance is inconsistent");
            if(publicDeath&&(!record.planId().equals(record.key().canonicalCommandId())
                ||!record.stepId().equals("public-death")||record.planRevision()!=1))
                throw new IllegalArgumentException("public death effect plan provenance is inconsistent");
            String slot = record.key().canonicalCommandId() + "/" + record.key().targetEpoch().subjectKey().canonical()
                + "/" + record.key().targetEpoch().generation();
            if (!sourceQualification && !recoveryMutation && !recoveryWork && !lifecycleSlots.add(slot)) throw new IllegalArgumentException("legacy/new lifecycle aliases duplicated one effect slot");
            canonical.append(EffectRecord.hash(record.canonical())).append('\n');
            result.put(record);
            if (row.contains("source_location") || row.contains("source_location_hash")) {
                if (tag.getInt("schema") < 2 || !row.contains("source_location", Tag.TAG_STRING)
                    || !row.contains("source_location_hash", Tag.TAG_STRING))
                    throw new IllegalArgumentException("source location schema mismatch");
                var location = dev.ronova.coremod.root.api.RecoverySourceLocation.parse(row.getString("source_location"));
                String hash = EffectRecord.hash(location.canonical());
                if (!hash.equals(row.getString("source_location_hash"))) throw new IllegalArgumentException("source location hash mismatch");
                result.locate(record.key(), location); canonical.append(hash).append('\n');
            }
            if(description!=null){result.describeWork(record.key(),description);canonical.append(EffectRecord.hash(description.canonical())).append('\n');}
            previous = key;
        }
        if (!EffectRecord.hash(canonical.toString()).equals(tag.getString("hash"))) throw new IllegalArgumentException("effect journal hash mismatch");
        if((tag.getInt("schema")==5)!=result.hasStorageGeneration()||(tag.getInt("schema")>=4)!=result.hasStorageFileIdentity()||tag.getInt("schema")>=3&&!result.hasRecoveryWork()||tag.getInt("schema")<3&&result.hasRecoveryWork()
            ||tag.getInt("schema")==2&&!result.hasLocations()||tag.getInt("schema")==1&&result.hasLocations())throw new IllegalArgumentException("effect metadata/schema mismatch");
        return result;
    }

    boolean recoverPrepared() {
        boolean changed = false;
        for (EffectRecord record : snapshot()) {
            if (record.state() == EffectRecord.State.PREPARED) {
                put(record.unknown("restart-prepared-effect-unknown"));
                changed = true;
            }
        }
        return changed;
    }

    private static CompoundTag encodeRecord(EffectRecord record) {
        CompoundTag row = new CompoundTag();
        EffectKey key = record.key();
        row.putString("realm_id", key.targetEpoch().subjectKey().realmId().canonical());
        row.putUUID("subject_id", key.targetEpoch().subjectKey().subjectId());
        row.putLong("target_generation", key.targetEpoch().generation());
        row.putUUID("command_id", key.canonicalCommandId());
        row.putString("operation", key.semanticOperationId());
        row.putString("slot", key.semanticSlotId());
        row.putString("authority_hash", record.authorityPayloadHash());
        row.putLong("authority_commit_index", record.authorityCommitIndex());
        row.putString("payload_hash", record.effectPayloadHash());
        row.putLong("policy_revision", record.policyRevision());
        row.putUUID("plan_id", record.planId());
        row.putLong("plan_revision", record.planRevision());
        row.putString("step_id", record.stepId());
        row.putUUID("attempt_id", record.attemptId());
        row.putString("side_effect_class", record.sideEffectClass().name());
        row.putString("recovery_capability", record.recoveryCapability().name());
        row.putString("state", record.state().name());
        row.putLong("revision", record.revision());
        row.putString("evidence_ref", record.evidenceRef());
        row.putString("record_hash", EffectRecord.hash(record.canonical()));
        return row;
    }

    private static EffectRecord decodeRecord(CompoundTag row) {
        for (String key : List.of("subject_id", "command_id", "plan_id", "attempt_id")) {
            if (!row.hasUUID(key)) throw new IllegalArgumentException("effect UUID is missing");
        }
        for (String key : List.of("target_generation", "authority_commit_index", "policy_revision", "plan_revision", "revision")) {
            if (!row.contains(key, Tag.TAG_LONG)) throw new IllegalArgumentException("effect revision is missing");
        }
        for (String key : List.of("realm_id", "operation", "slot", "authority_hash", "payload_hash", "step_id",
            "side_effect_class", "recovery_capability", "state", "evidence_ref", "record_hash")) {
            if (!row.contains(key, Tag.TAG_STRING)) throw new IllegalArgumentException("effect field is missing");
        }
        EffectKey key = new EffectKey(row.getUUID("command_id"), new TargetEpoch(new SubjectKey(
            RealmId.fromString(row.getString("realm_id")), row.getUUID("subject_id")), row.getLong("target_generation")),
            row.getString("operation"), row.getString("slot"));
        EffectRecord record = new EffectRecord(key, row.getString("authority_hash"), row.getLong("authority_commit_index"),
            row.getString("payload_hash"), row.getLong("policy_revision"), row.getUUID("plan_id"), row.getLong("plan_revision"),
            row.getString("step_id"), row.getUUID("attempt_id"), EffectRecord.SideEffectClass.valueOf(row.getString("side_effect_class")),
            EffectRecord.RecoveryCapability.valueOf(row.getString("recovery_capability")), EffectRecord.State.valueOf(row.getString("state")),
            row.getLong("revision"), row.getString("evidence_ref"));
        if (!EffectRecord.hash(record.canonical()).equals(row.getString("record_hash"))) throw new IllegalArgumentException("effect record hash mismatch");
        return record;
    }
}
