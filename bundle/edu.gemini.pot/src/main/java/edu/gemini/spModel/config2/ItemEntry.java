package edu.gemini.spModel.config2;

import java.io.Serializable;

/**
 * The (immutable) ItemEntry class groups an item value with its associated key.
 * It is used in methods of the {@link Config} class that return collections of
 * items such as {@link Config#itemEntries()}.  This class is comparable to
 * Map.Entry in form and purpose.
 */
public final class ItemEntry implements Comparable<ItemEntry>, Serializable {
    public static final ItemEntry[] EMPTY_ARRAY = new ItemEntry[0];

    private ItemKey _key;
    private Object _item;

    /**
     * Constructs with the key and the item value.
     */
    public ItemEntry(ItemKey key, Object item) {
        if (key == null) throw new NullPointerException("null ItemKey");
        _key = key;
        _item = item; // may be null
    }

    public ItemKey getKey() {
        return _key;
    }

    public Object getItemValue() {
        return _item;
    }

    @Override
    public int compareTo(ItemEntry that) {
        int res;

        res = _key.compareTo(that._key);
        if (res != 0) return res;

        if (_item == null) {
            if (that._item != null) res = -1;
        } else if (that._item == null) {
            res = 1;
        } else if (!_item.equals(that._item)) {
            // an arbitrary way to decide which comes first and keep the comparable
            // implementation consistent with equals
            int i1 = System.identityHashCode(_item);
            int i2 = System.identityHashCode(that._item);
            res = (i1 < i2) ? -1 : 1;
        }
        return res;
    }

    public boolean equals(Object other) {
        if (!(other instanceof ItemEntry)) return false;

        ItemEntry that = (ItemEntry) other;
        if (!_key.equals(that._key)) return false;

        if (_item == null) {
            return (that._item == null);
        }
        if (that._item == null) {
            return false;
        }
        return _item.equals(that._item);
    }

    public int hashCode() {
        return hashCode(_key, _item);
    }

    // provides a way to get the same hashCode that you would get from the
    // ItemEntry.hashCode() method without creating the ItemEntry
    static int hashCode(ItemKey key, Object item) {
        int res = key.hashCode();
        if (item != null) res = 37 * res + item.hashCode();
        return res;
    }

    @Override public String toString() {
        return String.valueOf(_key) + " => " + _item;
    }
}
