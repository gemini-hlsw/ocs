package jsky.catalog.gui;

import jsky.catalog.FieldDesc;
import jsky.catalog.QueryResult;
import jsky.catalog.TableQueryResult;
import jsky.util.Preferences;
import jsky.util.PrintableWithDialog;
import jsky.util.SaveableAsHTML;
import jsky.util.Storeable;
import jsky.util.gui.*;
import jsky.coords.gui.SexagesimalTableCellEditor;
import jsky.coords.gui.SexagesimalTableCellRenderer;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.print.PrinterException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This widget displays the contents of a TableQueryResult in a JTable and
 * implements scrolling, editing, searching and sorting methods.
 */
public class TableDisplay extends JPanel
        implements QueryResultDisplay, PrintableWithDialog, SaveableAsHTML, Storeable {
    private final static Logger LOG = Logger.getLogger(TableDisplay.class.getName());

    private static final TableCellRenderer MagnitudeRenderer = new DefaultTableCellRenderer() {
        {
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override public void setValue(final Object value) {
            if (value instanceof Double) {
                setText(String.format("%.2f", (Double)value));
            } else {
                setText("");
            }
        }
    };

    /** The table object containing the data to be displayed */
    private TableQueryResult _tableQueryResult;

    /** Used to display any query results found by following links */
    private final QueryResultDisplay _queryResultDisplay;

    /** Used to scroll the table */
    private final JScrollPane _scrollPane;

    /** widget used to display the table */
    private final SortedJTable _table;

    /** Sum of column widths, used during resize */
    private int _sumColWidths = 0;

    /** If not null, an array specifying which columns to show or hide */
    private boolean[] _show;

    /** Used to remember _show settings */
    private static Map<String, boolean[]> _showTab = new HashMap<>();

    /** Name of file used to remember the _showTab settings between sessions */
    private static final String SHOW_TAB_FILE_NAME = "tableDisplayShowTab";

    /** Restore _showTab from the previous session, if the file exists */
    static {
        try {
            _showTab = (Hashtable) Preferences.getPreferences().deserialize(SHOW_TAB_FILE_NAME);
        } catch (final Exception e) {
            LOG.warning("Could not deserialize TableDisplay preferences from file " + SHOW_TAB_FILE_NAME + ": " + e.getMessage());
        }
    }


    /**
     * Create a TableDisplay for viewing the given table data.
     *
     * @param tableQueryResult the table to use.
     * @param queryResultDisplay used to display any query results (resulting from following links)
     */
    public TableDisplay(final TableQueryResult tableQueryResult, final QueryResultDisplay queryResultDisplay) {
        _queryResultDisplay = queryResultDisplay;
        _table = new SortedJTable();
        setBackground(Color.white);
        if (tableQueryResult != null)
            setModel(tableQueryResult);

        _table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        _table.setRowSelectionAllowed(true);
        _table.setColumnSelectionAllowed(false);

        final JTableHeader header = _table.getTableHeader();
        header.setUpdateTableInRealTime(false);
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        setLayout(new BorderLayout());
        _scrollPane = new JScrollPane(_table);
        add(_scrollPane, BorderLayout.CENTER);

        // handle resize events
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(final ComponentEvent e) {
                resize();
            }
        });
    }

    /**
     * Initialize an empty table. Call setModel() to set the data to display,
     * and setQueryResultDisplay to set the display class to use when following links.
     */
    protected TableDisplay() {
        this(null, null);
    }

    /** Return the widget used to display the table. */
    public SortedJTable getTable() {
        return _table;
    }

    /**
     * If the given query result is a table, display it,
     * otherwise do nothing.
     */
    @Override public void setQueryResult(final QueryResult queryResult) {
        if (queryResult instanceof TableQueryResult)
            setModel((TableQueryResult) queryResult);
    }

    /**
     * Return the current table query result (same as the table model).
     */
    public TableQueryResult getTableQueryResult() {
        return _tableQueryResult;
    }


    /**
     * Set the data to display in the table.
     */
    public void setModel(final TableQueryResult tableQueryResult) {
        _tableQueryResult = tableQueryResult;

        final Object o = _showTab.get(_tableQueryResult.getName());
        if (o != null) {
            _show = (boolean[]) o;
            if (_show.length != _tableQueryResult.getColumnCount())
                _show = null;
        }

        _table.setModel(tableQueryResult);
        setColumnRenderers();
        _sumColWidths = initColumnSizes(_table, _show);
    }

    /**
     * Update the table after the model has changed.
     */
    public void update() {
        _table.setModel(_tableQueryResult);
        setColumnRenderers();
        _sumColWidths = initColumnSizes(_table, _show);
    }


    /**
     * This method picks good column sizes for the given JTable.
     * If all column heads are wider than the column's cells'
     * contents, then you can just use column.sizeWidthToFit().
     *
     * @param table the target JTable
     * @param show if not null, should be an array with a boolean entry for each column
     *             indicating whether the column should be shown or ignored.
     *
     * @return the sum of all the column widths
     */
    protected int initColumnSizes(final JTable table, final boolean[] show) {
        return TableUtil.initColumnSizes(table, show);
    }


    /**
     * Set up any required JTable renderers, so that the values in the query result are displayed
     * correctly by the JTable. This is needed, for example, if a certain column should
     * contain a button or other widget instead of the default text item, or if a value needs
     * special formatting.
     */
    protected void setColumnRenderers() {
        final int numCols = _tableQueryResult.getColumnCount();
        for (int col = 0; col < numCols; col++) {
            final FieldDesc field = _tableQueryResult.getColumnDesc(col);
            final TableColumn column = _table.getColumn(_tableQueryResult.getColumnName(col));

            // linked fields are displayed as buttons
            if (field.hasLink()) {
                column.setCellRenderer(new HyperlinkTableCellRenderer(field, _tableQueryResult));
                column.setCellEditor(new HyperlinkTableCellEditor(field, _tableQueryResult, _queryResultDisplay));
            } else if (field.isRA()) {
                // RA,DEC coordinates are displayed in sexagesimal (hh:mm:ss.sss) notation
                column.setCellRenderer(new SexagesimalTableCellRenderer(true));
                column.setCellEditor(new SexagesimalTableCellEditor(true));
            } else if (field.isDec()) {
                column.setCellRenderer(new SexagesimalTableCellRenderer(false));
                column.setCellEditor(new SexagesimalTableCellEditor(false));
            } else {
                final Class<?> c = _tableQueryResult.getColumnClass(col);
                if (c != null && c.isArray()) {
                    column.setCellRenderer(new ArrayTableCellRenderer());
                } else if (String.class.equals(c) || Object.class.equals(c)) {
                    // center non-numeric columns, to leave more space
                    final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
                    renderer.setHorizontalAlignment(JLabel.CENTER);
                    column.setCellRenderer(renderer);
                } else if (Double.class.equals(c)) {
                    column.setCellRenderer(MagnitudeRenderer);
                }
            }
        }
    }

    /**
     * Display a print dialog to print the contents of this object
     * with the specified table title.
     */
    public void print(final String title) throws PrinterException {
        _table.setTitle(title);
        _table.showPrintDialog();
    }

    /**
     * Display a print dialog to print the contents of this object.
     */
    @Override public void print() throws PrinterException {
        _table.showPrintDialog();
    }

    /** Called when the table is resized */
    public void resize() {
        if (_tableQueryResult == null)
            return;
        final int numCols = _tableQueryResult.getColumnCount();
        if (numCols == 1) {
            _table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            _table.sizeColumnsToFit(0);
        } else {
            if (_sumColWidths < getWidth()) {
                _table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            } else {
                _table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            }
        }
    }

    /**
     * Select the given table row.
     *
     * @param row the index of the row (0 is the first row)
     */
    public void selectRow(final int row) {
        // table may be sorted ...
        final int unsortedRow = _table.getUnsortedRowIndex(row);

        // select the row
        final ListSelectionModel model = _table.getSelectionModel();
        model.addSelectionInterval(unsortedRow, unsortedRow);

        // scroll to make the row visible
        final BoundedRangeModel brm = _scrollPane.getVerticalScrollBar().getModel();
        final int min = brm.getMinimum();
        final int max = brm.getMaximum();
        final int value = brm.getValue();
        final int extent = brm.getExtent();
        final int newvalue = unsortedRow * (max - min) / _tableQueryResult.getRowCount();

        if (newvalue < value || newvalue > value + extent)
            brm.setValue(newvalue - extent / 2);
    }

    /**
     * Deselect the given table row.
     *
     * @param row the index of the row (0 is the first row)
     */
    public void deselectRow(final int row) {
        final int unsortedRow = _table.getUnsortedRowIndex(row);
        _table.getSelectionModel().removeSelectionInterval(unsortedRow, unsortedRow);
    }

    /** Return an array specifying which columns to show, if defined, otherwise null. */
    public boolean[] getShow() {
        return _show;
    }

    /** Set an array specifying which columns to show (or null, for default) */
    public void setShow(final boolean[] show) {
        _show = show;
        //_sumColWidths = initColumnSizes(_table, _show);

        if (_show != null)
            _showTab.put(_tableQueryResult.getName(), show);
        else
            _showTab.remove(_tableQueryResult.getName());

        update();

        // make the change permanent
        try {
            Preferences.getPreferences().serialize(SHOW_TAB_FILE_NAME, _showTab);
        } catch (final Exception e) {
            LOG.warning("Could not deserialize TableDisplay preferences to file " + SHOW_TAB_FILE_NAME + ": " + e.getMessage());
        }
    }


    /** Save the table to the given filename in HTML format */
    @Override public void saveAsHTML(final String filename) throws IOException {
        final int numCols = _table.getColumnCount();
        if (numCols == 0) return;
        final int numRows = _table.getRowCount();

        final FileOutputStream os = new FileOutputStream(filename);
        final PrintStream out = new PrintStream(os);

        // table title
        out.println("<html>");
        out.println("<body>");
        out.println("<table BORDER COLS=" + numCols + " WIDTH=\"100%\" NOSAVE>");
        out.println("<caption>" + _tableQueryResult.getTitle() + "</caption>");

        // column headings
        out.println("<tr>");
        for (int col = 0; col < numCols; col++) {
            if (_show == null || _show[col])
                out.println("<th>" + _table.getColumnName(col) + "</th>");
        }
        out.println("</tr>");

        // data rows
        for (int row = 0; row < numRows; row++) {
            out.println("<tr>");
            for (int col = 0; col < numCols; col++) {
                if (_show == null || _show[col]) {
                    // The renderer might display a different string, so use it to get the string
                    final TableCellRenderer r = _table.getCellRenderer(row, col);
                    final Component c = r.getTableCellRendererComponent(_table,
                                                                        _table.getValueAt(row, col),
                                                                        false, false, row, col);
                    final String s;
                    if (c instanceof JLabel) {
                        s = ((JLabel) r).getText();
                    } else if (c instanceof AbstractButton) {
                        s = ((AbstractButton) r).getText();
                    } else {
                        final Object o = _table.getValueAt(row, col);
                        if (o != null)
                            s = o.toString();
                        else
                            s = " ";
                    }
                    out.println("<td>" + s + "</td>");
                }
            }
            out.println("</tr>");
        }
        out.println("</table>");
        out.println("</body>");
        out.println("</html>");
    }


    /** Store the current settings in a serializable object and return the object. */
    @Override public Object storeSettings() {
        final TableSettings settings = new TableSettings();
        settings.show = _show;
        settings.columnToSort = _table.getSortColumn();
        settings.sortType = _table.getSortType();
        return settings;
    }

    /** Restore the settings previously stored. */
    @Override public boolean restoreSettings(Object obj) {
        if (obj instanceof TableSettings) {
            final TableSettings settings = (TableSettings) obj;
            _table.setSortType(settings.sortType);
            _table.setSortColumn(settings.columnToSort);
            if (settings.show != null && _show != null && settings.show.length == _show.length)
                setShow(settings.show);
            return true;
        }
        return false;
    }


    // Local class for storing and restoring table display settings
    private static class TableSettings implements Serializable {
        boolean[] show;
        int columnToSort;
        int sortType;
    }
}

