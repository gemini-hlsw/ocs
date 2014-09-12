package jsky.app.ot.gemini.texes;

import java.awt.*;
import javax.swing.*;
import jsky.util.gui.*;
/*
 * Created by JFormDesigner on Wed Apr 26 11:49:25 CLT 2006
 */

/**
 * $Id%
 */
public class TexesForm extends JPanel {
	public TexesForm() {
		initComponents();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner non-commercial license
		JLabel filterLabel = new JLabel();
		JLabel posAngleLabel = new JLabel();
		posAngle = new NumberBoxWidget();
		JLabel posAngleUnits = new JLabel();
		JLabel expTimeLabel = new JLabel();
		JLabel coaddsLabel = new JLabel();
		coadds = new NumberBoxWidget();
		selectedDisperser = new DropDownListBoxWidget();
		JLabel expTimeUnits = new JLabel();
		JLabel coaddsUnits = new JLabel();
		exposureTime = new NumberBoxWidget();
		JLabel posAngleLabel2 = new JLabel();
		wavelenght = new NumberBoxWidget();
		JLabel scienceFOVLabel = new JLabel();
		scienceFOV = new JLabel();

		//======== this ========
		setToolTipText("");
		setLayout(new GridBagLayout());
		((GridBagLayout)getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		((GridBagLayout)getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

		//---- filterLabel ----
		filterLabel.setLabelFor(null);
		filterLabel.setText("Disperser");
		add(filterLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
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

		//---- selectedDisperser ----
		selectedDisperser.setToolTipText("Select the Disperser");
		add(selectedDisperser, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
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

		//---- exposureTime ----
		exposureTime.setToolTipText("Enter the exposure time in seconds");
		add(exposureTime, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(0, 11, 0, 0), 0, 0));

		//---- posAngleLabel2 ----
		posAngleLabel2.setToolTipText("Position angle in degrees E of N");
		posAngleLabel2.setLabelFor(null);
		posAngleLabel2.setText("Wavelength");
		add(posAngleLabel2, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));
		add(wavelenght, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(0, 11, 0, 0), 0, 0));

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
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	public NumberBoxWidget getPosAngle() {
		return posAngle;
	}

	public NumberBoxWidget getCoadds() {
		return coadds;
	}

	public DropDownListBoxWidget getSelectedDisperser() {
		return selectedDisperser;
	}

	public NumberBoxWidget getExposureTime() {
		return exposureTime;
	}

	public NumberBoxWidget getWavelenght() {
		return wavelenght;
	}

	public JLabel getScienceFOV() {
		return scienceFOV;
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner non-commercial license
	NumberBoxWidget posAngle;
	NumberBoxWidget coadds;
	DropDownListBoxWidget selectedDisperser;
	NumberBoxWidget exposureTime;
	private NumberBoxWidget wavelenght;
	JLabel scienceFOV;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
