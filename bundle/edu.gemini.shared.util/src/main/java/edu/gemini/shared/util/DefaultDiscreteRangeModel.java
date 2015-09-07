// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: DefaultDiscreteRangeModel.java 4392 2004-01-30 06:40:18Z gillies $
//

package edu.gemini.shared.util;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.Calendar;
import java.util.Collections;
import java.io.Serializable;

/**
 * This class represents a collection of DiscreteRange objects.
 * It is ordered by the natural ordering of the DiscreteRange objects.
 * See interface DiscreteRange.
 * It keeps the collection in a normalized form by joining
 * DiscreteRange together as they are added so that there are
 * never overlapping DiscreteRanges.
 * Don't try to mix types of Ranges.
 */
public class DefaultDiscreteRangeModel extends AbstractDiscreteRangeModel {

    /** The ordered collection of DiscreteRanges */
    protected TreeSet ranges = new TreeSet();

    /**
     * Gets the actual DiscreteRange collection so the client can search, etc.
     * @return An unmodifiable Set of the ranges.
     */
    public Set getDiscreteRanges() {
        return Collections.unmodifiableSet(ranges);
    }

    /**
     * Sets the actual DiscreteRange collection by doing a shallow copy of
     * the input argument set.  A shallow copy is fine since the objects in
     * the collection are immutable (DiscreteDateRange).
     */
    public void setDiscreteRanges(Set newRangeSet) {
        // We store ranges in a TreeSet.  We need our own copy of the Set.
        TreeSet newRangeTreeSet = new TreeSet();
        for (Iterator itr = newRangeSet.iterator(); itr.hasNext();) {
            newRangeTreeSet.add(itr.next());
        }
        ranges = newRangeTreeSet;
        fireModelChanged();
    }

    public boolean isEmpty() {
        return (ranges.size() == 0);
    }

    /**
     * Returns true if the specified object is within any range in the collection.
     */
    public boolean contains(Object o) {
        for (Iterator itr = ranges.iterator(); itr.hasNext();) {
            DiscreteRange r = (DiscreteRange) itr.next();
            if (r.contains(o))
                return true;
        }
        return false;
    }

    /**
     * Returns true if the specified object is within any range in the collection.
     */
    public DiscreteRange findContainingRange(Object o) {
        for (Iterator itr = ranges.iterator(); itr.hasNext();) {
            DiscreteRange r = (DiscreteRange) itr.next();
            if (r.contains(o))
                return r;
        }
        return null;
    }

    /**
     * Returns the minimum of all the ranges in the collection.
     * For example, if the ranges were date ranges, this would return
     * the earliest date in the first range.
     */
    public Object getMin() {
        if (ranges.size() == 0)
            return null;
        // The ranges are ordered.
        DiscreteRange r = (DiscreteRange) ranges.first();
        return r.getStart();
    }

    /**
     * Returns the maximum of all the ranges in the collection.
     * For example, if the ranges were date ranges, this would return
     * the latest date in the last range.
     */
    public Object getMax() {
        if (ranges.size() == 0)
            return null;
        // The ranges are ordered.
        DiscreteRange r = (DiscreteRange) ranges.last();
        return r.getEnd();
    }

    /**
     * Adds the specified DiscreteRange to the collection of Ranges keeping the
     * collection in normal form.
     * Before the Range is added, this method fires an event to the
     * addBegin() method of the listeners.  Listeners can examine
     * the range and deny the add before it happens.
     */
    public boolean add(DiscreteRange newRange) {
        newRange = (DiscreteRange) newRange.clone();   // Want our own copy
        newRange = newRange.normalize();  // make sure it is in normal form

        // Allow listeners to validate the range before we add it.
        if (!fireAddBegin(newRange).getAllowOperation())
            return false;

        // Listeners did not disallow the addition.

        if (getSelectionMode() == SINGLE_INTERVAL_SELECTION) {
            // Only a single reange is allowed to be selected at a time.
            // So clear any previous selections and then add this range.
            clear();
            _add(newRange);
            return true;
        }
        boolean not_done = true;  // a hack to end my loop
        // can't remove ranges while iterating, so make a list of ranges
        // to discard and remove them all after the iteration loop.
        List discardedRanges = new ArrayList();
        for (Iterator itr = ranges.iterator(); itr.hasNext() && not_done;) {
            DiscreteRange r = (DiscreteRange) itr.next();
            if (r.contains(newRange))
                return true;  // done
            if (r.touches(newRange)) {
                // Ranges touch and can therefore be combined
                discardedRanges.add(r);        // mark old range for deletion
                newRange = newRange.union(r);  // absorb old range
            }
            else if (r.after(newRange))
                not_done = false;
        }
        for (Iterator itr = discardedRanges.iterator(); itr.hasNext();) {
            DiscreteRange r = (DiscreteRange) itr.next();
            remove(r);  // allow the method to do it so it can fire events
        }
        _add(newRange);
        return true;
    }

    // This routine adds newRange to the collection, no checking, no merging.
    // The calling routine had better do the checking.
    private void _add(DiscreteRange newRange) {
        // this is the only call to add so as to centralize the handling of events
        ranges.add(newRange);
        fireModelChanged(newRange, DiscreteRangeModelEvent.RANGE_ADDED);
        return;
    }

    /**
     * Removes the specified DiscreteRange from the collection.
     * The specified range does not have to exist in the collection
     * and could remove many ranges or parts of ranges.
     * Any parts of any ranges that intersect specified range
     * will be removed.
     */
    public void remove(DiscreteRange range) {
        range = range.normalize();  // make sure it is in normal form
        List discardedRanges = new ArrayList();
        for (Iterator itr = ranges.iterator(); itr.hasNext();) {
            DiscreteRange r = (DiscreteRange) itr.next();
            if (r.intersects(range)) {
                discardedRanges.add(r);
                /*
                DiscreteRange difference = r.difference(range);
                if (difference != null) {
                   _add(difference);
                }*/
            }
            else if (r.after(range))
                break;
        }
        for (Iterator itr = discardedRanges.iterator(); itr.hasNext();) {
            DiscreteRange r = (DiscreteRange) itr.next();
            ranges.remove(r);  // allow the method to do it so it can fire events
            fireModelChanged(r, DiscreteRangeModelEvent.RANGE_REMOVED);
        }
    }

    /**
     * Removes the all DiscreteRanges from the collection.
     */
    public void clear() {
        // Can't iterate over the real set of ranges and remove them
        // or we'll get an exception.  Work from a copy of the set.
        ranges = new TreeSet();
        fireModelChanged();

        /*
          for (Iterator itr = ranges.iterator(); itr.hasNext(); )
          {
          DiscreteRange r = (DiscreteRange)itr.next();
          copyRanges.add(r);
          }
          for (Iterator itr = copyRanges.iterator(); itr.hasNext(); )
          {
          DiscreteRange r = (DiscreteRange)itr.next();
          remove(r);
          }*/
    }

}
