package net.minecraft.nbt;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Map-backed compile/test stub. It is never packaged into Ronova. */
public class CompoundTag extends Tag {
    private static final long serialVersionUID = 1L;
    private final Map<String, Object> values = new LinkedHashMap<>();

    public void put(String key, Tag value) { values.put(key, value); }
    public void putString(String key, String value) { values.put(key, Objects.toString(value, "")); }
    public void putLong(String key, long value) { values.put(key, value); }
    public void putInt(String key, int value) { values.put(key, value); }
    public void putDouble(String key, double value) { values.put(key, value); }
    public void putBoolean(String key, boolean value) { values.put(key, value); }
    public void putByteArray(String key, byte[] value) {
        values.put(key, value == null ? new byte[0] : value.clone());
    }
    public void putUUID(String key, UUID value) {
        values.put(key, Objects.requireNonNull(value, "value"));
    }

    public boolean contains(String key) { return values.containsKey(key); }
    public boolean contains(String key, int type) {
        return values.containsKey(key) && getTagType(key) == type;
    }
    public boolean hasUUID(String key) { return values.get(key) instanceof UUID; }

    public String getString(String key) {
        Object value = values.get(key);
        return value instanceof String string ? string : "";
    }
    public long getLong(String key) {
        Object value = values.get(key);
        return value instanceof Number number ? number.longValue() : 0L;
    }
    public int getInt(String key) {
        Object value = values.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }
    public double getDouble(String key) {
        Object value = values.get(key);
        return value instanceof Number number ? number.doubleValue() : 0.0D;
    }
    public boolean getBoolean(String key) {
        Object value = values.get(key);
        return value instanceof Boolean flag && flag;
    }
    public byte[] getByteArray(String key) {
        Object value = values.get(key);
        return value instanceof byte[] bytes ? bytes.clone() : new byte[0];
    }
    public UUID getUUID(String key) {
        Object value = values.get(key);
        if (!(value instanceof UUID uuid)) {
            throw new IllegalArgumentException("missing UUID: " + key);
        }
        return uuid;
    }
    public CompoundTag getCompound(String key) {
        Object value = values.get(key);
        return value instanceof CompoundTag compound ? compound : new CompoundTag();
    }
    public ListTag getList(String key, int ignoredElementType) {
        Object value = values.get(key);
        return value instanceof ListTag list ? list : new ListTag();
    }

    public byte getTagType(String key) {
        Object value = values.get(key);
        if (value == null) return TAG_END;
        if (value instanceof Boolean || value instanceof Byte) return TAG_BYTE;
        if (value instanceof Short) return TAG_SHORT;
        if (value instanceof Integer) return TAG_INT;
        if (value instanceof Long) return TAG_LONG;
        if (value instanceof Float) return TAG_FLOAT;
        if (value instanceof Double) return TAG_DOUBLE;
        if (value instanceof byte[]) return TAG_BYTE_ARRAY;
        if (value instanceof String) return TAG_STRING;
        if (value instanceof ListTag) return TAG_LIST;
        if (value instanceof CompoundTag) return TAG_COMPOUND;
        if (value instanceof UUID || value instanceof int[]) return TAG_INT_ARRAY;
        if (value instanceof long[]) return TAG_LONG_ARRAY;
        return TAG_END;
    }

    public void remove(String key) { values.remove(key); }

    @Override
    public CompoundTag copy() {
        CompoundTag copy = new CompoundTag();
        values.forEach((key, value) -> copy.values.put(key, copyValue(value)));
        return copy;
    }

    private static Object copyValue(Object value) {
        if (value instanceof Tag tag) return tag.copy();
        if (value instanceof byte[] bytes) return bytes.clone();
        if (value instanceof int[] ints) return ints.clone();
        if (value instanceof long[] longs) return longs.clone();
        return value;
    }
}
