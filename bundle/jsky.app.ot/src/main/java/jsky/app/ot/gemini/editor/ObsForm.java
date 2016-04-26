package jsky.app.ot.gemini.editor;

import com.jgoodies.forms.factories.DefaultComponentFactory;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import edu.gemini.spModel.obs.ObsPhase2Status;
import edu.gemini.spModel.obs.ObsQaState;
import jsky.app.ot.gemini.parallacticangle.ParallacticAngleControls;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.NumberBoxWidget;
import jsky.util.gui.SingleSelectComboBox;
import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
import java.awt.*;
/*
 * Created by JFormDesigner on Wed May 25 14:26:02 CEST 2005
 */



/**
 * @author Allan Brighton
 */
public class ObsForm extends JPanel {
    public ObsForm() {
        initComponents();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        DefaultComponentFactory compFactory = DefaultComponentFactory.getInstance();
        originatingTemplateLabel = new JLabel();
        originatingTemplate = new JLabel();
        reapplyButton = new JButton();
        libraryIdLabel = new JLabel();
        libraryIdTextField = new JTextField();
        JLabel obsNameLabel = new JLabel();
        obsTitle = new TextBoxWidget();
        JLabel obsIdLabel = new JLabel();
        obsId = new JLabel();
        JLabel priorityLabel = new JLabel();
        JPanel panel1 = new JPanel();
        priorityHigh = new JToggleButton();
        priorityMedium = new JToggleButton();
        priorityLow = new JToggleButton();
        tooCardPanel = new JPanel();
        JComponent goodiesFormsSeparator1 = compFactory.createSeparator("Status");

        JLabel phase2StatusLabel = new JLabel();
        phase2StatusBox = new SingleSelectComboBox<>();
        execStatusPanel = new JPanel();

        final JLabel schedulingBlockLabel = new JLabel();
        schedulingBlock = new ParallacticAngleControls(false);

        JLabel label3 = new JLabel();
        qaStateBox = new DropDownListBoxWidget<>();
        override = new JCheckBox();
        qaStateSum = new JLabel();
        JLabel dataflowStepLabel = new JLabel();
        dataflowStep = new JLabel();
        JComponent timeSeparator = compFactory.createSeparator("Observing Time");
        JLabel label1 = new JLabel();
        obsClass = new JLabel();
        JPanel panel3 = new JPanel();
        JPanel panel2 = new JPanel();
        plannedLabel = new JLabel();
        execLabel = new JLabel();
        execTime = new JLabel();
        piLabel = new JLabel();
        piTime = new JLabel();
        JScrollPane scrollPane1 = new JScrollPane();
        timeSummaryTable = new JTable();
        JComponent goodiesFormsSeparator2 = compFactory.createSeparator("Time Correction Log");
        JScrollPane correctionScrollPane = new JScrollPane();
        correctionTable = new JTable();
        JPanel correctionPanel = new JPanel();
        JLabel correctionLabel = new JLabel();
        timeCorrectionOp = new DropDownListBoxWidget<>();
        timeCorrection = new NumberBoxWidget();
        timeCorrectionUnits = new SingleSelectComboBox<>();
        correctionToFromLabel = new JLabel();
        chargeClass = new SingleSelectComboBox<>();
        JLabel commentLabel = new JLabel();
        comment = new TextBoxWidget();
        addCorrectionButton = new JButton();
        noTooButton = new JRadioButton();
        tooRadioButtonPanel = new JPanel();
        JLabel tooLabel = new JLabel();
        rapidTooButton = new JRadioButton();
        standardTooButton = new JRadioButton();
        tooLabelOnlyPanel = new JPanel();
        JLabel label2 = new JLabel();
        tooSinglePriorityLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setLayout(new FormLayout(
            new ColumnSpec[] {
                ColumnSpec.decode("right:max(min;64dlu)"),
                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                ColumnSpec.decode("max(pref;50dlu)"),
                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                new ColumnSpec(ColumnSpec.LEFT, Sizes.DEFAULT, FormSpec.NO_GROW),
                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                new ColumnSpec(ColumnSpec.LEFT, Sizes.DEFAULT, FormSpec.NO_GROW),
                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW)
            },
            new RowSpec[] {
                FormFactory.PARAGRAPH_GAP_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.MIN_ROWSPEC,
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
                FormFactory.PARAGRAPH_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.PARAGRAPH_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.PARAGRAPH_GAP_ROWSPEC
            }));

        int row = 3;

        //---- originatingTemplateLabel ----
        originatingTemplateLabel.setText("Source Template");
        add(originatingTemplateLabel, cc.xy(1, row));

        //---- originatingTemplate ----
        originatingTemplate.setText("text");
        originatingTemplate.setForeground(Color.gray);
        add(originatingTemplate, cc.xywh(3, row, 5, 1));

        //---- reapplyButton ----
        reapplyButton.setText("Reapply...");
        reapplyButton.setEnabled(false);
        add(reapplyButton, cc.xy(11, row));

        row += 2;

        //---- libraryIdLabel ----
        libraryIdLabel.setText("Library ID");
        add(libraryIdLabel, cc.xy(1, row));
        add(libraryIdTextField, cc.xywh(3, row, 9, 1));

        row += 2;

        //---- obsNameLabel ----
        obsNameLabel.setText("Observation Name ");
        add(obsNameLabel, cc.xy(1, row));
        add(obsTitle, cc.xywh(3, row, 9, 1));


        row += 2;

        //---- obsIdLabel ----
        obsIdLabel.setText("Observation Id ");
        add(obsIdLabel, cc.xy(1, row));

        //---- obsId ----
        obsId.setBorder(null);
        obsId.setText("unknown");
        add(obsId, cc.xywh(3, row, 9, 1));

        row += 2;

        //---- priorityLabel ----
        priorityLabel.setText("Priority ");
        add(priorityLabel, new CellConstraints(1, row, 1, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT, new Insets( 0, 0, 0, 2)));

        //======== panel1 ========
        {
            panel1.setLayout(new FormLayout(
                new ColumnSpec[] {
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.RELATED_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.RELATED_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.UNRELATED_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                },
                RowSpec.decodeSpecs("default")));

            //---- priorityHigh ----
            priorityHigh.setText("High");
            priorityHigh.setToolTipText("High priority observation");
            panel1.add(priorityHigh, cc.xy(1, 1));

            //---- priorityMedium ----
            priorityMedium.setText("Medium");
            priorityMedium.setToolTipText("Medium priority observation ");
            panel1.add(priorityMedium, cc.xy(3, 1));

            //---- priorityLow ----
            priorityLow.setText("Low");
            priorityLow.setToolTipText("Low priority observation");
            panel1.add(priorityLow, cc.xy(5, 1));

            //======== tooCardPanel ========
            {
                tooCardPanel.setLayout(new CardLayout());
            }
            panel1.add(tooCardPanel, cc.xywh(7, 1, 5, 1, CellConstraints.FILL, CellConstraints.FILL));
        }
        add(panel1, cc.xywh(3, row, 9, 1));

        row += 2;

        schedulingBlockLabel.setText("Scheduling ");
        add(schedulingBlockLabel, cc.xy(1, row));
        add(schedulingBlock.peer(), cc.xywh(3, row, 9, 1));

        row += 2;

        add(goodiesFormsSeparator1, cc.xywh(1, row, 11, 1));

        row += 2;

        //---- statusLabel ----
        phase2StatusLabel.setText("Phase 2 Status ");
        add(phase2StatusLabel, cc.xy(1, row));
        add(phase2StatusBox, cc.xy(3, row));
        add(execStatusPanel, cc.xywh(5, row, 5, 1, CellConstraints.LEFT, CellConstraints.FILL));

        row += 2;

        //---- statusLabel ----
//		statusLabel.setText("Observation Status ");
//		add(statusLabel, cc.xy(1, row));
//		add(statusBox, cc.xy(3, row));
//
//        row += 2;

        //---- label3 ----
        label3.setText("QA State ");
        add(label3, cc.xy(1, row));
        add(qaStateBox, cc.xy(3, row));

        //---- override ----
        override.setText("Override");
        add(override, cc.xy(5, row));

        //---- qaStateSum ----
        qaStateSum.setText("(No Data)");
        add(qaStateSum, cc.xy(7, row));

        row += 2;

        //---- dataflowStepLabel ----
        dataflowStepLabel.setText("Dataflow Step ");
        add(dataflowStepLabel, cc.xy(1, row));

        //---- dataflowStep ----
        dataflowStep.setText("text");
        add(dataflowStep, cc.xy(3, row));

        row += 2;

        add(timeSeparator, cc.xywh(1, row, 11, 1));

        row += 2;

        //---- label1 ----
        label1.setText(" Class ");
        add(label1, cc.xy(1, row));

        //---- obsClass ----
        obsClass.setBorder(null);
        obsClass.setText("unknown");
        add(obsClass, cc.xywh(3, row, 9, 1));

        row += 2;

        //======== panel3 ========
        {
            panel3.setLayout(new FormLayout(
                new ColumnSpec[] {
                    new ColumnSpec(ColumnSpec.CENTER, Sizes.dluX(40), FormSpec.NO_GROW),
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                },
                RowSpec.decodeSpecs("fill:66dlu")));

            //======== panel2 ========
            {
                panel2.setLayout(new FormLayout(
                    ColumnSpec.decodeSpecs("center:default"),
                    new RowSpec[] {
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.NARROW_LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.NARROW_LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.UNRELATED_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.NARROW_LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC
                    }));

                //---- plannedLabel ----
                plannedLabel.setText("Planned");
                panel2.add(plannedLabel, cc.xy(1, 1));

                //---- execLabel ----
                execLabel.setText("Exec");
                panel2.add(execLabel, cc.xy(1, 3));

                //---- execTime ----
                execTime.setText("00:00:00");
                execTime.setToolTipText("The total time planned for the execution of observation, in hours:min:sec");
                panel2.add(execTime, cc.xy(1, 5));

                //---- piLabel ----
                piLabel.setText("PI");
                panel2.add(piLabel, cc.xy(1, 7));

                //---- piTime ----
                piTime.setText("00:00:00");
                piTime.setToolTipText("The total time planned to be charged to the PI for this observation, in hours:minutes:seconds");
                panel2.add(piTime, cc.xy(1, 9));
            }
            panel3.add(panel2, cc.xy(1, 1));

            //======== scrollPane1 ========
            {
                scrollPane1.setRequestFocusEnabled(false);

                //---- timeSummaryTable ----
                timeSummaryTable.setBackground(Color.lightGray);
                scrollPane1.setViewportView(timeSummaryTable);
            }
            panel3.add(scrollPane1, cc.xy(3, 1));
        }
        add(panel3, cc.xywh(1, row, 11, 1));

        row += 2;

        add(goodiesFormsSeparator2, cc.xywh(1, row, 11, 1));

        row += 2;

        //======== correctionScrollPane ========
        {

            //---- correctionTable ----
            correctionTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            correctionTable.setBackground(Color.lightGray);
            correctionScrollPane.setViewportView(correctionTable);
        }
        add(correctionScrollPane, cc.xywh(1, row, 11, 1));

        row += 2;

        //======== correctionPanel ========
        {
            correctionPanel.setLayout(new FormLayout(
                new ColumnSpec[] {
                    ColumnSpec.decode("right:max(default;64dlu)"),
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    new ColumnSpec(Sizes.dluX(30)),
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    FormFactory.PREF_COLSPEC,
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                },
                new RowSpec[] {
                    FormFactory.DEFAULT_ROWSPEC,
                    FormFactory.LINE_GAP_ROWSPEC,
                    FormFactory.DEFAULT_ROWSPEC
                }));

            //---- correctionLabel ----
            correctionLabel.setText("Correction ");
            correctionPanel.add(correctionLabel, cc.xy(1, 1));

            //---- timeCorrectionOp ----
            timeCorrectionOp.setModel(new DefaultComboBoxModel<>(new String[] {
                "Subtract",
                "Add"
            }));
            correctionPanel.add(timeCorrectionOp, cc.xy(3, 1));

            //---- timeCorrection ----
            timeCorrection.setToolTipText("Additional time used for the observation, in hours:minutes:seconds ");
            correctionPanel.add(timeCorrection, cc.xy(5, 1));
            correctionPanel.add(timeCorrectionUnits, cc.xywh(7, 1, 1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));

            //---- correctionToFromLabel ----
            correctionToFromLabel.setText(" from ");
            correctionPanel.add(correctionToFromLabel, cc.xy(9, 1));
            correctionPanel.add(chargeClass, cc.xy(11, 1));

            //---- commentLabel ----
            commentLabel.setText("Comment ");
            correctionPanel.add(commentLabel, cc.xy(1, 3));

            //---- comment ----
            comment.setToolTipText("Reason for time correction");
            correctionPanel.add(comment, cc.xywh(3, 3, 9, 1));

            //---- addCorrectionButton ----
            addCorrectionButton.setText("Apply Correction");
            addCorrectionButton.setToolTipText("Add a new row to the table");
            correctionPanel.add(addCorrectionButton, cc.xy(13, 3));
        }
        add(correctionPanel, cc.xywh(1, row, 11, 1));

        //---- noTooButton ----
        noTooButton.setText("None");

        //======== tooRadioButtonPanel ========
        {
            tooRadioButtonPanel.setLayout(new GridBagLayout());
            ((GridBagLayout)tooRadioButtonPanel.getLayout()).columnWidths = new int[] {0, 0, 0, 0};
            ((GridBagLayout)tooRadioButtonPanel.getLayout()).rowHeights = new int[] {0, 0};
            ((GridBagLayout)tooRadioButtonPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};
            ((GridBagLayout)tooRadioButtonPanel.getLayout()).rowWeights = new double[] {1.0, 1.0E-4};

            //---- tooLabel ----
            tooLabel.setText("TOO Priority  ");
            tooRadioButtonPanel.add(tooLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));

            //---- rapidTooButton ----
            rapidTooButton.setText("Rapid");
            tooRadioButtonPanel.add(rapidTooButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 3), 0, 0));

            //---- standardTooButton ----
            standardTooButton.setText("Standard");
            tooRadioButtonPanel.add(standardTooButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
        }

        //======== tooLabelOnlyPanel ========
        {
            tooLabelOnlyPanel.setBorder(null);
            tooLabelOnlyPanel.setLayout(new GridBagLayout());
            ((GridBagLayout)tooLabelOnlyPanel.getLayout()).columnWidths = new int[] {0, 0, 0};
            ((GridBagLayout)tooLabelOnlyPanel.getLayout()).rowHeights = new int[] {0, 0};
            ((GridBagLayout)tooLabelOnlyPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
            ((GridBagLayout)tooLabelOnlyPanel.getLayout()).rowWeights = new double[] {1.0, 1.0E-4};

            //---- label2 ----
            label2.setText("TOO Priority ");
            tooLabelOnlyPanel.add(label2, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 6), 0, 0));

            //---- tooSinglePriorityLabel ----
            tooSinglePriorityLabel.setText("Standard");
            tooLabelOnlyPanel.add(tooSinglePriorityLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    JLabel originatingTemplateLabel;
    JLabel originatingTemplate;
    JButton reapplyButton;
    JLabel libraryIdLabel;
    JTextField libraryIdTextField;
    TextBoxWidget obsTitle;
    JLabel obsId;
    JToggleButton priorityHigh;
    JToggleButton priorityMedium;
    JToggleButton priorityLow;
    JPanel tooCardPanel;
    SingleSelectComboBox<ObsPhase2Status> phase2StatusBox;
    JPanel execStatusPanel;
    DropDownListBoxWidget<ObsQaState> qaStateBox;
    JCheckBox override;
    JLabel qaStateSum;
    JLabel dataflowStep;
    JLabel obsClass;
    JLabel plannedLabel;
    JLabel execLabel;
    JLabel execTime;
    JLabel piLabel;
    JLabel piTime;
    JTable timeSummaryTable;
    JTable correctionTable;
    DropDownListBoxWidget<String> timeCorrectionOp;
    NumberBoxWidget timeCorrection;
    SingleSelectComboBox<String> timeCorrectionUnits;
    JLabel correctionToFromLabel;
    SingleSelectComboBox<String> chargeClass;
    TextBoxWidget comment;
    JButton addCorrectionButton;
    JRadioButton noTooButton;
    JPanel tooRadioButtonPanel;
    JRadioButton rapidTooButton;
    JRadioButton standardTooButton;
    JPanel tooLabelOnlyPanel;
    JLabel tooSinglePriorityLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    ParallacticAngleControls schedulingBlock;

}
