// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: DropDownListBoxWidgetWatcher.java 6719 2005-11-08 19:35:36Z brighton $
//
package jsky.util.gui;

/**
 * An interface supported by clients that which to be notified of
 * DropDownListBoxWidget selection and action.
 */
public interface DropDownListBoxWidgetWatcher {
    /**
     * Called when an item is selected.
     */
    public void dropDownListBoxAction(DropDownListBoxWidget ddlbwe, int index, String val);
}

