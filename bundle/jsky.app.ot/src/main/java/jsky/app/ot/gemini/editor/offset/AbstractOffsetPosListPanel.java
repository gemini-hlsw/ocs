package jsky.app.ot.gemini.editor.offset;

import javax.swing.*;
import java.awt.*;


/**
 * Base class for offset position editors.
 */
public abstract class AbstractOffsetPosListPanel extends JPanel {
    private JTextField titleTextField;
    private OffsetPosTablePanel posTablePanel;
    private OffsetPosUI posUI;

    public AbstractOffsetPosListPanel(OffsetPosUI posUI) {
        super(new GridBagLayout());
        this.posUI = posUI;

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor  = GridBagConstraints.WEST;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.gridx   = 0;
        gbc.gridy   = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets  = new Insets(0, 0, 10, 0);
        add(makeTitlePanel(), gbc);

        gbc.anchor  = GridBagConstraints.CENTER;
        gbc.fill    = GridBagConstraints.BOTH;
        gbc.gridx   = 0;
        gbc.gridy   = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets  = new Insets(0, 0, 0, 0);
        posTablePanel = new OffsetPosTablePanel();
        add(posTablePanel, gbc);

        gbc.anchor  = GridBagConstraints.CENTER;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.gridx   = 0;
        gbc.gridy   = 2;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets  = new Insets(10, 0, 0, 0);
        add(posUI.getPanel(), gbc);
    }

    protected JPanel makeTitlePanel() {
        JPanel pan = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel titleLabel = new JLabel("Title");
        gbc.anchor  = GridBagConstraints.EAST;
        gbc.fill    = GridBagConstraints.NONE;
        gbc.gridx   = 0;
        gbc.gridy   = 0;
        gbc.weightx = 0;
        gbc.insets  = new Insets(0, 0, 0, 10);
        pan.add(titleLabel, gbc);

        titleTextField = new JTextField();
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.gridx   = 1;
        gbc.gridy   = 0;
        gbc.weightx = 1;
        gbc.insets  = new Insets(0, 0, 0, 0);
        pan.add(titleTextField, gbc);

        return pan;
    }

    public OffsetPosUI getOffsetPosEditorUI() {
        return posUI;
    }

    public OffsetPosTablePanel getOffsetPosTablePanel() {
        return posTablePanel;
    }

    public JTextField getTitleTextField() {
        return titleTextField;
    }
}
