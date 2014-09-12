//
// $Id: Config.java 38078 2011-10-18 15:15:29Z swalker $
//
package edu.gemini.spModel.config2;

import edu.gemini.shared.util.immutable.MapOp;

import java.io.Serializable;
import java.util.Map;

/**
 * A telescope/instrument configuration object.  A configuration describes the
 * desired state of a system or collection of systems such as the telescope and
 * its instruments.  Configurations are matched by the principal systems at
 * observation execution time.
 *
 * <p>A Config may be thought of as a Map of items where the key
 * is a {@link ItemKey} and the value is an arbitrary Object.
 */
public interface Config extends Serializable {

    /**
     * Removes all the items from this Config, leaving it empty.
     *
     * @throws UnsupportedOperationException
     */
     void clear();

    /**
     * Returns <code>true</code> if the Config contains a single item denoted
     * by the given ItemKey.  If there is no such item, or if the key
     * refers to a collection of items, then <code>false</code> is returned.
     *
     * @param key ItemKey whose corresponding item (if any) is sought
     *
     * @return <code>true</code> if there is a single item associated with the
     * given <code>key</code>; <code>false</code> otherwise
     */
     boolean containsItem(ItemKey key);

    /**
     * Gets all the items contained in this Config, in the form of
     * {@link ItemEntry key,item pair}s.
     *
     * @return array of {@link ItemEntry}, one for each item contained in this
     * Config; an empty array if there are no items in this Config
     */
     ItemEntry[] itemEntries();

    /**
     * Gets the subset of items contained in this Config whose keys share the
     * same ancestor.  For example all the "instrument" or "telescope" items
     * may be extracted in this way.
     * <pre>
     *     ItemKey instKey = new ItemKey("instrument");
     *     Config instConfig = config.itemEntries(instKey);
     * </pre>
     *
     * @param parent ItemKey which all the items to extract have as a
     * common ancestor
     *
     * @return array of {@link ItemEntry}, one for each item contained in this
     * Config whose key is an descendent of <code>parent</code>; an empty array
     * if there are no such items in this Config
     */
     ItemEntry[] itemEntries(ItemKey parent);

    /**
     * Returns <code>true</code> if the Config contains no items.
     */
     boolean isEmpty();

    /**
     * Gets the keys of all the items that are contained in this Config.
     *
     * @return array of ItemKey, one for each item in the Config; an empty
     * array if there are no items in the Config
     */
     ItemKey[] getKeys();

    /**
     * Gets the keys of all the items which given the given <code>parent</code>
     * key as an ancestor.
     *
     * @return array of ItemKey, one for each item in the Config whose key
     * is a descendent of the given <code>parent</code>; an empty array if
     * there are no such items in the Config
     */
     ItemKey[] getKeys(ItemKey parent);

    /**
     * Gets the single item value indicated by the given key, if any.  If there
     * is no item associated with this key, then <code>null</code> is returned.
     *
     * @param key ItemKey whose associated item value should be returned
     *
     * @return the single item value associated with the given key, if any;
     * <code>null</code> otherwise
     */
     Object getItemValue(ItemKey key);

    /**
     * Gets the sub Config of this Config whose items all contain the given
     * key as an ancestor.  If there are no such items, then an empty Config
     * is returned.
     *
     * @param parent key that all items in the sub Config to be returned must
     * have as a common ancestor
     *
     * @return sub Config of this Config indicated by the given parent key;
     * all items in the returned Config will be associated with a key that is
     * a descendent of <code>parent</code>; an empty Config if there are no
     * such matching items
     */
     Config getAll(ItemKey parent);

    /**
     * Gets the sub Config of this Config whose items are keyed by the given
     * <code>keys</code>.  If there are no such items, then an empty Config
     * is returned.
     *
     * @param parents array of keys to use to look for matching items in this
     * Config
     *
     * @return sub Config of this Config indicated by the given
     * <code>keys</code>; the key of all items in the returned Config will be
     * among the <code>keys</code> array
     */
     Config getAll(ItemKey[] parents);

     <K> Map<K, ItemEntry[]> groupBy(MapOp<ItemEntry, K> f);

    /**
     * Adds the given <code>item</code> to this Config, associated with the key
     * <code>key</code>.  Any previous item associated with this key (if any) is
     * subsequently forgotten.
     *
     * @param key key to associate with the item
     * @param item item to add to the Config
     *
     * @return item that was previously associated with <code>key</code>, if
     * any
     *
     * @throws UnsupportedOperationException
     */
     Object putItem(ItemKey key, Object item);

    /**
     * Adds all the items in the given <code>config</code> to this Config.
     * After this operation, <code>config</code> will be a subset of this
     * Config, but will not be modified in any way.
     *
     * @param config configuration whose items should be added to this
     * Config
     *
     * @throws UnsupportedOperationException
     */
     void putAll(Config config);

    /**
     * Removes the item associated with <code>key</code>, if any, from this
     * Config.
     *
     * @param key key whose associated item should be removed
     *
     * @return the item that was removed, or <code>null</code> if there is
     * no such item
     *
     * @throws UnsupportedOperationException
     */
     Object remove(ItemKey key);

    /**
     * Removes all the items that contain <code>parentKey</code> as an
     * ancestor of their associated key.  For example:
     *
     * <pre>
     *     config.removeAll(new ItemKey("instrument"));
     * </pre>
     *
     * would remove <code>instrument.filter</code> and
     * <code>instrument.grism</code>.  It would not remove
     * <code>telescope.p</code>.  See {@link ItemKey}.
     *
     * @param parent base key of the items to remove; any item whose key
     * descends from this key will be removed from this Config
     *
     * @throws UnsupportedOperationException
     */
     void removeAll(ItemKey parent);

    /**
     * Removes every item from this Config whose key is in the given array of
     * <code>keys</code>, or whose key contains a parent that is in the given
     * array.  For example, all items with keys that begin with "instrument"
     * or "telescope" can be removed with:
     *
     * <pre>
     *      config.removeAll(new ItemKey[] {
     *          new ItemKey("instrument), new ItemKey("telescope"),
     *      });
     * </pre>
     *
     * @param parents array of ItemKey indicating which items to remove from
     * this Config
     *
     * @throws UnsupportedOperationException
     */
     void removeAll(ItemKey[] parents);

    /**
     * Removes all items contained in the given <code>config</code> from this
     * Config.  Only items with the same ItemKey and value (according to
     * the .equals() method) are removed from this Config.  Two items with
     * the same ItemKey, but with distinct values are left alone.
     *
     * @param config configuration whose matching values should be removed
     *
     * @throws UnsupportedOperationException
     */
     void removeAll(Config config);

    /**
     * Removes all items from this Config except those whose associated key is
     * a descendent of <code>key</code>.  For example:
     *
     * <pre>
     *     config.retainAll(new ItemKey("instrument"));
     * </pre>
     *
     * removes all items that are not "instrument" items.
     *
     * @param parent base key of the items to retain (all others will be
     * removed); any item whose key descends from this key will be maintained
     * while all other items are deleted.
     *
     * @throws UnsupportedOperationException
     */
     void retainAll(ItemKey parent);

    /**
     * Removes every item from this Config except for those whose key is in the
     * given array of <code>keys</code>, or whose key contains a parent that
     * is in the given array.  For example, all items with keys do <em>not</em>
     *  begin with "instrument" or "telescope" can be removed with:
     *
     * <pre>
     *      config.retainAll(new ItemKey[] {
     *          new ItemKey("instrument), new ItemKey("telescope"),
     *      });
     * </pre>
     *
     * For instance, any "ocs" items would be removed by this call.
     *
     * @param parents array of ItemKey indicating which items to remove from
     * this Config
     *
     * @throws UnsupportedOperationException
     */
     void retainAll(ItemKey[] parents);

    /**
     * Removes all items from this Config except for those contained in the
     * given <code>config</code>.  Only items with the same ItemKey and value
     * (according to the .equals() method) are left remaining in this Config.
     * Two items with the same ItemKey, but with distinct values, are not
     * considered the same and so will be removed from this Config.
     *
     * @param config configuration whose matching values should be retained
     *
     * @throws UnsupportedOperationException
     */
     void retainAll(Config config);

    /**
     * Determines whether the given <code>config</code> is a subset of this
     * Config.  In other words, whether every item contained in
     * <code>config</code> is also contained in this Config, associated with
     * same key and with a value that is .equals().
     *
     * <p>Every empty Config will match, and every Config instance matches
     * itself.
     *
     * @param config configuration to match against this Config
     *
     * @return <code>true</code> if <code>config</code> is a subset of this
     * Config
     */
     boolean matches(Config config);

    /**
     * Obtains the number of items contained in this Config instance.
     *
     * @return number of items in this Config
     */
     int size();


    /**
     * Equals returns <code>true</code> if <code>other</code> is also a Config
     * and contains the exact same {@link ItemKey} to Object mappings.  In other
     * words, if {@link #itemEntries()} returns the same array of
     * {@link ItemEntry} for both Configs (though possibly in different orders).
     * In this way, equals works across different implementations of Config.
     */
    boolean equals(Object other);

    /**
     * Returns the sum of the hashCodes of each {@link ItemEntry} in the
     * Config.  In this way, hashCode works across different implementations of
     * Config
     */
    int hashCode();
}
