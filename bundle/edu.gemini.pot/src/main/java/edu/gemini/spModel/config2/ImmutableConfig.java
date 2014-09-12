//
// $Id: ImmutableConfig.java 38078 2011-10-18 15:15:29Z swalker $
//

package edu.gemini.spModel.config2;

import edu.gemini.shared.util.immutable.MapOp;

import java.util.Map;

/**
 * An immutable {@link Config} implementation.
 */
public final class ImmutableConfig implements Config {
    private Config _delegate;

    public ImmutableConfig(Config delegate) {
        if (delegate instanceof ImmutableConfig) {
            throw new IllegalArgumentException("cannot wrap with immutable config");
        }
        _delegate = delegate;
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean containsItem(ItemKey key) {
        return _delegate.containsItem(key);
    }

    public ItemEntry[] itemEntries() {
        return _delegate.itemEntries();
    }

    public ItemEntry[] itemEntries(ItemKey parent) {
        return _delegate.itemEntries(parent);
    }

    public boolean isEmpty() {
        return _delegate.isEmpty();
    }

    public ItemKey[] getKeys() {
        return _delegate.getKeys();
    }

    public ItemKey[] getKeys(ItemKey parent) {
        return _delegate.getKeys(parent);
    }

    public Object getItemValue(ItemKey key) {
        return _delegate.getItemValue(key);
    }

    public Config getAll(ItemKey parent) {
        return _delegate.getAll(parent);
    }

    public Config getAll(ItemKey[] parents) {
        return _delegate.getAll(parents);
    }

    public <K> Map<K, ItemEntry[]> groupBy(MapOp<ItemEntry, K> f) {
        return _delegate.groupBy(f);
    }

    public Object putItem(ItemKey key, Object item) {
        throw new UnsupportedOperationException();
    }

    public void putAll(Config config) {
        throw new UnsupportedOperationException();
    }

    public Object remove(ItemKey key) {
        throw new UnsupportedOperationException();
    }

    public void removeAll(ItemKey parent) {
        throw new UnsupportedOperationException();
    }

    public void removeAll(ItemKey[] parents) {
        throw new UnsupportedOperationException();
    }

    public void removeAll(Config config) {
        throw new UnsupportedOperationException();
    }

    public void retainAll(ItemKey parent) {
        throw new UnsupportedOperationException();
    }

    public void retainAll(ItemKey[] parents) {
        throw new UnsupportedOperationException();
    }

    public void retainAll(Config config) {
        throw new UnsupportedOperationException();
    }

    public boolean matches(Config config) {
        return _delegate.matches(config);
    }

    public int size() {
        return _delegate.size();
    }

    public boolean equals(Object other) {
        return _delegate.equals(other);
    }

    public int hashCode() {
        return _delegate.hashCode();
    }
}
