/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TableSelectionListener.java 4414 2004-02-03 16:21:36Z brighton $
 */

package jsky.catalog.gui;

import java.util.EventListener;

/**
 * This defines the interface for listening for selection events on a catalog plot tables.
 *
 * @version $Revision: 4414 $
 * @author Daniella Malin
 */
public abstract interface TableSelectionListener extends EventListener {

    /**
     * Invoked when the table is selected.
     */
    public void tableSelected(TableSelectionEvent e);

}
