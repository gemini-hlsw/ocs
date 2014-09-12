// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: DiscreteRangeModelPublisher.java 4392 2004-01-30 06:40:18Z gillies $
//

package edu.gemini.shared.util;

/**
 * This interface is implemented by objects producing DiscreteRangeEvents.
 */
public interface DiscreteRangeModelPublisher {

    /**
     * Adds a DiscreteRangeModelListener to the listener list.
     */
    public void addDiscreteRangeModelListener(DiscreteRangeModelListener l);

    /**
     * Removes a DiscreteRangeModelListener from the listener list.
     */
    public void removeDiscreteRangeModelListener(DiscreteRangeModelListener l);

}

