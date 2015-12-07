package edu.gemini.shared.util;

import java.util.Set;

/**
 * This class represents a collection of {@link DiscreteRange}s.
 * It keeps the collection in a normalized form by joining
 * DiscreteRanges together as they are added so that there are
 * never overlapping DiscreteRanges.
 * Don't try to mix types of Ranges.
 * This role of this class is like that of ListSelectionModel, rather
 * than ListModel because the data involved here is time and time
 * doesn't need to be modeled.
 */
public interface DiscreteRangeModel<T> extends DiscreteRangeModelPublisher {

    // These selection mode constants are taken from ListSelectionModel
    // to provide some familiarity.
    /**
     * A value for the rangeSelectionMode property: select one contiguous
     * range of indices at a time.
     *
     * @see #setSelectionMode
     */
    int SINGLE_INTERVAL_SELECTION = 1;

    /**
     * A value for the rangeSelectionMode property: select one or more
     * contiguous ranges of indices at a time.
     *
     * @see #setSelectionMode
     */
    int MULTIPLE_INTERVAL_SELECTION = 2;

    /**
     * Set the selection mode. The following selectionMode values are allowed:
     * <ul>
     * <li> <code>SINGLE_INTERVAL_SELECTION</code>
     *   One contiguous index interval can be selected at a time.
     * <li> <code>MULTIPLE_INTERVAL_SELECTION</code>
     *   In this mode, there's no restriction on what can be selected.
     * </ul>
     *
     * @see #getSelectionMode
     */
    void setSelectionMode(int selectionMode);

    /**
     * Returns the current selection mode.
     * @return The value of the selectionMode property.
     * @see #setSelectionMode
     */
    int getSelectionMode();

    /**
     * Gets the actual DiscreteRange collection so the client can search, etc.
     * @return An unmodifiable Set of the ranges.
     */
    Set<T> getDiscreteRanges();

    /**
     * Returns true if collection is empty.
     */
    boolean isEmpty();

    /**
     * Returns true if the specified object is within any range in the collection.
     */
    boolean contains(Object o);

    /**
     * If the specified object is within any range in the collection,
     * that range is returned.
     */
    DiscreteRange findContainingRange(Object o);

    /**
     * Returns the minimum of all the ranges in the collection.
     * For example, if the ranges were date ranges, this would return
     * the earliest date in the first range.
     */
    Object getMin();

    /**
     * Returns the maximum of all the ranges in the collection.
     * For example, if the ranges were date ranges, this would return
     * the latest date in the last range.
     */
    Object getMax();

    /**
     * Adds the specified DiscreteRange to the collection of Ranges keeping the
     * collection in normal form.
     * Before the Range is added, this method fires an event to the
     * addBegin() method of the listeners.  Listeners can examine
     * the range and deny the add before it happens.
     * @return true if range was added, false if add operation was denied.
     * Note that returning true does not mean that the collection changed.
     * For example if the new range is already contained in a range in the
     * collection then this method will return true without changing the
     * collection.
     */
    boolean add(DiscreteRange newRange);

    /**
     * Removes the specified DiscreteRange from the collection.
     * The specified range does not have to exist in the collection
     * and could remove many ranges or parts of ranges.
     * Any parts of any ranges that intersect specified range
     * will be removed.
     */
    void remove(DiscreteRange range);

    /**
     * Removes the all DiscreteRanges from the collection.
     */
    void clear();

    /**
     * Adds a DiscreteRangeModelListener to the listener list.
     */
    void addDiscreteRangeModelListener(DiscreteRangeModelListener l);

    /**
     * Removes a DiscreteRangeModelListener from the listener list.
     */
    void removeDiscreteRangeModelListener(DiscreteRangeModelListener l);

}
