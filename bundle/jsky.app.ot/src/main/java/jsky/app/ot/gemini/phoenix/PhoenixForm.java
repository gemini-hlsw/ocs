package jsky.app.ot.gemini.phoenix;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import edu.gemini.spModel.gemini.phoenix.PhoenixParams;
import jsky.util.gui.*;

public class PhoenixForm extends JPanel {

    final JComboBox<PhoenixParams.Filter> selectedFilter;
    final NumberBoxWidget posAngle;
    final NumberBoxWidget coadds;
    final DropDownListBoxWidget mask;
    final NumberBoxWidget exposureTime;
    final JLabel scienceFOV;
    final JRadioButton wavelengthRadioButton;
    final JLabel wavelengthUnits;
    final JRadioButton wavenumberRadioButton;
    final JLabel wavenumberUnits;
    final NumberBoxWidget gratingWavelength;
    final NumberBoxWidget gratingWavenumber;

    public PhoenixForm() {
        JLabel filterLabel = new JLabel();
        JLabel maskLabel = new JLabel();
        JLabel posAngleLabel = new JLabel();
        posAngle = new NumberBoxWidget();
        JLabel posAngleUnits = new JLabel();
        JLabel expTimeLabel = new JLabel();
        JLabel coaddsLabel = new JLabel();
        coadds = new NumberBoxWidget();
        selectedFilter = new JComboBox<>();
        JLabel expTimeUnits = new JLabel();
        JLabel coaddsUnits = new JLabel();
        mask = new DropDownListBoxWidget();
        exposureTime = new NumberBoxWidget();
        JLabel component1 = new JLabel();
        JLabel scienceFOVLabel = new JLabel();
        scienceFOV = new JLabel();
        JPanel jPanel1 = new JPanel();
        wavelengthRadioButton = new JRadioButton();
        wavelengthUnits = new JLabel();
        wavenumberRadioButton = new JRadioButton();
        wavenumberUnits = new JLabel();
        gratingWavelength = new NumberBoxWidget();
        gratingWavenumber = new NumberBoxWidget();

        //======== this ========
        setToolTipText("");
        setLayout(new GridBagLayout());

        //---- filterLabel ----
        filterLabel.setLabelFor(null);
        filterLabel.setText("Filter");
        add(filterLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(0, 11, 0, 0), 0, 0));

        //---- maskLabel ----
        maskLabel.setToolTipText("");
        maskLabel.setLabelFor(null);
        maskLabel.setText("Focal Plane Mask");
        add(maskLabel, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(11, 11, 0, 0), 0, 0));

        //---- posAngleLabel ----
        posAngleLabel.setToolTipText("Position angle in degrees E of N");
        posAngleLabel.setLabelFor(null);
        posAngleLabel.setText("Position Angle");
        add(posAngleLabel, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(11, 11, 0, 0), 0, 0));

        //---- posAngle ----
        posAngle.setToolTipText("Enter the position angle in degrees E of N");
        add(posAngle, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(0, 11, 0, 0), 0, 0));

        //---- posAngleUnits ----
        posAngleUnits.setText("deg E of N");
        add(posAngleUnits, new GridBagConstraints(2, 3, 1, 1, 1.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(0, 3, 0, 0), 0, 0));

        //---- expTimeLabel ----
        expTimeLabel.setToolTipText("Exposure time in seconds");
        expTimeLabel.setLabelFor(null);
        expTimeLabel.setText("Exposure Time");
        add(expTimeLabel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(11, 11, 0, 0), 0, 0));

        //---- coaddsLabel ----
        coaddsLabel.setToolTipText("Coadds (number of exposures per observation)");
        coaddsLabel.setLabelFor(null);
        coaddsLabel.setText("Coadds");
        add(coaddsLabel, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(11, 11, 0, 0), 0, 0));

        //---- coadds ----
        coadds.setToolTipText("Enter the coadds (number of exposures per observation)");
        add(coadds, new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(0, 11, 0, 0), 0, 0));

        //---- selectedFilter ----
        selectedFilter.setToolTipText("Select the Filter");
        add(selectedFilter, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(0, 11, 0, 0), 0, 0));

        //---- expTimeUnits ----
        expTimeUnits.setText("sec");
        add(expTimeUnits, new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(0, 3, 0, 0), 0, 0));

        //---- coaddsUnits ----
        coaddsUnits.setToolTipText("");
        coaddsUnits.setText("exp/obs");
        add(coaddsUnits, new GridBagConstraints(4, 1, 1, 1, 1.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(0, 3, 0, 0), 0, 0));

        //---- mask ----
        mask.setToolTipText("Select the Focal Plane Mask");
        mask.setMaximumRowCount(9);
        add(mask, new GridBagConstraints(0, 3, 1, 1, 1.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(0, 11, 0, 0), 0, 0));

        //---- exposureTime ----
        exposureTime.setToolTipText("Enter the exposure time in seconds");
        add(exposureTime, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(0, 11, 0, 0), 0, 0));
        add(component1, new GridBagConstraints(0, 8, 4, 1, 2.0, 2.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- scienceFOVLabel ----
        scienceFOVLabel.setToolTipText("Science field of view in arcsec");
        scienceFOVLabel.setText("Science FOV");
        add(scienceFOVLabel, new GridBagConstraints(0, 5, 1, 1, 1.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(11, 11, 0, 0), 0, 0));

        //---- scienceFOV ----
        scienceFOV.setText("000.0000 arcsec");
        add(scienceFOV, new GridBagConstraints(0, 6, 5, 1, 0.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(0, 11, 0, 0), 0, 0));

        //======== jPanel1 ========
        {
            jPanel1.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)), "Grating Angle"));
            jPanel1.setLayout(new GridBagLayout());

            //---- wavelengthRadioButton ----
            wavelengthRadioButton.setActionCommand("wavelength");
            wavelengthRadioButton.setSelected(true);
            wavelengthRadioButton.setText("Wavelength");
            jPanel1.add(wavelengthRadioButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
                    new Insets(0, 11, 0, 0), 0, 0));

            //---- wavelengthUnits ----
            wavelengthUnits.setText("um");
            jPanel1.add(wavelengthUnits, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(0, 6, 0, 6), 0, 0));

            //---- wavenumberRadioButton ----
            wavenumberRadioButton.setActionCommand("wavenumber");
            wavenumberRadioButton.setText("Wavenumber");
            jPanel1.add(wavenumberRadioButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
                    new Insets(0, 11, 0, 0), 0, 0));

            //---- wavenumberUnits ----
            wavenumberUnits.setText("1/cm");
            jPanel1.add(wavenumberUnits, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(0, 6, 0, 6), 0, 0));

            //---- gratingWavelength ----
            gratingWavelength.setToolTipText("Enter the grating angle in um units");
            jPanel1.add(gratingWavelength, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 11, 0), 0, 0));

            //---- gratingWavenumber ----
            gratingWavenumber.setToolTipText("Enter the grating angle in 1/cm units");
            jPanel1.add(gratingWavenumber, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 0), 0, 0));
        }
        add(jPanel1, new GridBagConstraints(0, 7, 2, 1, 1.0, 0.2,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(11, 6, 0, 6), 0, 0));

        //---- buttonGroup1 ----
        ButtonGroup buttonGroup1 = new ButtonGroup();
        buttonGroup1.add(wavelengthRadioButton);
        buttonGroup1.add(wavenumberRadioButton);
    }

}
