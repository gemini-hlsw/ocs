// Copyright 2001 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: ObsCatalogTableColumnConfigPanel.java 6424 2005-06-18 13:10:34Z brighton $
//
package jsky.app.ot.gemini.obscat;

import jsky.app.ot.shared.gemini.obscat.ObsCatalogInfo;
import jsky.catalog.gui.TableDisplay;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.*;


/**
 * A panel for choosing which table columns to display for the result of an ObsCatalog query.
 * This version is used instead of TableColumnConfigPanel in order to put the instrument
 * specific columns in separate tabs.
 */
public class ObsCatalogTableColumnConfigPanel extends JPanel {

    // The widget displaying the target table
    private final TableDisplay _tableDisplay;

    // Column names for the configuration table
    private final Vector<String> _columnNames;

    // The table displaying the available non-instrument specific table columns
    private final JTable _table;

    // The tables displaying the available instrument specific table columns
    private final JTable[] _instTables;

    // Number of columns in the target table
    private final int _numTargetColumns = getNumTableColumns();


    /**
     * Constructor.
     *
     * @param tableDisplay the widget displaying the table
     */
    public ObsCatalogTableColumnConfigPanel(TableDisplay tableDisplay) {
        _columnNames = new Vector<String>(2);
        _columnNames.add("Column Name");
        _columnNames.add("Show?");

        _tableDisplay = tableDisplay;

        boolean[] show = _tableDisplay.getShow();
        if (show != null && show.length != _numTargetColumns)
            show = null; // table columns might have changed since last session

        int showOffset = 0;
        String[] colNames = ObsCatalogInfo.getTableColumns(null);
        _table = _makeTable(colNames, show, showOffset);
        showOffset += colNames.length;

        // Add a pane for the instrument specific columns
        final JTabbedPane tabbedPane = new JTabbedPane();
        final int numInst = ObsCatalogInfo.INSTRUMENTS.length;
        _instTables = new JTable[numInst];
        for (int i = 0; i < numInst; i++) {
            final JPanel panel = new JPanel(new BorderLayout());
            colNames = ObsCatalogInfo.getTableColumns(ObsCatalogInfo.INSTRUMENTS[i]);
            _instTables[i] = _makeTable(colNames, show, showOffset);
            showOffset += colNames.length;
            panel.add(_instTables[i], BorderLayout.CENTER);
            tabbedPane.add(panel, ObsCatalogInfo.INSTRUMENTS[i]);
        }

        tabbedPane.setPreferredSize(new Dimension(250, 300));
        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _table, tabbedPane);
        splitPane.setDividerLocation(0.5);
        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
    }

    /**
     * Return the total number of table columns, including the instrument specific columns
     */
    private static int getNumTableColumns() {
        int n = ObsCatalogInfo.TABLE_COLUMNS.length;
        for (String inst : ObsCatalogInfo.INSTRUMENTS) {
            final List l = ObsCatalogInfo.getInstConfigInfoList(inst);
            n += l.size();
        }
        return n;
    }

    // Make and return the table for displaying column names to show or hide.
    // The first argument gives the column headings in the target table (rows in this table).
    // The show array contains the current visibility settings, starting at the given offset,
    // or null if not known.
    private JTable _makeTable(String[] columnIdentifiers, boolean[] show, int showOffset) {
        final JTable table = new JTable();
        table.setBackground(getBackground());

        final JTableHeader header = table.getTableHeader();
        header.setUpdateTableInRealTime(false);
        header.setFont(header.getFont().deriveFont(Font.BOLD));

        _setModel(table, columnIdentifiers, show, showOffset);
        table.sizeColumnsToFit(0);

        return table;
    }

    // Set the model for the given table, based on the given column identifiers and show array.
    // showOffset specifies the offset in the show array that is valid for these arguments.
    private void _setModel(JTable table, String[] columnIdentifiers, boolean[] show, int showOffset) {
        final Vector<Vector<Object>> data = new Vector<Vector<Object>>(columnIdentifiers.length);

        for (int i = 0; i < columnIdentifiers.length; i++) {
            final Vector<Object> row = new Vector<Object>(2);
            row.add(columnIdentifiers[i]);

            final int index = showOffset + i;
            if (show != null && show.length > index) {
                row.add(show[index]);
            } else {
                row.add(Boolean.TRUE);
            }
            data.add(row);
        }

        table.setModel(new DefaultTableModel(data, _columnNames) {
            public Class getColumnClass(int columnIndex) {
                if (columnIndex == 0)
                    return String.class;
                return Boolean.class;
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return (columnIndex == 1);
            }
        });
    }

    /** Apply any changes */
    public void apply() {
        final boolean[] show = new boolean[_numTargetColumns];
        int showOffset = 0;
        _apply(_table, show, showOffset);
        showOffset += ObsCatalogInfo.getNumTableColumns(null);

        final int n = ObsCatalogInfo.INSTRUMENTS.length;
        for (int i = 0; i < n; i++) {
            _apply(_instTables[i], show, showOffset);
            showOffset += ObsCatalogInfo.getNumTableColumns(ObsCatalogInfo.INSTRUMENTS[i]);
        }

        // hide duplicate column names
        final Collection<String> map = new HashSet<String>(_numTargetColumns);
        final String[] ar = ObsCatalogInfo.getTableColumns();
        for(int i = 0; i < ar.length; i++) {
            if (show[i]) {
                if (map.contains(ar[i])) {
                    show[i] = false;
                } else {
                    map.add(ar[i]);
                }
            }
        }

        _tableDisplay.setShow(show);
    }

    /** Apply any changes to the given config table */
    private void _apply(JTable table, boolean[] show, int showOffset) {
        final DefaultTableModel model = (DefaultTableModel) table.getModel();
        final List data = model.getDataVector();
        final int numCols = data.size();
        for (int i = 0; i < numCols; i++) {
            final List row = (List) data.get(i);
            show[showOffset + i] = (Boolean) row.get(1);
        }
    }


    /** Cancel any changes */
    public void cancel() {
        final boolean[] show = _tableDisplay.getShow();
        String[] colNames = ObsCatalogInfo.getTableColumns(null);
        int showOffset = 0;
        _setModel(_table, colNames, show, showOffset);
        showOffset += colNames.length;
        final int n = ObsCatalogInfo.INSTRUMENTS.length;
        for (int i = 0; i < n; i++) {
            colNames = ObsCatalogInfo.getTableColumns(ObsCatalogInfo.INSTRUMENTS[i]);
            _setModel(_instTables[i], colNames, show, showOffset);
            showOffset += colNames.length;
        }
    }
}

