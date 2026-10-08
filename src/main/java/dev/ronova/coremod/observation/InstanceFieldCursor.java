package dev.ronova.coremod.observation;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** One field-discovery step per visit, including skipped fields; no retained reflection Field list. */
public final class InstanceFieldCursor {
    public record Step(Field field, boolean complete, boolean unavailable) {}
    private WeakReference<Class<?>> root = new WeakReference<>(null), current = new WeakReference<>(null);
    private int position;
    private boolean complete;
    private String shape;
    public void clear() { root.clear(); current.clear(); position = 0; complete = false; shape = null; }
    public Step next(Class<?> type) {
        if (root.get() != type) {
            clear(); root = new WeakReference<>(type); current = new WeakReference<>(type);
        }
        if (complete) return new Step(null, true, false);
        Class<?> visiting = current.get();
        if (visiting == null || visiting == Object.class) {
            complete = true; return new Step(null, true, visiting == null);
        }
        try {
            var fields = ReflectionFieldLayout.get(visiting);
            if (!fields.shape().equals(shape)) { shape = fields.shape(); position = 0; }
            if (position >= fields.size()) {
                current = new WeakReference<>(visiting.getSuperclass()); position = 0; shape = null;
                return new Step(null, false, false);
            }
            Field field = fields.field(position++);
            if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive() || field.isSynthetic())
                return new Step(null, false, false);
            return field.trySetAccessible() ? new Step(field, false, false) : new Step(null, false, true);
        } catch (RuntimeException | LinkageError unavailable) {
            current = new WeakReference<>(visiting.getSuperclass()); position = 0; shape = null;
            return new Step(null, false, true);
        }
    }
}
