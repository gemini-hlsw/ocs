package jsky.app.ot.userprefs.observer;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.Site;
import jsky.app.ot.userprefs.ui.PreferenceDialog;
import jsky.app.ot.userprefs.ui.PreferencePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ObserverPreferencesPanel implements PreferencePanel {
    private static final String SITE_NOTE = "Observations will be queued to this site and it will be checked for ToO events.";
    private final JPanel panel = new JPanel(new GridBagLayout());

    public ObserverPreferencesPanel() {
        panel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        panel.add(sitePanel(), new GridBagConstraints() {{
            gridx = 0;
            gridy = 0;
            anchor = WEST;
            fill = HORIZONTAL;
            weightx = 1.0;
        }});

        final JCheckBox tooAlerts = new JCheckBox("Enable audible ToO alerts") {{
            setSelected(ObserverPreferences.fetch().isAudibleTooAlerts());
            setFocusable(false);
            addActionListener(e -> ObserverPreferences.fetch().withAudibleTooAlerts(isSelected()).store());
        }};
        panel.add(tooAlerts, new GridBagConstraints() {{
            gridx = 0;
            gridy = 2;
            anchor = WEST;
            fill = HORIZONTAL;
            weightx = 1.0;
            insets = new Insets(10, 0, 0, 0);
        }});
    }

    private static JPanel sitePanel() {
        final class SiteAction implements ActionListener {
            private final Site site;

            SiteAction(Site site) {
                this.site = site;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                ObserverPreferences.fetch().withObservingSite(site).store();
            }
        }

        final JLabel lab = new JLabel("Observing Site");
        final ButtonGroup grp = new ButtonGroup();
        final JRadioButton gn = new JRadioButton("Gemini North") {{
            setFocusable(false);
            addActionListener(new SiteAction(Site.GN));
        }};
        final JRadioButton gs = new JRadioButton("Gemini South") {{
            setFocusable(false);
            addActionListener(new SiteAction(Site.GS));
        }};
        final JRadioButton none = new JRadioButton("None") {{
            setFocusable(false);
            addActionListener(new SiteAction(null));
        }};
        grp.add(gn);
        grp.add(gs);
        grp.add(none);
        final Site s = ObserverPreferences.fetch().observingSite();

        // Initialize the site selection.
        if (s == null) none.setSelected(true);
        else switch (s) {
            case GN:
                gn.setSelected(true);
                break;
            case GS:
                gs.setSelected(true);
                break;
            default:
                none.setSelected(true);
        }

        return new JPanel(new GridBagLayout()) {{
            add(lab, new GridBagConstraints() {{
                gridx = 0;
                gridy = 0;
                gridwidth = 4;
                anchor = WEST;
                fill = HORIZONTAL;
                weightx = 1.0;
                insets = new Insets(0, 0, 5, 0);
            }});
            add(gn, new GridBagConstraints() {{
                gridx = 0;
                gridy = 1;
                insets = new Insets(0, 15, 0, 5);
            }});
            add(gs, new GridBagConstraints() {{
                gridx = 1;
                gridy = 1;
                insets = new Insets(0, 0, 0, 5);
            }});
            add(none, new GridBagConstraints() {{
                gridx = 2;
                gridy = 1;
            }});
            add(new JPanel(), new GridBagConstraints() {{
                gridx = 3;
                gridy = 1;
                fill = HORIZONTAL;
                weightx = 1.0;
            }});
            add(PreferenceDialog.mkNote(SITE_NOTE), new GridBagConstraints() {{
                gridx = 0;
                gridy = 2;
                gridwidth = 4;
                anchor = WEST;
                fill = HORIZONTAL;
                weightx = 1.0;
                insets = new Insets(0, 15, 0, 0);
            }});
        }};
    }

    public String getDisplayName() {
        return "Observer";
    }

    public Option<String> getToolTip() {
        return new Some<>("Observer preferences.");
    }

    public Option<Icon> getIcon() {
        return None.instance();
    }

    public Component getUserInterface() {
        return panel;
    }
}
