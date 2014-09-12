//
// $Id$
//

package jsky.app.ot.progadmin;

import javax.swing.*;
import java.awt.*;

/**
 * UI for editing GSA attributes.
 */
final class GsaUI extends JPanel {
    private JSpinner _monthSpinner;
    private JCheckBox _headerCheckBox;

    public GsaUI() {
        super(new GridBagLayout());

//        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();

        JLabel lab = new JLabel("Proprietary Period");
        gbc.gridx  = 0;
        gbc.gridy  = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill   = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 5, 5);
        add(lab, gbc);

        _monthSpinner = new JSpinner(new SpinnerNumberModel(18, 0, Integer.MAX_VALUE, 1));
        gbc.gridx  = 1;
        gbc.gridy  = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill   = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 5, 5, 5);
        add(_monthSpinner, gbc);

        lab = new JLabel("months");
        gbc.gridx  = 2;
        gbc.gridy  = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill   = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 5, 5, 0);
        add(lab, gbc);

        _headerCheckBox = new JCheckBox("Keep header private");
        gbc.gridx  = 0;
        gbc.gridy  = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 0, 0);
        gbc.gridwidth = 3;
        add(_headerCheckBox, gbc);
    }

    public JSpinner getMonthSpinner() {
        return _monthSpinner;
    }

    public JCheckBox getHeaderCheckbox() {
        return _headerCheckBox;
    }
}
