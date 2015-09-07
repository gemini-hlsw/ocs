// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: ListBoxWidgetWatcher.java 7030 2006-05-11 17:55:34Z shane $
//
package jsky.util.gui;

/**
 * An interface supported by clients that which to be notified of
 * ListBoxWidget selection and action.
 */
public interface ListBoxWidgetWatcher {
    /**
     * Called when an item is selected.
     */
    public void listBoxSelect(ListBoxWidget lbwe, int index, Object val);

    /**
     * Called when an item is double clicked.
     */
    public void listBoxAction(ListBoxWidget lbwe, int index, Object val);
}

