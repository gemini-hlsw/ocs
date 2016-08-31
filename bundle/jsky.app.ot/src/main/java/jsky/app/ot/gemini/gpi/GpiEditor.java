package jsky.app.ot.gemini.gpi;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.shared.gui.bean.CheckboxPropertyCtrl;
import edu.gemini.shared.gui.bean.ComboPropertyCtrl;
import edu.gemini.shared.gui.bean.EditEvent;
import edu.gemini.shared.gui.bean.EditListener;
import edu.gemini.shared.gui.bean.TextFieldPropertyCtrl;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.gemini.gpi.Gpi;
import jsky.app.ot.editor.eng.EngEditor;
import jsky.app.ot.gemini.editor.ComponentEditor;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;

/**
 * User interface and editor for GPI.
 */
public class GpiEditor extends ComponentEditor<ISPObsComponent, Gpi> implements EngEditor {

    // Displays the extra text box when Half Wave Plate Angle is set to MANUAL
    private final class HalfWavePlateAngleManualEnabler implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            update((Gpi) evt.getSource());
        }

        void update(Gpi inst) {
            final boolean visible = inst.getDisperser() == Gpi.Disperser.WOLLASTON;

            halfWavePlateAngleValueLabel.setVisible(visible);
            halfWavePlateAngleValueCtrl.getComponent().setVisible(visible);
            halfWavePlateAngleValueUnits.setVisible(visible);
            halfWavePlateAngleValueCtrl.getComponent().setEnabled(visible);
            if (visible)
                halfWavePlateAngleValueCtrl.updateComponent();
        }
    }

    // Updates manual HalfWavePlateAngle error message
    private final class HalfWavePlateAngleMessageUpdater implements EditListener<Gpi, Double>, PropertyChangeListener {
        private final JLabel label;

        HalfWavePlateAngleMessageUpdater(JLabel label) {
            this.label = label;
        }

        public void valueChanged(EditEvent<Gpi, Double> event) {
            update(event.getNewValue());
        }

        public void propertyChange(PropertyChangeEvent evt) {
            update();
        }

        void update() {
            final Gpi inst = getDataObject();
            update((inst == null) ? null : inst.getHalfWavePlateAngle());
        }

        void update(Double angle) {
            final Gpi inst = getDataObject();
            Color fg = Color.black;
            String txt = "";
            if (inst != null && angle != null) {
                if (angle > 360) {
                    fg = FATAL_FG_COLOR;
                    txt = "<html>Max value is 360 deg!";
                } else if (angle < 0) {
                    fg = FATAL_FG_COLOR;
                    txt = "<html>Min value is 0 deg!";
                }
            }

            label.setText(txt);
            label.setForeground(fg);
        }
    }

    // Displays the extra text box when ArtificialSource.WHITE_LIGHT_LAMP is selected
    private final class ArtificialSourceEnabler implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            update((Gpi) evt.getSource());
        }

        void update(Gpi inst) {
            boolean visible = inst.getSuperContinuumLampEnum() == Gpi.ArtificialSource.ON
                    || inst.getVisibleLaserLampEnum() == Gpi.ArtificialSource.ON
                    || inst.getIrLaserLampEnum() == Gpi.ArtificialSource.ON;

            attenuationLabel.setVisible(visible);
            attenuationCtrl.getComponent().setVisible(visible);
            attenuationCtrl.getComponent().setEnabled(visible);
            attenuationUnitsLabel.setVisible(visible);
        }
    }

    // Updates exposure time warning message
    private final class ExposureTimeMessageUpdater implements EditListener<Gpi, Double>, PropertyChangeListener {
        private final JLabel label;

        ExposureTimeMessageUpdater(JLabel label) {
            this.label = label;
        }

        public void valueChanged(EditEvent<Gpi, Double> event) {
            update(event.getNewValue());
        }

        public void propertyChange(PropertyChangeEvent evt) {
            update();
        }

        void update() {
            final Gpi inst = getDataObject();
            update((inst == null) ? null : inst.getExposureTime());
        }

        void update(Double expTime) {
            final Gpi inst = getDataObject();
            final int coadds = inst.getCoadds();
            final int maxVal = inst.getMaximumExposureTimeSecs();
            final double minVal = inst.getMinimumExposureTimeSecs();
            Color fg = Color.black;
            String txt = "";
            if (expTime != null) {
                if (expTime > maxVal) {
                    fg = FATAL_FG_COLOR;
                    txt = "<html>Exposure times of less than " + maxVal + " seconds give optimum performance.";
                } else if (expTime * coadds > maxVal) {
                    fg = FATAL_FG_COLOR;
                    final float c = 0.1F; // See OT-78
                    final int n = Math.round((float) (maxVal / (expTime + c)));
                    txt = "<html>Exposures longer than " + maxVal + " seconds may lead to smearing. <br>"
                            + n + " coadds are recommended based on the exposure time";
                } else if (expTime < minVal) {
                    fg = FATAL_FG_COLOR;
                    txt = String.format("Below recommendation (" + "%.2f" + " sec).", minVal);
                }
            }

            label.setText(txt);
            label.setForeground(fg);
        }
    }



    // Updates Artificial source attenuation warning message
    private final class AttenuationMessageUpdater implements EditListener<Gpi, Double>, PropertyChangeListener {
        private final JLabel label;

    AttenuationMessageUpdater(JLabel label) {
            this.label = label;
        }

        public void valueChanged(EditEvent<Gpi, Double> event) {
            update(event.getNewValue());
        }

        public void propertyChange(PropertyChangeEvent evt) {
            update();
        }

        void update() {
            Gpi inst = getDataObject();
            update((inst == null) ? null : inst.getArtificialSourceAttenuation());
        }

        void update(Double attenuation) {
            double maxVal = Gpi.MAX_ARTIFICIAL_SOURCE_ATTENUATION;
            double minVal = Gpi.MIN_ARTIFICIAL_SOURCE_ATTENUATION;
            Color fg = Color.black;
            String txt = "";
            if (attenuation > maxVal) {
                fg = FATAL_FG_COLOR;
                txt = "<html>Artificial source attenuation greater than maximum " + Gpi.MAX_ARTIFICIAL_SOURCE_ATTENUATION;
            } else if (attenuation < minVal) {
                fg = FATAL_FG_COLOR;
                txt = "<html>Artificial source attenuation less than minimum " + Gpi.MIN_ARTIFICIAL_SOURCE_ATTENUATION;
            }

            label.setText(txt);
            label.setForeground(fg);
        }
    }


    // Updates observing mode error message
    private final class ObservingModeMessageUpdater implements EditListener<Gpi, Option<Gpi.ObservingMode>>, PropertyChangeListener {
        private final JLabel label;

        ObservingModeMessageUpdater(JLabel label) {
            this.label = label;
        }

        public void valueChanged(final EditEvent<Gpi, Option<Gpi.ObservingMode>> event) {
            SwingUtilities.invokeLater(() -> update(event.getNewValue()));
        }

        public void propertyChange(PropertyChangeEvent evt) {
            SwingUtilities.invokeLater(this::update);
        }

        void update() {
            final Gpi inst = getDataObject();
            update((inst == null) ? null : inst.getObservingMode());
        }

        void update(Option<Gpi.ObservingMode> obsMode) {
            final Gpi inst = getDataObject();
            Color fg = Color.black;
            String txt = "";
            if (inst != null && obsMode != null) {
                if (obsMode.isEmpty()) {
                    fg = FATAL_FG_COLOR;
                    txt = "<html>Please select an observing mode.";
                    updateComboBox(Gpi.ObservingMode.NONENGINEERING_VALUES, false);
                } else {
                    final Gpi.ObservingMode observingMode = obsMode.getValue();
                    if (observingMode == Gpi.ObservingMode.NONSTANDARD
                            || inst.getFilter() != observingMode.getFilter()
                            || inst.getApodizer() != observingMode.getApodizer()
                            || inst.getFpm() != observingMode.getFpm()
                            || inst.getLyot() != observingMode.getLyot()) {
                        fg = WARNING_FG_COLOR;
                        txt = "<html>Observing Mode will not be fully applied.";
                        updateComboBox(Gpi.ObservingMode.ENGINEERING_VALUES, true);
                    } else {
                        updateComboBox(Gpi.ObservingMode.NONENGINEERING_VALUES, false);
                    }
                }
            }

            label.setText(txt);
            label.setForeground(fg);
        }

        // Updates the list of available items in the obs mode combobox
        @SuppressWarnings("rawtypes")
        private void updateComboBox(Option<Gpi.ObservingMode>[] values, boolean selectLast) {
            final JComboBox cb = ((JComboBox) observingModeCtrl.getComponent());
            if (cb.getModel().getSize() != values.length) {
                int selected = cb.getSelectedIndex();
                cb.setModel(new DefaultComboBoxModel<>(values));
                if (selectLast) {
                    cb.setSelectedIndex(values.length - 1);
                } else if (selected < values.length) {
                    cb.setSelectedIndex(selected);
                }
            }
        }
    }

    // Updates the value of the useCal/useAo/noFpmPinhole/aoOptimize if it changes
    private final class UseLoopListener implements PropertyChangeListener {
        private final CheckboxPropertyCtrl<Gpi> ctrl;

        public UseLoopListener(CheckboxPropertyCtrl<Gpi> ctrl) {
            this.ctrl = ctrl;
        }
        public void propertyChange(PropertyChangeEvent evt) {
            ((JCheckBox)ctrl.getComponent()).setSelected(evt.getNewValue() == Boolean.TRUE);
        }
    }

    private final JPanel pan;

    private final ComboPropertyCtrl<Gpi, Gpi.Disperser> disperserCtrl;
    private final ComboPropertyCtrl<Gpi, Gpi.Adc> adcCtrl;
    private final ComboPropertyCtrl<Gpi, Option<Gpi.ObservingMode>> observingModeCtrl;

    private final TextFieldPropertyCtrl<Gpi, Integer> coaddsCtrl;
    private final TextFieldPropertyCtrl<Gpi, Double> expTimeCtrl;

    private final ExposureTimeMessageUpdater exposureTimeMessageUpdater;
    private final HalfWavePlateAngleMessageUpdater halfWavePlateAngleMessageUpdater;
    private final ObservingModeMessageUpdater observingModeMessageUpdater;

    private final CheckboxPropertyCtrl<Gpi> astrometricFieldCtrl;

    private final HalfWavePlateAngleManualEnabler halfWavePlateAngleManualEnabler = new HalfWavePlateAngleManualEnabler();

    private final JLabel halfWavePlateAngleValueLabel;
    private final TextFieldPropertyCtrl<Gpi, Double> halfWavePlateAngleValueCtrl;
    private final JLabel halfWavePlateAngleValueUnits;

    private final ComboPropertyCtrl<Gpi, Gpi.Filter> filterCtrl;

    private final ComboPropertyCtrl<Gpi, Gpi.Shutter> entranceShutterCtrl;
    private final ComboPropertyCtrl<Gpi, Gpi.Shutter> scienceArmShutterCtrl;
    private final ComboPropertyCtrl<Gpi, Gpi.Shutter> calEntranceShutterCtrl;
    private final ComboPropertyCtrl<Gpi, Gpi.Shutter> referenceArmShutterCtrl;

    private final ComboPropertyCtrl<Gpi, Gpi.Apodizer> apodizerCtrl;
    private final ComboPropertyCtrl<Gpi, Gpi.Lyot> lyotCtrl;

    private final JLabel artificialSourceLabel;
    private final CheckboxPropertyCtrl<Gpi> irLaserLampCtrl;
    private final CheckboxPropertyCtrl<Gpi> visibleLaserLampCtrl;
    private final CheckboxPropertyCtrl<Gpi> superContinuumLampCtrl;
    private final ArtificialSourceEnabler artificialSourceEnabler = new ArtificialSourceEnabler();
    private final AttenuationMessageUpdater attenuationMessageUpdater;

    private final JLabel attenuationWarning;

    private final JLabel attenuationLabel;
    private final TextFieldPropertyCtrl<Gpi, Double> attenuationCtrl;
    private final JLabel attenuationUnitsLabel;

    private final ComboPropertyCtrl<Gpi, Gpi.PupilCamera> pupilCameraCtrl;
    private final ComboPropertyCtrl<Gpi, Gpi.FPM> fpmCtrl;
    private final CheckboxPropertyCtrl<Gpi> alwaysRestoreModelCtrl;

    private final CheckboxPropertyCtrl<Gpi> useAoCtrl;
    private final CheckboxPropertyCtrl<Gpi> useCalCtrl;
    private final UseLoopListener useCalUpdater;
    private final UseLoopListener useAoUpdater;

    private final CheckboxPropertyCtrl<Gpi> aoOptimizeCtrl;
    private final CheckboxPropertyCtrl<Gpi> noFpmPinholeCtrl;
    private final UseLoopListener aoOptimizeUpdater;
    private final UseLoopListener noFpmPinholeUpdater;

    private static final int halfWidth     = 3;

    private static final int leftLabelCol  = 0;
    private static final int leftWidgetCol = 1;
    private static final int leftUnitsCol  = 2;
    private static final int gapCol        = 3;
    private static final int rightLabelCol = 4;
    private static final int rightWidgetCol= 5;
    private static final int rightUnitsCol = 6;
    private static final int colCount      = rightUnitsCol + 1;


    public GpiEditor() {
        pan = new JPanel(new GridBagLayout());
        pan.setBorder(PANEL_BORDER);

        int row = 0;
        final GridBagConstraints gbc;

        // Column Gap
        pan.add(new JPanel(), colGapGbc(gapCol, row));

        // Astrometric field
        astrometricFieldCtrl = new CheckboxPropertyCtrl<>(Gpi.ASTROMETRIC_FIELD_PROP);
        pan.add(astrometricFieldCtrl.getComponent(), propWidgetGbc(leftWidgetCol, row));

        ++row;

        // Observing Mode
        observingModeCtrl = ComboPropertyCtrl.optionEnumInstance(Gpi.OBSERVING_MODE_PROP, Gpi.ObservingMode.class);
        // See OT-102
        ((JComboBox) observingModeCtrl.getComponent()).setModel(
                new DefaultComboBoxModel<>(Gpi.ObservingMode.NONENGINEERING_VALUES));

        addCtrl(pan, leftLabelCol, row, observingModeCtrl);

        // Observing Mode warning
        final JLabel observingModeWarning = new JLabel("");
        observingModeWarning.setHorizontalAlignment(JLabel.LEFT);
        observingModeMessageUpdater = new ObservingModeMessageUpdater(observingModeWarning);
        pan.add(observingModeWarning, warningLabelGbc(rightLabelCol, row, halfWidth));

        ++row;

        // ----------------------------------------------------------------
        pan.add(new JSeparator(), separatorGbc(leftLabelCol, row, colCount));
        // ----------------------------------------------------------------

        ++row;

        // Disperser
        disperserCtrl = ComboPropertyCtrl.enumInstance(Gpi.DISPERSER_PROP);
        // See OT-50: Disperser.OPEN only available in engineering screen
        ((JComboBox<Gpi.Disperser>)disperserCtrl.getComponent()).setModel(new DefaultComboBoxModel<>(Gpi.Disperser.nonEngineeringValues()));
        addCtrl(pan, leftLabelCol, row, disperserCtrl);

        // ADC
        adcCtrl = ComboPropertyCtrl.enumInstance(Gpi.ADC_PROP);
        addCtrl(pan, rightLabelCol, row, adcCtrl);

        ++row;

        // Half Wave Plate Angle manual entry
        PropertyDescriptor pd = Gpi.HALF_WAVE_PLATE_ANGLE_VALUE_PROP;
        halfWavePlateAngleValueCtrl = TextFieldPropertyCtrl.createDoubleInstance(pd, 1);
        halfWavePlateAngleValueLabel = new JLabel("Value");
        pan.add(halfWavePlateAngleValueLabel, propLabelGbc(leftLabelCol, row));

        gbc = propWidgetGbc(leftWidgetCol, row);
        pan.add(halfWavePlateAngleValueCtrl.getComponent(), gbc);
        pan.add(halfWavePlateAngleValueUnits = new JLabel("deg"), propUnitsGbc(leftUnitsCol, row));

        ++row;

        // Half Wave Plate Angle error message
        final JLabel halfWavePlateAngleWarning = new JLabel("");
        halfWavePlateAngleMessageUpdater = new HalfWavePlateAngleMessageUpdater(halfWavePlateAngleWarning);
        pan.add(halfWavePlateAngleWarning, warningLabelGbc(rightLabelCol, row, halfWidth));

        ++row;

        // ----------------------------------------------------------------
        pan.add(new JSeparator(), separatorGbc(leftLabelCol, row, colCount));
        // ----------------------------------------------------------------

        ++row;

        // Exposure Time
        pd = Gpi.EXPOSURE_TIME_PROP;
        expTimeCtrl = TextFieldPropertyCtrl.createDoubleInstance(pd, 1);
        expTimeCtrl.setColumns(6);
        pan.add(new JLabel("Exp Time"), propLabelGbc(leftLabelCol, row));
        pan.add(expTimeCtrl.getComponent(), propWidgetGbc(leftWidgetCol, row));
        pan.add(new JLabel("sec"), propUnitsGbc(leftUnitsCol, row));

        // Coadds
        pd = Gpi.COADDS_PROP;
        coaddsCtrl = TextFieldPropertyCtrl.createIntegerInstance(pd);
        coaddsCtrl.setColumns(6);
        pan.add(new JLabel("Coadds"), propLabelGbc(rightLabelCol, row));
        pan.add(coaddsCtrl.getComponent(), propWidgetGbc(rightWidgetCol, row));
        pan.add(new JLabel("exp/obs"), propUnitsGbc(rightUnitsCol, row));

        ++row;

        // Exposure Time warning
        final JLabel exposureTimeWarning = new JLabel("");
        exposureTimeMessageUpdater = new ExposureTimeMessageUpdater(exposureTimeWarning);
        pan.add(exposureTimeWarning, warningLabelGbc(leftLabelCol, row, halfWidth));

        row += 2;

        // Filler
        pan.add(new JPanel(), pushGbc(colCount, row));

       // Engineering controls
        filterCtrl = ComboPropertyCtrl.enumInstance(Gpi.FILTER_PROP);

        entranceShutterCtrl = ComboPropertyCtrl.enumInstance(Gpi.ENTRANCE_SHUTTER_PROP);
        scienceArmShutterCtrl = ComboPropertyCtrl.enumInstance(Gpi.SCIENCE_ARM_SHUTTER_PROP);
        calEntranceShutterCtrl = ComboPropertyCtrl.enumInstance(Gpi.CAL_ENTRANCE_SHUTTER_PROP);
        referenceArmShutterCtrl = ComboPropertyCtrl.enumInstance(Gpi.REFERENCE_ARM_SHUTTER_PROP);

        apodizerCtrl = new ComboPropertyCtrl<>(Gpi.APODIZER_PROP, Gpi.Apodizer.validValues());
        lyotCtrl = ComboPropertyCtrl.enumInstance(Gpi.LYOT_PROP);

        artificialSourceLabel = new JLabel("                   Artificial Source");
        irLaserLampCtrl = new CheckboxPropertyCtrl<>(Gpi.IR_LASER_LAMP_PROP);
        visibleLaserLampCtrl = new CheckboxPropertyCtrl<>(Gpi.VISIBLE_LASER_LAMP_PROP);
        superContinuumLampCtrl = new CheckboxPropertyCtrl<>(Gpi.SUPER_CONTINUUM_LAMP_PROP);

        attenuationCtrl = TextFieldPropertyCtrl.createDoubleInstance(Gpi.ARTIFICIAL_SOURCE_ATTENUATION_PROP, 1);
        attenuationLabel = new JLabel(attenuationCtrl.getDescriptor().getDisplayName());
        attenuationUnitsLabel = new JLabel("dB");

        // Artificial source attenuation warning
        attenuationWarning = new JLabel("");
        attenuationMessageUpdater = new AttenuationMessageUpdater(attenuationWarning);

        pupilCameraCtrl = ComboPropertyCtrl.enumInstance(Gpi.PUPUL_CAMERA_PROP);
        fpmCtrl = ComboPropertyCtrl.enumInstance(Gpi.FPM_PROP);

        alwaysRestoreModelCtrl = new CheckboxPropertyCtrl<>(Gpi.ALWAYS_RESTORE_MODEL_PROP);

        useAoCtrl = new CheckboxPropertyCtrl<>(Gpi.USE_AO_PROP);
        useAoUpdater = new UseLoopListener(useAoCtrl);

        useCalCtrl = new CheckboxPropertyCtrl<>(Gpi.USE_CAL_PROP);
        useCalUpdater = new UseLoopListener(useCalCtrl);

        aoOptimizeCtrl = new CheckboxPropertyCtrl<>(Gpi.AO_OPTIMIZE_PROP);
        aoOptimizeUpdater = new UseLoopListener(aoOptimizeCtrl);

        noFpmPinholeCtrl = new CheckboxPropertyCtrl<>(Gpi.ALIGN_FPM_PINHOLE_BIAS_PROP);
        noFpmPinholeUpdater = new UseLoopListener(noFpmPinholeCtrl);
    }

    @Override
    public Component getEngineeringComponent() {
        final JPanel pan = new JPanel(new GridBagLayout());
        int row = 0;

        // Column Gap
        pan.add(new JPanel(), colGapGbc(gapCol, row));

        addCtrl(pan, leftLabelCol, row, filterCtrl);
        ((JCheckBox)alwaysRestoreModelCtrl.getComponent()).setHorizontalAlignment(JCheckBox.CENTER);
        pan.add(alwaysRestoreModelCtrl.getComponent(), propWidgetGbc(rightLabelCol, row++, 3, 1));

        // ----------------------------------------------------------------
        pan.add(new JSeparator(), separatorGbc(leftLabelCol, row++, colCount));
        // ----------------------------------------------------------------

        addCtrl(pan, leftLabelCol, row++, entranceShutterCtrl);
        addCtrl(pan, leftLabelCol, row++, scienceArmShutterCtrl);
        addCtrl(pan, leftLabelCol, row++, referenceArmShutterCtrl);
        row++;
        addCtrl(pan, leftLabelCol, row++, calEntranceShutterCtrl);
        row++;

        // right column: artificial source and cal source
        int r = row - 6;
        pan.add(artificialSourceLabel, propLabelGbc(rightLabelCol, r, 1, 3));
        JPanel artificialSourcePanel = new JPanel(new GridBagLayout());
        artificialSourcePanel.add(irLaserLampCtrl.getComponent(), propWidgetGbc(rightLabelCol, 0));
        artificialSourcePanel.add(visibleLaserLampCtrl.getComponent(), propWidgetGbc(rightLabelCol, 1));
        artificialSourcePanel.add(superContinuumLampCtrl.getComponent(), propWidgetGbc(rightLabelCol, 2));
        pan.add(artificialSourcePanel, propLabelGbc(rightWidgetCol, r, 1, 3));
        r += 4;
        pan.add(attenuationLabel, propLabelGbc(rightLabelCol, r));
        pan.add(attenuationCtrl.getComponent(), propWidgetGbc(rightWidgetCol, r));
        pan.add(attenuationUnitsLabel, propUnitsGbc(rightUnitsCol, r++));
        pan.add(attenuationWarning, warningLabelGbc(rightLabelCol, r++, halfWidth));

        // ----------------------------------------------------------------
        pan.add(new JSeparator(), separatorGbc(leftLabelCol, row++, colCount));
        // ----------------------------------------------------------------

        addCtrl(pan, leftLabelCol, row++, apodizerCtrl);
        addCtrl(pan, leftLabelCol, row++, fpmCtrl);
        addCtrl(pan, leftLabelCol, row++, lyotCtrl);
        addCtrl(pan, leftLabelCol, row++, pupilCameraCtrl);

        // ----------------------------------------------------------------
        pan.add(new JSeparator(), separatorGbc(leftLabelCol, row++, colCount));
        // ----------------------------------------------------------------

        pan.add(useAoCtrl.getComponent(), propWidgetGbc(leftWidgetCol, row++, 3, 1));
        pan.add(useCalCtrl.getComponent(), propWidgetGbc(leftWidgetCol, row++, 3, 1));

        pan.add(aoOptimizeCtrl.getComponent(), propWidgetGbc(leftWidgetCol, row++, 3, 1));
        pan.add(noFpmPinholeCtrl.getComponent(), propWidgetGbc(leftWidgetCol, row++, 3, 1));

        // filler
        final int finalRow = row;
        pan.add(new JPanel(), new GridBagConstraints() {{
             gridx=0; gridy=finalRow; weighty=1.0; fill=VERTICAL;
        }});

        return pan;
    }

    @Override
    public JPanel getWindow() {
        return pan;
    }

    @Override
    public void handlePreDataObjectUpdate(Gpi inst) {
        if (inst == null) return;
        inst.removePropertyChangeListener(Gpi.DISPERSER_PROP.getName(), halfWavePlateAngleManualEnabler);
        inst.removePropertyChangeListener(Gpi.IR_LASER_LAMP_PROP.getName(), artificialSourceEnabler);
        inst.removePropertyChangeListener(Gpi.VISIBLE_LASER_LAMP_PROP.getName(), artificialSourceEnabler);
        inst.removePropertyChangeListener(Gpi.SUPER_CONTINUUM_LAMP_PROP.getName(), artificialSourceEnabler);
        inst.removePropertyChangeListener(Gpi.ARTIFICIAL_SOURCE_ATTENUATION_PROP.getName(), attenuationMessageUpdater);

        inst.removePropertyChangeListener(Gpi.USE_AO_PROP.getName(), attenuationMessageUpdater);

        inst.removePropertyChangeListener(Gpi.EXPOSURE_TIME_PROP.getName(), exposureTimeMessageUpdater);
        inst.removePropertyChangeListener(Gpi.COADDS_PROP.getName(), exposureTimeMessageUpdater);
        inst.removePropertyChangeListener(Gpi.DETECTOR_READOUT_AREA_PROP.getName(), exposureTimeMessageUpdater);
        inst.removePropertyChangeListener(Gpi.READOUT_AREA_PROP.getName(), exposureTimeMessageUpdater);

        inst.removePropertyChangeListener(Gpi.OBSERVING_MODE_PROP.getName(), observingModeMessageUpdater);
        inst.removePropertyChangeListener(Gpi.FILTER_PROP.getName(), observingModeMessageUpdater);
        inst.removePropertyChangeListener(Gpi.APODIZER_PROP.getName(), observingModeMessageUpdater);
        inst.removePropertyChangeListener(Gpi.FPM_PROP.getName(), observingModeMessageUpdater);
        inst.removePropertyChangeListener(Gpi.LYOT_PROP.getName(), observingModeMessageUpdater);

        inst.removePropertyChangeListener(Gpi.HALF_WAVE_PLATE_ANGLE_VALUE_PROP.getName(), halfWavePlateAngleMessageUpdater);
        inst.removePropertyChangeListener(Gpi.DISPERSER_PROP.getName(), halfWavePlateAngleMessageUpdater);
    }

    @Override
    public void handlePostDataObjectUpdate(Gpi inst) {
        disperserCtrl.setBean(inst);
        adcCtrl.setBean(inst);
        observingModeCtrl.setBean(inst);

        coaddsCtrl.setBean(inst);
        expTimeCtrl.setBean(inst);
        astrometricFieldCtrl.setBean(inst);

        halfWavePlateAngleValueCtrl.setBean(inst);

        inst.addPropertyChangeListener(Gpi.DISPERSER_PROP.getName(), halfWavePlateAngleManualEnabler);
        halfWavePlateAngleManualEnabler.update(inst);

        inst.addPropertyChangeListener(Gpi.IR_LASER_LAMP_PROP.getName(), artificialSourceEnabler);
        inst.addPropertyChangeListener(Gpi.VISIBLE_LASER_LAMP_PROP.getName(), artificialSourceEnabler);
        inst.addPropertyChangeListener(Gpi.SUPER_CONTINUUM_LAMP_PROP.getName(), artificialSourceEnabler);
        artificialSourceEnabler.update(inst);
        inst.addPropertyChangeListener(Gpi.ARTIFICIAL_SOURCE_ATTENUATION_PROP.getName(), attenuationMessageUpdater);
        attenuationMessageUpdater.update();

        inst.addPropertyChangeListener(Gpi.EXPOSURE_TIME_PROP.getName(), exposureTimeMessageUpdater);
        inst.addPropertyChangeListener(Gpi.COADDS_PROP.getName(), exposureTimeMessageUpdater);
        inst.addPropertyChangeListener(Gpi.DETECTOR_READOUT_AREA_PROP.getName(), exposureTimeMessageUpdater);
        inst.addPropertyChangeListener(Gpi.READOUT_AREA_PROP.getName(), exposureTimeMessageUpdater);
        exposureTimeMessageUpdater.update();

        inst.addPropertyChangeListener(Gpi.OBSERVING_MODE_PROP.getName(), observingModeMessageUpdater);
        inst.addPropertyChangeListener(Gpi.FILTER_PROP.getName(), observingModeMessageUpdater);
        inst.addPropertyChangeListener(Gpi.APODIZER_PROP.getName(), observingModeMessageUpdater);
        inst.addPropertyChangeListener(Gpi.FPM_PROP.getName(), observingModeMessageUpdater);
        inst.addPropertyChangeListener(Gpi.LYOT_PROP.getName(), observingModeMessageUpdater);
        observingModeMessageUpdater.update();

        inst.addPropertyChangeListener(Gpi.HALF_WAVE_PLATE_ANGLE_VALUE_PROP.getName(), halfWavePlateAngleMessageUpdater);
        inst.addPropertyChangeListener(Gpi.DISPERSER_PROP.getName(), halfWavePlateAngleMessageUpdater);
        halfWavePlateAngleMessageUpdater.update();

        filterCtrl.setBean(inst);

        entranceShutterCtrl.setBean(inst);
        scienceArmShutterCtrl.setBean(inst);
        calEntranceShutterCtrl.setBean(inst);
        referenceArmShutterCtrl.setBean(inst);

        apodizerCtrl.setBean(inst);
        lyotCtrl.setBean(inst);

        irLaserLampCtrl.setBean(inst);
        visibleLaserLampCtrl.setBean(inst);
        superContinuumLampCtrl.setBean(inst);
        attenuationCtrl.setBean(inst);

        pupilCameraCtrl.setBean(inst);

        fpmCtrl.setBean(inst);
        alwaysRestoreModelCtrl.setBean(inst);
        useAoCtrl.setBean(inst);
        inst.addPropertyChangeListener(Gpi.USE_CAL_PROP.getName(), useCalUpdater);
        inst.addPropertyChangeListener(Gpi.USE_AO_PROP.getName(), useAoUpdater);
        useCalCtrl.setBean(inst);

        aoOptimizeCtrl.setBean(inst);
        noFpmPinholeCtrl.setBean(inst);
        inst.addPropertyChangeListener(Gpi.AO_OPTIMIZE_PROP.getName(), aoOptimizeUpdater);
        inst.addPropertyChangeListener(Gpi.ALIGN_FPM_PINHOLE_BIAS_PROP.getName(), noFpmPinholeUpdater);
    }
}
