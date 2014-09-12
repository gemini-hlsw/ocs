// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: AbstractDiscreteRangeModel.java 4392 2004-01-30 06:40:18Z gillies $
//

package edu.gemini.shared.util;

import java.io.Serializable;
import javax.swing.event.EventListenerList;

/**
 * This class represents a collection of DiscreteRange.
 * See interface DiscreteRange.
 * It keeps the collection in a normalized form by joining
 * DiscreteRange together as they are added so that there are
 * never overlapping DiscreteRange.
 * Don't try to mix types of Ranges.
 */
public abstract class AbstractDiscreteRangeModel implements DiscreteRangeModel, Serializable {

    protected EventListenerList listenerList = new EventListenerList();

    private int _selectionMode = MULTIPLE_INTERVAL_SELECTION;

    /**
     * Add a listener to the list that's notified each time a change
     * to the data model occurs.
     * @param l the DiscreteRangeModelListener
     */
    public void addDiscreteRangeModelListener(DiscreteRangeModelListener l) {
        listenerList.add(DiscreteRangeModelListener.class, l);
    }

    /**
     * Remove a listener from the list that's notified each time a
     * change to the data model occurs.
     * @param l the DiscreteRangeModelListener
     */
    public void removeDiscreteRangeModelListener(DiscreteRangeModelListener l) {
        listenerList.remove(DiscreteRangeModelListener.class, l);
    }

    /**
     * This fires an ADD event to listeners in advance of adding the range.
     * It gives listeners that implement addBegin() a chance to deny the
     * add operation by calling setAllowOperation(false) on the event.
     */
    protected DiscreteRangeModelEvent fireAddBegin(DiscreteRange r) {
        Object[] listeners = listenerList.getListenerList();
        DiscreteRangeModelEvent e = new DiscreteRangeModelEvent(this, r, DiscreteRangeModelEvent.RANGE_ADDED);

        // Loop through the listeners as long as nobody vetoes
        for (int i = listeners.length - 2; i >= 0 && e.getAllowOperation(); i -= 2) {
            if (listeners[i] == DiscreteRangeModelListener.class) {
                ((DiscreteRangeModelListener) listeners[i + 1]).addBegin(e);
            }
        }
        return e;
    }

    /**
     * This event signals that the collection has changed.
     */
    protected void fireModelChanged() {
        fireModelChanged(null, DiscreteRangeModelEvent.CONTENTS_CHANGED);
    }

    /**
     * This event signals that the collection has changed.
     */
    protected void fireModelChanged(DiscreteRange r, int operation) {
        Object[] listeners = listenerList.getListenerList();
        DiscreteRangeModelEvent e = null;

        // Loop through the listeners as long as nobody vetoes
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == DiscreteRangeModelListener.class) {
                if (e == null) {
                    e = new DiscreteRangeModelEvent(this, r, operation);
                }
                ((DiscreteRangeModelListener) listeners[i + 1]).modelChanged(e);
            }
        }
    }

    /** implements DiscreteRangeModel */
    public int getSelectionMode() {
        return _selectionMode;
    }

    /** implements DiscreteRangeModel */
    public void setSelectionMode(int selectionMode) {
        switch (selectionMode) {
        case SINGLE_INTERVAL_SELECTION:
        case MULTIPLE_INTERVAL_SELECTION:
            this._selectionMode = selectionMode;
            break;
        default:
            throw new IllegalArgumentException("invalid selectionMode");
        }
    }

}
