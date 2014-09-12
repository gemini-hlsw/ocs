/**
 * Title:        JSky<p>
 * Description:  <p>
 * Copyright:    Copyright (c) Allan Brighton<p>
 * Company:      <p>
 * @author Allan Brighton
 * @version 1.0
 */
package jsky.app.ot.editor;

import java.awt.*;
import javax.swing.*;

import jsky.util.gui.*;

import java.awt.event.*;

public class MiniConfigIterGUI extends JPanel {
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel jLabel2 = new JLabel();
    JScrollPane itemsScrollPane = new JScrollPane();
    ListBoxWidget availableItems = new ListBoxWidget();
    JLabel jLabel3 = new JLabel();
    JLabel tableInfo = new JLabel();
    JScrollPane configScrollPane = new JScrollPane();
    JPanel jPanel1 = new JPanel();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    JButton deleteItem = new JButton();
    JButton deleteStep = new JButton();
    JButton addStep = new JButton();
    JButton top = new JButton();
    JButton up = new JButton();
    JButton down = new JButton();
    JButton bottom = new JButton();
    CellSelectTableWidget iterStepsTable = new CellSelectTableWidget();
    JLabel listBoxTitle = new JLabel();
    JPanel listBoxGroup = new JPanel();
    GridBagLayout gridBagLayout4 = new GridBagLayout();
    GridBagLayout gridBagLayout5 = new GridBagLayout();
    GridBagLayout gridBagLayout6 = new GridBagLayout();
    JPanel choicePanel = new JPanel();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    JPanel textBoxGroup = new JPanel();
    JPanel comboBoxGroup = new JPanel();
    JPanel numberBoxGroup = new JPanel();
    JScrollPane choicesScrollPane = new JScrollPane();
    BorderLayout borderLayout1 = new BorderLayout();
    ListBoxWidget availableChoices = new ListBoxWidget();
    JLabel textBoxTitle = new JLabel();
    JLabel comboBoxTitle = new JLabel();
    JLabel numberBoxTitle = new JLabel();
    TextBoxWidget textBox = new TextBoxWidget();
    DropDownListBoxWidget comboBox = new DropDownListBoxWidget();
    NumberBoxWidget numberBox = new NumberBoxWidget();

    public MiniConfigIterGUI() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        this.setPreferredSize(new Dimension(394, 356));
        this.setLayout(gridBagLayout1);

        jLabel2.setLabelFor(availableItems);
        jLabel2.setText("Available Items:");

        jLabel3.setLabelFor(configScrollPane);
        jLabel3.setText("Iteration Config:");

        tableInfo.setText("(0 Items, 0 Steps)");

        jPanel1.setLayout(gridBagLayout2);

        deleteItem.setToolTipText("Delete an item (column) from the table");
        deleteItem.setText("Delete Item");

        deleteStep.setToolTipText("Delete a step (row) from the table");
        deleteStep.setText("Delete Step");

        addStep.setToolTipText("Add a step (row) to the table");
        addStep.setText("Add Step");
        down.setToolTipText("Move the selected item down one row");
        down.setMargin(new Insets(2, 2, 2, 2));
        availableItems.setBackground(Color.lightGray);
        availableItems.setToolTipText("Select an item to add to the iteration config table");
        iterStepsTable.setBackground(Color.lightGray);
        iterStepsTable.setShowHorizontalLines(false);
        iterStepsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        listBoxTitle.setText("Available Choices:");

        Dimension labelDim = new Dimension(200, 20);
        textBox.setPreferredSize(labelDim);
        textBoxTitle.setPreferredSize(labelDim);
        textBoxTitle.setMaximumSize(labelDim);
        textBoxTitle.setMinimumSize(labelDim);
        numberBox.setPreferredSize(labelDim);
        numberBoxTitle.setPreferredSize(labelDim);
        numberBoxTitle.setMaximumSize(labelDim);
        numberBoxTitle.setMinimumSize(labelDim);
        comboBoxTitle.setPreferredSize(labelDim);
        comboBoxTitle.setMaximumSize(labelDim);
        comboBoxTitle.setMinimumSize(labelDim);
        listBoxTitle.setPreferredSize(labelDim);
        listBoxTitle.setMaximumSize(labelDim);
        listBoxTitle.setMinimumSize(labelDim);

        listBoxGroup.setLayout(gridBagLayout3);
        choicePanel.setLayout(borderLayout1);
        textBoxGroup.setLayout(gridBagLayout4);
        comboBoxGroup.setLayout(gridBagLayout6);
        numberBoxGroup.setLayout(gridBagLayout5);
        numberBox.setAllowNegative(false);
        availableChoices.setBackground(Color.lightGray);
        availableChoices.setToolTipText("Choose an item to insert at the selected position in the iteration " +
                "config table");

        textBoxTitle.setLabelFor(textBox);
        comboBoxTitle.setLabelFor(comboBox);
        comboBox.setEditable(true);
        numberBoxTitle.setLabelFor(numberBox);
        textBoxTitle.setText("Value:");
        comboBoxTitle.setText("Value:");
        numberBoxTitle.setText("Value:");
        top.setToolTipText("Move the selected item to the top");
        top.setMargin(new Insets(2, 2, 2, 2));
        up.setToolTipText("Move the selected item up one row");
        up.setMargin(new Insets(2, 2, 2, 2));
        bottom.setToolTipText("Move the selected item to the bottom");
        bottom.setMargin(new Insets(2, 2, 2, 2));
        itemsScrollPane.setPreferredSize(new Dimension(200, 131));
    choicePanel.setPreferredSize(new Dimension(259, 151));
    this.add(jLabel2,  new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 5, 0, 5), 0, 0));
        this.add(itemsScrollPane,       new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(4, 5, 0, 0), 0, 0));
        this.add(jLabel3, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        this.add(tableInfo, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 0, 0, 11), 0, 0));
        this.add(configScrollPane,   new GridBagConstraints(0, 3, 2, 4, 0.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 11, 0, 0), 0, 0));
        configScrollPane.getViewport().add(iterStepsTable, null);
        this.add(jPanel1, new GridBagConstraints(0, 8, 2, 1, 1.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(deleteItem, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(11, 5, 11, 0), 0, 0));
        jPanel1.add(deleteStep, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(11, 5, 11, 0), 0, 0));
        jPanel1.add(addStep, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
                , GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(11, 5, 11, 0), 0, 0));
        this.add(top,   new GridBagConstraints(2, 3, 1, 1, 0.0, 0.25
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
        this.add(up,   new GridBagConstraints(2, 4, 1, 1, 0.0, 0.25
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
        this.add(down,   new GridBagConstraints(2, 5, 1, 1, 0.0, 0.25
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
        this.add(bottom,   new GridBagConstraints(2, 6, 1, 1, 0.0, 0.25
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
        this.add(choicePanel,   new GridBagConstraints(0, 0, 1, 2, 0.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(11, 11, 0, 0), 0, 0));
        choicePanel.add(listBoxGroup,  BorderLayout.CENTER);
        listBoxGroup.add(choicesScrollPane,   new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        listBoxGroup.add(listBoxTitle, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        textBoxGroup.add(textBoxTitle, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        comboBoxGroup.add(comboBoxTitle, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        numberBoxGroup.add(numberBoxTitle, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        textBoxGroup.add(textBox, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
                , GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        comboBoxGroup.add(comboBox, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
                , GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        numberBoxGroup.add(numberBox, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
                , GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        choicesScrollPane.getViewport().add(availableChoices, null);
        itemsScrollPane.getViewport().add(availableItems, null);
    }
}
