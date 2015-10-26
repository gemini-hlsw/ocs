package jsky.navigator;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

import edu.gemini.catalog.ui.tpe.CatalogImageDisplay;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import jsky.catalog.Catalog;
import jsky.catalog.CatalogDirectory;
import jsky.catalog.gui.CatalogNavigator;
import jsky.catalog.gui.CatalogNavigatorOpener;
import jsky.catalog.gui.StoreImageServerAction;
import jsky.util.I18N;
import jsky.util.ProxyServerUtil;
import jsky.util.Preferences;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.ProxyServerDialog;

/**
 * Implements a standard catalog menu with separate submenus for different
 * catalog types.
 *
 * @version $Revision: 7122 $
 * @author Allan Brighton
 */
public class NavigatorCatalogMenu extends JMenu implements TreeModelListener {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(NavigatorCatalogMenu.class);

    /** Object responsible for loading the sky image */
    private CatalogImageDisplay _opener;

    /** Image server submenu */
    private JMenu _imageServerMenu;

    /** The "Proxy Settings" menu item */
    private JMenuItem _proxyMenuItem;

    // This restores any proxy settings from a previous session
    static {
        ProxyServerUtil.init();
    }

    /**
     * Create the menubar for the given main image display.
     *
     * @param opener the object responsible for creating and displaying the catalog window
     */
    public NavigatorCatalogMenu(CatalogImageDisplay opener) {
        super(_I18N.getString("catalog"));
        _opener = opener;

        addMenuItems();
    }

    /** Add the catalog menu items. */
    public void addMenuItems() {
        CatalogDirectory dir;
        try {
            dir = CatalogNavigator.getCatalogDirectory();
        } catch (Exception e) {
            DialogUtil.error(e);
            return;
        }

        // update menu when the config file changes
        dir.removeTreeModelListener(this);
        dir.addTreeModelListener(this);

        _imageServerMenu = _createCatalogSubMenu(this, _imageServerMenu, true, dir, _I18N.getString("imageServers"));

        if (_proxyMenuItem == null) {
            add(_proxyMenuItem = _createProxySettingsMenuItem());
        }
    }

    /**
     * Create and return a submenu listing catalogs of the given type.
     *
     * @param parentMenu the menu to add the new menu to
     * @param oldMenu if not null, update this menu, otherwise create a new one
     * @param clearMenu if true and oldMenu is not null, clear out the old menu, otherwise add to it
     * @param dir the catalog directory (config file) reference
     * @param label the label for the submenu
     * @return the ne or updated menu
     */
    private JMenu _createCatalogSubMenu(JMenu parentMenu, JMenu oldMenu, boolean clearMenu,
                                        CatalogDirectory dir, String label) {
        JMenu menu = oldMenu;
        if (menu == null) {
            menu = new JMenu(label);
            parentMenu.add(menu);
        } else if (clearMenu) {
            menu.removeAll();
        }

        if (dir == null) {
            return menu;
        }
        int n = dir.getNumCatalogs();
        ButtonGroup b = new ButtonGroup();
        Option<Catalog> userCat = _getUserCatalog();
        for (int i = 0; i < n; i++) {
            Catalog cat = dir.getCatalog(i);
            if (cat.isImageServer()) {
                JMenuItem mi = _createCatalogMenuItem(cat);
                menu.add(mi);
                b.add(mi);
                //Mark this catalog as selected if this is the catalog selected previously by the user
                userCat.filter(c -> c.getName().equals(cat.getName())).foreach(c -> mi.setSelected(true));
            }
        }
        return menu;
    }

    /**
     * Create a menu item for accessing a specific catalog.
     */
    private JMenuItem _createCatalogMenuItem(final Catalog cat) {
        StoreImageServerAction a = StoreImageServerAction.getAction(cat);
        final JMenuItem menuItem = new JRadioButtonMenuItem(a);
        menuItem.setText(cat.getName());
        a.appendValue("MenuItem", menuItem);
        menuItem.addActionListener(ae -> {
            // First save the preference, then load the image
            a.actionPerformed(ae);
            _opener.loadSkyImage();
        });
        return menuItem;
    }

     private Option<Catalog> _getUserCatalog() {
        String catalogProperties = Preferences.get(Catalog.SKY_USER_CATALOG, Catalog.DEFAULT_IMAGE_SERVER);
        String args[] = catalogProperties.split("\\*");
        if (args.length <= 0) return None.instance();

        Catalog c = CatalogNavigator.getCatalogDirectory().getCatalog(args[0]);

        if (c == null || !c.isImageServer()) return None.instance();

        return new Some<>(c);
    }


    /**
     * Create the Catalog => "Proxy Settings..." menu item
     */
    private JMenuItem _createProxySettingsMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("proxySettings"));
        menuItem.addActionListener(ae -> {
            ProxyServerDialog proxyDialog = new ProxyServerDialog();
            proxyDialog.setVisible(true);
        });
        return menuItem;
    }

    // -- implement the TreeModelListener interface
    // (so we can update the menus whenever the catalog tree is changed)

    public void treeNodesChanged(TreeModelEvent e) {
        addMenuItems();
    }

    public void treeNodesInserted(TreeModelEvent e) {
        addMenuItems();
    }

    public void treeNodesRemoved(TreeModelEvent e) {
        addMenuItems();
    }

    public void treeStructureChanged(TreeModelEvent e) {
        addMenuItems();
    }
}
