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
    private final JLabel timeAwardedLabel;
    private final JTextField minTimeField;
    private final Map<TimeAcctCategory, JTextField> hoursFieldMap;
    private final Map<TimeAcctCategory, JLabel> percentLabelMap;

    public TimeAcctUI() {
        super(new BorderLayout());

        timeAwardedLabel = new JLabel();
        minTimeField     = new JTextField();
        minTimeField.setColumns(5);

        hoursFieldMap = new HashMap<>();
        percentLabelMap = new HashMap<>();
        for (final TimeAcctCategory cat : TimeAcctCategory.values()) {
            final JTextField tf = new JTextField();
            tf.setColumns(5);
            tf.setToolTipText("Enter the number of hours to charge to " + cat.getDisplayName());
            hoursFieldMap.put(cat, tf);

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
        timeAwardedLabel.setForeground(Color.black);
        pan.add(timeAwardedLabel, gbc);

        gbc.gridx  = 2;
        gbc.insets = inLeft;
        pan.add(new JLabel("hours"), gbc);

        gbc.gridx   = 3;
        gbc.insets  = inEmpty;
        pan.add(Box.createHorizontalStrut(20), gbc);

        gbc.gridx   = 4;
        gbc.insets  = inRight;
        pan.add(new JLabel("Min Time (Band 3 Only)"), gbc);

        gbc.gridx   = 5;
        gbc.insets  = inEmpty;
        pan.add(minTimeField, gbc);

        gbc.gridx   = 6;
        gbc.insets  = inLeft;
        pan.add(new JLabel("hours"), gbc);

        gbc.gridx   = 7;
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

        final int div = catA.length/2 + catA.length%2;
        for (int i=0; i<catA.length; ++i) {
            final TimeAcctCategory cat = catA[i];

            int row = i % div;
            int col = (i / div) * 3;

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
            pan.add(hoursFieldMap.get(cat), gbc);

            gbc.anchor    = GridBagConstraints.EAST;
            gbc.gridx     = col+2;
            gbc.gridy     = row;
            gbc.insets    = new Insets(10, 0, 0, (col == 0) ? 30 : 0);
            pan.add(percentLabelMap.get(cat), gbc);
        }

        return pan;
    }

    public JLabel getTimeAwardedLabel() {
        return timeAwardedLabel;
    }

    public JTextField getMinimumTimeField() {
        return minTimeField;
    }

    public JTextField getHoursField(final TimeAcctCategory cat) {
        return hoursFieldMap.get(cat);
    }

    public JLabel getPercentLabel(final TimeAcctCategory cat) {
        return percentLabelMap.get(cat);
    }
}
