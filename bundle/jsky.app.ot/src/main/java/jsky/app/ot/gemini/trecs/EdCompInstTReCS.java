// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: EdCompInstTReCS.java 7408 2006-11-10 19:49:19Z anunez $
//

package jsky.app.ot.gemini.trecs;

import edu.gemini.spModel.gemini.trecs.InstTReCS;
import edu.gemini.spModel.gemini.trecs.TReCSParams.*;
import jsky.app.ot.OTOptions;
import jsky.app.ot.editor.type.SpTypeComboBoxModel;
import jsky.app.ot.editor.type.SpTypeComboBoxRenderer;
import jsky.app.ot.gemini.editor.EdCompInstBase;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.TextBoxWidget;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;


/**
 * This is the editor for the TReCS instrument component.
 */
public final class EdCompInstTReCS extends EdCompInstBase<InstTReCS>
     implements ActionListener {


    // The GUI layout panel
    private final TReCSForm _w;

    /** If true, ignore property change events */
    private boolean _ignoreChanges = false;

    @Override
    protected double getDefaultExposureTime() {
        return InstTReCS.DEF_EXPOSURE_TIME;
    }

    /**
     * The constructor initializes the user interface.
     */
    public EdCompInstTReCS() {
        _w = new TReCSForm();

        // if not staff, hide some items
        _setVisibleItems();

        // initialize the combo boxes
        _initComboBox(Disperser.class, _w.disperserComboBox);
        _initComboBox(DataMode.class, _w.dataModeComboBox);
        _initComboBox(ObsMode.class,  _w.obsModeComboBox);
        _initComboBox(WindowWheel.class, _w.winWheelComboBox);
        _initComboBox(ReadoutMode.class, _w.readoutModeComboBox);
        _initComboBox(Filter.class, _w.filterComboBox);
        _initComboBox(NodOrientation.class, _w.nodOrientationComboBox);
        _initComboBox(Mask.class, _w.focalPlaneMaskComboBox);



        _w.filterComboBox.addActionListener(this);
        _w.disperserComboBox.addActionListener(this);
        _w.dataModeComboBox.addActionListener(this);
        _w.obsModeComboBox.addActionListener(this);
        _w.winWheelComboBox.addActionListener(this);
        _w.readoutModeComboBox.addActionListener(this);
        _w.nodOrientationComboBox.addActionListener(this);
        _w.focalPlaneMaskComboBox.addActionListener(this);

        _w.centralWavelength.addWatcher(this);
        _w.totalOnSourceTime.addWatcher(this);
        _w.savesetTime.addWatcher(this);
        _w.nodDwell.addWatcher(this);
        _w.nodSettle.addWatcher(this);
        _w.chopAngle.addWatcher(this);
        _w.chopThrow.addWatcher(this);

        // TODO: re-examine this
        final boolean enabled = false; // OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation()) && OTOptions.isStaffAndHasPreferredSite();
        _w.savesetTimeLabel.setEnabled(enabled);
        _w.savesetTime.setEnabled(enabled);

        _w.nodDwellLabel.setEnabled(enabled);
        _w.nodDwell.setEnabled(enabled);

        _w.nodSettleLabel.setEnabled(enabled);
        _w.nodSettle.setEnabled(enabled);
    }


    /** Return the window containing the editor */
    public JPanel getWindow() {
        return _w;
    }

    /** Set the data object corresponding to this editor. */
    @Override protected void init() {
        super.init();
        _updateFilter();
        _updateDisperser();
        _updateMask();
        _updateTotalOnSourceTime();
        _updateTimePerSaveset();
        _updateNodDwell();
        _updateNodSettle();
        _updateNodOrientation();
        _updateChopAngle();
        _updateChopThrow();
        _updateDataMode();
        _updateObsMode();
        _updateWinWheel();
        _updateReadoutMode();
    }

    // Display the current filter settings
    private void _updateFilter() {
       // _w.filterComboBox.setValue(getDataObject().getFilter().ordinal());
        _w.filterComboBox.getModel().setSelectedItem(getDataObject().getFilter());
    }

    // Display the current filter settings
    private void _updateNodOrientation() {
        _w.nodOrientationComboBox.getModel().setSelectedItem(getDataObject().getNodOrientation());
    }

    // Display the current Mask settings
    private void _updateMask() {
        _w.focalPlaneMaskComboBox.getModel().setSelectedItem(getDataObject().getMask());
    }

    // Display the current Disperser settings
    private void _updateDisperser() {
        _w.disperserComboBox.getModel().setSelectedItem(getDataObject().getDisperser());
        _w.centralWavelength.setValue(getDataObject().getDisperserLambda());
        final Disperser disperser = getDataObject().getDisperser();
        if (disperser == Disperser.MIRROR) {
            _w.centralWavelength.setEnabled(false);
            _w.centralWavelengthLabel.setEnabled(false);
        } else {
            final boolean enabled = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
            _w.centralWavelength.setEnabled(enabled);
            _w.centralWavelengthLabel.setEnabled(enabled);
        }
    }

    // Update the Total On-Source Time
    private void _updateTotalOnSourceTime() {
        _w.totalOnSourceTime.setValue(getDataObject().getTotalOnSourceTimeAsString());
    }

    // Update the Time Per Saveset
    private void _updateTimePerSaveset() {
        _w.savesetTime.setValue(getDataObject().getTimePerSavesetAsString());
    }

    // Update the Nod Dwell time
    private void _updateNodDwell() {
        _w.nodDwell.setValue(getDataObject().getNodDwellAsString());
    }

    // Update the Nod Settle time
    private void _updateNodSettle() {
        _w.nodSettle.setValue(getDataObject().getNodSettleAsString());
    }

    // Update the Chop Angle
    private void _updateChopAngle() {
        _w.chopAngle.setValue(getDataObject().getChopAngleAsString());
    }

    // Update the Chop Throw
    private void _updateChopThrow() {
        _w.chopThrow.setValue(getDataObject().getChopThrowAsString());
    }

    // Display the current DataMode settings
    private void _updateDataMode() {
        _w.dataModeComboBox.getModel().setSelectedItem(getDataObject().getDataMode());
    }

    // Display the current ObsMode settings
    private void _updateObsMode() {
        _w.obsModeComboBox.getModel().setSelectedItem(getDataObject().getObsMode());
    }

    // Display the current Window Wheel settings
    private void _updateWinWheel() {
        _w.winWheelComboBox.getModel().setSelectedItem(getDataObject().getWindowWheel());
    }

    // Display the current Readout Mode settings
    private void _updateReadoutMode() {
        _w.readoutModeComboBox.getModel().setSelectedItem(getDataObject().getReadoutMode());
    }

    // Some items are only visible for staff (-onsite).
    private void _setVisibleItems() {
        if (!OTOptions.isStaffGlobally()) { // getProgram().getProgramID())) {
            _w.savesetTime.setVisible(false);
            _w.savesetTimeLabel.setVisible(false);

            _w.nodDwell.setVisible(false);
            _w.nodDwellLabel.setVisible(false);

            _w.nodSettle.setVisible(false);
            _w.nodSettleLabel.setVisible(false);

            _w.nodOrientationComboBox.setVisible(false);
            _w.nodOrientationLabel.setVisible(false);

            _w.dataModeComboBox.setVisible(false);
            _w.dataModeLabel.setVisible(false);

            _w.obsModeComboBox.setVisible(false);
            _w.obsModeLabel.setVisible(false);

            _w.winWheelComboBox.setVisible(false);
            _w.winWheelLabel.setVisible(false);
        }
    }


    /** Return the position angle text box */
    public TextBoxWidget getPosAngleTextBox() {
        return _w.posAngle;
    }

    /** Return the exposure time text box */
    public TextBoxWidget getExposureTimeTextBox() {
        return null;
    }

    /** Return the coadds text box. */
    public TextBoxWidget getCoaddsTextBox() {
        return null;
    }


    public void actionPerformed(ActionEvent actionEvent) {
        final Object w = actionEvent.getSource();

        if (w == _w.nodOrientationComboBox) {
            getDataObject().setNodOrientation((NodOrientation)_w.nodOrientationComboBox.getSelectedItem());
        } else if (w == _w.filterComboBox) {
            getDataObject().setFilter((Filter)_w.filterComboBox.getSelectedItem());
        } else if (w == _w.disperserComboBox) {
            getDataObject().setDisperser((Disperser)_w.disperserComboBox.getSelectedItem());
            _updateDisperser();
        } else if (w == _w.focalPlaneMaskComboBox) {
            getDataObject().setMask((Mask)_w.focalPlaneMaskComboBox.getSelectedItem());
        } else if (w == _w.dataModeComboBox) {
            getDataObject().setDataMode((DataMode)_w.dataModeComboBox.getSelectedItem());
        } else if (w == _w.obsModeComboBox) {
            getDataObject().setObsMode((ObsMode)_w.obsModeComboBox.getSelectedItem());
        } else if (w == _w.winWheelComboBox) {
            getDataObject().setWindowWheel((WindowWheel)_w.winWheelComboBox.getSelectedItem());
        } else if (w == _w.readoutModeComboBox) {
            getDataObject().setReadoutMode((ReadoutMode)_w.readoutModeComboBox.getSelectedItem());
        }
    }


    /**
     * A key was pressed in the given TextBoxWidget.
     */
    public void textBoxKeyPress(TextBoxWidget tbwe) {
        super.textBoxKeyPress(tbwe);

        // Note: assume all numeric fields for now
        final double value;
        try {
            value = Double.parseDouble(tbwe.getValue());
        } catch (NumberFormatException e) {
            return;
        }

        if (tbwe == _w.centralWavelength) {
            getDataObject().setDisperserLambda(value);
        } else if (tbwe == _w.totalOnSourceTime) {
            getDataObject().setTimeOnSource(value);
        } else if (tbwe == _w.savesetTime) {
            getDataObject().setTimePerSaveset(value);
        } else if (tbwe == _w.nodDwell) {
            getDataObject().setNodDwell(value);
        } else if (tbwe == _w.nodSettle) {
            getDataObject().setNodSettle(value);
        } else if (tbwe == _w.chopAngle) {
            _ignoreChanges = true;
            try {
                getDataObject().setChopAngle(value);
            } catch (Exception e) {
                DialogUtil.error(e);
            }
            _ignoreChanges = false;
        } else if (tbwe == _w.chopThrow) {
            _ignoreChanges = true;
            try {
                getDataObject().setChopThrow(value);
            } catch (Exception e) {
                DialogUtil.error(e);
            }
            _ignoreChanges = false;
        }
    }

    /**
     * A return key was pressed in the given TextBoxWidget.
     */
    public void textBoxAction(TextBoxWidget tbwe) {
    }


    /**
     * Redefined from the parent class to enable/disabled some components
     */
    protected void updateEnabledState(boolean enabled) {
        boolean enabled1 = enabled;
        super.updateEnabledState(enabled1);

        enabled1 = enabled1 && OTOptions.isStaff(getProgram().getProgramID());
        _w.savesetTimeLabel.setEnabled(enabled1);
        _w.savesetTime.setEnabled(enabled1);

        _w.nodDwellLabel.setEnabled(enabled1);
        _w.nodDwell.setEnabled(enabled1);

        _w.nodSettleLabel.setEnabled(enabled1);
        _w.nodSettle.setEnabled(enabled1);
    }


    /**
     * Implements the PropertyChangeListener interface
     * (redefined from parent class)
     */
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);

        if (_ignoreChanges)
            return;

        final InstTReCS inst = getDataObject();
        if (inst != null && evt.getSource() == inst) {
            final String newChopAngle = inst.getChopAngleAsString();
            if (!newChopAngle.equals(_w.chopAngle.getText())) {
                _w.chopAngle.setText(newChopAngle);
            }
            final String newChopThrow = inst.getChopThrowAsString();
            if (!newChopThrow.equals(_w.chopThrow.getText())) {
                _w.chopThrow.setText(newChopThrow);
            }
        }
    }

    /**
     * Just an auxiliary method to fill up the combo boxes with the
     * appropriate enum types an renderers
     */
    private <T extends Enum<T>> void _initComboBox(Class<T> c, JComboBox cb) {
        final ComboBoxModel model = new SpTypeComboBoxModel<T>(c);
        cb.setModel(model);
        cb.setRenderer(new SpTypeComboBoxRenderer());
        cb.setMaximumRowCount(c.getEnumConstants().length);
    }

}

