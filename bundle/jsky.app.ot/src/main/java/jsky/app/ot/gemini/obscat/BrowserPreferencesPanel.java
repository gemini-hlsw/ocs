package jsky.app.ot.gemini.obscat;

import edu.gemini.shared.gui.ThinBorder;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import jsky.app.ot.userprefs.ui.PreferencePanel;
import jsky.app.ot.util.OtColor;

import javax.swing.*;
import java.awt.*;

/**
 * The UI for editing browser preferences.
 */
public enum BrowserPreferencesPanel implements PreferencePanel {
    instance;

    private static final String TEXT  = "Select this option to include filters, dispersers, and other instrument items that are no longer installed on the telescope but may exist in older programs.";

    private final JPanel panel = new JPanel(new GridBagLayout());

    BrowserPreferencesPanel() {

        final JPanel optPanel = new JPanel(new GridBagLayout());
        optPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        final JCheckBox showObsolete = new JCheckBox("Show obsolete options") {{
            setSelected(BrowserPreferences.fetch().showObsoleteOptions());
            setFocusable(false);
            addActionListener(e -> BrowserPreferences.fetch().withShowObsoleteOptions(isSelected()).store());
        }};
        optPanel.add(showObsolete, new GridBagConstraints() {{
            gridx=0; gridy=0; anchor=WEST; fill=HORIZONTAL; weightx=1.0;
            insets=new Insets(0, 0, 0, 0);
        }});

        final JTextArea txt = new JTextArea() {{
            setColumns(45);
            setText(TEXT);
            setEditable(false);
            setOpaque(false);
            setLineWrap(true);
            setWrapStyleWord(true);
            setFont(getFont().deriveFont(getFont().getSize2D()-2));
        }};
        optPanel.add(txt, new GridBagConstraints() {{
            gridx=0; gridy=1; anchor=WEST; fill=HORIZONTAL; weightx=1.0;
            insets=new Insets(0, 15, 0, 0);
        }});

        panel.add(optPanel, new GridBagConstraints() {{
            gridx=0; gridy=0; fill=BOTH; weightx=1.0; weighty=1.0;
        }});

        final JPanel msgPanel = new JPanel(new BorderLayout()) {{
            setBackground(OtColor.BANANA);
            setBorder(BorderFactory.createCompoundBorder(new ThinBorder(),
                         BorderFactory.createEmptyBorder(5, 5 , 5, 5)));
        }};

        final JLabel msg = new JLabel("Restart the OT for changes to take effect.") {{
            setForeground(Color.black);
            setFont(getFont().deriveFont(getFont().getSize2D()-2));
        }};
        msgPanel.add(msg, BorderLayout.CENTER);

        panel.add(msgPanel, new GridBagConstraints() {{
            gridx=0; gridy=1; fill=HORIZONTAL; weightx=1.0;
        }});
    }

    public String getDisplayName() {
        return "Browser";
    }

    public Option<String> getToolTip() {
        return new Some<>("OT database browser preferences.");
    }

    public Option<Icon> getIcon() {
        return None.instance();
    }

    public Component getUserInterface() {
        return panel;
    }
}
