package jsky.util.gui;

/**
 * The interface to be supported by CellSelectTableWidget clients that
 * want to be informed of when cells are selected and actioned.
 */
@FunctionalInterface
public interface CellSelectTableWatcher {
    /**
     * The given cell was selected.
     */
    void cellSelected(CellSelectTableWidget w, int colIndex, int rowIndex);
}

