package jsky.app.ot.editor;
import java.awt.*;
import javax.swing.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import jsky.util.gui.*;
/*
 * Created by JFormDesigner on Mon May 09 17:38:42 CEST 2005
 */



/**
 * @author Allan Brighton
 */
public class MiniConfigIterForm extends JPanel {
	public MiniConfigIterForm() {
		initComponents();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner non-commercial license
		titlePanel = new JPanel();
		label1 = new JLabel();
		title = new TextBoxWidget();
		choicePanel = new JPanel();
		panel1 = new JPanel();
		availableItemsLabel = new JLabel();
		itemsScrollPane = new JScrollPane();
		availableItems = new ListBoxWidget();
		iterConfigLabel = new JLabel();
		tableInfo = new JLabel();
		panel2 = new JPanel();
		configScrollPane = new JScrollPane();
		iterStepsTable = new CellSelectTableWidget();
		top = new JButton();
		up = new JButton();
		down = new JButton();
		bottom = new JButton();
		buttonPanel = new JPanel();
		addStep = new JButton();
		deleteStep = new JButton();
		deleteItem = new JButton();
		textBoxGroup = new JPanel();
		textBoxTitle = new JLabel();
		textBox = new TextBoxWidget();
		numberBoxGroup = new JPanel();
		numberBoxTitle = new JLabel();
		numberBox = new NumberBoxWidget();
		comboBoxGroup = new JPanel();
		comboBoxTitle = new JLabel();
		comboBox = new DropDownListBoxWidget();
		listBoxGroup = new JPanel();
		listBoxTitle = new JLabel();
		choicesScrollPane = new JScrollPane();
		availableChoices = new ListBoxWidget();
		CellConstraints cc = new CellConstraints();

		//======== this ========
		setLayout(new FormLayout(
			new ColumnSpec[] {
				new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
				FormFactory.UNRELATED_GAP_COLSPEC,
				new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
			},
			new RowSpec[] {
				FormFactory.UNRELATED_GAP_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.UNRELATED_GAP_ROWSPEC,
				new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.NARROW_LINE_GAP_ROWSPEC,
				new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC
			}));
		((FormLayout)getLayout()).setColumnGroups(new int[][] {{1, 3}});
		((FormLayout)getLayout()).setRowGroups(new int[][] {{5, 9}});

		//======== titlePanel ========
		{
			titlePanel.setLayout(new FormLayout(
				new ColumnSpec[] {
					FormFactory.DEFAULT_COLSPEC,
					FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
					new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
				},
				RowSpec.decodeSpecs("default")));

			//---- label1 ----
			label1.setText("Title");
			titlePanel.add(label1, cc.xy(1, 1));
			titlePanel.add(title, cc.xy(3, 1));
		}
		add(titlePanel, cc.xywh(1, 3, 3, 1));

		//======== choicePanel ========
		{
			choicePanel.setLayout(new BorderLayout());
		}
		add(choicePanel, cc.xy(1, 5));

		//======== panel1 ========
		{
			panel1.setLayout(new FormLayout(
				ColumnSpec.decodeSpecs("default:grow"),
				new RowSpec[] {
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.NARROW_LINE_GAP_ROWSPEC,
					new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
				}));

			//---- availableItemsLabel ----
			availableItemsLabel.setText("Available Items");
			panel1.add(availableItemsLabel, cc.xy(1, 1));

			//======== itemsScrollPane ========
			{

				//---- availableItems ----
				availableItems.setBackground(Color.lightGray);
				availableItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				itemsScrollPane.setViewportView(availableItems);
			}
			panel1.add(itemsScrollPane, cc.xy(1, 3));
		}
		add(panel1, cc.xy(3, 5));

		//---- iterConfigLabel ----
		iterConfigLabel.setText("Iteration Configuration");
		add(iterConfigLabel, cc.xy(1, 7));

		//---- tableInfo ----
		tableInfo.setText("(0 Items, 0 Steps)");
		add(tableInfo, cc.xy(3, 7));

		//======== panel2 ========
		{
			panel2.setLayout(new FormLayout(
				new ColumnSpec[] {
					new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
					FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
					FormFactory.DEFAULT_COLSPEC
				},
				new RowSpec[] {
					new RowSpec(RowSpec.CENTER, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
					FormFactory.LINE_GAP_ROWSPEC,
					new RowSpec(RowSpec.CENTER, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
					FormFactory.LINE_GAP_ROWSPEC,
					new RowSpec(RowSpec.CENTER, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
					FormFactory.LINE_GAP_ROWSPEC,
					new RowSpec(RowSpec.CENTER, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
				}));

			//======== configScrollPane ========
			{

				//---- iterStepsTable ----
				iterStepsTable.setBackground(Color.lightGray);
				iterStepsTable.setShowHorizontalLines(false);
				iterStepsTable.setShowVerticalLines(true);
				iterStepsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
				configScrollPane.setViewportView(iterStepsTable);
			}
			panel2.add(configScrollPane, cc.xywh(1, 1, 1, 7));

			//---- top ----
			top.setMargin(new Insets(2, 2, 2, 2));
			panel2.add(top, cc.xy(3, 1));

			//---- up ----
			up.setMargin(new Insets(2, 2, 2, 2));
			panel2.add(up, cc.xy(3, 3));

			//---- down ----
			down.setMargin(new Insets(2, 2, 2, 2));
			panel2.add(down, cc.xy(3, 5));

			//---- bottom ----
			bottom.setMargin(new Insets(2, 2, 2, 2));
			panel2.add(bottom, cc.xy(3, 7));
		}
		add(panel2, cc.xywh(1, 9, 3, 1));

		//======== buttonPanel ========
		{
			buttonPanel.setLayout(new FormLayout(
				new ColumnSpec[] {
					new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
					FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
					FormFactory.DEFAULT_COLSPEC,
					FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
					FormFactory.DEFAULT_COLSPEC,
					FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
					FormFactory.DEFAULT_COLSPEC,
					FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
					FormFactory.UNRELATED_GAP_COLSPEC
				},
				RowSpec.decodeSpecs("default")));

			//---- addStep ----
			addStep.setText("Add Step");
			buttonPanel.add(addStep, cc.xy(3, 1));

			//---- deleteStep ----
			deleteStep.setText("Delete Step");
			buttonPanel.add(deleteStep, cc.xy(5, 1));

			//---- deleteItem ----
			deleteItem.setText("Delete Item");
			buttonPanel.add(deleteItem, cc.xy(7, 1));
		}
		add(buttonPanel, cc.xywh(1, 11, 3, 1));

		//======== textBoxGroup ========
		{
			textBoxGroup.setLayout(new FormLayout(
				ColumnSpec.decodeSpecs("80dlu:grow"),
				new RowSpec[] {
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.NARROW_LINE_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC
				}));

			//---- textBoxTitle ----
			textBoxTitle.setText("text");
			textBoxGroup.add(textBoxTitle, cc.xy(1, 1));
			textBoxGroup.add(textBox, cc.xy(1, 3));
		}

		//======== numberBoxGroup ========
		{
			numberBoxGroup.setLayout(new FormLayout(
				new ColumnSpec[] {
					ColumnSpec.decode("max(pref;60dlu):grow"),
					FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
					new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
				},
				new RowSpec[] {
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.NARROW_LINE_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC
				}));

			//---- numberBoxTitle ----
			numberBoxTitle.setText("text");
			numberBoxGroup.add(numberBoxTitle, cc.xy(1, 1));
			numberBoxGroup.add(numberBox, cc.xy(1, 3));
		}

		//======== comboBoxGroup ========
		{
			comboBoxGroup.setLayout(new FormLayout(
				ColumnSpec.decodeSpecs("max(pref;60dlu)"),
				new RowSpec[] {
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.NARROW_LINE_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC
				}));

			//---- comboBoxTitle ----
			comboBoxTitle.setText("text");
			comboBoxGroup.add(comboBoxTitle, cc.xy(1, 1));

			//---- comboBox ----
			comboBox.setEditable(true);
			comboBoxGroup.add(comboBox, cc.xy(1, 3));
		}

		//======== listBoxGroup ========
		{
			listBoxGroup.setLayout(new FormLayout(
				ColumnSpec.decodeSpecs("default:grow"),
				new RowSpec[] {
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.NARROW_LINE_GAP_ROWSPEC,
					new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
				}));

			//---- listBoxTitle ----
			listBoxTitle.setText("Available Choices");
			listBoxGroup.add(listBoxTitle, cc.xy(1, 1));

			//======== choicesScrollPane ========
			{

				//---- availableChoices ----
				availableChoices.setBackground(Color.lightGray);
				availableChoices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				choicesScrollPane.setViewportView(availableChoices);
			}
			listBoxGroup.add(choicesScrollPane, cc.xy(1, 3));
		}
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner non-commercial license
	private JPanel titlePanel;
	private JLabel label1;
	TextBoxWidget title;
	JPanel choicePanel;
	private JPanel panel1;
	private JLabel availableItemsLabel;
	JScrollPane itemsScrollPane;
	ListBoxWidget availableItems;
	private JLabel iterConfigLabel;
	JLabel tableInfo;
	private JPanel panel2;
	private JScrollPane configScrollPane;
	CellSelectTableWidget iterStepsTable;
	JButton top;
	JButton up;
	JButton down;
	JButton bottom;
	private JPanel buttonPanel;
	JButton addStep;
	JButton deleteStep;
	JButton deleteItem;
	JPanel textBoxGroup;
	JLabel textBoxTitle;
	TextBoxWidget textBox;
	JPanel numberBoxGroup;
	JLabel numberBoxTitle;
	NumberBoxWidget numberBox;
	JPanel comboBoxGroup;
	JLabel comboBoxTitle;
	DropDownListBoxWidget comboBox;
	JPanel listBoxGroup;
	JLabel listBoxTitle;
	private JScrollPane choicesScrollPane;
	ListBoxWidget availableChoices;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
