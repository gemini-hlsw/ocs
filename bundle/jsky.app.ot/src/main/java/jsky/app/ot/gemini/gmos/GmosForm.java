package jsky.app.ot.gemini.gmos;

import com.jgoodies.forms.factories.DefaultComponentFactory;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.gmos.InstGmosCommon;
import edu.gemini.spModel.guide.GuideOption;
import edu.gemini.spModel.target.offset.OffsetPos;
import jsky.app.ot.gemini.editor.ComponentEditor;
import jsky.app.ot.gemini.parallacticangle.PositionAnglePanel;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.NumberBoxWidget;
import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class GmosForm<T extends InstGmosCommon> extends JPanel {

    private final static int leftLabelCol   = 0;
    private final static int leftWidgetCol  = 1;
    private final static int centerGapCol   = 2;
    private final static int extraGapCol    = 3;
    private final static int rightLabelCol  = 4;
    private final static int rightWidgetCol = 5;
    private final static int rightGapCol    = 6;
    public static final Border PANEL_BORDER = BorderFactory.createEmptyBorder(15, 15, 15, 15);


    final JComboBox filterComboBox;
    final TextBoxWidget exposureTime;
    final JComboBox disperserComboBox;
    final JLabel centralWavelengthLabel;
    final TextBoxWidget centralWavelength;
    final JLabel orderLabel;
    final DropDownListBoxWidget<String> orderComboBox;
    final JCheckBox preImgCheckButton;
    final JCheckBox nsCheckButton;
    protected final JComboBox detectorManufacturerComboBox;
    final JLabel warning1;
    final JRadioButton focalPlaneBuiltInButton;
    final DropDownListBoxWidget<String> builtinComboBox;
    final JRadioButton focalPlaneMaskButton;
    final TextBoxWidget focalPlaneMask;
    final JButton focalPlaneMaskPlotButton;
    final DropDownListBoxWidget<String> customSlitWidthComboBox;
    final JTabbedPane tabbedPane = new JTabbedPane();
    final DropDownListBoxWidget<String> xBinComboBox = new DropDownListBoxWidget<>();
    final DropDownListBoxWidget<String> yBinComboBox = new DropDownListBoxWidget<>();
    final JRadioButton ccdSlowLowButton = new JRadioButton();
    final JRadioButton ccd3AmpButton = new JRadioButton();
    final JRadioButton ccdFastLowButton = new JRadioButton();
    final JRadioButton ccd6AmpButton = new JRadioButton();
    final JRadioButton ccd12AmpButton = new JRadioButton();
    final JRadioButton ccdFastHighButton = new JRadioButton();
    final JRadioButton ccdSlowHighButton = new JRadioButton();
    final JLabel ccdGainLabel = new JLabel();
    final JLabel meanReadNoiseLabel = new JLabel();
    final JLabel ampCountLabel = new JLabel();
    final JRadioButton transFollowXYButton = new JRadioButton();
    final JRadioButton transFollowXYZButton = new JRadioButton();
    final JRadioButton transFollowZButton = new JRadioButton();
    final JRadioButton transNoFollowButton = new JRadioButton();
    final JSpinner transDtaSpinner = new JSpinner();
    final JLabel warning3 = new JLabel();
    final JRadioButton noROIButton = new JRadioButton();
    final JRadioButton ccd2Button = new JRadioButton();
    final JRadioButton centralSpectrumButton = new JRadioButton();
    final JRadioButton centralStampButton = new JRadioButton();
    final JRadioButton customButton = new JRadioButton();
    final JLabel warningCustomROI = new JLabel();
    final GmosCustomROITableWidget customROITable = new GmosCustomROITableWidget();
    final NumberBoxWidget xMin = new NumberBoxWidget();
    final NumberBoxWidget yMin = new NumberBoxWidget();
    final NumberBoxWidget xRange = new NumberBoxWidget();
    final NumberBoxWidget yRange = new NumberBoxWidget();
    final JButton customROINewButton = new JButton();
    final JButton customROIPasteButton = new JButton();
    final JButton customROIRemoveButton = new JButton();
    final JButton customROIRemoveAllButton = new JButton();
    final JRadioButton upLookingButton = new JRadioButton();
    final JRadioButton sideLookingButton = new JRadioButton();
    final JPanel nsPanel = new JPanel();
    final GmosOffsetPosTableWidget<OffsetPos> offsetTable = new GmosOffsetPosTableWidget<>();
    final NumberBoxWidget xOffset = new NumberBoxWidget();
    final NumberBoxWidget yOffset = new NumberBoxWidget();
    final DropDownListBoxWidget<GuideOption> oiwfsBox = new DropDownListBoxWidget<>();
    final JCheckBox electronicOffsetCheckBox = new JCheckBox();
    final JButton newButton = new JButton();
    final JButton removeButton = new JButton();
    final JButton removeAllButton = new JButton();
    final NumberBoxWidget shuffleOffset = new NumberBoxWidget();
    final NumberBoxWidget detectorRows = new NumberBoxWidget();
    final NumberBoxWidget numNSCycles = new NumberBoxWidget();
    final JTextField totalTime = new JTextField();
    final JLabel warning2 = new JLabel();
    final JLabel warning4 = new JLabel();

    PositionAnglePanel<T, EdCompInstGMOS<T> > posAnglePanel;

    private static Insets LABEL_INSETS = new Insets(ComponentEditor.PROPERTY_ROW_GAP, 0, 0, ComponentEditor.LABEL_WIDGET_GAP);
    private static GridBagConstraints labelGbc(final int row, final int col) {
        return new GridBagConstraints() {{
            gridy  = row;
            gridx  = col;
            anchor = GridBagConstraints.EAST;
            insets = LABEL_INSETS;
        }};
    }

    private static Insets WIDGET_INSETS = new Insets(ComponentEditor.PROPERTY_ROW_GAP, 0, 0, 0);
    private static GridBagConstraints widgetGbc(final int row, final int col) {
        return new GridBagConstraints() {{
            gridy  = row;
            gridx  = col;
            anchor = GridBagConstraints.WEST;
            fill   = GridBagConstraints.HORIZONTAL;
            insets = WIDGET_INSETS;
        }};
    }

    private static Insets SEPARATOR_INSETS = WIDGET_INSETS;
    private static GridBagConstraints separatorGbc(final int row, final int col, final int width) {
        return new GridBagConstraints() {{
            gridy     = row;
            gridx     = col;
            gridwidth = width;
            weightx   = 1.0;
            anchor    = GridBagConstraints.WEST;
            fill      = GridBagConstraints.HORIZONTAL;
            insets    = SEPARATOR_INSETS;
        }};
    }

    public GmosForm() {
        DefaultComponentFactory compFactory = DefaultComponentFactory.getInstance();

        xMin.setAllowNegative(false);
        yMin.setAllowNegative(false);
        xRange.setAllowNegative(false);
        yRange.setAllowNegative(false);

        ccd3AmpButton.setEnabled(false);
        ccd6AmpButton.setEnabled(false);
        ccd12AmpButton.setEnabled(false);
        //======== this ========
        setLayout(new GridBagLayout());
        setBorder(PANEL_BORDER);

        // Column gaps.
        add(new JPanel(), new GridBagConstraints() {{
            gridx   = centerGapCol;
            weightx = 1.0;
            fill    = GridBagConstraints.HORIZONTAL;
        }});
        add(new JPanel(), new GridBagConstraints() {{
            gridx = extraGapCol;
            ipadx = 10;
        }});
        add(new JPanel(), new GridBagConstraints() {{
            gridx   = rightGapCol;
            weightx = 1.0;
            fill    = GridBagConstraints.HORIZONTAL;
        }});

        // Filter
        final int filterRow = 0;
        add(new JLabel("Filter"), labelGbc(filterRow, leftLabelCol));
        filterComboBox = new JComboBox();
        add(filterComboBox, widgetGbc(filterRow, leftWidgetCol));

        // Exposure time
        final int exposureTimeRow = 0;
        add(new JLabel("Exposure Time (sec)"), labelGbc(exposureTimeRow, rightLabelCol));
        exposureTime = new TextBoxWidget();
        exposureTime.setToolTipText("Enter the exposure time in seconds");
        add(exposureTime, widgetGbc(exposureTimeRow, rightWidgetCol));

        // Disperser
        final int disperserRow = 1;
        add(new JLabel("Disperser"), labelGbc(disperserRow, leftLabelCol));
        disperserComboBox = new JComboBox();
        add(disperserComboBox, widgetGbc(disperserRow, leftWidgetCol));

        // Central wavelength
        final int centralWavelengthRow = 1;
        centralWavelengthLabel = new JLabel("Central Wavelength (nm)");
        centralWavelengthLabel.setToolTipText("Grating Central Wavelength in nanometers");
        add(centralWavelengthLabel, labelGbc(centralWavelengthRow, rightLabelCol));
        centralWavelength = new TextBoxWidget();
        centralWavelength.setToolTipText("Set the Grating Central Wavelength in Nanometers");
        add(centralWavelength, widgetGbc(centralWavelengthRow, rightWidgetCol));

        // Order and MOS pre-imaging
        final int orderMOSRow = 2;
        orderLabel = new JLabel("Order");
        add(orderLabel, labelGbc(orderMOSRow, leftLabelCol));
        final JPanel orderMOSPanel = new JPanel(new GridBagLayout());
        orderComboBox = new DropDownListBoxWidget<>();
        orderMOSPanel.add(orderComboBox, new GridBagConstraints() {{
            anchor = GridBagConstraints.WEST;
            insets = WIDGET_INSETS;
        }});
        orderMOSPanel.add(new JPanel(), new GridBagConstraints() {{
            gridx   = 1;
            fill    = GridBagConstraints.HORIZONTAL;
            weightx = 1.0;
        }});
        preImgCheckButton = new JCheckBox("MOS pre-imaging");
        orderMOSPanel.add(preImgCheckButton, new GridBagConstraints() {{
            gridx  = 2;
            anchor = GridBagConstraints.EAST;
            insets = WIDGET_INSETS;
        }});
        add(orderMOSPanel, new GridBagConstraints() {{
            gridy     = orderMOSRow;
            gridx     = leftWidgetCol;
            anchor    = GridBagConstraints.WEST;
            fill      = GridBagConstraints.HORIZONTAL;
        }});

        // Nod & Shuffle
        final int nsRow = 2;
        nsCheckButton = new JCheckBox("Use Nod & Shuffle");
        final GridBagConstraints nsCheckButtonGbc = widgetGbc(nsRow, rightLabelCol);
        nsCheckButtonGbc.gridwidth = 2;
        add(nsCheckButton, nsCheckButtonGbc);

        // CCD manufacturer
        final int ccdRow = 3;
        add(new JLabel("CCD manufacturer"), labelGbc(ccdRow, leftLabelCol));
        detectorManufacturerComboBox = new JComboBox();
        add(detectorManufacturerComboBox, widgetGbc(ccdRow, leftWidgetCol));

        // Wavelength warning
        final int nsWarningRow = 4;
        warning1 = new JLabel("Warning");
        warning1.setForeground(Color.red);
        add(warning1, new GridBagConstraints() {{
            gridy     = nsWarningRow;
            gridx     = leftLabelCol;
            gridwidth = 3;
            anchor    = GridBagConstraints.WEST;
            fill      = GridBagConstraints.HORIZONTAL;
        }});

        // Position angle
        final int posAngleRow = 5;
        final JComponent posAngleSeparator = compFactory.createSeparator("Position Angle");
        add(posAngleSeparator, separatorGbc(posAngleRow, leftLabelCol, 3));

        posAnglePanel = PositionAnglePanel.apply(SPComponentType.INSTRUMENT_GMOS);
        add(posAnglePanel.peer(), new GridBagConstraints() {{
            gridy      = posAngleRow+1;
            gridx      = leftLabelCol;
            gridwidth  = 3;
            gridheight = 2;
            anchor     = GridBagConstraints.WEST;
            fill       = GridBagConstraints.HORIZONTAL;
            insets     = WIDGET_INSETS;
        }});

        // Focal plane unit
        final int fpuRow = 5;
        final JComponent fpuSeparator = compFactory.createSeparator("Focal Plane Unit");
        add(fpuSeparator, separatorGbc(fpuRow, rightLabelCol, 2));

        focalPlaneBuiltInButton = new JRadioButton("Built-in");
        add(focalPlaneBuiltInButton, widgetGbc(fpuRow+1, rightLabelCol));

        builtinComboBox = new DropDownListBoxWidget<>();
        add(builtinComboBox, widgetGbc(fpuRow+1, rightWidgetCol));

        focalPlaneMaskButton = new JRadioButton("Custom Mask MDF");
        add(focalPlaneMaskButton, widgetGbc(fpuRow+2, rightLabelCol));
        focalPlaneMask = new TextBoxWidget();
        focalPlaneMask.setToolTipText("Enter the name of the custom mask");
        add(focalPlaneMask, widgetGbc(fpuRow+2, rightWidgetCol));

        focalPlaneMaskPlotButton = new JButton("Plot...");
        focalPlaneMaskPlotButton.setToolTipText("Plot a custom mask file in the position editor (FITS format)");
        final GridBagConstraints focalPlaneMaskPlotButtonGbc = widgetGbc(fpuRow+2, rightGapCol);
        focalPlaneMaskPlotButtonGbc.fill = GridBagConstraints.NONE;
        add(focalPlaneMaskPlotButton, focalPlaneMaskPlotButtonGbc);

        final JLabel slitWidthLabel = new JLabel("Slit Width");
        add(slitWidthLabel, new GridBagConstraints() {{
            gridy  = fpuRow+3;
            gridx  = rightLabelCol;
            anchor = GridBagConstraints.WEST;
            insets = new Insets(0, 35, 0, 0);
        }});
        customSlitWidthComboBox = new DropDownListBoxWidget<>();
        add(customSlitWidthComboBox, widgetGbc(fpuRow+3, rightWidgetCol));

        //======== tabbedPane ========
        {
            CellConstraints cc = new CellConstraints();

            tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

            //======== panel6 ========
            {
                final JPanel panel6 = new JPanel(new FormLayout(
                        new ColumnSpec[] {
                                FormFactory.RELATED_GAP_COLSPEC,
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                new ColumnSpec(Sizes.DLUX4),
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                FormFactory.UNRELATED_GAP_COLSPEC
                        },
                        new RowSpec[] {
                                new RowSpec(RowSpec.FILL, Sizes.DLUY9, 0.1),
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                new RowSpec(RowSpec.FILL, Sizes.DLUY9, 0.1),
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.PARAGRAPH_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                new RowSpec(RowSpec.FILL, Sizes.DLUY9, 0.1),
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.LINE_GAP_ROWSPEC,
                                new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                        }));

                //======== panel12 ========
                {
                    final JPanel panel12 = new JPanel(new FormLayout(
                            new ColumnSpec[] {
                                    FormFactory.DEFAULT_COLSPEC,
                                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                    FormFactory.DEFAULT_COLSPEC,
                                    new ColumnSpec(ColumnSpec.FILL, Sizes.DLUX7, 0.1),
                                    FormFactory.DEFAULT_COLSPEC,
                                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                    FormFactory.DEFAULT_COLSPEC,
                                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                    new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                            },
                            RowSpec.decodeSpecs("default")));

                    //---- label10 ----
                    final JLabel label10 = new JLabel("X Binning");
                    panel12.add(label10, cc.xy(1, 1));
                    panel12.add(xBinComboBox, cc.xy(3, 1));

                    //---- label11 ----
                    final JLabel label11 = new JLabel("Y Binning");
                    panel12.add(label11, cc.xy(5, 1));
                    panel12.add(yBinComboBox, cc.xy(7, 1));
                    panel6.add(panel12, cc.xywh(3, 3, 3, 1));
                }

                final JComponent ccdReadoutSeparator = compFactory.createSeparator("Set the CCD Readout Characteristics");
                panel6.add(ccdReadoutSeparator, cc.xywh(3, 5, 3, 1));

                //======== panel7 ========
                {
                    final JPanel panel7 = new JPanel(new FormLayout(
                            new ColumnSpec[] {
                                    new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, 0.1),
                                    new ColumnSpec(ColumnSpec.LEFT, Sizes.DLUX11, FormSpec.NO_GROW),
                                    new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, 0.1),
                                    new ColumnSpec(ColumnSpec.LEFT, Sizes.DLUX11, FormSpec.NO_GROW),
                                    new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, 0.1),
                                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                    new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                            },
                            new RowSpec[] {
                                    FormFactory.DEFAULT_ROWSPEC,
                                    FormFactory.NARROW_LINE_GAP_ROWSPEC,
                                    FormFactory.DEFAULT_ROWSPEC,
                                    FormFactory.LINE_GAP_ROWSPEC,
                                    FormFactory.DEFAULT_ROWSPEC,
                                    FormFactory.LINE_GAP_ROWSPEC,
                                    FormFactory.DEFAULT_ROWSPEC
                            }));

                    //---- ccdSlowLowButton ----
                    ccdSlowLowButton.setText("Slow Read/Low Gain : Standard Science Mode");
                    panel7.add(ccdSlowLowButton, cc.xy(1, 1));

                    //---- ccd3AmpButton ----
                    ccd3AmpButton.setText("Use 3 Amplifiers");
                    panel7.add(ccd3AmpButton, cc.xy(3, 1));

                    //---- ccdFastLowButton ----
                    ccdFastLowButton.setText("Fast Read/Low Gain : Acquisitions / Rapid Readout");
                    panel7.add(ccdFastLowButton, cc.xy(1, 3));

                    //---- ccd6AmpButton ----
                    ccd6AmpButton.setText("Use 6 Amplifiers");
                    panel7.add(ccd6AmpButton, cc.xy(3, 3));

                    //---- ccd12AmpButton ----
                    ccd12AmpButton.setText("Use 12 Amplifiers");
                    panel7.add(ccd12AmpButton, cc.xy(3, 5));

                    //---- ccdFastHighButton ----
                    ccdFastHighButton.setText("Fast Read/High Gain : Bright Targets Imaging/Spectroscopy");
                    panel7.add(ccdFastHighButton, cc.xy(1, 5));

                    //---- ccdSlowHighButton ----
                    ccdSlowHighButton.setText("Slow Read/High Gain : Engineering Only");
                    panel7.add(ccdSlowHighButton, cc.xy(1, 7));
                    panel6.add(panel7, cc.xy(5, 7));
                }

                //======== panel11 ========
                {
                    final JPanel panel11 = new JPanel(new FormLayout(
                            new ColumnSpec[] {
                                    FormFactory.DEFAULT_COLSPEC,
                                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                    FormFactory.DEFAULT_COLSPEC
                            },
                            new RowSpec[] {
                                    FormFactory.DEFAULT_ROWSPEC,
                                    FormFactory.LINE_GAP_ROWSPEC,
                                    FormFactory.DEFAULT_ROWSPEC,
                                    FormFactory.LINE_GAP_ROWSPEC,
                                    FormFactory.DEFAULT_ROWSPEC
                            }));

                    //---- label12 ----
                    final JLabel label12 = new JLabel("Resulting CCD Gain:");
                    panel11.add(label12, cc.xy(1, 1));

                    //---- ccdGainLabel ----
                    ccdGainLabel.setText("2");
                    panel11.add(ccdGainLabel, cc.xy(4, 1));

                    //---- meanReadNoiseLabelLabel ----
                    final JLabel meanReadNoiseLabelLabel = new JLabel("Mean read noise:");
                    panel11.add(meanReadNoiseLabelLabel, cc.xy(1, 3));

                    //---- meanReadNoiseLabel ----
                    meanReadNoiseLabel.setText("3.4");
                    panel11.add(meanReadNoiseLabel, cc.xy(4, 3));

                    //---- ampCountLabel ----
                    ampCountLabel.setText("3.4");
                    panel11.add(ampCountLabel, cc.xy(4, 5));

                    //---- ampCountLabelLabel ----
                    final JLabel ampCountLabel = new JLabel("Number of amps:");
                    panel11.add(ampCountLabel, cc.xy(1, 5));
                    panel6.add(panel11, cc.xywh(3, 9, 3, 1));
                }
                warning4.setForeground(Color.red);
                panel6.add(warning4, cc.xywh(3,11,3,1));
                tabbedPane.addTab("CCD Readout", panel6);
            }

            //======== panel14 ========
            {
                final JPanel panel14 = new JPanel(new FormLayout(
                        new ColumnSpec[] {
                                FormFactory.UNRELATED_GAP_COLSPEC,
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                FormFactory.DEFAULT_COLSPEC,
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                FormFactory.DEFAULT_COLSPEC,
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                        },
                        new RowSpec[] {
                                new RowSpec(RowSpec.FILL, Sizes.DLUY9, 0.5),
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                new RowSpec(RowSpec.FILL, Sizes.DLUY3, 0.5),
                                FormFactory.DEFAULT_ROWSPEC,
                                new RowSpec(RowSpec.FILL, Sizes.DLUY3, 0.5),
                                FormFactory.DEFAULT_ROWSPEC,
                                new RowSpec(RowSpec.FILL, Sizes.DLUY3, 0.5),
                                FormFactory.DEFAULT_ROWSPEC,
                                new RowSpec(RowSpec.FILL, Sizes.DLUY9, 0.5),
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.LINE_GAP_ROWSPEC,
                                new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                        }));

                //---- transFollowXYButton ----
                transFollowXYButton.setText("Follow in X and Y");
                panel14.add(transFollowXYButton, cc.xywh(3, 3, 3, 1));

                //---- transFollowXYZButton ----
                transFollowXYZButton.setText("* Follow in X, Y and Z (focus)");
                panel14.add(transFollowXYZButton, cc.xywh(3, 5, 3, 1));

                //---- transFollowZButton ----
                transFollowZButton.setText("* Follow in Z Only");
                panel14.add(transFollowZButton, cc.xywh(3, 7, 3, 1));

                //---- transNoFollowButton ----
                transNoFollowButton.setText("Do Not Follow");
                panel14.add(transNoFollowButton, cc.xywh(3, 9, 3, 1));

                //---- label8 ----
                final JLabel label8 = new JLabel("Detector translation assembly (DTA-X) offset:");
                panel14.add(label8, cc.xy(3, 11));

                //---- transDtaSpinner ----
                transDtaSpinner.setToolTipText("Specify the DTA-X offset in unbinned pixels (range: ?6 pixels)");
                panel14.add(transDtaSpinner, cc.xy(5, 11));

                //---- warning3 ----
                warning3.setText("Warning");
                warning3.setForeground(Color.red);
                panel14.add(warning3, cc.xywh(3, 13, 5, 1));
                tabbedPane.addTab("Translation Stage", panel14);
            }


            //======== panel15 ========
            {
                final JPanel panel15 = new JPanel(new FormLayout(
                        new ColumnSpec[] {
                                FormFactory.UNRELATED_GAP_COLSPEC,
                                FormFactory.DEFAULT_COLSPEC,
                                FormFactory.UNRELATED_GAP_COLSPEC,
                                ColumnSpec.decode("max(min;160dlu):grow"),
                                FormFactory.UNRELATED_GAP_COLSPEC,
                        },
                        new RowSpec[] {
                                FormFactory.PARAGRAPH_GAP_ROWSPEC,
                                new RowSpec(RowSpec.FILL, Sizes.DLUY3, FormSpec.DEFAULT_GROW),
                                FormFactory.DEFAULT_ROWSPEC,
                                new RowSpec(RowSpec.FILL, Sizes.DLUY3, FormSpec.DEFAULT_GROW),
                                FormFactory.DEFAULT_ROWSPEC,
                                new RowSpec(RowSpec.FILL, Sizes.DLUY3, FormSpec.DEFAULT_GROW),
                                FormFactory.DEFAULT_ROWSPEC,
                                new RowSpec(RowSpec.FILL, Sizes.DLUY3, FormSpec.DEFAULT_GROW),
                                FormFactory.DEFAULT_ROWSPEC,
                                new RowSpec(RowSpec.FILL, Sizes.DLUY3, FormSpec.DEFAULT_GROW),
                                FormFactory.DEFAULT_ROWSPEC,
                                new RowSpec(RowSpec.FILL, Sizes.DLUY3, FormSpec.DEFAULT_GROW),
                                FormFactory.DEFAULT_ROWSPEC,
                                new RowSpec(RowSpec.FILL, Sizes.DLUY3, FormSpec.DEFAULT_GROW),
                                new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                        }));

                //---- noROIButton ----
                noROIButton.setText("Full Frame Readout");
                panel15.add(noROIButton, cc.xy(2, 3));

                //---- ccd2Button ----
                ccd2Button.setText("CCD2");
                panel15.add(ccd2Button, cc.xy(2, 5));

                //---- centralSpectrumButton ----
                centralSpectrumButton.setText("Central Spectrum");
                panel15.add(centralSpectrumButton, cc.xy(2, 7));

                //---- centralStampButton ----
                centralStampButton.setText("Central Stamp");
                panel15.add(centralStampButton, cc.xy(2, 9));

                //---- customButton ----
                customButton.setText("Custom ROI");
                panel15.add(customButton, cc.xy(2, 11));

                final JPanel customROIPanel = new JPanel(new FormLayout(
                        new ColumnSpec[] {
                                ColumnSpec.decode("max(min;40dlu):grow"),
                                ColumnSpec.decode("max(min;40dlu):grow"),
                                ColumnSpec.decode("max(min;40dlu):grow"),
                                ColumnSpec.decode("max(min;40dlu):grow"),
                        },
                        new RowSpec[] {
                                new RowSpec(RowSpec.FILL, Sizes.DLUY1, FormSpec.DEFAULT_GROW),
                                FormFactory.RELATED_GAP_ROWSPEC,

                                new RowSpec(RowSpec.FILL, Sizes.DLUY1, FormSpec.DEFAULT_GROW),
                                FormFactory.RELATED_GAP_ROWSPEC,

                                new RowSpec(RowSpec.FILL, Sizes.DLUY1, FormSpec.DEFAULT_GROW),
                                FormFactory.RELATED_GAP_ROWSPEC,

                                new RowSpec(RowSpec.FILL, Sizes.DLUY1, FormSpec.DEFAULT_GROW),

                                FormFactory.UNRELATED_GAP_ROWSPEC,
                                FormFactory.UNRELATED_GAP_ROWSPEC,

                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.UNRELATED_GAP_ROWSPEC,

                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.UNRELATED_GAP_ROWSPEC,

                        }));
                //---- warning1 ----
                warningCustomROI.setText("Warning");
                warningCustomROI.setForeground(Color.red);
                customROIPanel.add(warningCustomROI, cc.xywh(1, 7, 4, 2));

                //---- xMin ----
                xMin.setToolTipText("Xmin in unbinned pixels, specifying the X coordinate of the lower lefthand corner of the ROI");
                customROIPanel.add(xMin, cc.xy(1, 10,  CellConstraints.FILL, CellConstraints.DEFAULT));
                //---- yMin ----
                yMin.setToolTipText("Ymin in unbinned pixels, specifying the Y coordinate of the lower lefthand corner of the ROI");
                customROIPanel.add(yMin, cc.xy(2, 10,  CellConstraints.FILL, CellConstraints.DEFAULT));
                //---- xRange ----
                xRange.setToolTipText("Xrange in binned pixels, specifying the width of the ROI");
                customROIPanel.add(xRange, cc.xy(3, 10,  CellConstraints.FILL, CellConstraints.DEFAULT));
                //---- yRange ----
                yRange.setToolTipText("Yrange in binned pixels, specifying the height of the ROI");
                customROIPanel.add(yRange, cc.xy(4, 10,  CellConstraints.FILL, CellConstraints.DEFAULT));

                //---- customROIPasteButton ----
                customROIPasteButton.setText("Paste");
                customROIPasteButton.setToolTipText("Paste a set of rows of ROI data, each line with four integer values");
                customROIPanel.add(customROIPasteButton, cc.xy(1, 12));
                //---- customROINewButton ----
                customROINewButton.setText("Add");
                customROIPanel.add(customROINewButton, cc.xy(2, 12));
                //---- customROIRemoveButton ----
                customROIRemoveButton.setText("Remove");
                customROIPanel.add(customROIRemoveButton, cc.xy(3, 12));
                //---- customROIRemoveAllButton ----
                customROIRemoveAllButton.setText("Remove All");
                customROIPanel.add(customROIRemoveAllButton, cc.xy(4, 12));

                final JScrollPane scrollPaneROI = new JScrollPane();
                scrollPaneROI.setViewportView(customROITable);
                customROIPanel.setBorder(BorderFactory.createTitledBorder("Custom ROIs"));
                customROIPanel.add(scrollPaneROI, cc.xywh(1, 1, 4, 5));

                panel15.add(customROIPanel,cc.xywh(4,3,1,11));
                tabbedPane.addTab("Regions of Interest", panel15);
            }

            //======== panel16 ========
            {
                final JPanel panel16 = new JPanel(new FormLayout(
                        new ColumnSpec[] {
                                FormFactory.UNRELATED_GAP_COLSPEC,
                                FormFactory.DEFAULT_COLSPEC
                        },
                        new RowSpec[] {
                                new RowSpec(RowSpec.FILL, Sizes.DLUY9, 0.5),
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                new RowSpec(RowSpec.FILL, Sizes.DLUY3, 0.5),
                                FormFactory.DEFAULT_ROWSPEC,
                                new RowSpec(RowSpec.FILL, Sizes.DLUY3, 0.5),
                                new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                        }));

                //---- upLookingButton ----
                upLookingButton.setText("Up-looking");
                panel16.add(upLookingButton, cc.xy(2, 3));

                //---- sideLookingButton ----
                sideLookingButton.setText("Side-looking");
                panel16.add(sideLookingButton, cc.xy(2, 5));
                tabbedPane.addTab("ISS Port", panel16);
            }

            //======== nsPanel ========
            {
                nsPanel.setLayout(new FormLayout(
                        new ColumnSpec[] {
                                FormFactory.UNRELATED_GAP_COLSPEC,
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                ColumnSpec.decode("max(min;40dlu)"),
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                ColumnSpec.decode("max(min;12dlu):grow"),
                                FormFactory.UNRELATED_GAP_COLSPEC,
                                new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                ColumnSpec.decode("max(min;12dlu):grow"),
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                FormFactory.UNRELATED_GAP_COLSPEC
                        },
                        new RowSpec[] {
                                FormFactory.PARAGRAPH_GAP_ROWSPEC,
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.UNRELATED_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.PARAGRAPH_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.PARAGRAPH_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC
                        }));

                final JComponent nodSeparator = compFactory.createSeparator("Nod (arcsec)");
                nsPanel.add(nodSeparator, cc.xywh(3, 3, 9, 1));

                //======== scrollPane1 ========
                final JScrollPane scrollPane1 = new JScrollPane();
                scrollPane1.setViewportView(offsetTable);
                nsPanel.add(scrollPane1, cc.xywh(7, 5, 5, 5));

                //---- label13 ----
                final JLabel label13 = new JLabel("p");
                nsPanel.add(label13, cc.xy(3, 5));

                //---- xOffset ----
                xOffset.setToolTipText("The nod offset from the base position RA in arcsec");
                nsPanel.add(xOffset, cc.xywh(5, 5, 1, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

                //---- label14 ----
                final JLabel label14 = new JLabel("q");
                nsPanel.add(label14, cc.xy(3, 7));

                //---- yOffset ----
                yOffset.setToolTipText("The nod offset from the base position Dec in arcsec");
                nsPanel.add(yOffset, cc.xywh(5, 7, 1, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

                //---- label15 ----
                final JLabel label15 = new JLabel("OIWFS");
                nsPanel.add(label15, cc.xy(3, 9));

                //---- oiwfsBox ----
                oiwfsBox.setToolTipText("The OIWFS setting to use for the selected nod offset");
                nsPanel.add(oiwfsBox, cc.xy(5, 9));

                //======== panel2 ========
                {
                    final JPanel panel2 = new JPanel(new FormLayout(
                            new ColumnSpec[] {
                                    new ColumnSpec(ColumnSpec.FILL, Sizes.PREFERRED, FormSpec.DEFAULT_GROW),
                                    FormFactory.GLUE_COLSPEC,
                                    FormFactory.DEFAULT_COLSPEC,
                                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                    FormFactory.DEFAULT_COLSPEC,
                                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                    FormFactory.DEFAULT_COLSPEC
                            },
                            RowSpec.decodeSpecs("default")));

                    //---- electronicOffsetCheckBox ----
                    electronicOffsetCheckBox.setText("Use Electronic Offsetting?");
                    electronicOffsetCheckBox.setToolTipText("Enable or disable electronic offsetting");
                    panel2.add(electronicOffsetCheckBox, new CellConstraints(1, 1, 1, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT, new Insets( 0, 11, 0, 0)));

                    //---- newButton ----
                    newButton.setText("New");
                    newButton.setToolTipText("Add a new nod offset position");
                    panel2.add(newButton, cc.xy(3, 1));

                    //---- removeButton ----
                    removeButton.setText("Remove");
                    removeButton.setToolTipText("Remove the selected nod offset positions");
                    panel2.add(removeButton, cc.xy(5, 1));

                    //---- removeAllButton ----
                    removeAllButton.setText("Remove All");
                    removeAllButton.setToolTipText("Remove all nod offset positions");
                    panel2.add(removeAllButton, cc.xy(7, 1));
                    nsPanel.add(panel2, cc.xywh(1, 11, 11, 1));
                }

                //---- label16 ----
                final JLabel label16 = new JLabel("Offset (arcsec)");
                nsPanel.add(label16, cc.xywh(3, 13, 3, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

                //---- shuffleOffset ----
                shuffleOffset.setToolTipText("The shuffle offset in arcsec");
                nsPanel.add(shuffleOffset, cc.xy(7, 13));

                //---- label18 ----
                final JLabel label18 = new JLabel("Offset (detector rows)");
                nsPanel.add(label18, cc.xy(9, 13));

                //---- detectorRows ----
                detectorRows.setToolTipText("The shuffle offset in detector rows");
                nsPanel.add(detectorRows, cc.xy(11, 13));

                //---- label17 ----
                final JLabel label17 = new JLabel("Number of N&S Cycles");
                nsPanel.add(label17, cc.xywh(3, 15, 3, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

                //---- numNSCycles ----
                numNSCycles.setToolTipText("The number of nod & shuffle cycles");
                nsPanel.add(numNSCycles, cc.xy(7, 15));

                //---- totalTimeUnitsLabel ----
                final JLabel totalTimeUnitsLabel = new JLabel("Total Observe Time (sec)");
                nsPanel.add(totalTimeUnitsLabel, cc.xy(9, 15));

                //---- totalTime ----
                totalTime.setEditable(false);
                totalTime.setToolTipText("The total observe time in seconds");
                nsPanel.add(totalTime, cc.xy(11, 15));

                //---- warning2 ----
                warning2.setText("Warning");
                warning2.setForeground(Color.red);
                nsPanel.add(warning2, cc.xywh(3, 17, 9, 1));
            }
            tabbedPane.addTab("Nod & Shuffle", nsPanel);
        }

        final int tabbedPaneRow = 9;
        add(tabbedPane, new GridBagConstraints() {{
            gridx     = 0;
            gridy     = tabbedPaneRow;
            gridwidth = rightGapCol+1;
            anchor    = GridBagConstraints.NORTHWEST;
            fill      = GridBagConstraints.BOTH;
            weightx   = 1.0;
            weighty   = 1.0;
        }});

        //---- fpuGroup ----
        ButtonGroup fpuGroup = new ButtonGroup();
        fpuGroup.add(focalPlaneBuiltInButton);
        fpuGroup.add(focalPlaneMaskButton);

        //---- slowFastGroup ----
        ButtonGroup slowFastGroup = new ButtonGroup();
        slowFastGroup.add(ccdSlowLowButton);
        slowFastGroup.add(ccdFastLowButton);
        slowFastGroup.add(ccdFastHighButton);
        slowFastGroup.add(ccdSlowHighButton);

        //---- ampGroup ----
        ButtonGroup ampGroup = new ButtonGroup();
        ampGroup.add(ccd3AmpButton);
        ampGroup.add(ccd6AmpButton);
        ampGroup.add(ccd12AmpButton);

        //---- transStageGroup ----
        ButtonGroup transStageGroup = new ButtonGroup();
        transStageGroup.add(transFollowXYButton);
        transStageGroup.add(transFollowXYZButton);
        transStageGroup.add(transFollowZButton);
        transStageGroup.add(transNoFollowButton);

        //---- roiGroup ----
        ButtonGroup roiGroup = new ButtonGroup();
        roiGroup.add(noROIButton);
        roiGroup.add(ccd2Button);
        roiGroup.add(centralSpectrumButton);
        roiGroup.add(centralStampButton);
        roiGroup.add(customButton);

        //---- issGroup ----
        ButtonGroup issGroup = new ButtonGroup();
        issGroup.add(upLookingButton);
        issGroup.add(sideLookingButton);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

}
