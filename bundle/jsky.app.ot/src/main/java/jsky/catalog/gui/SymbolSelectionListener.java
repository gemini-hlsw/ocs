package jsky.catalog.gui;

import java.util.EventListener;

/**
 * This defines the interface for listening for selection events on a catalog plot symbols.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public interface SymbolSelectionListener extends EventListener {

    /**
     * Invoked when the symbol is selected.
     */
    void symbolSelected(SymbolSelectionEvent e);

    /**
     * Invoked when the symbol is deselected.
     */
    void symbolDeselected(SymbolSelectionEvent e);
}
