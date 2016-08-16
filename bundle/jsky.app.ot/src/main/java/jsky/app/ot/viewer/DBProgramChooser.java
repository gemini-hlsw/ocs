// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: DBProgramChooser.java 7450 2006-11-22 21:26:45Z shane $
//

package jsky.app.ot.viewer;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.util.DBProgramInfo;
import jsky.app.ot.OT;
import jsky.app.ot.shared.spModel.util.DBProgramListFunctor;
import jsky.util.gui.Resources;
import jsky.app.ot.viewer.open.KeySelectionCombo;
import jsky.util.Preferences;
import jsky.util.gui.GridBagUtil;
import jsky.util.gui.SortedJTable;
import jsky.util.gui.TableUtil;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import static java.awt.GridBagConstraints.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.util.*;
import java.util.List;

/**
 * This class implements a JFileChooser-like popup window for
 * selecting programs in an IDBDatabase.
 */
public final class DBProgramChooser extends JDialog implements ListSelectionListener {
    private enum Col {
        name("Name"),
        id("Id"),
        modified("Modified")
        ;

        final String display;
        Col(String display) { this.display = display; }

        public static Vector<String> toHeadings() {
            final Vector<String> res = new Vector<>();
            for (final Col c : values()) res.add(c.display);
            return res;
        }
    }

    // Icon to use in the list
    private final static Icon _ICON = Resources.getIcon("program_small.png");

    // Displays the list of available science programs
    private final SortedJTable _table;

    // Dialog buttons
    private JButton _applyButton;

    // Displays the selected science program title
    private JTextField _programName;

    /** The object representing the selected programs (List of DBProgramInfo) */
    private List<DBProgramInfo> _selectedPrograms = new ArrayList<>();

    // defines a panel to filter out programs based on site and program type (may be null)
    private final IDBProgramChooserFilter _filter;

    // Used to format the timestamp
    private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT);
    private static final DateFormat TIME_FORMAT = DateFormat.getTimeInstance(DateFormat.SHORT);

    /**
     * Return a dialog to use to select science programs to delete.
     */
    public static DBProgramChooser getRemoveDialog(IDBProgramChooserFilter filter) {
        return new DBProgramChooser("Select the Science Programs to Delete:",
                                    "Remove", "Cancel", filter);
    }

    /**
     * Return a dialog to use to select science programs to export.
     */
    public static DBProgramChooser getExportDialog(IDBProgramChooserFilter filter) {
        return new DBProgramChooser("Select the Science Programs to Export:",
                                    "Export", "Cancel", filter);
    }


    /**
     * A dialog to use to select a science program from a list of available programs.
     *
     * @param titleStr the title for the dialog
     * @param applyStr the label for the button confirms the selection
     * @param cancelStr the label for the button that cancels the operation
     * @param filter defines a panel to filter out programs based on site and program type
     */
    public DBProgramChooser(String titleStr, String applyStr, String cancelStr,
                            IDBProgramChooserFilter filter) {
        super((JFrame) null, "Program Chooser", true);

        final KeySelectionCombo combo = new KeySelectionCombo(OT.getKeyChain(), tuple2Option -> refresh());
        combo.refresh();

        _filter = filter;
        _filter.addActionListener(e -> refresh());

        _table = new SortedJTable() {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        _table.setCellSelectionEnabled(false);
        _table.setRowSelectionAllowed(true);
        _table.setColumnSelectionAllowed(false);
        _table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        _table.getSelectionModel().addListSelectionListener(this);
        _table.setShowHorizontalLines(false);
        _table.setShowVerticalLines(false);
        _table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        _table.setModel(new DefaultTableModel(Col.toHeadings(), 0));
        _table.rememberSortColumn(getClass().getName() + ".tableSort", Col.id.ordinal(), SortedJTable.ASCENDING);

        // make double-click in the list a short cut for pressing the apply button
        _table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    _applyButton.doClick();
                }
            }
        });

        final JScrollPane scrollPane = new JScrollPane(_table);
        scrollPane.getViewport().setBackground(Color.white);

        final JPanel filterPanel   = new JPanel();
        final GridBagUtil fpLayout = new GridBagUtil(filterPanel);
        fpLayout.add(new JLabel("Key"),       0, 0, 1, 1, 0.0, 0.0, NONE,       EAST,   new Insets(0, 0, 0, 6));
        fpLayout.add(combo.peer(),            1, 0, 1, 1, 0.0, 0.0, NONE,       WEST,   new Insets(0, 0, 0, 0));
        fpLayout.add(new JPanel(),            2, 0, 1, 1, 1.0, 0.0, HORIZONTAL, CENTER, new Insets(0, 0, 0, 0));
        fpLayout.add(filter.getFilterPanel(), 3, 0, 1, 1, 0.0, 0.0, NONE,       EAST,   new Insets(0, 0, 0, 0));

        final JPanel buttonPanel = _makeButtonPanel(applyStr, cancelStr);
        final JLabel topLabel    = new JLabel(titleStr);

        _programName = new JTextField();
        _programName.addActionListener(new ApplyListener());
        _programName.setEditable(false);

        // A FlowLayout JPanel for the buttons
        final JLabel openLabel = new JLabel(applyStr + ":");

        final Container contentPane = getContentPane();
        final GridBagUtil layout = new GridBagUtil(contentPane);
        layout.add(topLabel,     0, 0, 2, 1, 0., 0., HORIZONTAL, WEST,   new Insets(11, 11, 0, 11));
        layout.add(scrollPane,   0, 1, 2, 1, 1., 1., BOTH,       CENTER, new Insets(11, 11, 0, 11));
        layout.add(filterPanel,  0, 2, 2, 1, 1., 0., HORIZONTAL, WEST,   new Insets(11, 11, 0, 11));
        layout.add(openLabel,    0, 3, 1, 1, 0., 0., NONE,       WEST,   new Insets(11, 11, 0,  0));
        layout.add(_programName, 1, 3, 1, 1, 0., 0., HORIZONTAL, WEST,   new Insets(11, 11, 0, 11));
        layout.add(buttonPanel,  0, 4, 2, 1, 0., 0., HORIZONTAL, WEST,   new Insets(11, 11, 6, 11));

        Preferences.manageSize(scrollPane, new Dimension(950, 300), getClass().getName() + ".size");
        Preferences.manageLocation(this);
        pack();
    }


    // Make and return the lower button panel with the given button labels
    private JPanel _makeButtonPanel(String applyStr, String cancelStr) {
        _applyButton = new JButton(applyStr);
        _applyButton.setActionCommand(applyStr);
        _applyButton.addActionListener(new ApplyListener());

        JButton cancelButton = new JButton(cancelStr);
        cancelButton.setActionCommand(cancelStr);
        cancelButton.addActionListener(new CancelListener());

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        panel.add(_applyButton);
        panel.add(cancelButton);

        return panel;
    }

    public void refresh() {
        final IDBDatabaseService     db = SPDB.get();
        final DBProgramListFunctor   f0 = new DBProgramListFunctor(DBProgramListFunctor.EMPTY_PROGRAM_ID_OR_READABLE);
        final DBProgramListFunctor   f1 = db.getQueryRunner(OT.getUser()).queryPrograms(f0);
        final List<DBProgramInfo> progs = f1.getList();

        final List<DBProgramInfo> filteredProgs = _filter.filter(db, progs);

        final Vector<Vector<Object>> data = new Vector<>(filteredProgs.size());
        for (DBProgramInfo progInfo : filteredProgs) {
            final Vector<Object> row = new Vector<>(3);
            row.add(progInfo);
            row.add(progInfo.programID);
            row.add(progInfo.timestamp);
            data.add(row);
        }

        _table.setModel(new DefaultTableModel(data, Col.toHeadings()));

        installCellRenderers();
        if (data.size() > 0) _table.getSelectionModel().setSelectionInterval(0, 0);
        else _table.getSelectionModel().clearSelection();
        TableUtil.initColumnSizes(_table);

        final TableColumnModel tcm = _table.getColumnModel();
        final TableColumn tcId = tcm.getColumn(Col.id.ordinal());
        tcId.setMinWidth(tcId.getPreferredWidth());
        final TableColumn tcMod = tcm.getColumn(Col.modified.ordinal());
        tcMod.setMinWidth(tcMod.getPreferredWidth());
    }

    // install the table cell renderers
    private void installCellRenderers() {
        // display an icon in the first column
        _table.getColumn(Col.name.display).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean cellHasFocus,
                                                           int row,
                                                           int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, cellHasFocus, row, column);
                ((JLabel) c).setIcon(_ICON);
                ((JLabel) c).setText(((DBProgramInfo) value).programName);
                return c;
            }
        });

        // Format the "Modified" date column
        _table.getColumn(Col.modified.display).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean cellHasFocus,
                                                           int row,
                                                           int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, cellHasFocus, row, column);

                long timestamp = (Long) value;
                Date date = new Date(timestamp);
                String timeStr = DATE_FORMAT.format(date) + " " + TIME_FORMAT.format(date);

                ((JLabel) c).setText(timeStr);
                return c;
            }
        });
    }


    /** The listener for selecting an item in the table */
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            if (_table.getSelectedRow() == -1) {
                // No selection, so disable the open button
                _applyButton.setEnabled(false);
                _programName.setText("");
            } else {
                // Show the Selected item(s)
                _applyButton.setEnabled(true);

                ListSelectionModel lsm = _table.getSelectionModel();
                int firstIndex = lsm.getMinSelectionIndex();
                int lastIndex = lsm.getMaxSelectionIndex();
                String names = "";
                String sep = "";
                for (int i = firstIndex; i <= lastIndex; i++) {
                    if (lsm.isSelectedIndex(i)) {
                        DBProgramInfo progInfo = (DBProgramInfo) _table.getValueAt(i, Col.name.ordinal());
                        names += sep + progInfo.programName;
                        sep = ", ";
                    }
                }
                _programName.setText(names);
                _programName.setCaretPosition(0);
            }
        }
    }

    /** Return the object representing the selected programs (List of DBProgramInfo) */
    public List<DBProgramInfo> getSelectedPrograms() {
        return _selectedPrograms;
    }

    // Update the list of selected programs (_selectedPrograms)
    private void _updateSelectedPrograms() {
        _selectedPrograms = new ArrayList<>();
        ListSelectionModel lsm = _table.getSelectionModel();
        int firstIndex = lsm.getMinSelectionIndex();
        if (firstIndex == -1)
            return;
        int lastIndex = lsm.getMaxSelectionIndex();
        for (int i = firstIndex; i <= lastIndex; i++) {
            if (lsm.isSelectedIndex(i)) {
                _selectedPrograms.add((DBProgramInfo)_table.getValueAt(i, Col.name.ordinal()));
            }
        }
    }

    // The listener for selecting an open button
    class ApplyListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            _updateSelectedPrograms();
            dispose();
        }
    }

    // The listener for selecting the cancel button
    class CancelListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            _selectedPrograms = new ArrayList<>();
            dispose();
        }
    }
}
