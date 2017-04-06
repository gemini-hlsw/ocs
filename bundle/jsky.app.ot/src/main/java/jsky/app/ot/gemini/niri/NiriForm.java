package jsky.app.ot.gemini.niri;

import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.NumberBoxWidget;
import jsky.util.gui.SingleSelectComboBox;

import javax.swing.*;
import java.awt.*;


public class NiriForm extends JPanel {
    public NiriForm() {
        initComponents();
    }

    private void initComponents() {
        JLabel cameraLabel = new JLabel();
        JLabel expTimeLabel = new JLabel();
        JLabel disperserLabel = new JLabel();
        JLabel maskLabel = new JLabel();
        JLabel coaddsLabel = new JLabel();
        JLabel posAngleLabel = new JLabel();
        JLabel scienceFOVLabel = new JLabel();
        JLabel expTimeUnits = new JLabel();
        JLabel coaddsUnits = new JLabel();
        JLabel posAngleUnits = new JLabel();
        JLabel beamSplitterLabel = new JLabel();
        JLabel filterLabel = new JLabel();
        JLabel fastModeExposuresLabel = new JLabel();
        JLabel fastModeReadsUnits = new JLabel();
        JTabbedPane jTabbedPane1 = new JTabbedPane();
        JPanel readModePanel = new JPanel();
        JLabel component1 = new JLabel();
        JLabel component2 = new JLabel();
        JPanel roiPanel = new JPanel();

        camera = new DropDownListBoxWidget();
        exposureTime = new NumberBoxWidget();
        disperser = new DropDownListBoxWidget();
        mask = new DropDownListBoxWidget();
        coadds = new NumberBoxWidget();
        posAngle = new NumberBoxWidget();
        beamSplitter = new DropDownListBoxWidget();
        selectedFilter = new SingleSelectComboBox();
        fastModeExposures = new NumberBoxWidget();
        scienceFOV = new JLabel();
        readMode1To25Button = new JRadioButton();
        readModeNarrowBandButton = new JRadioButton();
        readMode3To5Button = new JRadioButton();
        readModeLowBgLabel = new JLabel();
        readModeMediumBgLabel = new JLabel();
        readModeHighBgLabel = new JLabel();
        readModeMinExpTime = new JLabel();
        readModeNoise = new JLabel();
        readModeNoiseLabel = new JLabel();
        readModeRecMinExpTimeLabel = new JLabel();
        readModeRecMinExpTime = new JLabel();
        minExpTimeLabel = new JLabel();
        shallowWellDepthButton = new JRadioButton();
        deepWellDepthButton = new JRadioButton();
        roiFullFrameButton = new JRadioButton();
        roiCentral768Button = new JRadioButton();
        roiCentral256Button = new JRadioButton();
        roiCentral512Button = new JRadioButton();
        roiSpec1024x512Button = new JRadioButton();

        //======== this ========
        setMinimumSize(new Dimension(350, 378));
        setPreferredSize(new Dimension(350, 378));
        setToolTipText("");
        setLayout(new GridBagLayout());

        //---- cameraLabel ----
        cameraLabel.setLabelFor(null);
        cameraLabel.setText("Camera");
        add(cameraLabel, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(11, 11, 0, 0), 0, 0));

        //---- expTimeLabel ----
        expTimeLabel.setToolTipText("Exposure time in seconds");
        expTimeLabel.setLabelFor(null);
        expTimeLabel.setText("Exposure Time");
        add(expTimeLabel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(11, 11, 0, 0), 0, 0));

        //---- camera ----
        camera.setToolTipText("Select the Camera to use");
        add(camera, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 11, 0, 0), 0, 0));

        //---- exposureTime ----
        exposureTime.setToolTipText("Enter the exposure time in seconds");
        add(exposureTime, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 11, 0, 0), 0, 0));

        //---- disperserLabel ----
        disperserLabel.setLabelFor(null);
        disperserLabel.setText("Disperser");
        add(disperserLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(11, 11, 0, 0), 0, 0));

        //---- disperser ----
        disperser.setToolTipText("Select the Disperser to use");
        add(disperser, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 11, 0, 0), 0, 0));

        //---- maskLabel ----
        maskLabel.setToolTipText("");
        maskLabel.setLabelFor(null);
        maskLabel.setText("Focal Plane Mask");
        add(maskLabel, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(11, 11, 0, 0), 0, 0));

        //---- mask ----
        mask.setToolTipText("Select the Focal Plane Mask");
        mask.setMaximumRowCount(9);
        add(mask, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 11, 0, 0), -10, 0));

        //---- coaddsLabel ----
        coaddsLabel.setToolTipText("Coadds (number of exposures per observation)");
        coaddsLabel.setLabelFor(null);
        coaddsLabel.setText("Coadds");
        add(coaddsLabel, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(11, 11, 0, 0), 0, 0));

        //---- posAngleLabel ----
        posAngleLabel.setToolTipText("Position angle in degrees E of N");
        posAngleLabel.setLabelFor(null);
        posAngleLabel.setText("Position Angle");
        add(posAngleLabel, new GridBagConstraints(3, 2, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(11, 11, 0, 0), 0, 0));

        //---- coadds ----
        coadds.setToolTipText("Enter the coadds (number of exposures per observation)");
        add(coadds, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 11, 0, 0), 0, 0));

        //---- posAngle ----
        posAngle.setMinimumSize(new Dimension(80, 21));
        posAngle.setToolTipText("Enter the position angle in degrees E of N");
        add(posAngle, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 11, 0, 0), 0, 0));

        //---- scienceFOVLabel ----
        scienceFOVLabel.setToolTipText("Science field of view in arcsec");
        scienceFOVLabel.setText("Science FOV");
        add(scienceFOVLabel, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(11, 11, 0, 0), 0, 0));

        //---- expTimeUnits ----
        expTimeUnits.setText("sec");
        add(expTimeUnits, new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 6, 0, 0), 0, 0));

        //---- coaddsUnits ----
        coaddsUnits.setToolTipText("");
        coaddsUnits.setText("exp/obs");
        add(coaddsUnits, new GridBagConstraints(4, 1, 2, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 6, 0, 0), 0, 0));

        //---- posAngleUnits ----
        posAngleUnits.setText("deg E of N");
        add(posAngleUnits, new GridBagConstraints(4, 3, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 6, 0, 6), 0, 0));

        //---- beamSplitterLabel ----
        beamSplitterLabel.setLabelFor(null);
        beamSplitterLabel.setText("Beam Splitter");
        add(beamSplitterLabel, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(11, 11, 0, -1000), 0, 0));

        //---- beamSplitter ----
        beamSplitter.setToolTipText("Select the Beam Splitter");
        add(beamSplitter, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 11, 0, 0), -10, 0));

        //---- filterLabel ----
        filterLabel.setLabelFor(null);
        filterLabel.setText("Filter");
        add(filterLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(11, 11, 0, 0), 0, 0));

        //---- selectedFilter ----
        selectedFilter.setToolTipText("Select the Filter (Broadband, Narrowband)");
        add(selectedFilter, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 11, 0, 0), 0, 0));

        //---- fastModeExposuresLabel ----
        fastModeExposuresLabel.setText("Fast Mode Exposures");
        add(fastModeExposuresLabel, new GridBagConstraints(3, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(11, 11, 0, 0), 0, 0));

        //---- fastModeExposures ----
        fastModeExposures.setMinimumSize(new Dimension(80, 21));
        fastModeExposures.setToolTipText("Enter the number of fast mode reads.  The default value is 1.");
        add(fastModeExposures, new GridBagConstraints(3, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 11, 0, 0), 0, 0));

        //---- fastModeReadsUnits ----
        fastModeReadsUnits.setText(">= 1");
        add(fastModeReadsUnits, new GridBagConstraints(4, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 6, 0, 6), 0, 0));

        //---- scienceFOV ----
        scienceFOV.setText("000.0000 arcsecs");
        add(scienceFOV, new GridBagConstraints(0, 7, 5, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 11, 0, 0), 0, 0));

        //======== jTabbedPane1 ========
        {

            //======== readModePanel ========
            {
                readModePanel.setLayout(new GridBagLayout());

                //---- readMode1To25Button ----
                readMode1To25Button.setText("<html>1-2.5um: JHK and Bright Object Narrow-band Imaging / Bright Object Spectroscopy</html>");
                readModePanel.add(readMode1To25Button, new GridBagConstraints(0, 1, 4, 1, 0.0, 1.0,
                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                        new Insets(5, 11, 0, 0), 0, 0));

                //---- readModeNarrowBandButton ----
                readModeNarrowBandButton.setText("1-2.5um: Faint Object Narrow-band Imaging/Spectroscopy");
                readModePanel.add(readModeNarrowBandButton, new GridBagConstraints(0, 0, 4, 1, 0.0, 1.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(5, 11, 0, 0), 0, 0));

                //---- readMode3To5Button ----
                readMode3To5Button.setText("3-5um: Imaging / Spectroscopy");
                readModePanel.add(readMode3To5Button, new GridBagConstraints(0, 2, 3, 1, 0.0, 1.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(5, 11, 0, 0), 0, 0));

                //---- readModeLowBgLabel ----
                readModeLowBgLabel.setEnabled(false);
                readModeLowBgLabel.setText("Low Background");
                readModePanel.add(readModeLowBgLabel, new GridBagConstraints(4, 0, 2, 1, 1.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(5, 11, 0, 0), 0, 0));

                //---- readModeMediumBgLabel ----
                readModeMediumBgLabel.setEnabled(false);
                readModeMediumBgLabel.setText("Medium Background");
                readModePanel.add(readModeMediumBgLabel, new GridBagConstraints(4, 1, 2, 1, 1.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(5, 11, 0, 0), 0, 0));

                //---- readModeHighBgLabel ----
                readModeHighBgLabel.setEnabled(false);
                readModeHighBgLabel.setText("High Background");
                readModePanel.add(readModeHighBgLabel, new GridBagConstraints(4, 2, 2, 1, 1.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(5, 11, 0, 0), 0, 0));

                //---- readModeMinExpTime ----
                readModeMinExpTime.setText("-");
                readModePanel.add(readModeMinExpTime, new GridBagConstraints(2, 4, 2, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(17, 11, 0, 0), 0, 0));

                //---- readModeNoise ----
                readModeNoise.setText("-");
                readModePanel.add(readModeNoise, new GridBagConstraints(5, 4, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(17, 11, 0, 0), 0, 0));

                //---- readModeNoiseLabel ----
                readModeNoiseLabel.setToolTipText("");
                readModeNoiseLabel.setText("Read Noise:");
                readModePanel.add(readModeNoiseLabel, new GridBagConstraints(4, 4, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(17, 11, 0, 0), 0, 0));
                readModePanel.add(component1, new GridBagConstraints(0, 7, 1, 1, 0.0, 1.0,
                        GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
                        new Insets(0, 0, 0, 0), 0, 0));

                //---- readModeRecMinExpTimeLabel ----
                readModeRecMinExpTimeLabel.setText("Recommended Exposure Time:");
                readModePanel.add(readModeRecMinExpTimeLabel, new GridBagConstraints(0, 5, 2, 1, 0.0, 1.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(11, 11, 0, 0), 0, 0));

                //---- readModeRecMinExpTime ----
                readModeRecMinExpTime.setText("-");
                readModePanel.add(readModeRecMinExpTime, new GridBagConstraints(2, 5, 2, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(11, 11, 0, 0), 0, 0));

                //---- minExpTimeLabel ----
                minExpTimeLabel.setText("Minimum Exposure Time:");
                readModePanel.add(minExpTimeLabel, new GridBagConstraints(0, 4, 2, 1, 0.0, 1.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(17, 11, 0, 0), 0, 0));

                //---- shallowWellDepthButton ----
                shallowWellDepthButton.setText("Shallow well (1-2.5 um)");
                readModePanel.add(shallowWellDepthButton, new GridBagConstraints(0, 6, 2, 1, 0.0, 1.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(17, 11, 11, 0), 0, 0));

                //---- deepWellDepthButton ----
                deepWellDepthButton.setText("Deep well (3-5 um)");
                readModePanel.add(deepWellDepthButton, new GridBagConstraints(4, 6, 2, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(17, 11, 11, 0), 0, 0));
            }
            jTabbedPane1.addTab("Read Mode", readModePanel);

            //======== roiPanel ========
            {
                roiPanel.setLayout(new GridBagLayout());

                //---- roiFullFrameButton ----
                roiFullFrameButton.setToolTipText("Set the Region of Interest to the Full Frame");
                roiFullFrameButton.setSelected(true);
                roiFullFrameButton.setText("Full Frame Readout");
                roiPanel.add(roiFullFrameButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(6, 11, 0, 0), 0, 0));

                //---- roiCentral768Button ----
                roiCentral768Button.setToolTipText("Set the Region of Interest to the central 768x768 pixels");
                roiCentral768Button.setText("Central 768x768");
                roiPanel.add(roiCentral768Button, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(6, 11, 0, 0), 0, 0));

                //---- roiCentral256Button ----
                roiCentral256Button.setToolTipText("Set the Region of Interest to the central 256x256 pixels");
                roiCentral256Button.setText("Central 256x256");
                roiPanel.add(roiCentral256Button, new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(6, 11, 0, 0), 0, 0));

                //---- roiCentral512Button ----
                roiCentral512Button.setToolTipText("Set the Region of Interest to the central 128x128 pixels");
                roiCentral512Button.setText("Central 512x512");
                roiPanel.add(roiCentral512Button, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(6, 11, 0, 0), 0, 0));

                //---- roiSpec1024x512Button ----
                roiSpec1024x512Button.setText("Spectroscopy 1024x512");
                roiPanel.add(roiSpec1024x512Button, new GridBagConstraints(0, 4, 1, 1, 1.0, 1.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(6, 11, 0, 0), 0, 0));
                roiPanel.add(component2, new GridBagConstraints(0, 5, 1, 1, 0.0, 2.0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0));
            }
            jTabbedPane1.addTab("Array / Subarray Size", roiPanel);

        }
        add(jTabbedPane1, new GridBagConstraints(0, 8, 5, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(6, 6, 6, 6), 0, 0));

        //---- readModeButtonGroup ----
        ButtonGroup readModeButtonGroup = new ButtonGroup();
        readModeButtonGroup.add(readMode1To25Button);
        readModeButtonGroup.add(readModeNarrowBandButton);
        readModeButtonGroup.add(readMode3To5Button);

        //---- wellButtonGroup ----
        ButtonGroup wellButtonGroup = new ButtonGroup();
        wellButtonGroup.add(shallowWellDepthButton);
        wellButtonGroup.add(deepWellDepthButton);

        //---- roiButtonGroup ----
        ButtonGroup roiButtonGroup = new ButtonGroup();
        roiButtonGroup.add(roiFullFrameButton);
        roiButtonGroup.add(roiCentral768Button);
        roiButtonGroup.add(roiCentral256Button);
        roiButtonGroup.add(roiCentral512Button);
        roiButtonGroup.add(roiSpec1024x512Button);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    DropDownListBoxWidget camera;
    NumberBoxWidget exposureTime;
    DropDownListBoxWidget disperser;
    DropDownListBoxWidget mask;
    NumberBoxWidget coadds;
    NumberBoxWidget posAngle;
    DropDownListBoxWidget beamSplitter;
    SingleSelectComboBox selectedFilter;
    NumberBoxWidget fastModeExposures;
    JLabel scienceFOV;
    JRadioButton readMode1To25Button;
    JRadioButton readModeNarrowBandButton;
    JRadioButton readMode3To5Button;
    JLabel readModeLowBgLabel;
    JLabel readModeMediumBgLabel;
    JLabel readModeHighBgLabel;
    JLabel readModeMinExpTime;
    JLabel readModeNoise;
    JLabel readModeNoiseLabel;
    JLabel readModeRecMinExpTimeLabel;
    JLabel readModeRecMinExpTime;
    JLabel minExpTimeLabel;
    JRadioButton shallowWellDepthButton;
    JRadioButton deepWellDepthButton;
    JRadioButton roiFullFrameButton;
    JRadioButton roiCentral768Button;
    JRadioButton roiCentral256Button;
    JRadioButton roiCentral512Button;
    JRadioButton roiSpec1024x512Button;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
