package jsky.app.ot.progadmin;

import edu.gemini.spModel.timeacct.TimeAcctCategory;

import javax.swing.*;
import java.util.Map;
import java.util.HashMap;
import java.awt.*;

/**
 * User interface widgets for time accounting;
 */
final class TimeAcctUI extends JPanel {
    private final JLabel totalAwardLabel;
    private final JLabel programAwardLabel;
    private final JLabel partnerAwardLabel;

    private final JTextField minTimeField;
    private final Map<TimeAcctCategory, JTextField> programAwardFieldMap;
    private final Map<TimeAcctCategory, JTextField> partnerAwardFieldMap;
    private final Map<TimeAcctCategory, JLabel> percentLabelMap;

    public TimeAcctUI() {
        super(new BorderLayout());

        totalAwardLabel   = new JLabel();
        programAwardLabel = new JLabel();
        partnerAwardLabel = new JLabel();
        minTimeField      = new JTextField();
        minTimeField.setColumns(5);

        programAwardFieldMap = new HashMap<>();
        partnerAwardFieldMap = new HashMap<>();
        percentLabelMap      = new HashMap<>();
        for (final TimeAcctCategory cat : TimeAcctCategory.values()) {
            final JTextField programTf = new JTextField();
            programTf.setColumns(5);
            programTf.setToolTipText("Enter the number of program hours to charge to " + cat.getDisplayName());
            programAwardFieldMap.put(cat, programTf);

            final JTextField partnerTf = new JTextField();
            partnerTf.setColumns(5);
            partnerTf.setToolTipText("Enter the number of partner hours to charge to " + cat.getDisplayName());
            partnerAwardFieldMap.put(cat, partnerTf);

            final JLabel lab = new JLabel();
            lab.setHorizontalAlignment(JLabel.TRAILING);
            lab.setPreferredSize(new Dimension(lab.getFontMetrics(lab.getFont()).stringWidth("100%"),
                    lab.getFontMetrics(lab.getFont()).getHeight()));
            percentLabelMap.put(cat, lab);
        }

        add(getTimeAwardedPanel(), BorderLayout.NORTH);
        add(getRatiosPanel(), BorderLayout.CENTER);
    }

    private JComponent getTimeAwardedPanel() {
        final JPanel pan = new JPanel(new GridBagLayout());

        final Insets inEmpty = new Insets(0, 0, 0, 0);
        final Insets inLeft  = new Insets(0, 5, 0, 0);
        final Insets inRight = new Insets(0, 0, 0, 5);

        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx  = 0;
        gbc.insets = inRight;
        pan.add(new JLabel("Time Awarded"), gbc);

        gbc.gridx  = 1;
        gbc.insets = inEmpty;
        totalAwardLabel.setForeground(Color.black);
        pan.add(totalAwardLabel, gbc);

        gbc.gridx  = 2;
        gbc.insets = inLeft;
        pan.add(new JLabel("hours"), gbc);

        gbc.gridx  = 3;
        gbc.insets = inLeft;
        pan.add(new JLabel("("), gbc);

        gbc.gridx  = 4;
        gbc.insets = inEmpty;
        pan.add(programAwardLabel, gbc);

        gbc.gridx  = 5;
        gbc.insets = inLeft;
        pan.add(new JLabel("program +"), gbc);

        gbc.gridx  = 6;
        gbc.insets = inLeft;
        pan.add(partnerAwardLabel, gbc);

        gbc.gridx  = 7;
        gbc.insets = inLeft;
        pan.add(new JLabel("partner)"), gbc);

        gbc.gridx   = 8;
        gbc.insets  = inEmpty;
        pan.add(Box.createHorizontalStrut(20), gbc);

        gbc.gridx   = 9;
        gbc.insets  = inRight;
        pan.add(new JLabel("Min Time (Band 3 Only)"), gbc);

        gbc.gridx   = 10;
        gbc.insets  = inEmpty;
        pan.add(minTimeField, gbc);

        gbc.gridx   = 11;
        gbc.insets  = inLeft;
        pan.add(new JLabel("hours"), gbc);

        gbc.gridx   = 12;
        gbc.insets  = inEmpty;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        pan.add(new JPanel(), gbc);

        return pan;
    }

    private JPanel getRatiosPanel() {
        final JPanel pan = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();

        final TimeAcctCategory[] catA = TimeAcctCategory.values();

        // Column Headers
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.gridy     = 0;
        gbc.insets    = new Insets(10, 0, 0, 5);

        gbc.gridx     = 1;
        pan.add(new JLabel("Program"), gbc);
        gbc.gridx     = 2;
        pan.add(new JLabel("Partner"), gbc);
        gbc.gridx     = 5;
        pan.add(new JLabel("Program"), gbc);
        gbc.gridx     = 6;
        pan.add(new JLabel("Partner"), gbc);

        // Time Accounting Allocation
        final int div = catA.length/2 + catA.length%2;
        for (int i=0; i<catA.length; ++i) {
            final TimeAcctCategory cat = catA[i];

            int row = 1 + i % div;
            int col = (i / div) * 4;

            final JLabel lab = new JLabel(String.format("%s (%s)", cat.name(), cat.getDisplayName()));

            gbc.anchor    = GridBagConstraints.EAST;
            gbc.gridx     = col;
            gbc.gridy     = row;
            gbc.insets    = new Insets(10, 0, 0, 5);
            pan.add(lab, gbc);

            gbc.anchor    = GridBagConstraints.WEST;
            gbc.gridx     = col+1;
            gbc.gridy     = row;
            gbc.insets    = new Insets(10, 0, 0, 5);
            pan.add(programAwardFieldMap.get(cat), gbc);

            gbc.anchor    = GridBagConstraints.WEST;
            gbc.gridx     = col+2;
            gbc.gridy     = row;
            gbc.insets    = new Insets(10, 0, 0, 5);
            pan.add(partnerAwardFieldMap.get(cat), gbc);

            gbc.anchor    = GridBagConstraints.EAST;
            gbc.gridx     = col+3;
            gbc.gridy     = row;
            gbc.insets    = new Insets(10, 0, 0, (col == 0) ? 30 : 0);
            pan.add(percentLabelMap.get(cat), gbc);
        }

        return pan;
    }

    public JLabel getTotalAwardLabel() {
        return totalAwardLabel;
    }
    public JLabel getProgramAwardLabel() {
        return programAwardLabel;
    }
    public JLabel getPartnerAwardLabel() {
        return partnerAwardLabel;
    }

    public JTextField getMinimumTimeField() {
        return minTimeField;
    }

    public JTextField getProgramAwardField(final TimeAcctCategory cat) {
        return programAwardFieldMap.get(cat);
    }

    public JTextField getPartnerAwardField(final TimeAcctCategory cat) {
        return partnerAwardFieldMap.get(cat);
    }

    public JLabel getPercentLabel(final TimeAcctCategory cat) {
        return percentLabelMap.get(cat);
    }
}
