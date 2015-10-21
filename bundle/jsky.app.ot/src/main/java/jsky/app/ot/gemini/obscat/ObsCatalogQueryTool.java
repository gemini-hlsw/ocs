package jsky.app.ot.gemini.obscat;

import jsky.catalog.Catalog;
import jsky.catalog.gui.CatalogQueryPanel;
import jsky.catalog.gui.CatalogQueryTool;
import jsky.catalog.gui.QueryResultDisplay;
import jsky.util.Preferences;

import java.awt.*;

import static java.awt.GridBagConstraints.*;
import javax.swing.*;

/**
 * Defines the user interface for querying an ObsCatalog. This replaces the default
 * {@link jsky.catalog.gui.CatalogQueryTool} with once specialized for the ObsCatalog class.
 *
 * @author Allan Brighton
 */
public final class ObsCatalogQueryTool extends CatalogQueryTool {
    private static final String PREF_KEY = ObsCatalogQueryTool.class.getName();

    private JCheckBox remote;

    /**
     * Initialize a query panel for the given catalog.
     *
     * @param catalog the catalog, for which a user interface component is being generated
     * @param display object used to display the results of a query.
     */
    public ObsCatalogQueryTool(Catalog catalog, QueryResultDisplay display) {
        super(catalog, display, false);
    }

    /** Make and return the catalog query panel */
    protected CatalogQueryPanel makeCatalogQueryPanel(Catalog catalog) {
        return new ObsCatalogQueryPanel(catalog, 6);
    }

    protected JPanel makeButtonPanel() {
        remote = new JCheckBox("Include Remote Programs") {{
            setToolTipText("Check to include programs in the remote database in query results.");
            setSelected(Preferences.get(PREF_KEY + ".remote", true));
            addActionListener(e -> Preferences.set(PREF_KEY + ".remote", isSelected()));
        }};

        final JPanel res = new JPanel(new GridBagLayout());
        res.add(new JPanel(),            new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, CENTER, HORIZONTAL, new Insets(0,0,0, 0), 0, 0));
        res.add(remote,                  new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, CENTER, NONE,       new Insets(0,0,0,10), 0, 0));
        res.add(super.makeButtonPanel(), new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, CENTER, NONE,       new Insets(0,0,0, 0), 0, 0));
        return res;
    }

    public boolean includeRemote() {
        return remote.isSelected();
    }
}
