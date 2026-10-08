package dev.ronova.coremod.observation;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Short-lived discovery metadata only; never an ownership witness or write permission. */
final class ReflectionFieldLayout {
    private static final long MAX_AGE_NANOS = 1_000_000_000L;
    private static final ClassValue<Cache> CACHE = new ClassValue<>() {
        @Override protected Cache computeValue(Class<?> type) { return new Cache(); }
    };
    static final class View {
        private final Field[] fields;
        private final String shape;
        private View(Field[] fields) {
            this.fields = fields;
            var descriptor = new StringBuilder();
            for (Field field : fields) descriptor.append(field.getName().length()).append(':').append(field.getName())
                .append(':').append(field.getType().getName().length()).append(':').append(field.getType().getName())
                .append(':').append(field.getModifiers()).append(';');
            shape = shapeId(descriptor.toString());
        }
        int size() { return fields.length; }
        Field field(int index) { return fields[index]; }
        String shape() { return shape; }
    }
    private static final class Cache {
        private WeakReference<View> view = new WeakReference<>(null);
        private long created;
        synchronized View get(Class<?> type, long now) {
            var current = view.get(); long age = now - created;
            if (current != null && age >= 0 && age < MAX_AGE_NANOS) return current;
            Field[] fields = type.getDeclaredFields();
            Arrays.sort(fields, Comparator.comparing(Field::getName));
            var refreshed = new View(fields);
            view = new WeakReference<>(refreshed); created = now;
            return refreshed;
        }
    }
    private ReflectionFieldLayout() {}
    static View get(Class<?> type) { return get(type, System.nanoTime()); }
    static View get(Class<?> type, long now) { return CACHE.get(type).get(type, now); }
    /** Position invalidation only: this digest cannot authorize a write or attest installed code. */
    static String hierarchyShape(Class<?> type) {
        var descriptor = new StringBuilder();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass())
            descriptor.append(get(current).shape());
        return shapeId(descriptor.toString());
    }
    private static String shapeId(String descriptor) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(descriptor.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException unavailable) { throw new IllegalStateException("layout digest unavailable", unavailable); }
    }
}
