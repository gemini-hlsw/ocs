package edu.gemini.qpt.ui.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;

/**
 * Provides methods to get and set a table's selection as an array. The table
 * in question must use a ListTableModel of an compatible type.
 * @author rnorris
 */
public class ListTableSelectionHelper<T> {

    private final JTable table;
    
    public ListTableSelectionHelper(JTable table) {        
        if (!(table.getModel() instanceof ListTableModel))
            throw new IllegalArgumentException("Sorry, this only works with a ListSelectionModel");        
        this.table = table;
    }

    @SuppressWarnings("unchecked")
    public T[] getSelection() {
        ListSelectionModel sm = table.getSelectionModel();
        if (sm.isSelectionEmpty()) return (T[]) new Object[0];
        List<T> list = new ArrayList<T>();
        ListTableModel<T, ?> ltm = (ListTableModel<T, ?>) table.getModel();
        for (int i = sm.getMinSelectionIndex(); i <= sm.getMaxSelectionIndex(); i++) {
            if (sm.isSelectedIndex(i))
                list.add(ltm.getValue(i));
        }
        return (T[]) list.toArray();
    }
    
    @SuppressWarnings("unchecked")
    public void setSelection(Object... selection) {
        
        // Only do this if the selection is different
        if (Arrays.equals(getSelection(), selection)) return;
        
        // This is O(N^2) .. sorry        
        ListSelectionModel sm = table.getSelectionModel();
        sm.setValueIsAdjusting(true);
        sm.clearSelection();        
        if (selection != null) {
            ListTableModel<T, ?> ltm = (ListTableModel<T, ?>) table.getModel();
            for (Object value: selection) {
                int i = ltm.indexOf(value);
                if (i != -1)    sm.setSelectionInterval(i, i);
            }
        }
        sm.setValueIsAdjusting(false);
    }
    
}
