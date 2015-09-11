// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: RowManipulateTableWidget.java 18053 2009-02-20 20:16:23Z swalker $
//
package jsky.util.gui;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
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

    /**
     * Insert a new row into the table at a given index, not just adding to
     * the end and changing display indices.
     */
    public void absInsertRowAt(Vector row, int index) {
        if (row == null) {
            row = new Vector(getModel().getColumnCount());
        }
        ((DefaultTableModel) getModel()).insertRow(index, row);
    }

    /** Clear the table */
    public void clear() {
        ((DefaultTableModel) getModel()).setRowCount(0);
    }

    /* Set the column headers */
    public void setColumnHeaders(String[] names) {
        ((DefaultTableModel) getModel()).setColumnIdentifiers(names);
    }

    /* Set the column headers */
    public void setColumnHeaders(Vector names) {
        ((DefaultTableModel) getModel()).setColumnIdentifiers(names);
    }

    /* Set the column widths in pixels. */
    public void setColumnWidths(int[] ar) {
        TableColumnModel model = getColumnModel();
        //setAutoResizeMode(AUTO_RESIZE_LAST_COLUMN);
        for (int i = 0; i < ar.length; ++i)
            model.getColumn(i).setPreferredWidth(ar[i]);
    }


    /** Return the value in the cell at the given row and column. */
    public Object getCell(int col, int row) {
        Vector data = ((DefaultTableModel) getModel()).getDataVector();
        return ((Vector) data.get(row)).get(col);
    }

    /** Set the value in the cell at the given row and column. */
    public void setCell(Object value, int col, int row) {
        ((DefaultTableModel) getModel()).setValueAt(value, row, col);
    }

    /** Remove all rows */
    public void removeAllRows() {
        ((DefaultTableModel) getModel()).setRowCount(0);
    }

    /** Remove all columns */
    public void removeAllColumns() {
        ((DefaultTableModel) getModel()).setColumnCount(0);
    }


    /** Set the focus at the given row (actually just select the row and deselect all other rows). */
    public void focusAtRow(int index) {
        getSelectionModel().setSelectionInterval(index, index);
    }


    /** Return the indexes of the selected rows */
    public int[] getSelectedRowIndexes() {
        return getSelectedRows();
    }

    /** Remove the given row */
    public void removeRowAt(int index) {
        ((DefaultTableModel) getModel()).removeRow(index);
    }

    /** Remove the given column */
    public void removeColumnAt(int index) {
        DefaultTableModel model = (DefaultTableModel) getModel();
        int numCols = model.getColumnCount();
        Vector<String> columnNames = new Vector<String>(numCols - 1);
        for (int i = 0; i < numCols; i++) {
            if (i != index) {
                columnNames.add(model.getColumnName(i));
            }
        }
        int numRows = model.getRowCount();
        Vector data = model.getDataVector();
        Vector<Vector<Object>> newData = new Vector<Vector<Object>>(numRows);
        for (int i = 0; i < numRows; i++) {
            Vector<Object> row = new Vector<Object>(numCols - 1);
            for (int j = 0; j < numCols; j++) {
                if (j != index) {
                    row.add(((Vector) data.get(i)).get(j));
                }
            }
            newData.add(row);
        }

        setModel(new DefaultTableModel(newData, columnNames) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
    }


    /**
     * Debugging method that prints a "table row".
     */
    protected void _printTableRow(int index, Vector v) {
        System.out.print(index + ") ");
        for (int i = 0; i < v.size(); ++i) {
            System.out.print("\t\"" + v.elementAt(i) + "\"");
        }
        System.out.println();
    }

    /**
     * Move a row to the front (index 0).
     */
    public void absMoveToFirstRowAt(int index) {
        if ((index <= 0) || (index >= getModel().getRowCount())) {
            return;
        }

        Vector[] va = getAllRowsData();

        Vector vTemp = va[index];

        // Move the rows above the index row down one.
        System.arraycopy(va, 0, va, 1, index);

        va[0] = vTemp;

        //_printTableRow(0, va[0]);
        setRows(va);
    }

    /**
     * Decrement a row's absolute position (move it higher in the table).
     */
    public void absDecrementRowAt(int index) {
        if ((index <= 0) || (index >= getModel().getRowCount())) {
            return;
        }

        Vector[] va = getAllRowsData();

        Vector vTemp = va[index - 1];
        va[index - 1] = va[index];
        va[index] = vTemp;

        setRows(va);
    }

    /**
     * Increment a row's absolute position (move it lower in the table).
     */
    public void absIncrementRowAt(int index) {
        int lastIndex = getModel().getRowCount() - 1;
        if ((index < 0) || (index >= lastIndex)) {
            return;
        }

        Vector[] va = getAllRowsData();

        Vector vTemp = va[index];
        va[index] = va[index + 1];
        va[index + 1] = vTemp;

        setRows(va);
    }

    /**
     * Decrement a row's absolute position (move it higher in the table).
     */
    public void absMoveToLastRowAt(int index) {
        int lastIndex = getModel().getRowCount() - 1;
        if ((index < 0) || (index >= lastIndex)) {
            return;
        }

        Vector[] va = getAllRowsData();

        Vector vTemp = va[index];
        for (int i = index; i < lastIndex; ++i) {
            va[i] = va[i + 1];
        }
        va[lastIndex] = vTemp;

        setRows(va);
    }

    /**
     * Get the data in all the rows in the table.  Each row is an entry
     * in the array of Vectors.  Each Vector contains a row's worth of
     * data.
     */
    public Vector[] getAllRowsData() {
        Vector[] va = new Vector[getModel().getRowCount()];
        getAllRowsData(va);
        return va;
    }

    /**
     * Get the data in all the rows in the table.  Each row is an entry
     * in the array of Vectors.  Each Vector contains a row's worth of
     * data.
     */
    public void getAllRowsData(Vector[] va) {
        int rowCount = getModel().getRowCount();
        Vector data = ((DefaultTableModel) getModel()).getDataVector();
        for (int i = 0; i < rowCount; ++i) {
            va[i] = (Vector) data.get(i);
        }
    }

    /** Set the contents of the table */
    public void setRows(Vector<Object>[] v) {
        int rowCount = v.length;
        Vector<Vector<Object>> data = new Vector<Vector<Object>>(rowCount);
        data.addAll(Arrays.asList(v).subList(0, rowCount));
        DefaultTableModel model = (DefaultTableModel) getModel();
        int colCount = model.getColumnCount();
        Vector<String> header = new Vector<String>(colCount);
        for (int i = 0; i < colCount; ++i) {
            header.add(model.getColumnName(i));
        }
        model.setDataVector(data, header);
    }

    /**
     * Override marimba's method that prints nasty error message if
     * there are no rows. Null vector is ok as result of guidestar search.
     */
    public Vector getRowData(int index) {
        DefaultTableModel model = (DefaultTableModel) getModel();
        // do nothing if one of the parameters is incorrect
        if (index < 0 || index >= model.getRowCount()) {
            return null;
        }
        Vector data = ((DefaultTableModel) getModel()).getDataVector();
        return (Vector) data.get(index);
    }
}

