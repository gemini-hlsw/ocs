package jsky.app.ot.gemini.editor.targetComponent;

import com.jgoodies.forms.layout.*;
import jsky.app.ot.ags.AgsSelectorControl;
import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;

class TelescopeForm extends JPanel {

    private final EdCompTargetList owner;

    public TelescopeForm(EdCompTargetList owner) {
        this.owner = owner;
        initComponents();
    }
    final JPanel buttonPanel = new JPanel();
    private void initComponents() {
        final JPanel targetListPanel = new JPanel();
        newMenuBar = new JMenuBar();
        newMenu = new JMenu();
        removeButton = new JButton();
        copyButton = new JButton();
        pasteButton = new JButton();
        duplicateButton = new JButton();
        primaryButton = new JButton();
        final JPanel spacerPanel = new JPanel();
        final JScrollPane posTableScrollPane = new JScrollPane();
        positionTable = new TelescopePosTableWidget(owner);
        guideGroupPanel = new JPanel();
        tag = new JComboBox();
        final JLabel guideGroupNameLabel = new JLabel();
        guideGroupName = new TextBoxWidget();
        setBaseButton = new JButton();
        final CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setLayout(new GridBagLayout());

        //======== targetListPanel ========
        {
            targetListPanel.setBorder(new BevelBorder(BevelBorder.RAISED));
            targetListPanel.setLayout(new BorderLayout());

            //======== buttonPanel ========
            {
                buttonPanel.setBackground(new Color(238, 238, 238));
                buttonPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
                buttonPanel.setLayout(new GridBagLayout());
                ((GridBagLayout) buttonPanel.getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0};
                ((GridBagLayout) buttonPanel.getLayout()).rowHeights = new int[] {0, 0};
                ((GridBagLayout) buttonPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
                ((GridBagLayout) buttonPanel.getLayout()).rowWeights = new double[] {0.0, 1.0E-4};

                //---- newButton ----
                newMenu.setToolTipText("Create a new target or guide group");
                newMenu.setMargin(new Insets(2, 2, 2, 2));
                newMenu.setText("New");
                newMenu.setFocusable(false);
                newMenuBar.add(newMenu);
                buttonPanel.add(newMenuBar, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));

                //---- removeButton ----
                removeButton.setToolTipText("Remove the selected target");
                removeButton.setMargin(new Insets(2, 2, 2, 2));
                removeButton.setText("Remove");
                removeButton.setFocusable(false);
                buttonPanel.add(removeButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));

                //---- copyButton ----
                copyButton.setText("Copy");
                copyButton.setFocusable(false);
                copyButton.setToolTipText("Copy selected target coordinates");
                buttonPanel.add(copyButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 10, 0, 0), 0, 0));

                //---- pasteButton ----
                pasteButton.setText("Paste");
                pasteButton.setFocusable(false);
                pasteButton.setToolTipText("Paste coordinates on selected target");
                buttonPanel.add(pasteButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));

                //---- duplicateButton ----
                duplicateButton.setText("Duplicate");
                duplicateButton.setFocusable(false);
                duplicateButton.setToolTipText("Duplicate selected target");
                buttonPanel.add(duplicateButton, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));

                //---- primaryButton ----
                primaryButton.setText("Primary");
                primaryButton.setToolTipText("Set/unset as active guide star");
                buttonPanel.add(primaryButton, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 10, 0, 0), 0, 0));

                //======== spacerPanel ========
                {
                    spacerPanel.setBackground(null);
                    spacerPanel.setOpaque(false);
                    spacerPanel.setLayout(new BorderLayout());
                }
                buttonPanel.add(spacerPanel, new GridBagConstraints(6, 0, 1, 1, 1.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));

                guidingControls = new GuidingControls();
                buttonPanel.add(guidingControls.peer(), new GridBagConstraints(7, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));
            }

            final JPanel feedbackAndButtonPanel = new JPanel(new BorderLayout());
            feedbackAndButtonPanel.add(buttonPanel, BorderLayout.SOUTH);

            targetListPanel.add(feedbackAndButtonPanel, BorderLayout.SOUTH);

            //======== posTableScrollPane ========
            {
                posTableScrollPane.setViewportView(positionTable);
            }
            targetListPanel.add(posTableScrollPane, BorderLayout.CENTER);
        }
        add(targetListPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 0), 0, 0));

        //======== coordinatesPanel ========
        {

            tag.setToolTipText("Target Type");

            //======== guideGroupPanel ========
            {
                guideGroupPanel.setLayout(new GridBagLayout());

                //---- guideGroupNameLabel ----
                guideGroupNameLabel.setLabelFor(null);
                guideGroupNameLabel.setText("Guide Group Name");
                guideGroupPanel.add(guideGroupNameLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(0, 10, 0, 5), 0, 0));

                //---- guideGroupName ----
                guideGroupName.setToolTipText("Guide Group name (optional)");
                guideGroupName.setHorizontalAlignment(JTextField.LEFT);
                guideGroupName.setColumns(20);
                guideGroupPanel.add(guideGroupName, new GridBagConstraints(2, 0, 2, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));

                // fill
                guideGroupPanel.add(new JPanel(), new GridBagConstraints(1, 1, 3, 1, 1.0, 1.0,
                    GridBagConstraints.NORTH, GridBagConstraints.NONE,
                    new Insets(233, 0, 0, 0), 0, 0));
            }

            guideGroupPanel.setVisible(false);

        }

        //======== guidingPanel ========

        autoGuideStarButton         = guidingControls.autoGuideStarButton().peer();
        manualGuideStarButton       = guidingControls.manualGuideStarButton().peer();
        autoGuideStarGuiderSelector = guidingControls.autoGuideStarGuiderSelector();

    }

    JMenuBar newMenuBar;
    JMenu newMenu;
    JButton removeButton;
    JButton copyButton;
    JButton pasteButton;
    JButton duplicateButton;
    JButton primaryButton;
    TelescopePosTableWidget positionTable;
    JPanel guideGroupPanel;
    JComboBox<PositionType> tag;
    TextBoxWidget guideGroupName;
    JButton setBaseButton;

    // Components for the Guider panel.
    GuidingControls guidingControls;

    // These are just convenient placeholders to simplify the interactions with EdCompTargetList and are
    // set to the peers in the guidingPanel.
    JButton autoGuideStarButton;
    JButton manualGuideStarButton;
    AgsSelectorControl autoGuideStarGuiderSelector;
}
