// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: TableWidgetWatcher.java 6719 2005-11-08 19:35:36Z brighton $
//
package jsky.util.gui;




/**
 * An interface supported by clients that which to be notified of
 * TableWidget selection and action.
 */
public interface TableWidgetWatcher {
    /**
     * Called when a row is selected.
     */
    default void tableRowSelected(TableWidget twe, int rowIndex) {}

    /**
     * Called when a row is double clicked (or return key is pressed).
     */
    default void tableAction(TableWidget twe, int colIndex, int rowIndex) {}
}

