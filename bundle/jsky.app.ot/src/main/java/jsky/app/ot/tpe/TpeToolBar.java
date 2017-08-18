package jsky.app.ot.tpe;

import edu.gemini.catalog.image.ImageCatalog;
import edu.gemini.catalog.ui.tpe.CatalogImageDisplay;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import jsky.app.ot.ags.AgsSelectorControl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;

/**
 * Contains toggle buttons for selecting the current mode
 * (Browse, Drag, Erase), create mode (guide star, offset positions),
 * and for toggling the visibility of various position editor
 * features.
 *
 * @version $Revision: 45662 $
 * @author Allan Brighton */
final class TpeToolBar extends JPanel {

    /**
     * The ButtonPanel houses a related group of toggle buttons, checkboxes,
     * radio buttons, etc. that appear in the TPE toolbar.
     */
    private static final class ButtonPanel extends JPanel {
        final JPanel                    contents = new JPanel(new GridLayout(0, 2));
        final java.util.List<JComponent> btnList = new ArrayList<>();

        ButtonPanel(String label) {
            super(new GridBagLayout());

            add(new JLabel(label), new GridBagConstraints() {{
                gridx = 0;
                gridy = 0;
                anchor = WEST;
                fill = HORIZONTAL;
                weightx = 1.0;
                insets = new Insets(0, 0, 3, 0);
            }});

            add(contents, new GridBagConstraints() {{
                gridx   = 0;
                gridy   = 1;
                fill    = BOTH;
                weightx = 1.0;
                weighty = 1.0;
                insets  = new Insets(0, 5, 0, 0);
            }});
        }

        private void rejigger() {
            final JComponent[] cs = btnList.stream().filter(Component::isVisible).toArray(JComponent[]::new);
            final int size        = cs.length;
            final int half        = size / 2 + size % 2;

            setVisible(size > 0);
            contents.removeAll();

            // We want the buttons to be arranged in two columns like a news
            // paper, where you read to the end of one column and then pick up
            // at the top of the next column.  Unfortunately GridLayout works
            // the other way: left, right, left, right, ...
            for (int i=0; i<half; ++i) {
                contents.add(cs[i]);
                final int j = i + half;
                if (j < size) contents.add(cs[j]);
            }

            contents.revalidate();
            contents.repaint();
        }

        void addButton(JToggleButton btn) {
            btnList.add(btn);
            rejigger();

            btn.addComponentListener(new ComponentAdapter() {
                public void componentShown(ComponentEvent e)  { rejigger(); }
                public void componentHidden(ComponentEvent e) { rejigger(); }
            });
        }
    }

    private final ImageCatalogPanel _imageCatalogPanel;

    private static GridBagConstraints gbc(final int y) {
        return new GridBagConstraints() {{
            gridx = 0; weightx = 1; gridy = y; anchor = WEST; fill = HORIZONTAL;
        }};
    }

    // The Tools panel
    private final ButtonPanel _modePanel;

    // The Create panel
    private final ButtonPanel _createPanel;

    // Shared button group between mode and create panel
    private final ButtonGroup _buttonGroup = new ButtonGroup();

    // The View panel
    private final ButtonPanel[] _viewPanel = new ButtonPanel[TpeImageFeatureCategory.values().length];

    // The ags guider selection panel
    private final AgsSelectorControl _guiderSelector = new AgsStrategyPanel();

    /**
     * The left side toolbar for the position editor.
     */
    TpeToolBar(CatalogImageDisplay display) {
        super(new GridBagLayout());

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));

        _modePanel = new ButtonPanel("Tools");
        add(_modePanel, new GridBagConstraints() {{
            gridx = 0; gridy = 0; anchor = NORTH; fill = HORIZONTAL; weightx = 1;
            insets = new Insets(0, 0, 0, 0);
        }});

        _createPanel = new ButtonPanel("Create");
        add(_createPanel, new GridBagConstraints() {{
            gridx = 0; gridy = 1; anchor = NORTH; fill = HORIZONTAL; weightx = 1;
            insets = new Insets(5, 0, 0, 0);
        }});

        int i=0;
        for (TpeImageFeatureCategory cat : TpeImageFeatureCategory.values()) {
            _viewPanel[i] = new ButtonPanel(cat.displayName());

            GridBagConstraints gbc = new GridBagConstraints() {{
                gridx = 0; anchor = NORTH; fill = HORIZONTAL;
                insets = new Insets(10, 0, 0, 0);
            }};
            gbc.gridy = 2 + i;

            add(_viewPanel[i], gbc);
            ++i;
        }

        final int yPos = 2 + i;
        add(_guiderSelector.getUi(), new GridBagConstraints() {{
            gridx = 0; gridy = yPos; anchor = NORTH; fill = HORIZONTAL;
            insets = new Insets(10, 0, 0, 0);
        }});
        final int catalogYPos = yPos + i;
        _imageCatalogPanel = new ImageCatalogPanel(display);
        add(_imageCatalogPanel.panel().peer(), new GridBagConstraints() {{
            gridx = 0; gridy = catalogYPos; anchor = NORTH; fill = HORIZONTAL;
            insets = new Insets(10, 0, 0, 0);
        }});

        // consume remaining vertical space
        final GridBagConstraints gbc = new GridBagConstraints() {{
            gridx = 0; fill = BOTH; weighty = 1.0;
        }};
        gbc.gridy = 4 + i;
        add(new JPanel(), gbc);

    }

    void addModeButton(JToggleButton btn) {
        _buttonGroup.add(btn);
        _modePanel.addButton(btn);
    }

    void addCreateButton(JToggleButton btn) {
        _buttonGroup.add(btn);
        _createPanel.addButton(btn);
    }

    void hideCreateButtons() {
        _createPanel.setVisible(false);
    }

    void addViewItem(JToggleButton btn, TpeImageFeatureCategory cat) {
        final ButtonPanel viewPanel = _viewPanel[cat.ordinal()];
        viewPanel.addButton(btn);
    }

    void hideViewButtons() {
        for (JPanel pan : _viewPanel) pan.setVisible(false);
    }

    AgsSelectorControl getGuiderSelector() {
        return _guiderSelector;
    }

    /**
     * Updates the image catalogue selection panel
     */
    void updateImageCatalogState(ISPObservation obs) {
        _imageCatalogPanel.resetCatalogue(obs);
    }

    /**
     * Verifies that the catalog is selected on the UI
     */
    boolean isCatalogSelected(ImageCatalog catalog) {
        return _imageCatalogPanel.isCatalogSelected(catalog);
    }
}

