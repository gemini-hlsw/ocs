/**
 * Title:        JSky<p>
 * Description:  <p>
 * Copyright:    Copyright (c) Allan Brighton<p>
 * Company:      <p>
 * @author Allan Brighton
 * @version 1.0
 */
package jsky.app.ot.gemini.gnirs;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import jsky.util.gui.*;
import jsky.util.gui.SingleSelectComboBox;

import java.awt.event.*;

public class GnirsGUI extends JPanel {

    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel pixelScaleLabel = new JLabel();
    JLabel filterLabel = new JLabel();
    JLabel exposureTimeLabel = new JLabel();
    SingleSelectComboBox pixelScale = new SingleSelectComboBox();
    SingleSelectComboBox filter = new SingleSelectComboBox();
    NumberBoxWidget exposureTime = new NumberBoxWidget();
    JLabel jLabel3 = new JLabel();
    JLabel disperserLabel = new JLabel();
    SingleSelectComboBox disperser = new SingleSelectComboBox();
    JLabel slitWidthLabel = new JLabel();
    JLabel centralWavelengthLabel = new JLabel();
    JTabbedPane tabbedPane = new JTabbedPane();
    JPanel top1 = new JPanel();
    GridBagLayout gridBagLayout11 = new GridBagLayout();
    GridBagLayout gridBagLayout12 = new GridBagLayout();
    GridBagLayout gridBagLayout16 = new GridBagLayout();
    JLabel coaddsLabel = new JLabel();
    NumberBoxWidget coadds = new NumberBoxWidget();
    JLabel coaddsUnitsLabel = new JLabel();
    SingleSelectComboBox slitWidth = new SingleSelectComboBox();
    DropDownListBoxWidget centralWavelength = new DropDownListBoxWidget();
    JLabel centralWavelengthUnitsLabel = new JLabel();
    JLabel crossDispersedLabel = new JLabel();
    JLabel posAngleUnitsLabel = new JLabel();
    JPanel crossDispersedPanel = new JPanel();
    JRadioButton crossDispersedNoRadioButton = new JRadioButton();
    JRadioButton crossDispersedYesRadioButton = new JRadioButton();
    GridBagLayout gridBagLayout17 = new GridBagLayout();
    JLabel wollastonPrismLabel = new JLabel();
    JPanel wollastonPrismPanel = new JPanel();
    JRadioButton wollastonPrismNoRadioButton = new JRadioButton();
    JRadioButton wollastonPrismYesRadioButton = new JRadioButton();
    GridBagLayout gridBagLayout19 = new GridBagLayout();
    ButtonGroup wollastonPrismButtonGroup = new ButtonGroup();
    ButtonGroup crossDispersedButtonGroup = new ButtonGroup();
    JLabel posAngleLabel = new JLabel();
    NumberBoxWidget posAngle = new NumberBoxWidget();
    JLabel scienceFovLabel = new JLabel();
    JLabel scienceFOV = new JLabel();
    JPanel readModeTab = new JPanel();
    JPanel crossDispersedTab = new JPanel();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    JRadioButton readModeBrightRadioButton = new JRadioButton();
    JLabel readModeBrightLabel = new JLabel();
    JRadioButton readModeFaintRadioButton = new JRadioButton();
    JRadioButton readModeVeryBrightRadioButton = new JRadioButton();
    JRadioButton readModeAcquisitionRadioButton = new JRadioButton();
    JRadioButton readModeVeryFaintRadioButton = new JRadioButton();
    JLabel readModeFaintLabel = new JLabel();
    JLabel readModeVeryBrightLabel = new JLabel();
    JLabel readModeVeryFaintLabel = new JLabel();
    JLabel biasLevelLabel = new JLabel();
    JLabel lowNoiseReadsLabel = new JLabel();
    JLabel minExpTimeLabel = new JLabel();
    JLabel minExpTime = new JLabel();
    ButtonGroup biasLevelButtonGroup = new ButtonGroup();
    GridBagLayout gridBagLayout4 = new GridBagLayout();
    ButtonGroup lowNoiseReadsButtonGroup = new ButtonGroup();
    GridBagLayout gridBagLayout6 = new GridBagLayout();
    JLabel crossDispersedCentralWavelengthsLabel = new JLabel();
    Component component1;
    ButtonGroup readModeButtonGroup = new ButtonGroup();
    JLabel lowNoiseReads = new JLabel();
    Component readModeFillComponent;
    JLabel biasLevel = new JLabel();
    JLabel readNoiseLabel = new JLabel();
    JLabel readNoise = new JLabel();
    JScrollPane orderTableScrollPane = new JScrollPane();
    JTable orderTable = new JTable();
    Component component2;
  Component component3;
  Component component4;

    public GnirsGUI() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        component1 = Box.createVerticalStrut(8);
        readModeFillComponent = Box.createVerticalStrut(8);
        component2 = Box.createHorizontalStrut(8);
        component3 = Box.createVerticalStrut(8);
    component4 = Box.createVerticalStrut(8);
    coadds.setText("");
        coaddsUnitsLabel.setText("exp/obs");
        centralWavelength.setPreferredSize(new Dimension(110, 25));
        centralWavelength.setToolTipText("Central Wavelength (Menu shows default for each band");
        centralWavelength.setEditable(true);
        centralWavelengthUnitsLabel.setRequestFocusEnabled(true);
        centralWavelengthUnitsLabel.setText("um");
        crossDispersedLabel.setText("Cross-dispersed");
        posAngleUnitsLabel.setText("deg E of N");
        crossDispersedNoRadioButton.setText("No");
        crossDispersedYesRadioButton.setText("Yes");
        crossDispersedPanel.setLayout(gridBagLayout17);
        wollastonPrismLabel.setText("Wollaston Prism");
        wollastonPrismNoRadioButton.setText("No");
        wollastonPrismYesRadioButton.setText("Yes");
        wollastonPrismPanel.setLayout(gridBagLayout19);
        posAngleLabel.setText("Position Angle");
        posAngle.setToolTipText("Set the position angle in degrees east of north");
        posAngle.setText("");
        scienceFovLabel.setText("Science FOV");
        scienceFOV.setRequestFocusEnabled(true);
        scienceFOV.setToolTipText("Science FOV: Calculated value, from slit width+pixel scale+IFU/XD/Woll");
        scienceFOV.setText("000.000 arcsecs");
        pixelScaleLabel.setLabelFor(pixelScale);
        pixelScaleLabel.setText("Pixel Scale");
        this.setMinimumSize(new Dimension(400, 453));
        this.setPreferredSize(new Dimension(400, 453));
        this.setToolTipText("");
        this.setLayout(gridBagLayout1);
        filterLabel.setLabelFor(filter);
        filterLabel.setText("Filter");
        exposureTimeLabel.setLabelFor(exposureTime);
        exposureTimeLabel.setText("Exposure Time");
        exposureTime.setMaximumSize(new Dimension(1000, 1000));
        exposureTime.setAllowNegative(false);
        exposureTime.setToolTipText("Enter the exposure time in seconds");
        jLabel3.setText("sec");
        disperserLabel.setLabelFor(disperser);
        disperserLabel.setText("Disperser");
        slitWidthLabel.setText("Slit Width/IFU");
        slitWidth.setMaximumRowCount(12);
        centralWavelengthLabel.setToolTipText("");
        centralWavelengthLabel.setText("Central Wavelength");
        pixelScale.setFont(new Font("Dialog", 0, 12));
        disperser.setFont(new Font("Dialog", 0, 12));
        top1.setLayout(gridBagLayout11);
        coaddsLabel.setText("Coadds");
        readModeTab.setLayout(gridBagLayout2);
        readModeBrightRadioButton.setText("Bright Objects");
        readModeBrightLabel.setEnabled(false);
        readModeBrightLabel.setText("Shallow Well");
        readModeFaintRadioButton.setText("Faint Objects");
        readModeVeryBrightRadioButton.setMinimumSize(new Dimension(200, 40));
    readModeVeryBrightRadioButton.setText("<html>High Background (Thermal) or Very Bright Objects</html>");
        readModeVeryFaintRadioButton.setText("Very Faint Objects");
        readModeFaintLabel.setEnabled(false);
        readModeFaintLabel.setText("Shallow Well");
        readModeVeryBrightLabel.setEnabled(false);
        readModeVeryFaintLabel.setEnabled(false);
        readModeVeryBrightLabel.setRequestFocusEnabled(true);
        readModeVeryFaintLabel.setRequestFocusEnabled(true);
        readModeVeryBrightLabel.setText("Deep Well");
        readModeVeryFaintLabel.setText("Shallow Well");
        biasLevelLabel.setText("Bias level:");
        lowNoiseReadsLabel.setToolTipText("");
        lowNoiseReadsLabel.setText("Low Noise Reads:");
        minExpTimeLabel.setText("Min exposure time:");
        minExpTime.setPreferredSize(new Dimension(80, 16));
        minExpTime.setToolTipText("");
        minExpTime.setText("0 sec");
        crossDispersedTab.setLayout(gridBagLayout6);
        crossDispersedCentralWavelengthsLabel.setText("Central Wavelengths:");
        lowNoiseReads.setRequestFocusEnabled(true);
        lowNoiseReads.setText("0");
        biasLevel.setText("0 mV");
        readNoiseLabel.setText("Read Noise:");
        readNoise.setText("--");
        orderTable.setBackground(Color.white);
        orderTable.setShowHorizontalLines(false);
        orderTable.setToolTipText("");
        readModeAcquisitionRadioButton.setText("Acquisition");
    top1.add(pixelScaleLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(exposureTimeLabel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(pixelScale, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(exposureTime, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(jLabel3, new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        top1.add(disperserLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(disperser, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(filterLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(filter, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(slitWidthLabel, new GridBagConstraints(1, 2, 2, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(centralWavelengthLabel, new GridBagConstraints(3, 2, 3, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(coaddsLabel, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(tabbedPane, new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0
                , GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(11, 6, 0, 6), 0, -9));
        this.add(top1, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        top1.add(coadds, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(coaddsUnitsLabel, new GridBagConstraints(4, 1, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, -2));
        top1.add(slitWidth, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(centralWavelength, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(centralWavelengthUnitsLabel, new GridBagConstraints(4, 3, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        top1.add(crossDispersedLabel, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(posAngleUnitsLabel, new GridBagConstraints(4, 6, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        top1.add(crossDispersedPanel, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        crossDispersedPanel.add(crossDispersedYesRadioButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        crossDispersedPanel.add(crossDispersedNoRadioButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(wollastonPrismLabel, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(wollastonPrismPanel, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        wollastonPrismPanel.add(wollastonPrismYesRadioButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        wollastonPrismPanel.add(wollastonPrismNoRadioButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        wollastonPrismButtonGroup.add(wollastonPrismYesRadioButton);
        wollastonPrismButtonGroup.add(wollastonPrismNoRadioButton);
        crossDispersedButtonGroup.add(crossDispersedYesRadioButton);
        crossDispersedButtonGroup.add(crossDispersedNoRadioButton);
        top1.add(posAngleLabel, new GridBagConstraints(3, 5, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(posAngle, new GridBagConstraints(3, 6, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(scienceFovLabel, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(scienceFOV, new GridBagConstraints(0, 8, 6, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        tabbedPane.add(readModeTab, "Read Mode");
        readModeTab.add(readModeBrightRadioButton,  new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        readModeTab.add(readModeBrightLabel,  new GridBagConstraints(2, 0, 2, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        readModeTab.add(readModeFaintRadioButton,  new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        readModeTab.add(readModeVeryBrightRadioButton,   new GridBagConstraints(0, 3, 2, 1, 1.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        readModeTab.add(readModeAcquisitionRadioButton,   new GridBagConstraints(0, 4, 2, 1, 1.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        readModeTab.add(readModeVeryFaintRadioButton,   new GridBagConstraints(0, 2, 2, 1, 1.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        readModeTab.add(readModeFaintLabel,  new GridBagConstraints(2, 1, 2, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        readModeTab.add(readModeVeryBrightLabel,   new GridBagConstraints(2, 3, 2, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        readModeTab.add(readModeVeryFaintLabel,   new GridBagConstraints(2, 2, 2, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        readModeTab.add(biasLevelLabel,     new GridBagConstraints(0, 7, 1, 1, 0.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        readModeTab.add(lowNoiseReadsLabel,     new GridBagConstraints(0, 6, 1, 1, 0.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        readModeTab.add(minExpTimeLabel,     new GridBagConstraints(2, 6, 1, 1, 0.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        readModeTab.add(minExpTime,     new GridBagConstraints(3, 6, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        readModeTab.add(lowNoiseReads,     new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        readModeTab.add(readModeFillComponent,  new GridBagConstraints(0, 5, 1, 1, 0.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        readModeTab.add(biasLevel,    new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        tabbedPane.add(crossDispersedTab, "Cross-dispersed");
        crossDispersedTab.add(crossDispersedCentralWavelengthsLabel, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0
                , GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        crossDispersedTab.add(component1, new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0
                , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        readModeButtonGroup.add(readModeBrightRadioButton);
        readModeButtonGroup.add(readModeFaintRadioButton);
        readModeButtonGroup.add(readModeVeryFaintRadioButton);
        readModeButtonGroup.add(readModeVeryBrightRadioButton);
        readModeButtonGroup.add(readModeAcquisitionRadioButton);
        crossDispersedTab.add(orderTableScrollPane, new GridBagConstraints(1, 2, 2, 1, 1.0, 1.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 11, 11, 11), 0, 0));
        orderTableScrollPane.getViewport().add(orderTable, null);
        readModeTab.add(readNoiseLabel,    new GridBagConstraints(2, 7, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        readModeTab.add(readNoise,    new GridBagConstraints(3, 7, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
    readModeTab.add(component3,    new GridBagConstraints(0, 10, 1, 1, 0.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
        top1.add(component2, new GridBagConstraints(5, 1, 1, 1, 1.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    readModeTab.add(component4, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    }
}
