package jsky.app.ot.gemini.flamingos2;

import com.jgoodies.forms.factories.DefaultComponentFactory;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.gui.ThinBorder;
import edu.gemini.shared.gui.bean.*;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.YesNoType;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.telescope.IssPort;
import edu.gemini.spModel.type.SpTypeUtil;
import jsky.app.ot.OTOptions;
import jsky.app.ot.editor.eng.EngEditor;
import jsky.app.ot.gemini.editor.ComponentEditor;
import jsky.app.ot.gemini.parallacticangle.PositionAnglePanel;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;

/**
 * User interface and editor for Flamingos 2.
 *
 * (Work in progress: slated to replace EdCompInstFlamingos2, Flamingos2Form, and Flamingos2Form.jfd)
 */
public class Flamingos2Editor extends ComponentEditor<ISPObsComponent, Flamingos2> implements EngEditor {
    private final PropertyChangeListener updateParallacticAnglePCL = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            posAnglePanel.updateParallacticControls();
        }
    };
    private final PropertyChangeListener updateUnboundedAnglePCL = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            posAnglePanel.updateUnboundedControls();
        }
    };

    private final class CustomMdfEnabler implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            update((Flamingos2) evt.getSource());
        }

        void update(Flamingos2 inst) {
            final boolean visible = inst.getFpu() == Flamingos2.FPUnit.CUSTOM_MASK;
            mdfLabel.setVisible(visible);
            mdfCtrl.getComponent().setVisible(visible);
            mdfCtrl.getComponent().setEnabled(visible);
            slitWidthLabel.setVisible(visible);
            customSlitCtrl.getComponent().setVisible(visible);
            customSlitCtrl.getComponent().setEnabled(visible);
        }
    }

    private final class ExposureTimeMessageUpdater implements EditListener<Flamingos2, Double>, PropertyChangeListener {
        private final JLabel label;

        ExposureTimeMessageUpdater(JLabel label) {
            this.label = label;
        }

        public void valueChanged(EditEvent<Flamingos2, Double> event) {
            update(event.getNewValue());
        }

        public void propertyChange(PropertyChangeEvent evt) {
            update();
        }

        void update() {
            final Flamingos2 flam2 = getDataObject();
            update((flam2 == null) ? null : flam2.getExposureTime());
        }

        void update(Double val) {
            final Flamingos2 flam2 = getDataObject();
            Color fg = Color.black;
            String txt = "";
            if ((flam2 != null) && (val != null)) {
                final double min = flam2.getMinimumExposureTimeSecs();
                final double rec = flam2.getRecommendedExposureTimeSecs();

                if (val < min) {
                    fg = FATAL_FG_COLOR;
                    txt = String.format("Below minimum (%.1f sec).", min);
                } else if ((val > Flamingos2.FRACTIONAL_EXP_TIME_MAX) && (val != Math.floor(val))) {
                    fg = FATAL_FG_COLOR;
                    txt = "Millisec precision not supported over " + Flamingos2.FRACTIONAL_EXP_TIME_MAX + " sec.";
                } else if (val < rec) {
                    fg = WARNING_FG_COLOR;
                    final String formatStr = (rec < Flamingos2.FRACTIONAL_EXP_TIME_MAX) ? "%.1f" : "%.0f";
                    txt = String.format("Below recommendation (" + formatStr + " sec).", rec);
                }
            }

            label.setText(txt);
            label.setForeground(fg);
        }
    }

    private class PreImagingChangeListener implements ItemListener, PropertyChangeListener {
        private static final String WARNING =
               "Selecting MOS pre-imaging will set the Focal Plane Unit to\n" +
               "'Imaging' and the Disperser to 'None'.";

        public void propertyChange(PropertyChangeEvent evt) {
            preImaging.removeItemListener(this);
            preImaging.setSelected(getDataObject().getMosPreimaging() == YesNoType.YES);
            preImaging.addItemListener(this);
        }

        public void itemStateChanged(ItemEvent e) {
            final JCheckBox cb = (JCheckBox) e.getSource();

            final Flamingos2 inst = getDataObject();

            final boolean selected = cb.isSelected();

            if (selected && ((inst.getFpu() != Flamingos2.FPUnit.FPU_NONE) ||
                (inst.getDisperser() != Flamingos2.Disperser.NONE))) {

                final int res = JOptionPane.showOptionDialog(pan, WARNING,
                        "Set MOS pre-imaging?", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null,
                        new String[] {"Set MOS pre-imaging", "Cancel"},
                        "Cancel");

                if (res != 0) {
                    preImaging.removeItemListener(this);
                    preImaging.setSelected(false);
                    preImaging.addItemListener(this);
                    return;
                }
            }

            inst.removePropertyChangeListener(this);
            inst.setMosPreimaging(selected ? YesNoType.YES : YesNoType.NO);
            inst.addPropertyChangeListener(this);
        }
    }

    private final class MessagePanel extends JPanel implements PropertyChangeListener {
        private class PropValueLabel extends JLabel {
            PropValueLabel() {
                setForeground(Color.black);
            }
        }

        private final JLabel readsLabel           = new PropValueLabel();
        private final JLabel readsWarning         = new JLabel();
        private final JLabel readNoiseLabel       = new PropValueLabel();
        private final JLabel recExposureTimeLabel = new PropValueLabel();
        private final JLabel minExposureTimeLabel = new PropValueLabel();

        private final JLabel scalePropertyLabel   = new JLabel();
        private final JLabel scaleLabel           = new PropValueLabel();
        private final JLabel scaleUnitsLabel      = new JLabel();
        private final JLabel fovLabel             = new PropValueLabel();
        private final JLabel fovUnitsLabel        = new JLabel();
        private final JLabel modeLabel            = new JLabel();

        MessagePanel() {
            super(new GridBagLayout());

            final Border b = new ThinBorder(BevelBorder.RAISED);
            setBorder(BorderFactory.createCompoundBorder(b, BorderFactory.createEmptyBorder(5, 15, 5, 5)));

            setBackground(INFO_BG_COLOR);

            int row = 0;
            readsWarning.setText(" *See Engineering Tab");
            readsWarning.setVisible(false);
            addProperty(row++, "Reads:",         readsLabel, readsWarning);
            addProperty(row++, "Read Noise:",    readNoiseLabel);
            addProperty(row++, "Exposure Time:", recExposureTimeLabel, new JLabel("sec (recommended)"), minExposureTimeLabel, new JLabel("sec (min)"));

            final int sepRow = row++;
            add(new JSeparator(), new GridBagConstraints(){{
                gridy     = sepRow;
                gridwidth = 3;
                weightx   = 1.0;
                fill      = HORIZONTAL;
                insets    = new Insets(3, 0, 1, 0);
            }});

            addProperty(row++, scalePropertyLabel, scaleLabel, scaleUnitsLabel);
            addProperty(row,   "Science FOV:",     fovLabel, fovUnitsLabel, modeLabel);

            // Push all to the left.
            final GridBagConstraints gbc = new GridBagConstraints() {{
                gridx = 2; weightx = 1.0; fill = HORIZONTAL;
            }};
            add(new JPanel() {{setOpaque(false);}}, gbc);
        }

        private void addProperty(int row, String propertyNameLabel, JLabel... propertyValueLabels) {
            addProperty(row, new JLabel(propertyNameLabel), propertyValueLabels);
        }

        private void addProperty(int row, JLabel propertyNameLabel, JLabel... propertyValueLabels) {
            final GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.fill  = GridBagConstraints.NONE;

            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(row == 0 ? 0 : 2, 0, 0, 0);
            add(propertyNameLabel, gbc);

            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(row == 0 ? 0 : 2, 7, 0, 0);
            ++gbc.gridx;
            add(createPropertyValuePanel(propertyValueLabels), gbc);
        }

        private JPanel createPropertyValuePanel(JLabel... propertyValueLabels) {
            final JPanel pan = new JPanel(new GridBagLayout()) {{ setOpaque(false); }};

            final GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx  = 0;
            gbc.insets = new Insets(0, 0, 0, 0);
            pan.add(propertyValueLabels[0], gbc);

            for (int i=1; i<propertyValueLabels.length; ++i) {
                ++gbc.gridx;
                gbc.insets = new Insets(0, (i%2==1) ? 3 : 7, 0, 0);
                pan.add(propertyValueLabels[i], gbc);
            }
            return pan;
        }

        public void propertyChange(PropertyChangeEvent evt) {
            update();
        }

        void update() {
            final Flamingos2 inst = getDataObject();
            if (inst == null) return;

            final Flamingos2.ReadMode readMode = inst.getReadMode();
            int reads = readMode.readCount();

            boolean overridden = false;
            final Option<Flamingos2.Reads> engReads = inst.getReads();
            if (!None.instance().equals(engReads)) {
                if (reads != engReads.getValue().getCount()) {
                    overridden = true;
                    reads = engReads.getValue().getCount();
                }
            }

            readsLabel.setText(String.valueOf(reads));
            if (overridden) {
                readsLabel.setFont(readsLabel.getFont().deriveFont(Font.BOLD | Font.ITALIC));
            } else {
                readsLabel.setFont(readsLabel.getFont().deriveFont(Font.PLAIN));
            }
            readsWarning.setVisible(overridden);


            readNoiseLabel.setText(readMode.formatReadNoise());

            String time = String.format("> %.0f", readMode.recomendedExpTimeSec());
            recExposureTimeLabel.setText(time);

            time = String.format("%.1f", readMode.minimumExpTimeSec());
            minExposureTimeLabel.setText(time);

            // -------

            final Flamingos2.FPUnit fpu = inst.getFpu();

            int prec = 2;
            final String scaleProperty = "Pixel Scale";
            final double scale = inst.getLyotWheel().getPixelScale();
            String units = "arcsec/pixel";
            scalePropertyLabel.setText(scaleProperty);
            if (scale == 0.0) {
                scaleLabel.setText("na");
            } else {
                scaleLabel.setText(String.format("%."+prec+"f", scale));
            }
            scaleUnitsLabel.setText(units);

            final double[] sciArea  = inst.getScienceArea();
            String sciAreaTxt = "na";
            units = "";
            if ((sciArea[0] > 0) || (sciArea[1] > 0)) {
                prec = (fpu.isLongslit()) ? 2 : 0;
                sciAreaTxt = String.format("%."+prec+"f x %.0f", sciArea[0], sciArea[1]);
                units = "arcsec";
            }

            String mode = "";
            if (inst.getLyotWheel().getPlateScale() == 0) {
                mode = "(Focusing)";
            } else if (fpu == Flamingos2.FPUnit.FPU_NONE) {
                mode = "(Imaging)";
            } else if (fpu == Flamingos2.FPUnit.CUSTOM_MASK) {
                mode = "(Custom Mask)";
            } else if (fpu.isLongslit()) {
                mode = "(Spectroscopy)";
            }

            fovLabel.setText(sciAreaTxt);
            fovUnitsLabel.setText(units);
            modeLabel.setText(mode);

            // TODO: DO we need to do anything for parallactic angle here?
        }
    }

    private final JPanel pan;

    private final ComboPropertyCtrl<Flamingos2, Flamingos2.FPUnit> fpuCtrl;

    private final JLabel mdfLabel;
    private final TextFieldPropertyCtrl<Flamingos2, String> mdfCtrl;
    private final JLabel slitWidthLabel;
    private final ComboPropertyCtrl<Flamingos2, Flamingos2.CustomSlitWidth> customSlitCtrl;

    private final JCheckBox preImaging;

    private final TextFieldPropertyCtrl<Flamingos2, Double> expTimeCtrl;

    private final ComboPropertyCtrl<Flamingos2, Flamingos2.Filter> filterCtrl;
    private final ComboPropertyCtrl<Flamingos2, Flamingos2.Disperser> disperserCtrl;
    private final ComboPropertyCtrl<Flamingos2, Flamingos2.LyotWheel> lyotCtrl;

    private final PositionAnglePanel<Flamingos2, Flamingos2Editor> posAnglePanel;

    private final CustomMdfEnabler customMdfEnabler = new CustomMdfEnabler();
    private final ExposureTimeMessageUpdater exposureTimeMessageUpdater;
    private final PreImagingChangeListener preImagingListener;

    private final RadioPropertyCtrl<Flamingos2, Flamingos2.ReadMode> readModeCtrl;
    private final RadioPropertyCtrl<Flamingos2, IssPort> portCtrl;

    private final MessagePanel messagePanel;

    private final ComboPropertyCtrl<Flamingos2, Option<Flamingos2.WindowCover>> windowCoverCtrl;
    private final ComboPropertyCtrl<Flamingos2, Flamingos2.Decker> deckerCtrl;
    private final ComboPropertyCtrl<Flamingos2, Option<Flamingos2.ReadoutMode>> readoutModeCtrl;
    private final ComboPropertyCtrl<Flamingos2, Option<Flamingos2.Reads>> readsCtrl;

    private final CheckboxPropertyCtrl<Flamingos2> eOffsetCtrl;

    private static final int leftLabelCol        = 0;
    private static final int leftWidgetCol       = 1;
    private static final int leftUnitsCol        = 2;
    private static final int leftGapCol          = 3;

    private static final int centerWidgetCol     = 4;
    private static final int centerGapCol        = 5;

    private static final int rightLabelCol       = 6;
    private static final int rightWidgetCol      = 7;
    private static final int rightUnitsCol       = 8;

    private static final int leftWidth           = 3;
    private static final int centerWidth         = 1;
    private static final int rightWidth          = 3;
    private static final int centerAndRightWidth = centerWidth + rightWidth + 1;
    private static final int colCount            = rightUnitsCol + 1;


    public Flamingos2Editor() {
        pan = new JPanel(new GridBagLayout());
        pan.setBorder(PANEL_BORDER);


        int row = 0;
        GridBagConstraints gbc;

        // Column gaps
        pan.add(new JPanel(), colGapGbc(leftGapCol,   row));
        pan.add(new JPanel(), colGapGbc(centerGapCol, row));

        // FPU and mask: takes up two rows to accommodate the Custom MDF.
        fpuCtrl = ComboPropertyCtrl.enumInstance(Flamingos2.FPU_PROP);
        addCtrl(pan, leftLabelCol, row, fpuCtrl, null);

        // FPU mask.
        PropertyDescriptor pd = Flamingos2.FPU_MASK_PROP;
        mdfCtrl = TextFieldPropertyCtrl.createStringInstance(pd);
        customSlitCtrl = ComboPropertyCtrl.enumInstance(Flamingos2.CUSTOM_SLIT_WIDTH_PROP);

        mdfLabel = new JLabel("Custom MDF");
        pan.add(mdfLabel, propLabelGbc(leftLabelCol, row+1));
        gbc = propWidgetGbc(leftWidgetCol, row+1);
        pan.add(mdfCtrl.getComponent(), gbc);

        slitWidthLabel = new JLabel("Slit Width");
        pan.add(slitWidthLabel, propLabelGbc(leftLabelCol, row+2));
        gbc = propWidgetGbc(leftWidgetCol, row+2);
        pan.add(customSlitCtrl.getComponent(), gbc);

        // MOS pre-imaging: takes up two rows to accommodate the Custom MDF and exposure time warning.
        preImaging = new JCheckBox("MOS pre-imaging");
        preImagingListener = new PreImagingChangeListener();
        preImaging.addItemListener(preImagingListener);
        gbc = propWidgetGbc(centerWidgetCol, row); gbc.gridwidth = centerWidth;
        pan.add(preImaging, gbc);

        // Exposure Time
        pd = Flamingos2.EXPOSURE_TIME_PROP;
        final JLabel exposureTimeWarning = new JLabel("");
        exposureTimeMessageUpdater = new ExposureTimeMessageUpdater(exposureTimeWarning);
        expTimeCtrl = TextFieldPropertyCtrl.createDoubleInstance(pd, 1);
        expTimeCtrl.setColumns(6);
        expTimeCtrl.addEditListener(exposureTimeMessageUpdater);

        pan.add(new JLabel("Exp Time"), propLabelGbc(rightLabelCol, row));
        pan.add(expTimeCtrl.getComponent(), propWidgetGbc(rightWidgetCol, row));
        pan.add(new JLabel("sec"), propUnitsGbc(rightUnitsCol, row));

        pan.add(exposureTimeWarning, warningLabelGbc(rightLabelCol, row+1, rightWidth));


        // Increment the row by 3 since previous widgets were allotted for three rows.
        row += 3;

        // -------- SEPARATORS --------
        pan.add(new JSeparator(), separatorGbc(leftLabelCol, row, leftWidth));
        final JComponent posAngleSeparator = DefaultComponentFactory.getInstance().createSeparator("Position Angle");
        pan.add(posAngleSeparator, separatorGbc(centerWidgetCol, row, centerAndRightWidth));
        // ----------------------------

        ++row;

        // REL-3031: changed so that obsolete items are respected.
        // filterCtrl = ComboPropertyCtrl.enumInstance(Flamingos2.FILTER_PROP);
        filterCtrl = new ComboPropertyCtrl(
                Flamingos2.FILTER_PROP,
                SpTypeUtil.getSelectableItems((Class<Flamingos2.Filter>) Flamingos2.FILTER_PROP.getPropertyType()).toArray());
        addCtrl(pan, leftLabelCol, row, filterCtrl);

        // Position Angle
        posAnglePanel = PositionAnglePanel.apply(SPComponentType.INSTRUMENT_FLAMINGOS2);
        gbc = propWidgetGbc(centerWidgetCol, row, centerAndRightWidth, 2);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 5, 0, 0);
        pan.add(posAnglePanel.peer(), gbc);
        ++row;

        // REL-525: changed so that obsolete items are respected.
        // lyotCtrl = ComboPropertyCtrl.enumInstance(Flamingos2.LYOT_WHEEL_PROP);
        lyotCtrl = new ComboPropertyCtrl(
                Flamingos2.LYOT_WHEEL_PROP,
                SpTypeUtil.getSelectableItems((Class<Flamingos2.LyotWheel>) Flamingos2.LYOT_WHEEL_PROP.getPropertyType()).toArray());
        addCtrl(pan, leftLabelCol, row, lyotCtrl);
        ++row;

        // Disperser falls into next row underneath.
        disperserCtrl = ComboPropertyCtrl.enumInstance(Flamingos2.DISPERSER_PROP);
        gbc = propLabelGbc(leftLabelCol, row);
        pan.add(new JLabel(Flamingos2.DISPERSER_PROP.getDisplayName()), gbc);
        gbc = propWidgetGbc(leftWidgetCol, row);
        pan.add(disperserCtrl.getComponent(), gbc);
        ++row;

        final JTabbedPane tabPane = new JTabbedPane();

        readModeCtrl = new RadioPropertyCtrl<>(Flamingos2.READMODE_PROP);
        portCtrl     = new RadioPropertyCtrl<>(Flamingos2.PORT_PROP);

        tabPane.addTab("Read Mode", getTabPanel(readModeCtrl.getComponent()));
        tabPane.addTab("ISS Port", getTabPanel(portCtrl.getComponent()));

        gbc = new GridBagConstraints();
        gbc.gridx     = 0;        gbc.gridy      = row;
        gbc.gridwidth = colCount; gbc.gridheight = 1;
        gbc.weightx   = 1.0;      gbc.weighty    = 0;
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(PROPERTY_ROW_GAP, 0, 0, 0);
        pan.add(tabPane, gbc);

        ++row;

        messagePanel = new MessagePanel();
        gbc.gridy = row;
        gbc.insets = new Insets(2, 0, 0, 0);
        pan.add(messagePanel, gbc);

        ++row;

        pan.add(new JPanel(), pushGbc(colCount, row));

        // Engineering controls
        windowCoverCtrl = ComboPropertyCtrl.optionEnumInstance(Flamingos2.WINDOW_COVER_PROP, Flamingos2.WindowCover.class);
        deckerCtrl = ComboPropertyCtrl.enumInstance(Flamingos2.DECKER_PROP);
        readoutModeCtrl = ComboPropertyCtrl.optionEnumInstance(Flamingos2.READOUT_MODE_PROP, Flamingos2.ReadoutMode.class);
        readsCtrl = ComboPropertyCtrl.optionEnumInstance(Flamingos2.READS_PROP, Flamingos2.Reads.class);

        eOffsetCtrl = new CheckboxPropertyCtrl<>(Flamingos2.USE_ELECTRONIC_OFFSETTING_PROP);
    }

    private static JPanel getTabPanel(JComponent comp) {
        final JPanel pan = new JPanel();
        pan.setBorder(TAB_PANEL_BORDER);
        pan.setLayout(new BoxLayout(pan, BoxLayout.PAGE_AXIS));
        pan.add(comp);
        pan.add(Box.createVerticalGlue());
        return pan;
    }

    public JPanel getWindow() {
        return pan;
    }

    public Component getEngineeringComponent() {
        final JPanel pan = new JPanel(new GridBagLayout());
        addCtrl(pan, 0, 0, deckerCtrl);
        addCtrl(pan, 0, 1, readoutModeCtrl);
        addCtrl(pan, 0, 2, readsCtrl);
        addCtrl(pan, 0, 3, windowCoverCtrl);

        final GridBagConstraints gbc = propWidgetGbc(0, 4);
        gbc.gridwidth = 2;
        pan.add(eOffsetCtrl.getComponent(), gbc);
        eOffsetCtrl.getComponent().setToolTipText("Use electronic offsetting when possible.");

        pan.add(new JPanel(), new GridBagConstraints(){{
             gridx=0; gridy=5; weighty=1.0; fill=VERTICAL;
        }});

        return pan;
    }


    @Override
    public void handlePreDataObjectUpdate(Flamingos2 inst) {
        if (inst == null) return;
        inst.removePropertyChangeListener(messagePanel);
        inst.removePropertyChangeListener(Flamingos2.READMODE_PROP.getName(),       exposureTimeMessageUpdater);
        inst.removePropertyChangeListener(Flamingos2.FPU_PROP.getName(),            customMdfEnabler);
        inst.removePropertyChangeListener(Flamingos2.MOS_PREIMAGING_PROP.getName(), preImagingListener);

        inst.removePropertyChangeListener(Flamingos2.POS_ANGLE_PROP.getName(),      updateParallacticAnglePCL);
        inst.removePropertyChangeListener(Flamingos2.FPU_PROP.getName(),            updateParallacticAnglePCL);
        inst.removePropertyChangeListener(Flamingos2.DISPERSER_PROP.getName(),      updateParallacticAnglePCL);
        inst.removePropertyChangeListener(Flamingos2.MOS_PREIMAGING_PROP.getName(), updateUnboundedAnglePCL);
        inst.removePropertyChangeListener(Flamingos2.FPU_PROP.getName(),            updateUnboundedAnglePCL);

    }

    @Override
    public void handlePostDataObjectUpdate(Flamingos2 inst) {
        fpuCtrl.setBean(inst);
        mdfCtrl.setBean(inst);
        customSlitCtrl.setBean(inst);

        filterCtrl.setBean(inst);
        disperserCtrl.setBean(inst);
        lyotCtrl.setBean(inst);

        expTimeCtrl.setBean(inst);
        readModeCtrl.setBean(inst);
        portCtrl.setBean(inst);

        deckerCtrl.setBean(inst);
        readoutModeCtrl.setBean(inst);
        readsCtrl.setBean(inst);
        windowCoverCtrl.setBean(inst);
        eOffsetCtrl.setBean(inst);

        inst.addPropertyChangeListener(messagePanel);
        inst.addPropertyChangeListener(Flamingos2.FPU_PROP.getName(), customMdfEnabler);
        inst.addPropertyChangeListener(Flamingos2.READMODE_PROP.getName(), exposureTimeMessageUpdater);
        inst.addPropertyChangeListener(Flamingos2.MOS_PREIMAGING_PROP.getName(), preImagingListener);

        preImaging.setSelected(inst.getMosPreimaging() == YesNoType.YES);
        customMdfEnabler.update(inst);
        exposureTimeMessageUpdater.update();
        messagePanel.update();

        posAnglePanel.init(this, Site.GS);
        final boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
        posAnglePanel.updateEnabledState(editable);

        // If the position angle mode or FPU mode properties change, force an update on the parallactic angle mode.
        inst.addPropertyChangeListener(Flamingos2.POS_ANGLE_PROP.getName(), updateParallacticAnglePCL);
        inst.addPropertyChangeListener(Flamingos2.FPU_PROP.getName(),       updateParallacticAnglePCL);
        inst.addPropertyChangeListener(Flamingos2.DISPERSER_PROP.getName(), updateParallacticAnglePCL);

        // If MOS preimaging or the FPU mode properties change, force an update on the unbounded angle mode.
        inst.addPropertyChangeListener(Flamingos2.MOS_PREIMAGING_PROP.getName(), updateUnboundedAnglePCL);
        inst.addPropertyChangeListener(Flamingos2.FPU_PROP.getName(),            updateUnboundedAnglePCL);
    }
}
