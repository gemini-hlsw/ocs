//
// $
//

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
    private JLabel timeAwardedLabel;
    private JTextField minTimeField;
    private Map<TimeAcctCategory, JTextField> hoursFieldMap;
    private Map<TimeAcctCategory, JLabel> percentLabelMap;

    public TimeAcctUI() {
        super(new BorderLayout());

        timeAwardedLabel = new JLabel();
        minTimeField     = new JTextField();
        minTimeField.setColumns(5);

        hoursFieldMap = new HashMap<TimeAcctCategory, JTextField>();
        percentLabelMap = new HashMap<TimeAcctCategory, JLabel>();
        for (TimeAcctCategory cat : TimeAcctCategory.values()) {
            JTextField tf = new JTextField();
            tf.setColumns(5);
            tf.setToolTipText("Enter the number of hours to charge to " + cat.getDisplayName());
            hoursFieldMap.put(cat, tf);

            JLabel lab = new JLabel();
            lab.setHorizontalAlignment(JLabel.TRAILING);
            lab.setPreferredSize(new Dimension(lab.getFontMetrics(lab.getFont()).stringWidth("100%"),
                    lab.getFontMetrics(lab.getFont()).getHeight()));
            percentLabelMap.put(cat, lab);
        }

        add(getTimeAwardedPanel(), BorderLayout.NORTH);
        add(getRatiosPanel(), BorderLayout.CENTER);
    }

    private JComponent getTimeAwardedPanel() {
        JPanel pan = new JPanel(new GridBagLayout());

        Insets inEmpty = new Insets(0, 0, 0, 0);
        Insets inLeft  = new Insets(0, 5, 0, 0);
        Insets inRight = new Insets(0, 0, 0, 5);

        GridBagConstraints gbc = new GridBagConstraints();

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
        JPanel pan = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        TimeAcctCategory[] catA = TimeAcctCategory.values();

        int div = catA.length/2 + catA.length%2;
        for (int i=0; i<catA.length; ++i) {
            TimeAcctCategory cat = catA[i];

            int row = i % div;
            int col = (i / div) * 3;

            String labText;
            labText = String.format("%s (%s)", cat.name(), cat.getDisplayName());
            JLabel lab = new JLabel(labText);

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

    public JTextField getHoursField(TimeAcctCategory cat) {
        return hoursFieldMap.get(cat);
    }

    public JLabel getPercentLabel(TimeAcctCategory cat) {
        return percentLabelMap.get(cat);
    }
}
