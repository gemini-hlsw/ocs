package jsky.app.ot.gemini.editor.offset;

import edu.gemini.spModel.guide.GuideProbe;
import jsky.util.gui.NumberBoxWidget;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * Widgets for editing standard {@link edu.gemini.spModel.target.offset.OffsetPos offset positions}.
 */
public final class StandardOffsetPosPanel extends JPanel implements OffsetPosUI {
    private static final int COLUMN_SEP = 15;
    private static final int ROW_SEP    =  6;
    private static final int LABEL_SEP  =  5;

    protected JLabel orientationLabel;

    private NumberBoxWidget pOffset;
    private NumberBoxWidget qOffset;

    private OffsetPosGuiderPanel guidersPanel;

    public StandardOffsetPosPanel() {
        super(new GridBagLayout());

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Selected Position (s)"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));


        // Orientation label ---------------------------------------------------
        int col = 0;

        GridBagConstraints gbc = new GridBagConstraints();
        orientationLabel = new JLabel();
        gbc.anchor     = GridBagConstraints.CENTER;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridx      = col;
        gbc.gridy      = 0;
        gbc.gridheight = 2;
        gbc.gridwidth  = 1;
        gbc.weightx    = 0;
        gbc.weighty    = 0;
        gbc.insets  = new Insets(0, 0, 0, COLUMN_SEP);
        add(orientationLabel, gbc);

        // p, q editors -----------------------------------------------------
        ++col;

        JLabel lab = new JLabel("p");
        gbc.anchor     = GridBagConstraints.EAST;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridx      = col;
        gbc.gridy      = 0;
        gbc.gridheight = 1;
        gbc.gridwidth  = 1;
        gbc.weightx    = 0;
        gbc.weighty    = 0;
        gbc.insets  = new Insets(0, 0, ROW_SEP, LABEL_SEP);
        add(lab, gbc);

        pOffset = new NumberBoxWidget();
        pOffset.setColumns(7);
        pOffset.setMinimumSize(new Dimension(80, 20));
        gbc.anchor     = GridBagConstraints.WEST;
        gbc.fill       = GridBagConstraints.BOTH;
        gbc.gridx      = col+1;
        gbc.gridy      = 0;
        gbc.gridheight = 1;
        gbc.gridwidth  = 1;
        gbc.weightx    = 0;
        gbc.weighty    = 1;
        gbc.insets  = new Insets(0, 0, ROW_SEP, LABEL_SEP);
        add(pOffset, gbc);

        lab = new JLabel("(arcsec)");
        gbc.anchor     = GridBagConstraints.WEST;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridx      = col+2;
        gbc.gridy      = 0;
        gbc.gridheight = 1;
        gbc.gridwidth  = 1;
        gbc.weightx    = 0;
        gbc.weighty    = 0;
        gbc.insets  = new Insets(0, 0, ROW_SEP, COLUMN_SEP);
        add(lab, gbc);

        lab = new JLabel("q");
        gbc.anchor     = GridBagConstraints.EAST;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridx      = col;
        gbc.gridy      = 1;
        gbc.gridheight = 1;
        gbc.gridwidth  = 1;
        gbc.weightx    = 0;
        gbc.weighty    = 0;
        gbc.insets  = new Insets(0, 0, 0, LABEL_SEP);
        add(lab, gbc);

        qOffset = new NumberBoxWidget();
        qOffset.setColumns(7);
        qOffset.setMinimumSize(new Dimension(80, 20));
        gbc.anchor     = GridBagConstraints.WEST;
        gbc.fill       = GridBagConstraints.BOTH;
        gbc.gridx      = col+1;
        gbc.gridy      = 1;
        gbc.gridheight = 1;
        gbc.gridwidth  = 1;
        gbc.weightx    = 0;
        gbc.weighty    = 1;
        gbc.insets  = new Insets(0, 0, 0, LABEL_SEP);
        add(qOffset, gbc);

        lab = new JLabel("(arcsec)");
        gbc.anchor     = GridBagConstraints.WEST;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridx      = col+2;
        gbc.gridy      = 1;
        gbc.gridheight = 1;
        gbc.gridwidth  = 1;
        gbc.weightx    = 0;
        gbc.weighty    = 0;
        gbc.insets  = new Insets(0, 0, 0, COLUMN_SEP);
        add(lab, gbc);


        // WFS combo boxes -----------------------------------------------------
        col += 3;

        guidersPanel = new OffsetPosGuiderPanel();
        gbc.anchor     = GridBagConstraints.WEST;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridx      = col;
        gbc.gridy      = 0;
        gbc.gridheight = 2;
        gbc.gridwidth  = 1;
        gbc.weightx    = 0;
        gbc.weighty    = 0;
        gbc.insets     = new Insets(0, 0, 0, 0);
        add(guidersPanel, gbc);
    }

    public JPanel getPanel() {
        return this;
    }

    public JLabel getOrientationLabel() {
        return orientationLabel;
    }

    public NumberBoxWidget getPOffsetTextBox() {
        return pOffset;
    }

    public NumberBoxWidget getQOffsetTextBox() {
        return qOffset;
    }

    @Override
    public JComboBox getDefaultGuiderCombo() {
        return guidersPanel.getDefaultCombo();
    }

    public JComboBox getAdvancedGuiderCombo(GuideProbe guider) {
        return guidersPanel.getAdvancedCombo(guider);
    }

    public void setGuiders(Set<GuideProbe> guiders) {
        guidersPanel.setGuiders(guiders);
    }
}
