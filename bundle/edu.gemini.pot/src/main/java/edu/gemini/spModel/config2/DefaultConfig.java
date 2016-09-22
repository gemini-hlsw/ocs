package edu.gemini.spModel.config2;

import edu.gemini.shared.util.immutable.MapOp;

import java.util.*;
import java.io.Serializable;

/**
 * A straightforward, mutable implementation of {@link Config}.
 *
 * <p>Not MT-safe, access must be synchronized by the caller if multiple
 * threads will use the same Config instance.
 */
public final class DefaultConfig implements Config, Serializable {

    public static final Config[] EMPTY_ARRAY = new DefaultConfig[0];

    private TreeMap<ItemKey, Object> _configMap = new TreeMap<>();

    /**
     * Constructs an empty Config object.
     */
    public DefaultConfig() {
    }

    /**
     * Constructs a Config object which contains all the items in the given
     * Config.  The item values themselves are <em>not</em> copied.
     *
     * @param copy Config whose items should be added to this Config
     */
    public DefaultConfig(Config copy) {
        if (copy instanceof DefaultConfig) {
            _configMap.putAll(((DefaultConfig) copy)._configMap);
        } else {
            ItemEntry[] entries = copy.itemEntries();
            for (ItemEntry ie : entries) {
                _configMap.put(ie.getKey(), ie.getItemValue());
            }
        }
    }

    private DefaultConfig(DefaultConfig copy, ItemKey parent) {
        _configMap.putAll(copy._subMap(parent));
    }

    /**
     * Constructs a Config that contains all the items indicated by the given
     * set of {@link ItemEntry}s.
     *
     * @param entries {@link ItemEntry}s that indicate the initial state of
     * the Config
     */
    public DefaultConfig(ItemEntry[] entries) {
        for (ItemEntry ie : entries) {
            _configMap.put(ie.getKey(), ie.getItemValue());
        }
    }

    public void clear() {
        _configMap.clear();
    }


    public boolean containsItem(ItemKey key) {
        return _configMap.containsKey(key);
    }


    public ItemEntry[] itemEntries() {
        return _itemEntries(_configMap);
    }


    public ItemEntry[] itemEntries(ItemKey parent) {
        return _itemEntries(_subMap(parent));
    }

    private ItemEntry[] _itemEntries(SortedMap<ItemKey, Object> map) {
        int i = 0;
        ItemEntry[] res = new ItemEntry[map.size()];
        for (Map.Entry<ItemKey, Object> me : map.entrySet()) {
            res[i++] = new ItemEntry(me.getKey(), me.getValue());
        }
        return res;
    }


    public boolean isEmpty() {
        return _configMap.isEmpty();
    }


    public ItemKey[] getKeys() {
        return _getKeys(_configMap);
    }

    public ItemKey[] getKeys(ItemKey parent) {
        return _getKeys(_subMap(parent));
    }

    private ItemKey[] _getKeys(SortedMap<ItemKey, Object> map) {
        //noinspection ToArrayCallWithZeroLengthArrayArgument
        return map.keySet().toArray(ItemKey.EMPTY_ARRAY);
    }


    public Object getItemValue(ItemKey key) {
        return _configMap.get(key);
    }

    public Config getAll(ItemKey parent) {
        return new DefaultConfig(this, parent);
    }


    public Config getAll(ItemKey[] parents) {
        DefaultConfig res = new DefaultConfig();
        for (ItemKey parent : parents) {
            res._configMap.putAll(_subMap(parent));
        }
        return res;
    }

    public <K> Map<K, ItemEntry[]> groupBy(MapOp<ItemEntry, K> f) {
        Map<K, Config> tmp = new HashMap<>();

        for (ItemEntry ie : itemEntries()) {
            K key = f.apply(ie);
            Config c = tmp.get(key);
            if (c == null) {
                c = new DefaultConfig();
                tmp.put(key, c);
            }
            c.putItem(ie.getKey(), ie.getItemValue());
        }

        // Map the Configs to ItemEntry[].
        Map<K, ItemEntry[]> res = new HashMap<>();
        for (Map.Entry<K, Config> me : tmp.entrySet()) {
            res.put(me.getKey(), me.getValue().itemEntries());
        }
        return res;
    }

    public Object putItem(ItemKey key, Object item) {
        return _configMap.put(key, item);
    }


    public void putAll(Config config) {
        if (config instanceof DefaultConfig) {
            _configMap.putAll(((DefaultConfig) config)._configMap);
        } else {
            ItemEntry[] entries = config.itemEntries();
            for (ItemEntry ie : entries) {
                _configMap.put(ie.getKey(), ie.getItemValue());
            }
        }
    }


    public Object remove(ItemKey key) {
        return _configMap.remove(key);
    }


    public void removeAll(ItemKey parent) {
        _subMap(parent).clear();
    }

    public void removeAll(ItemKey[] parents) {
        for (ItemKey parent : parents) {
            _subMap(parent).clear();
        }
    }


    public void removeAll(Config config) {
        if (config instanceof DefaultConfig) {
            _configMap.entrySet().removeAll(((DefaultConfig) config)._configMap.entrySet());
        } else {
            ItemEntry[] entries = config.itemEntries();
            for (ItemEntry ie : entries) {
                ItemKey key = ie.getKey();
                if (ie.getItemValue().equals(_configMap.get(key))) {
                    _configMap.remove(key);
                }
            }
        }
    }

    public void retainAll(ItemKey parent) {
        _configMap = new TreeMap<>(_subMap(parent));
    }


    public void retainAll(ItemKey[] parents) {
        TreeMap<ItemKey, Object> newmap = new TreeMap<>();
        for (ItemKey parent : parents) {
            newmap.putAll(_subMap(parent));
        }
        _configMap = newmap;
    }

    public void retainAll(Config config) {
        if (config instanceof DefaultConfig) {
            _configMap.entrySet().retainAll(((DefaultConfig) config)._configMap.entrySet());
        } else {
            for (Iterator<Map.Entry<ItemKey, Object>> it=_configMap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<ItemKey, Object> me = it.next();
                ItemKey key = me.getKey();
                Object val  = me.getValue();
                if (!val.equals(config.getItemValue(key))) {
                    it.remove();
                }
            }
        }
    }

    public boolean matches(Config config) {

        // two version of this in order to avoid the object creation required
        // by the itemEntries() method if possible

        if (config instanceof DefaultConfig) {
            for (Map.Entry<ItemKey, Object> o : ((DefaultConfig) config)._configMap.entrySet()) {
                Object item = _configMap.get(o.getKey());
                if (item == null) return false;
                if (!item.equals(o.getValue())) return false;
            }
            return true;
        }

        ItemEntry[] entries = config.itemEntries();
        for (ItemEntry ie : entries) {
            ItemKey key = ie.getKey();
            Object val = _configMap.get(key);
            if (val == null) return false;
            if (!val.equals(ie.getItemValue())) return false;
        }
        return true;
    }

    public int size() {
        return _configMap.size();
    }

    public boolean equals(Object other) {
        if (other instanceof DefaultConfig) {
            DefaultConfig that = (DefaultConfig) other;
            return _configMap.equals(that._configMap);
        }

        if (!(other instanceof Config)) return false;
        Config that = (Config) other;
        if (size() != that.size()) return false;

        for (Map.Entry<ItemKey, Object> itemKeyObjectEntry : _configMap.entrySet()) {
            ItemKey key = itemKeyObjectEntry.getKey();
            Object val = itemKeyObjectEntry.getValue();
            if (!val.equals(that.getItemValue(key))) return false;
        }

        return true;
    }

    public int hashCode() {
        int res = 0;
        for (Map.Entry<ItemKey, Object> itemKeyObjectEntry : _configMap.entrySet()) {
            res += ItemEntry.hashCode(itemKeyObjectEntry.getKey(), itemKeyObjectEntry.getValue());
        }
        return res;
    }


    /**
     * Gets the submap of _configMap that contains all the items which have
     * ItemKey as an ancestor.
     */
    private SortedMap<ItemKey, Object> _subMap(ItemKey key) {
        ItemKey toKey = new ItemKey(key.getPath() + ":\uFFFF");
        return _configMap.subMap(key, toKey);
    }
}
