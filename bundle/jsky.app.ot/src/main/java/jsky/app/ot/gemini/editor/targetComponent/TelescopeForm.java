package jsky.app.ot.gemini.editor.targetComponent;

import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;

class TelescopeForm extends JPanel {

    final GuidingControls guidingControls = new GuidingControls();

    final JMenu newMenu = new JMenu() {{
        setToolTipText("Create a new target or guide group");
        setMargin(new Insets(2, 2, 2, 2));
        setText("New");
        setFocusable(false);
    }};

    final JMenuBar newMenuBar = new JMenuBar() {{
        add(newMenu);
    }};

    final JButton removeButton = new JButton() {{
        setToolTipText("Remove the selected target");
        setMargin(new Insets(2, 2, 2, 2));
        setText("Remove");
        setFocusable(false);
    }};

    final JButton copyButton = new JButton() {{
        setText("Copy");
        setFocusable(false);
        setToolTipText("Copy selected target coordinates");
    }};

    final JButton pasteButton = new JButton() {{
        setText("Paste");
        setFocusable(false);
        setToolTipText("Paste coordinates on selected target");
    }};

    final JButton duplicateButton = new JButton() {{
        setText("Duplicate");
        setFocusable(false);
        setToolTipText("Duplicate selected target");
    }};

    final JButton primaryButton = new JButton() {{
        setText("Primary");
        setToolTipText("Set/unset as active guide star");
    }};

    final JComboBox<PositionType> tag = new JComboBox<PositionType>() {{
        setToolTipText("Target Type");
    }};

    final TextBoxWidget guideGroupName = new TextBoxWidget() {{
        setToolTipText("Guide Group name (optional)");
        setHorizontalAlignment(JTextField.LEFT);
        setColumns(20);
    }};

    final JPanel guideGroupPanel = new JPanel() {{
        final JLabel guideGroupNameLabel = new JLabel() {{
            setLabelFor(null);
            setText("Guide Group Name");
        }};
        setLayout(new GridBagLayout());
        add(guideGroupNameLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 5), 0, 0));
        add(guideGroupName, new GridBagConstraints(2, 0, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        add(new JPanel(), new GridBagConstraints(1, 1, 3, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(233, 0, 0, 0), 0, 0));
        setVisible(false);
    }};

    final JPanel buttonPanel = new JPanel() {{
        setBackground(new Color(238, 238, 238));
        setBorder(new EmptyBorder(2, 2, 2, 2));
        setLayout(new GridBagLayout() {{
            columnWidths  = new int[]{ 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            rowHeights    = new int[]{ 0, 0 };
            columnWeights = new double[]{ 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4 };
            rowWeights    = new double[]{ 0.0, 1.0E-4 };
        }});
        final JPanel spacerPanel = new JPanel() {{
            setBackground(null);
            setOpaque(false);
            setLayout(new BorderLayout());
        }};
        add(newMenuBar,             new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,  0, 0, 0), 0, 0));
        add(removeButton,           new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,  0, 0, 0), 0, 0));
        add(copyButton,             new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 10, 0, 0), 0, 0));
        add(pasteButton,            new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,  0, 0, 0), 0, 0));
        add(duplicateButton,        new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,  0, 0, 0), 0, 0));
        add(primaryButton,          new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 10, 0, 0), 0, 0));
        add(spacerPanel,            new GridBagConstraints(6, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,  0, 0, 0), 0, 0));
        add(guidingControls.peer(), new GridBagConstraints(7, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,  0, 0, 0), 0, 0));
    }};

    final TelescopePosTableWidget positionTable;

    public TelescopeForm(EdCompTargetList owner) {

        positionTable = new TelescopePosTableWidget(owner);

        final JPanel targetListPanel = new JPanel() {{
            setBorder(new BevelBorder(BevelBorder.RAISED));
            setLayout(new BorderLayout());
            final JPanel feedbackAndButtonPanel = new JPanel(new BorderLayout()) {{
                add(buttonPanel, BorderLayout.SOUTH);
            }};
            add(feedbackAndButtonPanel, BorderLayout.SOUTH);
            final JScrollPane posTableScrollPane = new JScrollPane() {{
                setViewportView(positionTable);
            }};
            add(posTableScrollPane, BorderLayout.CENTER);
        }};

        setBorder(new EmptyBorder(5, 5, 5, 5));
        setLayout(new GridBagLayout());
        add(targetListPanel, new GridBagConstraints() {{
            gridx = 0;
            gridy = 0;
            fill = GridBagConstraints.BOTH;
            weighty = 1000;
            weightx = 1000;
        }});

    }

}
