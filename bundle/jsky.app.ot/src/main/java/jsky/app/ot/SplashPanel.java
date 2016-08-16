package jsky.app.ot;

import edu.gemini.pot.sp.ISPProgramNode;
import jsky.app.ot.gemini.obscat.ObsCatalog;
import jsky.app.ot.plugin.OtActionPlugin;
import jsky.app.ot.plugin.OtContext;
import jsky.app.ot.userprefs.observer.ObservingPeer;
import jsky.util.gui.Resources;
import jsky.app.ot.viewer.SPViewer;
import jsky.app.ot.viewer.action.*;
import jsky.app.ot.viewer.plugin.PluginConsumer;
import jsky.app.ot.viewer.plugin.PluginRegistry;
import scala.Option;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;

public final class SplashPanel extends JPanel implements PluginConsumer, ActionListener {

    private JComboBox<PluginWrapper> pluginSelector = new JComboBox<>();

    public SplashPanel(final URL welcomeURL, final boolean quitOnClose) {

        // Just make the damn thing non-null. This gets rid of a bunch of idiocy later on.
        if (welcomeURL == null)
            throw new IllegalArgumentException("Welcome URL can't be null.");

        // We need to know about plugins
        PluginRegistry.registerConsumer(this);

        // Ok construct and add everything.
        setLayout(new GridBagLayout());

        // This is the scrolly thing with the HTML help text
        add(new JScrollPane() {{
            setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            setBorder(BorderFactory.createEmptyBorder());
            setPreferredSize(new Dimension(9, 40));
            setViewportView(new JEditorPane() {{
                setMinimumSize(new Dimension(350, 23));
                setPreferredSize(new Dimension(350, 23));
                setBorder(new EmptyBorder(4, 5, 4, 5));
                setEditable(false);
                try {
                    setPage(welcomeURL);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }});
        }}, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(11, 0, 0, 11), 0, 0));

        // This is the button strip
        add(new JPanel() {{
            setLayout(new FlowLayout());

            // New Program
            add(new JButton(new NewAction() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    super.actionPerformed(evt);
                    if (!SPViewer.instances().isEmpty())
                        dismiss(false);
                }
            }));

            // Open Program
            add(new JButton(new OpenAction() {

                @Override
                public void actionPerformed(ActionEvent evt) {
                    super.actionPerformed(evt);
                    if (!SPViewer.instances().isEmpty())
                        dismiss(false);
                }

            }) {{
                setEnabled(true);
                setIcon(null);
            }});

            // Manage Keys
            add(new JButton(new OpenKeyManagerAction(null) {
                {
                    setEnabled(true);
                }
            }));

            // OT Browser
            add(new JButton(new QueryAction(ObsCatalog.QUERY_MANAGER)));

            // Import XML
            add(new JButton(new ImportAction() {

                @Override
                public void actionPerformed(ActionEvent evt) {
                    super.actionPerformed(evt);
                    if (!SPViewer.instances().isEmpty())
                        dismiss(false);
                }

            }));

            // Tools/Plugins (only visible if there are indeed plugins)
            add(pluginSelector);

            // Quit/Close
            add(new JButton(new AbstractAction() {
                {
                    putValue(Action.NAME, quitOnClose ? "Quit" : "Close");
                }
                public void actionPerformed(ActionEvent actionEvent) {
                    dismiss(quitOnClose);
                }
            }));

        }}, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(11, 3, 6, 6), 0, 0));

        // This is the pretty picture
        add(new JLabel() {{
            setBorder(LineBorder.createBlackLineBorder());
            setIcon(Resources.getIcon("splash.png"));
        }}, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(11, 11, 0, 0), 0, 0));

        // Sizing
        setMinimumSize(new Dimension(690, 318));
        setPreferredSize(new Dimension(690, 318));

        // initialize plugin selector
        rebuildPluginSelector();

    }

    private void dismiss(boolean quit) {
        if (quit) {
            System.exit(0);
        } else {
            Component c = this;
            while (!(c instanceof Window))
                c = c.getParent();
            c.setVisible(false);
            ((Window) c).dispose();
        }
    }

    /** Called by plugin registry on arrival of new plugins. */
    public void install(OtActionPlugin plugin) {
        rebuildPluginSelector();
    }

    /** Called by plugin registry on departure of a plugin. */
    public void uninstall(OtActionPlugin plugin) {
        rebuildPluginSelector();
    }

    /** Creates the plugin selector combo box. */
    private void rebuildPluginSelector() {
        // get all available plugins in display order
        final java.util.List<OtActionPlugin> plugins = PluginRegistry.pluginsByNameForJava();
        pluginSelector.removeActionListener(this);
        pluginSelector.removeAllItems();
        if (plugins.size() > 0) {
            // if there are plugins for this user: add them, install action listener and make combo box visible
            for (final OtActionPlugin  plugin: plugins) pluginSelector.addItem(new PluginWrapper(plugin));
            pluginSelector.setSelectedIndex(0);
            pluginSelector.addActionListener(this);
            pluginSelector.setVisible(true);
        } else {
            // if there are no plugins for this user: hide combo box
            pluginSelector.setVisible(false);
        }
    }

    /** Action listener implementation used for plugin selector. */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == pluginSelector) {
            ((PluginWrapper) pluginSelector.getSelectedItem()).openPlugin();
        }
    }

    /** Helper class that represents a plugin in the plugins combo box. */
    private class PluginWrapper {
        private final OtActionPlugin plugin;
        PluginWrapper(OtActionPlugin plugin) { this.plugin = plugin; }
        public String toString() { return plugin.name(); }
        public void openPlugin() {
            Option<ISPProgramNode> none = Option.apply(null);
            OtContext otContext = new OtContext(none, ObservingPeer.get(), OT.getKeyChain(), OT.getMagnitudeTable());
            plugin.apply(otContext, SwingUtilities.getWindowAncestor(getParent()));
        }
    }

}

