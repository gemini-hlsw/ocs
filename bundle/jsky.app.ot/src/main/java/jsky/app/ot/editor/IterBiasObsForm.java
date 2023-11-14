package jsky.app.ot.editor;

import javax.swing.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import jsky.util.gui.*;
/*
 * Created by JFormDesigner on Mon May 30 07:53:05 CEST 2005
 */



/**
 * @author Allan Brighton
 */
public class IterBiasObsForm extends JPanel {
	public IterBiasObsForm() {
		initComponents();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner non-commercial license
		label4 = new JLabel();
		obsClass = new DropDownListBoxWidget();
		label2 = new JLabel();
		repeatSpinner = new JSpinner();
		label3 = new JLabel();
		label6 = new JLabel();
		coadds = new NumberBoxWidget();
		label8 = new JLabel();
		CellConstraints cc = new CellConstraints();

		//======== this ========
		setLayout(new FormLayout(
			new ColumnSpec[] {
				new ColumnSpec(ColumnSpec.FILL, Sizes.DLUX11, FormSpec.DEFAULT_GROW),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("max(default;40dlu)"),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				FormFactory.UNRELATED_GAP_COLSPEC
			},
			new RowSpec[] {
				FormFactory.PARAGRAPH_GAP_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				new RowSpec(RowSpec.TOP, Sizes.DLUY3, FormSpec.DEFAULT_GROW),
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
			}));

		//---- label4 ----
		label4.setText("Class");
		add(label4, cc.xy(9, 3));
		add(obsClass, cc.xywh(11, 3, 1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));

		//---- label2 ----
		label2.setText("Observe");
		label2.setLabelFor(repeatSpinner);
		add(label2, cc.xy(3, 5));
		add(repeatSpinner, cc.xy(5, 5));

		//---- label3 ----
		label3.setText("X");
		add(label3, cc.xy(7, 5));

		//---- label6 ----
		label6.setText("Coadds/Sub-exposures");
		add(label6, cc.xy(3, 7));
		add(coadds, cc.xy(5, 7));

		//---- label8 ----
		label8.setText("(exp / obs)");
		add(label8, cc.xy(7, 7));
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner non-commercial license
	private JLabel label4;
	DropDownListBoxWidget obsClass;
	private JLabel label2;
	JSpinner repeatSpinner;
	private JLabel label3;
	private JLabel label6;
	NumberBoxWidget coadds;
	private JLabel label8;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
