package jsky.app.ot.gemini.michelle;

import jsky.util.gui.NumberBoxWidget;
import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
import java.awt.*;

public class MichelleForm extends JPanel {
	public MichelleForm() {
		initComponents();
	}

	private void initComponents() {
		JPanel top1 = new JPanel();
		filterLabel = new JLabel();
		totalOnSourceTimeLabel = new JLabel();
		filterComboBox = new JComboBox();
		filterOverride = new JLabel();
		totalOnSourceTime = new NumberBoxWidget();
		totalOnSourceTimeUnitsLabel = new JLabel();
		nodIntervalLabel = new JLabel();
		nodInterval = new NumberBoxWidget();
		nodIntervalUnitsLabel = new JLabel();
		focalPlaneMaskLabel = new JLabel();
		posAngleLabel = new JLabel();
		focalPlaneMaskComboBox = new JComboBox();
		posAngle = new NumberBoxWidget();
		posAngleUnitsLabel = new JLabel();
		disperserLabel = new JLabel();
		centralWavelengthLabel = new JLabel();
		disperserComboBox = new JComboBox();
		centralWavelength = new NumberBoxWidget();
		chopAngleLabel = new JLabel();
		chopThrowLabel = new JLabel();
		chopAngle = new NumberBoxWidget();
		chopThrow = new NumberBoxWidget();
		chopAngleUnitsLabel = new JLabel();
		JLabel scienceFOVLabel = new JLabel();
		centralWavelengthUnitsLabel = new JLabel();
		chopThrowUnitsLabel = new JLabel();
		scienceFOV = new JLabel();
		exposureTimeLabel = new JLabel();
		exposureTime = new TextBoxWidget();
		exposureTimeUnitsLabel = new JLabel();
		autoConfigureLabel = new JLabel();
		JPanel autoConfigurePanel = new JPanel();
		autoConfigureYesButton = new JRadioButton();
		autoConfigureNoButton = new JRadioButton();
		nodOrientationLabel = new JLabel();
		nodOrientationComboBox = new JComboBox();
		maskOverride = new JLabel();
		JLabel polarimetryLabel = new JLabel();
		JPanel polarimetryPanel = new JPanel();
		polarimetryYesButton = new JRadioButton();
		polarimetryNoButton = new JRadioButton();
		JPanel jPanel2 = new JPanel();

		//======== this ========
		setLayout(new GridBagLayout());

		//======== top1 ========
		{
			top1.setLayout(new GridBagLayout());
			
			//---- filterLabel ----
			filterLabel.setLabelFor(null);
			filterLabel.setText("Filter");
			top1.add(filterLabel, new GridBagConstraints(0, 0, 1, 1, 0.2, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));
			
			//---- totalOnSourceTimeLabel ----
			totalOnSourceTimeLabel.setLabelFor(null);
			totalOnSourceTimeLabel.setText("Total On-Source Time");
			top1.add(totalOnSourceTimeLabel, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));
			
			//---- filterComboBox ----
			filterComboBox.setFont(new Font("Dialog", Font.PLAIN, 12));
			filterComboBox.setToolTipText("Select the Filter to use");
			top1.add(filterComboBox, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));
			top1.add(filterOverride, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(0, 5, 0, 0), 0, 0));
			
			//---- totalOnSourceTime ----
			totalOnSourceTime.setToolTipText("Enter the Total On-Source Time in Seconds");
			top1.add(totalOnSourceTime, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));
			
			//---- totalOnSourceTimeUnitsLabel ----
			totalOnSourceTimeUnitsLabel.setText("sec");
			top1.add(totalOnSourceTimeUnitsLabel, new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 6, 0, 0), 0, 0));
			
			//---- nodIntervalLabel ----
			nodIntervalLabel.setLabelFor(null);
			nodIntervalLabel.setText("Nod Interval");
			top1.add(nodIntervalLabel, new GridBagConstraints(2, 4, 2, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));
			
			//---- nodInterval ----
			nodInterval.setToolTipText("Enter the Nod Interval in Seconds");
			top1.add(nodInterval, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));
			
			//---- nodIntervalUnitsLabel ----
			nodIntervalUnitsLabel.setText("sec");
			top1.add(nodIntervalUnitsLabel, new GridBagConstraints(3, 5, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 6, 0, 0), 0, 0));
			
			//---- focalPlaneMaskLabel ----
			focalPlaneMaskLabel.setLabelFor(null);
			focalPlaneMaskLabel.setText("Focal Plane Mask");
			top1.add(focalPlaneMaskLabel, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));
			
			//---- posAngleLabel ----
			posAngleLabel.setLabelFor(null);
			posAngleLabel.setText("Position Angle");
			top1.add(posAngleLabel, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));
			
			//---- focalPlaneMaskComboBox ----
			focalPlaneMaskComboBox.setToolTipText("Select the Focal Plane Mask to use");
			top1.add(focalPlaneMaskComboBox, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));
			
			//---- posAngle ----
			posAngle.setToolTipText("Enter the Position Angle in Degrees East of North");
			top1.add(posAngle, new GridBagConstraints(2, 7, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));
			
			//---- posAngleUnitsLabel ----
			posAngleUnitsLabel.setText("deg E of N");
			top1.add(posAngleUnitsLabel, new GridBagConstraints(3, 7, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 6, 0, 0), 0, 0));
			
			//---- disperserLabel ----
			disperserLabel.setLabelFor(null);
			disperserLabel.setText("Disperser");
			top1.add(disperserLabel, new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));
			
			//---- centralWavelengthLabel ----
			centralWavelengthLabel.setLabelFor(null);
			centralWavelengthLabel.setText("Grating Central Wavelength");
			top1.add(centralWavelengthLabel, new GridBagConstraints(2, 8, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));
			
			//---- disperserComboBox ----
			disperserComboBox.setToolTipText("Select the Disperser to use");
			top1.add(disperserComboBox, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));
			
			//---- centralWavelength ----
			centralWavelength.setToolTipText("Enter the Grating Central Wavelength in um");
			top1.add(centralWavelength, new GridBagConstraints(2, 9, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));
			
			//---- chopAngleLabel ----
			chopAngleLabel.setLabelFor(null);
			chopAngleLabel.setText("Chop Angle");
			top1.add(chopAngleLabel, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));
			
			//---- chopThrowLabel ----
			chopThrowLabel.setLabelFor(null);
			chopThrowLabel.setText("Chop Throw");
			top1.add(chopThrowLabel, new GridBagConstraints(2, 10, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));
			
			//---- chopAngle ----
			chopAngle.setToolTipText("Enter the Chop Angle in degrees East of North");
			top1.add(chopAngle, new GridBagConstraints(0, 11, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));
			
			//---- chopThrow ----
			chopThrow.setToolTipText("Enter the Chop Throw in arcsec");
			top1.add(chopThrow, new GridBagConstraints(2, 11, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));
			
			//---- chopAngleUnitsLabel ----
			chopAngleUnitsLabel.setText("deg E of N");
			top1.add(chopAngleUnitsLabel, new GridBagConstraints(1, 11, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 6, 0, 0), 0, 0));
			
			//---- scienceFOVLabel ----
			scienceFOVLabel.setText("Science FOV");
			top1.add(scienceFOVLabel, new GridBagConstraints(0, 12, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));
			
			//---- centralWavelengthUnitsLabel ----
			centralWavelengthUnitsLabel.setText("um");
			top1.add(centralWavelengthUnitsLabel, new GridBagConstraints(3, 9, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 6, 0, 0), 0, 0));
			
			//---- chopThrowUnitsLabel ----
			chopThrowUnitsLabel.setText("arcsec");
			top1.add(chopThrowUnitsLabel, new GridBagConstraints(3, 11, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 6, 0, 0), 0, 0));
			
			//---- scienceFOV ----
			scienceFOV.setToolTipText("The Calculated Field of View");
			scienceFOV.setText("000.000 arcsec");
			top1.add(scienceFOV, new GridBagConstraints(0, 13, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));
			
			//---- exposureTimeLabel ----
			exposureTimeLabel.setToolTipText("");
			exposureTimeLabel.setLabelFor(null);
			exposureTimeLabel.setText("Exposure (Frame) Time");
			top1.add(exposureTimeLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));
			
			//---- exposureTime ----
			exposureTime.setToolTipText("Set the Exposure (Frame) Time in Seconds");
			top1.add(exposureTime, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));
			
			//---- exposureTimeUnitsLabel ----
			exposureTimeUnitsLabel.setText("sec");
			top1.add(exposureTimeUnitsLabel, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 6, 0, 0), 0, 0));
			
			//---- autoConfigureLabel ----
			autoConfigureLabel.setLabelFor(null);
			autoConfigureLabel.setText("Auto-Configure");
			top1.add(autoConfigureLabel, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));
			
			//======== autoConfigurePanel ========
			{
				autoConfigurePanel.setLayout(new GridBagLayout());
				
				//---- autoConfigureYesButton ----
				autoConfigureYesButton.setToolTipText("Automatically configure the instrument exposure time");
				autoConfigureYesButton.setText("Yes");
				autoConfigurePanel.add(autoConfigureYesButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(0, 11, 0, 0), 0, 0));
				
				//---- autoConfigureNoButton ----
				autoConfigureNoButton.setToolTipText("Do not automatically configure the instrument exposure time");
				autoConfigureNoButton.setText("No");
				autoConfigurePanel.add(autoConfigureNoButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(0, 11, 0, 0), 0, 0));
			}
			top1.add(autoConfigurePanel, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0));
			
			//---- nodOrientationLabel ----
			nodOrientationLabel.setText("Nod Orientation");
			top1.add(nodOrientationLabel, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));
			top1.add(nodOrientationComboBox, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 11, 0, 0), 0, 0));
			top1.add(maskOverride, new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(0, 5, 0, 0), 0, 0));
			
			//---- polarimetryLabel ----
			polarimetryLabel.setText("Polarimetry");
			top1.add(polarimetryLabel, new GridBagConstraints(2, 12, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(11, 11, 0, 0), 0, 0));
			
			//======== polarimetryPanel ========
			{
				polarimetryPanel.setLayout(new GridBagLayout());
				
				//---- polarimetryYesButton ----
				polarimetryYesButton.setText("Yes");
				polarimetryPanel.add(polarimetryYesButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(0, 11, 0, 0), 0, 0));
				
				//---- polarimetryNoButton ----
				polarimetryNoButton.setText("No");
				polarimetryPanel.add(polarimetryNoButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(0, 11, 0, 0), 0, 0));
			}
			top1.add(polarimetryPanel, new GridBagConstraints(2, 13, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0));
		}
		add(top1, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(0, 0, 37, 0), 0, 0));

		//======== jPanel2 ========
		{
			jPanel2.setLayout(new FlowLayout());
		}
		add(jPanel2, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(0, 0, 0, 0), 0, 0));

		//---- autoConfigureButtonGroup ----
		ButtonGroup autoConfigureButtonGroup = new ButtonGroup();
		autoConfigureButtonGroup.add(autoConfigureYesButton);
		autoConfigureButtonGroup.add(autoConfigureNoButton);

		//---- polarimetryButtonGroup ----
		ButtonGroup polarimetryButtonGroup = new ButtonGroup();
		polarimetryButtonGroup.add(polarimetryYesButton);
		polarimetryButtonGroup.add(polarimetryNoButton);
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	JLabel filterLabel;
	JLabel totalOnSourceTimeLabel;
	JComboBox filterComboBox;
	JLabel filterOverride;
	NumberBoxWidget totalOnSourceTime;
	JLabel totalOnSourceTimeUnitsLabel;
	JLabel nodIntervalLabel;
	NumberBoxWidget nodInterval;
	JLabel nodIntervalUnitsLabel;
	JLabel focalPlaneMaskLabel;
	JLabel posAngleLabel;
	JComboBox focalPlaneMaskComboBox;
	NumberBoxWidget posAngle;
	JLabel posAngleUnitsLabel;
	JLabel disperserLabel;
	JLabel centralWavelengthLabel;
	JComboBox disperserComboBox;
	NumberBoxWidget centralWavelength;
	JLabel chopAngleLabel;
	JLabel chopThrowLabel;
	NumberBoxWidget chopAngle;
	NumberBoxWidget chopThrow;
	JLabel chopAngleUnitsLabel;
	JLabel centralWavelengthUnitsLabel;
	JLabel chopThrowUnitsLabel;
	JLabel scienceFOV;
	JLabel exposureTimeLabel;
	TextBoxWidget exposureTime;
	JLabel exposureTimeUnitsLabel;
	JLabel autoConfigureLabel;
	JRadioButton autoConfigureYesButton;
	JRadioButton autoConfigureNoButton;
	JLabel nodOrientationLabel;
	JComboBox nodOrientationComboBox;
	JLabel maskOverride;
	JRadioButton polarimetryYesButton;
	JRadioButton polarimetryNoButton;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
