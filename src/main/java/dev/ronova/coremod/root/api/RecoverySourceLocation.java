package dev.ronova.coremod.root.api;

import java.util.List;
import java.util.Objects;

/** Portable lookup hints only. Resolving a class name never proves its loader, source ownership or write authority. */
public record RecoverySourceLocation(RecoverySnapshotPlan.StorageScope scope, String dimension, String storageName,
    String mapOwner, String mapField, String snapshotOwner, String snapshotField) {
    private static final String PREFIX = "ronova-source-location/1\n";
    public RecoverySourceLocation {
        Objects.requireNonNull(scope);
        for (String value : List.of(dimension, storageName, mapOwner, mapField, snapshotOwner, snapshotField)) {
            if (value.length() > 512 || value.chars().anyMatch(Character::isISOControl))
                throw new IllegalArgumentException("invalid source location component");
        }
        if (mapOwner.isEmpty() || mapField.isEmpty() || snapshotOwner.isEmpty() != snapshotField.isEmpty())
            throw new IllegalArgumentException("incomplete source field location");
        boolean loaded = scope == RecoverySnapshotPlan.StorageScope.LOADED_SAVED_DATA;
        if (loaded ? dimension.isEmpty() || storageName.isEmpty() : !dimension.isEmpty() || !storageName.isEmpty())
            throw new IllegalArgumentException("source location scope mismatch");
    }
    public boolean directRecord() { return snapshotField.isEmpty(); }
    public String canonical() {
        var result = new StringBuilder(PREFIX);
        for (String value : List.of(scope.name(), dimension, storageName, mapOwner, mapField, snapshotOwner, snapshotField))
            result.append(value.length()).append(':').append(value);
        return result.toString();
    }
    public static RecoverySourceLocation parse(String encoded) {
        if (encoded == null || encoded.length() > 4096 || !encoded.startsWith(PREFIX))
            throw new IllegalArgumentException("unsupported source location encoding");
        String[] parts = new String[7]; int cursor = PREFIX.length();
        for (int index = 0; index < parts.length; index++) {
            int colon = encoded.indexOf(':', cursor);
            if (colon < 0 || colon - cursor > 3) throw new IllegalArgumentException("invalid source location length");
            String length = encoded.substring(cursor, colon);
            if (!length.matches("0|[1-9][0-9]{0,2}")) throw new IllegalArgumentException("noncanonical source location length");
            int count = Integer.parseInt(length); cursor = colon + 1;
            if (count > 512 || count > encoded.length() - cursor) throw new IllegalArgumentException("truncated source location");
            parts[index] = encoded.substring(cursor, cursor + count); cursor += count;
        }
        if (cursor != encoded.length()) throw new IllegalArgumentException("trailing source location data");
        return new RecoverySourceLocation(RecoverySnapshotPlan.StorageScope.valueOf(parts[0]),
            parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]);
    }
    /** Preserve the existing locator hash so introducing recoverable metadata does not create new semantic slots. */
    public String locatorHash() {
        String holder = scope == RecoverySnapshotPlan.StorageScope.STATIC_MANAGER
            ? "static-nbt-record\n" + mapOwner + "#" + mapField
            : dimension + "/" + storageName + "\n" + mapOwner + "#" + mapField;
        return EffectRecord.hash(holder + "\n" + (directRecord() ? "direct" : snapshotOwner + "#" + snapshotField));
    }
}
