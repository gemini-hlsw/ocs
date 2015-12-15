package jsky.util.gui;

import javax.swing.table.DefaultTableModel;
import java.util.Arrays;
import java.util.Vector;

/**
 * A TableWidget subclass that supports manipulation of table rows
 * (moving them up and down and to the front and back).
 */
public class RowManipulateTableWidget extends PrintableJTable {

    /** Default constructor */
    public RowManipulateTableWidget() {
        // disable editing by default
        setModel(new DefaultTableModel() {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        setCellSelectionEnabled(false);
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);

        // need to change some code if this is enabled
        getTableHeader().setReorderingAllowed(false);
    }

    /** Clear the table */
    public void clear() {
        ((DefaultTableModel) getModel()).setRowCount(0);
    }

    /* Set the column headers */
    public void setColumnHeaders(String[] names) {
        ((DefaultTableModel) getModel()).setColumnIdentifiers(names);
    }

    /** Set the value in the cell at the given row and column. */
    public void setCell(Object value, int col, int row) {
        getModel().setValueAt(value, row, col);
    }
    /** Set the focus at the given row (actually just select the row and deselect all other rows). */
    public void focusAtRow(int index) {
        getSelectionModel().setSelectionInterval(index, index);
    }


    /** Return the indexes of the selected rows */
    public int[] getSelectedRowIndexes() {
        return getSelectedRows();
    }

    /** Set the contents of the table */
    public void setRows(Vector<Object>[] v) {
        int rowCount = v.length;
        Vector<Vector<Object>> data = new Vector<>(rowCount);
        data.addAll(Arrays.asList(v).subList(0, rowCount));
        DefaultTableModel model = (DefaultTableModel) getModel();
        int colCount = model.getColumnCount();
        Vector<String> header = new Vector<>(colCount);
        for (int i = 0; i < colCount; ++i) {
            header.add(model.getColumnName(i));
        }
        model.setDataVector(data, header);
    }

}

