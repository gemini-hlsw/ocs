package jsky.navigator;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

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
@Deprecated
public class NavigatorCatalogMenu extends JMenu implements TreeModelListener {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(NavigatorCatalogMenu.class);

    /** Object responsible for creating and/or displaying the catalog window. */
    private CatalogNavigatorOpener _opener;

    /** Set to true if this menu is in the Catalog window menubar (doesn't have a Browse item) */
    private boolean _isInCatalogWindow = false;

    /** Catalog submenu */
    private JMenu _catalogMenu;

    /** Image server submenu */
    private JMenu _imageServerMenu;

    /** Local Catalog submenu */
    private JMenu _localCatalogMenu;

    /** The "Browse" menu item */
    private JMenuItem _browseMenuItem;

    /** The "New Browse" menu item */
    private JMenuItem _newBrowseMenuItem;

    /** The "Proxy Settings" menu item */
    private JMenuItem _proxyMenuItem;

    // Dialog window for proxy server settings
    private ProxyServerDialog _proxyDialog;

    // This restores any proxy settings from a previous session
    static {
        ProxyServerUtil.init();
    }

    /**
     * Create the menubar for the given main image display.
     *
     * @param opener the object responsible for creating and displaying the catalog window
     * @param addBrowseItem if true, add the "Browse" menu item
     */
    public NavigatorCatalogMenu(CatalogNavigatorOpener opener, boolean addBrowseItem) {
        super(_I18N.getString("catalog"));
        _opener = opener;
        _isInCatalogWindow = !addBrowseItem;

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

        _catalogMenu = _createCatalogSubMenu(this, _catalogMenu, true, dir, Catalog.CATALOG, _I18N.getString("catalogs"));

        _imageServerMenu = _createCatalogSubMenu(this, _imageServerMenu, true, dir, Catalog.IMAGE_SERVER, _I18N.getString("imageServers"));

        _localCatalogMenu = _createCatalogSubMenu(this, _localCatalogMenu, true, dir, Catalog.LOCAL, _I18N.getString("localCats"));
        _localCatalogMenu.addSeparator();
        _localCatalogMenu.add(_createCatalogLocalOpenMenuItem());

        if (!_isInCatalogWindow && _browseMenuItem == null) {
            addSeparator();
            add(_browseMenuItem = _createCatalogBrowseMenuItem());
            add(_newBrowseMenuItem = _createCatalogNewBrowseMenuItem());
        }

        if (_proxyMenuItem == null) {
            addSeparator();
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
     * @param servType the server type string for the catalogs that should be in the menu
     * @param label the label for the submenu
     * @return the ne or updated menu
     */
    private JMenu _createCatalogSubMenu(JMenu parentMenu, JMenu oldMenu, boolean clearMenu,
                                        CatalogDirectory dir, String servType,
                                        String label) {
        JMenu menu = oldMenu;
        if (menu == null) {
            menu = new JMenu(label);
            parentMenu.add(menu);
        } else if (clearMenu) {
            menu.removeAll();
        }

        if (dir == null) {
            System.out.println("XXX null config file");
            return menu;
        }
        int n = dir.getNumCatalogs();
        ButtonGroup b = new ButtonGroup();
        Catalog userCat = _getUserCatalog();
        for (int i = 0; i < n; i++) {
            Catalog cat = dir.getCatalog(i);
            if (cat.getType().equals(servType)) {
                JMenuItem mi;
                menu.add(mi = _createCatalogMenuItem(cat));
                if (cat.isImageServer()) {
                    b.add(mi);
                    //Mark this catalog as selected if this is the catalog selected previously by the user
                    if (userCat != null) {
                        if (userCat.getName().equals(cat.getName())) {
                            mi.setSelected(true);
                        }
                    }
                }
            }
        }
        return menu;
    }

    /**
     * Create a menu item for accessing a specific catalog.
     */
    private JMenuItem _createCatalogMenuItem(final Catalog cat) {
        final JMenuItem menuItem;
        if (cat.isImageServer()) {
            StoreImageServerAction a;
            menuItem = new JRadioButtonMenuItem(a = StoreImageServerAction.getAction(cat));
            menuItem.setText(cat.getName());
            a.appendValue("MenuItem", menuItem);
        } else {
            menuItem = new JMenuItem(cat.getName());
        }
        menuItem.addActionListener(ae -> _opener.openCatalogWindow(cat));
        return menuItem;
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

    /**
     * Create the Catalog => "Local Catalogs" => "Open..." menu item
     */
    private JMenuItem _createCatalogLocalOpenMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("open") + "...");
        menuItem.addActionListener(ae -> _opener.openLocalCatalog());
        return menuItem;
    }

    /**
     * Create the Catalog => "Browse..." menu item
     */
    private JMenuItem _createCatalogBrowseMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("browse") + "...");
        //menuItem.addActionListener(ae -> _opener.openCatalogWindow());
        return menuItem;
    }

    /**
     * Create the Catalog => "New Browse..." menu item
     */
    private JMenuItem _createCatalogNewBrowseMenuItem() {
        JMenuItem menuItem = new JMenuItem("New Browse ...");
        //menuItem.addActionListener(ae -> _opener.openCatalogWindow());
        return menuItem;
    }

    /**
     * Create the Catalog => "Proxy Settings..." menu item
     */
    private JMenuItem _createProxySettingsMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("proxySettings"));
        menuItem.addActionListener(ae -> {
            if (_proxyDialog == null)
                _proxyDialog = new ProxyServerDialog();
            _proxyDialog.setVisible(true);
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
