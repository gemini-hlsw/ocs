/**
 * Title:        JSky<p>
 * Description:  <p>
 * Copyright:    Copyright (c) Allan Brighton<p>
 * Company:      <p>
 * @author Allan Brighton
 * @version 1.0
 */
package jsky.app.ot.gemini.gmos;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import jsky.util.gui.*;

import java.awt.event.*;

public class GmosGUI extends JPanel {
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel filterLabel = new JLabel();
    JLabel exposureTimeLabel = new JLabel();
    jsky.util.gui.DropDownListBoxWidget filterComboBox = new DropDownListBoxWidget();
    NumberBoxWidget exposureTime = new NumberBoxWidget();
    JLabel jLabel3 = new JLabel();
    JLabel disperserLabel = new JLabel();
    DropDownListBoxWidget disperserComboBox = new DropDownListBoxWidget();
    JLabel centralWavelengthLabel = new JLabel();
    NumberBoxWidget centralWavelength = new NumberBoxWidget();
    JLabel orderLabel = new JLabel();
    JPanel jPanel1 = new JPanel();
    TitledBorder titledBorder1;
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    OptionWidget posAngleSetButton = new OptionWidget();
    OptionWidget posAngleFollowButton = new OptionWidget();
    DropDownListBoxWidget orderComboBox = new DropDownListBoxWidget();
    OptionWidget posAngleAverageButton = new OptionWidget();
    NumberBoxWidget posAngle = new NumberBoxWidget();
    JPanel jPanel2 = new JPanel();
    OptionWidget focalPlaneMaskButton = new OptionWidget();
    OptionWidget focalPlaneBuiltInButton = new OptionWidget();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    DropDownListBoxWidget builtinComboBox = new DropDownListBoxWidget();
    TextBoxWidget focalPlaneMask = new TextBoxWidget();
    JTabbedPane tabbedPane = new JTabbedPane();
    JPanel ccdReadoutDetailsPanel = new JPanel();
    JPanel adcDetailsPanel = new JPanel();
    JPanel transStageDetailsPanel = new JPanel();
    GridBagLayout gridBagLayout4 = new GridBagLayout();
    JLabel jLabel5 = new JLabel();
    DropDownListBoxWidget xBinComboBox = new DropDownListBoxWidget();
    JLabel jLabel7 = new JLabel();
    DropDownListBoxWidget yBinComboBox = new DropDownListBoxWidget();
    JPanel ccdReadoutCharPanel = new JPanel();
    TitledBorder titledBorder2;
    GridBagLayout gridBagLayout5 = new GridBagLayout();
    JRadioButton ccdSlowButton = new JRadioButton();
    JRadioButton ccd3AmpButton = new JRadioButton();
    JRadioButton ccdLowGainButton = new JRadioButton();
    JRadioButton ccdFastButton = new JRadioButton();
    JRadioButton ccd6AmpButton = new JRadioButton();
    JRadioButton ccdHighGainButton = new JRadioButton();
    GridBagLayout gridBagLayout6 = new GridBagLayout();
    JPanel jPanel7 = new JPanel();
    GridBagLayout gridBagLayout7 = new GridBagLayout();
    JRadioButton adcNoCorrectionButton = new JRadioButton();
    JRadioButton adcBestCorrectionButton = new JRadioButton();
    JRadioButton adcFollowButton = new JRadioButton();
    GridBagLayout gridBagLayout8 = new GridBagLayout();
    JRadioButton transNoFollowButton = new JRadioButton();
    JRadioButton transFollowXYZButton = new JRadioButton();
    JRadioButton transFollowXYButton = new JRadioButton();
    GridBagLayout gridBagLayout9 = new GridBagLayout();
    JPanel jPanel8 = new JPanel();
    TitledBorder titledBorder4;
    JPanel jPanel3 = new JPanel();
    GridBagLayout gridBagLayout10 = new GridBagLayout();
    JLabel jLabel10 = new JLabel();
    JLabel ccdGainLabel = new JLabel();
    JPanel top1 = new JPanel();
    GridBagLayout gridBagLayout11 = new GridBagLayout();
    GridBagLayout gridBagLayout12 = new GridBagLayout();
    TitledBorder titledBorder3;
    JLabel warning1 = new JLabel();
    JLabel warning2 = new JLabel();
    JPanel roiPanel = new JPanel();
    ButtonGroup roiGroup = new ButtonGroup();
    GridBagLayout gridBagLayout13 = new GridBagLayout();
    JRadioButton ccd2Button = new JRadioButton();
    JRadioButton centralSpectrumButton = new JRadioButton();
    JRadioButton centralStampButton = new JRadioButton();
    JRadioButton topSpectrumButton = new JRadioButton();
    JRadioButton bottomSpectrumButton = new JRadioButton();
    JRadioButton noROIButton = new JRadioButton();
    JLabel jLabel1 = new JLabel();
    JLabel centralWavelengthUnits = new JLabel();
    JPanel portPanel = new JPanel();
    GridBagLayout gridBagLayout14 = new GridBagLayout();
    JRadioButton upLookingButton = new JRadioButton();
    JRadioButton sideLookingButton = new JRadioButton();
    ButtonGroup portButtonGroup = new ButtonGroup();
    ButtonGroup posAngleButtonGroup = new ButtonGroup();
    ButtonGroup fpuButtonGroup = new ButtonGroup();
    ButtonGroup ccdSlowFastButtonGroup = new ButtonGroup();
    ButtonGroup ccdAmpButtonGroup = new ButtonGroup();
    ButtonGroup ccdGainButtonGroup = new ButtonGroup();
    ButtonGroup adcButtonGroup = new ButtonGroup();
    ButtonGroup transStageButtonGroup = new ButtonGroup();
    JButton focalPlaneMaskPlotButton = new JButton();
    JPanel nsPanel = new JPanel();
    GridBagLayout gridBagLayout15 = new GridBagLayout();
    DropDownListBoxWidget oiwfsBox = new DropDownListBoxWidget();
    JLabel nodLabel = new JLabel();
    JLabel pLabel = new JLabel();
    GmosOffsetPosTableWidget offsetTable = new GmosOffsetPosTableWidget();
    NumberBoxWidget xOffset = new NumberBoxWidget();
    NumberBoxWidget yOffset = new NumberBoxWidget();
    JLabel qLabel = new JLabel();
    JScrollPane nsTableScrollPane = new JScrollPane();
    JLabel oiwfsLabel = new JLabel();
    GridBagLayout gridBagLayout16 = new GridBagLayout();
    JLabel numNSCyclesLabel = new JLabel();
    NumberBoxWidget numNSCycles = new NumberBoxWidget();
    Component component4;
    Component component5;
    Component component6;
    Component component7;
    JLabel nsLabel = new JLabel();
    JRadioButton nsYesRadioButton = new JRadioButton();
    JRadioButton nsNoRadioButton = new JRadioButton();
    ButtonGroup nsButtonGroup = new ButtonGroup();
    NumberBoxWidget shuffleOffset = new NumberBoxWidget();
    JLabel shuffleOffsetUnitsLabel = new JLabel();
    NumberBoxWidget detectorRows = new NumberBoxWidget();
    JLabel detectorRowsLabel = new JLabel();
    Component component1;
    JPanel nsLowerPanel = new JPanel();
    GridBagLayout gridBagLayout18 = new GridBagLayout();
    JLabel totalTimeUnitsLabel = new JLabel();
    JTextField totalTime = new JTextField();
    JLabel totalTimeUnits = new JLabel();
    JPanel nsOffsetTableButtonPanel = new JPanel();
    JButton newButton = new JButton();
    JButton removeButton = new JButton();
    JButton removeAllButton = new JButton();
    JLabel dtaLabel = new JLabel();
    JSpinner transDtaSpinner = new JSpinner();
    JLabel warning3 = new JLabel();
    Component component2;
    JRadioButton transFollowZButton = new JRadioButton();
    JCheckBox electronicOffsetCheckBox = new JCheckBox();
    JLabel offsetLabel = new JLabel();

    public GmosGUI() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        component1 = Box.createVerticalStrut(8);
        component2 = Box.createVerticalStrut(8);
        shuffleOffsetUnitsLabel.setText("arcsec");
        detectorRowsLabel.setText("detector rows");
        numNSCycles.setMinimumSize(new Dimension(80, 21));
        numNSCycles.setPreferredSize(new Dimension(80, 21));
        numNSCycles.setToolTipText("The number of nod & shuffle cycles");
        nsPanel.setLayout(gridBagLayout15);
        yOffset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                yOffset_actionPerformed(e);
            }
        });
        shuffleOffset.setMinimumSize(new Dimension(80, 21));
        shuffleOffset.setPreferredSize(new Dimension(80, 21));
        shuffleOffset.setToolTipText("The shuffle offset in arcsec");
        detectorRows.setMinimumSize(new Dimension(100, 21));
        detectorRows.setPreferredSize(new Dimension(100, 21));
        detectorRows.setToolTipText("The shuffle offset in detector rows");
        nsLowerPanel.setLayout(gridBagLayout18);
        totalTimeUnitsLabel.setText("Total Observe Time:");
        totalTime.setMinimumSize(new Dimension(80, 21));
        totalTime.setPreferredSize(new Dimension(100, 21));
        totalTime.setToolTipText("The total observe time in seconds");
        totalTime.setEditable(false);
        totalTimeUnits.setText("sec");
        xOffset.setMinimumSize(new Dimension(80, 21));
        yOffset.setMinimumSize(new Dimension(80, 21));
        yOffset.setPreferredSize(new Dimension(80, 21));
        newButton.setToolTipText("Add a new nod offset position");
        newButton.setText("New");
        removeButton.setToolTipText("Remove the selected nod offset positions");
        removeButton.setText("Remove");
        removeAllButton.setToolTipText("Remove all nod offset positions");
        removeAllButton.setText("Remove All");
        nsYesRadioButton.setToolTipText("Turn on Nod & Shuffle mode");
        electronicOffsetCheckBox.setToolTipText("Enable or disable electronic offsetting");
        nsNoRadioButton.setToolTipText("Turn off Nod & Shuffle mode");
        orderComboBox.setToolTipText("Select the disperser order");
        posAngle.setToolTipText("Set the position angle in degrees east of north");
        focalPlaneMask.setToolTipText("Enter the name of the custom mask");
        dtaLabel.setForeground(Color.black);
        dtaLabel.setToolTipText("Specify the DTA-X offset in unbinned pixels (range: ?6 pixels)");
        dtaLabel.setText("Detector translation assembly (DTA-X) offset:");
        transDtaSpinner.setToolTipText("Specify the DTA-X offset in unbinned pixels (range: ?6 pixels)");
        warning3.setFont(new java.awt.Font("Dialog", 0, 12));
        warning3.setForeground(Color.red);
        warning3.setText("Warning:");
        transFollowZButton.setText("Follow in Z Only");
        electronicOffsetCheckBox.setEnabled(false);
        electronicOffsetCheckBox.setText("Use Electronic Offsetting?");
        offsetLabel.setText("Offset");
        warning2.setVerticalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel5.setToolTipText("");
    focalPlaneMaskButton.addActionListener(new GmosGUI_focalPlaneMaskButton_actionAdapter(this));
    focalPlaneBuiltInButton.addActionListener(new GmosGUI_focalPlaneBuiltInButton_actionAdapter(this));
    nsLowerPanel.add(numNSCyclesLabel,      new GridBagConstraints(1, 6, 3, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        nsLowerPanel.add(warning2,      new GridBagConstraints(0, 8, 6, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 11, 2, 5), 0, 0));
        nsLowerPanel.add(numNSCycles,   new GridBagConstraints(0, 7, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 6, 0), 0, 0));
        nsLowerPanel.add(shuffleOffsetUnitsLabel,       new GridBagConstraints(3, 5, 1, 1, 0.1, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 3, 0, 0), 0, 0));
        nsLowerPanel.add(detectorRows,   new GridBagConstraints(4, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        nsLowerPanel.add(detectorRowsLabel,       new GridBagConstraints(5, 5, 1, 1, 0.3, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 3, 0, 0), 0, 0));
        nsLowerPanel.add(shuffleOffset,     new GridBagConstraints(0, 5, 3, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        nsLowerPanel.add(totalTimeUnitsLabel,   new GridBagConstraints(4, 6, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        nsLowerPanel.add(totalTime,   new GridBagConstraints(4, 7, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 6, 0), 0, 0));
        nsLowerPanel.add(totalTimeUnits,   new GridBagConstraints(5, 7, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 3, 0, 0), 0, 0));
        nsLowerPanel.add(offsetLabel,            new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        nsPanel.add(nsTableScrollPane, new GridBagConstraints(2, 0, 2, 4, 1.0, 1.0
            ,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(6, 11, 0, 0), 0, 0));
        nsPanel.add(electronicOffsetCheckBox, new GridBagConstraints(0, 4, 3, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        nsTableScrollPane.getViewport().add(offsetTable, null);
        nsPanel.add(nsOffsetTableButtonPanel, new GridBagConstraints(2, 4, 2, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        titledBorder1 = new TitledBorder(new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)), "Position Angle");
        titledBorder2 = new TitledBorder(new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)), "Set the CCD Readout Characteristics");
        titledBorder4 = new TitledBorder(new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)), "Focal Plane Unit");
        titledBorder3 = new TitledBorder(new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)), "Disperser");
        component4 = Box.createVerticalStrut(8);
        component5 = Box.createVerticalStrut(8);
        component6 = Box.createVerticalStrut(8);
        component7 = Box.createVerticalStrut(8);
        filterLabel.setLabelFor(filterComboBox);
        filterLabel.setText("Filter");
        this.setMinimumSize(new Dimension(400, 453));
        this.setPreferredSize(new Dimension(400, 453));
        this.setToolTipText("");
        this.setLayout(gridBagLayout1);
        exposureTimeLabel.setLabelFor(exposureTime);
        exposureTimeLabel.setText("Exposure Time");
        exposureTime.setMaximumSize(new Dimension(1000, 1000));
        exposureTime.setMinimumSize(new Dimension(80, 21));
        exposureTime.setPreferredSize(new Dimension(80, 21));
        exposureTime.setAllowNegative(false);
        exposureTime.setToolTipText("Enter the exposure time in seconds");
        jLabel3.setText("sec");
        disperserLabel.setLabelFor(disperserComboBox);
        disperserLabel.setText("Disperser");
        centralWavelengthLabel.setLabelFor(centralWavelength);
        centralWavelengthLabel.setText("Grating Central Wavelength");
        orderLabel.setToolTipText("Instrument Support Structure (ISS) Port");
        orderLabel.setLabelFor(orderComboBox);
        orderLabel.setText("Order");
        jPanel1.setBorder(titledBorder1);
        jPanel1.setLayout(gridBagLayout2);
        posAngleSetButton.setSelected(true);
        posAngleSetButton.setText("Set to");
        posAngleFollowButton.setText("Follow in Parallactic Angle");
        filterComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        orderComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        disperserComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        builtinComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        xBinComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        yBinComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        centralWavelength.setMinimumSize(new Dimension(80, 21));
        centralWavelength.setPreferredSize(new Dimension(80, 21));
        centralWavelength.setToolTipText("Set the Grating Central Wavelength in Nanometers");
        centralWavelength.setAllowNegative(false);
        posAngleAverageButton.setText("Use Average Parallactic Angle");
        posAngle.setMinimumSize(new Dimension(80, 21));
        posAngle.setPreferredSize(new Dimension(80, 21));
        jPanel2.setLayout(gridBagLayout3);
        jPanel2.setBorder(titledBorder4);
        focalPlaneMaskButton.setText("Custom Mask MDF");
        focalPlaneBuiltInButton.setSelected(true);
        focalPlaneBuiltInButton.setText("Built-in");
        focalPlaneMask.setEnabled(false);
        focalPlaneMask.setMinimumSize(new Dimension(130, 21));
        focalPlaneMask.setPreferredSize(new Dimension(130, 21));
        ccdReadoutDetailsPanel.setLayout(gridBagLayout4);
        jLabel5.setText("X Binning");
        jLabel7.setText("Y Binning");
        ccdReadoutCharPanel.setBorder(titledBorder2);
        ccdReadoutCharPanel.setLayout(gridBagLayout5);
        ccdSlowButton.setSelected(true);
        ccdSlowButton.setText("Slow");
        ccd3AmpButton.setSelected(true);
        ccd3AmpButton.setText("Use 3 Amplifiers");
        ccd3AmpButton.setToolTipText("");
        ccdLowGainButton.setSelected(true);
        ccdLowGainButton.setText("Low Gain");
        ccdFastButton.setText("Fast");
        ccd6AmpButton.setText("Use 6 Amplifiers");
        ccdHighGainButton.setText("High Gain");
        adcDetailsPanel.setLayout(gridBagLayout6);
        jPanel7.setLayout(gridBagLayout7);
        adcNoCorrectionButton.setSelected(true);
        adcNoCorrectionButton.setText("No Correction");
        adcBestCorrectionButton.setText("Best Static Correction");
        adcFollowButton.setText("Follow During the Exposure");
        transStageDetailsPanel.setLayout(gridBagLayout8);
        transNoFollowButton.setText("Do Not Follow");
        transFollowXYZButton.setText("Follow in X, Y and Z (focus)");
        transFollowXYButton.setSelected(true);
        transFollowXYButton.setText("Follow in X and Y");
        jPanel8.setLayout(gridBagLayout9);
        xBinComboBox.setMinimumSize(new Dimension(40, 22));
        xBinComboBox.setPreferredSize(new Dimension(40, 22));
        yBinComboBox.setMinimumSize(new Dimension(40, 22));
        yBinComboBox.setPreferredSize(new Dimension(40, 22));
        orderComboBox.setMinimumSize(new Dimension(40, 22));
        orderComboBox.setPreferredSize(new Dimension(40, 22));
        jPanel3.setLayout(gridBagLayout10);
        jLabel10.setText("Resulting CCD Gain:");
        ccdGainLabel.setText("2");
        top1.setLayout(gridBagLayout11);
        warning1.setFont(new java.awt.Font("Dialog", 0, 12));
        warning1.setForeground(Color.red);
        warning1.setText("Warning:");
        warning2.setFont(new java.awt.Font("Dialog", 0, 12));
        warning2.setForeground(Color.red);
        warning2.setText("Warning:");
        roiPanel.setLayout(gridBagLayout13);
        ccd2Button.setText("CCD2");
        centralSpectrumButton.setText("Central Spectrum");
        centralStampButton.setText("Central Stamp");
        topSpectrumButton.setText("Top Spectrum");
        bottomSpectrumButton.setText("Bottom Spectrum");
        noROIButton.setSelected(true);
        noROIButton.setText("Full Frame Readout");
        jLabel1.setText("deg E of N");
        centralWavelengthUnits.setToolTipText("");
        centralWavelengthUnits.setText("nanometers");
        roiPanel.setToolTipText("Regions of Interest");
        transStageDetailsPanel.setToolTipText("Translation Stage Details");
        adcDetailsPanel.setToolTipText("Atmospheric Dispersion Corrector  (ADC) Details");
        portPanel.setLayout(gridBagLayout14);
        upLookingButton.setText("Up-looking");
        sideLookingButton.setSelected(true);
        sideLookingButton.setText("Side-looking");
        focalPlaneMaskPlotButton.setToolTipText("Plot a custom mask file in the position editor (FITS format)");
        focalPlaneMaskPlotButton.setText("Plot...");
        oiwfsBox.setToolTipText("The OIWFS setting to use for the selected nod offset");
        nodLabel.setText("Nod (arcsec)");
        pLabel.setLabelFor(xOffset);
        pLabel.setText("p");
        xOffset.setToolTipText("The nod offset from the base position RA in arcsec");
        yOffset.setToolTipText("The nod offset from the base position Dec in arcsec");
        qLabel.setLabelFor(yOffset);
        qLabel.setText("q");
        nsTableScrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
        nsTableScrollPane.setPreferredSize(new Dimension(10, 10));
        nsTableScrollPane.setToolTipText("Nod offset positions and the associated guide probe settings");
        oiwfsLabel.setLabelFor(oiwfsBox);
        oiwfsLabel.setText("OIWFS");
        numNSCyclesLabel.setText("Number of N&S Cycles");
        nsLabel.setText("Use Nod & Shuffle");
        nsYesRadioButton.setText("Yes");
        nsNoRadioButton.setSelected(true);
        nsNoRadioButton.setText("No");
        top1.add(filterLabel, new GridBagConstraints(0, 0, 1, 1, 0.2, 0.0
                                                     , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        top1.add(exposureTimeLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
                                                           , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        top1.add(filterComboBox, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                                                        , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(exposureTime, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
                                                      , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(jLabel3,  new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 3, 0, 0), 0, 0));
        top1.add(disperserLabel, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0
                                                        , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        top1.add(disperserComboBox, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
                                                           , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(centralWavelengthLabel, new GridBagConstraints(1, 2, 2, 1, 1.0, 0.0
                                                                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        top1.add(centralWavelength, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
                                                           , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(orderLabel,  new GridBagConstraints(3, 2, 2, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 0, 0, 0), 0, 0));
        this.add(jPanel1, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0
                                                 , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 6, 0, 6), 0, 0));
        jPanel1.add(posAngleSetButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0
                                                              , GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 11, 0, 0), 0, 0));
        jPanel1.add(posAngleFollowButton, new GridBagConstraints(0, 2, 3, 1, 1.0, 1.0
                                                                 , GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(3, 11, 0, 0), 1, 1));
        jPanel1.add(posAngleAverageButton, new GridBagConstraints(0, 1, 3, 1, 1.0, 1.0
                                                                  , GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(3, 11, 0, 0), 0, 0));
        jPanel1.add(posAngle, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
                                                     , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(jLabel1,  new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 3, 0, 6), 0, 0));
        top1.add(orderComboBox,  new GridBagConstraints(3, 3, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        top1.add(warning1, new GridBagConstraints(0, 4, 5, 1, 0.0, 0.0
                                                  , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 11, 2, 5), 0, 0));
        top1.add(centralWavelengthUnits,  new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 3, 0, 0), 0, 0));
        top1.add(nsLabel, new GridBagConstraints(3, 0, 2, 1, 0.0, 0.0
                                                 , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        top1.add(nsYesRadioButton, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0
                                                          , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(nsNoRadioButton, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0
                                                         , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        this.add(jPanel2, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
                                                 , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 6, 0, 6), 0, 0));
        jPanel2.add(focalPlaneBuiltInButton,  new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 6, 0, 0), 0, 0));
        jPanel2.add(focalPlaneMaskButton,  new GridBagConstraints(0, 1, 1, 1, 0.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(3, 6, 0, 0), 0, 0));
        jPanel2.add(builtinComboBox,  new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 3, 0, 6), 0, 0));
        jPanel2.add(focalPlaneMask,    new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 3, 0, 6), 0, 0));
        jPanel2.add(focalPlaneMaskPlotButton, new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0
                                                                     , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(3, 0, 0, 0), 0, 0));
        this.add(tabbedPane, new GridBagConstraints(0, 3, 2, 1, 1.0, 1.0
                                                    , GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(3, 6, 0, 6), 0, -9));
        tabbedPane.add(ccdReadoutDetailsPanel, "CCD Readout");
        ccdReadoutDetailsPanel.add(jLabel5, new GridBagConstraints(0, 1, 2, 1, 0.0, 1.0
                                                                   , GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 11, 0, 11), 0, 0));
        ccdReadoutDetailsPanel.add(xBinComboBox, new GridBagConstraints(2, 0, 1, 2, 1.0, 0.0
                                                                        , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
        ccdReadoutDetailsPanel.add(jLabel7, new GridBagConstraints(3, 0, 1, 2, 0.0, 1.0
                                                                   , GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 20, 0, 11), 0, 0));
        ccdReadoutDetailsPanel.add(yBinComboBox, new GridBagConstraints(4, 0, 1, 2, 1.0, 0.0
                                                                        , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 0, 10), 0, 0));
        ccdReadoutDetailsPanel.add(ccdReadoutCharPanel, new GridBagConstraints(0, 2, 5, 1, 1.0, 1.0
                                                                               , GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(0, 6, 0, 5), 0, 0));
        ccdReadoutCharPanel.add(ccdSlowButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
                                                                      , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
        ccdReadoutCharPanel.add(ccd3AmpButton, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
                                                                      , GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        ccdReadoutCharPanel.add(ccdLowGainButton, new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0
                                                                         , GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        ccdReadoutCharPanel.add(ccdFastButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 1.0
                                                                      , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
        ccdReadoutCharPanel.add(ccd6AmpButton, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
                                                                      , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        ccdReadoutCharPanel.add(ccdHighGainButton, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
                                                                          , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        ccdReadoutDetailsPanel.add(jPanel3, new GridBagConstraints(0, 4, 5, 1, 1.0, 1.0
                                                                   , GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 5, 0, 5), 0, 2));
        jPanel3.add(jLabel10, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0
                                                     , GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 11, 0, 5), 0, 0));
        jPanel3.add(ccdGainLabel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0
                                                         , GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 5, 0, 5), 0, 0));
        ccdReadoutDetailsPanel.add(component1, new GridBagConstraints(0, 5, 1, 1, 0.0, 1.0
                                                                      , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        tabbedPane.add(adcDetailsPanel, "ADC");
        adcDetailsPanel.add(jPanel7, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
                                                            , GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
        jPanel7.add(adcNoCorrectionButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
                                                                  , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 5), 0, 0));
        jPanel7.add(adcBestCorrectionButton, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
                                                                    , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 5), 0, 0));
        jPanel7.add(adcFollowButton, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0
                                                            , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 5), 0, 0));
        jPanel7.add(component6, new GridBagConstraints(0, 3, 1, 1, 0.0, 1.0
                                                       , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        tabbedPane.add(transStageDetailsPanel, "Translation Stage");
        transStageDetailsPanel.add(jPanel8, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
                                                                   , GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
        jPanel8.add(transFollowXYButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 1.0
                                                                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 5), 0, 0));
        jPanel8.add(transFollowXYZButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0
                                                                 , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 5), 0, 0));
        jPanel8.add(transNoFollowButton, new GridBagConstraints(0, 4, 1, 1, 0.0, 1.0
                                                                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 5), 0, 0));
        jPanel8.add(dtaLabel, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
                                                     , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 6), 0, 0));
        jPanel8.add(transDtaSpinner, new GridBagConstraints(1, 5, 1, 1, 1.0, 0.0
                                                            , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        jPanel8.add(warning3, new GridBagConstraints(0, 6, 2, 1, 0.0, 0.0
                                                     , GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 2, 0));
        tabbedPane.add(roiPanel, "Regions of Interest");
        roiPanel.add(ccd2Button, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
                                                        , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 5), 0, 0));
        roiPanel.add(centralSpectrumButton, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0
                                                                   , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 5), 0, 0));
        roiPanel.add(centralStampButton, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0
                                                                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
        roiPanel.add(topSpectrumButton, new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0
                                                               , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
        roiPanel.add(bottomSpectrumButton, new GridBagConstraints(1, 2, 1, 2, 1.0, 1.0
                                                                  , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
        roiPanel.add(noROIButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
                                                         , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 5), 0, 0));
        roiPanel.add(component4, new GridBagConstraints(0, 3, 1, 2, 0.0, 1.0
                                                        , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        roiPanel.add(component5, new GridBagConstraints(1, 4, 1, 1, 0.0, 1.0
                                                        , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        tabbedPane.add(portPanel, "ISS Port");
        this.add(top1, new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0
                                              , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        roiGroup.add(bottomSpectrumButton);
        roiGroup.add(topSpectrumButton);
        roiGroup.add(centralStampButton);
        roiGroup.add(centralSpectrumButton);
        roiGroup.add(ccd2Button);
        roiGroup.add(noROIButton);
        portPanel.add(upLookingButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
                                                              , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 6, 0, 0), 0, 0));
        portPanel.add(sideLookingButton, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
                                                                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 6, 0, 0), 0, 0));
        portPanel.add(component7, new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0
                                                         , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        tabbedPane.add(nsPanel, "Nod & Shuffle");
        portButtonGroup.add(upLookingButton);
        portButtonGroup.add(sideLookingButton);
        posAngleButtonGroup.add(posAngleSetButton);
        posAngleButtonGroup.add(posAngleAverageButton);
        posAngleButtonGroup.add(posAngleFollowButton);
        fpuButtonGroup.add(focalPlaneBuiltInButton);
        fpuButtonGroup.add(focalPlaneMaskButton);
        ccdSlowFastButtonGroup.add(ccdSlowButton);
        ccdSlowFastButtonGroup.add(ccdFastButton);
        ccdAmpButtonGroup.add(ccd3AmpButton);
        ccdAmpButtonGroup.add(ccd6AmpButton);
        ccdGainButtonGroup.add(ccdLowGainButton);
        ccdGainButtonGroup.add(ccdHighGainButton);
        adcButtonGroup.add(adcNoCorrectionButton);
        adcButtonGroup.add(adcBestCorrectionButton);
        adcButtonGroup.add(adcFollowButton);
        transStageButtonGroup.add(transFollowXYButton);
        transStageButtonGroup.add(transFollowXYZButton);
        transStageButtonGroup.add(transNoFollowButton);
        transStageButtonGroup.add(transFollowZButton);
        nsButtonGroup.add(nsYesRadioButton);
        nsButtonGroup.add(nsNoRadioButton);
        nsOffsetTableButtonPanel.add(newButton, null);
        nsOffsetTableButtonPanel.add(removeButton, null);
        nsOffsetTableButtonPanel.add(removeAllButton, null);
        nsPanel.add(oiwfsLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        nsPanel.add(oiwfsBox, new GridBagConstraints(1, 3, 1, 3, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(6, 11, 0, 0), 0, 0));
        nsPanel.add(nodLabel, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        nsPanel.add(pLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        nsPanel.add(xOffset, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        nsPanel.add(yOffset, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(6, 11, 0, 0), 0, 0));
        nsPanel.add(qLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        nsPanel.add(nsLowerPanel,      new GridBagConstraints(0, 6, 4, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(-6, 55, 0, 0), 0, 0));
        jPanel8.add(component2, new GridBagConstraints(0, 7, 1, 1, 0.0, 1.0
                                                       , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel8.add(transFollowZButton, new GridBagConstraints(0, 3, 1, 1, 0.0, 1.0
                                                               , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 5), 0, 0));
    }

    void yOffset_actionPerformed(ActionEvent e) {

    }

  void focalPlaneMaskButton_actionPerformed(ActionEvent e) {

  }

  void focalPlaneBuiltInButton_actionPerformed(ActionEvent e) {

  }
}

class GmosGUI_focalPlaneMaskButton_actionAdapter implements java.awt.event.ActionListener {
  GmosGUI adaptee;

  GmosGUI_focalPlaneMaskButton_actionAdapter(GmosGUI adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.focalPlaneMaskButton_actionPerformed(e);
  }
}

class GmosGUI_focalPlaneBuiltInButton_actionAdapter implements java.awt.event.ActionListener {
  GmosGUI adaptee;

  GmosGUI_focalPlaneBuiltInButton_actionAdapter(GmosGUI adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.focalPlaneBuiltInButton_actionPerformed(e);
  }
}
