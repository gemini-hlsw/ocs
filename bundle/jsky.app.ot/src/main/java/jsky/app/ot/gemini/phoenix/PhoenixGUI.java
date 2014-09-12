/**
 * Title:        JSky<p>
 * Description:  Phoenix Instrument Editor GUI<p>
 * Company:      Gemini<p>
 * @version 1.0
 */
package jsky.app.ot.gemini.phoenix;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;

import jsky.util.gui.*;

public class PhoenixGUI extends JPanel {
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel expTimeLabel = new JLabel();
    NumberBoxWidget exposureTime = new NumberBoxWidget();
    JLabel maskLabel = new JLabel();
    DropDownListBoxWidget mask = new DropDownListBoxWidget();
    JLabel coaddsLabel = new JLabel();
    JLabel posAngleLabel = new JLabel();
    NumberBoxWidget coadds = new NumberBoxWidget();
    NumberBoxWidget posAngle = new NumberBoxWidget();
    JLabel expTimeUnits = new JLabel();
    JLabel coaddsUnits = new JLabel();
    JLabel posAngleUnits = new JLabel();
    JLabel filterLabel = new JLabel();
    DropDownListBoxWidget selectedFilter = new DropDownListBoxWidget();
    Component component1;
    JLabel scienceFOVLabel = new JLabel();
    JLabel scienceFOV = new JLabel();
    JPanel jPanel1 = new JPanel();
    OptionWidget wavelengthRadioButton = new OptionWidget();
    JLabel wavelengthUnits = new JLabel();
    OptionWidget wavenumberRadioButton = new OptionWidget();
    JLabel wavenumberUnits = new JLabel();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    TitledBorder titledBorder1;
    ButtonGroup buttonGroup1 = new ButtonGroup();
    NumberBoxWidget gratingWavelength = new NumberBoxWidget();
    NumberBoxWidget gratingWavenumber = new NumberBoxWidget();

    public PhoenixGUI() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {

        component1 = Box.createGlue();
        titledBorder1 = new TitledBorder(new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)), "Grating Angle");
        this.setMinimumSize(new Dimension(350, 378));
        this.setPreferredSize(new Dimension(350, 378));
        this.setToolTipText("");
        this.setLayout(gridBagLayout1);


        expTimeLabel.setToolTipText("Exposure time in seconds");
        expTimeLabel.setLabelFor(exposureTime);
        expTimeLabel.setText("Exposure Time");
        exposureTime.setAllowNegative(false);
        exposureTime.setToolTipText("Enter the exposure time in seconds");


        maskLabel.setToolTipText("");
        maskLabel.setLabelFor(mask);
        maskLabel.setText("Focal Plane Mask");


        coaddsLabel.setToolTipText("Coadds (number of exposures per observation)");
        coaddsLabel.setLabelFor(coadds);
        coaddsLabel.setText("Coadds");


        posAngleLabel.setToolTipText("Position angle in degrees E of N");
        posAngleLabel.setLabelFor(posAngle);
        posAngleLabel.setText("Position Angle");


        coadds.setToolTipText("Enter the coadds (number of exposures per observation)");
        coadds.setAllowNegative(false);
        posAngle.setToolTipText("Enter the position angle in degrees E of N");
        expTimeUnits.setText("sec");
        coaddsUnits.setToolTipText("");
        coaddsUnits.setText("exp/obs");
        posAngleUnits.setText("deg E of N");
        filterLabel.setLabelFor(selectedFilter);
        filterLabel.setText("Filter");
        selectedFilter.setToolTipText("Select the Filter");
        selectedFilter.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                selectedFilter_actionPerformed(e);
            }
        });
        mask.setToolTipText("Select the Focal Plane Mask");
        mask.setMaximumRowCount(9);
        mask.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mask_actionPerformed(e);
            }
        });


        scienceFOVLabel.setToolTipText("Science field of view in arcsec");
        scienceFOVLabel.setText("Science FOV");
        scienceFOV.setText("000.0000 arcsec");
        wavelengthRadioButton.setText("Wavelength");
        wavelengthRadioButton.setSelected(true);
        wavelengthRadioButton.setActionCommand("wavelength");
        wavelengthUnits.setText("um");
        wavenumberRadioButton.setText("Wavenumber");
        wavenumberRadioButton.setActionCommand("wavenumber");
        wavenumberUnits.setText("1/cm");
        jPanel1.setLayout(gridBagLayout2);
        jPanel1.setBorder(titledBorder1);
        gratingWavelength.setAllowNegative(false);
        gratingWavelength.setToolTipText("Enter the grating angle in um units");
        gratingWavenumber.setAllowNegative(false);
        gratingWavenumber.setToolTipText("Enter the grating angle in 1/cm units");
        this.add(filterLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
                                                     , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        this.add(maskLabel, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0
                                                   , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(posAngleLabel, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
                                                       , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(posAngle, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0
                                                  , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        this.add(posAngleUnits,  new GridBagConstraints(2, 3, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 3, 0, 0), 0, 0));
        this.add(expTimeLabel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
                                                      , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(coaddsLabel, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0
                                                     , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(coadds, new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0
                                                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        this.add(selectedFilter, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0
                                                        , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        this.add(expTimeUnits,  new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 3, 0, 0), 0, 0));
        this.add(coaddsUnits,  new GridBagConstraints(4, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 3, 0, 0), 0, 0));
        this.add(mask, new GridBagConstraints(0, 3, 1, 1, 1.0, 0.0
                                              , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        this.add(exposureTime, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
                                                      , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 0, 0), 0, 0));
        this.add(component1, new GridBagConstraints(0, 8, 4, 1, 2.0, 2.0
                                                    , GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        this.add(scienceFOVLabel, new GridBagConstraints(0, 5, 1, 1, 1.0, 0.0
                                                         , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(scienceFOV, new GridBagConstraints(0, 6, 5, 1, 0.0, 0.0
                                                    , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        this.add(jPanel1, new GridBagConstraints(0, 7, 2, 1, 1.0, 0.2
                                                 , GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(11, 6, 0, 6), 0, 0));
        jPanel1.add(wavelengthRadioButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                                                                  , GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 11, 0, 0), 0, 0));
        jPanel1.add(wavelengthUnits, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
                                                            , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 6), 0, 0));
        jPanel1.add(wavenumberRadioButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                                                                  , GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(0, 11, 0, 0), 0, 0));
        jPanel1.add(wavenumberUnits, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
                                                            , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 6), 0, 0));
        jPanel1.add(gratingWavelength, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
                                                              , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 11, 0), 0, 0));
        jPanel1.add(gratingWavenumber, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
                                                              , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        buttonGroup1.add(wavelengthRadioButton);
        buttonGroup1.add(wavenumberRadioButton);
    }


    void mask_actionPerformed(ActionEvent e) {

    }

    void selectedFilter_actionPerformed(ActionEvent e) {

    }

}
