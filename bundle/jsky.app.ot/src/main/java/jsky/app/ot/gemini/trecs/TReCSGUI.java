/**
 * Title:        JSky<p>
 * Description:  <p>
 * Copyright:    Copyright (c) Allan Brighton<p>
 * Company:      <p>
 * @author Allan Brighton
 * @version 1.0
 */

package jsky.app.ot.gemini.trecs;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

import jsky.util.gui.*;

public class TReCSGUI extends JPanel {

    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel filterLabel = new JLabel();
    JLabel totalOnSourceTimeLabel = new JLabel();
    DropDownListBoxWidget filterComboBox = new DropDownListBoxWidget();
    NumberBoxWidget totalOnSourceTime = new NumberBoxWidget();
    JLabel totalOnSourceTimeUnitsLabel = new JLabel();
    JLabel timePerSavesetLabel = new JLabel();
    JLabel nodDwellLabel = new JLabel();
    JLabel nodSettleLabel = new JLabel();
    NumberBoxWidget nodDwell = new NumberBoxWidget();
    NumberBoxWidget nodSettle = new NumberBoxWidget();
    JPanel top1 = new JPanel();
    GridBagLayout gridBagLayout11 = new GridBagLayout();
    JLabel nodDwellUnitsLabel = new JLabel();
    JLabel nodSettleUnitsLabel = new JLabel();
    JLabel timePerSavesetUnitsLabel = new JLabel();
    NumberBoxWidget timePerSaveset = new NumberBoxWidget();
    JLabel focalPlaneMaskLabel = new JLabel();
    JLabel posAngleLabel = new JLabel();
    DropDownListBoxWidget focalPlaneMaskComboBox = new DropDownListBoxWidget();
    NumberBoxWidget posAngle = new NumberBoxWidget();
    JLabel posAngleUnitsLabel = new JLabel();
    JLabel disperserLabel = new JLabel();
    JLabel centralWavelengthLabel = new JLabel();
    DropDownListBoxWidget disperserComboBox = new DropDownListBoxWidget();
    NumberBoxWidget centralWavelength = new NumberBoxWidget();
    JLabel chopAngleLabel = new JLabel();
    JLabel chopThrowLabel = new JLabel();
    NumberBoxWidget chopAngle = new NumberBoxWidget();
    NumberBoxWidget chopThrow = new NumberBoxWidget();
    JLabel chopAngleUnitsLabel = new JLabel();
    JLabel scienceFOVLabel = new JLabel();
    JLabel centralWavelengthUnitsLabel = new JLabel();
    JPanel freezeDetectorConfigPanel = new JPanel();
    JPanel jPanel2 = new JPanel();
    JLabel chopThrowUnitsLabel = new JLabel();
    ButtonGroup freezeDetectorConfigButtonGroup = new ButtonGroup();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    JLabel scienceFOV = new JLabel();
    JLabel exposureTimeLabel = new JLabel();
    JLabel exposureTimeUnitsLabel = new JLabel();
    JPanel autoConfigurePanel = new JPanel();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    ButtonGroup autoConfigureButtonGroup = new ButtonGroup();
    DropDownListBoxWidget exposureTime = new DropDownListBoxWidget();
    JLabel dataModeLabel = new JLabel();
    JLabel obsModeLabel = new JLabel();
    DropDownListBoxWidget dataModeComboBox = new DropDownListBoxWidget();
    DropDownListBoxWidget obsModeComboBox = new DropDownListBoxWidget();
    JLabel winWheelLabel = new JLabel();
    DropDownListBoxWidget winWheelComboBox = new DropDownListBoxWidget();
    JLabel jLabel1 = new JLabel();
    JLabel jLabel2 = new JLabel();
    JLabel nodOrientationLabel = new JLabel();
    DropDownListBoxWidget nodOrientationComboBox = new DropDownListBoxWidget();

    public TReCSGUI() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        filterLabel.setLabelFor(filterComboBox);
        filterLabel.setText("Filter:");
        this.setMinimumSize(new Dimension(400, 453));
        this.setPreferredSize(new Dimension(400, 453));
        this.setLayout(gridBagLayout1);
        totalOnSourceTimeLabel.setLabelFor(totalOnSourceTime);
        totalOnSourceTimeLabel.setText("Total On-Source Time:");
        totalOnSourceTime.setMaximumSize(new Dimension(1000, 1000));
        totalOnSourceTime.setMinimumSize(new Dimension(80, 21));
        totalOnSourceTime.setAllowNegative(false);
        totalOnSourceTime.setToolTipText("Enter the Total On-Source Time in Seconds");
        totalOnSourceTimeUnitsLabel.setText("sec");
        timePerSavesetLabel.setLabelFor(timePerSaveset);
        timePerSavesetLabel.setText("Saveset Time:");
        nodDwellLabel.setLabelFor(nodDwell);
        nodDwellLabel.setText("Nod Dwell:");
        nodSettleLabel.setLabelFor(nodSettle);
        nodSettleLabel.setText("Nod Settle:");
        filterComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        filterComboBox.setToolTipText("Select the Filter to use");
        top1.setLayout(gridBagLayout11);
        nodDwellUnitsLabel.setText("sec");
        nodSettleUnitsLabel.setText("sec");
        timePerSavesetUnitsLabel.setText("sec");
        timePerSaveset.setMinimumSize(new Dimension(80, 21));
        timePerSaveset.setToolTipText("Enter the Time per Saveset in Seconds");
        focalPlaneMaskLabel.setLabelFor(focalPlaneMaskComboBox);
        focalPlaneMaskLabel.setText("Focal Plane Mask:");
        posAngleLabel.setLabelFor(posAngle);
        posAngleLabel.setText("Position Angle:");
        posAngleUnitsLabel.setText("deg E of N");
        disperserLabel.setLabelFor(disperserComboBox);
        disperserLabel.setText("Disperser:");
        centralWavelengthLabel.setLabelFor(centralWavelength);
        centralWavelengthLabel.setText("Grating Central Wavelength:");
        chopAngleLabel.setLabelFor(chopAngle);
        chopAngleLabel.setText("Chop Angle:");
        chopThrowLabel.setLabelFor(chopThrow);
        chopThrowLabel.setText("Chop Throw:");
        chopAngleUnitsLabel.setText("deg E of N");
        scienceFOVLabel.setText("Science FOV:");
        centralWavelengthUnitsLabel.setText("um");
        chopAngle.setToolTipText("Enter the Chop Angle in degrees East of North");
        disperserComboBox.setToolTipText("Select the Disperser to use");
        centralWavelength.setToolTipText("Enter the Grating Central Wavelength in um");
        posAngle.setToolTipText("Enter the Position Angle in Degrees East of North");
        focalPlaneMaskComboBox.setToolTipText("Select the Focal Plane Mask to use");
        chopThrowUnitsLabel.setText("arcsec");
        chopThrow.setToolTipText("Enter the Chop Throw in arcsec");
        freezeDetectorConfigPanel.setLayout(gridBagLayout2);
        scienceFOV.setToolTipText("The Calculated Field of View");
        scienceFOV.setText("000.0000 arcsec");
        exposureTimeLabel.setText("Exposure (Frame) Time:");
        exposureTimeUnitsLabel.setText("sec");
        autoConfigurePanel.setLayout(gridBagLayout3);
        exposureTime.setToolTipText("Set the exposure time in seconds");
        exposureTime.setEditable(true);
        dataModeLabel.setText("Data Mode:");
        obsModeLabel.setText("Observing Mode:");
        winWheelLabel.setText("Window Wheel:");

        nodOrientationLabel.setText("Nod Orientation:");
        top1.add(filterLabel,    new GridBagConstraints(0, 0, 1, 1, 0.2, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(totalOnSourceTimeLabel,    new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(filterComboBox,    new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(totalOnSourceTime,    new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(totalOnSourceTimeUnitsLabel,    new GridBagConstraints(4, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        top1.add(timePerSavesetLabel,    new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));

        this.add(top1, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
                                              , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

        top1.add(nodDwellLabel,    new GridBagConstraints(0, 6, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(nodDwell,    new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(nodDwellUnitsLabel,    new GridBagConstraints(1, 7, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));

        top1.add(nodSettleLabel,    new GridBagConstraints(2, 6, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(nodSettle,    new GridBagConstraints(2, 7, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(nodSettleUnitsLabel,    new GridBagConstraints(4, 7, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));

        top1.add(timePerSavesetUnitsLabel,    new GridBagConstraints(4, 3, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        top1.add(timePerSaveset,    new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(focalPlaneMaskLabel,     new GridBagConstraints(0, 10, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(posAngleLabel,      new GridBagConstraints(2, 10, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(focalPlaneMaskComboBox,    new GridBagConstraints(0, 11, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(posAngle,    new GridBagConstraints(2, 11, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(posAngleUnitsLabel,    new GridBagConstraints(4, 11, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        top1.add(disperserLabel,    new GridBagConstraints(0, 12, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(centralWavelengthLabel,    new GridBagConstraints(2, 12, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(disperserComboBox,    new GridBagConstraints(0, 13, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(centralWavelength,    new GridBagConstraints(2, 13, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(chopAngleLabel,    new GridBagConstraints(0, 14, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(chopThrowLabel,    new GridBagConstraints(2, 14, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(chopAngle,    new GridBagConstraints(0, 15, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(chopThrow,    new GridBagConstraints(2, 15, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(chopAngleUnitsLabel,    new GridBagConstraints(1, 15, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        top1.add(scienceFOVLabel,    new GridBagConstraints(0, 16, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(centralWavelengthUnitsLabel,    new GridBagConstraints(4, 13, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        top1.add(freezeDetectorConfigPanel,    new GridBagConstraints(2, 17, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        this.add(jPanel2, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
                                                 , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        top1.add(chopThrowUnitsLabel,    new GridBagConstraints(4, 15, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        top1.add(scienceFOV,    new GridBagConstraints(0, 17, 2, 2, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(exposureTimeLabel,    new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(exposureTimeUnitsLabel,    new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        top1.add(autoConfigurePanel,    new GridBagConstraints(2, 3, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        top1.add(exposureTime,    new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(dataModeLabel,    new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(obsModeLabel,    new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(dataModeComboBox,    new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(obsModeComboBox,    new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(winWheelLabel,     new GridBagConstraints(2, 8, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(winWheelComboBox,     new GridBagConstraints(2, 9, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(nodOrientationLabel,     new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(nodOrientationComboBox,   new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
    }
}
