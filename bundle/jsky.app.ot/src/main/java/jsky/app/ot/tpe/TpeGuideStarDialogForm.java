package jsky.app.ot.tpe;

import javax.swing.*;
import java.awt.*;
/*
 * Created by JFormDesigner on Mon Oct 31 14:01:32 CET 2005
 */



/**
 * @author User #1
 */
public class TpeGuideStarDialogForm extends JPanel {
	public TpeGuideStarDialogForm() {
		initComponents();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner non-commercial license
		catalogComboBox = new JComboBox();
		JLabel typeLabel = new JLabel();
		typeComboBox = new JComboBox();
		JLabel catalogLabel = new JLabel();
		JLabel instLabel = new JLabel();
		instComboBox = new JComboBox();
		catalogWarning = new JLabel();
		JPanel buttonPanel = new JPanel();
		okButton = new JButton();
		cancelButton = new JButton();
		guideStarWarning = new JLabel();

		//======== this ========
		setLayout(new GridBagLayout());
		setMinimumSize(new Dimension(460,170));
		setPreferredSize(new Dimension(460,170));
		add(catalogComboBox, new GridBagConstraints(1, 2, 2, 1, 1.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(11, 11, 0, 11), 0, 0));

		//---- typeLabel ----
		typeLabel.setLabelFor(null);
		typeLabel.setText("Guide Star Type:");
		add(typeLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(0, 11, 0, 0), 0, 0));
		add(typeComboBox, new GridBagConstraints(1, 1, 2, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(11, 11, 0, 11), 0, 0));

		//---- catalogLabel ----
		catalogLabel.setLabelFor(null);
		catalogLabel.setText("Search in Catalog:");
		add(catalogLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));

		//---- instLabel ----
		instLabel.setRequestFocusEnabled(true);
		instLabel.setVerifyInputWhenFocusTarget(true);
		instLabel.setText("Instrument:");
		add(instLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));
		add(instComboBox, new GridBagConstraints(1, 0, 2, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(11, 11, 0, 11), 0, 0));

		//---- catalogWarning ----
		catalogWarning.setText("Warning!");
		catalogWarning.setForeground(Color.red);
		add(catalogWarning, new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(6, 11, 6, 0), 0, 0));

		//======== buttonPanel ========
		{
			buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 1));
			
			//---- okButton ----
			okButton.setText("    OK    ");
			buttonPanel.add(okButton);
			
			//---- cancelButton ----
			cancelButton.setText("Cancel");
			buttonPanel.add(cancelButton);
		}
		add(buttonPanel, new GridBagConstraints(2, 3, 1, 2, 0.0, 0.0,
			GridBagConstraints.EAST, GridBagConstraints.NONE,
			new Insets(0, 0, 0, 0), 0, 0));

		//---- guideStarWarning ----
		guideStarWarning.setText("Warning!");
		guideStarWarning.setForeground(Color.red);
		add(guideStarWarning, new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0,
			GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
			new Insets(6, 11, 6, 0), 0, 0));
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner non-commercial license
	JComboBox catalogComboBox;
	JComboBox typeComboBox;
	JComboBox instComboBox;
	JLabel catalogWarning;
	JButton okButton;
	JButton cancelButton;
	JLabel guideStarWarning;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
