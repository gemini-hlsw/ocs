/**
 * Title:        JSky<p>
 * Description:  NIRI Instrument Editor GUI<p>
 * Company:      Gemini<p>
 * @author Allan Brighton
 * @version 1.0
 */
package jsky.app.ot.gemini.niri;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.NumberBoxWidget;

import java.awt.event.*;

public class NiriGUI extends JPanel {

    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel cameraLabel = new JLabel();
    JLabel expTimeLabel = new JLabel();
    DropDownListBoxWidget camera = new DropDownListBoxWidget();
    NumberBoxWidget exposureTime = new NumberBoxWidget();
    JLabel disperserLabel = new JLabel();
    DropDownListBoxWidget disperser = new DropDownListBoxWidget();
    JLabel maskLabel = new JLabel();
    DropDownListBoxWidget mask = new DropDownListBoxWidget();
    JLabel coaddsLabel = new JLabel();
    JLabel posAngleLabel = new JLabel();
    NumberBoxWidget coadds = new NumberBoxWidget();
    NumberBoxWidget posAngle = new NumberBoxWidget();
    JLabel scienceFOVLabel = new JLabel();
    TitledBorder titledBorder1;
    JLabel expTimeUnits = new JLabel();
    JLabel coaddsUnits = new JLabel();
    JLabel posAngleUnits = new JLabel();
    JLabel beamSplitterLabel = new JLabel();
    DropDownListBoxWidget beamSplitter = new DropDownListBoxWidget();
    JTabbedPane jTabbedPane1 = new JTabbedPane();
    JPanel readModePanel = new JPanel();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    JPanel roiPanel = new JPanel();
    JRadioButton readMode1To25Button = new JRadioButton();
    JRadioButton readModeNarrowBandButton = new JRadioButton();
    JRadioButton readMode3To5Button = new JRadioButton();
    JLabel readModeLowBgLabel = new JLabel();
    JLabel readModeMediumBgLabel = new JLabel();
    JLabel readModeHighBgLabel = new JLabel();
    JLabel readModeMinExpTime = new JLabel();
    JLabel readModeNoise = new JLabel();
    JLabel readModeNoiseLabel = new JLabel();
    ButtonGroup filterButtonGroup = new ButtonGroup();
    ButtonGroup readModeButtonGroup = new ButtonGroup();
    JRadioButton roiFullFrameButton = new JRadioButton();
    GridBagLayout gridBagLayout4 = new GridBagLayout();
    JLabel filterLabel = new JLabel();
    DropDownListBoxWidget selectedFilter = new DropDownListBoxWidget();
    Component component1;
    JLabel scienceFOV = new JLabel();
    JRadioButton roiCentral768Button = new JRadioButton();
    JRadioButton roiCentral256Button = new JRadioButton();
    ButtonGroup roiButtonGroup = new ButtonGroup();
    ButtonGroup wellButtonGroup = new ButtonGroup();
    JLabel readModeRecMinExpTimeLabel = new JLabel();
    JLabel readModeRecMinExpTime = new JLabel();
    JLabel minExpTimeLabel = new JLabel();
    JRadioButton roiCentral512Button = new JRadioButton();
    JRadioButton roiSpec1024x512Button = new JRadioButton();
    JRadioButton shallowWellDepthButton = new JRadioButton();
    JRadioButton deepWellDepthButton = new JRadioButton();
    Component component2;

    public NiriGUI() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        titledBorder1 = new TitledBorder(new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)), "Filter");


        component1 = Box.createVerticalStrut(8);
        component2 = Box.createVerticalStrut(8);
        cameraLabel.setLabelFor(camera);
        cameraLabel.setText("Camera");
        this.setMinimumSize(new Dimension(350, 378));
        this.setPreferredSize(new Dimension(350, 378));
        this.setToolTipText("");
        this.setLayout(gridBagLayout1);


        expTimeLabel.setToolTipText("Exposure time in seconds");
        expTimeLabel.setLabelFor(exposureTime);
        expTimeLabel.setText("Exposure Time");
        exposureTime.setAllowNegative(false);
        exposureTime.setToolTipText("Enter the exposure time in seconds");

        disperserLabel.setLabelFor(disperser);
        disperserLabel.setText("Disperser");


        maskLabel.setToolTipText("");
        maskLabel.setLabelFor(mask);
        maskLabel.setText("Focal Plane Mask");


        coaddsLabel.setToolTipText("Coadds (number of exposures per observation)");
        coaddsLabel.setLabelFor(coadds);
        coaddsLabel.setText("Coadds");


        posAngleLabel.setToolTipText("Position angle in degrees E of N");
        posAngleLabel.setLabelFor(posAngle);
        posAngleLabel.setText("Position Angle");

        scienceFOVLabel.setToolTipText("Science field of view in arcsec");
        scienceFOVLabel.setText("Science FOV");

        coadds.setToolTipText("Enter the coadds (number of exposures per observation)");
        coadds.setAllowNegative(false);
        posAngle.setMinimumSize(new Dimension(80, 21));
        posAngle.setToolTipText("Enter the position angle in degrees E of N");
        expTimeUnits.setText("sec");
        coaddsUnits.setToolTipText("");
        coaddsUnits.setText("exp/obs");
        posAngleUnits.setText("deg E of N");
        beamSplitterLabel.setLabelFor(beamSplitter);
        beamSplitterLabel.setText("Beam Splitter");
        beamSplitter.setToolTipText("Select the Beam Splitter");
        readModePanel.setLayout(gridBagLayout3);
        readMode1To25Button.setText("<html>1-2.5um: JHK and Bright Object Narrow-band Imaging / Bright Object Spectroscopy</html>");
        readModeNarrowBandButton.setText("1-2.5um: Faint Object Narrow-band Imaging/Spectroscopy");
        readModeNarrowBandButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                readModeNarrowBandButton_actionPerformed(e);
            }
        });
        readMode3To5Button.setText("3-5um: Imaging / Spectroscopy");
        readModeLowBgLabel.setEnabled(false);
        readModeLowBgLabel.setText("Low Background");
        readModeMediumBgLabel.setEnabled(false);
        readModeMediumBgLabel.setText("Medium Background");
        readModeHighBgLabel.setEnabled(false);
        readModeHighBgLabel.setText("High Background");
        readModeMinExpTime.setText("-");
        readModeNoise.setText("-");
        readModeNoiseLabel.setToolTipText("");
        readModeNoiseLabel.setText("Read Noise:");
        roiFullFrameButton.setToolTipText("Set the Region of Interest to the Full Frame");
        roiFullFrameButton.setSelected(true);
        roiFullFrameButton.setText("Full Frame Readout");
        roiPanel.setLayout(gridBagLayout4);
        filterLabel.setLabelFor(selectedFilter);
        filterLabel.setText("Filter");
        selectedFilter.setToolTipText("Select the Filter (Broadband, Narrowband)");
        mask.setToolTipText("Select the Focal Plane Mask");
        mask.setMaximumRowCount(9);
        disperser.setToolTipText("Select the Disperser to use");
        camera.setToolTipText("Select the Camera to use");
        scienceFOV.setBorder(null);
        scienceFOV.setText("000.0000 arcsecs");
        roiCentral768Button.setToolTipText("Set the Region of Interest to the central 768x768 pixels");
        roiCentral768Button.setText("Central 768x768");
        roiCentral256Button.setToolTipText("Set the Region of Interest to the central 256x256 pixels");
        roiCentral256Button.setText("Central 256x256");
        readModeRecMinExpTimeLabel.setText("Recommended Exposure Time:");
        readModeRecMinExpTime.setText("-");
        minExpTimeLabel.setText("Minimum Exposure Time:");
        roiCentral512Button.setText("Central 512x512");
        roiCentral512Button.setToolTipText("Set the Region of Interest to the central 128x128 pixels");
        roiCentral512Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                roiCentral512Button_actionPerformed(e);
            }
        });
        roiSpec1024x512Button.setText("Spectroscopy 1024x512");
        shallowWellDepthButton.setText("Shallow well (1-2.5 um)");
        shallowWellDepthButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                shallowWellDepthButton_actionPerformed(e);
            }
        });
        deepWellDepthButton.setText("Deep well (3-5 um)");
        this.add(cameraLabel, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
                                                     , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(expTimeLabel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
                                                      , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(camera, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
                                                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        this.add(exposureTime, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
                                                      , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        this.add(disperserLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
                                                        , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(disperser, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
                                                   , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        this.add(maskLabel, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
                                                   , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(mask, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
                                              , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), -10, 0));
        this.add(coaddsLabel, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0
                                                     , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(posAngleLabel, new GridBagConstraints(3, 2, 2, 1, 0.0, 0.0
                                                       , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(coadds, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0
                                                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        this.add(posAngle, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0
                                                  , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        this.add(scienceFOVLabel, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0
                                                         , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(expTimeUnits, new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0
                                                      , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        this.add(coaddsUnits, new GridBagConstraints(4, 1, 2, 1, 1.0, 0.0
                                                     , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0));
        this.add(posAngleUnits, new GridBagConstraints(4, 3, 1, 1, 1.0, 0.0
                                                       , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 6), 0, 0));
        this.add(beamSplitterLabel, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
                                                           , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, -1000), 0, 0));
        this.add(beamSplitter, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0
                                                      , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), -10, 0));
        this.add(filterLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
                                                     , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(selectedFilter, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                                                        , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        readModeButtonGroup.add(readModeNarrowBandButton);
        readModeButtonGroup.add(readMode1To25Button);
        readModeButtonGroup.add(readMode3To5Button);
        this.add(scienceFOV, new GridBagConstraints(0, 7, 5, 1, 0.0, 0.0
                                                    , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        this.add(jTabbedPane1, new GridBagConstraints(0, 8, 5, 1, 1.0, 1.0
                                                      , GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(6, 6, 6, 6), 0, 0));
        jTabbedPane1.add(readModePanel, "Read Mode");
        readModePanel.add(readMode1To25Button, new GridBagConstraints(0, 1, 4, 1, 0.0, 1.0
                                                                      , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 11, 0, 0), 0, 0));
        readModePanel.add(readModeNarrowBandButton, new GridBagConstraints(0, 0, 4, 1, 0.0, 1.0
                                                                           , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 11, 0, 0), 0, 0));
        readModePanel.add(readMode3To5Button, new GridBagConstraints(0, 2, 3, 1, 0.0, 1.0
                                                                     , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 11, 0, 0), 0, 0));
        readModePanel.add(readModeLowBgLabel, new GridBagConstraints(4, 0, 2, 1, 1.0, 0.0
                                                                     , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 11, 0, 0), 0, 0));
        readModePanel.add(readModeMediumBgLabel, new GridBagConstraints(4, 1, 2, 1, 1.0, 0.0
                                                                        , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 11, 0, 0), 0, 0));
        readModePanel.add(readModeHighBgLabel, new GridBagConstraints(4, 2, 2, 1, 1.0, 0.0
                                                                      , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 11, 0, 0), 0, 0));
        readModePanel.add(readModeMinExpTime, new GridBagConstraints(2, 4, 2, 1, 0.0, 0.0
                                                                     , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(17, 11, 0, 0), 0, 0));
        readModePanel.add(readModeNoise, new GridBagConstraints(5, 4, 1, 1, 0.0, 0.0
                                                                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(17, 11, 0, 0), 0, 0));
        readModePanel.add(readModeNoiseLabel, new GridBagConstraints(4, 4, 1, 1, 0.0, 0.0
                                                                     , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(17, 11, 0, 0), 0, 0));
        readModePanel.add(component1, new GridBagConstraints(0, 7, 1, 1, 0.0, 1.0
                                                             , GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
        readModePanel.add(readModeRecMinExpTimeLabel,   new GridBagConstraints(0, 5, 2, 1, 0.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        readModePanel.add(readModeRecMinExpTime, new GridBagConstraints(2, 5, 2, 1, 0.0, 0.0
                                                                        , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        readModePanel.add(minExpTimeLabel,   new GridBagConstraints(0, 4, 2, 1, 0.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(17, 11, 0, 0), 0, 0));
        readModePanel.add(shallowWellDepthButton, new GridBagConstraints(0, 6, 2, 1, 0.0, 1.0
                                                                         , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(17, 11, 11, 0), 0, 0));
        readModePanel.add(deepWellDepthButton, new GridBagConstraints(4, 6, 2, 1, 0.0, 0.0
                                                                      , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(17, 11, 11, 0), 0, 0));
        jTabbedPane1.add(roiPanel, "Array / Subarray Size");
        roiPanel.add(roiFullFrameButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
                                                                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        roiPanel.add(roiCentral768Button, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
                                                                 , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        roiPanel.add(roiCentral256Button, new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0
                                                                 , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        roiPanel.add(roiCentral512Button, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0
                                                                 , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        roiPanel.add(roiSpec1024x512Button, new GridBagConstraints(0, 4, 1, 1, 1.0, 1.0
                                                                   , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        roiPanel.add(component2, new GridBagConstraints(0, 5, 1, 1, 0.0, 2.0
                                                        , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        roiButtonGroup.add(roiFullFrameButton);
        roiButtonGroup.add(roiCentral768Button);
        roiButtonGroup.add(roiCentral256Button);
        roiButtonGroup.add(roiCentral512Button);
        roiButtonGroup.add(roiSpec1024x512Button);
        //jTabbedPane1.setSelectedIndex(0);
        wellButtonGroup.add(deepWellDepthButton);
        wellButtonGroup.add(shallowWellDepthButton);
    }

    void readModeNarrowBandButton_actionPerformed(ActionEvent e) {

    }

    void roiCentral512Button_actionPerformed(ActionEvent e) {

    }

    void shallowWellDepthButton_actionPerformed(ActionEvent e) {

    }
}
