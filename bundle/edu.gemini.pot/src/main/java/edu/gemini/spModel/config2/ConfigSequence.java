package edu.gemini.spModel.config2;

import java.util.*;
import java.io.Serializable;
import java.io.IOException;

/**
 * The ConfigSequence is an ordered collection of {@link Config} instances.
 * The intent is to represent an Observation's series of configuration changes
 * to be applied to the system.  Each sequence "step" corresponds to a Config
 * in the ConfigSequence.
 *
 * <p>Each Config in the ConfigSequence contains every item that was present in
 * the previous step.  For example, if the first step contains
 * <code>instrument:filter = "J"</code> and a second step is added that does
 * not mention the instrument filter, then regardless it too will contain an
 * item for the instrument:filter set to "J".  The following lines:
 *
 * <pre>
 *      ItemKey key = new ItemKey("instrument:filter");
 *      System.out.println(configSequence.getItem(0, key));
 *      System.out.println(configSequence.getItem(1, key));
 * </pre>
 *
 * would print "J" twice even though the instrument filter wasn't included in
 * the Config added to the sequence in step 1 because the instrument filter
 * remains at "J" in the second step of the sequence.
 *
 * <p>A "compact" representation that includes <em>only</em> the changes between
 * successive steps is available via the {@link #getCompactView()} and
 * {@link #compactIterator()} methods.
 *
 * <p><b>Note that this class is not mt-safe</b> If multiple threads access a
 * ConfigSequence concurrently, and at least one of the threads modifies the
 * sequence structurally, it <em>must</em> be synchronized externally.
 */
public class ConfigSequence implements Serializable {

    public interface Predicate {
        boolean matches(Config config);
    }

    private List<Config> _configs = new ArrayList<>();
    private boolean _isCompact = true;

    private transient List<Config> _completeConfigs;

    // Iterator class used to make copies of the Config object it generates.
    private class CopyIterator implements Iterator<Config> {
        private Iterator<Config> _srcIter;

        CopyIterator(Iterator<Config> srcIter) {
            _srcIter = srcIter;
        }

        public boolean hasNext() {
            return _srcIter.hasNext();
        }

        public Config next() {
            return new DefaultConfig(_srcIter.next());
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static final ConfigSequence EMPTY = new ConfigSequence();

    /**
     * Constructs an empty ConfigSequence.
     */
    public ConfigSequence() {
    }

    /**
     * Constructs a ConfigSequence containing the given {@link Config}s in the
     * given order.
     *
     * @param configs Config instances initially contained in this sequence
     */
    public ConfigSequence(Config[] configs) {
        if (configs.length > 1) _isCompact = false;
        for (Config config : configs) {
            _configs.add(new DefaultConfig(config));
        }
    }

    /**
     * Creates a ConfigSequence that is a copy of the given sequence.
     */
    public ConfigSequence(ConfigSequence copy) {
        _configs = new ArrayList<>(copy._configs);

        for (ListIterator<Config> lit=_configs.listIterator(); lit.hasNext(); ) {
            lit.set(new DefaultConfig(lit.next()));
        }
        _isCompact = copy._isCompact;
    }

    //
    // Compacts the representation of the sequence such that each step only
    // contains items that are different from the previous step.  For example,
    // if step 0 has instrument:filter J and step 1 also has instrument:filter
    // J, then instrument:filter is removed from step 1 in the _configs list.
    //
    private void _compact() {
        if (_isCompact) return;
        if (_configs.size() <= 1) {
            _isCompact = true;
            return;
        }

        List<Config> res = new ArrayList<>();
        Config curConfig = new DefaultConfig();
        for (Config next : _configs) {

            Config tmp = new DefaultConfig(next);
            tmp.removeAll(curConfig);
            res.add(tmp);

            curConfig.putAll(next);
        }
        _configs   = res;
        _isCompact = true;
    }

    //
    // Fills in the transient _completeConfigs list with Config instances in
    // which every successive Config contains all items that are in effect at
    // that step.
    //
    private void _complete() {
        if (_completeConfigs != null) return;

        _completeConfigs = new ArrayList<>();
        if (_configs.size() == 0) return;

        Config curConfig = new DefaultConfig();
        for (Config config : _configs) {
            curConfig.putAll(config);
            _completeConfigs.add(new DefaultConfig(curConfig));
        }
    }

    /**
     * Gets all of the {@link Config} objects contained in this sequence in the
     * order that they occur.  Each Config object contains all the items that
     * are in effect during the lifetime of the configuration.
     *
     * <p>Note that modifications to the array or to any Config object it
     * contains have no impact on this ConfigSequence.
     *
     * @return all the {@link Config} objects in this sequence
     */
    public Config[] getAllSteps() {
        _complete();

        Config[] res = _completeConfigs.toArray(DefaultConfig.EMPTY_ARRAY);
        for (int i=0; i<res.length; ++i) {
            res[i] = new DefaultConfig(res[i]);
        }
        return res;
    }

    /**
     * Filters the ConfigSequence on the given predicate.  Contained Config
     * objects that match the predicate are kept, those that don't are dropped.
     *
     * @param p predicate used to test each contained Config
     *
     * @return new ConfigSequence with just those Configs that match the
     * predicate
     */
    public ConfigSequence filter(Predicate p) {
        _complete();
        List<Config> res = new ArrayList<>(_completeConfigs.size());
        for (Config c : _completeConfigs) {
            if (p.matches(c)) res.add(c);
        }
        return new ConfigSequence(res.toArray(new Config[res.size()]));
    }

    /**
     * Gets an array of all the Config objects in the sequence, but in a
     * compact representation where each successive Config contains only those
     * items that differ from the previous Config.
     *
     * <p>Note that modifications to the array or to any Config object it
     * contains have no impact on this ConfigSeuqnce.
     *
     * @return an array of {@link Config} objects, where each successive
     * Config contains only those items that differ from the previous Config
     */
    public Config[] getCompactView() {
        _compact();
        Config[] res = _configs.toArray(DefaultConfig.EMPTY_ARRAY);
        for (int i=0; i<res.length; ++i) {
            res[i] = new DefaultConfig(res[i]);
        }
        return res;
    }

    /**
     * Gets (a copy of) the Config active at the given <code>step</code>.
     *
     * @param step number of the Config in the sequence to return
     *
     * @return Config at the given <code>step</code>
     */
    public Config getStep(int step) {
        _complete();
        return new DefaultConfig(_completeConfigs.get(step));
    }

    /**
     * Gets the value of the item associated with the given <code>key</code> in
     * the given <code>step</code>.
     *
     * @param step sequence number of interest
     * @param key item key whose value should be returned
     *
     * @return value of the item associated with <code>key</code> at the given
     * <code
     */
    public Object getItemValue(int step, ItemKey key) {
        Config conf = getStep(step);
        return conf.getItemValue(key);
    }

    /**
     * Gets the value of the item associated with <code>key</code> at each
     * step of the sequence.  If the item is not included in the Config at a
     * particular step, then <code>null</code> is entered for the value at that
     * step.
     *
     * @param key key associated with the item whose values should be returned
     *
     * @return array of item values at each step of the sequence for the item
     * associated with <code>key</code>
     */
    public Object[] getItemValueAtEachStep(ItemKey key) {
        _complete();
        Object[] res = new Object[_completeConfigs.size()];
        int i = 0;
        for (Config completeConfig : _completeConfigs) {
            res[i++] = completeConfig.getItemValue(key);
        }
        return res;
    }

    /**
     * Gets the distinct values for the given key over the lifetime of the
     * ConfigSequence.  If an item is introduced to the sequence after the
     * initial step, this will include a <code>null</code> object to indicate
     * this fact.
     *
     * @param key item whose distinct values should be returned
     *
     * @return the values that the item associated with the specified
     * <code>key</code> obtains over the lifetime of the ConfigSequence
     */
    public Object[] getDistinctItemValues(ItemKey key) {
        _complete();
        Set<Object> s = new HashSet<>();
        for (Config completeConfig : _completeConfigs) {
            s.add(completeConfig.getItemValue(key));
        }
        return s.toArray(new Object[s.size()]);
    }

    /**
     * Gets the keys of items whose values change over the course of the
     * sequence.  This includes changes to values (like a filter moving from
     * "J" to "K") and the introduction of new items that were not present
     * in the original configuration.
     *
     * @return the collection of items whose value changes over the course
     * of the sequence (in no particular order)
     */
    public ItemKey[] getIteratedKeys() {
        if (_configs.size() <= 1) return ItemKey.EMPTY_ARRAY;

        Config startConfig = _configs.get(0);

        Set<ItemKey> res = new HashSet<>();
        for (int i=1; i<_configs.size(); ++i) {
            Config conf = _configs.get(i);
            ItemEntry[] itemEntryArray = conf.itemEntries();
            for (ItemEntry ie : itemEntryArray) {
                ItemKey key = ie.getKey();
                Object  val = ie.getItemValue();

                if (!val.equals(startConfig.getItemValue(key))) {
                    res.add(key);
                }
            }
        }
        return res.toArray(ItemKey.EMPTY_ARRAY);
    }

    /**
     * Gets the keys of items whose values never change over the course of the
     * sequence.  Any item that is introduced to the sequence after the initial
     * step is counted as having changed over the course of the sequence.
     *
     * @return the collection of items whose value does not change over the
     * course of the sequence (in no particular order)
     */
    public ItemKey[] getStaticKeys() {
        if (_configs.size() <= 0) return ItemKey.EMPTY_ARRAY;

        Config startConfig = _configs.get(0);
        Set<ItemKey> res = new HashSet<>();
        Collections.addAll(res, startConfig.getKeys());

        for (int i=1; i<_configs.size(); ++i) {
            Config conf = _configs.get(i);
            ItemEntry[] itemEntryArray = conf.itemEntries();
            for (ItemEntry ie : itemEntryArray) {
                ItemKey key = ie.getKey();
                Object  val = ie.getItemValue();

                if (!val.equals(startConfig.getItemValue(key))) {
                    res.remove(key);
                }
            }
        }
        return res.toArray(ItemKey.EMPTY_ARRAY);

    }

    /**
     * Returns the first Config in the sequence for which <code>template</code>
     * is a subset.  In other words, the first Config which contains all the
     * items with values equal to those in <code>template</code>.
     *
     * @param template template Config to match against the sequence
     *
     * @return first matching Config in the sequence; <code>null</code> if there
     * is no matching configuration
     */
    public Config match(Config template) {
        int index = indexMatching(template);
        return (index < 0) ? null : getStep(index);
    }

    /**
     * @param template template Config to match against the sequence
     *
     * @return the index of the first Config that matches the given template, if
     * any; -1 otherwise
     */
    private int indexMatching(Config template) {
       _complete();
        int i=0;
        for (Config conf : _completeConfigs) {
            if (conf.matches(template)) return i;
            ++i;
        }
        return -1;
    }

    /**
     * Adds the changes in the given Config to the end of the sequence.  Note
     * that a subsequent call to <code>sequence.get(sequence.size()-1)</code>
     * will return a Config that includes any additional items, not included
     * in <code>conf</code>, that are active at this step.  In other words,
     * if the instrument filter position is "J" during the previous step, then
     * an item for "instrument:filter" will be included even if not present
     * in <code>conf</code>.
     *
     * @param conf the configuration information to add to the end of the
     * sequence; it is not modified by this method, and subsequent changes to
     * <code>conf</code> do not impact this ConfigSequence
     *
     * @param conf the changes that should be added to the end of this sequence
     */
    public void addStep(Config conf) {
        Config nextConfig = new DefaultConfig(conf);

        if (_isCompact && (_completeConfigs != null)) {
            Config lastConfig = _completeConfigs.get(_completeConfigs.size() - 1);
            nextConfig.removeAll(lastConfig);
            _configs.add(nextConfig);

            lastConfig = new DefaultConfig(lastConfig);
            lastConfig.putAll(nextConfig);
            _completeConfigs.add(lastConfig);
        } else {
            _isCompact = false;
            _completeConfigs = null;
            _configs.add(nextConfig);
        }
    }

    /**
     * Adds the items in the given Config to the sequence at the given
     * <code>step</code>.  Works the same as {@link #addStep(Config)}, except
     * allows the insertion of Config updates at a particular step.
     *
     * @param step sequence step in which the given <code>conf</code> should
     * be added
     * @param conf the changes to the sequence that should be applied;
     *  <code>conf</code> is not modified by this method, and
     * subsequent changes to <code>conf</code> do not impact this
     * ConfigSequence.
     *
     */
    public void addStep(int step, Config conf) {
        if (step == _configs.size()) {
            addStep(conf);
        } else {
            _isCompact = false;
            _completeConfigs = null;
            _configs.add(step, new DefaultConfig(conf));
        }
    }

    /**
     * Clears sequence of Configs, leaving the sequence empty.
     */
    public void clear() {
        _configs.clear();
        if (_completeConfigs != null) _completeConfigs.clear();
        _isCompact = true;
    }

    /**
     * Iterates over each {@link Config} in the sequence.  See also
     * {@link #compactIterator()}.
     */
    public Iterator<Config> iterator() {
        _complete();
        return new CopyIterator(_completeConfigs.iterator());
    }

    /**
     * Iterates over each {@link Config} in the sequence, returning Config
     * values at each step which contain only the changes from the previous
     * step.  In other words, items are not repeated from one step to the next.
     * Only those item values that differ from the previous step are included
     * in each successive Config.
     */
    public Iterator<Config> compactIterator() {
        _compact();
        return new CopyIterator(_configs.iterator());
    }

    /**
     * Returns <code>true</code> if the sequence contains no {@link Config}s,
     * <code>false</code> otherwise.
     */
    public boolean isEmpty() {
        return _configs.isEmpty();
    }

//    public boolean remove(Config config) {
//        return _configs.remove(config);
//    }

    /**
     * Removes the {@link Config} at the given <code>step</code>.  Any
     * subsequent steps that inherited item values from this step may be
     * effected by this.
     */
    public void removeStep(int step) {
        if (step != (_configs.size() - 1)) {
            _isCompact = false;
            _completeConfigs = null;
        }
        _configs.remove(step);
    }

    /**
     * Sets the configuration information at the specified <code>step</code>.
     * Item values contained in <code>conf</code> are either added or
     * modified in the ConfigSequence at this step.  Items that were
     * introduced to the ConfigSequence at this step, if not present in
     * <code>conf</code> are removed from the sequence.  Items that are
     * inherited from previous steps remain in this step and subsequent
     * steps.
     *
     * @param step sequence step in which the given <code>conf</code> should
     * be added
     *
     * @param conf the changes to the sequence that should be applied;
     * <code>conf</code> is not modified by this method, and
     * subsequent changes to <code>conf</code> do not impact this
     * ConfigSequence.
     */
    public void setStep(int step, Config conf) {
        _isCompact = false;
        _completeConfigs = null;
        _configs.set(step, new DefaultConfig(conf));
    }

    /**
     * Retrieves the number of {@link Config} steps in the sequence.
     */
    public int size() {
        return _configs.size();
    }

    /**
     * Creates a sequence containing the same configs as this sequence, but only
     * from the <code>from</code> index (inclusive) to the <code>to</code>
     * index (exclusive).  Unlike the similarly named methods on
     * <code>java.util.List</code>, the ConfigSequence returned is not backed
     * by this ConfigSequence and changes to it do not impact this sequence.
     *
     * @param from first index to include in the sub sequence to be returned
     * (inclusive)
     *
     * @param to last index of the sub sequence (exclusive)
     *
     * @return a new, distinct ConfigSequence consisting of the {@link Config}
     * objects of this sequence from the <code>from</code> index to the
     * <code>to</code> index.
     */
    public ConfigSequence subSequence(int from, int to) {
        _complete();
        Config[] subconfigs = new Config[to - from];
        for (int i=from; i<to; ++i) {
            subconfigs[i-from] = _completeConfigs.get(i);
        }
        return new ConfigSequence(subconfigs);
    }

    //
    // Converts to to the most compact representation before serializing.
    //
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        _compact();
        out.defaultWriteObject();
    }

}
