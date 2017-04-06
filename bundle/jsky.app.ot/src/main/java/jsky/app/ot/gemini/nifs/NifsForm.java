package jsky.app.ot.gemini.nifs;

import java.awt.*;
import javax.swing.*;
import jsky.util.gui.*;

public class NifsForm extends JPanel {
	public NifsForm() {
		initComponents();
	}

	private void initComponents() {
		JLabel expTimeLabel = new JLabel();
		exposureTime = new NumberBoxWidget();
		JLabel disperserLabel = new JLabel();
		disperser = new DropDownListBoxWidget();
		JLabel maskLabel = new JLabel();
		mask = new DropDownListBoxWidget();
		JLabel coaddsLabel = new JLabel();
		JLabel posAngleLabel = new JLabel();
		coadds = new NumberBoxWidget();
		posAngle = new NumberBoxWidget();
		JLabel scienceFOVLabel = new JLabel();
		JLabel expTimeUnits = new JLabel();
		JLabel coaddsUnits = new JLabel();
		JLabel posAngleUnits = new JLabel();
		JLabel imagingMirrorLabel = new JLabel();
		imagingMirror = new DropDownListBoxWidget();
		JLabel filterLabel = new JLabel();
		filter = new DropDownListBoxWidget();
		scienceFOV = new JLabel();
		JTabbedPane jTabbedPane1 = new JTabbedPane();
		JPanel readModePanel = new JPanel();
		readModeFaintButton = new JRadioButton();
		readModeBrightButton = new JRadioButton();
		readModeFaintLabel = new JLabel();
		readModeMediumButton = new JRadioButton();
		readModeMediumLabel = new JLabel();
		readModeBrightLabel = new JLabel();
		readModeMinExpTime = new JLabel();
		readModeNoise = new JLabel();
		JLabel readModeNoiseLabel = new JLabel();
		JLabel readModeRecMinExpTimeLabel = new JLabel();
		readModeRecMinExpTime = new JLabel();
		JLabel minExpTimeLabel = new JLabel();
		JLabel centralWavelengthLabel = new JLabel();
		centralWavelength = new NumberBoxWidget();
		JLabel maskOffsetLabel = new JLabel();
		maskOffset = new NumberBoxWidget();

		//======== this ========
		setLayout(new GridBagLayout());

		//---- expTimeLabel ----
		expTimeLabel.setToolTipText("Exposure time in seconds");
		expTimeLabel.setLabelFor(null);
		expTimeLabel.setText("Exposure Time");
		add(expTimeLabel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));
		add(exposureTime, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(0, 11, 0, 0), 0, 0));

		//---- disperserLabel ----
		disperserLabel.setLabelFor(null);
		disperserLabel.setText("Disperser");
		add(disperserLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));

		//---- disperser ----
		disperser.setToolTipText("Select the Disperser to use");
		add(disperser, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(0, 11, 0, 0), 0, 0));

		//---- maskLabel ----
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
			new Insets(0, 3, 0, 0), 0, 0));

		//---- coaddsUnits ----
		coaddsUnits.setText("exp/obs");
		add(coaddsUnits, new GridBagConstraints(4, 1, 2, 1, 1.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(0, 3, 0, 0), 0, 0));

		//---- posAngleUnits ----
		posAngleUnits.setText("deg E of N");
		add(posAngleUnits, new GridBagConstraints(4, 3, 1, 1, 1.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(0, 3, 0, 6), 0, 0));

		//---- imagingMirrorLabel ----
		imagingMirrorLabel.setLabelFor(null);
		imagingMirrorLabel.setText("Imaging Mirror");
		add(imagingMirrorLabel, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, -1000), 0, 0));
		add(imagingMirror, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(0, 11, 0, 0), -10, 0));

		//---- filterLabel ----
		filterLabel.setLabelFor(null);
		filterLabel.setText("Filter");
		add(filterLabel, new GridBagConstraints(0, 4, 1, 1, 1.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));

		//---- filter ----
		filter.setToolTipText("Select the Filter (Broadband, Narrowband)");
		add(filter, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(0, 11, 0, 0), 0, 0));

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
				
				//---- readModeFaintButton ----
				readModeFaintButton.setText("1-2.5um: Faint Object Spectroscopy");
				readModePanel.add(readModeFaintButton, new GridBagConstraints(0, 0, 4, 1, 1.0, 1.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(5, 11, 0, 0), 0, 0));
				
				//---- readModeBrightButton ----
				readModeBrightButton.setText("1-2.5um: Bright Object Spectroscopy");
				readModePanel.add(readModeBrightButton, new GridBagConstraints(0, 2, 3, 1, 0.0, 1.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(5, 11, 0, 0), 0, 0));
				
				//---- readModeFaintLabel ----
				readModeFaintLabel.setEnabled(false);
				readModeFaintLabel.setRequestFocusEnabled(true);
				readModeFaintLabel.setText("Weak Source");
				readModePanel.add(readModeFaintLabel, new GridBagConstraints(4, 0, 2, 1, 1.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(5, 11, 0, 0), 0, 0));
				
				//---- readModeMediumButton ----
				readModeMediumButton.setText("1-2.5um: Medium Object Spectroscopy");
				readModePanel.add(readModeMediumButton, new GridBagConstraints(0, 1, 4, 1, 1.0, 1.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(5, 11, 0, 0), 0, 0));
				
				//---- readModeMediumLabel ----
				readModeMediumLabel.setText("Medium Source");
				readModeMediumLabel.setEnabled(false);
				readModePanel.add(readModeMediumLabel, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(5, 11, 0, 0), 0, 0));
				
				//---- readModeBrightLabel ----
				readModeBrightLabel.setEnabled(false);
				readModeBrightLabel.setText("Strong Source");
				readModePanel.add(readModeBrightLabel, new GridBagConstraints(4, 2, 2, 1, 1.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(5, 11, 0, 0), 0, 0));
				
				//---- readModeMinExpTime ----
				readModeMinExpTime.setText("-");
				readModePanel.add(readModeMinExpTime, new GridBagConstraints(2, 4, 2, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(11, 11, 0, 0), 0, 0));
				
				//---- readModeNoise ----
				readModeNoise.setText("10 e-");
				readModePanel.add(readModeNoise, new GridBagConstraints(5, 4, 1, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(17, 11, 0, 0), 0, 0));
				
				//---- readModeNoiseLabel ----
				readModeNoiseLabel.setText("Read Noise");
				readModePanel.add(readModeNoiseLabel, new GridBagConstraints(4, 4, 1, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(17, 11, 0, 0), 0, 0));
				
				//---- readModeRecMinExpTimeLabel ----
				readModeRecMinExpTimeLabel.setText("Recommended Exposure Time:");
				readModePanel.add(readModeRecMinExpTimeLabel, new GridBagConstraints(0, 5, 2, 1, 0.0, 1.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(11, 11, 0, 0), 0, 0));
				
				//---- readModeRecMinExpTime ----
				readModeRecMinExpTime.setText(">90.0 sec");
				readModePanel.add(readModeRecMinExpTime, new GridBagConstraints(2, 5, 2, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(11, 11, 0, 0), 0, 0));
				
				//---- minExpTimeLabel ----
				minExpTimeLabel.setText("Minimum Exposure Time:");
				readModePanel.add(minExpTimeLabel, new GridBagConstraints(0, 4, 2, 1, 0.0, 1.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(11, 11, 0, 0), 0, 0));
			}
			jTabbedPane1.addTab("Read Mode", readModePanel);
			
		}
		add(jTabbedPane1, new GridBagConstraints(0, 8, 5, 1, 1.0, 1.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(6, 6, 6, 6), 0, 0));

		//---- centralWavelengthLabel ----
		centralWavelengthLabel.setLabelFor(null);
		centralWavelengthLabel.setText("Central Wavelength");
		add(centralWavelengthLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));

		//---- centralWavelength ----
		centralWavelength.setToolTipText("Central wavelength in microns");
		add(centralWavelength, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(0, 11, 0, 0), 0, 0));

		//---- maskOffsetLabel ----
		maskOffsetLabel.setText("Mask Offset");
		add(maskOffsetLabel, new GridBagConstraints(3, 4, 2, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));

		//---- maskOffset ----
		maskOffset.setToolTipText("Enter the occulting disk offset");
		add(maskOffset, new GridBagConstraints(3, 5, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(0, 11, 0, 0), 0, 0));

		//---- readModeButtonGroup ----
		ButtonGroup readModeButtonGroup = new ButtonGroup();
		readModeButtonGroup.add(readModeFaintButton);
		readModeButtonGroup.add(readModeBrightButton);
		readModeButtonGroup.add(readModeMediumButton);
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner non-commercial license
	NumberBoxWidget exposureTime;
	DropDownListBoxWidget disperser;
	DropDownListBoxWidget mask;
	NumberBoxWidget coadds;
	NumberBoxWidget posAngle;
	DropDownListBoxWidget imagingMirror;
	DropDownListBoxWidget filter;
	JLabel scienceFOV;
	JRadioButton readModeFaintButton;
	JRadioButton readModeBrightButton;
	JLabel readModeFaintLabel;
	JRadioButton readModeMediumButton;
	JLabel readModeMediumLabel;
	JLabel readModeBrightLabel;
	JLabel readModeMinExpTime;
	JLabel readModeNoise;
	JLabel readModeRecMinExpTime;
	NumberBoxWidget centralWavelength;
	NumberBoxWidget maskOffset;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
