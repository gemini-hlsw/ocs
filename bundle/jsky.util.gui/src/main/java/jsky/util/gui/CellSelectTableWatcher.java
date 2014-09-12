// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: CellSelectTableWatcher.java 6719 2005-11-08 19:35:36Z brighton $
//
package jsky.util.gui;

/**
 * The interface to be supported by CellSelectTableWidget clients that
 * want to be informed of when cells are selected and actioned.
 *
 * @see CellSelectTableWidget
 */
public interface CellSelectTableWatcher {

    /**
     * The given cell was selected.
     */
    public void cellSelected(CellSelectTableWidget w, int colIndex, int rowIndex);

    /**
     * The given cell was "actioned".
     * XXX allan: no needed
     public void cellAction(CellSelectTableWidget w, int colIndex, int rowIndex);
     */
}

