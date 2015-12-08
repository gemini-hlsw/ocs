package jsky.util.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.IllegalArgumentException;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import jsky.util.Preferences;
import jsky.util.Resources;

/**
 * A JTable that allows the user to sort the table by clicking on the
 * column headers.  An icon is shown in the sort column to indicate the
 * sort.  Ascending and descending sorting is supported.
 * <P>
 * Sorting depends on the java.lang.Comparable interface.  SortedJTable
 * will attempt to compare the cell contents using Comparable.  If the
 * contents are not Comparable, then their toString() values are compared.
 * Note that all the Java primitive wrapper objects (Integer, Double, etc)
 * implement Comparable, so you'll get better sorting (1, 5, 10 instead of
 * 1, 10, 5) if you use, for example, Integers instead of Strings.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		10/27/99
 *
 * @author              Kong Eu Tak (for Swing Connection Article)
 * @author		J. Jones / 588
 * @author		A. Brighton (changes, additions, fixes, performance improvements)
 *
 * @see			java.lang.Comparable
 **/
public class SortedJTable extends PrintableJTable
        implements MouseListener, TableModelListener {

    /** Ascending sort type. **/
    public static final int ASCENDING = 1;

    /** Descending sort type. **/
    public static final int DESCENDING = -1;

    // These are used for storing user preferences
    private static final String SORT_COL_KEY = ".sortCol";
    private static final String SORT_ORDER_KEY = ".sortOrder";

    private TableModel _realModel;
    private int _columnToSort = -1;
    private int _sortType = ASCENDING;
    private int[] _mapToSorted;
    private int _numColumns;

    // listens for changes in the table model
    private TableModelListener tableModelListener;

    // These are here (instead of in CustomHeaderRenderer) so that they can be static.
    // They're static so that they're only loaded once.
    private static Icon _ascendingIcon = null;
    private static Icon _descendingIcon = null;

    // If not null, the key to use to save the sorting preferences
    private String _sortColumnPrefsKey;

    // Set to false to disable table sorting
    private boolean _sortingAllowed = true;


    /**
     * Constructs a JTable which is initialized with model as the data model,
     * a default column model, and a default selection model.
     **/
    public SortedJTable(TableModel model) {
        this();
        setModel(model);
    }

    /** Default constructor */
    public SortedJTable() {
        getTableHeader().addMouseListener(this);
    }

    public void setSortingAllowed(boolean sortingAllowed) {
        _sortingAllowed = sortingAllowed;
    }

    /**
     * Set the table model and arrange to have it sorted when the user
     * clicks on the header.
     */
    public void setModel(TableModel model) {
        _realModel = model;

        // Insert the model wrapper which translates to the sorted rows
        super.setModel(new ModelWrapper());

        // Install the header renderers
        _numColumns = getColumnCount();
        if (_sortingAllowed) {
            for (int i = 0; i < _numColumns; ++i) {
                setCustomHeaderRenderer(i);
            }
        }

        tableModelListener = e -> {
            // Automatically resort since the table data has changed
            doSort();

            // If number of columns has changed, reinstall the custom column headers
            if (e.getColumn() > _numColumns || e.getColumn() == TableModelEvent.ALL_COLUMNS) {
                _numColumns = getColumnCount();

                // Reinstall the header renderers if necessary,
                // preserving the tool tip text of the original header renderer
                for (int i = 0; i < _numColumns; ++i) {
                    setCustomHeaderRenderer(i);
                }
            }
        };
        // Install the listener that will automatically sort when new cells are added
        _realModel.removeTableModelListener(tableModelListener);
        _realModel.addTableModelListener(tableModelListener);
        doSort();
    }


    /**
     * Returns the column number of the current sort column.
     *
     * @return	current sort column number
     **/
    public int getSortColumn() {
        return _columnToSort;
    }

    /**
     * Sets the sort column.  The table will be resorted.
     *
     * @param	column	new sort column
     **/
    public void setSortColumn(int column) {
        if (column >= getColumnCount()) {
            throw new IllegalArgumentException("Column number is out of range.");
        }

        _columnToSort = column;

        sortAndUpdate();
    }

    /** Return the index of the given row after sorting */
    public int getUnsortedRowIndex(int row) {
        if (_mapToSorted != null) {
            for (int i = 0; i < _mapToSorted.length; i++)
                if (_mapToSorted[i] == row)
                    return i;
        }
        return row;
    }

    /** Return the index of the given sorted row before sorting */
    public int getSortedRowIndex(int row) {
        if (_mapToSorted != null)
            return _mapToSorted[row];
        return row;
    }

    /**
     * Returns whether the current sort is ASCENDING or DESCENDING.
     *
     * @return	ASCENDING or DESCENDING
     **/
    public int getSortType() {
        return _sortType;
    }

    /**
     * Sets the sort type.  Valid values are ASCENDING and DESCENDING.
     *
     * @param	type	new sort type
     **/
    public void setSortType(int type) {
        if (type != ASCENDING && type != DESCENDING) {
            throw new IllegalArgumentException("Invalid sort type (must be ASCENDING or DESCENDING).");
        }

        _sortType = type;

        sortAndUpdate();
    }

    /**
     * Remove the given rows from the table (assumes use of DefaultTableModel).
     *
     * @param rows array of row indexes
     */
    public void removeRows(int[] rows) {
        if (!(_realModel instanceof DefaultTableModel)) {
            throw new RuntimeException("removeRows() requires a DefaultTableModel");
        }
        DefaultTableModel model = (DefaultTableModel) _realModel;

        // avoid multiple updates
        model.removeTableModelListener(tableModelListener);

        if (_mapToSorted != null) {
            for (int i = 0; i < rows.length; i++)
                rows[i] = _mapToSorted[rows[i]];
        }

        // sort the row indexes
        Arrays.sort(rows);

        // remove rows in reverse order, so the indexes don't change
        for (int i = rows.length - 1; i >= 0; i--) {
            model.removeRow(rows[i]);
        }
        setModel(model);
    }


    /**
     * Add the given row in the table.
     * The argument may be null, in which case an empty row is inserted
     *  (that can be edited by the user).
     */
    public void addRow(final Vector<Object> row) {
        if (!(_realModel instanceof DefaultTableModel)) {
            throw new RuntimeException("addRow() requires a DefaultTableModel");
        }
        final DefaultTableModel model = (DefaultTableModel) _realModel;
        if (row == null) {
            // generate a dummy row
            final int n = model.getColumnCount();
            final Vector<Object> emptyRow = new Vector<>(n);
            for (int i = 0; i < n; i++) {
                emptyRow.add(null);
            }
            model.addRow(emptyRow);
        } else {
            model.addRow(row);
        }
        setModel(model);
    }


    /**
     * Performs the sort.
     **/
    protected void doSort() {
        if (_columnToSort < 0) {
            _mapToSorted = null;
            return;
        }

        int rowCount = _realModel.getRowCount();
        if (_mapToSorted == null || (_mapToSorted.length < rowCount)) {
            _mapToSorted = new int[((rowCount / 50) + 1) * 50];
        }

        Object[] a = new Object[rowCount];
        for (int i = 0; i < rowCount; i++) {
            _mapToSorted[i] = i;
            a[i] = _realModel.getValueAt(i, _columnToSort);
        }

        quickSort(a, 0, rowCount - 1);
    }


    /** Compare two objects, which may or may not be Comparables, or nulls */
    @SuppressWarnings("unchecked")
    protected int compareObjects(Object a, Object b) {
        if (a instanceof Comparable) {
            if (b instanceof Comparable)
                return ((Comparable) a).compareTo(b) * _sortType;
            else
                return _sortType;
        }
        if (b instanceof Comparable) {
            return -1 * _sortType;
        }
        return 0;
    }


    /**
     * Do a quick sort on the given array, which contains column values.
     * As a result, the index array "_mapToSorted" is modified, not the
     * paramater array.
     *
     * @param a an array of values for the column to sort
     * @param lo0 starting index in the array
     * @param hi0 end index in array
     */
    protected void quickSort(Object a[], int lo0, int hi0) {
        int lo = lo0;
        int hi = hi0;

        if (hi0 > lo0) {
            Object mid = a[_mapToSorted[(lo0 + hi0) / 2]];
            while (lo <= hi) {
                while (lo < hi0 && compareObjects(a[_mapToSorted[lo]], mid) < 0)
                    lo += 1;
                while (hi > lo0 && compareObjects(a[_mapToSorted[hi]], mid) > 0)
                    hi -= 1;
                if (lo <= hi) {
                    int tmp = _mapToSorted[lo];
                    _mapToSorted[lo] = _mapToSorted[hi];
                    _mapToSorted[hi] = tmp;
                    lo += 1;
                    hi -= 1;
                }
            }
            if (lo0 < hi)
                quickSort(a, lo0, hi);
            if (lo < hi0)
                quickSort(a, lo, hi0);
        }
    }

    public void mouseEntered(MouseEvent m) {
    }

    public void mouseExited(MouseEvent m) {
    }

    public void mousePressed(MouseEvent m) {
    }
    // Install a listener to keep track of column order changes so we can remember them
    //TableColumnModel tcModel = getColumnModel();
    //tcModel.removeColumnModelListener(this);
    //tcModel.addColumnModelListener(this);

    public void mouseReleased(MouseEvent m) {
    }

    /**
     * If user clicks on column header, perform sort and toggle sort ordering
     * if the sort column was already the sort column.
     **/
    public void mouseClicked(MouseEvent m) {
        if (!_sortingAllowed) return;
        int targetCol = convertColumnIndexToModel(getTableHeader().columnAtPoint(m.getPoint()));

        if (targetCol == _columnToSort) {
            if (_sortType == ASCENDING)
                _sortType = DESCENDING;
            else
                _columnToSort = -1;
        } else {
            _columnToSort = targetCol;

            _sortType = ASCENDING;
        }

        sortAndUpdate();
    }

    /**
     * Sorts the table, notifies the model, and redraws the header.
     **/
    protected void sortAndUpdate() {
        getSelectionModel().clearSelection();
        doSort();

        getTableHeader().repaint();

        if (_sortColumnPrefsKey != null)
            _savePrefs();
    }


    /**
     * Remember the user's selection for the column to sort and the
     * sorting direction and restore it in the next session.
     *
     * @param key the key to save the information under
     * @param defaultSortCol the default sort column (-1 if not known)
     * @param defaultSortOrder the default sort order: one of (ASCENDING, DESCENDING)
     * @see jsky.util.Preferences
     */
    public void rememberSortColumn(String key, int defaultSortCol, int defaultSortOrder) {
        if (!_sortingAllowed) return;
        _sortColumnPrefsKey = key;
        _columnToSort = defaultSortCol;
        _sortType = defaultSortOrder;
        if (key != null)
            _loadPrefs();
    }


    // Save the user's preferences for column sorting
    private void _savePrefs() {
        String sortColumnKey = _sortColumnPrefsKey + SORT_COL_KEY;
        String sortOrderKey = _sortColumnPrefsKey + SORT_ORDER_KEY;
        if (_columnToSort != -1) {
            String sortColStr = String.valueOf(_columnToSort);
            String sortOrderStr = String.valueOf(_sortType);
            Preferences.set(sortColumnKey, sortColStr);
            Preferences.set(sortOrderKey, sortOrderStr);
        } else {
            Preferences.unset(sortColumnKey);
            Preferences.unset(sortOrderKey);
        }
    }

    // Load and restore the user's preferences for column sorting
    private void _loadPrefs() {
        String sortColumnKey = _sortColumnPrefsKey + SORT_COL_KEY;
        String sortOrderKey = _sortColumnPrefsKey + SORT_ORDER_KEY;

        String sortColStr = Preferences.get(sortColumnKey);
        if (sortColStr != null) {
            _columnToSort = Integer.parseInt(sortColStr);
            String sortOrderStr = Preferences.get(sortOrderKey);
            if (sortOrderStr != null) {
                _sortType = Integer.parseInt(sortOrderStr);
            }
        }
        sortAndUpdate();
    }


    /**
     * Assigns a new CustomHeaderRenderer to a column,
     * preserving state of the existing header renderer.
     *
     * @param	i	set header for this column
     **/
    protected void setCustomHeaderRenderer(int i) {
        TableColumn column = getColumn(getColumnName(i));
        TableCellRenderer rend = TableUtil.getDefaultRenderer(this, column);
        if (!(rend instanceof CustomHeaderRenderer)) {
            CustomHeaderRenderer newHeader = new CustomHeaderRenderer();

            // Preserve the ToolTipText of the existing header renderer
            if (rend instanceof DefaultTableCellRenderer) {
                newHeader.setToolTipText(((DefaultTableCellRenderer) rend).getToolTipText());
            }

            column.setHeaderRenderer(newHeader);
        }
    }


    /**
     * Wraps the original table model by mapping rows to their
     * sorted row equivalents.
     **/
    protected class ModelWrapper implements TableModel {

        public void addTableModelListener(TableModelListener l) {
            _realModel.addTableModelListener(l);
        }

        public Class<?> getColumnClass(int index) {
            return _realModel.getColumnClass(index);
        }

        public int getColumnCount() {
            return _realModel.getColumnCount();
        }

        public String getColumnName(int index) {
            return _realModel.getColumnName(index);
        }

        public int getRowCount() {
            return _realModel.getRowCount();
        }

        public Object getValueAt(int row, int col) {
            if (_mapToSorted != null)
                row = _mapToSorted[row];
            return _realModel.getValueAt(row, col);
        }

        public boolean isCellEditable(int row, int col) {
            if (_mapToSorted != null)
                row = _mapToSorted[row];
            return _realModel.isCellEditable(row, col);
        }

        public void removeTableModelListener(TableModelListener l) {
            _realModel.removeTableModelListener(l);
        }

        public void setValueAt(Object value, int row, int col) {
            if (_mapToSorted != null)
                row = _mapToSorted[row];
            _realModel.setValueAt(value, row, col);
        }
    }

    /**
     * Renders a header with the appropriate sort icon.
     **/
    protected class CustomHeaderRenderer extends DefaultTableCellRenderer {

        public CustomHeaderRenderer() {
            super();

            setHorizontalAlignment(SwingConstants.CENTER);
            setHorizontalTextPosition(SwingConstants.RIGHT);
            setVerticalTextPosition(SwingConstants.CENTER);

            if (_ascendingIcon == null) {
                _ascendingIcon = Resources.getIcon("AscendSort.gif");
            }

            if (_descendingIcon == null) {
                _descendingIcon = Resources.getIcon("DescendSort.gif");
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean hasFocus, int row, int col) {
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                setForeground(header.getForeground());
                setBackground(header.getBackground());
                setFont(header.getFont());
            }

            setText((value == null) ? "" : value.toString());
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));

            if (table.convertColumnIndexToModel(col) == _columnToSort) {
                setIcon((_sortType == ASCENDING) ? _ascendingIcon : _descendingIcon);
            } else {
                setIcon(null);
            }

            return this;
        }
    }


    /**
     * test main
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("SortedJTable");
        TableModel dataModel = new AbstractTableModel() {

            public int getColumnCount() {
                return 10;
            }

            public int getRowCount() {
                return 10;
            }

            public Object getValueAt(int row, int col) {
                return row * col;
            }
        };
        SortedJTable table = new SortedJTable(dataModel);

        frame.getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}
