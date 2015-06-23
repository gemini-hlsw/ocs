// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: EdProgram.java 47001 2012-07-26 19:40:02Z swalker $
//
package jsky.app.ot.gemini.editor;

import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.spModel.core.Affiliate;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.shared.util.TimeValue;
import edu.gemini.spModel.data.YesNoType;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.obs.ObsTimesService;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummary;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummaryService;
import edu.gemini.spModel.time.ChargeClass;
import edu.gemini.spModel.time.ObsTimeCharges;
import edu.gemini.spModel.time.ObsTimes;
import edu.gemini.spModel.time.TimeAmountFormatter;
import edu.gemini.spModel.too.TooType;
import jsky.app.ot.OTOptions;
import jsky.app.ot.StaffBean;
import jsky.app.ot.editor.OtItemEditor;
import jsky.app.ot.gemini.editor.auxfile.AuxFileEditor;
import jsky.app.ot.vcs.VcsOtClient;
import jsky.coords.HMS;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.TextBoxWidget;
import jsky.util.gui.TextBoxWidgetWatcher;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the editor for the Science Program component.
 */
public final class EdProgram extends OtItemEditor<ISPProgram, SPProgram> implements jsky.util.gui.TextBoxWidgetWatcher, ItemListener {

    private static final Logger LOGGER = Logger.getLogger(EdProgram.class.getName());
    // table column indexes (keep in sync with _tableHead below)

    // the GUI layout panel
    final ProgramForm _w;
    private final AuxFileEditor _edAux;

    /**
     * The constructor initializes the user interface.
     */
    public EdProgram() {
        _w = new ProgramForm();
        _edAux = new AuxFileEditor(_w);

        _w.contactBox.setForeground(Color.black);

        _w.titleBox.addWatcher(this);
        _w.firstNameBox.addWatcher(this);
        _w.lastNameBox.addWatcher(this);
        _w.emailBox.addWatcher(this);
        _w.phoneBox.addWatcher(this);
        _w.ngoContactBox.addWatcher(this);

        // add menu choices
        _w.affiliationBox.setForeground(Color.black);

        _w.historyTable.setBackground(_w.getBackground());
        _w.historyTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        _w.activeCheckBox.addItemListener(this);
        _w.completedCheckBox.addItemListener(this);
        adjustStaffOnlyFields(OTOptions.isStaffGlobally());
        StaffBean.addPropertyChangeListener(new PropertyChangeListener() {
            @Override public void propertyChange(PropertyChangeEvent evt) {
                adjustStaffOnlyFields(OTOptions.isStaff(getProgram().getProgramID()));
            }
        });
        _w.notifyPiCheckBox.addItemListener(this);

        // override green theme for value labels
        _w.totalPlannedPiTime.setForeground(Color.black);
        _w.totalPlannedExecTime.setForeground(Color.black);
        _w.minimumTime.setForeground(Color.black);
        _w.allocatedTime.setForeground(Color.black);
        _w.partnerTime.setForeground(Color.black);
        _w.timeRemaining.setForeground(Color.black);
        _w.programTime.setForeground(Color.black);
        _w.progRefBox.setForeground(Color.black);
        _w.tooStatusLabel.setForeground(Color.black);
    }

    private void adjustStaffOnlyFields(boolean isStaff) {
        _w.totalPlannedPiTimeLabel.setText(isStaff ? "PI" : "Planned");

        // Active checkbox is only for on site use.
        _w.activeCheckBox.setVisible(isStaff);
        _w.completedCheckBox.setVisible(isStaff);
        _w.plannedLabel.setVisible(isStaff);
        _w.totalPlannedExecTimeLabel.setVisible(isStaff);
        _w.totalPlannedExecTime.setVisible(isStaff);
        _w.usedLabel.setVisible(isStaff);
    }

    /**
     * Return the window containing the editor
     */
    public JPanel getWindow() {
        return _w;
    }

    /**
     * Return the data object corresponding to this editor
     * Set the data object corresponding to this editor.
     */
    public void init() {

        adjustStaffOnlyFields(OTOptions.isStaff(getProgram().getProgramID()));

        _showTitle();
        _showPiInfo();

        // misc
        _w.contactBox.setText(getDataObject().getContactPerson());
        _w.ngoContactBox.setText(getDataObject().getNGOContactEmail());

        final TooType tooType = getDataObject().getTooType();
        final String typeStr = tooType.getDisplayValue();
        _w.tooStatusLabel.setText(typeStr);

        final SPProgramID progId = _showProgId();
        _edAux.update(progId);
        EdProgramHelper.updateHistoryTable(this, VcsOtClient.unsafeGetRegistrar());
        _showActiveState();
        _showCompletedState();
        _showNotifyPiState();
        _updateEnabledStates();

        // The total planed time is updated whenever the sequence or instrument
        // is changed. If the user didn't press Apply before selecting the program
        // node, we may need to give give the events a chance to be handled, to make
        // sure this value is updated before it is displayed.
        SwingUtilities.invokeLater(() -> _updateTimes());
    }

    private SPProgramID _showProgId() {

        final StringBuilder buf = new StringBuilder();
        final SPProgramID spProgID = getNode().getProgramID();
        String idStr = null;
        if (spProgID != null) idStr = spProgID.stringValue();
        if ((idStr == null) || "".equals(idStr.trim())) idStr = "none";
        buf.append(idStr.trim()).append(" ");

        buf.append("(");
        if (getDataObject().getProgramMode() == SPProgram.ProgramMode.QUEUE) {
            buf.append("Queue");
            final String queueBand = getDataObject().getQueueBand();
            try {
                final Integer i = Integer.parseInt(queueBand);
                buf.append(", Band ").append(i);
            } catch (Exception ex) {
                // ignore
            }
        } else {
            buf.append("Classical");
        }
        if (_isRollover()) {
            buf.append(", Rollover");
        }
        if (getDataObject().isThesis()) {
            buf.append(", Thesis");
        }
        buf.append(")");

        _w.progRefBox.setText(buf.toString());
        return spProgID;
    }

    private void _showActiveState() {
        _w.activeCheckBox.setSelected(getDataObject().isActive());
    }

    private void _showCompletedState() {
        _w.completedCheckBox.setSelected(getDataObject().isCompleted());
    }

    private void _showNotifyPiState() {
        _w.notifyPiCheckBox.setSelected(getDataObject().isNotifyPi());
    }

    private void _showPiInfo() {
        final SPProgram.PIInfo piInfo = getDataObject().getPIInfo();
        _w.firstNameBox.setText(piInfo.getFirstName());
        _w.lastNameBox.setText(piInfo.getLastName());
        _w.emailBox.setText(piInfo.getEmail());
        _w.phoneBox.setText(piInfo.getPhone());

        final Affiliate affiliate = piInfo.getAffiliate();
        if (affiliate != null) {
            _w.affiliationBox.setText(affiliate.displayValue);
        } else {
            _w.affiliationBox.setText("None");
        }

    }

    private void _showTitle() {
        final String val = getDataObject().getTitle();
        if (val == null) {
            _w.titleBox.setText("");
        } else {
            _w.titleBox.setText(val);
            _w.titleBox.setCaretPosition(0);
        }
    }


    private boolean _isRollover() {
        try {
            //we only care if the program is band 1.
            if (Integer.parseInt(getDataObject().getQueueBand()) == 1) {
                return getDataObject().getRolloverStatus();
            }
        } catch (NumberFormatException nfe) {
            // this is ok .. blank in new programs, etc.
        } catch (Exception random) {
            // we don't want the lack of rollover info to be an issue
        }
        return false;
    }

//    // Update the history table from the data object
//    private void _updateHistoryTable() {
//        final Vector<Vector<String>> tableRows = new Vector<Vector<String>>();
//        final HistoryList l = getDataObject().getHistoryList();
//        final Iterator it = l.iterator();
//        while (it.hasNext()) {
//            final HistoryList.HistoryListItem item = (HistoryList.HistoryListItem) it.next();
//            final long time = item.getTime();
//            final ObsEventMsg event = item.getEvent();
//            final String message = item.getMessage();
//            final Vector<String> row = new Vector<String>();
//            row.add(event.toString());
//            row.add(DateUtil.formatUTC(time));
//            row.add(message);
//            tableRows.add(row);
//        }
//
//        final DefaultTableModel model = new DefaultTableModel(tableRows, _historyTableHead) {
//            public boolean isCellEditable(int row, int column) {
//                return false;
//            }
//        };
//        _w.historyTable.setModel(model);
//        _setColumnWidths();
//    }


    // Update the total planned, allocated, and remaining time displays
    private void _updateTimes() {
        // Total Planned Time
        long piTime = 0, execTime = 0;
        try {
            final PlannedTimeSummary pt = PlannedTimeSummaryService.getTotalTime(getNode());
            piTime = pt.getPiTime();
            execTime = pt.getExecTime();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception at _updateTimes", e);
        }
        _w.totalPlannedPiTime.setText(TimeAmountFormatter.getHMSFormat(piTime));
        _w.totalPlannedExecTime.setText(TimeAmountFormatter.getHMSFormat(execTime));
        // Allocated Time
        TimeValue awardedTime = null;
        try {
            // string is in the format "<value> units"
            awardedTime = getDataObject().getAwardedTime();
            if (awardedTime == null) {
                awardedTime = new TimeValue(0, TimeValue.Units.hours);
            } else {
                awardedTime = awardedTime.convertTo(TimeValue.Units.hours);
            }
            _w.allocatedTime.setText(new HMS(awardedTime.getTimeAmount()).toString());
        } catch (Exception e) {
            DialogUtil.error(e);
            _w.allocatedTime.setText("00:00:00");
        }

        // Minimum time (certain 07B and later Band 3 programs only)
        TimeValue minTime = null;
        try {
            if (Integer.parseInt(getDataObject().getQueueBand()) == 3) {
                minTime = getDataObject().getMinimumTime();
            }
        } catch (NumberFormatException nfe) {
            // this is ok .. blank in new programs, etc.
        }
        if (minTime != null) {
            final double hours = minTime.convertTimeAmountTo(TimeValue.Units.hours);
            _w.minimumTime.setText(new HMS(hours).toString());
            _w.minimumTime.setVisible(true);
            _w.minimumTimeLabel.setVisible(true);
        } else {
            _w.minimumTime.setVisible(false);
            _w.minimumTimeLabel.setVisible(false);
        }

        // Remaining Time (total of Allocated Time minus the sum of the Program Time
        // fields in the observations).
        // XXX TODO: Add elapsed and non-charged times?
        if (awardedTime == null) {
            _w.timeRemaining.setText("00:00:00");
        } else {
            try {
                final ObsTimes obsTimes = ObsTimesService.getCorrectedObsTimes(getProgram());
                long remainingTime = (long) (awardedTime.getTimeAmount() * 3600000);
//                String totalTimeStr = "00:00:00";
                String progTimeStr = "00:00:00";
                String partTimeStr = "00:00:00";
//                String nonChargedTimeStr  = "00:00:00";
                if (obsTimes != null) {
//                    long totalTime = obsTimes.getTotalTime();
//                    totalTimeStr = TimeAmountFormatter.getHMSFormat(totalTime);

                    final ObsTimeCharges otc = obsTimes.getTimeCharges();
                    final long progTime = otc.getTime(ChargeClass.PROGRAM);
                    progTimeStr = TimeAmountFormatter.getHMSFormat(progTime);
                    remainingTime -= progTime;

                    final long partTime = otc.getTime(ChargeClass.PARTNER);
                    partTimeStr = TimeAmountFormatter.getHMSFormat(partTime);

//                    long nonChargedTime = otc.getTime(ChargeClass.NONCHARGED);
//                    nonChargedTimeStr = TimeAmountFormatter.getHMSFormat(nonChargedTime);
                }
                _w.partnerTime.setText(partTimeStr);
                _w.programTime.setText(progTimeStr);

                final String remainingTimeStr = TimeAmountFormatter.getHMSFormat(remainingTime);
                _w.timeRemaining.setText(remainingTimeStr);
            } catch (Exception e) {
                DialogUtil.error(e);
                _w.partnerTime.setText("00:00:00");
                _w.programTime.setText("00:00:00");
                _w.timeRemaining.setText("00:00:00");
            }
        }
    }

    /**
     * Update the enabled states of the widgets based on the program status
     */
    private void _updateEnabledStates() {
        // enable all for now
        final boolean enabled = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
        _w.titleBox.setEnabled(enabled);
        _w.firstNameBox.setEnabled(enabled);
        _w.lastNameBox.setEnabled(enabled);

        _w.emailBox.setEnabled(OTOptions.isStaff(getProgram().getProgramID()));
        _w.phoneBox.setEnabled(enabled);
        _w.affiliationBox.setEnabled(enabled);
        //_w.progStatusBox.setEnabled(enabled);
        _w.contactBox.setEnabled(OTOptions.isStaff(getProgram().getProgramID()));
        _w.ngoContactBox.setEnabled(OTOptions.isStaff(getProgram().getProgramID()) || OTOptions.isNGO(getProgram().getProgramID()));
    }


    // Called when a checkbox is selected or deselected.
    public void itemStateChanged(ItemEvent evt) {
        final Object w = evt.getSource();
        if (w == _w.activeCheckBox) {
            if (_w.activeCheckBox.isSelected()) {
                getDataObject().setActive(SPProgram.Active.YES);
            } else {
                getDataObject().setActive(SPProgram.Active.NO);
            }
        } else if (w == _w.notifyPiCheckBox) {
            if (_w.notifyPiCheckBox.isSelected()) {
                getDataObject().setNotifyPi(YesNoType.YES);
            } else {
                getDataObject().setNotifyPi(YesNoType.NO);
            }
        } else if (w == _w.completedCheckBox) {
            if (_w.completedCheckBox.isSelected()) {
                getDataObject().setCompleted(true);
            } else {
                getDataObject().setCompleted(false);
            }
        }
    }

    /**
     * Watch changes to the title text box.
     *
     * @see TextBoxWidgetWatcher
     */
    public void textBoxKeyPress(TextBoxWidget tbwe) {
        final String s = tbwe.getText().trim();

        if (tbwe == _w.titleBox) {
            getDataObject().setTitle(s);
        } else if (tbwe == _w.firstNameBox) {
            final SPProgram.PIInfo piInfo = getDataObject().getPIInfo();
            getDataObject().setPIInfo(new SPProgram.PIInfo(s, piInfo.getLastName(),
                    piInfo.getEmail(), piInfo.getPhone(), piInfo.getAffiliate()));
        } else if (tbwe == _w.lastNameBox) {
            final SPProgram.PIInfo piInfo = getDataObject().getPIInfo();
            getDataObject().setPIInfo(new SPProgram.PIInfo(piInfo.getFirstName(), s,
                    piInfo.getEmail(), piInfo.getPhone(), piInfo.getAffiliate()));
        } else if (tbwe == _w.emailBox) {
            final SPProgram.PIInfo piInfo = getDataObject().getPIInfo();
            getDataObject().setPIInfo(new SPProgram.PIInfo(piInfo.getFirstName(),
                    piInfo.getLastName(), s, piInfo.getPhone(), piInfo.getAffiliate()));
        } else if (tbwe == _w.phoneBox) {
            final SPProgram.PIInfo piInfo = getDataObject().getPIInfo();
            getDataObject().setPIInfo(new SPProgram.PIInfo(piInfo.getFirstName(),
                    piInfo.getLastName(), piInfo.getEmail(), s, piInfo.getAffiliate()));
        } else if (tbwe == _w.ngoContactBox) {
            getDataObject().setNGOContactEmail(s);
        }
    }

    /**
     * Text box action, ignore.
     *
     * @see TextBoxWidgetWatcher
     */
    public void textBoxAction(TextBoxWidget tbwe) {
    }
}

