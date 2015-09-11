// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: DiscreteRangeModelListener.java 4392 2004-01-30 06:40:18Z gillies $
//

package edu.gemini.shared.util;

public interface DiscreteRangeModelListener extends java.util.EventListener {

    /**
     * Invoked before an attempt to add a range to a collection.
     * This gives a client a chance to validate the range
     * and possibly call setAllowOperation() to deny it.
     */
    public abstract void addBegin(DiscreteRangeModelEvent e);

    /**
     * Invoked after a DiscreteRangeModel has changed.
     * This event could represent either addition or deletion.
     * Note that a single add operation may result in several deletion
     * events followed by a single add event due to range merging.
     */
    public abstract void modelChanged(DiscreteRangeModelEvent e);
}

