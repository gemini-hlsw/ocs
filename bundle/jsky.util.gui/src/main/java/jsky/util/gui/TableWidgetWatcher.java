package jsky.util.gui;

/**
 * An interface supported by clients that which to be notified of
 * TableWidget selection.
 */
@FunctionalInterface
public interface TableWidgetWatcher {
    /**
     * Called when a row is selected.
     */
    void tableRowSelected(TableWidget twe, int rowIndex);
}

