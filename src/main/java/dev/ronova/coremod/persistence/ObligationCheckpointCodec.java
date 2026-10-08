package dev.ronova.coremod.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.ronova.coremod.obligation.ObligationCheckpoint;
import dev.ronova.coremod.root.api.EffectRecord;
import java.nio.charset.StandardCharsets;
import net.minecraft.nbt.*;

/** Canonical bounded JSON inside the single Root NBT image, independently checksummed. */
final class ObligationCheckpointCodec {
    private static final Gson JSON=new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
    private static final int MAX_BYTES=2*1024*1024;
    private record LegacyCheckpoint(long revision, long clock, java.util.List<dev.ronova.coremod.obligation.ObligationRegistry.View> entries,
        dev.ronova.coremod.obligation.ObligationRegistry.Coverage coverage) {
        LegacyCheckpoint(ObligationCheckpoint c) { this(c.revision(), c.clock(), c.entries(), c.coverage()); }
        ObligationCheckpoint upgrade() { return new ObligationCheckpoint(revision, clock, entries, coverage); }
    }
    private record RestrictedCheckpoint(long revision, long clock, java.util.List<dev.ronova.coremod.obligation.ObligationRegistry.View> entries,
        dev.ronova.coremod.obligation.ObligationRegistry.Coverage coverage,
        dev.ronova.coremod.obligation.CandidateRestrictions restrictions) {
        RestrictedCheckpoint(ObligationCheckpoint c) { this(c.revision(), c.clock(), c.entries(), c.coverage(), c.restrictions()); }
        ObligationCheckpoint upgrade() { return new ObligationCheckpoint(revision, clock, entries, coverage, restrictions); }
    }
    static boolean hasOverflowDetail(ObligationCheckpoint checkpoint) {
        return !checkpoint.overflow().equals(dev.ronova.coremod.obligation.OverflowCheckpoint.legacy(checkpoint.coverage().overflowCount()));
    }
    private static Object image(ObligationCheckpoint checkpoint, int schema) {
        return switch(schema) {
            case 1 -> new LegacyCheckpoint(checkpoint);
            case 2 -> new RestrictedCheckpoint(checkpoint);
            case 3 -> checkpoint;
            default -> throw new IllegalArgumentException("invalid obligation checkpoint schema");
        };
    }
    static CompoundTag encode(ObligationCheckpoint checkpoint) {
        int schema = hasOverflowDetail(checkpoint) ? 3 : checkpoint.restrictions().revision() == 0 ? 1 : 2;
        String payload=JSON.toJson(image(checkpoint,schema));checkSize(payload);
        CompoundTag tag=new CompoundTag();tag.putInt("schema",schema);tag.putString("payload",payload);tag.putString("hash",EffectRecord.hash(payload));return tag;
    }
    static ObligationCheckpoint decode(CompoundTag tag) {
        if(!tag.contains("schema",Tag.TAG_INT)||tag.getInt("schema")<1||tag.getInt("schema")>3||!tag.contains("payload",Tag.TAG_STRING)||!tag.contains("hash",Tag.TAG_STRING))
            throw new IllegalArgumentException("invalid obligation checkpoint schema");
        String payload=tag.getString("payload");checkSize(payload);
        if(!EffectRecord.hash(payload).equals(tag.getString("hash")))throw new IllegalArgumentException("obligation checkpoint hash mismatch");
        int schema=tag.getInt("schema");
        ObligationCheckpoint result=switch(schema) {
            case 1 -> JSON.fromJson(payload,LegacyCheckpoint.class).upgrade();
            case 2 -> JSON.fromJson(payload,RestrictedCheckpoint.class).upgrade();
            case 3 -> JSON.fromJson(payload,ObligationCheckpoint.class);
            default -> throw new IllegalArgumentException("invalid obligation checkpoint schema");
        };
        if(result==null||result.revision()==0&&!result.equals(ObligationCheckpoint.EMPTY)
            ||!payload.equals(JSON.toJson(image(result,schema)))
            || schema==2 && result.restrictions().revision()==0
            || (schema==3)!=hasOverflowDetail(result))throw new IllegalArgumentException("non-canonical obligation checkpoint");
        return result;
    }
    private static void checkSize(String payload) {
        if(payload.length()>MAX_BYTES||payload.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES)throw new IllegalArgumentException("obligation checkpoint size limit");
    }
}
