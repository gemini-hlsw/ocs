package jsky.app.ot.tpe;

import java.awt.*;
import javax.swing.*;
/*
 * Created by JFormDesigner on Tue Jun 06 14:44:16 CLT 2006
 */



/**
 * $Id: TpeSkyDialogForm.java 7126 2006-06-06 22:12:42Z anunez $
 */
public class TpeSkyDialogForm extends JPanel {
	public TpeSkyDialogForm() {
		initComponents();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner non-commercial license
		catalogComboBox = new JComboBox();
		JLabel catalogLabel = new JLabel();

		//======== this ========
		setLayout(new GridBagLayout());
		add(catalogComboBox, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(11, 11, 0, 11), 0, 0));

		//---- catalogLabel ----
		catalogLabel.setLabelFor(null);
		catalogLabel.setText("Search in Catalog:");
		add(catalogLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(11, 11, 0, 0), 0, 0));
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner non-commercial license
	JComboBox catalogComboBox;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
