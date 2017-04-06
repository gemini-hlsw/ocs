package jsky.app.ot.gemini.nici;

import java.awt.*;
import javax.swing.*;

public class NICIGui extends JPanel {
	public NICIGui(JPanel pan1, JPanel pan2) {
        panel1 = pan1;
        panel2 = pan2;
        initComponents();
	}

	private void initComponents() {
		JTabbedPane tabbedPane1 = new JTabbedPane();
		//======== this ========
		setLayout(new GridBagLayout());
		((GridBagLayout)getLayout()).columnWidths = new int[] {0, 0};
		((GridBagLayout)getLayout()).rowHeights = new int[] {0, 0};
		((GridBagLayout)getLayout()).columnWeights = new double[] {1.0, 1.0E-4};
		((GridBagLayout)getLayout()).rowWeights = new double[] {1.0, 1.0E-4};

		//======== tabbedPane1 ========
		{
			tabbedPane1.addTab("Main", panel1);
			tabbedPane1.addTab("Engineering", panel2);
		}
		add(tabbedPane1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(0, 0, 0, 0), 0, 0));
	}

    private JPanel panel1;
	private JPanel panel2;
}
