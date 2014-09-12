/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SymbolSelectionListener.java 4414 2004-02-03 16:21:36Z brighton $
 */

package jsky.catalog.gui;

import java.util.EventListener;

/**
 * This defines the interface for listening for selection events on a catalog plot symbols.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public abstract interface SymbolSelectionListener extends EventListener {

    /**
     * Invoked when the symbol is selected.
     */
    public void symbolSelected(SymbolSelectionEvent e);

    /**
     * Invoked when the symbol is deselected.
     */
    public void symbolDeselected(SymbolSelectionEvent e);
}
