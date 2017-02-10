package edu.gemini.shared.util;

import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * A least-recently used cache.
 */
public class LruCache<K, V> implements Map<K, V> {
    private final LinkedHashMap<K, V> del;

    /**
     * Creates the LRU cache with a specified maximum size.
     *
     * @param max the maximum number of elements that the cache will possibly
     * hold
     */
    public LruCache(final int max) {
        // A cache of 0 or less doesn't make sense.
        if (max <= 0) throw new IllegalArgumentException();

        // Compute an initialCapacity and load factor that corresponds to the
        // max.  Use the default 0.75 as the load factory.  Compute the
        // initial capacity. Want to choose an initial capacity large enough
        // that we won't be rehashing and increasing the capacity.  When it
        // fills, the hash map will have to hold max+1 elements temporarily,
        // so use max+1 in the calculations.

        // max+1 = initialCapacity * 0.75;
        // initialCapcity = (max+1)/0.75 ~= (max+1) * 1.334
        int initialCapacity = (int) Math.ceil((max+1) * 1.334);

        // Create a LinkedHashMap to delegate our calls to.  It will be sorted
        // in access order.  Implement the removeEldestEntry to kick out old
        // values.
        del = new LinkedHashMap<K, V>(initialCapacity, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<K, V> kvEntry) {
                return size() > max;
            }
        };
    }

    // Delegate everything.
    @Override public int size() { return del.size(); }
    @Override public boolean isEmpty() { return del.isEmpty(); }
    @Override public boolean containsKey(Object o) { return del.containsKey(o); }
    @Override public boolean containsValue(Object o) { return del.containsValue(o); }
    @Override public V get(Object o) { return del.get(o); }
    @Override public V put(K k, V v) { return del.put(k, v); }
    @Override public V remove(Object o) { return del.remove(o); }
    @Override public void putAll(Map<? extends K, ? extends V> map) { del.putAll(map); }
    @Override public void clear() { del.clear(); }
    @Override public Set<K> keySet() { return del.keySet(); }
    @Override public Collection<V> values() { return del.values(); }
    @Override public Set<Entry<K, V>> entrySet() { return del.entrySet(); }
}
