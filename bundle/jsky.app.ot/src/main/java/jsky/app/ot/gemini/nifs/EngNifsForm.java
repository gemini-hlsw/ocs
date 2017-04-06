package jsky.app.ot.gemini.nifs;

import javax.swing.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import jsky.util.gui.*;

public class EngNifsForm extends JPanel {
	public EngNifsForm() {
		initComponents();
	}

	private void initComponents() {
		label1 = new JLabel();
		engReadMode = new DropDownListBoxWidget();
		label3 = new JLabel();
		numberOfSamples = new NumberBoxWidget();
		label4 = new JLabel();
		period = new NumberBoxWidget();
		label6 = new JLabel();
		numberOfPeriods = new NumberBoxWidget();
		label5 = new JLabel();
		numberOfResets = new NumberBoxWidget();
		CellConstraints cc = new CellConstraints();

		//======== this ========
		setLayout(new FormLayout(
			new ColumnSpec[] {
				new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("max(min;80dlu)")
			},
			new RowSpec[] {
				new RowSpec(Sizes.DLUY11),
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC
			}));

		//---- label1 ----
		label1.setText("Engineering Read Mode");
		add(label1, cc.xy(1, 3));
		add(engReadMode, cc.xy(3, 3));

		//---- label3 ----
		label3.setText("Number of Samples");
		add(label3, cc.xy(1, 5));
		add(numberOfSamples, cc.xy(3, 5));

		//---- label4 ----
		label4.setText("Period (seconds)");
		add(label4, cc.xy(1, 7));
		add(period, cc.xy(3, 7));

		//---- label6 ----
		label6.setText("Number of Periods");
		add(label6, cc.xy(1, 9));
		add(numberOfPeriods, cc.xy(3, 9));

		//---- label5 ----
		label5.setText("Number of Resets");
		add(label5, cc.xy(1, 11));
		add(numberOfResets, cc.xy(3, 11));
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner non-commercial license
	private JLabel label1;
	DropDownListBoxWidget<String> engReadMode;
	private JLabel label3;
	NumberBoxWidget numberOfSamples;
	private JLabel label4;
	NumberBoxWidget period;
	private JLabel label6;
	NumberBoxWidget numberOfPeriods;
	private JLabel label5;
	NumberBoxWidget numberOfResets;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
