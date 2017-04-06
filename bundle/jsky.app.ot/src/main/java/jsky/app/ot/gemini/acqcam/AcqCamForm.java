package jsky.app.ot.gemini.acqcam;

import java.awt.*;
import javax.swing.*;
import jsky.util.gui.*;

public class AcqCamForm extends JPanel {
	public AcqCamForm() {
		initComponents();
	}

	private void initComponents() {
		cassRotatorLabel = new JLabel();
		cassRotatorFollowingButton = new JRadioButton();
		cassRotatorFixedButton = new JRadioButton();
		expTimeLabel = new JLabel();
		exposureTime = new NumberBoxWidget();
		disperserLabel = new JLabel();
		ndFilter = new jsky.util.gui.DropDownListBoxWidget();
		posAngleLabel = new JLabel();
		posAngle = new NumberBoxWidget();
		expTimeUnits = new JLabel();
		posAngleUnits = new JLabel();
		colorFilterLabel = new JLabel();
		colorFilter = new DropDownListBoxWidget();
		lensLabel = new JLabel();
		lens = new DropDownListBoxWidget();
		jTabbedPane1 = new JTabbedPane();
		binningPanel = new JPanel();
		binningOnButton = new JRadioButton();
		binningOffButton = new JRadioButton();
		component1 = new JLabel();
		roiPanel = new JPanel();
		windowingLabel = new JLabel();
		xLabel = new JLabel();
		yLabel = new JLabel();
		widthLabel = new JLabel();
		heightLabel = new JLabel();
		windowingPanel = new JPanel();
		windowingOnButton = new JRadioButton();
		windowingOffButton = new JRadioButton();
		x = new NumberBoxWidget();
		y = new NumberBoxWidget();
		width = new NumberBoxWidget();
		height = new NumberBoxWidget();
		xComment = new JLabel();
		fill = new JLabel();
		widthComment = new JLabel();

		//======== this ========
		setLayout(new GridBagLayout());

		//---- cassRotatorLabel ----
		cassRotatorLabel.setText("Cass Rotator");
		add(cassRotatorLabel, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));

		//---- cassRotatorFollowingButton ----
		cassRotatorFollowingButton.setText("Following");
		add(cassRotatorFollowingButton, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(0, 11, 0, 0), 0, 0));

		//---- cassRotatorFixedButton ----
		cassRotatorFixedButton.setText("Fixed");
		add(cassRotatorFixedButton, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(0, 0, 0, 0), 0, 0));

		//---- expTimeLabel ----
		expTimeLabel.setToolTipText("Exposure time in seconds");
		expTimeLabel.setLabelFor(null);
		expTimeLabel.setText("Exposure Time");
		add(expTimeLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));

		//---- exposureTime ----
		exposureTime.setToolTipText("Enter the exposure time in seconds");
		add(exposureTime, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(0, 11, 0, 0), 0, 0));

		//---- disperserLabel ----
		disperserLabel.setLabelFor(null);
		disperserLabel.setText("Neutral Density Filter");
		add(disperserLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));

		//---- ndFilter ----
		ndFilter.setToolTipText("Select the Neutral Density Filter");
		add(ndFilter, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(0, 11, 0, 0), 0, 0));

		//---- posAngleLabel ----
		posAngleLabel.setToolTipText("Position angle in degrees E of N");
		posAngleLabel.setLabelFor(null);
		posAngleLabel.setText("Position Angle");
		add(posAngleLabel, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));

		//---- posAngle ----
		posAngle.setToolTipText("Enter the position angle in degrees E of N");
		add(posAngle, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(0, 11, 0, 0), 0, 0));

		//---- expTimeUnits ----
		expTimeUnits.setText("sec");
		add(expTimeUnits, new GridBagConstraints(2, 1, 2, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(0, 3, 0, 11), 0, 0));

		//---- posAngleUnits ----
		posAngleUnits.setText("deg E of N");
		add(posAngleUnits, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(0, 3, 0, 0), 0, 0));

		//---- colorFilterLabel ----
		colorFilterLabel.setToolTipText("");
		colorFilterLabel.setLabelFor(null);
		colorFilterLabel.setText("Color Filter");
		add(colorFilterLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));

		//---- colorFilter ----
		colorFilter.setToolTipText("Select the Color Filter");
		add(colorFilter, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(0, 11, 0, 0), 0, 0));

		//---- lensLabel ----
		lensLabel.setText("Lens");
		add(lensLabel, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));
		add(lens, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(0, 11, 0, 0), 0, 0));

		//======== jTabbedPane1 ========
		{
			
			//======== binningPanel ========
			{
				binningPanel.setLayout(new GridBagLayout());
				
				//---- binningOnButton ----
				binningOnButton.setToolTipText("Turn on 2x2 Binning");
				binningOnButton.setText("2x2 Binning");
				binningPanel.add(binningOnButton, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(11, 11, 0, 0), 0, 0));
				
				//---- binningOffButton ----
				binningOffButton.setToolTipText("Turn off Binning");
				binningOffButton.setSelected(true);
				binningOffButton.setText("No  Binning (default)");
				binningPanel.add(binningOffButton, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(11, 11, 0, 0), 0, 0));
				binningPanel.add(component1, new GridBagConstraints(0, 3, 1, 1, 0.0, 1.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 0, 0), 0, 0));
			}
			jTabbedPane1.addTab("Binning", binningPanel);
			
			
			//======== roiPanel ========
			{
				roiPanel.setToolTipText("Regions of Interest");
				roiPanel.setLayout(new GridBagLayout());
				
				//---- windowingLabel ----
				windowingLabel.setLabelFor(null);
				windowingLabel.setText("Windowing");
				roiPanel.add(windowingLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(11, 11, 0, 0), 0, 0));
				
				//---- xLabel ----
				xLabel.setLabelFor(null);
				xLabel.setText("X Start");
				roiPanel.add(xLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 1.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 33, 0, 0), 0, 0));
				
				//---- yLabel ----
				yLabel.setLabelFor(null);
				yLabel.setText("Y Start");
				roiPanel.add(yLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 33, 0, 0), 0, 0));
				
				//---- widthLabel ----
				widthLabel.setToolTipText("");
				widthLabel.setLabelFor(null);
				widthLabel.setText("X Size");
				roiPanel.add(widthLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 1.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 33, 0, 0), 0, 0));
				
				//---- heightLabel ----
				heightLabel.setLabelFor(null);
				heightLabel.setText("Y Size");
				roiPanel.add(heightLabel, new GridBagConstraints(0, 4, 1, 1, 0.0, 1.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 33, 0, 0), 0, 0));
				
				//======== windowingPanel ========
				{
					windowingPanel.setLayout(new FlowLayout());
					
					//---- windowingOnButton ----
					windowingOnButton.setToolTipText("Turn on Windowing");
					windowingOnButton.setText("On");
					windowingPanel.add(windowingOnButton);
					
					//---- windowingOffButton ----
					windowingOffButton.setToolTipText("Turn off Windowing");
					windowingOffButton.setSelected(true);
					windowingOffButton.setText("Off");
					windowingPanel.add(windowingOffButton);
				}
				roiPanel.add(windowingPanel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(11, 11, 0, 0), 0, 0));
				
				//---- x ----
				x.setToolTipText(" X coordinate of the origin of the window in detector pixels");
				roiPanel.add(x, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(6, 11, 0, 0), 0, 0));
				
				//---- y ----
				y.setToolTipText(" Y coordinate of the origin of the window in detector pixels");
				roiPanel.add(y, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(6, 11, 0, 0), 0, 0));
				
				//---- width ----
				width.setToolTipText("Width of the window in detector pixels");
				roiPanel.add(width, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(6, 11, 0, 0), 0, 0));
				
				//---- height ----
				height.setToolTipText("Height of the window in detector pixels");
				roiPanel.add(height, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(6, 11, 0, 0), 0, 0));
				
				//---- xComment ----
				xComment.setToolTipText("");
				xComment.setText("Origin of window in pixels");
				roiPanel.add(xComment, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 0), 0, 0));
				roiPanel.add(fill, new GridBagConstraints(2, 5, 1, 1, 1.0, 1.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 0, 0), 0, 0));
				
				//---- widthComment ----
				widthComment.setText("Size of window in pixels");
				roiPanel.add(widthComment, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 0), 0, 0));
			}
			jTabbedPane1.addTab("Regions of Interest", roiPanel);
			
		}
		add(jTabbedPane1, new GridBagConstraints(0, 6, 4, 1, 1.0, 1.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(6, 6, 6, 6), 0, 0));

		//---- cassRotatorButtonGroup ----
		ButtonGroup cassRotatorButtonGroup = new ButtonGroup();
		cassRotatorButtonGroup.add(cassRotatorFollowingButton);
		cassRotatorButtonGroup.add(cassRotatorFixedButton);

		//---- binningButtonGroup ----
		ButtonGroup binningButtonGroup = new ButtonGroup();
		binningButtonGroup.add(binningOnButton);
		binningButtonGroup.add(binningOffButton);

		//---- windowingButtonGroup ----
		ButtonGroup windowingButtonGroup = new ButtonGroup();
		windowingButtonGroup.add(windowingOnButton);
		windowingButtonGroup.add(windowingOffButton);
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner non-commercial license
	private JLabel cassRotatorLabel;
	JRadioButton cassRotatorFollowingButton;
	JRadioButton cassRotatorFixedButton;
	private JLabel expTimeLabel;
	NumberBoxWidget exposureTime;
	private JLabel disperserLabel;
	DropDownListBoxWidget ndFilter;
	private JLabel posAngleLabel;
	NumberBoxWidget posAngle;
	private JLabel expTimeUnits;
	private JLabel posAngleUnits;
	private JLabel colorFilterLabel;
	DropDownListBoxWidget colorFilter;
	private JLabel lensLabel;
	DropDownListBoxWidget lens;
	JTabbedPane jTabbedPane1;
	private JPanel binningPanel;
	JRadioButton binningOnButton;
	JRadioButton binningOffButton;
	private JLabel component1;
	private JPanel roiPanel;
	private JLabel windowingLabel;
	JLabel xLabel;
	JLabel yLabel;
	JLabel widthLabel;
	JLabel heightLabel;
	private JPanel windowingPanel;
	JRadioButton windowingOnButton;
	JRadioButton windowingOffButton;
	NumberBoxWidget x;
	NumberBoxWidget y;
	NumberBoxWidget width;
	NumberBoxWidget height;
	JLabel xComment;
	private JLabel fill;
	JLabel widthComment;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
