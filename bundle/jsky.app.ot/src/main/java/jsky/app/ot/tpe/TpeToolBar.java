package jsky.app.ot.tpe;

import edu.gemini.catalog.ui.tpe.CatalogImageDisplay;
import jsky.app.ot.ags.AgsSelectorControl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

/**
 * Contains toggle buttons for selecting the current mode
 * (Browse, Drag, Erase), create mode (guide star, offset positions),
 * and for toggling the visibility of various position editor
 * features.
 *
 * @version $Revision: 45662 $
 * @author Allan Brighton */
final class TpeToolBar extends JPanel {

    private static GridBagConstraints gbc(final int y) {
        return new GridBagConstraints() {{
            gridx = 0; weightx = 1; gridy = y; anchor = WEST; fill = HORIZONTAL;
        }};
    }

    // The Tools panel
    private final JPanel _modePanel;

    // The Create panel
    private final JPanel _createPanel;

    // Shared button group between mode and create panel
    private final ButtonGroup _buttonGroup = new ButtonGroup();

    // The View panel
    private final JPanel[] _viewPanel = new JPanel[TpeImageFeatureCategory.values().length];

    // The ags guider selection panel
    private final AgsSelectorControl _guiderSelector = new AgsStrategyPanel();

    /**
     * The left side toolbar for the position editor.
     */
    TpeToolBar(CatalogImageDisplay display) {
        super(new GridBagLayout());

        setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        _modePanel = new JPanel(new GridBagLayout());
        add(_modePanel, new GridBagConstraints() {{
            gridx = 0; gridy = 0; anchor = NORTH; fill = HORIZONTAL; weightx = 1;
            insets = new Insets(0, 5, 0, 0);
        }});

        _createPanel = new JPanel(new GridBagLayout());
        add(_createPanel, new GridBagConstraints() {{
            gridx = 0; gridy = 1; anchor = NORTH; fill = HORIZONTAL; weightx = 1;
            insets = new Insets(5, 5, 0, 0);
        }});

        int i=0;
        for (TpeImageFeatureCategory cat : TpeImageFeatureCategory.values()) {
            _viewPanel[i] = new JPanel(new GridBagLayout());

            GridBagConstraints gbc = new GridBagConstraints() {{
                gridx = 0; anchor = NORTH; fill = HORIZONTAL;
                insets = new Insets(10, 0, 0, 0);
            }};
            gbc.gridy = 2+i;

            add(wrapPanel(cat.displayName(), _viewPanel[i]), gbc);
            ++i;
        }

        final int yPos = 2+i;
        final JPanel wrappedGuiderSelector = wrapPanel("", _guiderSelector.getUi());
        add(wrappedGuiderSelector, new GridBagConstraints() {{
            gridx = 0; gridy = yPos; anchor = NORTH; fill = HORIZONTAL;
            insets = new Insets(10, 0, 0, 0);
        }});

        // consume remaining vertical space
        final GridBagConstraints gbc = new GridBagConstraints() {{
            gridx = 0; fill = BOTH; weighty = 1.0;
        }};
        gbc.gridy = 3+i;
        add(new JPanel(), gbc);

    }

    private JPanel wrapPanel(String label, JComponent panel) {
        final JPanel res = new JPanel(new GridBagLayout());

        final JLabel lab = new JLabel(label);
        res.add(lab, new GridBagConstraints() {{
            gridx = 0; gridy = 0; anchor = WEST; fill = HORIZONTAL; weightx = 1;
            insets = new Insets(0, 0, 3, 0);
        }});
        res.add(panel, new GridBagConstraints() {{
            gridx = 0; gridy = 1; fill = BOTH; weightx = 1;
            insets = new Insets(0, 5, 0, 0);
        }});

        return res;
    }

    private void add(Component comp, JPanel pan) {
        pan.add(comp, gbc(pan.getComponentCount()));
    }

    private void hide(Container pan) {
        for (Component comp : pan.getComponents()) {
            comp.setVisible(false);
        }
    }

    void addModeButton(JToggleButton button) {
        _buttonGroup.add(button);
        add(button, _modePanel);
    }

    void addCreateButton(JToggleButton button) {
        _buttonGroup.add(button);
        add(button, _createPanel);
    }

    void hideCreateButtons() {
        hide(_createPanel);
    }

    static Component createKeyPanel(Component key) {
        final JPanel pan = new JPanel(new GridBagLayout());

        final Dimension d = new Dimension(16, 16);
        JPanel spacer = new JPanel() {{
            setPreferredSize(d); setMinimumSize(d); setMaximumSize(d);
        }};
        pan.add(spacer, new GridBagConstraints() {{
            gridx=0; gridy=0; weightx=0; weighty=0; fill=NONE; anchor=WEST;
        }});
        pan.add(key, new GridBagConstraints() {{
            gridx=1; gridy=0; weightx=1.0; weighty=0; fill=HORIZONTAL; anchor=WEST;
        }});
        return pan;
    }

    void addViewItem(Component comp, TpeImageFeatureCategory cat) {
        final JPanel viewPanel = _viewPanel[cat.ordinal()];
        add(comp, viewPanel);

        // Watch the component for visibility updates.  When changed, check
        // the other components in the panel.  If they are all hidden, hide
        // the entire parent that contains the label.  If some are visible,
        // show the entire panel that contains the label.
        comp.addComponentListener(new ComponentListener() {
            public void componentResized(ComponentEvent e) {}
            public void componentMoved(ComponentEvent e) {}

            public void componentShown(ComponentEvent e) {
                refreshViewPanelVisibility();
            }

            public void componentHidden(ComponentEvent e) {
                refreshViewPanelVisibility();
            }

            private boolean someVisible(JPanel pan) {
                for (Component comp : pan.getComponents()) {
                    if (comp.isVisible()) return true;
                }
                return false;
            }

            private void refreshViewPanelVisibility() {
                viewPanel.getParent().setVisible(someVisible(viewPanel));
            }
        });
    }

    void hideViewButtons() {
        for (JPanel pan : _viewPanel) hide(pan);
    }

    AgsSelectorControl getGuiderSelector() {
        return _guiderSelector;
    }
}

