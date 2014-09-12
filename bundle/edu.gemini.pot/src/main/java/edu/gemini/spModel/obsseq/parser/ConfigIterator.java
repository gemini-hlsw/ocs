//
// $
//

package edu.gemini.spModel.obsseq.parser;

import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemEntry;

import java.util.*;

/**
 *
 */
public final class ConfigIterator {
    private Map<ItemKey, List<Object>> _map = new TreeMap<ItemKey, List<Object>>();

    public ConfigIterator(Config config) {
        for (ItemEntry ie : config.itemEntries()) {
            List<Object> lst = new ArrayList<Object>();
            lst.add(ie.getItemValue());
            _map.put(ie.getKey(), lst);
        }
    }

    public Set<ItemKey> getKeys() {
        return Collections.unmodifiableSet(_map.keySet());
    }

    public List<Object> getValues(ItemKey key) {
        List<Object> lst = _map.get(key);
        if (lst == null) return Collections.emptyList();
        return Collections.unmodifiableList(lst);
    }

    public boolean isCompatible(ConfigIterator cit) {
        return _map.keySet().equals(cit._map.keySet());
    }

    public void mergeWith(ConfigIterator cit) {
        for (Map.Entry<ItemKey, List<Object>> me : cit._map.entrySet()) {
            List<Object> those = me.getValue();
            List<Object> these = _map.get(me.getKey());
            if (these == null) {
                these = new ArrayList<Object>(those);
                _map.put(me.getKey(), these);
            } else {
                these.addAll(those);
            }
        }
    }

    public boolean equals(Object other) {
        if (!(other instanceof ConfigIterator)) return false;
        ConfigIterator that = (ConfigIterator) other;
        return _map.equals(that._map);
    }

    public int hashCode() {
        return _map.hashCode();
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();

        for (Map.Entry<ItemKey, List<Object>> me : _map.entrySet()) {
            buf.append("[").append(me.getKey()).append(" -> ");
            for (Object obj : me.getValue()) {
                buf.append(obj).append(", ");
            }
            if (me.getValue().size() > 0) buf.setLength(buf.length()-2);
            buf.append("]");
        }

        return buf.toString();
    }
}
