package jsky.app.ot.gemini.editor;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
/*
 * Created by JFormDesigner on Wed May 11 15:02:33 CEST 2005
 */


/**
 * @author Allan Brighton
 */
public class IterSmartGcalObsForm extends JPanel {
	public IterSmartGcalObsForm() {
		initComponents();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner non-commercial license
        label0 = new JLabel();
		label1 = new JLabel();
        panel0 = new JPanel();
		panel1 = new JPanel();
        isSmart = new JCheckBox();
		lamp1 = new JRadioButton();
		lamp2 = new JRadioButton();
		lamp3 = new JRadioButton();
		label2 = new JLabel();
		panel2 = new JPanel();
		arc1 = new JCheckBox();
		arc2 = new JCheckBox();
		arc3 = new JCheckBox();
		arc4 = new JCheckBox();
		label3 = new JLabel();
		shutter = new DropDownListBoxWidget();
		label4 = new JLabel();
		filter = new JComboBox();
		label5 = new JLabel();
		diffuser = new DropDownListBoxWidget();
		label6 = new JLabel();
		repeatSpinner = new JSpinner();
		label7 = new JLabel();
		exposureTime = new TextBoxWidget();
		label72 = new JLabel();
		label8 = new JLabel();
		coadds = new TextBoxWidget();
		label82 = new JLabel();
		label42 = new JLabel();
		obsClass = new DropDownListBoxWidget();
		CellConstraints cc = new CellConstraints();

		//======== this ========
		setLayout(new FormLayout(
			new ColumnSpec[] {
				new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("max(pref;50dlu)"),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				new ColumnSpec(ColumnSpec.FILL, Sizes.PREFERRED, FormSpec.DEFAULT_GROW),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				FormFactory.PREF_COLSPEC,
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				new ColumnSpec(ColumnSpec.LEFT, Sizes.DLUX7, FormSpec.NO_GROW)
			},
			new RowSpec[] {
				FormFactory.PARAGRAPH_GAP_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				new RowSpec(RowSpec.CENTER, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				new RowSpec(RowSpec.CENTER, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
			}));
		((FormLayout)getLayout()).setRowGroups(new int[][] {{5, 25}});


		//---- label42 ----
		label42.setText("Class");
		add(label42, cc.xywh(7, 3, 1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));
		add(obsClass, cc.xywh(9, 3, 1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner non-commercial license
    JLabel label0;
    JPanel panel0;
    JCheckBox isSmart;
	JLabel label1;
	JPanel panel1;
	JRadioButton lamp1;
	JRadioButton lamp2;
	JRadioButton lamp3;
	JLabel label2;
	JPanel panel2;
	JCheckBox arc1;
	JCheckBox arc2;
	JCheckBox arc3;
	JCheckBox arc4;
	private JLabel label3;
	DropDownListBoxWidget shutter;
	private JLabel label4;
	JComboBox filter;
	private JLabel label5;
	DropDownListBoxWidget diffuser;
	private JLabel label6;
	JSpinner repeatSpinner;
	private JLabel label7;
	TextBoxWidget exposureTime;
	private JLabel label72;
	private JLabel label8;
	TextBoxWidget coadds;
	private JLabel label82;
	JLabel label42;
	DropDownListBoxWidget obsClass;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
