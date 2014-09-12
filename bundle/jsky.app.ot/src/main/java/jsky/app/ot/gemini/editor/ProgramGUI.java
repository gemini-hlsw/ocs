/**
 * Title:        JSky<p>
 * Description:  <p>
 * Copyright:    Copyright (c) Allan Brighton<p>
 * Company:      <p>
 * @author Allan Brighton
 * @version 1.0
 */
package jsky.app.ot.gemini.editor;

import java.awt.*;
import javax.swing.*;

import jsky.util.gui.TextBoxWidget;
import jsky.util.gui.DropDownListBoxWidget;

public class ProgramGUI extends JPanel {

    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel titleLabel = new JLabel();
    TextBoxWidget titleBox = new TextBoxWidget();
    JLabel propModeLabel = new JLabel();
    JRadioButton queueOption = new JRadioButton();
    JRadioButton classicalOption = new JRadioButton();
    JLabel firstNameLabel = new JLabel();
    TextBoxWidget firstNameBox = new TextBoxWidget();
    JLabel lastNameLabel = new JLabel();
    JLabel emailLabel = new JLabel();
    JLabel phoneLabel = new JLabel();
    JLabel affiliationLabel = new JLabel();
    JLabel progRefLabel = new JLabel();
    JLabel contactLabel = new JLabel();
    TextBoxWidget lastNameBox = new TextBoxWidget();
    TextBoxWidget emailBox = new TextBoxWidget();
    TextBoxWidget phoneBox = new TextBoxWidget();
    JLabel queueBandLabel = new JLabel();
    TextBoxWidget queueBandBox = new TextBoxWidget();
    JLabel progStatusLabel = new JLabel();
    JLabel progKeyLabel = new JLabel();
    jsky.util.gui.DropDownListBoxWidget affiliationBox = new DropDownListBoxWidget();
    DropDownListBoxWidget progStatusBox = new DropDownListBoxWidget();
    TextBoxWidget progKeyBox = new TextBoxWidget();
    JLabel progRefBox = new JLabel();
    TextBoxWidget contactBox = new TextBoxWidget();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    JLabel totalPlannedTimeLabel = new JLabel();
    JLabel totalPlannedTime = new JLabel();
    JLabel piLabel = new JLabel();
    JLabel allocatedTimeLabel = new JLabel();
    JLabel allocatedTime = new JLabel();
    JLabel timeRemainingLabel = new JLabel();
    JLabel timeRemaining = new JLabel();
    JPanel propModePanel = new JPanel();
    GridBagLayout gridBagLayout4 = new GridBagLayout();
    JPanel timePanel = new JPanel();
    JLabel ngoContactLabel = new JLabel();
    TextBoxWidget ngoContactBox = new TextBoxWidget();
    JLabel historyLabel = new JLabel();
    JScrollPane jScrollPane1 = new JScrollPane();
    JTable historyTable = new JTable();
    JCheckBox activeCheckBox = new JCheckBox();
    JCheckBox notifyPiCheckBox = new JCheckBox();

    public ProgramGUI() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {

        titleLabel.setLabelFor(titleBox);
        titleLabel.setText("Program Title");
        this.setLayout(gridBagLayout1);
        timePanel.setLayout(gridBagLayout2);

        propModeLabel.setLabelFor(queueOption);
        propModeLabel.setText("Proposal Mode");
        queueOption.setSelected(true);
        queueOption.setText("Queue");

        classicalOption.setText("Classical");


        firstNameLabel.setLabelFor(firstNameBox);
        firstNameLabel.setText("First Name");


        lastNameLabel.setLabelFor(lastNameBox);
        lastNameLabel.setText("Last Name");


        emailLabel.setLabelFor(emailBox);
        emailLabel.setText("PI / PC Email");


        phoneLabel.setLabelFor(phoneBox);
        phoneLabel.setText("Phone");


        affiliationLabel.setLabelFor(affiliationBox);
        affiliationLabel.setText("Affiliation");


        progRefLabel.setLabelFor(progRefBox);
        progRefLabel.setText("Program Reference");


        contactLabel.setLabelFor(contactBox);
        contactLabel.setText("Contact Sci. Email");


        queueBandLabel.setLabelFor(queueBandBox);
        queueBandLabel.setText("Queue Band");


        progStatusLabel.setVisible(false);
        progStatusLabel.setLabelFor(progStatusBox);
        progStatusLabel.setText("Program Status");

        progKeyLabel.setLabelFor(progKeyBox);
        progKeyLabel.setText("Program Key");

        totalPlannedTimeLabel.setToolTipText(
                "The total time planned for the observation, in hours:minutes:seconds");
        totalPlannedTimeLabel.setLabelFor(totalPlannedTime);
        totalPlannedTimeLabel.setText("Total Planned Time");
        totalPlannedTime.setFont(new java.awt.Font("Dialog", 0, 12));
        totalPlannedTime.setForeground(Color.black);
        totalPlannedTime.setToolTipText(
                "The total time planned for the observation, in hours:minutes:seconds");
        totalPlannedTime.setText("00:00:00");

        piLabel.setText("Principal Investigator / Contact");
        allocatedTimeLabel.setToolTipText(
                "The total time allocated for the observation, in hours:minutes:seconds");
        allocatedTimeLabel.setText("Allocated");
        allocatedTime.setFont(new java.awt.Font("Dialog", 0, 12));
        allocatedTime.setForeground(Color.black);
        allocatedTime.setToolTipText(
                "The total time allocated for the observation, in hours:minutes:seconds");
        allocatedTime.setText("00:00:00");
        timeRemainingLabel.setToolTipText("Remaining Time (total of Allocated Time minus the sum of the Time " +
                "Used fields in the observations)");
        timeRemainingLabel.setText("Remaining");
        timeRemaining.setFont(new java.awt.Font("Dialog", 0, 12));
        timeRemaining.setForeground(Color.black);
        timeRemaining.setToolTipText("Remaining Time (total of Allocated Time minus the sum of the Time " +
                "Used fields in the observations)");
        timeRemaining.setText("00:00:00");
        propModePanel.setLayout(gridBagLayout4);
        progRefBox.setText("unknown");
        progRefBox.setForeground(Color.black);
        ngoContactLabel.setText("NGO Contact Email");
        ngoContactBox.setText("");
        progStatusBox.setVisible(false);
        historyLabel.setText("History");
        affiliationBox.setMinimumSize(new Dimension(120, 25));
        affiliationBox.setPreferredSize(new Dimension(120, 25));
        historyTable.setRowHeight(20);
        activeCheckBox.setText("Active");
        activeCheckBox.setToolTipText("Mark this program as active or inactive");
        notifyPiCheckBox.setText("Notify PI");
        notifyPiCheckBox.setBorderPaintedFlat(false);
        notifyPiCheckBox.setToolTipText(
                "Enable/disable automatic notification of PI when data are collected");
        notifyPiCheckBox.setBorderPainted(false);
        this.add(titleLabel,
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(titleBox,
                new GridBagConstraints(1, 0, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                        GridBagConstraints.HORIZONTAL, new Insets(11, 5, 0, 11), 0, 0));
        propModePanel.add(classicalOption,
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(6, 5, 0, 0), 0, 0));
        propModePanel.add(queueOption,
                new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(6, 5, 0, 0), 0, 0));
        this.add(activeCheckBox,
                 new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 8, 0, 0), 0, 0));
        this.add(emailLabel,
                new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(emailBox,
                new GridBagConstraints(1, 8, 3, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, new Insets(11, 5, 0, 11), 0, 0));
        this.add(ngoContactBox,
                new GridBagConstraints(1, 9, 3, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, new Insets(6, 5, 0, 11), 0, 0));
        this.add(ngoContactLabel,
                new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        this.add(progKeyLabel,
                new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        this.add(progKeyBox,
                new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, new Insets(6, 5, 0, 11), 0, 0));
        this.add(historyLabel,
                 new GridBagConstraints(0, 12, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        this.add(queueBandLabel,
                new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        this.add(queueBandBox,
                new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, new Insets(6, 5, 0, 11), 0, 0));
        this.add(progStatusLabel,
                new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        this.add(progStatusBox,
                new GridBagConstraints(1, 4, 4, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        this.add(lastNameLabel,
                new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        this.add(lastNameBox,
                new GridBagConstraints(3, 6, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, new Insets(6, 5, 0, 11), 0, 0));
        this.add(phoneLabel,
                new GridBagConstraints(2, 7, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        this.add(phoneBox,
                new GridBagConstraints(3, 7, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, new Insets(6, 5, 0, 11), 0, 0));
        this.add(progRefLabel,
                new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(progRefBox,
                new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, new Insets(11, 5, 0, 11), 0, 0));
        this.add(firstNameLabel,
                new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, new Insets(6, 40, 0, 0), 0, 0));
        this.add(firstNameBox,
                new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, new Insets(6, 5, 0, 0), 0, 0));
        this.add(affiliationLabel,
                new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, new Insets(6, 40, 0, 0), 0, 0));
        this.add(contactLabel,
                new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        this.add(affiliationBox,
                new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, new Insets(6, 5, 0, 0), 0, 0));
        this.add(timePanel,
                new GridBagConstraints(0, 11, 4, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        timePanel.add(totalPlannedTime,
                new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(6, 5, 0, 0), 0, 0));
        timePanel.add(totalPlannedTimeLabel,
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        this.add(contactBox,
                new GridBagConstraints(1, 10, 3, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, new Insets(6, 5, 0, 11), 0, 0));
        this.add(piLabel,
                 new GridBagConstraints(0, 5, 5, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        timePanel.add(allocatedTimeLabel,
                new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        timePanel.add(allocatedTime,
                new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(6, 5, 0, 0), 0, 0));
        timePanel.add(timeRemainingLabel,
                new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        timePanel.add(timeRemaining,
                new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(6, 5, 0, 0), 0, 0));
        this.add(jScrollPane1,
                new GridBagConstraints(0, 13, 4, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                        GridBagConstraints.BOTH, new Insets(6, 11, 11, 11), 0, 0));
        this.add(propModeLabel,
                new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        this.add(propModePanel,
                new GridBagConstraints(1, 2, 3, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jScrollPane1.getViewport().add(historyTable, null);
        this.add(notifyPiCheckBox,
                  new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 8, 0, 0), 0, 0));
        this.setPreferredSize(new Dimension(488, 427));
    }
}
