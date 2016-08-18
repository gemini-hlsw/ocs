package jsky.catalog.gui;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;
import java.io.File;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jsky.catalog.MemoryCatalog;
import jsky.catalog.QueryResult;
import jsky.catalog.TableQueryResult;
import jsky.util.I18N;
import jsky.util.IApplyCancel;
import jsky.util.PrintableWithDialog;
import jsky.util.Saveable;
import jsky.util.SaveableAsHTML;
import jsky.util.SaveableWithDialog;
import jsky.util.Storeable;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.GridBagUtil;
import jsky.util.gui.SortedJTable;
import jsky.util.gui.SwingUtil;
import jsky.util.gui.TabbedPanel;
import jsky.util.gui.TabbedPanelFrame;
import jsky.util.gui.TabbedPanelInternalFrame;

/**
 * Combines a TableDisplay component for displaying query results in
 * tabular form with a title and some buttons to perform various actions.
 */
public class TableDisplayTool extends JPanel
        implements QueryResultDisplay, Saveable, SaveableWithDialog, SaveableAsHTML,
        PrintableWithDialog, Storeable {
    private static final Logger logger = Logger.getLogger(TableDisplayTool.class.getName());

    // Used to access internationalized strings (see i18n/gui*.properties)
    private static final I18N _I18N = I18N.getInstance(TableDisplayTool.class);

    // The catalog to use
    private TableQueryResult _table;

    // Set to true if the table data has been plotted
    private boolean _plotted = false;

    // Table for displaying query results
    private TableDisplay _tableDisplay;

    // Table for displaying query results
    private SortedJTable _sortedJTable;

    // The object to use to plot the table data
    private TablePlotter _plotter;

    // Title for query results table
    private JLabel _tableTitle;

    // Panel containing command buttons
    private JPanel _buttonPanel;

    // Panel buttons
    private JButton _plotButton;
    private JButton _unplotButton;
    private JButton _unplotAllButton;

    // JFrame or JInternalFrame for _configPanel
    private Component _configFrame;

    // Panel used to configure the table and plot symbol display
    private TabbedPanel _configPanel;

    // Panel inside configPanel used to select columns to display
    private TableColumnConfigPanel _tableConfig;

    // If true, ignore selection events on plot symbols or rows
    private boolean ignoreSelection = false;

    // reuse file chooser widget
    private static JFileChooser _fileChooser;

    // Used to select a table row when the symbol is selected
    private SymbolSelectionListener symbolListener = new SymbolSelectionListener() {

        public void symbolSelected(SymbolSelectionEvent e) {
            if (!ignoreSelection && e.getTable() == _table) {
                ignoreSelection = true;
                try {
                    _tableDisplay.selectRow(e.getRow());
                } catch (Exception ex) {
                }
                ignoreSelection = false;
            }
        }

        public void symbolDeselected(SymbolSelectionEvent e) {
            if (!ignoreSelection && e.getTable() == _table) {
                ignoreSelection = true;
                try {
                    _tableDisplay.deselectRow(e.getRow());
                } catch (Exception ex) {
                }
                ignoreSelection = false;
            }
        }
    };

    // Used to select a plot symbol when the table row is selected
    private ListSelectionListener selectionListener = new ListSelectionListener() {

        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting() || ignoreSelection)
                return;
            ListSelectionModel model = _tableDisplay.getTable().getSelectionModel();
            int first = e.getFirstIndex();
            int last = e.getLastIndex();
            for (int i = first; i <= last; i++) {
                int index = _sortedJTable.getSortedRowIndex(i);
                if (model.isSelectedIndex(i)) {
                    _plotter.selectSymbol(_table, index);
                } else {
                    _plotter.deselectSymbol(_table, index);
                }
            }
        }
    };


    /**
     * Create a TableDisplayTool for viewing the given table data.
     *
     * @param table the table data
     * @param queryResultDisplay object used to display any query results resulting from following links
     * @param plotter object used to plot the table data
     */
    public TableDisplayTool(TableQueryResult table, QueryResultDisplay queryResultDisplay,
                            TablePlotter plotter) {
        _table = table;
        _plotter = plotter;

        makeLayout(queryResultDisplay);

        // try to plot the table after it is displayed (make sure its the event thread)
        SwingUtilities.invokeLater(TableDisplayTool.this::plot);
    }


    /**
     * Do the window layout
     *
     * @param queryResultDisplay object used to display any query results resulting from following links
     */
    protected void makeLayout(QueryResultDisplay queryResultDisplay) {
        _tableDisplay = makeTableDisplay(_table, queryResultDisplay);
        _sortedJTable = _tableDisplay.getTable();

        _tableTitle = new JLabel("", JLabel.CENTER);
        updateTitle();

        _buttonPanel = makeButtonPanel();

        GridBagUtil layout = new GridBagUtil(this);
        layout.add(_tableTitle, 0, 0, 1, 1, 0.0, 0.0,
                   GridBagConstraints.NONE,
                   GridBagConstraints.CENTER,
                   new Insets(3, 0, 3, 0));
        layout.add(_tableDisplay, 0, 1, 1, 1, 1.0, 1.0,
                   GridBagConstraints.BOTH,
                   GridBagConstraints.CENTER,
                   new Insets(0, 0, 0, 0));
        layout.add(_buttonPanel, 0, 2, 1, 1, 0.0, 0.0,
                   GridBagConstraints.HORIZONTAL,
                   GridBagConstraints.CENTER,
                   new Insets(5, 0, 0, 0));
    }

    /** Create the TableDisplay or a derived widget. */
    protected TableDisplay makeTableDisplay(TableQueryResult table, QueryResultDisplay queryResultDisplay) {
        return new TableDisplay(table, queryResultDisplay);
    }


    /** Return the table for displaying query results */
    public TableDisplay getTableDisplay() {
        return _tableDisplay;
    }


    /** make and return the button panel */
    protected JPanel makeButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        _plotButton = new JButton(_I18N.getString("plot"));
        _plotButton.setToolTipText(_I18N.getString("plotTip"));
        panel.add(_plotButton);
        _plotButton.addActionListener(ev -> {
            unplot();
            plot();
        });

        _unplotButton = new JButton(_I18N.getString("unplot"));
        _unplotButton.setToolTipText(_I18N.getString("unplotTip"));
        panel.add(_unplotButton);
        _unplotButton.addActionListener(ev -> unplot());

        _unplotAllButton = new JButton(_I18N.getString("unplotAll"));
        _unplotAllButton.setToolTipText(_I18N.getString("unplotTip"));
        panel.add(_unplotAllButton);
        _unplotAllButton.addActionListener(ev -> unplotAll());

        JButton _configButton = new JButton(_I18N.getString("configure"));
        _configButton.setToolTipText(_I18N.getString("configureTip"));
        panel.add(_configButton);
        _configButton.addActionListener(ev -> configure());

        return panel;
    }


    /** Return the button panel */
    public JPanel getButtonPanel() {
        return _buttonPanel;
    }

    public JButton getPlotButton() {
        return _plotButton;
    }

    public JButton getUnplotButton() {
        return _unplotButton;
    }

    public JButton getUnplotAllButton() {
        return _unplotAllButton;
    }

    /** Plot the contents of the table. */
    public void plot() {
        if (_plotter != null) {
            _tableDisplay.getTable().clearSelection(); // do this or add code to keep selections in sync
            _plotter.addSymbolSelectionListener(symbolListener);
            _tableDisplay.getTable().getSelectionModel().addListSelectionListener(selectionListener);
            try {
                _plotter.plot(_table);
                _plotted = true;
            } catch (Exception e) {
                logger.warning("TableDisplayTool.plot() threw " + e.toString());
                DialogUtil.error(e);
            }
        }
    }

    /** Remove any plot symbols for this table. */
    public void unplot() {
        if (_plotter != null) {
            _plotter.removeSymbolSelectionListener(symbolListener);
            _tableDisplay.getTable().getSelectionModel().removeListSelectionListener(selectionListener);
            _plotter.unplot(_table);
            _plotted = false;
        }
    }

    /** Remove all plot symbols. */
    public void unplotAll() {
        if (_plotter != null) {
            _plotter.removeSymbolSelectionListener(symbolListener);
            _plotter.unplotAll();
            _plotted = false;
        }
    }


    /** Replot any plot symbols for this table. */
    public void replot() {
        unplot();
        plot();
    }


    /** Return the TableQueryResult corresponding to this object */
    public TableQueryResult getTable() {
        return _table;
    }

    /** Return the table for displaying query results */
    protected SortedJTable getSortedJTable() {
        return _sortedJTable;
    }

    /** Return the object to use to plot the table data */
    public TablePlotter getPlotter() {
        return _plotter;
    }

    /** Set the object to use to plot the table data */
    public void setPlotter(TablePlotter plotter) {
        _plotter = plotter;
    }

    /** Pop up a dialog to configure the plot symbols and table display. */
    public void configure() {
        if (_configFrame != null) {
            SwingUtil.showFrame(_configFrame);
            return;
        }

        String title = _I18N.getString("configureTableDisplay");
        JDesktopPane desktop = DialogUtil.getDesktop();
        if (desktop != null) {
            _configFrame = new TabbedPanelInternalFrame(title);
            _configPanel = ((TabbedPanelInternalFrame) _configFrame).getTabbedPanel();
            desktop.add(_configFrame, JLayeredPane.DEFAULT_LAYER);
            desktop.moveToFront(_configFrame);
        } else {
            _configFrame = new TabbedPanelFrame(title);
            _configPanel = ((TabbedPanelFrame) _configFrame).getTabbedPanel();
        }

        addTableColumnConfigPanel();

        if (_plotter != null)
            addPlotterConfigPanel();

        // make sure to get the frame size set correctly
        if (desktop != null)
            ((TabbedPanelInternalFrame) _configFrame).pack();
        else
            ((TabbedPanelFrame) _configFrame).pack();
        _configFrame.setVisible(true);
    }

    /** Add a panel to the config window to configure the symbol plotting */
    protected void addPlotterConfigPanel() {
        final JTabbedPane tabbedPane = _configPanel.getTabbedPane();
        final IApplyCancel symbolConfig = (IApplyCancel) getPlotter().getConfigPanel(getTable());
        tabbedPane.add((JPanel) symbolConfig, _I18N.getString("plotSymbols"));

        ActionListener applyListener = e -> symbolConfig.apply();
        ActionListener cancelListener = e -> symbolConfig.cancel();
        _configPanel.getApplyButton().addActionListener(applyListener);
        _configPanel.getOKButton().addActionListener(applyListener);
        _configPanel.getCancelButton().addActionListener(cancelListener);
    }


    /** Add a panel to the config window to configure the table columns */
    protected void addTableColumnConfigPanel() {
        JTabbedPane tabbedPane = _configPanel.getTabbedPane();
        _tableConfig = new TableColumnConfigPanel(_tableDisplay);
        tabbedPane.add(_tableConfig, _I18N.getString("showTableCols"));

        ActionListener applyListener = e -> _tableConfig.apply();
        ActionListener cancelListener = e -> _tableConfig.cancel();
        _configPanel.getApplyButton().addActionListener(applyListener);
        _configPanel.getOKButton().addActionListener(applyListener);
        _configPanel.getCancelButton().addActionListener(cancelListener);
    }

    /** Panel used to configure the table and plot symbol display */
    public TabbedPanel getConfigPanel() {
        return _configPanel;
    }

    /**
     * Return the name of this component (based on the data being displayed)
     */
    public String getName() {
        if (_table != null)
            return _table.getName();
        return _I18N.getString("table");
    }

    /**
     * Display the given query results. Tabular data is displayed in
     * the table. Other query result types must be implemented in
     * a derived class.
     *
     * @param queryResult an object returned from the Catalog query method.
     */
    public void setQueryResult(QueryResult queryResult) {
        if (queryResult instanceof TableQueryResult) {
            if (_plotted && _plotter != null)
                _plotter.unplot(_table);

            _table = (TableQueryResult) queryResult;

            // check if more data was available than was returned
            String title = _table.getTitle() + " (" + _table.getRowCount() + (_table.isMore() ? "+)" : ")");
            _tableTitle.setText(title);
            _tableDisplay.setModel(_table);
            _tableDisplay.resize();

            if (_plotter != null)
                _plotter.plot(_table);

            updateConfigPanel();
        }
    }

    /** Update the table config panel, if needed */
    protected void updateConfigPanel() {
        if (_tableConfig != null) {
            _tableConfig.cancel();
        }
    }


    /**
     * Create and return a new file chooser to be used to select a local catalog file
     * to open.
     */
    protected JFileChooser makeFileChooser() {
        return new JFileChooser(new File("."));
    }


    /**
     * Pop up a dialog to ask the user for a file name, and then save the contents of the
     * table to the selected file.
     */
    public void saveAs() {
        if (_table instanceof Saveable) {
            if (_fileChooser == null) {
                _fileChooser = makeFileChooser();
            }
            int option = _fileChooser.showSaveDialog(this);
            if (option == JFileChooser.APPROVE_OPTION && _fileChooser.getSelectedFile() != null) {
                saveAs(_fileChooser.getSelectedFile().getAbsolutePath());
            }
        } else {
            DialogUtil.error(_I18N.getString("saveNotSupportedForTableType") + ": " + _table.getClass());
        }
    }


    /**
     * Save the table to the given file.
     */
    public void saveAs(String filename) {
        if (_table instanceof Saveable) {
            try {
                File file = new File(filename);
                if (file.exists()) {
                    // File already exists.  Prompt for overwrite
                    String msg = _I18N.getString("fileOverWritePrompt", filename);
                    int ans = DialogUtil.confirm(msg);
                    if (ans != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                ((Saveable) _table).saveAs(filename);
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        } else {
            DialogUtil.error(_I18N.getString("saveNotSupportedForTableType") + ": " + _table.getClass());
        }
    }


    /**
     * Save the table to the given file in HTML format.
     */
    public void saveAsHTML(String filename) {
        try {
            File file = new File(filename);
            if (file.exists()) {
                // File already exists.  Prompt for overwrite
                String msg = _I18N.getString("fileOverWritePrompt", filename);
                int ans = DialogUtil.confirm(msg);
                if (ans != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            _tableDisplay.saveAsHTML(filename);
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    /**
     * Display a print dialog to print the contents of this object.
     */
    public void print() throws PrinterException {
        _tableDisplay.print(_tableTitle.getText());
    }

    /** Update the table and frame titles to show the table title and number of rows */
    protected void updateTitle() {
        if (_table != null) {
            String titleString = _table.getTitle() + " (" + _table.getRowCount() + ")";
            setName(titleString);
            _tableTitle.setText(titleString);
        }
    }


    /**
     * Add an empty row to the table.
     */
    public void addRow() {
        addRow(null);
    }

    /**
     * Add a row to the table.
     */
    public void addRow(Vector<Object> v) {
        _tableDisplay.getTable().addRow(v);
        _tableDisplay.update();
        updateTitle();
    }

    /**
     * Update a row in the table with the new data.
     * An exception will be thrown if the row index is
     * out of range or the vector has the wrong size.
     */
    public void updateRow(int rowIndex, Vector<Object> v) {
        TableQueryResult table = _tableDisplay.getTableQueryResult();
        for (int colIndex = 0; colIndex < v.size(); colIndex++) {
            table.setValueAt(v.get(colIndex), rowIndex, colIndex);
        }
        _tableDisplay.update();
    }


    /**
     * Delete the selected rows.
     */
    public void deleteSelectedRows() {
        SortedJTable t = _tableDisplay.getTable();
        int[] selected = t.getSelectedRows();
        t.removeRows(selected);
        _tableDisplay.update();
        if (_plotter != null && _plotted) {
            unplot();
            plot();
        }
        updateTitle();
    }


    /**
     * Set the editable state of the cells in the displayed table.
     */
    public void setTableCellsEditable(boolean b) {
        TableQueryResult queryResult = _tableDisplay.getTableQueryResult();
        if (queryResult instanceof MemoryCatalog)
            ((MemoryCatalog) queryResult).setReadOnly(!b);
    }


    /**
     * Return the number of rows in the table.
     */
    public int getRowCount() {
        return _tableDisplay.getTableQueryResult().getRowCount();
    }


    /** Store the current settings in a serializable object and return the object. */
    public Object storeSettings() {
        return _tableDisplay.storeSettings();
    }

    /** Restore the settings previously stored. */
    public boolean restoreSettings(Object obj) {
        return _tableDisplay.restoreSettings(obj);
    }
}

