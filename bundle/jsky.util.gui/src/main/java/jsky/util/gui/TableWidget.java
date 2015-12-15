package jsky.util.gui;

import javax.swing.ListSelectionModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Was an extension of the Marimba TableWidget to support row selection
 * and action observers. Now this class is derived from JTable.
 */
public class TableWidget extends RowManipulateTableWidget {

    // Observers
    private final List<TableWidgetWatcher> _watchers = new ArrayList<>();

    /** Default constructor */
    public TableWidget() {
        getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int i = getSelectionModel().getMinSelectionIndex();
                if (i >= 0)
                    _notifySelect(i);
            }
        });
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setShowHorizontalLines(false);
    }


    /**
     * Add a watcher.  Watchers are notified when an item is selected.
     */
    public synchronized final void addWatcher(TableWidgetWatcher tww) {
        if (_watchers.contains(tww)) {
            return;
        }
        _watchers.add(tww);
    }

    /**
     * Delete a watcher.
     */
    public synchronized final void deleteWatcher(TableWidgetWatcher tww) {
        _watchers.remove(tww);
    }

    /**
     * Select the given row and notify observers
     */
    public void selectRowAt(int rowIndex) {
        getSelectionModel().addSelectionInterval(rowIndex, rowIndex);
    }

    /**
     * Notify observers when a row is selected.
     *
     * @param rowIndex the index of the row that was selected
     */
    protected void _notifySelect(int rowIndex) {
        List<TableWidgetWatcher> v = Collections.unmodifiableList(_watchers);

        for (TableWidgetWatcher tww : v) {
            tww.tableRowSelected(this, rowIndex);
        }
    }

}

