// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: DiscreteRangeModelEvent.java 4392 2004-01-30 06:40:18Z gillies $
//

package edu.gemini.shared.util;

/**
 * This class represents a change in a collection of DiscreteRanges (the model).
 * i.e. an addition or deletion.
 * Note that a single DiscreteRange addition to a collection could result
 * in the merging of many ranges.  This results in the firing of
 * many DiscreteRangeCollectionEvents - the deletions, and finally the
 * addition of the resultant DiscreteRange.
 */

public class DiscreteRangeModelEvent extends java.util.EventObject {

    /**
     * Indicates a range is being added.
     */
    public static final int RANGE_ADDED = 0;

    /**
     * Indicates a range is being removed.
     */
    public static final int RANGE_REMOVED = 1;

    /**
     * Indicates the model changed in a way too complex to describe here.
     * Client should query the source for more information.
     */
    public static final int CONTENTS_CHANGED = 2;

    private DiscreteRange _range = null;

    private int _operation;  // ADD or REMOVE

    // This allows a listener to validate a range and stop
    // the addition operation before any changes are made.
    private boolean _allowOperation = true;

    /**
     * Constructs a DiscreteRangeEvent.
     * @param source - the source of the event
     * @param range - the DiscreteRange involved in the operation
     */
    public DiscreteRangeModelEvent(Object source, DiscreteRange range, int operation) {
        super(source);
        _range = range;
        _operation = operation;
        setAllowOperation(true);
    }

    /**
     * Gets the range involved in the operation.
     */
    public DiscreteRange getRange() {
        return _range;
    }

    /**
     * Sets the range involved in the operation.
     */
    public void setRange(DiscreteRange range) {
        _range = range;
    }

    /**
     * Gets the operation type.
     */
    public int getOperation() {
        return _operation;
    }

    /**
     * Sets the operation type.
     */
    public void setOperation(int operation) {
        _operation = operation;
    }

    /**
     * A client can set this to false to disallow a range addition
     * before it takes place.
     */
    public void setAllowOperation(boolean b) {
        _allowOperation = b;
    }

    /**
     * Retrieve whether the operation will be allowed or denied.
     */
    public boolean getAllowOperation() {
        return _allowOperation;
    }

    public String toString() {
        String s = "DiscreteRangeEvent ";
        if (_operation == RANGE_ADDED)
            s += "RANGE_ADDED ";
        if (_operation == RANGE_REMOVED)
            s += "RANGE_REMOVED ";
        if (_operation == CONTENTS_CHANGED)
            s += "CONTENTS_CHANGED ";
        s += _range;
        if (!_allowOperation)
            s += " - operation not allowed";
        return s;
    }

}

