package jsky.app.ot.gemini.obscat;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Site;
import jsky.app.ot.plot.ObsTargetDesc;
import jsky.app.ot.OTOptions;
import jsky.app.ot.session.SessionQueue;
import jsky.app.ot.session.SessionQueuePanel;
import jsky.app.ot.shared.gemini.obscat.ObsCatalogInfo;
import jsky.app.ot.userprefs.observer.ObservingSite;
import jsky.util.gui.Resources;
import jsky.app.ot.vcs.VcsOtClient;
import jsky.app.ot.viewer.OpenUtils;
import jsky.app.ot.viewer.SPElevationPlotPlugin;
import jsky.app.ot.viewer.ViewerManager;
import jsky.app.ot.viewer.open.OpenDialog;
import jsky.catalog.QueryResult;
import jsky.catalog.TableQueryResult;
import jsky.catalog.gui.QueryResultDisplay;
import jsky.catalog.gui.TableDisplay;
import jsky.catalog.gui.TableDisplayTool;
import jsky.plot.TargetDesc;
import jsky.plot.ElevationPlotManager;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.SortedJTable;
import jsky.util.gui.TabbedPanel;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

/**
 * Displays the results of an ObsCatalog query in a table.
 *
 * @author Allan Brighton
 */
public final class ObsCatalogQueryResultDisplay extends TableDisplayTool implements ListSelectionListener {

    // Icons displayed with the program id to indicate whether or not an observation belongs to a group
    private static final Icon OBS_ICON = Resources.getIcon("noObsGroup.gif");
    private static final Icon GROUP_ICON = Resources.getIcon("obsGroup.gif");

    // Button added to the table display window, used to show the OT
    private final JButton _otButton = new JButton("Show Observation") {{
        setEnabled(false);
        setToolTipText("Show the selected observation in the OT (Observing Tool)");
        addActionListener(e -> _showSelectedObservation());
    }};

    // Button to save the results in text format
    private final JButton _saveAsButton = new JButton("Save As...") {{
        setEnabled(false);
        addActionListener(e -> saveAs());
    }};

    // Button to save the results in text format
    private final JButton _closeButton = new JButton("Close") {{
        addActionListener(e -> SwingUtilities.getWindowAncestor(this).setVisible(false));
    }};

    // Button to add the selected observation to the session queue
    private final JButton _addToSessionQueueButton = new JButton("Add to Queue") {{
        setEnabled(false);
        setToolTipText("Add the selected observation to the session queue");
        addActionListener(e -> {
            try {
                final SPObservationID obsId = (SPObservationID) getSelectedRowValue(ObsCatalogInfo.OBS_ID);
                if (obsId != null)
                    SessionQueue.INSTANCE.addObservation(obsId);
            } catch (Exception ex) {
                DialogUtil.error(ex);
            }
        });
    }};

    // Button to display an elevation plot for selected observations
    private final JButton _elevationPlotButton = new JButton("Elevation Plot") {{
        setEnabled(false);
        setToolTipText("Display an elevation plot for the selected observations");
        addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    final ISPObservation[] obs = _getSelectedObservations();
                    if (obs != null)
                        _elevationPlot(obs);
                } catch (Exception ex) {
                    DialogUtil.error(ex);
                }
            }

            // Return an array of observations corresponding to the selected rows
            private ISPObservation[] _getSelectedObservations() {
                final int n = getSortedJTable().getSelectedRowCount();
                if (n != 0) {
                    final ISPObservation[] obs = new ISPObservation[n];
                    final int[] rows = getSortedJTable().getSelectedRows();
                    final Map<Integer, ISPProgram> progs = loadPrograms(rows);
                    for (int i = 0; i < n; i++) {
                        final ISPProgram prog = progs.get(rows[i]);
                        obs[i] = (prog == null) ? null : _getObservation(prog, rows[i]);
                    }
                    return obs;
                }
                return null;
            }

        });
    }};

    // Button to display the session queue
    private final JButton _displaySessionQueueButton = new JButton("Display Session Queue") {{
        setToolTipText("Display the session queue window");
        addActionListener(e -> SessionQueuePanel.getInstance().showFrame());
    }};

    // Panel inside configPanel used to select columns to display
    private final ObsCatalogTableColumnConfigPanel _tableConfig =
            new ObsCatalogTableColumnConfigPanel(getTableDisplay());

    public ObsCatalogQueryResultDisplay(TableQueryResult tableQueryResult) {
        super(tableQueryResult, null, null);

        // Configure some options on the table
        final SortedJTable jt = getSortedJTable();
        jt.rememberSortColumn(getClass().getName() + ".tableSort", 0, SortedJTable.ASCENDING);
        jt.setBorder(new LineBorder(Color.black));
        jt.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    _showSelectedObservation();
                }
            }
        });

        // Hide unwanted plot buttons
        getPlotButton().setVisible(false);
        getUnplotButton().setVisible(false);
        getUnplotAllButton().setVisible(false);

        // Add some buttons
        final JPanel buttonPanel = getButtonPanel();
        buttonPanel.add(_saveAsButton);
        buttonPanel.add(_otButton);
        buttonPanel.add(_elevationPlotButton);
        if (OTOptions.isStaffGlobally()) {
            buttonPanel.add(_addToSessionQueueButton);
            buttonPanel.add(_displaySessionQueueButton);
        }
        buttonPanel.add(_closeButton);

        // Keep track of the table selection, to enable/disable the button
        jt.getSelectionModel().addListSelectionListener(this);

        // Set the defaults for which columns are visible, if not already set
        final TableDisplay tableDisplay = getTableDisplay();
        final boolean[] show = tableDisplay.getShow();
        final boolean[] defaultShow = _getDefaultShow();
        if (show == null || show.length != defaultShow.length) {
            tableDisplay.setShow(defaultShow);
        }
    }

    // Return an array indicating which table columns in a query result should be displayed by default
    private boolean[] _getDefaultShow() {
        final String[] tableColumns = ObsCatalogInfo.getTableColumns();
        final String[] instTableColumns = ObsCatalogInfo.getInstTableColumns();
        final int n = tableColumns.length;
        final int maxN = n - instTableColumns.length;
        final boolean[] show = new boolean[n];
        for (int i = 0; i < n; i++)
            show[i] = i < maxN;
        return show;
    }

    // Create the table display widget (redefined from parent class to add a cell renderer).
    protected TableDisplay makeTableDisplay(final TableQueryResult queryResult, QueryResultDisplay queryResultDisplay) {
        return new TableDisplay(queryResult, queryResultDisplay) {

            protected void setColumnRenderers() {
                super.setColumnRenderers();

                // Ok this is kind of nuts, sorry.
                final SortedJTable table = getTable();
                setupGroupIconThing(table, getTableQueryResult());
                final TableColumnModel columnModel = table.getColumnModel();
                final TableCellRenderer defaultTcr = new DefaultTableCellRenderer();
                for (int i = 0; i < columnModel.getColumnCount(); i++) {
                    final TableColumn col = columnModel.getColumn(i);
                    final TableCellRenderer tcr = col.getCellRenderer();
                    col.setCellRenderer((t, value, isSelected, hasFocus, row, column) -> {
                        final TableCellRenderer r = (tcr != null) ? tcr : defaultTcr;
                        final Component c = r.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);

                        // Ok, now figure out whether it's local or not
                        final ObsCatalogQueryResult result = (ObsCatalogQueryResult) queryResult;
                        final int resultRow = table.getSortedRowIndex(row);
                        final DB db = result.getDatabase(resultRow);
                        if (db != null) // don't ask
                            c.setForeground(db.isLocal() ? Color.BLACK : Color.GRAY);

                        // Done
                        return c;
                    });
                }

            }

            // Add an icon to the prog ref column indicating whether or not the observation is in a group.
            private void setupGroupIconThing(final SortedJTable sortedJTable, final TableQueryResult tableQueryResult) {
                if (sortedJTable != null) {
                    final TableColumn column;
                    try {
                        column = sortedJTable.getColumn(ObsCatalogInfo.PROG_REF);
                    } catch (Exception e) {
                        return;
                    }

                    column.setCellRenderer(new DefaultTableCellRenderer() {

                        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean cellHasFocus, int row, int column) {
                            final JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, cellHasFocus, row, column);
                            try {
                                final Icon icon = _getRowIcon(sortedJTable.getSortedRowIndex(row), tableQueryResult);
                                label.setIcon(icon);
                            } catch (Exception e) {
                                // ignore: could happen if user adds a row manually
                            }
                            return label;
                        }

                        // Return the icon to use for the given row to indicate if the observation belongs to a group.
                        private Icon _getRowIcon(int row, TableQueryResult tableQueryResult) {
                            final ObsCatalogQueryResult queryResult = (ObsCatalogQueryResult) tableQueryResult;
                            final List idRows = queryResult.getIdRows();
                            final List v = (List) idRows.get(row);
                            final Object o = v.get(ObsCatalogQueryResult.GROUP);
                            return (o != null) ? GROUP_ICON : OBS_ICON;
                        }

                    });
                }
            }

        };
    }

    // Display an elevation plot for the selected observations
    private void _elevationPlot(final ISPObservation[] obs) {
        final SPElevationPlotPlugin plugin = SPElevationPlotPlugin.getInstance();
        plugin.setSelectedObservations(obs);
        final TargetDesc[] targets = _getTargets(obs, plugin.useTargetName());
        if (targets == null) {
            return;
        }

        final Site preferredSite = ObservingSite.getOrNull();

        ElevationPlotManager.show(targets, preferredSite, plugin);

        // Redisplay elevation plot if plugin settings change
        plugin.setChangeListener(e -> {
            try {
                _elevationPlot(obs);
            } catch (Exception ex) {
                DialogUtil.error(ex);
            }
        });
    }

    // Return an array of targets given an array of observations.
    private TargetDesc[] _getTargets(ISPObservation[] obs, boolean useTargetName) {
        final IDBDatabaseService db = SPDB.get();
        final List<TargetDesc> result = new ArrayList<>();
        for (ISPObservation ob : obs) {
            final TargetDesc targetDesc = ObsTargetDesc.getTargetDesc(db, ob, useTargetName);
            if (targetDesc != null) {
                result.add(targetDesc);
            }
        }
        final TargetDesc[] targets = new TargetDesc[result.size()];
        result.toArray(targets);
        return targets;
    }

    // Return the currently selected science program object. If that can't be done, return null.
    private ISPProgram loadSelectedProgram() {
        final int i = getSortedJTable().getSelectedRow();
        return (i < 0) ? null : loadProgram(i);
    }

    private Map<Integer, ISPProgram> loadPrograms(int[] indices) {
        // get the vector of (progId, obsId, groupName) corresponding to the table rows
        final ObsCatalogQueryResult queryResult = (ObsCatalogQueryResult) getTable();
        final Map<SPNodeKey, ISPProgram> localProgs = new TreeMap<>();
        final Map<SPProgramID, ISPProgram> remoteProgs = new TreeMap<>();
        final Map<Integer, ISPProgram> result = new TreeMap<>();

        for (int index : indices) {
            final int rowIndex = getSortedJTable().getSortedRowIndex(index);
            final DB db = queryResult.getDatabase(rowIndex);
            ISPProgram p;
            if (db.isLocal()) {
                // Old code here, simplified
                final List idData = (List) queryResult.getIdRows().get(rowIndex);
                final SPNodeKey key = (SPNodeKey) idData.get(ObsCatalogQueryResult.PROG_ID);
                p = localProgs.get(key);
                if (p == null) {
                    p = OpenUtils.openDBProgram(SPDB.get(), key);
                    localProgs.put(key, p);
                }
            } else {
                final SPProgramID pid = (SPProgramID) queryResult.getValueAt(rowIndex, ObsCatalogInfo.PROG_REF);
                p = remoteProgs.get(pid);
                if (p == null) {
                    final Remote remote = (Remote) db;
                    p = OpenDialog.checkout(SPDB.get(), pid, remote.peer(), this, VcsOtClient.unsafeGetRegistrar());
                    if (p != null) remoteProgs.put(pid, p);
                }
            }
            if (p != null) result.put(index, p);
        }

        // Ok this is poor. We need to update the database in the results, but we need to
        // do it later because it will blow away the selection (which is needed to complete
        // the work some callers are doing).
        SwingUtilities.invokeLater(() -> {
            for (SPProgramID pid : remoteProgs.keySet()) {
                queryResult.updateDatabase(pid, Local$.MODULE$);
            }
            getTableDisplay().update();
        });

        return result;
    }

    private ISPProgram loadProgram(int index) {
        return loadPrograms(new int[] {index}).get(index);
    }

    private int getQueryResultRow() {
        final SortedJTable table = getSortedJTable();
        final int i = table.getSelectedRow();
        return (i >= 0) ? table.getSortedRowIndex(i) : i;
    }

    // Return the currently selected observation, or null
    private ISPObservation _getSelectedObservation(ISPProgram prog) {
        final int i = getSortedJTable().getSelectedRow();
        return (i < 0) ? null : _getObservation(prog, i);
    }

    // Return the observation corresponding the given row.
    private ISPObservation _getObservation(ISPProgram prog, int index) {
        // get the vector of (progId, obsId, groupName) rows corresponding to the table rows
        final ObsCatalogQueryResult queryResult = (ObsCatalogQueryResult) getTable();
        final List idRows = queryResult.getIdRows();

        final int rowIndex = getSortedJTable().getSortedRowIndex(index);
        final int colIndex = ObsCatalogQueryResult.OBS_ID;
        final List v = (List) idRows.get(rowIndex);
        final Object o = v.get(colIndex);
        if (!(o instanceof SPNodeKey)) {
            DialogUtil.error(
                    "No observation id was found for the selected row in the table");
            return null;
        }
        final SPNodeKey obsKey = (SPNodeKey) o;

        for (ISPObservation obs : prog.getAllObservations()) {
            if (obs.getNodeKey().equals(obsKey)) {
                return obs;
            }
        }

        DialogUtil.error("No observation was found with the selected id");
        return null;
    }

    // Display the selected observation in the OT.
    private void _showSelectedObservation() {
        final ISPProgram prog = loadSelectedProgram();
        if (prog != null) {
            final ISPObservation obs = _getSelectedObservation(prog);
            if (obs != null)
                ViewerManager.open(obs);
        }
    }

    // Called when the table selection changes.
    public void valueChanged(ListSelectionEvent e) {
        final int n = getSortedJTable().getSelectedRowCount();
        _otButton.setEnabled(n == 1);
        if (n == 1) {
            final Boolean ready = (Boolean) getSelectedRowValue(ObsCatalogInfo.READY);
            _addToSessionQueueButton.setEnabled(ready != null && ready);
        }
        _elevationPlotButton.setEnabled(n >= 1);
    }

    private Object getSelectedRowValue(String col) {
        final int i = getQueryResultRow();
        if (i >= 0) {
            final TableQueryResult t = getTable();
            return t.getValueAt(i, t.getColumnIndex(col));
        }
        return null;
    }

    // Add a panel to the config window to configure the table columns. Redefined from the parent class
    // to use a window with separate tabs for instrument specific columns.
    protected void addTableColumnConfigPanel() {
        final TabbedPanel configPanel = getConfigPanel();
        final JTabbedPane tabbedPane = configPanel.getTabbedPane();
        tabbedPane.add(_tableConfig, "Show Table Columns");

        final ActionListener applyListener = e -> _tableConfig.apply();
        final ActionListener cancelListener = e -> _tableConfig.cancel();
        configPanel.getApplyButton().addActionListener(applyListener);
        configPanel.getOKButton().addActionListener(applyListener);
        configPanel.getCancelButton().addActionListener(cancelListener);
    }

    // Update the table config panel, if needed
    protected void updateConfigPanel() {
        if (_tableConfig != null) {
            _tableConfig.cancel();
        }
    }

    // Store the current settings in a serializable object and return the object.
    public Object storeSettings() {
        return new Object[]{
                super.storeSettings(),
                SPElevationPlotPlugin.getInstance().storeSettings(),
        };
    }

    // Restore the settings previously stored.
    public boolean restoreSettings(Object obj) {
        if (obj instanceof Object[]) {
            final Object[] ar = (Object[]) obj;
            return ar.length == 2 &&
                    super.restoreSettings(ar[0]) &&
                    SPElevationPlotPlugin.getInstance().restoreSettings(ar[1]);
        }
        return false;
    }

    @Override
    public void setQueryResult(QueryResult queryResult) {
        _saveAsButton.setEnabled(true);
        super.setQueryResult(queryResult);
    }

}
