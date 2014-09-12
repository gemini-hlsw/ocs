package jsky.app.ot.gemini.gpi;

import edu.gemini.spModel.guide.GuideProbe;
import jsky.app.ot.gemini.editor.offset.OffsetPosGuiderPanel;
import jsky.app.ot.gemini.editor.offset.OffsetPosUI;
import jsky.util.gui.NumberBoxWidget;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Set;

/**
 * UI for editing GPI offset positions
 */
public final class GpiOffsetPosPanel extends JPanel implements OffsetPosUI {
    private static final int COLUMN_SEP = 15;
    private static final int ROW_SEP    =  6;
    private static final int LABEL_SEP  =  5;

    protected JLabel orientationLabel;

    private NumberBoxWidget xOffset;
    private NumberBoxWidget yOffset;

    private OffsetPosGuiderPanel guidersPanel;

    public GpiOffsetPosPanel() {
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

        // x, y editors -----------------------------------------------------
        ++col;

        JLabel lab = new JLabel("X");
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

        xOffset = new NumberBoxWidget();
        xOffset.setColumns(7);
        xOffset.setMinimumSize(new Dimension(80, 20));
        gbc.anchor     = GridBagConstraints.WEST;
        gbc.fill       = GridBagConstraints.BOTH;
        gbc.gridx      = col+1;
        gbc.gridy      = 0;
        gbc.gridheight = 1;
        gbc.gridwidth  = 1;
        gbc.weightx    = 0;
        gbc.weighty    = 1;
        gbc.insets  = new Insets(0, 0, ROW_SEP, LABEL_SEP);
        add(xOffset, gbc);

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

        lab = new JLabel("Y");
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

        yOffset = new NumberBoxWidget();
        yOffset.setColumns(7);
        yOffset.setMinimumSize(new Dimension(80, 20));
        gbc.anchor     = GridBagConstraints.WEST;
        gbc.fill       = GridBagConstraints.BOTH;
        gbc.gridx      = col+1;
        gbc.gridy      = 1;
        gbc.gridheight = 1;
        gbc.gridwidth  = 1;
        gbc.weightx    = 0;
        gbc.weighty    = 1;
        gbc.insets  = new Insets(0, 0, 0, LABEL_SEP);
        add(yOffset, gbc);

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
        return xOffset;
    }

    public NumberBoxWidget getQOffsetTextBox() {
        return yOffset;
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
