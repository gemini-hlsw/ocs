package jsky.app.ot.gemini.editor.horizons;

import com.jgoodies.forms.factories.DefaultComponentFactory;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
/*
 * Created by JFormDesigner on Sun Nov 26 17:40:12 CLST 2006
 */



/**
 * @author User #6
 */
public class HorizonsMultipleChoiceDialog extends JPanel {
	public HorizonsMultipleChoiceDialog() {
		initComponents();
	}


	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner non-commercial license
		DefaultComponentFactory compFactory = DefaultComponentFactory.getInstance();
		JPanel dialogPane = new JPanel();
		JPanel contentPanel = new JPanel();
		JComponent separator1 = compFactory.createSeparator("Multiple Answers Found. Please select one object");
		JScrollPane scrollPane1 = new JScrollPane();
		resultsTable = new JTable();
		JPanel buttonBar = new JPanel();
		okButton = new JButton();
		cancelButton = new JButton();

		//======== this ========
		setLayout(new BorderLayout());

		//======== dialogPane ========
		{
			dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
			dialogPane.setLayout(new BorderLayout());
			
			//======== contentPanel ========
			{
				contentPanel.setLayout(new GridBagLayout());
				((GridBagLayout)contentPanel.getLayout()).columnWidths = new int[] {590, 0};
				((GridBagLayout)contentPanel.getLayout()).rowHeights = new int[] {0, 320, 0};
				((GridBagLayout)contentPanel.getLayout()).columnWeights = new double[] {1.0, 1.0E-4};
				((GridBagLayout)contentPanel.getLayout()).rowWeights = new double[] {0.0, 1.0, 1.0E-4};
				contentPanel.add(separator1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 0), 0, 0));
				
				//======== scrollPane1 ========
				{
					scrollPane1.setViewportView(resultsTable);
				}
				contentPanel.add(scrollPane1, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 0, 0), 0, 0));
			}
			dialogPane.add(contentPanel, BorderLayout.CENTER);
			
			//======== buttonBar ========
			{
				buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
				buttonBar.setLayout(new GridBagLayout());
				((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
				((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};
				
				//---- okButton ----
				okButton.setText("OK");
				buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 0, 5), 0, 0));
				
				//---- cancelButton ----
				cancelButton.setText("Cancel");
				buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 0, 0), 0, 0));
			}
			dialogPane.add(buttonBar, BorderLayout.SOUTH);
		}
		add(dialogPane, BorderLayout.CENTER);
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	public JTable getResultsTable() {
		return resultsTable;
	}

	public JButton getOkButton() {
		return okButton;
	}

	public JButton getCancelButton() {
		return cancelButton;
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner non-commercial license
	private JTable resultsTable;
	private JButton okButton;
	private JButton cancelButton;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
