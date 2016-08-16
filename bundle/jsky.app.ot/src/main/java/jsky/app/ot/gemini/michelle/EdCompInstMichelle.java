// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: EdCompInstMichelle.java 22544 2009-11-02 15:55:10Z swalker $
//

package jsky.app.ot.gemini.michelle;

import edu.gemini.shared.gui.bean.ComboPropertyCtrl;
import edu.gemini.shared.gui.bean.TextFieldPropertyCtrl;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.data.YesNoType;
import edu.gemini.spModel.gemini.michelle.InstMichelle;
import edu.gemini.spModel.gemini.michelle.MichelleParams.*;
import jsky.app.ot.OTOptions;
import jsky.app.ot.editor.eng.EngEditor;
import jsky.app.ot.editor.type.SpTypeUIUtil;
import jsky.app.ot.gemini.editor.ComponentEditor;
import jsky.app.ot.gemini.editor.EdCompInstBase;
import jsky.util.gui.Resources;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.TextBoxWidget;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;


/**
 * This is the editor for the Michelle instrument component.
 */
public final class EdCompInstMichelle extends EdCompInstBase<InstMichelle>
        implements ActionListener, EngEditor {

    private static final Icon BLANK_ICON = Resources.getIcon("eclipse/blank.gif");
    private static final Icon ENG_ICON = Resources.getIcon("eclipse/engineering.gif");

    // The GUI layout panel
    private final MichelleForm _w;

    /**
     * If true, ignore property change events
     */
    private boolean _ignoreChanges = false;

    private final ComboPropertyCtrl<InstMichelle, Option<DisperserOrder>> disperserOrderCtrl;
    private final ComboPropertyCtrl<InstMichelle, Option<FilterWheelA>> filterACtrl;
    private final ComboPropertyCtrl<InstMichelle, Option<FilterWheelB>> filterBCtrl;
    private final ComboPropertyCtrl<InstMichelle, Option<Position>> injectorPosCtrl;
    private final ComboPropertyCtrl<InstMichelle, Option<Position>> extractorPosCtrl;
    private final TextFieldPropertyCtrl<InstMichelle, Option<Double>> fieldRotatorCtrl;
    private final TextFieldPropertyCtrl<InstMichelle, Option<Double>> slitAngleCtrl;
    private final ComboPropertyCtrl<InstMichelle, Option<EngMask>> engMaskCtrl;
    private final ComboPropertyCtrl<InstMichelle, Option<ChopMode>> chopModeCtrl;
    private final ComboPropertyCtrl<InstMichelle, Option<ChopWaveform>> chopWaveformCtrl;
    private final TextFieldPropertyCtrl<InstMichelle, Integer> nexpCtrl;

    private final DocumentListener expTimeListener = new DocumentListener() {
        private void handleUpdate(DocumentEvent evt) {
            try {
                final Document doc = evt.getDocument();
                final String strVal = doc.getText(0, doc.getLength());
                final double d = Double.parseDouble(strVal);
                getDataObject().setExposureTime(d);
            } catch (Exception ex) {
                // ignore
            }
        }

        public void insertUpdate(DocumentEvent e) {
            handleUpdate(e);
        }

        public void removeUpdate(DocumentEvent e) {
            handleUpdate(e);
        }

        public void changedUpdate(DocumentEvent e) {
            handleUpdate(e);
        }
    };

    /**
     * The constructor initializes the user interface.
     */
    public EdCompInstMichelle() {
        _w = new MichelleForm();

        // add action listeners to the radio buttons
        _w.autoConfigureYesButton.addActionListener(this);
        _w.exposureTime.getDocument().addDocumentListener(expTimeListener);
        _w.autoConfigureNoButton.addActionListener(this);
        _w.polarimetryYesButton.addActionListener(this);
        _w.polarimetryNoButton.addActionListener(this);

        // initialize the combo boxes

        SpTypeUIUtil.initListBox(_w.disperserComboBox, Disperser.class, this);
        SpTypeUIUtil.initListBox(_w.filterComboBox, Filter.class, this);
        SpTypeUIUtil.initListBox(_w.nodOrientationComboBox, NodOrientation.class, this);
        SpTypeUIUtil.initListBox(_w.focalPlaneMaskComboBox, Mask.class, this);

        _w.centralWavelength.addWatcher(this);
        _w.totalOnSourceTime.addWatcher(this);
        _w.nodInterval.addWatcher(this);
        _w.chopAngle.addWatcher(this);
        _w.chopThrow.addWatcher(this);

        // Shows the actual chop throw when focus moves away from the chop
        // throw editor.   If the user types in a value too large or a negative
        // number, it is adjusted by the bean, so the focus listener is here to
        // make sure the actual value is reflected in the UI.
// DISABLED FOR NOW
//        _w.chopThrow.addFocusListener(new FocusListener() {
//            public void focusGained(FocusEvent e) { } // ignore
//                public void focusLost(FocusEvent e) {
//                if (getDataObject() == null) return;
//
//                double ct = getDataObject().getChopThrow();
//                try {
//                    _w.chopThrow.deleteWatcher(EdCompInstMichelle.this);
//                    _w.chopThrow.setText(String.valueOf(ct));
//                } finally {
//                    _w.chopThrow.addWatcher(EdCompInstMichelle.this);
//                }
//            }
//        });

//        final boolean enabled =
//                OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation()) &&
//                        OTOptions.isStaff(getProgram().getProgramID());
        _w.nodIntervalLabel.setEnabled(false);
        _w.nodInterval.setEnabled(false);


        disperserOrderCtrl = ComboPropertyCtrl.optionEnumInstance(InstMichelle.DISPERSER_ORDER_PROP, DisperserOrder.class);
        filterACtrl = ComboPropertyCtrl.optionEnumInstance(InstMichelle.FILTER_A_PROP, FilterWheelA.class);
        filterBCtrl = ComboPropertyCtrl.optionEnumInstance(InstMichelle.FILTER_B_PROP, FilterWheelB.class);
        injectorPosCtrl = ComboPropertyCtrl.optionEnumInstance(InstMichelle.INJECTOR_POSITION_PROP, Position.class);
        extractorPosCtrl = ComboPropertyCtrl.optionEnumInstance(InstMichelle.EXTRACTOR_POSITION_PROP, Position.class);
        fieldRotatorCtrl = TextFieldPropertyCtrl.createOptionDoubleInstance(InstMichelle.FIELD_ROTATOR_ANGLE_PROP, 3);
        slitAngleCtrl = TextFieldPropertyCtrl.createOptionDoubleInstance(InstMichelle.SLIT_ANGLE_PROP, 3);
        engMaskCtrl = ComboPropertyCtrl.optionEnumInstance(InstMichelle.ENG_MASK_PROP, EngMask.class);
        chopModeCtrl = ComboPropertyCtrl.optionEnumInstance(InstMichelle.CHOP_MODE_PROP, ChopMode.class);
        chopWaveformCtrl = ComboPropertyCtrl.optionEnumInstance(InstMichelle.CHOP_WAVEFORM_PROP, ChopWaveform.class);
        nexpCtrl = TextFieldPropertyCtrl.createIntegerInstance(InstMichelle.NEXP_PROP);
    }

    /**
     * Return the window containing the editor
     */
    public JPanel getWindow() {
        return _w;
    }

    public Component getEngineeringComponent() {
        final JPanel pan = new JPanel(new GridBagLayout());
        ComponentEditor.addCtrl(pan, 0, 0, disperserOrderCtrl);
        ComponentEditor.addCtrl(pan, 0, 1, filterACtrl);
        ComponentEditor.addCtrl(pan, 0, 2, filterBCtrl);
        ComponentEditor.addCtrl(pan, 0, 3, injectorPosCtrl);
        ComponentEditor.addCtrl(pan, 0, 4, extractorPosCtrl);
        ComponentEditor.addCtrl(pan, 0, 5, fieldRotatorCtrl, "deg");
        ComponentEditor.addCtrl(pan, 0, 6, slitAngleCtrl, "deg");
        ComponentEditor.addCtrl(pan, 0, 7, engMaskCtrl);
        ComponentEditor.addCtrl(pan, 0, 8, chopModeCtrl);
        ComponentEditor.addCtrl(pan, 0, 9, chopWaveformCtrl);
        ComponentEditor.addCtrl(pan, 0, 10, nexpCtrl);

        pan.add(new JPanel(), new GridBagConstraints() {{
            gridx = 0;
            gridy = 20;
            weighty = 1.0;
            fill = VERTICAL;
        }});

        return pan;
    }

    private final PropertyChangeListener engOverrideListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            _updateEngOverrideFeedback();
            _updateScienceFOV();
        }
    };

    private final PropertyChangeListener disperserListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            _updateChopConfigLabels();
        }
    };

    private final PropertyChangeListener chopModeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            _updateNexp();
        }
    };

    private static final PropertyDescriptor[] OVERRIDE_PROPS = new PropertyDescriptor[]{
            InstMichelle.ENG_MASK_PROP,
            InstMichelle.FILTER_A_PROP,
            InstMichelle.FILTER_B_PROP,
    };

    @Override
    protected void cleanup() {
        if (getDataObject() != null) {
            getDataObject().removePropertyChangeListener(InstMichelle.DISPERSER_PROP.getName(), disperserListener);
            getDataObject().removePropertyChangeListener(InstMichelle.CHOP_MODE_PROP.getName(), chopModeListener);
            for (PropertyDescriptor pd : OVERRIDE_PROPS) {
                getDataObject().removePropertyChangeListener(pd.getName(), engOverrideListener);
            }
        }
    }

    /**
     * Set the data object corresponding to this editor.
     */
    @Override protected void init() {
        super.init();
        for (PropertyDescriptor pd : OVERRIDE_PROPS) {
            getDataObject().addPropertyChangeListener(pd.getName(), engOverrideListener);
        }
        getDataObject().addPropertyChangeListener(InstMichelle.CHOP_MODE_PROP.getName(), chopModeListener);
        getDataObject().addPropertyChangeListener(InstMichelle.DISPERSER_PROP.getName(), disperserListener);

        _updateFilter();
        _updateDisperser();
        _updateMask();
        _updateTotalOnSourceTime();
        _updateNodInterval();
        _updateNodOrientation();
        _updateChopAngle();
        _updateChopThrow();
        _updateChopConfigLabels();
        _updateScienceFOV();
        _updateAutoConfigure();
        _updatePolarimetry();
        _updateNexp();
        _updateEngOverrideFeedback();

        disperserOrderCtrl.setBean(getDataObject());
        filterACtrl.setBean(getDataObject());
        filterBCtrl.setBean(getDataObject());
        injectorPosCtrl.setBean(getDataObject());
        extractorPosCtrl.setBean(getDataObject());
        fieldRotatorCtrl.setBean(getDataObject());
        slitAngleCtrl.setBean(getDataObject());
        engMaskCtrl.setBean(getDataObject());
        chopModeCtrl.setBean(getDataObject());
        chopWaveformCtrl.setBean(getDataObject());
        nexpCtrl.setBean(getDataObject());
    }


    private void _updateEngOverrideFeedback() {
        final Option<FilterWheelA> filtA = getDataObject().getFilterWheelA();
        final Option<FilterWheelB> filtB = getDataObject().getFilterWheelB();

        Icon icon = (filtA.isEmpty() && filtB.isEmpty()) ? BLANK_ICON : ENG_ICON;
        _w.filterOverride.setIcon(icon);

        Color c = (filtA.isEmpty() && filtB.isEmpty()) ? Color.black : Color.gray;
        _w.filterComboBox.setForeground(c);

        final Option<EngMask> engMask = getDataObject().getEngineeringMask();
        icon = engMask.isEmpty() ? BLANK_ICON : ENG_ICON;
        _w.maskOverride.setIcon(icon);

        c = (engMask.isEmpty()) ? Color.black : Color.gray;
        _w.focalPlaneMaskComboBox.setForeground(c);
    }

    private void _updateNexp() {
        final Option<ChopMode> chopMode = getDataObject().getChopMode();
        nexpCtrl.getComponent().setEnabled(!chopMode.isEmpty());
    }

    // Display the current filter settings
    private void _updateFilter() {
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
        final Disperser disperser = getDataObject().getDisperser();
        _w.disperserComboBox.removeActionListener(this);
        _w.disperserComboBox.getModel().setSelectedItem(disperser);
        _w.disperserComboBox.addActionListener(this);
        _w.centralWavelength.setValue(getDataObject().getDisperserLambdaAsString());
        if (disperser == Disperser.MIRROR) {
            _w.centralWavelength.setEnabled(false);
            _w.centralWavelengthLabel.setEnabled(false);
            _w.centralWavelengthUnitsLabel.setEnabled(false);
        } else {
            final boolean enabled = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
            _w.centralWavelength.setEnabled(enabled);
            _w.centralWavelengthUnitsLabel.setEnabled(enabled);
            _w.centralWavelengthLabel.setEnabled(enabled);
        }
    }

    // Update the science field of view based upon the camera and mask settings.
    private void _updateScienceFOV() {
        final Option<EngMask> emask = getDataObject().getEngineeringMask();

        final String text;
        if (emask.isEmpty()) {
            final double[] scienceArea = getDataObject().getScienceArea();
            text = scienceArea[0] + " x " + scienceArea[1] + " arcsec";
        } else {
            text = String.format("n/a (using %s)", emask.getValue().displayValue());
        }
        _w.scienceFOV.setText(text);
    }


    // Update the Total On-Source Time
    private void _updateTotalOnSourceTime() {
        _w.totalOnSourceTime.setValue(getDataObject().getTotalOnSourceTimeAsString());
    }

    // Update the Nod Interval
    private void _updateNodInterval() {
        _w.nodInterval.setValue(getDataObject().getNodIntervalAsString());
    }

    // Update the Chop Angle
    private void _updateChopAngle() {
        _w.chopAngle.setValue(getDataObject().getChopAngleAsString());
    }

    // Update the Chop Throw
    private void _updateChopThrow() {
        _w.chopThrow.setValue(getDataObject().getChopThrowAsString());
    }

    private void _updateChopConfigLabels() {
        String distanceLabel = "Chop Throw";
        String distanceTip = "Enter the Chop Throw in arcsec";
        String angleLabel = "Chop Angle";
        String angleTip = "Enter the Chop Angle in degrees East of North";

        if (getDataObject().getDisperser().getMode() == DisperserMode.NOD) {
            distanceLabel = "Nod Distance";
            distanceTip = "Enter the Nod Distance in arcsec";
            angleLabel = "Nod Angle";
            angleTip = "Enter the Nod Angle in degrees East of North";
        }

        _w.chopThrowLabel.setText(distanceLabel);
        _w.chopThrow.setToolTipText(distanceTip);
        _w.chopAngleLabel.setText(angleLabel);
        _w.chopAngle.setToolTipText(angleTip);
    }

    // Update the Auto-Configure radio buttons
    private void _updateAutoConfigure() {
        final boolean autoConfig = (getDataObject().getAutoConfigure() == AutoConfigure.YES);
        _w.autoConfigureYesButton.setSelected(autoConfig);
        _w.autoConfigureNoButton.setSelected(!autoConfig);
        _showAutoConfigure(autoConfig);
    }

    // Update the Polarimetry radio buttons
    private void _updatePolarimetry() {
        final boolean polarimetry = (getDataObject().getPolarimetry() == YesNoType.YES);
        _w.polarimetryYesButton.setSelected(polarimetry);
        _w.polarimetryNoButton.setSelected(!polarimetry);
    }


    // From an email from Phil:
    //
    // When the on-site flag is set to TRUE and auto-configure = yes the
    // exposure time should be hidden and the T-ReCS instrument s/w will define
    // the exposure time.
    // When the on-site flag is set FALSE, the following parameters
    // should be hidden: exposure time, time per saveset, nod interval and auto-configure.
    private void _showAutoConfigure(boolean autoConfig) {
        if (OTOptions.isStaff(getProgram().getProgramID())) {
            if (autoConfig) {
                _w.exposureTimeUnitsLabel.setVisible(false);
                _w.exposureTime.getDocument().removeDocumentListener(expTimeListener);
                _w.exposureTime.setText("auto");
                _w.exposureTime.getDocument().addDocumentListener(expTimeListener);
                _w.exposureTime.setEnabled(false);
            } else {
                _w.exposureTimeUnitsLabel.setVisible(true);
                _w.exposureTime.getDocument().removeDocumentListener(expTimeListener);
                _w.exposureTime.setText(getDataObject().getExposureTimeAsString());
                _w.exposureTime.getDocument().addDocumentListener(expTimeListener);
                _w.exposureTime.setEnabled(true);
            }
        } else {
            _w.exposureTime.setVisible(false);
            _w.exposureTimeLabel.setVisible(false);
            _w.exposureTimeUnitsLabel.setVisible(false);

            _w.nodInterval.setVisible(false);
            _w.nodIntervalLabel.setVisible(false);
            _w.nodIntervalUnitsLabel.setVisible(false);

            _w.nodOrientationComboBox.setVisible(false);
            _w.nodOrientationLabel.setVisible(false);

            _w.autoConfigureYesButton.setVisible(false);
            _w.autoConfigureNoButton.setVisible(false);
            _w.autoConfigureLabel.setVisible(false);
        }
    }


    /**
     * Return the position angle text box
     */
    public TextBoxWidget getPosAngleTextBox() {
        return _w.posAngle;
    }

    /**
     * Return the exposure time text box
     */
    public jsky.util.gui.TextBoxWidget getExposureTimeTextBox() {
        // We have to handle the exposure time text box ourselves.
        return null;
//        return _w.exposureTime;
    }

    /**
     * Return the coadds text box.
     */
    public TextBoxWidget getCoaddsTextBox() {
        return null;
    }


    /**
     * Handle action events.
     */
    public void actionPerformed(ActionEvent evt) {
        final Object w = evt.getSource();

//        boolean enabled = OTOptions.isEditable(_prog);

        if (w == _w.autoConfigureYesButton) {
            getDataObject().setAutoConfigure(AutoConfigure.YES);
            _showAutoConfigure(true);
        } else if (w == _w.autoConfigureNoButton) {
            getDataObject().setAutoConfigure(AutoConfigure.NO);
            _showAutoConfigure(false);
        } else if (w == _w.polarimetryYesButton) {
            getDataObject().setPolarimetry(YesNoType.YES);
        } else if (w == _w.polarimetryNoButton) {
            getDataObject().setPolarimetry(YesNoType.NO);
        } else if (w == _w.disperserComboBox) {
            final Disperser disperser = (Disperser) _w.disperserComboBox.getSelectedItem();
            getDataObject().setDisperser(disperser);
            _updateDisperser();
        } else if (w == _w.filterComboBox) {
            getDataObject().setFilter((Filter) _w.filterComboBox.getSelectedItem());
        } else if (w == _w.nodOrientationComboBox) {
            getDataObject().setNodOrientation((NodOrientation) _w.nodOrientationComboBox.getSelectedItem());
        } else if (w == _w.focalPlaneMaskComboBox) {
            getDataObject().setMask((Mask) _w.focalPlaneMaskComboBox.getSelectedItem());
            _updateScienceFOV();
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
        } else if (tbwe == _w.nodInterval) {
            getDataObject().setNodInterval(value);
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
        super.updateEnabledState(enabled);

        enabled = enabled && OTOptions.isStaff(getProgram().getProgramID());
        _w.nodIntervalLabel.setEnabled(enabled);
        _w.nodInterval.setEnabled(enabled);

        if (getDataObject() != null)
            _showAutoConfigure(getDataObject().getAutoConfigure() == AutoConfigure.YES);
    }


    /**
     * Implements the PropertyChangeListener interface
     * (redefined from parent class)
     */
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);

        if (_ignoreChanges)
            return;

        final InstMichelle inst = getDataObject();
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
}

