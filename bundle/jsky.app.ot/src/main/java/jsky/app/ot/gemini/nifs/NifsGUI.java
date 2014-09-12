/**
 * Title:        JSky<p>
 * Description:  NIFS Instrument Editor GUI<p>
 * Company:      Gemini<p>
 * @author Allan Brighton
 * @version 1.0
 */
package jsky.app.ot.gemini.nifs;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.NumberBoxWidget;

import java.awt.event.*;

public class NifsGUI extends JPanel {

    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel expTimeLabel = new JLabel();
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
    JLabel imagingMirrorLabel = new JLabel();
    DropDownListBoxWidget imagingMirror = new DropDownListBoxWidget();
    JTabbedPane jTabbedPane1 = new JTabbedPane();
    JPanel readModePanel = new JPanel();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    JRadioButton readModeFaintButton = new JRadioButton();
    JRadioButton readModeBrightButton = new JRadioButton();
    JLabel readModeFaintLabel = new JLabel();
    JLabel readModeBrightLabel = new JLabel();
    JLabel readModeMinExpTime = new JLabel();
    JLabel readModeNoise = new JLabel();
    JLabel readModeNoiseLabel = new JLabel();
    ButtonGroup filterButtonGroup = new ButtonGroup();
    ButtonGroup readModeButtonGroup = new ButtonGroup();
    JLabel filterLabel = new JLabel();
    DropDownListBoxWidget filter = new DropDownListBoxWidget();
    Component component1;
    JLabel scienceFOV = new JLabel();
    ButtonGroup roiButtonGroup = new ButtonGroup();
    ButtonGroup wellButtonGroup = new ButtonGroup();
    JLabel readModeRecMinExpTimeLabel = new JLabel();
    JLabel readModeRecMinExpTime = new JLabel();
    JLabel minExpTimeLabel = new JLabel();
    JLabel centralWavelengthLabel = new JLabel();
    NumberBoxWidget centralWavelength = new NumberBoxWidget();
    JLabel maskOffsetLabel = new JLabel();
    NumberBoxWidget maskOffset = new NumberBoxWidget();
    public NifsGUI() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        titledBorder1 =
                new TitledBorder(
                        new EtchedBorder(EtchedBorder.RAISED, Color.white,
                                new Color(142, 142, 142)),
                        "Filter");


        component1 = Box.createVerticalStrut(8);
        this.setMinimumSize(new Dimension(350, 378));
        this.setPreferredSize(new Dimension(350, 378));
        this.setLayout(gridBagLayout1);


        expTimeLabel.setToolTipText("Exposure time in seconds");
        expTimeLabel.setLabelFor(exposureTime);
        expTimeLabel.setText("Exposure Time");
        exposureTime.setAllowNegative(false);

        disperserLabel.setLabelFor(disperser);
        disperserLabel.setText("Disperser");


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
        coaddsUnits.setText("exp/obs");
        posAngleUnits.setText("deg E of N");
        imagingMirrorLabel.setLabelFor(imagingMirror);
        imagingMirrorLabel.setText("Imaging Mirror");
        readModePanel.setLayout(gridBagLayout3);
        readModeFaintButton.setText("1-2.5um: Faint Object Spectroscopy");
        readModeFaintButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                readModeFaintButton_actionPerformed(e);
            }
        });
        readModeBrightButton.setText("1-2.5um: Bright Object Spectroscopy");
        readModeFaintLabel.setEnabled(false);
        readModeFaintLabel.setRequestFocusEnabled(true);
        readModeFaintLabel.setText("Weak Source");
        readModeBrightLabel.setEnabled(false);
        readModeBrightLabel.setText("Strong Source");
        readModeMinExpTime.setText("-");
        readModeNoise.setText("10 e-");
        readModeNoiseLabel.setText("Read Noise");
        filterLabel.setLabelFor(filter);
        filterLabel.setText("Filter");
        filter.setToolTipText("Select the Filter (Broadband, Narrowband)");
        mask.setToolTipText("Select the Focal Plane Mask");
        mask.setMaximumRowCount(9);
        disperser.setToolTipText("Select the Disperser to use");
        scienceFOV.setBorder(null);
        scienceFOV.setText("000.0000 arcsecs");
        readModeRecMinExpTimeLabel.setText("Recommended Exposure Time:");
        readModeRecMinExpTime.setText(">90.0 sec");
        minExpTimeLabel.setText("Minimum Exposure Time:");
        centralWavelengthLabel.setLabelFor(centralWavelength);
        centralWavelengthLabel.setText("Central Wavelength");
        centralWavelength.setMinimumSize(new Dimension(80, 21));
        maskOffsetLabel.setText("Occulting Disk Offset");
        maskOffset.setToolTipText("Enter the occulting disk offset");
        this.add(expTimeLabel,
                new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(exposureTime,
                new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        this.add(disperserLabel,
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(disperser,
                new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        this.add(maskLabel,
                new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(mask,
                new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), -10, 0));
        this.add(coaddsLabel,
                new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(posAngleLabel,
                new GridBagConstraints(3, 2, 2, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(coadds,
                new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        this.add(posAngle,
                new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        this.add(scienceFOVLabel,
                new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(expTimeUnits,
                 new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 3, 0, 0), 0, 0));
        this.add(coaddsUnits,
                 new GridBagConstraints(4, 1, 2, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 3, 0, 0), 0, 0));
        this.add(posAngleUnits,
                 new GridBagConstraints(4, 3, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 3, 0, 6), 0, 0));
        this.add(imagingMirrorLabel,
                new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(11, 11, 0, -1000), 0, 0));
        this.add(imagingMirror,
                new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), -10, 0));
        this.add(filterLabel,
                new GridBagConstraints(0, 4, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(filter,
                new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        readModeButtonGroup.add(readModeFaintButton);
        readModeButtonGroup.add(readModeBrightButton);
        this.add(scienceFOV,
                new GridBagConstraints(0, 7, 5, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        this.add(jTabbedPane1,
                new GridBagConstraints(0, 8, 5, 1, 1.0, 1.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
                        new Insets(6, 6, 6, 6), 0, 0));
        jTabbedPane1.add(readModePanel,   "Read Mode");
        readModePanel.add(readModeFaintButton,
                 new GridBagConstraints(0, 0, 4, 1, 1.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 11, 0, 0), 0, 0));
        readModePanel.add(readModeBrightButton,
                 new GridBagConstraints(0, 1, 3, 1, 0.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 11, 0, 0), 0, 0));
        readModePanel.add(readModeFaintLabel,
                 new GridBagConstraints(4, 0, 2, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 11, 0, 0), 0, 0));
        readModePanel.add(readModeBrightLabel,
                 new GridBagConstraints(4, 1, 2, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 11, 0, 0), 0, 0));
        readModePanel.add(readModeMinExpTime,
                 new GridBagConstraints(2, 3, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        readModePanel.add(readModeNoise,
                 new GridBagConstraints(5, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(17, 11, 0, 0), 0, 0));
        readModePanel.add(readModeNoiseLabel,
                 new GridBagConstraints(4, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(17, 11, 0, 0), 0, 0));
        readModePanel.add(component1,
                 new GridBagConstraints(0, 5, 1, 1, 0.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
        readModePanel.add(readModeRecMinExpTimeLabel,
                 new GridBagConstraints(0, 4, 2, 1, 0.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        readModePanel.add(readModeRecMinExpTime,
                 new GridBagConstraints(2, 4, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        readModePanel.add(minExpTimeLabel,
                 new GridBagConstraints(0, 3, 2, 1, 0.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(centralWavelengthLabel,
                new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(centralWavelength,
                new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        this.add(maskOffsetLabel,
                new GridBagConstraints(3, 4, 2, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(maskOffset, new GridBagConstraints(3, 5, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 11, 0, 0), 0, 0));
    }

    void readModeFaintButton_actionPerformed(ActionEvent e) {

    }


}
