package jsky.app.ot.userprefs.general;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import jsky.app.ot.userprefs.ui.PreferenceDialog;
import jsky.app.ot.userprefs.ui.PreferencePanel;
import jsky.app.ot.viewer.SPViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A panel used to display and edit the general preferences.
 */
public class GeneralPreferencesPanel implements PreferencePanel {
    private static final String P2_TEXT = "Select this option to turn on program validity checking.";
    private static final String UNSAVED_TEXT = "Warn me if I close a program with unsynchronized changes.";

    private final SPViewer viewer;

    private final JPanel panel = new JPanel(new GridBagLayout());

    public GeneralPreferencesPanel(SPViewer v) {
        this.viewer = v;
        panel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        final JCheckBox p2Checking = new JCheckBox("Enable Phase 2 error checking") {{
            setSelected(GeneralPreferences.fetch().isPhase2Checking());
            setFocusable(false);
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    boolean enabled = isSelected();
                    GeneralPreferences.fetch().withPhase2Checking(enabled).store();

                    if (enabled) {
                        viewer.checkCurrentProgram();
                    } else {
                        viewer.clearProblemInformation();
                    }
                }
            });
        }};
        panel.add(p2Checking, new GridBagConstraints() {{
            gridx = 0;
            gridy = 0;
            anchor = WEST;
            fill = HORIZONTAL;
            weightx = 1.0;
        }});

        final JTextArea p2Txt = PreferenceDialog.mkNote(P2_TEXT);
        panel.add(p2Txt, new GridBagConstraints() {{
            gridx = 0;
            gridy = 1;
            anchor = WEST;
            fill = HORIZONTAL;
            weightx = 1.0;
            insets = new Insets(0, 15, 0, 0);
        }});

        final JCheckBox usPrompt = new JCheckBox("Warn for unsaved changes on close") {{
            setSelected(GeneralPreferences.fetch().warnUnsavedChanges());
            setFocusable(false);
            addActionListener(e -> GeneralPreferences.fetch().withWarnUnsavedChanges(isSelected()).store());
        }};
        panel.add(usPrompt, new GridBagConstraints() {{
            gridx   = 0;
            gridy   = 2;
            anchor  = WEST;
            fill    = HORIZONTAL;
            weightx = 1.0;
            insets  = new Insets(10, 0, 0, 0);
        }});

        final JTextArea usTxt = PreferenceDialog.mkNote(UNSAVED_TEXT);
        panel.add(usTxt, new GridBagConstraints() {{
            gridx = 0;
            gridy = 3;
            anchor = WEST;
            fill   = HORIZONTAL;
            weightx = 1.0;
            insets  = new Insets(0, 15, 0, 0);
        }});
    }

    public String getDisplayName() {
        return "General";
    }

    public Option<String> getToolTip() {
        return new Some<>("General preferences.");
    }

    public Option<Icon> getIcon() {
        return None.instance();
    }

    public Component getUserInterface() {
        return panel;
    }
}
