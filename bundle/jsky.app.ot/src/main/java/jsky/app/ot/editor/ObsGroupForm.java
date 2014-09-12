package jsky.app.ot.editor;

import javax.swing.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import jsky.util.gui.*;
/*
 * Created by JFormDesigner on Mon Jun 06 23:48:15 CEST 2005
 */



/**
 * @author User #1
 */
public class ObsGroupForm extends JPanel {
	public ObsGroupForm() {
		initComponents();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner non-commercial license
		DefaultComponentFactory compFactory = DefaultComponentFactory.getInstance();
		libraryIdLabel = new JLabel();
		libraryIdTextField = new JTextField();
		label1 = new JLabel();
		obsGroupName = new JTextField();
		label2 = new JLabel();
		groupType = new JComboBox();
		goodiesFormsSeparator1 = compFactory.createSeparator("Observing Time");
		panel1 = new JPanel();
		plannedLabel = new JLabel();
		usedLabel = new JLabel();
		totalExecTimeLabel = new JLabel();
		totalPiTimeLabel = new JLabel();
		programTimeLabel = new JLabel();
		partnerTimeLabel = new JLabel();
		totalExecTime = new JLabel();
		totalPiTime = new JLabel();
		programTime = new JLabel();
		partnerTime = new JLabel();
		CellConstraints cc = new CellConstraints();

		//======== this ========
		setLayout(new FormLayout(
			new ColumnSpec[] {
				new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, 0.19999999999999998),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				new ColumnSpec(Sizes.dluX(120)),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
			},
			new RowSpec[] {
				new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.PARAGRAPH_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.UNRELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				new RowSpec(RowSpec.FILL, Sizes.dluY(89), FormSpec.DEFAULT_GROW)
			}));

		//---- libraryIdLabel ----
		libraryIdLabel.setText("Library ID");
		add(libraryIdLabel, cc.xy(3, 3));
		add(libraryIdTextField, cc.xywh(5, 3, 3, 1));

		//---- label1 ----
		label1.setText("Group Name");
		add(label1, cc.xy(3, 5));
		add(obsGroupName, cc.xywh(5, 5, 3, 1));

		//---- label2 ----
		label2.setText("Group Type");
		add(label2, cc.xy(3, 7));
		add(groupType, cc.xywh(5, 7, 3, 1));
		add(goodiesFormsSeparator1, cc.xywh(3, 9, 5, 1));

		//======== panel1 ========
		{
			panel1.setLayout(new FormLayout(
				new ColumnSpec[] {
					new ColumnSpec(ColumnSpec.CENTER, Sizes.dluX(36), FormSpec.NO_GROW),
					FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
					new ColumnSpec(ColumnSpec.CENTER, Sizes.dluX(36), FormSpec.NO_GROW),
					FormFactory.UNRELATED_GAP_COLSPEC,
					new ColumnSpec(ColumnSpec.CENTER, Sizes.dluX(36), FormSpec.NO_GROW),
					FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
					new ColumnSpec(ColumnSpec.CENTER, Sizes.dluX(36), FormSpec.NO_GROW)
				},
				new RowSpec[] {
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.NARROW_LINE_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.LINE_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC
				}));

			//---- plannedLabel ----
			plannedLabel.setText("Planned");
			panel1.add(plannedLabel, cc.xywh(1, 1, 3, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

			//---- usedLabel ----
			usedLabel.setText("Used");
			panel1.add(usedLabel, cc.xywh(5, 1, 3, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

			//---- totalExecTimeLabel ----
			totalExecTimeLabel.setText("Exec");
			panel1.add(totalExecTimeLabel, cc.xy(1, 3));

			//---- totalPiTimeLabel ----
			totalPiTimeLabel.setText("PI");
			panel1.add(totalPiTimeLabel, cc.xy(3, 3));

			//---- programTimeLabel ----
			programTimeLabel.setText("Program");
			panel1.add(programTimeLabel, cc.xy(5, 3));

			//---- partnerTimeLabel ----
			partnerTimeLabel.setText("Partner");
			panel1.add(partnerTimeLabel, cc.xy(7, 3));

			//---- totalExecTime ----
			totalExecTime.setText("00:00:00");
			totalExecTime.setToolTipText("The total time planned for the execution of this group of observations, in hours:min:sec");
			panel1.add(totalExecTime, cc.xy(1, 5));

			//---- totalPiTime ----
			totalPiTime.setText("00:00:00");
			totalPiTime.setToolTipText("The total time planned to be charged to the PI for this group of observation, in hours:minutes:seconds");
			panel1.add(totalPiTime, cc.xy(3, 5));

			//---- programTime ----
			programTime.setText("00:00:00");
			programTime.setToolTipText("The total time charged to the science program for the observations in this group, in hours:min:sec");
			panel1.add(programTime, cc.xy(5, 5));

			//---- partnerTime ----
			partnerTime.setText("00:00:00");
			partnerTime.setToolTipText("The total time charged to the Gemini partner country/entity for the observations in this group, in hours:min:sec");
			panel1.add(partnerTime, cc.xy(7, 5));
		}
		add(panel1, cc.xywh(3, 11, 5, 1));
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner non-commercial license
	JLabel libraryIdLabel;
	JTextField libraryIdTextField;
	private JLabel label1;
	JTextField obsGroupName;
	private JLabel label2;
	JComboBox groupType;
	private JComponent goodiesFormsSeparator1;
	private JPanel panel1;
	JLabel plannedLabel;
	JLabel usedLabel;
	JLabel totalExecTimeLabel;
	JLabel totalPiTimeLabel;
	JLabel programTimeLabel;
	JLabel partnerTimeLabel;
	JLabel totalExecTime;
	JLabel totalPiTime;
	JLabel programTime;
	JLabel partnerTime;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
