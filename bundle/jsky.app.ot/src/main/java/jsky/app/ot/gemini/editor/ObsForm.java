package jsky.app.ot.gemini.editor;

import com.jgoodies.forms.factories.DefaultComponentFactory;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import edu.gemini.spModel.obs.ObsPhase2Status;
import edu.gemini.spModel.obs.ObsQaState;
import edu.gemini.spModel.obs.plannedtime.SetupTime;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.NumberBoxWidget;
import jsky.util.gui.SingleSelectComboBox;
import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
import java.awt.*;

public class ObsForm extends JPanel {
    public ObsForm() {
        final DefaultComponentFactory compFactory = DefaultComponentFactory.getInstance();
        final CellConstraints cc = new CellConstraints();

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

        // Row counter for components.
        int row = 3;

        // --- Source Template Controls ---
        originatingTemplateLabel = new JLabel("Source Template");
        add(originatingTemplateLabel, cc.xy(1, row));

        originatingTemplate = new JLabel();
        originatingTemplate.setForeground(Color.gray);
        add(originatingTemplate, cc.xywh(3, row, 5, 1));

        reapplyButton = new JButton("Reapply...");
        reapplyButton.setEnabled(false);
        add(reapplyButton, cc.xy(11, row));

        row += 2;

        // --- Library ID ---
        libraryIdLabel = new JLabel("Library ID");
        add(libraryIdLabel, cc.xy(1, row));

        libraryIdTextField = new JTextField();
        add(libraryIdTextField, cc.xywh(3, row, 9, 1));

        row += 2;

        // --- Observation Name ---
        add(new JLabel("Observation Name "), cc.xy(1, row));

        obsTitle = new TextBoxWidget();
        add(obsTitle, cc.xywh(3, row, 9, 1));

        row += 2;

        // --- Observation ID ---
        add(new JLabel("Observation Id "), cc.xy(1, row));

        obsId = new JLabel("unknown");
        obsId.setBorder(null);
        add(obsId, cc.xywh(3, row, 9, 1));

        row += 2;

        // --- Priority Controls ---
        add(new JLabel("Priority "), new CellConstraints(1, row, 1, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT, new Insets( 0, 0, 0, 2)));

        final JPanel priorityPanel = new JPanel(new FormLayout(
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
                RowSpec.decodeSpecs("default"))
        );

        final ButtonGroup bg = new ButtonGroup();
        priorityHigh = new JToggleButton("High");
        priorityHigh.setToolTipText("High priority observation");
        priorityPanel.add(priorityHigh, cc.xy(1, 1));
        bg.add(priorityHigh);

        priorityMedium = new JToggleButton("Medium");
        priorityMedium.setToolTipText("Medium priority observation ");
        priorityPanel.add(priorityMedium, cc.xy(3, 1));
        bg.add(priorityMedium);

        priorityLow = new JToggleButton("Low");
        priorityLow.setToolTipText("Low priority observation");
        priorityPanel.add(priorityLow, cc.xy(5, 1));
        bg.add(priorityLow);

        // --- TOO Controls ---
        tooCardPanel = new JPanel(new CardLayout());
        priorityPanel.add(tooCardPanel, cc.xywh(7, 1, 5, 1, CellConstraints.FILL, CellConstraints.FILL));
        add(priorityPanel, cc.xywh(3, row, 9, 1));

        row += 2;

        // --- Status Controls ---
        add(compFactory.createSeparator("Status"), cc.xywh(1, row, 11, 1));

        row += 2;

        add(new JLabel("Phase 2 Status "), cc.xy(1, row));
        phase2StatusBox = new SingleSelectComboBox<>();
        add(phase2StatusBox, cc.xy(3, row));

        execStatusPanel = new JPanel();
        add(execStatusPanel, cc.xywh(5, row, 5, 1, CellConstraints.LEFT, CellConstraints.FILL));

        row += 2;

        // --- QA State ---
        add(new JLabel("QA State "), cc.xy(1, row));
        qaStateBox = new DropDownListBoxWidget<>();
        add(qaStateBox, cc.xy(3, row));

        // --- Override ---
        override = new JCheckBox("Override");
        add(override, cc.xy(5, row));

        // --- QA State Sum ---
        qaStateSum = new JLabel("(No Data)");
        add(qaStateSum, cc.xy(7, row));

        row += 2;

        // --- Dataflow Step ---
        add(new JLabel("Dataflow Step "), cc.xy(1, row));

        dataflowStep = new JLabel();
        add(dataflowStep, cc.xy(3, row));

        row += 2;

        // --- Observing Time Information ---
        add(compFactory.createSeparator("Observing Time"), cc.xywh(1, row, 11, 1));

        row += 2;

        add(new JLabel("Class"), cc.xy(1, row));

        obsClass = new JLabel("unknown");
        obsClass.setBorder(null);
        add(obsClass, cc.xy(3, row));

        final Box setupPanel = new Box(BoxLayout.X_AXIS);
        setupPanel.add(new JLabel("Setup Type"));
        setupPanel.add(Box.createHorizontalStrut(5));
        setupTypeBox = new JComboBox<>(SetupTime.Type.values());
        setupPanel.add(setupTypeBox);
        setupPanel.add(Box.createHorizontalGlue());
        add(setupPanel, cc.xywh(5, row, 7, 1));

        row += 2;

        final JPanel obsPanel = new JPanel(new FormLayout(
                new ColumnSpec[] {
                        new ColumnSpec(ColumnSpec.CENTER, Sizes.dluX(40), FormSpec.NO_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                },
                RowSpec.decodeSpecs("fill:66dlu"))
        );

        final JPanel obsSubPanel = new JPanel(new FormLayout(
                ColumnSpec.decodeSpecs("center:default"),
                new RowSpec[]{
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.NARROW_LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.NARROW_LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.UNRELATED_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.NARROW_LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC
                })
        );

        plannedLabel = new JLabel("Planned");
        obsSubPanel.add(plannedLabel, cc.xy(1, 1));

        execLabel = new JLabel("Exec");
        obsSubPanel.add(execLabel, cc.xy(1, 3));

        execTime = new JLabel("00:00:00");
        execTime.setToolTipText("The total time planned for the execution of observation, in hours:min:sec");
        obsSubPanel.add(execTime, cc.xy(1, 5));

        piLabel = new JLabel("PI");
        obsSubPanel.add(piLabel, cc.xy(1, 7));

        piTime = new JLabel("00:00:00");
        piTime.setToolTipText("The total time planned to be charged to the PI for this observation, in hours:minutes:seconds");
        obsSubPanel.add(piTime, cc.xy(1, 9));


        timeSummaryTable = new JTable();
        timeSummaryTable.setBackground(Color.lightGray);

        JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setRequestFocusEnabled(false);
        scrollPane1.setViewportView(timeSummaryTable);

        obsPanel.add(obsSubPanel, cc.xy(1, 1));
        obsPanel.add(scrollPane1, cc.xy(3, 1));
        add(obsPanel, cc.xywh(1, row, 11, 1));

        row += 2;

        // --- Time Correction Log ---
        add(compFactory.createSeparator("Time Correction Log"), cc.xywh(1, row, 11, 1));

        row += 2;

        correctionTable = new JTable();
        correctionTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        correctionTable.setBackground(Color.lightGray);

        final JScrollPane correctionScrollPane = new JScrollPane();
        correctionScrollPane.setViewportView(correctionTable);
        add(correctionScrollPane, cc.xywh(1, row, 11, 1));

        row += 2;

        final JPanel correctionPanel = new JPanel(new FormLayout(
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
                })
        );

        correctionPanel.add(new JLabel("Correction "), cc.xy(1, 1));
        timeCorrectionOp = new DropDownListBoxWidget<>();
        timeCorrectionOp.setModel(new DefaultComboBoxModel<>(new String[] {
                "Subtract",
                "Add"
        }));
        correctionPanel.add(timeCorrectionOp, cc.xy(3, 1));

        timeCorrection = new NumberBoxWidget();
        timeCorrection.setToolTipText("Additional time used for the observation, in hours:minutes:seconds ");
        correctionPanel.add(timeCorrection, cc.xy(5, 1));

        timeCorrectionUnits = new SingleSelectComboBox<>();
        correctionPanel.add(timeCorrectionUnits, cc.xywh(7, 1, 1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));

        correctionToFromLabel = new JLabel(" from ");
        correctionPanel.add(correctionToFromLabel, cc.xy(9, 1));

        chargeClass = new SingleSelectComboBox<>();
        correctionPanel.add(chargeClass, cc.xy(11, 1));

        correctionPanel.add(new JLabel("Comment "), cc.xy(1, 3));
        comment = new TextBoxWidget();
        comment.setToolTipText("Reason for time correction");
        correctionPanel.add(comment, cc.xywh(3, 3, 9, 1));

        addCorrectionButton = new JButton("Apply Correction");
        addCorrectionButton.setToolTipText("Add a new row to the table");
        correctionPanel.add(addCorrectionButton, cc.xy(13, 3));

        add(correctionPanel, cc.xywh(1, row, 11, 1));

        // --- TOO Controls ---
        final GridBagLayout tooRadioButtonLayout = new GridBagLayout();
        tooRadioButtonLayout.columnWidths  = new int[] {0, 0, 0, 0};
        tooRadioButtonLayout.rowHeights    = new int[] {0, 0};
        tooRadioButtonLayout.columnWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};
        tooRadioButtonLayout.rowWeights    = new double[] {1.0, 1.0E-4};
        tooRadioButtonPanel = new JPanel(tooRadioButtonLayout);

        tooRadioButtonPanel.add(new JLabel("TOO Priority  "), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));

        final ButtonGroup tooButtons = new ButtonGroup();
        rapidTooButton = new JRadioButton("Rapid");
        tooRadioButtonPanel.add(rapidTooButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 3), 0, 0));
        tooButtons.add(rapidTooButton);

        standardTooButton = new JRadioButton("Standard");
        tooRadioButtonPanel.add(standardTooButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
        tooButtons.add(standardTooButton);

        noTooButton = new JRadioButton("None");
        tooButtons.add(noTooButton);


        final GridBagLayout tooLabelOnlyLayout = new GridBagLayout();
        tooLabelOnlyLayout.columnWidths  = new int[] {0, 0, 0};
        tooLabelOnlyLayout.rowHeights    = new int[] {0, 0};
        tooLabelOnlyLayout.columnWeights = new double[] {0.0, 0.0, 1.0E-4};
        tooLabelOnlyLayout.rowWeights    = new double[] {1.0, 1.0E-4};
        tooLabelOnlyPanel = new JPanel(tooLabelOnlyLayout);
        tooLabelOnlyPanel.setBorder(null);

        tooLabelOnlyPanel.add(new JLabel("TOO Priority "), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 6), 0, 0));

        tooSinglePriorityLabel = new JLabel("Standard");
        tooLabelOnlyPanel.add(tooSinglePriorityLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
    }

    final JLabel originatingTemplateLabel;
    final JLabel originatingTemplate;
    final JButton reapplyButton;
    final JLabel libraryIdLabel;
    final JTextField libraryIdTextField;
    final TextBoxWidget obsTitle;
    final JLabel obsId;
    final JToggleButton priorityHigh;
    final JToggleButton priorityMedium;
    final JToggleButton priorityLow;
    final JPanel tooCardPanel;
    final SingleSelectComboBox<ObsPhase2Status> phase2StatusBox;
    final JPanel execStatusPanel;
    final DropDownListBoxWidget<ObsQaState> qaStateBox;
    final JCheckBox override;
    final JLabel qaStateSum;
    final JLabel dataflowStep;
    final JLabel obsClass;
    final JComboBox<SetupTime.Type> setupTypeBox;
    final JLabel plannedLabel;
    final JLabel execLabel;
    final JLabel execTime;
    final JLabel piLabel;
    final JLabel piTime;
    final JTable timeSummaryTable;
    final JTable correctionTable;
    final DropDownListBoxWidget<String> timeCorrectionOp;
    final NumberBoxWidget timeCorrection;
    final SingleSelectComboBox<String> timeCorrectionUnits;
    final JLabel correctionToFromLabel;
    final SingleSelectComboBox<String> chargeClass;
    final TextBoxWidget comment;
    final JButton addCorrectionButton;
    final JRadioButton noTooButton;
    final JPanel tooRadioButtonPanel;
    final JRadioButton rapidTooButton;
    final JRadioButton standardTooButton;
    final JPanel tooLabelOnlyPanel;
    final JLabel tooSinglePriorityLabel;
}
