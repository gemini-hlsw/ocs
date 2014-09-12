package jsky.app.ot.session;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
/*
 * Created by JFormDesigner on Mon Oct 31 14:12:51 CET 2005
 */



/**
 * @author User #1
 */
public class SessionQueuePanelForm extends JPanel {
	public SessionQueuePanelForm() {
		initComponents();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner non-commercial license
		mainPanel = new JPanel();
		jScrollPane1 = new JScrollPane();
		sessionTable = new JTable();
		buttonPanel = new JPanel();
		removeButton = new JButton();
		updateButton = new JButton();
		closeButton = new JButton();

		//======== this ========
		setLayout(new GridBagLayout());

		//======== mainPanel ========
		{
			mainPanel.setLayout(new GridBagLayout());

			//======== jScrollPane1 ========
			{

				//---- sessionTable ----
				sessionTable.setPreferredScrollableViewportSize(new Dimension(600, 200));
				jScrollPane1.setViewportView(sessionTable);
			}
			mainPanel.add(jScrollPane1, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(11, 11, 0, 11), 0, 0));
		}
		add(mainPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(0, 0, 0, 0), 0, 0));

		//======== buttonPanel ========
		{
			buttonPanel.setLayout(new GridBagLayout());

			//---- removeButton ----
			removeButton.setEnabled(false);
			removeButton.setToolTipText("Remove the selected observation from the session queue");
			removeButton.setText("Remove");
			removeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					removeButton_actionPerformed(e);
				}
			});
			buttonPanel.add(removeButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(11, 11, 11, 0), 0, 0));

			//---- updateButton ----
			updateButton.setToolTipText("Update with the current list of observations in the session queue");
			updateButton.setText("Update");
			updateButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					updateButton_actionPerformed(e);
				}
			});
			buttonPanel.add(updateButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(11, 11, 11, 0), 0, 0));

			//---- closeButton ----
			closeButton.setToolTipText("Close the window");
			closeButton.setText("Close");
			closeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					closeButton_actionPerformed(e);
				}
			});
			buttonPanel.add(closeButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(11, 11, 11, 11), 0, 0));
		}
		add(buttonPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
			GridBagConstraints.EAST, GridBagConstraints.NONE,
			new Insets(0, 0, 0, 0), 0, 0));
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	void removeButton_actionPerformed(ActionEvent e) {
		// TODO add your code here
	}
	void updateButton_actionPerformed(ActionEvent e) {
		// TODO add your code here
	}
	void closeButton_actionPerformed(ActionEvent e) {
		// TODO add your code here
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner non-commercial license
	private JPanel mainPanel;
	private JScrollPane jScrollPane1;
	JTable sessionTable;
	private JPanel buttonPanel;
	JButton removeButton;
	JButton updateButton;
	JButton closeButton;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
