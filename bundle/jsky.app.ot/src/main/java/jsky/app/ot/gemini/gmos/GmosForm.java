package jsky.app.ot.gemini.gmos;

import com.jgoodies.forms.factories.DefaultComponentFactory;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import jsky.app.ot.gemini.parallacticangle.ParallacticAnglePanel;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.NumberBoxWidget;
import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
import java.awt.*;

/*
 * Created by JFormDesigner on Tue Nov 08 20:52:19 CET 2005
 *
 * JFormDesigner use deprecated. Update this file manually, and hopefully one
 * day let's do it in a way that is easy to mantain!!
 */



/**
 * @author User #1
 */
public class GmosForm extends JPanel {
    public GmosForm() {
        initComponents();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        DefaultComponentFactory compFactory = DefaultComponentFactory.getInstance();
        panel3 = new JPanel();
        label1 = new JLabel();
        filterComboBox = new JComboBox();
        label2 = new JLabel();
        exposureTime = new TextBoxWidget();
        label3 = new JLabel();
        disperserComboBox = new JComboBox();
        centralWavelengthLabel = new JLabel();
        centralWavelength = new TextBoxWidget();
        orderLabel = new JLabel();
        panel1 = new JPanel();
        orderComboBox = new DropDownListBoxWidget();
        preImgCheckButton = new JCheckBox();
        nsCheckButton = new JCheckBox();
        detectorManufacturerLabel = new JLabel();
        detectorManufacturerComboBox = new JComboBox();
        warning1 = new JLabel();
        goodiesFormsSeparator4 = compFactory.createSeparator("Position Angle");
        posAngle = new NumberBoxWidget();
        label4 = new JLabel();
        posAngle180 = new JCheckBox();
        goodiesFormsSeparator5 = compFactory.createSeparator("Focal Plane Unit");
        focalPlaneBuiltInButton = new JRadioButton();
        builtinComboBox = new DropDownListBoxWidget();
        focalPlaneMaskButton = new JRadioButton();
        focalPlaneMask = new TextBoxWidget();
        focalPlaneMaskPlotButton = new JButton();
        customSlitWidthComboBox = new DropDownListBoxWidget();
        tabbedPane = new JTabbedPane();
        panel6 = new JPanel();
        panel12 = new JPanel();
        label10 = new JLabel();
        xBinComboBox = new DropDownListBoxWidget();
        label11 = new JLabel();
        yBinComboBox = new DropDownListBoxWidget();
        goodiesFormsSeparator3 = compFactory.createSeparator("Set the CCD Readout Characteristics");
        panel7 = new JPanel();
        ccdSlowLowButton = new JRadioButton();
        ccd3AmpButton = new JRadioButton();
        ccdFastLowButton = new JRadioButton();
        ccd6AmpButton = new JRadioButton();
        ccd12AmpButton = new JRadioButton();
        ccdFastHighButton = new JRadioButton();
        ccdSlowHighButton = new JRadioButton();
        panel11 = new JPanel();
        label12 = new JLabel();
        ccdGainLabel = new JLabel();
        meanReadNoiseLabelLabel = new JLabel();
        meanReadNoiseLabel = new JLabel();
        ampCountLabel = new JLabel();
        ampCountLabelLabel = new JLabel();
        panel14 = new JPanel();
        transFollowXYButton = new JRadioButton();
        transFollowXYZButton = new JRadioButton();
        transFollowZButton = new JRadioButton();
        transNoFollowButton = new JRadioButton();
        label8 = new JLabel();
        transDtaSpinner = new JSpinner();
        warning3 = new JLabel();
        panel15 = new JPanel();
        noROIButton = new JRadioButton();
        ccd2Button = new JRadioButton();
        centralSpectrumButton = new JRadioButton();
        centralStampButton = new JRadioButton();
        customButton = new JRadioButton();
        customROIPanel = new JPanel();
        scrollPaneROI = new JScrollPane();
        customROITable = new GmosCustomROITableWidget();
        warningCustomROI = new JLabel();
        xMin = new NumberBoxWidget();
        xMin.setAllowNegative(false);
        yMin = new NumberBoxWidget();
        yMin.setAllowNegative(false);
        xRange = new NumberBoxWidget();
        xRange.setAllowNegative(false);
        yRange = new NumberBoxWidget();
        yRange.setAllowNegative(false);
        customROINewButton = new JButton();
        customROIPasteButton = new JButton();
        customROIRemoveButton = new JButton();
        customROIRemoveAllButton = new JButton();
        panel16 = new JPanel();
        upLookingButton = new JRadioButton();
        sideLookingButton = new JRadioButton();
        nsPanel = new JPanel();
        goodiesFormsSeparator1 = compFactory.createSeparator("Nod (arcsec)");
        scrollPane1 = new JScrollPane();
        offsetTable = new GmosOffsetPosTableWidget();
        label13 = new JLabel();
        xOffset = new NumberBoxWidget();
        label14 = new JLabel();
        yOffset = new NumberBoxWidget();
        label15 = new JLabel();
        oiwfsBox = new DropDownListBoxWidget();
        panel2 = new JPanel();
        electronicOffsetCheckBox = new JCheckBox();
        newButton = new JButton();
        removeButton = new JButton();
        removeAllButton = new JButton();
        label16 = new JLabel();
        shuffleOffset = new NumberBoxWidget();
        label18 = new JLabel();
        detectorRows = new NumberBoxWidget();
        label17 = new JLabel();
        numNSCycles = new NumberBoxWidget();
        totalTimeUnitsLabel = new JLabel();
        totalTime = new JTextField();
        warning2 = new JLabel();
        warning4 = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setLayout(new FormLayout(
                ColumnSpec.decodeSpecs("max(min;50dlu):grow"),
                new RowSpec[] {
                        FormFactory.PARAGRAPH_GAP_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.UNRELATED_GAP_ROWSPEC,
                        new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                }));

        //======== panel3 ========
        {
            panel3.setLayout(new FormLayout(
                    new ColumnSpec[] {
                            new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            ColumnSpec.decode("max(pref;200dlu)"),  // originally 84, then 150
                            FormFactory.UNRELATED_GAP_COLSPEC,
                            ColumnSpec.decode("right:max(pref;70dlu)"), // originally 50
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            ColumnSpec.decode("max(pref;50dlu)"),
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            ColumnSpec.decode("max(pref;50dlu)")
                    },
                    new RowSpec[] {
                            FormFactory.DEFAULT_ROWSPEC,
                            FormFactory.LINE_GAP_ROWSPEC,
                            FormFactory.DEFAULT_ROWSPEC,
                            FormFactory.LINE_GAP_ROWSPEC,
                            FormFactory.DEFAULT_ROWSPEC,
                            FormFactory.LINE_GAP_ROWSPEC,
                            FormFactory.DEFAULT_ROWSPEC,
                            FormFactory.LINE_GAP_ROWSPEC,
                            FormFactory.DEFAULT_ROWSPEC,
                            FormFactory.UNRELATED_GAP_ROWSPEC,
                            new RowSpec(RowSpec.TOP, Sizes.DEFAULT, FormSpec.NO_GROW),
                            FormFactory.LINE_GAP_ROWSPEC,
                            FormFactory.DEFAULT_ROWSPEC,
                            FormFactory.LINE_GAP_ROWSPEC,
                            FormFactory.DEFAULT_ROWSPEC,
                            FormFactory.LINE_GAP_ROWSPEC,
                            FormFactory.DEFAULT_ROWSPEC
                    }));

            //---- label1 ----
            label1.setText("Filter");
            panel3.add(label1, cc.xy(1, 1));
            panel3.add(filterComboBox, cc.xy(3, 1));

            //---- label2 ----
            label2.setText("Exposure Time (sec)");
            panel3.add(label2, cc.xy(5, 1));

            //---- exposureTime ----
            exposureTime.setToolTipText("Enter the exposure time in seconds");
            panel3.add(exposureTime, cc.xywh(7, 1, 1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

            //---- label3 ----
            label3.setText("Disperser");
            panel3.add(label3, cc.xy(1, 3));
            panel3.add(disperserComboBox, cc.xy(3, 3));

            //---- centralWavelengthLabel ----
            centralWavelengthLabel.setText("Central Wavelength (nm)");
            centralWavelengthLabel.setToolTipText("Grating Central Wavelength in nanometers");
            panel3.add(centralWavelengthLabel, cc.xy(5, 3));

            //---- centralWavelength ----
            centralWavelength.setToolTipText("Set the Grating Central Wavelength in Nanometers");
            panel3.add(centralWavelength, cc.xy(7, 3));

            //---- orderLabel ----
            orderLabel.setText("Order");
            panel3.add(orderLabel, cc.xy(1, 5));

            //======== panel1 ========
            {
                panel1.setLayout(new GridBagLayout());
                ((GridBagLayout)panel1.getLayout()).columnWidths = new int[] {0, 0, 0};
                ((GridBagLayout)panel1.getLayout()).rowHeights = new int[] {0, 0};
                ((GridBagLayout)panel1.getLayout()).columnWeights = new double[] {0.0, 1.0, 1.0E-4};
                ((GridBagLayout)panel1.getLayout()).rowWeights = new double[] {0.0, 1.0E-4};
                panel1.add(orderComboBox, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 5), 0, 0));

                //---- preImgCheckButton ----
                preImgCheckButton.setText("MOS pre-imaging");
                panel1.add(preImgCheckButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                        new Insets(0, 11, 0, 0), 0, 0));
            }
            panel3.add(panel1, cc.xy(3, 5));

            //---- nsCheckButton ----
            nsCheckButton.setText("Use Nod & Shuffle");
            panel3.add(nsCheckButton, new CellConstraints(5, 5, 1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT, new Insets( 0, 11, 0, 0)));

            //---- detectorManufacturerLabel ----
            detectorManufacturerLabel.setText("CCD manufacturer");
            panel3.add(detectorManufacturerLabel, cc.xy(1, 7));
            panel3.add(detectorManufacturerComboBox, cc.xy(3, 7));

            //---- warning1 ----
            warning1.setText("Warning");
            warning1.setForeground(Color.red);
            panel3.add(warning1, cc.xywh(1, 9, 7, 1));


            // ---- Position Angle ----
            panel3.add(goodiesFormsSeparator4, cc.xywh(1, 11, 3, 1));

            JPanel posAnglePanel = new JPanel(new GridBagLayout());
            posAngle.setColumns(4);
            posAngle.setToolTipText("Set the position angle in degrees east of north");
            posAnglePanel.add(posAngle, new GridBagConstraints() {{
                gridx      = 0;
                gridy      = 0;
                weightx    = 1.0;
                fill       = HORIZONTAL;
                insets     = new Insets(0, 11, 0, 0); // 10, 11
                anchor     = WEST;
            }});

            label4.setText("deg E of N");
            label4.setLabelFor(posAngle);
            posAnglePanel.add(label4, new GridBagConstraints() {{
                gridx      = 1;
                gridy      = 0;
                //weightx    = 1.0;
                insets     = new Insets(0, 5, 0, 0); // 10, 5
                anchor     = WEST;
                //fill       = HORIZONTAL;
            }});
            posAngle180.setText("Allow 180\u00ba change for guide star search");
            posAngle180.setToolTipText("Allow guide star search to adjust pos angle \u00b1180\u00ba");
            posAnglePanel.add(posAngle180, new GridBagConstraints() {{
                gridx = 2;
                gridy = 0;
                insets = new Insets(0, 20, 0, 0);
                anchor = EAST;
            }});
            panel3.add(posAnglePanel, cc.xywh(1, 13, 3, 1));

            // Create the parallactic angle panel and an empty panel underneath to absorb all vertical space.
            parallacticAnglePanel = new ParallacticAnglePanel();
            panel3.add(parallacticAnglePanel, new CellConstraints(1, 15, 3, 3, CellConstraints.LEFT, CellConstraints.DEFAULT, new Insets( 0, 11, 0, 0)));


            // ---- Focal Plane Unit ----
            panel3.add(goodiesFormsSeparator5, cc.xywh(5, 11, 3, 1));

            //---- focalPlaneBuiltInButton ----
            focalPlaneBuiltInButton.setText("Built-in");
            panel3.add(focalPlaneBuiltInButton, new CellConstraints(5, 13, 1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT, new Insets( 0, 11, 0, 0)));
            //panel3.add(focalPlaneBuiltInButton, cc.xy(5, 13));
            panel3.add(builtinComboBox, cc.xy(7, 13));

            //---- focalPlaneMaskButton ----
            focalPlaneMaskButton.setText("Custom Mask MDF");
            panel3.add(focalPlaneMaskButton, new CellConstraints(5, 15, 1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT, new Insets( 0, 11, 0, 0)));
            //panel3.add(focalPlaneMaskButton, cc.xy(5, 15));

            //---- focalPlaneMask ----
            focalPlaneMask.setToolTipText("Enter the name of the custom mask");
            panel3.add(focalPlaneMask, cc.xy(7, 15));

            //---- focalPlaneMaskPlotButton ----
            focalPlaneMaskPlotButton.setText("Plot...");
            focalPlaneMaskPlotButton.setToolTipText("Plot a custom mask file in the position editor (FITS format)");
            panel3.add(focalPlaneMaskPlotButton, new CellConstraints(9, 15, 1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT, new Insets( 0, 11, 0, 0)));

            //---- label4 ----
            JLabel slitWidthLabel = new JLabel("Slit Width");
            panel3.add(slitWidthLabel, new CellConstraints(5, 17, 1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT, new Insets( 0, 35, 0, 0)));
            panel3.add(customSlitWidthComboBox, cc.xy(7, 17));
        }
        add(panel3, cc.xy(1, 3));


        //======== tabbedPane ========
        {
            tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

            //======== panel6 ========
            {
                panel6.setLayout(new FormLayout(
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
                    panel12.setLayout(new FormLayout(
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
                    label10.setText("X Binning");
                    panel12.add(label10, cc.xy(1, 1));
                    panel12.add(xBinComboBox, cc.xy(3, 1));

                    //---- label11 ----
                    label11.setText("Y Binning");
                    panel12.add(label11, cc.xy(5, 1));
                    panel12.add(yBinComboBox, cc.xy(7, 1));
                }
                panel6.add(panel12, cc.xywh(3, 3, 3, 1));
                panel6.add(goodiesFormsSeparator3, cc.xywh(3, 5, 3, 1));

                //======== panel7 ========
                {
                    panel7.setLayout(new FormLayout(
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
                }
                panel6.add(panel7, cc.xy(5, 7));

                //======== panel11 ========
                {
                    panel11.setLayout(new FormLayout(
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
                    label12.setText("Resulting CCD Gain:");
                    panel11.add(label12, cc.xy(1, 1));

                    //---- ccdGainLabel ----
                    ccdGainLabel.setText("2");
                    panel11.add(ccdGainLabel, cc.xy(4, 1));

                    //---- meanReadNoiseLabelLabel ----
                    meanReadNoiseLabelLabel.setText("Mean read noise:");
                    panel11.add(meanReadNoiseLabelLabel, cc.xy(1, 3));

                    //---- meanReadNoiseLabel ----
                    meanReadNoiseLabel.setText("3.4");
                    panel11.add(meanReadNoiseLabel, cc.xy(4, 3));

                    //---- ampCountLabel ----
                    ampCountLabel.setText("3.4");
                    panel11.add(ampCountLabel, cc.xy(4, 5));

                    //---- ampCountLabelLabel ----
                    ampCountLabelLabel.setText("Number of amps:");
                    panel11.add(ampCountLabelLabel, cc.xy(1, 5));
                }
                panel6.add(panel11, cc.xywh(3, 9, 3, 1));
                warning4.setForeground(Color.red);
                panel6.add(warning4, cc.xywh(3,11,3,1));
            }
            tabbedPane.addTab("CCD Readout", panel6);


            //======== panel14 ========
            {
                panel14.setLayout(new FormLayout(
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
                label8.setText("Detector translation assembly (DTA-X) offset:");
                panel14.add(label8, cc.xy(3, 11));

                //---- transDtaSpinner ----
                transDtaSpinner.setToolTipText("Specify the DTA-X offset in unbinned pixels (range: ?6 pixels)");
                panel14.add(transDtaSpinner, cc.xy(5, 11));

                //---- warning3 ----
                warning3.setText("Warning");
                warning3.setForeground(Color.red);
                panel14.add(warning3, cc.xywh(3, 13, 5, 1));
            }
            tabbedPane.addTab("Translation Stage", panel14);


            //======== panel15 ========
            {
                panel15.setLayout(new FormLayout(
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

                customROIPanel.setLayout(new FormLayout(
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
                //======== scrollPane1 ========
                {
                    scrollPaneROI.setViewportView(customROITable);
                }
                customROIPanel.setBorder(BorderFactory.createTitledBorder("Custom ROIs"));
                customROIPanel.add(scrollPaneROI, cc.xywh(1, 1, 4, 5));

                panel15.add(customROIPanel,cc.xywh(4,3,1,11));
            }
            tabbedPane.addTab("Regions of Interest", panel15);


            //======== panel16 ========
            {
                panel16.setLayout(new FormLayout(
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
            }
            tabbedPane.addTab("ISS Port", panel16);


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
                nsPanel.add(goodiesFormsSeparator1, cc.xywh(3, 3, 9, 1));

                //======== scrollPane1 ========
                {
                    scrollPane1.setViewportView(offsetTable);
                }
                nsPanel.add(scrollPane1, cc.xywh(7, 5, 5, 5));

                //---- label13 ----
                label13.setText("p");
                nsPanel.add(label13, cc.xy(3, 5));

                //---- xOffset ----
                xOffset.setToolTipText("The nod offset from the base position RA in arcsec");
                nsPanel.add(xOffset, cc.xywh(5, 5, 1, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

                //---- label14 ----
                label14.setText("q");
                nsPanel.add(label14, cc.xy(3, 7));

                //---- yOffset ----
                yOffset.setToolTipText("The nod offset from the base position Dec in arcsec");
                nsPanel.add(yOffset, cc.xywh(5, 7, 1, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

                //---- label15 ----
                label15.setText("OIWFS");
                nsPanel.add(label15, cc.xy(3, 9));

                //---- oiwfsBox ----
                oiwfsBox.setToolTipText("The OIWFS setting to use for the selected nod offset");
                nsPanel.add(oiwfsBox, cc.xy(5, 9));

                //======== panel2 ========
                {
                    panel2.setLayout(new FormLayout(
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
                }
                nsPanel.add(panel2, cc.xywh(1, 11, 11, 1));

                //---- label16 ----
                label16.setText("Offset (arcsec)");
                nsPanel.add(label16, cc.xywh(3, 13, 3, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

                //---- shuffleOffset ----
                shuffleOffset.setToolTipText("The shuffle offset in arcsec");
                nsPanel.add(shuffleOffset, cc.xy(7, 13));

                //---- label18 ----
                label18.setText("Offset (detector rows)");
                nsPanel.add(label18, cc.xy(9, 13));

                //---- detectorRows ----
                detectorRows.setToolTipText("The shuffle offset in detector rows");
                nsPanel.add(detectorRows, cc.xy(11, 13));

                //---- label17 ----
                label17.setText("Number of N&S Cycles");
                nsPanel.add(label17, cc.xywh(3, 15, 3, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

                //---- numNSCycles ----
                numNSCycles.setToolTipText("The number of nod & shuffle cycles");
                nsPanel.add(numNSCycles, cc.xy(7, 15));

                //---- totalTimeUnitsLabel ----
                totalTimeUnitsLabel.setText("Total Observe Time (sec)");
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
        add(tabbedPane, cc.xy(1, 5));

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

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JPanel panel3;
    private JLabel label1;
    JComboBox filterComboBox;
    private JLabel label2;
    TextBoxWidget exposureTime;
    private JLabel label3;
    JComboBox disperserComboBox;
    JLabel centralWavelengthLabel;
    TextBoxWidget centralWavelength;
    JLabel orderLabel;
    private JPanel panel1;
    DropDownListBoxWidget orderComboBox;
    JCheckBox preImgCheckButton;
    JCheckBox nsCheckButton;
    private JLabel detectorManufacturerLabel;
    protected JComboBox detectorManufacturerComboBox;
    JLabel warning1;
    private JComponent goodiesFormsSeparator4;
    NumberBoxWidget posAngle;
    JCheckBox posAngle180;
    private JComponent goodiesFormsSeparator5;
    JRadioButton focalPlaneBuiltInButton;
    DropDownListBoxWidget builtinComboBox;
    JRadioButton focalPlaneMaskButton;
    TextBoxWidget focalPlaneMask;
    JButton focalPlaneMaskPlotButton;
    private JLabel label4;
    DropDownListBoxWidget customSlitWidthComboBox;
    JTabbedPane tabbedPane;
    private JPanel panel6;
    private JPanel panel12;
    private JLabel label10;
    DropDownListBoxWidget xBinComboBox;
    private JLabel label11;
    DropDownListBoxWidget yBinComboBox;
    private JComponent goodiesFormsSeparator3;
    private JPanel panel7;
    JRadioButton ccdSlowLowButton;
    JRadioButton ccd3AmpButton;
    JRadioButton ccdFastLowButton;
    JRadioButton ccd6AmpButton;
    JRadioButton ccd12AmpButton;
    JRadioButton ccdFastHighButton;
    JRadioButton ccdSlowHighButton;
    private JPanel panel11;
    private JLabel label12;
    JLabel ccdGainLabel;
    private JLabel meanReadNoiseLabelLabel;
    JLabel meanReadNoiseLabel;
    JLabel ampCountLabel;
    private JLabel ampCountLabelLabel;
    private JPanel panel14;
    JRadioButton transFollowXYButton;
    JRadioButton transFollowXYZButton;
    JRadioButton transFollowZButton;
    JRadioButton transNoFollowButton;
    private JLabel label8;
    JSpinner transDtaSpinner;
    JLabel warning3;
    private JPanel panel15;
    JRadioButton noROIButton;
    JRadioButton ccd2Button;
    JRadioButton centralSpectrumButton;
    JRadioButton centralStampButton;
    JRadioButton customButton;
    JLabel warningCustomROI;
    private JPanel customROIPanel;
    private JScrollPane scrollPaneROI;
    GmosCustomROITableWidget customROITable;
    NumberBoxWidget xMin;
    NumberBoxWidget yMin;
    NumberBoxWidget xRange;
    NumberBoxWidget yRange;
    JButton customROINewButton;
    JButton customROIPasteButton;
    JButton customROIRemoveButton;
    JButton customROIRemoveAllButton;
    private JPanel panel16;
    JRadioButton upLookingButton;
    JRadioButton sideLookingButton;
    JPanel nsPanel;
    private JComponent goodiesFormsSeparator1;
    private JScrollPane scrollPane1;
    GmosOffsetPosTableWidget offsetTable;
    private JLabel label13;
    NumberBoxWidget xOffset;
    private JLabel label14;
    NumberBoxWidget yOffset;
    private JLabel label15;
    DropDownListBoxWidget oiwfsBox;
    private JPanel panel2;
    JCheckBox electronicOffsetCheckBox;
    JButton newButton;
    JButton removeButton;
    JButton removeAllButton;
    private JLabel label16;
    NumberBoxWidget shuffleOffset;
    private JLabel label18;
    NumberBoxWidget detectorRows;
    private JLabel label17;
    NumberBoxWidget numNSCycles;
    private JLabel totalTimeUnitsLabel;
    JTextField totalTime;
    JLabel warning2;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    JLabel warning4;
    JLabel readNoiseLabelLabel;
    JLabel readNoiseLabel;

    ParallacticAnglePanel parallacticAnglePanel;
}
