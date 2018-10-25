package jsky.app.ot.gemini.editor;

import com.jgoodies.forms.factories.DefaultComponentFactory;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * NOTE: component names / labels are no longer congruent with data object member names because of REL-2942.
 */
public class ProgramForm extends JPanel {
    public ProgramForm() {
        final DefaultComponentFactory compFactory = DefaultComponentFactory.getInstance();
        final CellConstraints cc = new CellConstraints();

        setBorder(new EmptyBorder(6, 6, 6, 6));
        setLayout(new FormLayout(
                new ColumnSpec[]{
                        new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        ColumnSpec.decode("max(pref;150px)"),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.UNRELATED_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.MIN_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.LEFT, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC
                },
                new RowSpec[]{
                        FormFactory.RELATED_GAP_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.PARAGRAPH_GAP_ROWSPEC,
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
                        FormFactory.PARAGRAPH_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.PARAGRAPH_GAP_ROWSPEC,
                        new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                        FormFactory.LINE_GAP_ROWSPEC,
                        new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.NO_GROW)
                })
        );

        // Program Attribute Section

        // Program Title
        int row = 3;

        final JLabel titleLabel = new JLabel("Program Title");
        titleBox = new TextBoxWidget();
        titleLabel.setLabelFor(titleBox);
        add(titleLabel, cc.xy(1, row));
        add(titleBox, cc.xywh(3, row, 9, 1));

        // Program Reference
        row += 2;

        add(new JLabel("Program Reference"), cc.xy(1, row));
        progRefBox = new JLabel("GN0000000");
        add(progRefBox, cc.xywh(3, row, 9, 1));

        // TOO Status, Active, Completed
        row += 2;

        add(new JLabel("TOO Status"), cc.xy(1, row));
        tooStatusLabel = new JLabel();
        tooStatusLabel.setText("None");
        add(tooStatusLabel, cc.xy(3, row));

        final GridBagLayout panel1Layout = new GridBagLayout();
        panel1Layout.columnWidths = new int[]{0, 0, 0};
        panel1Layout.rowHeights = new int[]{0, 0};
        panel1Layout.columnWeights = new double[]{0.0, 0.0, 1.0E-4};
        panel1Layout.rowWeights = new double[]{1.0, 1.0E-4};
        final JPanel panel1 = new JPanel(panel1Layout);

        activeCheckBox = new JCheckBox("Active");
        activeCheckBox.setToolTipText("Mark this program as active or inactive");
        panel1.add(activeCheckBox, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 0), 0, 0));

        completedCheckBox = new JCheckBox("Completed");
        panel1.add(completedCheckBox, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 15, 0, 0), 0, 0));

        add(panel1, cc.xywh(5, row, 7, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));

        // Notifications
        row += 2;

        add(new JLabel("Notifications"), cc.xy(1, row));

        notifyPiCheckBox = new JCheckBox("New science data");
        notifyPiCheckBox.setToolTipText("Enable/disable automatic notification of PI when data are collected");
        add(notifyPiCheckBox, cc.xy(3, row, CellConstraints.LEFT, CellConstraints.DEFAULT));

        timingWindowNotificationCheckBox = new JCheckBox("Expired timing windows");
        timingWindowNotificationCheckBox.setToolTipText("Enable/disable email notifications sent for expired timing windows");
        add(timingWindowNotificationCheckBox, cc.xywh(5, row, 7, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));

        // Prinicipal Investigator Section Heading
        row += 2;

        add(compFactory.createSeparator("Principal Investigator / Contact"), cc.xywh(1, row, 11, 1));

        // First Name / Last Name
        row += 2;

        final JLabel firstNameLabel = new JLabel("First Name");
        firstNameBox = new TextBoxWidget();
        firstNameLabel.setLabelFor(firstNameBox);
        add(firstNameLabel, cc.xy(1, row));
        add(firstNameBox, cc.xy(3, row));

        final JLabel lastNamelabel = new JLabel("Last Name");
        lastNameBox = new TextBoxWidget();
        lastNamelabel.setLabelFor(lastNameBox);
        add(lastNamelabel, cc.xywh(7, row, 1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        add(lastNameBox, cc.xywh(9, row, 3, 1));

        // Support / Phone
        row += 2;

        add(new JLabel("Support"), cc.xy(1, row));
        affiliationBox = new JLabel();
        add(affiliationBox, cc.xy(3, row));

        final JLabel phoneLabel = new JLabel("Phone");
        phoneBox = new TextBoxWidget();
        phoneLabel.setLabelFor(phoneBox);
        add(phoneLabel, cc.xywh(7, row, 1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        add(phoneBox, cc.xywh(9, row, 3, 1));

        // PI Email
        row += 2;

        final JLabel emailLabel = new JLabel("Investigator Email");
        emailBox = new TextBoxWidget();
        emailLabel.setLabelFor(emailBox);
        add(emailLabel, cc.xy(1, row));
        add(emailBox, cc.xywh(3, row, 9, 1));

        // Support Email
        row += 2;

        principalSupportBox = new TextBoxWidget();
        final JLabel principalSupportLabel = new JLabel("Principal Support Email");
        principalSupportLabel.setLabelFor(principalSupportBox);
        add(principalSupportLabel, cc.xy(1, row));
        add(principalSupportBox, cc.xywh(3, row, 9, 1));

        // Additional Support Email
        row += 2;

        add(new JLabel("Additional Support Email"), cc.xy(1, row));
        additionalSupportBox = new JLabel();
        add(additionalSupportBox, cc.xywh(3, row, 9, 1));

        // Observing Time Section Header
        row += 2;

        add(compFactory.createSeparator("Observing Time"), cc.xywh(1, row, 11, 1));
        final JPanel timePanel = new JPanel(new FormLayout(
                    new ColumnSpec[]{
                            new ColumnSpec(ColumnSpec.CENTER, Sizes.dluX(36), FormSpec.NO_GROW),
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            new ColumnSpec(ColumnSpec.CENTER, Sizes.dluX(36), FormSpec.NO_GROW),
                            new ColumnSpec(ColumnSpec.LEFT, Sizes.dluX(15), FormSpec.NO_GROW),
                            new ColumnSpec(ColumnSpec.CENTER, Sizes.dluX(36), FormSpec.NO_GROW),
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            new ColumnSpec(ColumnSpec.CENTER, Sizes.dluX(36), FormSpec.NO_GROW),
                            new ColumnSpec(ColumnSpec.LEFT, Sizes.dluX(15), FormSpec.NO_GROW),
                            new ColumnSpec(ColumnSpec.CENTER, Sizes.dluX(56), FormSpec.NO_GROW),
                            FormFactory.UNRELATED_GAP_COLSPEC,
                            FormFactory.DEFAULT_COLSPEC,
                            new ColumnSpec(ColumnSpec.LEFT, Sizes.DLUX11, FormSpec.NO_GROW),
                            new ColumnSpec(ColumnSpec.CENTER, Sizes.dluX(58), FormSpec.NO_GROW)
                    },
                    new RowSpec[]{
                            FormFactory.DEFAULT_ROWSPEC,
                            FormFactory.NARROW_LINE_GAP_ROWSPEC,
                            FormFactory.DEFAULT_ROWSPEC,
                            FormFactory.LINE_GAP_ROWSPEC,
                            FormFactory.DEFAULT_ROWSPEC
                    })
        );

        plannedLabel = new JLabel("Planned");
        timePanel.add(plannedLabel, cc.xywh(1, 1, 3, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

        usedLabel = new JLabel("Used");
        timePanel.add(usedLabel, cc.xywh(5, 1, 3, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

        totalPlannedExecTimeLabel = new JLabel("Exec");
        timePanel.add(totalPlannedExecTimeLabel, cc.xy(1, 3));
        totalPlannedExecTime = new JLabel("00:00:00");
        totalPlannedExecTime.setToolTipText("The total time planned for the execution of  this program, in hours:minutes:seconds");
        timePanel.add(totalPlannedExecTime, cc.xy(1, 5));

        totalPlannedPiTimeLabel = new JLabel("PI");
        timePanel.add(totalPlannedPiTimeLabel, cc.xy(3, 3));
        totalPlannedPiTime = new JLabel("00:00:00");
        totalPlannedPiTime.setToolTipText("The total time planned to be charged to the PI for this program, in hours:minutes:seconds");
        timePanel.add(totalPlannedPiTime, cc.xy(3, 5));

        timePanel.add(new JLabel("Program"), cc.xy(5, 3));
        programTime = new JLabel("00:00:00");
        programTime.setToolTipText("Total time charged to the program");
        timePanel.add(programTime, cc.xy(5, 5));

        timePanel.add(new JLabel("Partner"), cc.xy(7, 3));
        partnerTime = new JLabel("00:00:00");
        partnerTime.setToolTipText("Total time charged to the Gemini partner");
        timePanel.add(partnerTime, cc.xy(7, 5));

        timePanel.add(new JLabel("Allocated Program"), cc.xy(9, 3));
        allocatedTime = new JLabel("00:00:00");
        allocatedTime.setToolTipText("The total program time allocated, in hours:minutes:seconds");
        timePanel.add(allocatedTime, cc.xy(9, 5));

        minimumTimeLabel = new JLabel("Minimum");
        timePanel.add(minimumTimeLabel, cc.xy(11, 3));
        minimumTime = new JLabel("00:00:00");
        minimumTime.setToolTipText("The minimal success time, in hours:minutes:seconds");
        timePanel.add(minimumTime, cc.xy(11, 5));

        timePanel.add(new JLabel("Remaining Program"), cc.xy(13, 3));
        remainingTime = new JLabel("00:00:00");
        remainingTime.setToolTipText("Remaining Program Time (total of Allocated Program Time minus the sum of the Program Time fields in the observations)");
        timePanel.add(remainingTime, cc.xy(13, 5));

        row += 2;
        add(timePanel, cc.xywh(1, row, 11, 1));

        // History
        row += 2;
        final JPanel historyPanel = new JPanel(new BorderLayout());
        historyTable = new JTable();
        historyTable.setBackground(Color.lightGray);
        historyPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Sync History", historyPanel);
        add(tabbedPane, cc.xywh(1, row, 11, 1));
    }

    final TextBoxWidget titleBox;
    final JLabel progRefBox;
    final JLabel tooStatusLabel;
    final JCheckBox notifyPiCheckBox;
    final JCheckBox timingWindowNotificationCheckBox;
    final JCheckBox activeCheckBox;
    final JCheckBox completedCheckBox;
    final TextBoxWidget firstNameBox;
    final TextBoxWidget lastNameBox;
    final JLabel affiliationBox;
    final TextBoxWidget phoneBox;
    final TextBoxWidget emailBox;
    final TextBoxWidget principalSupportBox;
    final JLabel additionalSupportBox;
    final JLabel plannedLabel;
    final JLabel usedLabel;
    final JLabel totalPlannedExecTimeLabel;
    final JLabel totalPlannedPiTimeLabel;
    final JLabel totalPlannedExecTime;
    final JLabel totalPlannedPiTime;
    final JLabel programTime;
    final JLabel partnerTime;
    final JLabel allocatedTime;
    final JLabel minimumTimeLabel;
    final JLabel minimumTime;
    final JLabel remainingTime;
    final JTable historyTable;
    public final JTabbedPane tabbedPane;
}
