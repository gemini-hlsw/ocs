package jsky.app.ot.gemini.nici;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import jsky.util.gui.NumberBoxWidget;

import javax.swing.*;
import java.awt.*;

public class NICIFormEng extends JPanel {
	public NICIFormEng() {
		initComponents();
	}

	private void initComponents() {
		JLabel label1 = new JLabel();
		JLabel label3 = new JLabel();
		focsCB = new JComboBox();
		pupilImagerCB = new JComboBox();
		JLabel label4 = new JLabel();
		JLabel label6 = new JLabel();
		spiderMaskCB = new JComboBox();
		smrAngleTF = new NumberBoxWidget();
		JLabel label7 = new JLabel();
		JLabel label2 = new JLabel();
		dhsModeCB = new JComboBox();
		CellConstraints cc = new CellConstraints();

		//======== this ========
		setBorder(Borders.DIALOG_BORDER);
		setLayout(new FormLayout(
			"pref:grow, pref, left:max(pref;5px):grow, pref, left:max(pref;5px):grow, left:max(pref;5px), pref:grow",
			"pref, top:max(pref;5px), pref, top:max(pref;10px), pref, top:max(pref;5px), pref, top:max(pref;10px), default, pref, max(pref;5px)"));

		//---- label1 ----
		label1.setText("FOCS");
		add(label1, cc.xy(2, 1));

		//---- label3 ----
		label3.setText("Pupil Imager");
		add(label3, cc.xy(4, 1));
		add(focsCB, cc.xy(2, 3));
		add(pupilImagerCB, cc.xy(4, 3));

		//---- label4 ----
		label4.setText("Spider Mask");
		add(label4, cc.xy(2, 5));

		//---- label6 ----
		label6.setText("SMR Angle");
		add(label6, cc.xy(4, 5));
		add(spiderMaskCB, cc.xy(2, 7));
		add(smrAngleTF, cc.xy(4, 7));

		//---- label7 ----
		label7.setText("degrees");
		add(label7, new CellConstraints(5, 7, 1, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT, new Insets( 0, 5, 0, 0)));

		//---- label2 ----
		label2.setText("DHS Mode");
		add(label2, cc.xy(2, 9));
		add(dhsModeCB, cc.xy(2, 10));
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	public JComboBox getFocsCB() {
		return focsCB;
	}

	public JComboBox getPupilImagerCB() {
		return pupilImagerCB;
	}

	public JComboBox getSpiderMaskCB() {
		return spiderMaskCB;
	}

	public NumberBoxWidget getSmrAngleTF() {
		return smrAngleTF;
	}

	public JComboBox getDhsModeCB() {
		return dhsModeCB;
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JComboBox focsCB;
	private JComboBox pupilImagerCB;
	private JComboBox spiderMaskCB;
	private NumberBoxWidget smrAngleTF;
	private JComboBox dhsModeCB;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
