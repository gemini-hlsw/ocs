package jsky.app.ot.gemini.editor;

import com.jgoodies.forms.factories.DefaultComponentFactory;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ProgramForm extends JPanel {
    public ProgramForm() {
        initComponents();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        DefaultComponentFactory compFactory = DefaultComponentFactory.getInstance();
        JLabel titleLabel = new JLabel();
        titleBox = new TextBoxWidget();
        JLabel progRefLabel = new JLabel();
        progRefBox = new JLabel();
        JLabel label2 = new JLabel();
        tooStatusLabel = new JLabel();
        JPanel panel2 = new JPanel();
        notifyPiCheckBox = new JCheckBox();
        activeCheckBox = new JCheckBox();
        completedCheckBox = new JCheckBox();
        JComponent piFormsSeparator = compFactory.createSeparator("Principal Investigator / Contact");
        JLabel firstNameLabel = new JLabel();
        firstNameBox = new TextBoxWidget();
        JLabel lastNamelabel = new JLabel();
        lastNameBox = new TextBoxWidget();
        JLabel affiliationLabel = new JLabel();
        affiliationBox = new JLabel();
        JLabel phoneLabel = new JLabel();
        phoneBox = new TextBoxWidget();
        JLabel emailLabel = new JLabel();
        emailBox = new TextBoxWidget();
        JLabel ngoContactLabel = new JLabel();
        ngoContactBox = new TextBoxWidget();
        JLabel contactLabel = new JLabel();
        contactBox = new JLabel();
        JComponent obsTimeFormsSeparator = compFactory.createSeparator("Observing Time");
        JPanel panel1 = new JPanel();
        plannedLabel = new JLabel();
        usedLabel = new JLabel();
        totalPlannedExecTimeLabel = new JLabel();
        totalPlannedPiTimeLabel = new JLabel();
        JLabel programTimeLabel = new JLabel();
        JLabel partnerTimeLabel = new JLabel();
        JLabel allocatedTimeLabel = new JLabel();
        minimumTimeLabel = new JLabel();
        JLabel timeRemainingLabel = new JLabel();
        totalPlannedExecTime = new JLabel();
        totalPlannedPiTime = new JLabel();
        programTime = new JLabel();
        partnerTime = new JLabel();
        allocatedTime = new JLabel();
        minimumTime = new JLabel();
        timeRemaining = new JLabel();
        tabbedPane = new JTabbedPane();
        addAttachmentButton = new JButton();
        removeAttachmentButton = new JButton();
        updateAttachmentButton = new JButton();
        fetchAttachmentButton = new JButton();
        openAttachmentButton = new JButton();
        setAttachDescButton = new JButton();
        attachDescField = new JTextField();
        attachmentTable = new JTable();
        JPanel panel4 = new JPanel();
        JScrollPane scrollPane1 = new JScrollPane();
        historyTable = new JTable();
        CellConstraints cc = new CellConstraints();

        //======== this ========
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
                }));

        //---- titleLabel ----
        titleLabel.setText("Program Title");
        titleLabel.setLabelFor(titleBox);
        add(titleLabel, cc.xy(1, 3));
        add(titleBox, cc.xywh(3, 3, 9, 1));

        //---- progRefLabel ----
        progRefLabel.setText("Program Reference");
        add(progRefLabel, cc.xy(1, 5));

        //---- progRefBox ----
        progRefBox.setText("GN0000000");
        add(progRefBox, cc.xywh(3, 5, 9, 1));

        //---- label2 ----
        label2.setText("TOO Status");
        add(label2, cc.xy(1, 7));

        //---- tooStatusLabel ----
        tooStatusLabel.setText("None");
        add(tooStatusLabel, cc.xy(3, 7));

        //======== panel2 ========
        {
            panel2.setLayout(new GridBagLayout());
            ((GridBagLayout) panel2.getLayout()).columnWidths = new int[]{0, 0, 0, 0};
            ((GridBagLayout) panel2.getLayout()).rowHeights = new int[]{0, 0};
            ((GridBagLayout) panel2.getLayout()).columnWeights = new double[]{0.0, 0.0, 0.0, 1.0E-4};
            ((GridBagLayout) panel2.getLayout()).rowWeights = new double[]{1.0, 1.0E-4};

            //---- notifyPiCheckBox ----
            notifyPiCheckBox.setText("Notify PI");
            notifyPiCheckBox.setToolTipText("Enable/disable automatic notification of PI when data are collected");
            panel2.add(notifyPiCheckBox, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 0), 0, 0));

            //---- activeCheckBox ----
            activeCheckBox.setText("Active");
            activeCheckBox.setToolTipText("Mark this program as active or inactive");
            panel2.add(activeCheckBox, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 15, 0, 0), 0, 0));

            //---- completedCheckBox ----
            completedCheckBox.setText("Completed");
            panel2.add(completedCheckBox, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 15, 0, 0), 0, 0));
        }
        add(panel2, cc.xywh(5, 7, 7, 1));
        add(piFormsSeparator, cc.xywh(1, 9, 11, 1));

        //---- firstNameLabel ----
        firstNameLabel.setText("First Name");
        firstNameLabel.setLabelFor(firstNameBox);
        add(firstNameLabel, cc.xy(1, 11));
        add(firstNameBox, cc.xy(3, 11));

        //---- lastNamelabel ----
        lastNamelabel.setText("Last Name");
        lastNamelabel.setLabelFor(lastNameBox);
        add(lastNamelabel, cc.xywh(7, 11, 1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        add(lastNameBox, cc.xywh(9, 11, 3, 1));

        //---- affiliationLabel ----
        affiliationLabel.setText("Support");
        add(affiliationLabel, cc.xy(1, 13));
        add(affiliationBox, cc.xy(3, 13));

        //---- phoneLabel ----
        phoneLabel.setText("Phone");
        phoneLabel.setLabelFor(phoneBox);
        add(phoneLabel, cc.xywh(7, 13, 1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        add(phoneBox, cc.xywh(9, 13, 3, 1));

        //---- emailLabel ----
        emailLabel.setText("PI / PC Email");
        emailLabel.setLabelFor(emailBox);
        add(emailLabel, cc.xy(1, 15));
        add(emailBox, cc.xywh(3, 15, 9, 1));

        //---- ngoContactLabel ----
        ngoContactLabel.setText("NGO Contact Email ");
        ngoContactLabel.setLabelFor(ngoContactBox);
        add(ngoContactLabel, cc.xy(1, 17));
        add(ngoContactBox, cc.xywh(3, 17, 9, 1));

        //---- contactLabel ----
        contactLabel.setText("Contact Sci. Email");
        contactLabel.setLabelFor(ngoContactBox);
        add(contactLabel, cc.xy(1, 19));
        add(contactBox, cc.xywh(3, 19, 9, 1));
        add(obsTimeFormsSeparator, cc.xywh(1, 21, 11, 1));

        //======== panel1 ========
        {
            panel1.setLayout(new FormLayout(
                    new ColumnSpec[]{
                            new ColumnSpec(ColumnSpec.CENTER, Sizes.dluX(36), FormSpec.NO_GROW),
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            new ColumnSpec(ColumnSpec.CENTER, Sizes.dluX(36), FormSpec.NO_GROW),
                            new ColumnSpec(ColumnSpec.LEFT, Sizes.dluX(15), FormSpec.NO_GROW),
                            new ColumnSpec(ColumnSpec.CENTER, Sizes.dluX(36), FormSpec.NO_GROW),
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            new ColumnSpec(ColumnSpec.CENTER, Sizes.dluX(36), FormSpec.NO_GROW),
                            new ColumnSpec(ColumnSpec.LEFT, Sizes.dluX(15), FormSpec.NO_GROW),
                            new ColumnSpec(ColumnSpec.CENTER, Sizes.dluX(36), FormSpec.NO_GROW),
                            FormFactory.UNRELATED_GAP_COLSPEC,
                            FormFactory.DEFAULT_COLSPEC,
                            new ColumnSpec(ColumnSpec.LEFT, Sizes.DLUX11, FormSpec.NO_GROW),
                            new ColumnSpec(ColumnSpec.CENTER, Sizes.dluX(36), FormSpec.NO_GROW)
                    },
                    new RowSpec[]{
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

            //---- totalPlannedExecTimeLabel ----
            totalPlannedExecTimeLabel.setText("Exec");
            panel1.add(totalPlannedExecTimeLabel, cc.xy(1, 3));

            //---- totalPlannedPiTimeLabel ----
            totalPlannedPiTimeLabel.setText("PI");
            panel1.add(totalPlannedPiTimeLabel, cc.xy(3, 3));

            //---- programTimeLabel ----
            programTimeLabel.setText("Program");
            panel1.add(programTimeLabel, cc.xy(5, 3));

            //---- partnerTimeLabel ----
            partnerTimeLabel.setText("Partner");
            panel1.add(partnerTimeLabel, cc.xy(7, 3));

            //---- allocatedTimeLabel ----
            allocatedTimeLabel.setText("Allocated");
            panel1.add(allocatedTimeLabel, cc.xy(9, 3));

            //---- minimumTimeLabel ----
            minimumTimeLabel.setText("Minimum");
            panel1.add(minimumTimeLabel, cc.xy(11, 3));

            //---- timeRemainingLabel ----
            timeRemainingLabel.setText("Remaining");
            panel1.add(timeRemainingLabel, cc.xy(13, 3));

            //---- totalPlannedExecTime ----
            totalPlannedExecTime.setText("00:00:00");
            totalPlannedExecTime.setToolTipText("The total time planned for the execution of  this program, in hours:minutes:seconds");
            panel1.add(totalPlannedExecTime, cc.xy(1, 5));

            //---- totalPlannedPiTime ----
            totalPlannedPiTime.setText("00:00:00");
            totalPlannedPiTime.setToolTipText("The total time planned to be charged to the PI for this program, in hours:minutes:seconds");
            panel1.add(totalPlannedPiTime, cc.xy(3, 5));

            //---- programTime ----
            programTime.setText("00:00:00");
            programTime.setToolTipText("Total time charged to the program");
            panel1.add(programTime, cc.xy(5, 5));

            //---- partnerTime ----
            partnerTime.setText("00:00:00");
            partnerTime.setToolTipText("Total time charged to the Gemini partner");
            panel1.add(partnerTime, cc.xy(7, 5));

            //---- allocatedTime ----
            allocatedTime.setText("00:00:00");
            allocatedTime.setToolTipText("The total time allocated for the observation, in hours:minutes:seconds");
            panel1.add(allocatedTime, cc.xy(9, 5));

            //---- minimumTime ----
            minimumTime.setText("00:00:00");
            minimumTime.setToolTipText("The minimal success time, in hours:minutes:seconds");
            panel1.add(minimumTime, cc.xy(11, 5));

            //---- timeRemaining ----
            timeRemaining.setText("00:00:00");
            timeRemaining.setToolTipText("Remaining Time (total of Allocated Time minus the sum of the Program Time fields in the observations)");
            panel1.add(timeRemaining, cc.xy(13, 5));
        }
        add(panel1, cc.xywh(1, 23, 11, 1));

        //======== tabbedPane1 ========
        {
            //======== panel4 ========
            {
                panel4.setLayout(new BorderLayout());

                //======== scrollPane1 ========
                {
                    //---- historyTable ----
                    historyTable.setBackground(Color.lightGray);
                    scrollPane1.setViewportView(historyTable);
                }
                panel4.add(scrollPane1, BorderLayout.CENTER);
            }
            tabbedPane.addTab("Sync History", panel4);

        }
        add(tabbedPane, cc.xywh(1, 25, 11, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    TextBoxWidget titleBox;
    JLabel progRefBox;
    JLabel tooStatusLabel;
    JCheckBox notifyPiCheckBox;
    JCheckBox activeCheckBox;
    JCheckBox completedCheckBox;
    TextBoxWidget firstNameBox;
    TextBoxWidget lastNameBox;
    JLabel affiliationBox;
    TextBoxWidget phoneBox;
    TextBoxWidget emailBox;
    TextBoxWidget ngoContactBox;
    JLabel contactBox;
    JLabel plannedLabel;
    JLabel usedLabel;
    JLabel totalPlannedExecTimeLabel;
    JLabel totalPlannedPiTimeLabel;
    public JLabel minimumTimeLabel;
    JLabel totalPlannedExecTime;
    JLabel totalPlannedPiTime;
    JLabel programTime;
    JLabel partnerTime;
    JLabel allocatedTime;
    JLabel minimumTime;
    JLabel timeRemaining;
    public JButton addAttachmentButton;
    public JButton removeAttachmentButton;
    public JButton updateAttachmentButton;
    public JButton fetchAttachmentButton;
    public JButton openAttachmentButton;
    public JButton setAttachDescButton;
    public JTextField attachDescField;
    public JTable attachmentTable;
    JTable historyTable;
    public JTabbedPane tabbedPane;
}
