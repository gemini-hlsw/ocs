/**
 * Title:        JSky<p>
 * Description:  <p>
 * Copyright:    Copyright (c) Allan Brighton<p>
 * Company:      <p>
 * @author Allan Brighton
 * @version 1.0
 */

package jsky.app.ot.gemini.michelle;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

import jsky.util.gui.*;
import jsky.util.gui.SingleSelectComboBox;

public class MichelleGUI extends JPanel {

    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel filterLabel = new JLabel();
    JLabel totalOnSourceTimeLabel = new JLabel();
    DropDownListBoxWidget filterComboBox = new DropDownListBoxWidget();
    NumberBoxWidget totalOnSourceTime = new NumberBoxWidget();
    JLabel totalOnSourceTimeUnitsLabel = new JLabel();
    JLabel nodIntervalLabel = new JLabel();
    NumberBoxWidget nodInterval = new NumberBoxWidget();
    JPanel top1 = new JPanel();
    GridBagLayout gridBagLayout11 = new GridBagLayout();
    JLabel nodIntervalUnitsLabel = new JLabel();
    JLabel focalPlaneMaskLabel = new JLabel();
    JLabel posAngleLabel = new JLabel();
    DropDownListBoxWidget focalPlaneMaskComboBox = new DropDownListBoxWidget();
    NumberBoxWidget posAngle = new NumberBoxWidget();
    JLabel posAngleUnitsLabel = new JLabel();
    JLabel disperserLabel = new JLabel();
    JLabel centralWavelengthLabel = new JLabel();
    SingleSelectComboBox disperserComboBox = new SingleSelectComboBox();
    NumberBoxWidget centralWavelength = new NumberBoxWidget();
    JLabel chopAngleLabel = new JLabel();
    JLabel chopThrowLabel = new JLabel();
    NumberBoxWidget chopAngle = new NumberBoxWidget();
    NumberBoxWidget chopThrow = new NumberBoxWidget();
    JLabel chopAngleUnitsLabel = new JLabel();
    JLabel scienceFOVLabel = new JLabel();
    JLabel centralWavelengthUnitsLabel = new JLabel();
    JPanel jPanel2 = new JPanel();
    JLabel chopThrowUnitsLabel = new JLabel();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    JLabel scienceFOV = new JLabel();
    JLabel exposureTimeLabel = new JLabel();
    TextBoxWidget exposureTime = new TextBoxWidget();
    JLabel exposureTimeUnitsLabel = new JLabel();
    JLabel autoConfigureLabel = new JLabel();
    JPanel autoConfigurePanel = new JPanel();
    JRadioButton autoConfigureNoButton = new JRadioButton();
    JRadioButton autoConfigureYesButton = new JRadioButton();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    ButtonGroup autoConfigureButtonGroup = new ButtonGroup();
    JLabel nodOrientationLabel = new JLabel();
    DropDownListBoxWidget nodOrientationComboBox = new DropDownListBoxWidget();

    public MichelleGUI() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        filterLabel.setLabelFor(filterComboBox);
        filterLabel.setText("Filter");
        this.setMinimumSize(new Dimension(400, 453));
        this.setPreferredSize(new Dimension(400, 453));
        this.setLayout(gridBagLayout1);
        totalOnSourceTimeLabel.setLabelFor(totalOnSourceTime);
        totalOnSourceTimeLabel.setText("Total On-Source Time");
        totalOnSourceTime.setAllowNegative(false);
        totalOnSourceTime.setToolTipText("Enter the Total On-Source Time in Seconds");
        totalOnSourceTimeUnitsLabel.setText("sec");
        nodIntervalLabel.setLabelFor(nodInterval);
        nodIntervalLabel.setText("Nod Interval");
        filterComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        filterComboBox.setToolTipText("Select the Filter to use");
        nodInterval.setToolTipText("Enter the Nod Interval in Seconds");
        nodInterval.setAllowNegative(false);
        top1.setLayout(gridBagLayout11);
        nodIntervalUnitsLabel.setText("sec");
        focalPlaneMaskLabel.setLabelFor(focalPlaneMaskComboBox);
        focalPlaneMaskLabel.setText("Focal Plane Mask");
        posAngleLabel.setLabelFor(posAngle);
        posAngleLabel.setText("Position Angle");
        posAngleUnitsLabel.setText("deg E of N");
        disperserLabel.setLabelFor(disperserComboBox);
        disperserLabel.setText("Disperser");
        centralWavelengthLabel.setLabelFor(centralWavelength);
        centralWavelengthLabel.setText("Grating Central Wavelength");
        chopAngleLabel.setLabelFor(chopAngle);
        chopAngleLabel.setText("Chop Angle");
        chopThrowLabel.setLabelFor(chopThrow);
        chopThrowLabel.setText("Chop Throw");
        chopAngleUnitsLabel.setText("deg E of N");
        scienceFOVLabel.setText("Science FOV");
        centralWavelengthUnitsLabel.setText("um");
        chopAngle.setToolTipText("Enter the Chop Angle in degrees East of North");
        disperserComboBox.setToolTipText("Select the Disperser to use");
        centralWavelength.setToolTipText("Enter the Grating Central Wavelength in um");
        posAngle.setToolTipText("Enter the Position Angle in Degrees East of North");
        focalPlaneMaskComboBox.setToolTipText("Select the Focal Plane Mask to use");
        chopThrowUnitsLabel.setText("arcsec");
        chopThrow.setToolTipText("Enter the Chop Throw in arcsec");
        scienceFOV.setToolTipText("The Calculated Field of View");
        scienceFOV.setText("000.000 arcsec");
        exposureTimeLabel.setToolTipText("");
        exposureTimeLabel.setLabelFor(exposureTime);
        exposureTimeLabel.setText("Exposure (Frame) Time");
        exposureTimeUnitsLabel.setText("sec");
        exposureTime.setToolTipText("Set the Exposure (Frame) Time in Seconds");
        autoConfigureLabel.setLabelFor(autoConfigureYesButton);
        autoConfigureLabel.setText("Auto-Configure");
        autoConfigureNoButton.setToolTipText("Do not automatically configure the instrument exposure time");
        autoConfigureNoButton.setText("No");
        autoConfigureNoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                autoConfigureNoButton_actionPerformed(e);
            }
        });
        autoConfigureYesButton.setToolTipText("Automatically configure the instrument exposure time");
        autoConfigureYesButton.setText("Yes");
        autoConfigureYesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                autoConfigureYesButton_actionPerformed(e);
            }
        });
        autoConfigurePanel.setLayout(gridBagLayout3);
        nodOrientationLabel.setText("Nod Orientation");
        top1.add(filterLabel, new GridBagConstraints(0, 0, 1, 1, 0.2, 0.0
                                                     , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(totalOnSourceTimeLabel, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
                                                                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(filterComboBox, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                                                        , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(totalOnSourceTime, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
                                                           , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(totalOnSourceTimeUnitsLabel, new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0
                                                                     , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        top1.add(nodIntervalLabel, new GridBagConstraints(2, 4, GridBagConstraints.REMAINDER, 1, 1.0, 0.0
                                                          , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(nodInterval, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0
                                                     , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(nodIntervalUnitsLabel, new GridBagConstraints(3, 5, 1, 1, 1.0, 0.0
                                                               , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        this.add(top1, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
                                              , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 37, 0), 0, 0));
        top1.add(focalPlaneMaskLabel, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0
                                                             , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(posAngleLabel, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0
                                                       , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(focalPlaneMaskComboBox, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0
                                                                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(posAngle, new GridBagConstraints(2, 7, 1, 1, 0.0, 0.0
                                                  , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(posAngleUnitsLabel, new GridBagConstraints(3, 7, 1, 1, 0.0, 0.0
                                                            , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        top1.add(disperserLabel, new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0
                                                        , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(centralWavelengthLabel, new GridBagConstraints(2, 8, 1, 1, 0.0, 0.0
                                                                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(disperserComboBox, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0
                                                           , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(centralWavelength, new GridBagConstraints(2, 9, 1, 1, 0.0, 0.0
                                                           , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(chopAngleLabel, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0
                                                        , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(chopThrowLabel, new GridBagConstraints(2, 10, 1, 1, 0.0, 0.0
                                                        , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(chopAngle, new GridBagConstraints(0, 11, 1, 1, 0.0, 0.0
                                                   , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(chopThrow, new GridBagConstraints(2, 11, 1, 1, 0.0, 0.0
                                                   , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(chopAngleUnitsLabel, new GridBagConstraints(1, 11, 1, 1, 0.0, 0.0
                                                             , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        top1.add(scienceFOVLabel, new GridBagConstraints(0, 12, 1, 1, 0.0, 0.0
                                                         , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(centralWavelengthUnitsLabel, new GridBagConstraints(3, 9, 1, 1, 0.0, 0.0
                                                                     , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        this.add(jPanel2, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
                                                 , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        top1.add(chopThrowUnitsLabel, new GridBagConstraints(3, 11, 1, 1, 0.0, 0.0
                                                             , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        top1.add(scienceFOV, new GridBagConstraints(0, 13, 4, 1, 0.0, 0.0
                                                    , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(exposureTimeLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
                                                           , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(exposureTime, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
                                                      , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(exposureTimeUnitsLabel, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
                                                                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        top1.add(autoConfigureLabel, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
                                                            , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        top1.add(autoConfigurePanel, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0
                                                            , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        autoConfigurePanel.add(autoConfigureYesButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                                                                              , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        autoConfigurePanel.add(autoConfigureNoButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
                                                                             , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        top1.add(nodOrientationLabel,  new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        autoConfigureButtonGroup.add(autoConfigureYesButton);
        autoConfigureButtonGroup.add(autoConfigureNoButton);
        top1.add(nodOrientationComboBox,  new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
    }

    void autoConfigureYesButton_actionPerformed(ActionEvent e) {

    }

    void autoConfigureNoButton_actionPerformed(ActionEvent e) {

    }
}
