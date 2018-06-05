package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.shared.gui.ButtonFlattener;
import jsky.app.ot.gemini.editor.targetComponent.details2.SPCoordinatesEditorPanel;
import jsky.app.ot.gemini.editor.targetComponent.details2.TargetDetailPanel;
import jsky.app.ot.gemini.parallacticangle.ParallacticAngleControls;
import jsky.util.gui.Resources;
import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class TelescopeForm extends JPanel {

    final GuidingControls guidingControls = new GuidingControls();
    final TargetDetailPanel detailEditor = new TargetDetailPanel();
    final SPCoordinatesEditorPanel coordinateEditor = new SPCoordinatesEditorPanel();
    final ParallacticAngleControls schedulingBlock = new ParallacticAngleControls(false);

    final JMenu newMenu = new JMenu() {{
        setToolTipText("Create a new target or guide group");
        setMargin(new Insets(2, 2, 2, 2));
        setText("New");
        setFocusable(false);
        setIcon(Resources.getIcon("eclipse/add_menu.gif"));
        setText("");
        addMouseListener(new MouseAdapter() {
            final Icon icon = Resources.getIcon("eclipse/add_menu.gif");

            public void mouseEntered(MouseEvent e) {
                newMenu.setIcon(newMenu.getRolloverIcon());
            }

            public void mouseExited(MouseEvent e) {
                newMenu.setIcon(icon);
            }
        });
        ButtonFlattener.flatten(this);
    }};

    final JMenuBar newMenuBar = new JMenuBar() {{
        setBorder(BorderFactory.createEmptyBorder());
        setOpaque(false);
        add(newMenu);
    }};

    final JButton removeButton = new JButton() {{
        setToolTipText("Remove the selected group or target");
        setMargin(new Insets(2, 2, 2, 2));
        setFocusable(false);
        setIcon(Resources.getIcon("eclipse/remove.gif"));
        ButtonFlattener.flatten(this);
    }};

    final JButton copyButton = new JButton() {{
        setFocusable(false);
        setToolTipText("Copy selected group contents or target coordinates");
        setIcon(Resources.getIcon("eclipse/copy.gif"));
        ButtonFlattener.flatten(this);
    }};

    final JButton pasteButton = new JButton() {{
        setFocusable(false);
        setToolTipText("Paste targets in selected group or coordinates in selected target");
        setIcon(Resources.getIcon("eclipse/paste.gif"));
        ButtonFlattener.flatten(this);
    }};

    final JButton duplicateButton = new JButton() {{
        setFocusable(false);
        setToolTipText("Duplicate selected group or target");
        setIcon(Resources.getIcon("eclipse/duplicate.gif"));
        ButtonFlattener.flatten(this);
    }};

    final JButton primaryButton = new JButton() {{
        setToolTipText("Set/unset as active group or guide star");
        setIcon(Resources.getIcon("eclipse/radiobuttons.gif"));
        ButtonFlattener.flatten(this);
    }};

    final JComboBox<PositionType> tag = new JComboBox<PositionType>() {{
        setToolTipText("Target Type");
    }};

    final JCheckBox linkBaseToTarget = new JCheckBox("Link base to target") {{
        setToolTipText("Link the base position to the science target(s)");
        setOpaque(false);
    }};

    final TextBoxWidget guideGroupName = new TextBoxWidget() {{
        setToolTipText("Guide Group name (optional)");
        setHorizontalAlignment(JTextField.LEFT);
        setColumns(20);
        setMinimumSize(getPreferredSize());
    }};

    final JPanel guideGroupPanel = new JPanel() {{
        final JLabel guideGroupNameLabel = new JLabel() {{
            setLabelFor(null);
            setText("Guide Group Name");
        }};
        setLayout(new GridBagLayout());
        add(guideGroupNameLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,  GridBagConstraints.NONE, new Insets(  0, 10, 0, 5), 0, 0));
        add(guideGroupName,      new GridBagConstraints(2, 0, 2, 1, 0.0, 0.0, GridBagConstraints.WEST,  GridBagConstraints.NONE, new Insets(  0,  0, 0, 0), 0, 0));
        add(new JPanel(),        new GridBagConstraints(1, 1, 3, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(233,  0, 0, 0), 0, 0));
        setVisible(false);
    }};

    final JPanel buttonPanel = new JPanel() {{
        setBackground(new Color(238, 238, 238));
        setBorder(new EmptyBorder(2, 2, 2, 2));
        setLayout(new GridBagLayout());
        final JPanel leftSpacerPanel = new JPanel() {{
            setBackground(null);
            setOpaque(false);
            setLayout(new BorderLayout());
        }};
        final JPanel rightSpacerPanel = new JPanel() {{
            setBackground(null);
            setOpaque(false);
            setLayout(new BorderLayout());
        }};

        add(newMenuBar,             new GridBagConstraints( 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,  0, 0, 0), 0, 0));
        add(removeButton,           new GridBagConstraints( 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,  0, 0, 0), 0, 0));
        add(copyButton,             new GridBagConstraints( 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
        add(pasteButton,            new GridBagConstraints( 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,  0, 0, 0), 0, 0));
        add(duplicateButton,        new GridBagConstraints( 4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,  0, 0, 0), 0, 0));
        add(primaryButton,          new GridBagConstraints( 5, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
        add(tag,                    new GridBagConstraints( 6, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
        add(leftSpacerPanel,        new GridBagConstraints( 7, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,  0, 0, 0), 0, 0));
        add(linkBaseToTarget,       new GridBagConstraints( 8, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,  0, 0, 0), 0, 0));
        add(rightSpacerPanel,       new GridBagConstraints( 9, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,  0, 0, 0), 0, 0));
        add(guidingControls.peer(), new GridBagConstraints(10, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,  0, 0, 0), 0, 0));
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
                getViewport().setBackground(Color.WHITE);
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

        final JPanel schedulingPanel = new JPanel() {{
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(0, 2, 0, 2),
                    BorderFactory.createTitledBorder("Scheduling")
            ));
            setLayout(new BorderLayout());
            schedulingBlock.peer().setBorder(
                    BorderFactory.createEmptyBorder(0, 5, 0, 0)
            );
            add(schedulingBlock.peer(), BorderLayout.WEST);
        }};

        add(schedulingPanel, new GridBagConstraints() {{
            gridx = 0;
            gridy = 1;
            fill = HORIZONTAL;
            insets = new Insets(5, 0, 5, 0);
        }});

        add(detailEditor, new GridBagConstraints() {{
            gridx = 0;
            gridy = 2;
            fill = HORIZONTAL;
            insets = new Insets(5, 0, 5, 0);
        }});

        add(coordinateEditor, new GridBagConstraints() {{
            gridx = 0;
            gridy = 3;
            fill = HORIZONTAL;
            insets = new Insets(5, 0, 5, 0);
        }});
        add(guideGroupPanel, new GridBagConstraints() {{
            gridx = 0;
            gridy = 4;
            fill = HORIZONTAL;
            insets = new Insets(5, 0, 5, 0);
        }});

    }

}
