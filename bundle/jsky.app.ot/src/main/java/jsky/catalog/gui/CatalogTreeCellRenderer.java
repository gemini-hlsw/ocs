/*
 * ESO Archive
 *
 * $Id: CatalogTreeCellRenderer.java 7129 2006-06-07 15:09:10Z anunez $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  2000/01/27  Created
 */

package jsky.catalog.gui;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import jsky.catalog.Catalog;
import jsky.catalog.CatalogDirectory;
import jsky.util.Resources;
import jsky.util.Preferences;

/**
 * This local class is used to override the default tree node
 * renderer and provide special catalog dependent icons.
 */
public class CatalogTreeCellRenderer extends DefaultTreeCellRenderer {

    private Icon _imagesvrIcon = Resources.getIcon("imagesvr.gif");
    private Icon _catalogIcon = Resources.getIcon("catalog.gif");
    private Icon _archiveIcon = Resources.getIcon("archive.gif");
    private Icon _namesvrIcon = Resources.getIcon("namesvr.gif");
    private Icon _imagesvrIconDefault = Resources.getIcon("imagesvr_default.gif");

    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, sel,
                                           expanded, leaf, row, hasFocus);

        setBackgroundNonSelectionColor(tree.getBackground());

        if (value instanceof Catalog) {
            if (value instanceof CatalogDirectory) {
                setIcon(getOpenIcon());
                setToolTipText(((CatalogDirectory) value).getDescription());
            } else {
                String servType = ((Catalog) value).getType();
                setToolTipText(getText());
                if (servType.equals("directory")) {
                    setIcon(getOpenIcon());
                }
                if (servType.equals("catalog")) {
                    setIcon(_catalogIcon);
                }
                if (servType.equals("archive")) {
                    setIcon(_archiveIcon);
                }
                if (servType.equals("namesvr")) {
                    setIcon(_namesvrIcon);
                }
                if (servType.equals("imagesvr")) {
                    Catalog defaultCatalog = _getUserCatalog();
                    if (defaultCatalog != null &&
                            defaultCatalog.getName().equals(((Catalog)value).getName())) {
                        setIcon(_imagesvrIconDefault);
                        setToolTipText(getText() + " (default)");
                    } else {
                        setIcon(_imagesvrIcon);
                    }
                }
            }
        }

        return this;
    }

     private Catalog _getUserCatalog() {
        String catalogProperties = Preferences.get(Catalog.SKY_USER_CATALOG);
        // If no properties, show the catalog browser
        if (catalogProperties == null) {
            return null;
        }

        String args[] = catalogProperties.split("\\*");

        if (args.length <= 0) return null;

        Catalog c = CatalogNavigator.getCatalogDirectory().getCatalog(args[0]);

        if (c == null || !c.isImageServer()) return null;

        return c;
    }
}

