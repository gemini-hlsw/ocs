package jsky.app.ot.userprefs.ui;

import edu.gemini.shared.util.immutable.*;
import jsky.app.ot.util.Resources;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * A class that can be used to show a dialog of {@link PreferencePanel
 * preference panels}.
 */
public final class PreferenceDialog {

    private static class Dialog extends JDialog {
        // Content has a tool bar panel in the north, and the current
        // preference panel UI in the center.
        private final JPanel content = new JPanel(new BorderLayout());
        private final JComponent toolBar;
        private Option<Component> current = None.instance();

        /**
         * @param owner  associated Frame
         * @param panels list of preferences panels to include in this dialog
         * @param index  which of the panels to display first (if less than
         *               zero, the zero'th panel is displayed)
         */
        private Dialog(Frame owner, ImList<PreferencePanel> panels, int index) {
            super(owner, true);

            setResizable(false);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            toolBar = createToolBar(panels);
            if (panels.size() > 1) {
                content.add(toolBar, BorderLayout.PAGE_START);
            }

            setContentPane(content);
            if (panels.size() > 0) {
                JToggleButton btn;
                btn = (JToggleButton) toolBar.getComponent(index < 0 ? 0 : index);
                btn.doClick();
            }

            setMinimumSize(new Dimension(250, 0));
        }

        /**
         * Shows the indicated panel.
         */
        private void show(PreferencePanel panel) {
            setTitle(panel.getDisplayName() + " Preferences");

            // Remove the old UI that we were editing, if any.
            if (!None.instance().equals(current)) {
                content.remove(current.getValue());
            }

            // Add the new UI that we will be editing.
            Component ui = panel.getUserInterface();
            current = new Some<>(ui);
            content.add(ui, BorderLayout.CENTER);

            // Resize and re-layout everything
            pack();
        }

        private JComponent createToolBar(ImList<PreferencePanel> panels) {

            // Map the UserPreferencesPanel collection to Actions
            ImList<Action> actions = panels.map( pan -> new AbstractAction(pan.getDisplayName()) {
                {
                    Option<Icon> icon = pan.getIcon();
                    if (!None.instance().equals(icon)) {
                        putValue(Action.SMALL_ICON, icon.getValue());
                    }

                    Option<String> tip = pan.getToolTip();
                    if (!None.instance().equals(tip)) {
                        putValue(Action.SHORT_DESCRIPTION, tip.getValue());
                    }
                }

                public void actionPerformed(ActionEvent evt) {
                    Dialog.this.show(pan);
                }
            });

            // Creates a tool button bar with a button for each panel
            return (new PreferenceButtonPanelBuilder(actions)).build();
        }
    }

    private final ImList<PreferencePanel> panels;

    /**
     * Creates the dialog with the given preference panels.
     */
    public PreferenceDialog(ImList<PreferencePanel> panels) {
        this.panels = panels;
    }

    /**
     * Shows the dialog with the first panel open.
     *
     * @param frame show the dialog box on the screen relative to the position of
     * the given frame, if any
     */
    public void show(Frame frame) {
        show(frame, null);
    }

    /**
     * Shows the dialog with the given panel open, assuming this object was
     * constructed with this panel in the list of panels to include.
     *
     * @param frame show the dialog box on the screen relative to the position of
     * the given frame, if any
     * @param panel panel to display
     */
    public void show(Frame frame, PreferencePanel panel) {
        int index = (panel == null) ? -1 : panels.indexOf(panel);
        Dialog d = new Dialog(frame, panels, index);
        d.setLocationRelativeTo(frame);
        d.setVisible(true);
    }

    public static JTextArea mkNote(final String text) {
        return new JTextArea() {{
            setColumns(45);
            setText(text);
            setEditable(false);
            setOpaque(false);
            setLineWrap(true);
            setWrapStyleWord(true);
            setFont(getFont().deriveFont(getFont().getSize2D() - 2));
            setForeground(Color.DARK_GRAY);
        }};
    }

}
