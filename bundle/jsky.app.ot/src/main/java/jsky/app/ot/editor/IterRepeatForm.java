package jsky.app.ot.editor;

import javax.swing.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import jsky.util.gui.*;
/*
 * Created by JFormDesigner on Sun May 08 21:36:41 CEST 2005
 */



/**
 * @author Allan Brighton
 */
public class IterRepeatForm extends JPanel {
	public IterRepeatForm() {
		initComponents();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner non-commercial license
		label1 = new JLabel();
		title = new TextBoxWidget();
		label2 = new JLabel();
		repeatSpinner = new JSpinner();
		label3 = new JLabel();
		CellConstraints cc = new CellConstraints();

		//======== this ========
		setLayout(new FormLayout(
			new ColumnSpec[] {
				new ColumnSpec(Sizes.DLUX11),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("max(default;40dlu)"),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				new ColumnSpec(Sizes.dluX(21))
			},
			new RowSpec[] {
				new RowSpec(RowSpec.TOP, Sizes.DLUY9, FormSpec.DEFAULT_GROW),
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
			}));

		//---- label1 ----
		label1.setText("Title");
		label1.setLabelFor(title);
		add(label1, cc.xy(3, 3));
		add(title, cc.xywh(5, 3, 3, 1));

		//---- label2 ----
		label2.setText("Repeat");
		label2.setLabelFor(repeatSpinner);
		add(label2, cc.xy(3, 5));
		add(repeatSpinner, cc.xy(5, 5));

		//---- label3 ----
		label3.setText("X");
		add(label3, cc.xy(7, 5));
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner non-commercial license
	private JLabel label1;
	TextBoxWidget title;
	private JLabel label2;
	JSpinner repeatSpinner;
	private JLabel label3;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
