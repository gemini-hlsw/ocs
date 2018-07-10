package edu.gemini.qpt.ui.util;

import javax.swing.table.AbstractTableModel;

/**
 * A TableModel that represents a multi-column list (as opposed to a spreadsheet-like model). Columns
 * are represented by an enum type.
 */
@SuppressWarnings("serial")
public abstract class ListTableModel<T, C extends Enum<?>> extends AbstractTableModel {

    private final C[] columns;
    
    public ListTableModel(C[] columns) {
        this.columns = columns;
    }

    @Override
    public int findColumn(String name) {
        for (int i = 0; i < columns.length; i++)
            if (columns[i].name().equals(name)) return i;
        return -1;
    }

    @Override
    public Class<?> getColumnClass(int i) {
        return getColumnClass(columns[i]);
    }

    protected Class<?> getColumnClass(@SuppressWarnings("unused") C column) {
        return Object.class;
    }
    
    @Override
    public String getColumnName(int i) {
        return columns[i].name();
    }

    @Override
    public boolean isCellEditable(int arg0, int arg1) {
        return false;
    }

    public int getColumnCount() {
        return columns.length;
    }

    public Object getValueAt(int row, int col) {
        return getValue(getValue(row), columns[col]);
    }

    public int indexOf(Object value) {
        for (int i = 0; i < getRowCount(); i++)
            if (getValue(i).equals(value)) return i;
        return -1;
    }
    
    public abstract T getValue(int row);

    protected abstract Object getValue(T value, C column);    
    
}
