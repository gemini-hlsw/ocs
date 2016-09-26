package jsky.plot;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import jsky.catalog.FieldDesc;
import jsky.catalog.FieldDescAdapter;
import jsky.catalog.MemoryCatalog;
import jsky.coords.gui.SexagesimalTableCellRenderer;
import jsky.util.I18N;
import jsky.util.PrintableWithDialog;
import jsky.util.SaveableWithDialog;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.SortedJTable;

/**
 * A panel for displaying elevation plot data in tabular form.
 *
 * @version $Revision: 7983 $
 * @author Allan Brighton
 */
public class TablePanel extends JPanel implements PrintableWithDialog, SaveableWithDialog {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(TablePanel.class);

    // Tabbed pane containing the data tables
    private JTabbedPane _tabbedPane;

    // An array of tables corresponding to the target positions
    private SortedJTable[] _tables;

    // Provides the model data for the graph and tables
    private ElevationPlotModel _model;

    // Used to format the time values as hours:minutes
    private TableCellRenderer _timeCellRenderer = new SexagesimalTableCellRenderer(false, false);

    // reuse file chooser widget
    private static JFileChooser _fileChooser;

    // Used to format elevation values as strings with one decimal place
    private static NumberFormat _nf1 = NumberFormat.getInstance(Locale.US);

    // Used to format airmass values as strings with two decimal places
    private static NumberFormat _nf2 = NumberFormat.getInstance(Locale.US);

    static {
        _nf1.setMinimumFractionDigits(1);
        _nf1.setMaximumFractionDigits(1);
        _nf2.setMinimumFractionDigits(2);
        _nf2.setMaximumFractionDigits(2);
    }

    // Used to format the elevations (1 decimal place)
    private TableCellRenderer _cellRenderer1 = new DefaultTableCellRenderer() {
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JLabel) component).setText(_nf1.format(((Double) value).doubleValue()));
            return component;
        }
    };

    // Used to format the airmass and parallactic angles (2 decimal places)
    private TableCellRenderer _cellRenderer2 = new DefaultTableCellRenderer() {
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JLabel) component).setText(_nf2.format(((Double) value).doubleValue()));
            return component;
        }
    };


    /**
     * Create a tabbed pane with a table for each of the targets in the elevation plot model.
     */
    public TablePanel() {
        setLayout(new BorderLayout());
        _tabbedPane = new JTabbedPane();
        //_tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT); // XXX causes ArrayIndexOutOfBoundsException in jdk code
        add(_tabbedPane, BorderLayout.CENTER);
    }


    /**
     * Set the model containing the elevation plot data and update the display.
     */
    public void setModel(ElevationPlotModel model) {
        _model = model;
        _update();
        _model.addChangeListener(e -> _update());
    }

    /**
     * Return the model containing the elevation plot data.
     */
    public ElevationPlotModel getModel() {
        return _model;
    }

    /** Update the GUI to reflect what is in the model */
    private void _update() {
        _tabbedPane.removeAll();
        TargetDesc[] targets = _model.getTargets();
        _tables = new SortedJTable[targets.length];
        for (int i = 0; i < targets.length; i++) {
            _tables[i] = new SortedJTable(_model.getTableModel(i));
            _tabbedPane.add(new JScrollPane(_tables[i]), targets[i].getName());

            // Setup cell renderers
            TableColumnModel tcm = _tables[i].getColumnModel();
            tcm.getColumn(0).setCellRenderer(_timeCellRenderer);
            tcm.getColumn(1).setCellRenderer(_cellRenderer1);
            tcm.getColumn(2).setCellRenderer(_cellRenderer2);
            tcm.getColumn(3).setCellRenderer(_cellRenderer2);
        }
    }


    /** Display a dialog for printing the current table */
    public void print() throws PrinterException {
        // print the currently selected table
        int index = _tabbedPane.getSelectedIndex();
        if (index != -1) {
            _tables[index].showPrintDialog(_tabbedPane.getTitleAt(index));
        }
    }

    // Create and return a new file chooser
    private JFileChooser _makeFileChooser() {
        return new JFileChooser();
    }

    /**
     * Display a dialog for saving the currently selected table.
     */
    public void saveAs() {
        try {
            // Save the currently selected table
            int index = _tabbedPane.getSelectedIndex();
            if (index != -1) {
                if (_fileChooser == null) {
                    _fileChooser = _makeFileChooser();
                }
                int option = _fileChooser.showSaveDialog(this);
                if (option == JFileChooser.APPROVE_OPTION && _fileChooser.getSelectedFile() != null) {
                    saveAs(_tables[index], _tabbedPane.getTitleAt(index), _fileChooser.getSelectedFile());
                }
            }
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    /**
     * Save the given table's data to the given file, with the given title.
     */
    public void saveAs(SortedJTable table, String title, File file) throws IOException {
        if (file.exists()) {
            // File already exists.  Prompt for overwrite
            String msg = _I18N.getString("fileOverWritePrompt", file.getName());
            int ans = DialogUtil.confirm(msg);
            if (ans != JOptionPane.YES_OPTION) {
                return;
            }
        }
        TableModel model = table.getModel();
        FieldDesc[] fieldDesc = new FieldDesc[]{
            new FieldDescAdapter(model.getColumnName(0)),
            new FieldDescAdapter(model.getColumnName(1))
        };
        int numRows = model.getRowCount();
        int numCols = model.getColumnCount();
        Vector<Vector<Object>> dataVector = new Vector<>(numRows);
        for (int i = 0; i < numRows; i++) {
            Vector<Object> row = new Vector<>(numCols);
            for (int j = 0; j < numCols; j++) {
                row.add(model.getValueAt(i, j));
            }
            dataVector.add(row);
        }
        MemoryCatalog catalog = new MemoryCatalog(fieldDesc, dataVector);
        catalog.setTitle(title);
        FileOutputStream os = new FileOutputStream(file);
        catalog.saveAs(os);
        os.close();
    }
}


