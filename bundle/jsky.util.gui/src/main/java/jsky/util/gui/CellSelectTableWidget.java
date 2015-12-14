package jsky.util.gui;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * A TableWidget subclass that supports selection of individual cells
 * (as opposed to just entire rows as int org.freebongo.gui.TableWidget).
 */
public class CellSelectTableWidget extends RowManipulateTableWidget {

    private List<CellSelectTableWatcher> _watchers = new ArrayList<>();

    /**
     * Return true if the cell at the given row and column is selected
     */
    public boolean isSelected(int row, int col) {
        return getSelectionModel().isSelectedIndex(row)
                && getColumnModel().getSelectionModel().isSelectedIndex(col);
    }

    /**
     * Construct the CellSelectTableWidget, setting the selectMode to NONE.
     */
    public CellSelectTableWidget() {
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(true);
        setCellSelectionEnabled(true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setShowHorizontalLines(true);

        // track row selections
        getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int col = getSelectedColumn();
                int row = getSelectionModel().getLeadSelectionIndex();
                if (isSelected(row, col)) {
                    cellSelected(col, row);
                }
            }
        });

        // track column selections
        getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            public void columnAdded(TableColumnModelEvent e) {
                selectCell(e.getToIndex(), getSelectionModel().getLeadSelectionIndex());
            }
            public void columnRemoved(TableColumnModelEvent e) {
            }
            public void columnMoved(TableColumnModelEvent e) {
            }
            public void columnMarginChanged(ChangeEvent e) {
            }
            public void columnSelectionChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int col = getColumnModel().getSelectionModel().getLeadSelectionIndex();
                    int row = getSelectedRow();
                    if (isSelected(row, col)) {
                        cellSelected(col, row);
                    }
                }
            }
        });
    }

    /**
     * Focus at a particular cell.
     */
    public void focusAtCell(int colIndex, int rowIndex) {
        selectCell(colIndex, rowIndex);
    }

    /**
     * Select a particular cell.
     */
    public void selectCell(int colIndex, int rowIndex) {
        if ((colIndex < 0) || (colIndex >= getModel().getColumnCount())) {
            return;
        }
        if ((rowIndex < 0) || (rowIndex >= getModel().getRowCount())) {
            return;
        }
        changeSelection(rowIndex, colIndex, false, false);
    }


    /**
     * Get the column and row indices (in that order) of the selected cell.
     * Returns { -1, -1 } if no column or row is selected.
     */
    public int[] getSelectedCoordinates() {
        int colIndex = getSelectedColumn();
        int rowIndex = getSelectedRow();
        return new int[]{colIndex, rowIndex};
    }

    /** Add an empty row to the table */
    public void addRow() {
        DefaultTableModel model = (DefaultTableModel) getModel();
        int columnCount = model.getColumnCount();
        Vector<Object> v = new Vector<>(columnCount);
        model.addRow(v);
    }

    /**
     * The given cell was selected.
     */
    public void cellSelected(int colIndex, int rowIndex) {
        if ((colIndex < 0) || (colIndex >= getModel().getColumnCount())) {
            return;
        }
        if ((rowIndex < 0) || (rowIndex >= getModel().getRowCount())) {
            return;
        }

        List<CellSelectTableWatcher> v ;
        synchronized (this) {
            v = new ArrayList<>(_watchers);
        }

        for (CellSelectTableWatcher aV : v) {
            aV.cellSelected(this, colIndex, rowIndex);
        }
    }

    /**
     * Add a CellSelectTableWatcher.
     */
    public synchronized final void addWatcher(CellSelectTableWatcher cstw) {
        if (!_watchers.contains(cstw)) {
            _watchers.add(cstw);
        }
    }

    /**
     * Delete a CellSelectTableWatcher.
     */
    public synchronized final void deleteWatcher(CellSelectTableWatcher cstw) {
        _watchers.remove(cstw);
    }

    /**
     * test main
     */
    @SuppressWarnings("rawtypes")
    public static void main(String[] args) {
        JFrame frame = new JFrame("CellSelectTableWidget");

        CellSelectTableWidget table = new CellSelectTableWidget();
        String[] headers = new String[]{"One", "Two", "Three", "Four"};
        table.setColumnHeaders(headers);
        Vector<Object>[] v = new Vector[5];
        for (int i = 0; i < v.length; i++) {
            v[i] = new Vector<>(4);
            for (int j = 0; j < headers.length; j++)
                v[i].add("cell " + i + ", " + j);
        }
        table.setRows(v);
        table.addWatcher((w, colIndex, rowIndex) -> System.out.println("tableCellSelected: " + rowIndex + ", " + colIndex));

        frame.getContentPane().add("Center", new JScrollPane(table));
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}

