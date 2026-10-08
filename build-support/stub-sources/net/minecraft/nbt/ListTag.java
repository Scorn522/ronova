package net.minecraft.nbt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ListTag extends Tag implements Iterable<Tag> {
    private static final long serialVersionUID = 1L;
    private final List<Tag> values = new ArrayList<>();

    public int size() {
        return values.size();
    }

    public Tag get(int index) {
        return values.get(index);
    }

    public Tag set(int index, Tag value) {
        return values.set(index, value);
    }

    public Tag remove(int index) {
        return values.remove(index);
    }

    public double getDouble(int index) {
        return 0.0D;
    }

    public boolean add(Object value) {
        if (!(value instanceof Tag tag)) {
            throw new IllegalArgumentException("ListTag accepts Tag values only");
        }
        return values.add(tag);
    }

    @Override
    public Iterator<Tag> iterator() {
        return values.iterator();
    }

    @Override
    public ListTag copy() {
        ListTag copy = new ListTag();
        for (Tag value : values) {
            copy.values.add(value == null ? null : value.copy());
        }
        return copy;
    }
}
