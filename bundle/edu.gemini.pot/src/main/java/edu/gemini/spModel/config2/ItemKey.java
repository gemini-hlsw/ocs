package edu.gemini.spModel.config2;

import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;

import java.io.Serializable;

/**
 * The (immutable) ItemKey is used to refer to either a single item, or a
 * collection of items.  For example the "instrument" ItemKey would refer to
 * all the items that make up an instrument, while "instrument:filter" refers
 * to a particular item. ItemKeys are hierarchical, with components of the key
 * separated by the {@link #SEPARATOR_CHAR}.
 *
 * <p>ItemKeys are comparable to <code>java.io.File</code> classes.  Files
 * may name either a directory, containing other files, or an individual File.
 * In the same way, a ItemKey may refer to a collection of ItemKeys or
 * a particular item.
 */
public final class ItemKey implements Comparable<ItemKey>, Serializable {
    public static final ItemKey[] EMPTY_ARRAY = new ItemKey[0];

    /**
     * The character used to separate config key names, ':'.
     */
    public static final char SEPARATOR_CHAR = ':';

    /**
     * String version of the {@link #SEPARATOR_CHAR separator character}.
     */
    public static final String SEPARATOR = ":";

    public static final String WILDCARD = "*";

    private final String _path;

    /**
     * Constructs with the given name or path.  A path is a single string
     * composed of multiple names separated by {@link #SEPARATOR_CHAR}
     * characters.
     *
     * @param path encoded hierarchy of config key names (or a single root
     * config key name); names may not contain the {@link #SEPARATOR_CHAR}
     * since this will be interpreted as a separator between two distinct
     * names
     */
    public ItemKey(String path) {
        if (path == null) throw new NullPointerException();
        _path = path;
    }

    /**
     * Constructs a new ItemKey using the given key as the parent.  The
     * <code>name</code> is typically a simple name, but may be its own
     * hierarchy of names separated by the {@link #SEPARATOR_CHAR}.
     *
     * @param parent parent ItemKey
     * @param name child (or encoded hierarchy of children)
     */
    public ItemKey(ItemKey parent, String name) {
        if (name == null) throw new NullPointerException();
        _path = parent.getPath() + SEPARATOR_CHAR + name;
    }

    /**
     * Gets the parent of this ItemKey, if any.
     *
     * @return parent ItemKey if any; <code>null</code> if there is no
     * parent of this ItemKey
     */
    public ItemKey getParent() {
        int index = _path.lastIndexOf(SEPARATOR_CHAR);
        if (index < 0) return null;
        return new ItemKey(_path.substring(0, index));
    }

    public ItemKey getRoot() {
        ItemKey parent = getParent();
        return (parent == null) ? this : parent.getRoot();
    }

    /**
     * Gets the name of this ItemKey, the last name in the sequence of
     * config names.
     *
     * @return name of the item or collection of items denoted by this
     * ItemKey
     */
    public String getName() {
        int index = _path.lastIndexOf(SEPARATOR_CHAR);
        if (index < 0) return _path;
        return _path.substring(index + 1);
    }

    /**
     * Gets the ItemKey encoded as a single path, or sequence, of names
     * separated by the {@link #SEPARATOR_CHAR}.
     *
     * @return sequence of names that make up this ItemKey, where each name
     * is separated by the {@link #SEPARATOR_CHAR}
     */
    public String getPath() {
        return _path;
    }

    public ImList<String> splitPath() {
        return DefaultImList.create(_path.split(SEPARATOR));
    }

    @Override
    public int compareTo(ItemKey that) {
        return getPath().compareTo(that.getPath());
    }

    public boolean equals(Object other) {
        if (!(other instanceof ItemKey)) return false;
        ItemKey that = (ItemKey) other;
        return _path.equals(that._path);
    }

    public int hashCode() {
        return _path.hashCode();
    }

    public String toString() {
        return _path;
    }

    /**
     * Implements a simplistic match on path names.  The two items have to
     * have the same length or they don't match.  All path elements have to have
     * the same values, or one or both can be a wildcard.  For example:
     * <code>instrument:*</code> matches <code>instrument:disperser</code>.
     */
    public boolean matches(ItemKey that) {
        String[] thisPath = this.getPath().split(SEPARATOR);
        String[] thatPath = that.getPath().split(SEPARATOR);

        if (thisPath.length != thatPath.length) return false;

        for (int i=0; i<thisPath.length; ++i) {
            String thisElem = thisPath[i];
            String thatElem = thatPath[i];
            if (!thisElem.equals(thatElem) && !(WILDCARD.equals(thisElem) || WILDCARD.equals(thatElem))) {
                return false;
            }
        }
        return true;
    }

    /*
    public static PredicateOp<ItemKey> matcher(String pattern) {
        final Pattern pat = Pattern.compile(pattern);
        return new PredicateOp<ItemKey>() {
            @Override public Boolean apply(ItemKey itemKey) {
                return pat.matcher(itemKey.getPath()).matches();
            }
        };
    }
    */

    public boolean isParentOf(ItemKey that) {
        return that.getPath().startsWith(_path);
    }

    public int size() {
        return _path.split(SEPARATOR).length;
    }
}
