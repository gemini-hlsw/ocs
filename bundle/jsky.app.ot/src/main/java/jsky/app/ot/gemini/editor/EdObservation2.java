// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: EdObservation2.java 47001 2012-07-26 19:40:02Z swalker $
//
package jsky.app.ot.gemini.editor;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.gui.text.AbstractDocumentListener;
import edu.gemini.skycalc.ObservingNight;
import edu.gemini.shared.util.TimeValue;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.dataset.DataflowStatus;
import edu.gemini.spModel.dataset.DatasetQaState;
import edu.gemini.spModel.dataset.DatasetQaStateSums;
import edu.gemini.spModel.obs.*;
import edu.gemini.spModel.obs.ObservationStatus;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummary;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummaryService;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obsrecord.ObsExecStatus;
import edu.gemini.spModel.template.OriginatingTemplateFunctor;
import edu.gemini.spModel.time.*;
import edu.gemini.spModel.too.Too;
import edu.gemini.spModel.too.TooType;
import jsky.app.ot.OT;
import jsky.app.ot.OTOptions;
import jsky.app.ot.editor.OtItemEditor;
import jsky.app.ot.editor.template.ReapplicationDialog;
import jsky.app.ot.editor.template.ReapplicationHelpers;
import jsky.app.ot.viewer.SPViewer;
import jsky.util.gui.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This is the editor for the Observation item.
 */
public final class EdObservation2 extends OtItemEditor<ISPObservation, SPObservation>
        implements jsky.util.gui.TextBoxWidgetWatcher, ActionListener {

    private static final Logger LOGGER = Logger.getLogger(Logger.class.getName());

    // Time summary table heading
    private static final Vector<String> _timeSummaryTableHead = new Vector<String>(5);

    static {
        _timeSummaryTableHead.add(" ");
        _timeSummaryTableHead.add("Program");
        _timeSummaryTableHead.add("Partner");
        _timeSummaryTableHead.add("Non-charged");
        _timeSummaryTableHead.add("Elapsed");
    }

    // Correction table heading
    private static final Vector<String> _correctionTableHead = new Vector<String>(4);

    static {
        _correctionTableHead.add("Timestamp");
        _correctionTableHead.add("Correction");
        _correctionTableHead.add("Charge Class");
        _correctionTableHead.add("Comment");
    }

    // table column indexe (keep in sync with _tableHead below)
    private static final int COMMENT_COLUMN = 3;
    private static final String TEMPLATE_NO_LONGER_EXISTS = "(Template no longer exists.)";

    // Display multiple lines for the dataset comments
    private final JTextAreaCellRenderer _commentRenderer = new JTextAreaCellRenderer();

    // our gui panel
    private final ObsForm _editorPanel = new ObsForm();

    // Set to true while updating from the data object
    private boolean _ignoreEvents;

    private static final String CARD_STANDARD_KEY = "LABEL_ONLY";
    private static final String CARD_RAPID_TOO_KEY = "RADIO_BUTTON";
    private static final String CARD_NO_TOO_KEY = "EMPTY_PANEL";

    private static final class ExecStatusComboBoxModel extends DefaultComboBoxModel {
        ExecStatusComboBoxModel() {
            removeAllElements();
            addElement(None.<ObsExecStatus>instance());
            int i = 1;
            for (ObsExecStatus s : ObsExecStatus.values()) {
                insertElementAt(new Some<ObsExecStatus>(s), i++);
            }
        }
    }

    private static final class ExecStatusComboBoxRenderer extends BasicComboBoxRenderer implements ListCellRenderer {
        private Option<ObsExecStatus> autoStatus = None.instance();

        void setAutoStatus(ObsExecStatus status) {
            if (status == null) {
                autoStatus = None.instance();
            } else {
                autoStatus = new Some<ObsExecStatus>(status);
            }
        }

        @SuppressWarnings("unchecked")
        public Component getListCellRendererComponent(JList jList, Object value, int index, boolean isSelected, boolean hasFocus) {
            final String text;
            if (value instanceof Some) {
                text = ((Some<ObsExecStatus>) value).getValue().displayValue();
            } else {
                text = autoStatus.isEmpty() ? "Pending Phase 2 Completion" : "Automatically Set (" + autoStatus.getValue().displayValue() + ")";
            }

            return super.getListCellRendererComponent(jList, text, index, isSelected, hasFocus);
        }
    }

    private final class ExecOverrideEditor {
        private final JComboBox combo;
        private final ExecStatusComboBoxRenderer renderer = new ExecStatusComboBoxRenderer();

        ExecOverrideEditor(JPanel pan) {
            pan.setLayout(new GridBagLayout());
            pan.add(new JLabel("Exec Status"), new GridBagConstraints() {{
                gridx=0; insets=new Insets(0,10,0,5);
            }});

            combo = new JComboBox() {{
                setModel(new ExecStatusComboBoxModel());
                addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final SPObservation obs = getDataObject();
                        @SuppressWarnings("unchecked") final Option<ObsExecStatus> s = (Option<ObsExecStatus>) combo.getSelectedItem();
                        if (obs != null) {
                            obs.setExecStatusOverride(s);
                            ExecOverrideEditor.this.update();
                        }
                    }
                });
            }};
            combo.setRenderer(renderer);

            // Tried hard to make the combo box resize based upon the options
            // in the update() method.  Failed.  Even setPrototypeDisplayValue
            // does nothing.  This seems to be a decent size.
            final Dimension p = combo.getPreferredSize();
            final Dimension d = new Dimension(195, p.height);
            combo.setMinimumSize(d);
            combo.setPreferredSize(d);

            pan.add(combo, new GridBagConstraints() {{
                gridx=1; insets=new Insets(0,0,0,0);
            }});
        }

        void update() {
            final boolean isStaff = OTOptions.isStaff(getProgram().getProgramID());
            final SPObservation obs = getDataObject();
            combo.setSelectedItem(obs.getExecStatusOverride());
            if (isStaff && obs.getPhase2Status() == ObsPhase2Status.PHASE_2_COMPLETE) {
                combo.setEnabled(true);
            } else {
                combo.setEnabled(false);
            }
            if (obs.getPhase2Status() == ObsPhase2Status.PHASE_2_COMPLETE) {
                renderer.setAutoStatus(ObservationStatus.execStatusFor(getNode()));
            } else {
                renderer.setAutoStatus(null);
            }
            combo.repaint();
        }
    }

    private ExecOverrideEditor _execOverrideEditor = new ExecOverrideEditor(_editorPanel.execStatusPanel);

    /**
     * The constructor initializes the user interface.
     */
    public EdObservation2() {

        _editorPanel.obsTitle.addWatcher(this);

        _editorPanel.libraryIdTextField.getDocument().addDocumentListener(new AbstractDocumentListener() {
            @Override
            public void textChanged(DocumentEvent docEvent, String newText) {
                final SPObservation obs = getDataObject();
                if (obs != null) obs.setLibraryId(newText.trim());
            }
        });


        _editorPanel.reapplyButton.setAction(_reapplyAction);

        ButtonGroup grp = new ButtonGroup();
        grp.add(_editorPanel.priorityHigh);
        grp.add(_editorPanel.priorityMedium);
        grp.add(_editorPanel.priorityLow);

        grp = new ButtonGroup();
        grp.add(_editorPanel.rapidTooButton);
        grp.add(_editorPanel.standardTooButton);
        grp.add(_editorPanel.noTooButton); // invisible button to make button group work

        //Add the cards to the ToOCardPanel, so we can select the appropriate one
        //based on the program type
        _editorPanel.tooCardPanel.add(_editorPanel.tooLabelOnlyPanel, CARD_STANDARD_KEY);
        _editorPanel.tooCardPanel.add(_editorPanel.tooRadioButtonPanel, CARD_RAPID_TOO_KEY);
        _editorPanel.tooCardPanel.add(new JPanel(), CARD_NO_TOO_KEY);

        _editorPanel.phase2StatusBox.setModel(new DefaultComboBoxModel(ObsPhase2Status.values()));
        _editorPanel.phase2StatusBox.setMaximumRowCount(ObsPhase2Status.values().length);
        _editorPanel.phase2StatusBox.addActionListener(this);

        _editorPanel.qaStateBox.setChoices(ObsQaState.values());
        _editorPanel.qaStateBox.addActionListener(this);

        _editorPanel.override.addActionListener(this);

        _editorPanel.dataflowStep.setText("No Data");

        _editorPanel.priorityHigh.addActionListener(this);
        _editorPanel.priorityMedium.addActionListener(this);
        _editorPanel.priorityLow.addActionListener(this);

        _editorPanel.rapidTooButton.addActionListener(this);
        _editorPanel.standardTooButton.addActionListener(this);

        // keep this time correction label up to date
        _editorPanel.timeCorrectionOp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (_editorPanel.timeCorrectionOp.getValue().equals("Subtract")) {
                    _editorPanel.correctionToFromLabel.setText("from");
                } else {
                    _editorPanel.correctionToFromLabel.setText("to");
                }
            }
        });

        _editorPanel.timeCorrectionUnits.setChoices(new String[]{
                // don't include nights
                TimeValue.Units.hours.name(),
                TimeValue.Units.minutes.name(),
                TimeValue.Units.seconds.name()
        });
        _editorPanel.timeCorrectionUnits.setSelectedItem(TimeValue.Units.minutes.name());

        _editorPanel.addCorrectionButton.addActionListener(this);

        _editorPanel.chargeClass.setChoices(new String[]{
                // don´t display Gemini as an option
                ChargeClass.PARTNER.displayValue(),
                ChargeClass.PROGRAM.displayValue()
        });

        _editorPanel.chargeClass.setSelectedItem(ChargeClass.DEFAULT.displayValue());

        // override green theme for value labels
        _editorPanel.execTime.setForeground(Color.black);
        _editorPanel.piTime.setForeground(Color.black);
        _editorPanel.obsClass.setForeground(Color.black);
        _editorPanel.obsId.setForeground(Color.black);
        _editorPanel.tooSinglePriorityLabel.setForeground(Color.black);
        _editorPanel.dataflowStep.setForeground(Color.black);

        if (!OTOptions.isStaffGlobally()) {
            // The planned time layout is different when off site
            _editorPanel.piLabel.setText("Planned");
            _editorPanel.plannedLabel.setVisible(false);
            _editorPanel.execLabel.setVisible(false);
            _editorPanel.execTime.setVisible(false);
        }

    }


    /**
     * Return the window containing the editor
     */
    public JPanel getWindow() {
        return _editorPanel;
    }

    private final PropertyChangeListener _statusListener = new PropertyChangeListener() {
        @Override public void propertyChange(PropertyChangeEvent evt) {
            if ((evt.getSource() instanceof ISPObsExecLog) && getDataObject().getExecStatusOverride().isEmpty()) {
                _execOverrideEditor.update();
            }
        }
    };

    /**
     * Set the data object corresponding to this editor.
     */
    @Override public void init() {
        getNode().addCompositeChangeListener(_statusListener);

        //update the ToO UI based on the ToO status of the program.
        _updateTooUI();
        _editorPanel.libraryIdLabel.setVisible(isLibraryProgram());
        _editorPanel.libraryIdTextField.setVisible(isLibraryProgram());

        final String libraryId = getDataObject().getLibraryId();
        _editorPanel.libraryIdTextField.setText(libraryId);
        _showTitle();
        _showObsId();
        _showPriority();
        _showStatus();
        _showOriginatingTemplate();

        _updateObsTimeCorrectionTable();
        _updateTotalUsedTime(false);

        // GUI editable state depends on the observation status
        OT.updateEditableState(getNode());

        // The total planed time is updated whenever the sequence or instrument
        // is changed. If the user didn't press Apply before selecting the observation
        // node, we may need to give the events a chance to be handled, to make
        // sure this value is updated before it is displayed.
        final ISPObservation obs = getNode();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // First make sure we're still editing the same node in the
                // tree.  The user can click on another node which could
                // insert an event before this Runnable is executed.
                ISPNode node = null;
                try {
                    node = getViewer().getTree().getSelectedNode();
                } catch (NullPointerException ex) {
                }
                if (obs == node) { // we're still looking at the same node
                    _updateTotalPlannedTime();
                    _updateObsClass();

                    // Sadly this has to be dona later, which can cause a flicker. It's probably
                    // possible to do this hack more effectively but I can't figure out how.
                    _updateOuterTitleStuff();
                }
            }
        });

    }

    @Override public void cleanup() {
        getNode().removeCompositeChangeListener(_statusListener);
    }

    private final Action _reapplyAction = new AbstractAction("Reapply...") {
        public void actionPerformed(ActionEvent evt) {
            try {
                ReapplicationDialog.open(_editorPanel, getProgram(), getNode());
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    private void _showOriginatingTemplate() {

        try {

            // Get our originating template (if any)
            final IDBDatabaseService database = SPDB.get();
            OriginatingTemplateFunctor otf = new OriginatingTemplateFunctor(ReapplicationHelpers.currentUserRolePrivileges());
            otf = database.getQueryRunner(OT.getUser()).execute(otf, getNode());

            _reapplyAction.setEnabled(otf.canReapply());

            if (otf.isTemplateDerived()) {

                final SPObservation todo = otf.getTemplateObservationDataObject();
                final String text;
                if (todo == null) {
                    text = TEMPLATE_NO_LONGER_EXISTS;
                } else {
                    final ISPObservation to = otf.getTemplateObservation();
                    text = String.format("[%d] %s", to.getObservationNumber(), todo.getTitle());
                }

                _editorPanel.originatingTemplate.setText(text);

                _editorPanel.originatingTemplateLabel.setVisible(true);
                _editorPanel.originatingTemplate.setVisible(true);
                _editorPanel.reapplyButton.setVisible(true);


            } else {

                _editorPanel.originatingTemplateLabel.setVisible(false);
                _editorPanel.originatingTemplate.setVisible(false);
                _editorPanel.reapplyButton.setVisible(false);

            }

        } catch (SPNodeNotLocalException e) {
            DialogUtil.error(e);
        }

    }

    private void _updateOuterTitleStuff() {
        try {
            final SPViewer vg = getViewer();
            final TitledBorder b = vg.getTitledBorder();
            if (isInsideTemplate()) {
                b.setTitle("Template Observation");
                vg.paintImmediately(vg.getBounds());
            } else {
                b.setTitle("Observation");
                vg.paintImmediately(vg.getBounds());
            }
        } catch (Exception e) {
            // We don't need to email this exception.  I believe this can
            // happen when the editor is switched by the user clicking on
            // another node before the background task that calls this method
            // runs.  In that case, the editor panel no longer has a parent.
            LOGGER.log(Level.INFO, "Swing component structure changed, so outer title update doesn't work.", e);
        }
    }


    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    // update the total used time display
    private void _updateTotalUsedTime(boolean localCorrections) {
        final ISPObservation obs = getNode();
        final Vector<Vector> tableRows = new Vector<Vector>();
        final DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd");
        dateFmt.setTimeZone(UTC);

        // Add a row for each item
        final Collection<NightObsTimes> c = NightObsTimesService.getObservingNightTimes(obs);
        for (NightObsTimes t : c) {
            final ObservingNight night = t.getNight();
            final ObsTimes times = t.getTimes();
            final ObsTimeCharges charges = times.getTimeCharges();

            // A row contains: date, programTime, partnerTime, nonCharged, elapsed
            final Vector<String> row = new Vector<String>(5);
            // Want the UTC date at the end of the night, but note that
            // ObservingNight.getEndTime() returns 1 ms after the end of
            // the night.  It is just outside of the night boundary.
            row.add(dateFmt.format(new Date(night.getEndTime() - 1)));
            row.add(TimeAmountFormatter.getHMSFormat(charges.getTime(ChargeClass.PROGRAM)));
            row.add(TimeAmountFormatter.getHMSFormat(charges.getTime(ChargeClass.PARTNER)));
            row.add(TimeAmountFormatter.getHMSFormat(charges.getTime(ChargeClass.NONCHARGED)));
            row.add(TimeAmountFormatter.getHMSFormat(times.getTotalTime()));
            tableRows.add(row);
        }

        // Add a row for the total corrections, if present
        final long programCorrections = getDataObject().getTotalObsTimeCorrection(ChargeClass.PROGRAM);
        final long partnerCorrections = getDataObject().getTotalObsTimeCorrection(ChargeClass.PARTNER);
        final long nonChargedCorrections = getDataObject().getTotalObsTimeCorrection(ChargeClass.NONCHARGED);
        if (programCorrections != 0 || partnerCorrections != 0 || nonChargedCorrections != 0) {
            final Vector<String> row = new Vector<String>(5);
            row.add("Corrections");
            row.add(TimeAmountFormatter.getHMSFormat(programCorrections));
            row.add(TimeAmountFormatter.getHMSFormat(partnerCorrections));
            row.add(TimeAmountFormatter.getHMSFormat(nonChargedCorrections));
            row.add(null); // no elapsed column here
            tableRows.add(row);
        }

        // Add a row for the totals
        final ObsTimes obsTimes = _getObsTimes(localCorrections);
        final ObsTimeCharges otc = obsTimes.getTimeCharges();
        final Vector<String> row = new Vector<String>(5);
        row.add("Total");
        row.add(TimeAmountFormatter.getHMSFormat(otc.getTime(ChargeClass.PROGRAM)));
        row.add(TimeAmountFormatter.getHMSFormat(otc.getTime(ChargeClass.PARTNER)));
        row.add(TimeAmountFormatter.getHMSFormat(otc.getTime(ChargeClass.NONCHARGED)));
        row.add(TimeAmountFormatter.getHMSFormat(obsTimes.getTotalTime()));
        tableRows.add(row);

        // Display the rows in the table
        final DefaultTableModel model = new DefaultTableModel(tableRows, _timeSummaryTableHead) {
            // disable cell editing
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            // right justify (it doesn't matter that the values are not Integer...)
            public Class getColumnClass(int col) {
                if (col > 0) return Integer.class;
                return String.class;
            }
        };
        _editorPanel.timeSummaryTable.setModel(model);

        TableUtil.initColumnSizes(_editorPanel.timeSummaryTable, null);

        // scroll to end of table
        final int numRows = tableRows.size();
        final Rectangle r = _editorPanel.timeSummaryTable.getCellRect(numRows - 1, 0, false);
        _editorPanel.timeSummaryTable.scrollRectToVisible(r);
    }

    // Return the ObsTimes instance to use, based on the argument
    private ObsTimes _getObsTimes(boolean localCorrections) {
        final ISPObservation obs = getNode();
        ObsTimes obsTimes;
        if (!localCorrections) {
            obsTimes = ObsTimesService.getCorrectedObsTimes(obs);
        } else {
            // Get raw obs times.
            obsTimes = ObsTimesService.getRawObsTimes(obs);

            // Apply local corrections.
            final ObsTimeCharges corrections = getDataObject().sumObsTimeCorrections();
            final ObsTimeCharges raw = obsTimes.getTimeCharges();

            final long totalTime = obsTimes.getTotalTime();
            obsTimes = new ObsTimes(totalTime, raw.addTimeCharges(corrections));
        }
        return obsTimes;
    }

    // update the total planned time display
    private void _updateTotalPlannedTime() {
        String piTime = "00:00:00";
        String execTime = "00:00:00";

        // This method is called via "SwingUtilities.invokeLater()".  If called
        // while the selected node is rapidly changing, the obs node can be
        // null.
        final ISPObservation obs = getNode();
        if (obs != null) {
            try {
                final PlannedTimeSummary pt = PlannedTimeSummaryService.getTotalTime(obs);
                piTime = TimeAmountFormatter.getHMSFormat(pt.getPiTime());
                execTime = TimeAmountFormatter.getHMSFormat(pt.getExecTime());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Exception at _updateTotalPlannedTime", e);
            }
        }
        _editorPanel.piTime.setText(piTime);
        _editorPanel.execTime.setText(execTime);
    }

    // update the observation class
    private void _updateObsClass() {
        try {
            final ObsClass obsClass = ObsClassService.lookupObsClass(getNode());
            if (obsClass == null) {
                _editorPanel.obsClass.setText("");
            } else {
                _editorPanel.obsClass.setText(obsClass.displayValue());
            }
        } catch (Exception e) {
            DialogUtil.error(e);
            _editorPanel.obsClass.setText("");
        }
    }

    // Show the observation status
    private void _showStatus() {

        // First collect the information to display.
        final DataflowStatus dispo = DatasetDispositionService.lookupDatasetDisposition(getNode());
        final DatasetQaStateSums sums = DatasetQaStateSumsService.sumDatasetQaStates(getNode());

        final ObsQaState obsQaState;
        if (getDataObject().isOverrideQaState()) {
            obsQaState = getDataObject().getOverriddenObsQaState();
        } else {
            obsQaState = ObsQaState.computeDefault(sums);
        }
        final boolean override = getDataObject().isOverrideQaState();

        // Format the dataset disposition ("dataflow step").
        final String statusString = (dispo == null) ? "No Data" : dispo.getDisplayString();

        // Format the summary of dataset information.
        String sumsStr = "(No Data)";
        final int total = sums.getTotalDatasets();
        if (total > 0) {
            final StringBuilder buf = new StringBuilder("(");
            buf.append(total).append(" Dataset");
            if (total > 1) buf.append("s");

            char sep = ':';
            for (DatasetQaState state : DatasetQaState.values()) {
                final int count = sums.getCount(state);
                if (count == 0) continue;
                buf.append(sep).append(" ");
                buf.append(count).append(" ").append(state.displayValue());
                sep = ',';
            }
            buf.append(")");
            sumsStr = buf.toString();
        }

        // Display updates.
        _ignoreEvents = true;
        try {
            _editorPanel.phase2StatusBox.setSelectedItem(getDataObject().getPhase2Status());
            _execOverrideEditor.update();
            _editorPanel.qaStateBox.setSelectedItem(obsQaState);
            _editorPanel.override.setSelected(override);
        } finally {
            _ignoreEvents = false;
        }
        _editorPanel.qaStateSum.setText(sumsStr);
        _editorPanel.dataflowStep.setText(statusString);

    }


    /**
     * Select the right UI to use based on the Program TOO status.
     * - Rapid TOO programs: we would use two radio buttons, one for "Rapid" one for "Standard"
     * - Standard TOO programs: we would show the label "TOO Priority: Standard" and no radio buttons since there is
     * no other option for the user
     * - Non-TOO programs: show nothing since this isn't a TOO program anyway
     * <p/>
     * See SCT-211 for further details
     */
    private void _updateTooUI() {
        final TooType tooType = Too.get(getProgram());
        final CardLayout lm = (CardLayout) _editorPanel.tooCardPanel.getLayout();

        switch (tooType) {
            case none:
                lm.show(_editorPanel.tooCardPanel, CARD_NO_TOO_KEY);
                break;
            case rapid:
                lm.show(_editorPanel.tooCardPanel, CARD_RAPID_TOO_KEY);
                break;
            case standard:
                lm.show(_editorPanel.tooCardPanel, CARD_STANDARD_KEY);
                break;
        }

    }

    // Show the priority
    private void _showPriority() {
        final SPObservation.Priority pri = getDataObject().getPriority();
        if (pri == SPObservation.Priority.HIGH) {
            _editorPanel.priorityHigh.setSelected(true);
        } else if (pri == SPObservation.Priority.MEDIUM) {
            _editorPanel.priorityMedium.setSelected(true);
        } else {
            _editorPanel.priorityLow.setSelected(true);
        }

        switch (Too.get(getNode())) {
            case none:
                _editorPanel.noTooButton.setSelected(true);
                break;
            case rapid:
                _editorPanel.rapidTooButton.setSelected(true);
                break;
            case standard:
                _editorPanel.standardTooButton.setSelected(true);
                break;
        }
    }

    // Show the obs id
    private void _showObsId() {
        final SPObservationID spObsID = getNode().getObservationID();
        final String id = (spObsID == null) ? "" : spObsID.stringValue();
        _editorPanel.obsId.setText(id);
    }

    // Show the title
    private void _showTitle() {
        final String title = getDataObject().getTitle();
        if (title != null) {
            _editorPanel.obsTitle.setText(title);
        } else {
            _editorPanel.obsTitle.setText("");
        }
    }


    // Update the time correction table from the data object
    private void _updateObsTimeCorrectionTable() {
        final ObsTimeCorrection[] items = getDataObject().getObsTimeCorrections();
        final Vector<Vector<String>> tableRows = new Vector<Vector<String>>(items.length);

        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(UTC);

        for (ObsTimeCorrection item : items) {
            final Vector<String> row = new Vector<String>(4);
            row.add(dateFormat.format(new Date(item.getTimestamp())));
            final long timeAmount = item.getCorrection().getMilliseconds();
            row.add(TimeAmountFormatter.getDescriptiveFormat(timeAmount));
            row.add(item.getChargeClass().displayValue());
            row.add(item.getReason());
            tableRows.add(row);
        }

        final DefaultTableModel model = new DefaultTableModel(tableRows, _correctionTableHead) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        _editorPanel.correctionTable.setModel(model);
        TableUtil.initColumnSizes(_editorPanel.correctionTable, null);

        final TableColumn c = _editorPanel.correctionTable.getColumnModel().getColumn(COMMENT_COLUMN);
        c.setCellRenderer(_commentRenderer);

        // since these are not in the data object, just clear them
        _editorPanel.timeCorrection.setText("");
        _editorPanel.comment.setText("");
    }

    /**
     * Update the enabled (editable) state of this editor.
     * The default implementation just enables or disables all components in
     * the editor window. Here we update the status items, which may remain enabled.
     */
    protected void updateEnabledState(boolean enabled) {
        super.updateEnabledState(enabled);

        // Make sure some items are still editable regardless for staff.
        final boolean b = OTOptions.isStaff(getProgram().getProgramID()) && !isInsideTemplate();
        _editorPanel.correctionTable.setEnabled(b);
        _editorPanel.timeCorrectionOp.setEnabled(b);
        _editorPanel.timeCorrection.setEnabled(b);
        _editorPanel.timeCorrectionUnits.setEnabled(b);
        _editorPanel.comment.setEnabled(b);
        _editorPanel.addCorrectionButton.setEnabled(b);
        _editorPanel.chargeClass.setEnabled(b);
        _updateStatusEnabledState();
    }

    /**
     * Update the enabled states of the widgets based on the program status
     */
    private void _updateStatusEnabledState() {
        final boolean isStaff = OTOptions.isStaff(getProgram().getProgramID());
        final boolean isNGO   = OTOptions.isNGO(getProgram().getProgramID());
        final boolean isPI    = OTOptions.isPI(getProgram().getProgramID());
        final boolean enabled = (isPI || isStaff || isNGO) && !isInsideTemplate();

        _editorPanel.phase2StatusBox.setEnabled(enabled);
        _execOverrideEditor.update();
        _editorPanel.qaStateBox.setEnabled(enabled && isStaff);
        _editorPanel.override.setEnabled(enabled && isStaff);

        // Set the enabled state of the status menu items
        if (!enabled) return;

        // Staff can change anything. Init to disabled for non-staff.
        final int size = ObservationStatus.values().length;
        for (int i=0; i<size; i++) _editorPanel.phase2StatusBox.setEnabledIndex(i, isStaff);
        if (isStaff) return; // done

        final ObsPhase2Status current = getDataObject().getPhase2Status();

        // An NGO can not downgrade if higher than FOR_ACTIVATION
        if (isNGO) {
            if (!(current.ordinal() > ObsPhase2Status.GEMINI_TO_ACTIVATE.ordinal()) ||
                    ((current == ObsPhase2Status.ON_HOLD) && !Too.isToo(getNode()))) {
                // SCT-230 dictates that NGOs can change from on hold to
                // something below, if not a TOO observation.
                _editorPanel.phase2StatusBox.setEnabledObject(ObsPhase2Status.PI_TO_COMPLETE, true);
                _editorPanel.phase2StatusBox.setEnabledObject(ObsPhase2Status.NGO_TO_REVIEW, true);
                _editorPanel.phase2StatusBox.setEnabledObject(ObsPhase2Status.NGO_IN_REVIEW, true);
                _editorPanel.phase2StatusBox.setEnabledObject(ObsPhase2Status.GEMINI_TO_ACTIVATE, true);
                if (current == ObsPhase2Status.ON_HOLD) {
                    // then to have gotten here the priority isn't TOO.  Enable
                    // ON_HOLD in this case even though the status is already
                    // set to ON_HOLD so that the combo box shows up enabled.
                    // See Note near the end of SCT-230.
                    _editorPanel.phase2StatusBox.setEnabledObject(ObsPhase2Status.ON_HOLD, true);
                }
            }
        } else {
            // must be a PI

            // Don't allow changing priorities when status is not phase1 (not even when on-hold).
            // This is to avoid getting to an un-editable state.
            final boolean b = (current == ObsPhase2Status.PI_TO_COMPLETE);
            _editorPanel.priorityHigh.setEnabled(b);
            _editorPanel.priorityMedium.setEnabled(b);
            _editorPanel.priorityLow.setEnabled(b);

            //  When a TOO observation (has priority set) is set to On Hold, the
            // PI can set it directly to Ready. Otherwise the PI can only set to
            // Phase2 or For Review.
            if (Too.isToo(getNode()) &&
                    ((current == ObsPhase2Status.ON_HOLD) ||
                     (ObservationStatus.computeFor(getNode()) == ObservationStatus.READY))) {
                _editorPanel.phase2StatusBox.setEnabledObject(ObsPhase2Status.ON_HOLD, true);
                _editorPanel.phase2StatusBox.setEnabledObject(ObsPhase2Status.PHASE_2_COMPLETE, true);
            } else {
                if (current.ordinal() > ObsPhase2Status.NGO_TO_REVIEW.ordinal()) {
                    _editorPanel.phase2StatusBox.setEnabled(false);
                } else {
                    _editorPanel.phase2StatusBox.setEnabledObject(ObsPhase2Status.PI_TO_COMPLETE, true);
                    _editorPanel.phase2StatusBox.setEnabledObject(ObsPhase2Status.NGO_TO_REVIEW, true);
                }
            }
        }
    }

    /**
     * Watch changes to the title text box.
     *
     * @see TextBoxWidgetWatcher
     */
    public void textBoxKeyPress(TextBoxWidget tbwe) {
        if (tbwe == _editorPanel.obsTitle) {
            getDataObject().setTitle(_editorPanel.obsTitle.getText().trim());
        }
    }

    /**
     * Text box action, ignore.
     *
     * @see jsky.util.gui.TextBoxWidgetWatcher
     */
    public void textBoxAction(TextBoxWidget tbwe) {
    }


    /**
     * Handle standard action events.
     */
    public void actionPerformed(ActionEvent evt) {
        if (_ignoreEvents) {
            return;
        }

        final Object w = evt.getSource();
        if (w == _editorPanel.addCorrectionButton) {
            _addTimeCorrection();
            return;
        }

        if (w == _editorPanel.override) {
            final ObsQaState oldState = (ObsQaState) _editorPanel.qaStateBox.getSelectedItem();
            getDataObject().setOverriddenObsQaState(oldState);
            getDataObject().setOverrideQaState(_editorPanel.override.isSelected());
            _showStatus();
            return;
        }

        if ((w instanceof AbstractButton) && !((AbstractButton) w).isSelected()) {
            return;
        }

        if (w == _editorPanel.priorityHigh) {
            getDataObject().setPriority(SPObservation.Priority.HIGH);
            return;
        }
        if (w == _editorPanel.priorityMedium) {
            getDataObject().setPriority(SPObservation.Priority.MEDIUM);
            return;
        }
        if (w == _editorPanel.priorityLow) {
            getDataObject().setPriority(SPObservation.Priority.LOW);
            return;
        }
        if (w == _editorPanel.rapidTooButton) {
            getDataObject().setOverrideRapidToo(false);
            return;
        }
        if (w == _editorPanel.standardTooButton) {
            getDataObject().setOverrideRapidToo(true);
            return;
        }
        if (w == _editorPanel.phase2StatusBox) {
            _setPhase2Status();
            _execOverrideEditor.update();
            return;
        }
        if (w == _editorPanel.qaStateBox) {
            final ObsQaState newState = (ObsQaState) _editorPanel.qaStateBox.getSelectedItem();
            getDataObject().setOverrideQaState(true);
            getDataObject().setOverriddenObsQaState(newState);
            _showStatus();
        }
    }

    // Add the time correction value entered by the user to the list
    private void _addTimeCorrection() {
        final String s = _editorPanel.timeCorrection.getText();
        double d;
        try {
            d = Double.parseDouble(s);
        } catch (Exception e) {
            DialogUtil.error("Please enter a numeric value");
            return;
        }

        // check if we are adding or subtracting the amount
        if (_editorPanel.timeCorrectionOp.getValue().equals("Subtract")) {
            d = -d;
        }

        final String unitsStr = _editorPanel.timeCorrectionUnits.getSelected();
        TimeValue.Units units = TimeValue.Units.hours;
        try {
            units = TimeValue.Units.valueOf(unitsStr);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "problem parsing time units: " + unitsStr);
        }

        final TimeValue correction = new TimeValue(d, units);
        final long timestamp = new Date().getTime();

        final String reason = _editorPanel.comment.getText();
        if (reason.length() == 0) {
            DialogUtil.error("Please enter a comment!");
            return;
        }

        final int i = _editorPanel.chargeClass.getSelectedIndex();
        if (i < 0) {
            DialogUtil.error("Please select a charge class for the time correction.");
            return;
        }
        final ChargeClass chargeClass = ChargeClass.values()[i + 1]; // ignore first item: Gemini

        getDataObject().addObsTimeCorrection(new ObsTimeCorrection(
                correction, timestamp, chargeClass, reason
        ));

        _updateObsTimeCorrectionTable();
        _updateTotalUsedTime(true);
    }

    private void _setPhase2Status() {
        final ObsPhase2Status status = (ObsPhase2Status) _editorPanel.phase2StatusBox.getSelectedItem();

        // Match bizarre old behavior of editing multiple observation status.
        getViewer().setPhase2Status(status);
        OT.updateEditableState(getNode());
    }

    private boolean isInsideTemplate() {
        return isInsideTemplate(getNode());
    }

    private static boolean isInsideTemplate(ISPNode node) {
        return (node != null) && ((node instanceof ISPTemplateFolder) || isInsideTemplate(node.getParent()));
    }
}

