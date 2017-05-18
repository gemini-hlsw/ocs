package jsky.app.ot.gemini.editor;

import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.shared.util.TimeValue;
import edu.gemini.spModel.data.YesNoType;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.obs.ObsTimesService;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummary;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummaryService;
import edu.gemini.spModel.time.*;
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

import javax.swing.*;
import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the editor for the Science Program component.
 * NOTE: component names / labels are no longer congruent with data object member names because of REL-2942.
 */
public final class EdProgram extends OtItemEditor<ISPProgram, SPProgram> implements TextBoxWidgetWatcher {
    private static final Logger LOGGER = Logger.getLogger(EdProgram.class.getName());

    // String representation in HMS of a time of zero.
    private static String ZERO_TIME_STRING = TimeAmountFormatter.getHMSFormat(0);

    // the GUI layout panel
    final ProgramForm _w;

    private final AuxFileEditor _edAux;


    /**
     * The constructor initializes the user interface.
     */
    public EdProgram() {
        _w = new ProgramForm();
        _edAux = new AuxFileEditor(_w);

        _w.additionalSupportBox.setForeground(Color.black);

        _w.titleBox.           addWatcher(this);
        _w.firstNameBox.       addWatcher(this);
        _w.lastNameBox.        addWatcher(this);
        _w.emailBox.           addWatcher(this);
        _w.phoneBox.           addWatcher(this);
        _w.principalSupportBox.addWatcher(this);

        // add menu choices
        _w.affiliationBox.setForeground(Color.black);

        _w.historyTable.setBackground(_w.getBackground());
        _w.historyTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        _w.activeCheckBox.addItemListener(e ->
                getDataObject().setActive(_w.activeCheckBox.isSelected() ? SPProgram.Active.YES : SPProgram.Active.NO));
        _w.completedCheckBox.addItemListener(e -> getDataObject().setCompleted(_w.completedCheckBox.isSelected()));

        adjustStaffOnlyFields(OTOptions.isStaffGlobally());
        StaffBean.addPropertyChangeListener(evt ->
                adjustStaffOnlyFields(OTOptions.isStaff(getProgram().getProgramID())));

        _w.notifyPiCheckBox.addItemListener(e ->
                getDataObject().setNotifyPi(_w.notifyPiCheckBox.isSelected() ? YesNoType.YES : YesNoType.NO));

        // override green theme for value labels
        _w.totalPlannedPiTime.setForeground(Color.black);
        _w.totalPlannedExecTime.setForeground(Color.black);
        _w.minimumTime.setForeground(Color.black);
        _w.allocatedTime.setForeground(Color.black);
        _w.partnerTime.setForeground(Color.black);
        _w.remainingTime.setForeground(Color.black);
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
    @Override
    public JPanel getWindow() {
        return _w;
    }

    /**
     * Return the data object corresponding to this editor
     * Set the data object corresponding to this editor.
     */
    @Override
    public void init() {

        adjustStaffOnlyFields(OTOptions.isStaff(getProgram().getProgramID()));

        _showTitle();
        _showPiInfo();

        // misc
        _w.additionalSupportBox.setText(getDataObject().getContactPerson());
        _w.principalSupportBox.setText(getDataObject().getPrimaryContactEmail());

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
        SwingUtilities.invokeLater(this::_updateTimes);
    }

    private SPProgramID _showProgId() {

        final StringBuilder buf = new StringBuilder();

        final SPProgramID spProgID = getNode().getProgramID();
        final String progIdStr = ImOption.apply(getNode().getProgramID()).
                map(id -> id.toString().trim()).
                filter(idStr -> !("".equals(idStr))).
                getOrElse("none");
        buf.append(progIdStr).append(" ");

        buf.append("(");
        if (getDataObject().getProgramMode() == SPProgram.ProgramMode.QUEUE) {
            buf.append("Queue");
            final String queueBand = getDataObject().getQueueBand();
            try {
                final Integer i = Integer.parseInt(queueBand);
                buf.append(", Band ").append(i);
            } catch (final Exception ex) {
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
        _w.lastNameBox. setText(piInfo.getLastName());
        _w.emailBox.    setText(piInfo.getEmail());
        _w.phoneBox.    setText(piInfo.getPhone());

        final String affiliateStr = ImOption.apply(piInfo.getAffiliate()).map(a -> a.displayValue).getOrElse("None");
        _w.affiliationBox.setText(affiliateStr);
    }

    private void _showTitle() {
        final String title = ImOption.apply(getDataObject().getTitle()).getOrElse("");
        _w.titleBox.setText(title);
        _w.titleBox.setCaretPosition(0);
    }


    private boolean _isRollover() {
        try {
            //we only care if the program is band 1.
            if (Integer.parseInt(getDataObject().getQueueBand()) == 1) {
                return getDataObject().getRolloverStatus();
            }
        } catch (final Exception e) {
            // If a number format exception (blank in new programs), ignore.
            // If another exception, we don't want the lack of rollover info to be an issue.
            // This is expected, as the value will be blank in new programs.
        }
        return false;
    }


    // Auxiliary method to retrieve the minimum time in hours.
    private Option<Double> getMinimumTimeInHours() {
        try {
            if (Integer.parseInt(getDataObject().getQueueBand()) == 3)
                return new Some<>(getDataObject().getMinimumTime().convertTimeAmountTo(TimeValue.Units.hours));
        } catch (final NumberFormatException nfe) {
            // This is expected, as the value will be blank in new programs.
        }
        return None.instance();
    }


    // Auxiliary methods to easily set time labels in the form.
    private static void setTimeLabel(final long time, final JLabel timeLabel) {
        final String timeStr = time == 0 ? ZERO_TIME_STRING : TimeAmountFormatter.getHMSFormat(time);
        timeLabel.setText(timeStr);
    }
    private static void setTimeLabel(final double time, final JLabel timeLabel) {
        final String timeStr = time == 0d ? ZERO_TIME_STRING : new HMS(time).toString();
        timeLabel.setText(timeStr);
    }

    // Update the total planned, allocated, and remaining time displays
    private void _updateTimes() {
        // Total planned time.
        try {
            final PlannedTimeSummary pt = PlannedTimeSummaryService.getTotalTime(getNode());
            setTimeLabel(pt.getPiTime(),   _w.totalPlannedPiTime);
            setTimeLabel(pt.getExecTime(), _w.totalPlannedExecTime);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception at _updateTimes", e);
            setTimeLabel(0, _w.totalPlannedPiTime);
            setTimeLabel(0, _w.totalPlannedExecTime);
        }

        // Allocated time (awarded time in hours).
        try {
            final double awardedTime = getDataObject().getAwardedProgramTime().getTimeAmount();
            setTimeLabel(awardedTime, _w.allocatedTime);
        } catch (final Exception e) {
            DialogUtil.error(e);
            setTimeLabel(0, _w.allocatedTime);
        }

        // Minimum time (certain 07B and later Band 3 programs only).
        final Option<Double> minTimeOpt = getMinimumTimeInHours();
        final boolean minTimeVisible = minTimeOpt.isDefined();
        _w.minimumTime.     setVisible(minTimeVisible);
        _w.minimumTimeLabel.setVisible(minTimeVisible);
        minTimeOpt.forEach(minTime -> setTimeLabel(minTime, _w.minimumTime));

        // Partner time, program time, and remaining time.
        try {
            final ISPProgram prog = getProgram();
            final ObsTimeCharges otc = ObsTimesService.getCorrectedObsTimes(prog).getTimeCharges();
            setTimeLabel(otc.getTime(ChargeClass.PARTNER), _w.partnerTime);
            setTimeLabel(otc.getTime(ChargeClass.PROGRAM), _w.programTime);
            setTimeLabel(ObsTimesService.getRemainingProgramTime(prog), _w.remainingTime);

        } catch (final Exception e) {
            // This can fail, for example, if the program is null.
            DialogUtil.error(e);
            setTimeLabel(0, _w.partnerTime);
            setTimeLabel(0, _w.programTime);
            setTimeLabel(0, _w.remainingTime);
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
        _w.additionalSupportBox.setEnabled(OTOptions.isStaff(getProgram().getProgramID()));

        // This confusion is due to the fact that this widget used to be for NGO support, and has now been relabeled.
        _w.principalSupportBox.setEnabled(OTOptions.isStaff(getProgram().getProgramID()) || OTOptions.isNGO(getProgram().getProgramID()));
    }

    /**
     * Watch changes to the title text box.
     *
     * @see TextBoxWidgetWatcher
     */
    @Override
    public void textBoxKeyPress(final TextBoxWidget tbwe) {
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
        } else if (tbwe == _w.principalSupportBox) {
            getDataObject().setPrimaryContactEmail(s);
        }
    }
}

