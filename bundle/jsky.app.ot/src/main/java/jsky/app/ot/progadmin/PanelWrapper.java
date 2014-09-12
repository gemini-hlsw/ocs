//
// $
//

package jsky.app.ot.progadmin;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;

/**
 * A decorator panel for the various sub panels of the admin dialog.
 */
final class PanelWrapper extends JPanel {
    public PanelWrapper(String title, JPanel content) {
        super(new BorderLayout());

        setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD));

        Color bg = UIManager.getColor("Label.foreground");
        Color fg = Color.white;

        titlePanel.setBackground(bg);
        label.setForeground(fg);
        titlePanel.add(label);
        add(titlePanel, BorderLayout.NORTH);

        JPanel contentWrapper = new JPanel(new GridBagLayout());
        contentWrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.fill      = GridBagConstraints.NONE;
        gbc.gridx     = 0;
        gbc.weightx   = 0;
        contentWrapper.add(content, gbc);

        gbc.anchor    = GridBagConstraints.CENTER;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.gridx     = 1;
        gbc.weightx   = 1.0;
        contentWrapper.add(new JPanel(), gbc);

        add(contentWrapper, BorderLayout.CENTER);
    }
}
