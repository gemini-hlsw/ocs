package jsky.catalog.gui;

import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import jsky.catalog.MemoryCatalog;
import jsky.catalog.TableQueryResult;
import jsky.util.I18N;
import jsky.util.Preferences;
import jsky.util.gui.GenericToolBar;

/**
 * Implements a menubar for a CatalogNavigator.
 */
@Deprecated
public class CatalogNavigatorMenuBar extends JMenuBar {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(CatalogNavigatorMenuBar.class);

    // names used to store settings in user preferences
    private final String _className = getClass().getName();
    private final String _viewToolBarPrefName = _className + ".ShowToolBar";
    private final String _showToolBarAsPrefName = _className + ".ShowToolBarAs";

    // Target panel
    private CatalogNavigator _navigator;

    // The toolbar associated with the image display
    private GenericToolBar _toolBar;

    // Handle for the Go menu
    private JMenu _goMenu;

    // Handle for the Query menu
    private JMenu _queryMenu;

    // Toggle menu item needs to be updated based on current display
    private JCheckBoxMenuItem _tableCellsEditableMenuItem;

    // The current catalog window (for the Go/history menu, which may be shared by
    // multiple windows)
    private static CatalogNavigator _currentCatalogNavigator;


    /**
     * Create the menubar for the given CatalogNavigator panel
     */
    public CatalogNavigatorMenuBar(CatalogNavigator navigator, GenericToolBar toolBar) {
        super();
        _navigator = navigator;
        _toolBar = toolBar;
        add(createFileMenu());
        add(createViewMenu());
        add(_goMenu = createGoMenu(null));
        add(createCatalogMenu());
        add(createTableMenu());
        add(_queryMenu = createQueryMenu(null));

        // Arrange to always set the current window for use by the CatalogHistoryItem class,
        // since the same items may be in the menus of multiple catalog windows
        _goMenu.addMenuListener(new MenuListener() {
            public void menuSelected(MenuEvent e) {
                _currentCatalogNavigator = _navigator;
            }

            public void menuDeselected(MenuEvent e) {
            }

            public void menuCanceled(MenuEvent e) {
            }
        });

        // Receive notification whenever a new catalog is displayed
        _navigator.addChangeListener(e -> {
            // keep the Go history menu up to date
            _updateGoMenu();
        });

        // Keep the query menu up to date
        _queryMenu.addMenuListener(new MenuListener() {
            public void menuSelected(MenuEvent e) {
                _currentCatalogNavigator = _navigator;
                _updateQueryMenu();
            }

            public void menuDeselected(MenuEvent e) {
            }

            public void menuCanceled(MenuEvent e) {
            }
        });

    }


    /** Update the Go menu after a change in the component displayed in the catalog navigator */
    private void _updateGoMenu() {
        _goMenu.removeAll();
        createGoMenu(_goMenu);
    }


    /**
     * Return the current catalog window (for the Go/history menu, which may be shared by
     * multiple catalog windows);
     */
    public static CatalogNavigator getCurrentCatalogNavigator() {
        return _currentCatalogNavigator;
    }

    /**
     * Create the File menu.
     */
    protected JMenu createFileMenu() {
        JMenu menu = new JMenu(_I18N.getString("file"));
        menu.add(_navigator.getOpenAction());
        menu.addSeparator();
        menu.add(createFileOpenURLMenuItem());
        menu.add(createFileClearMenuItem());
        menu.addSeparator();
        menu.add(_navigator.getSaveAsAction());
        menu.add(_navigator.getSaveWithImageAction());
        menu.add(_navigator.getSaveAsHTMLAction());
        menu.add(_navigator.getPrintAction());
        menu.addSeparator();

        menu.add(createFileCloseMenuItem());

        return menu;
    }


    /**
     * Create the File => "Open URL" menu item
     */
    protected JMenuItem createFileOpenURLMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("openURL") + "...");
        menuItem.addActionListener(ae -> _navigator.openURL());
        return menuItem;
    }

    /**
     * Create the File => Clear menu item
     */
    protected JMenuItem createFileClearMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("clear"));
        menuItem.addActionListener(ae -> _navigator.clear());
        return menuItem;
    }

    /**
     * Create the File => Close menu item
     */
    protected JMenuItem createFileCloseMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("close"));
        menuItem.addActionListener(ae -> _navigator.close());
        return menuItem;
    }

    /**
     * Create the View menu.
     */
    protected JMenu createViewMenu() {
        JMenu menu = new JMenu(_I18N.getString("view"));
        menu.add(createViewToolBarMenuItem());
        menu.add(createViewShowToolBarAsMenu());

        return menu;
    }

    /**
     * Create the View => "Toolbar" menu item
     */
    protected JCheckBoxMenuItem createViewToolBarMenuItem() {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(_I18N.getString("toolbar"));

        menuItem.addItemListener(e -> {
            JCheckBoxMenuItem rb = (JCheckBoxMenuItem) e.getSource();
            _toolBar.setVisible(rb.isSelected());
            if (rb.isSelected())
                Preferences.set(_viewToolBarPrefName, "true");
            else
                Preferences.set(_viewToolBarPrefName, "false");
        });

        // check for a previous preference setting
        String pref = Preferences.get(_viewToolBarPrefName);
        if (pref != null && pref.equals("true")) {
            menuItem.setSelected(true);
        } else {
            menuItem.setSelected(false);
            _toolBar.setVisible(false);
        }

        return menuItem;
    }

    /**
     * Create the View => "Show Toolbar As" menu
     */
    protected JMenu createViewShowToolBarAsMenu() {
        JMenu menu = new JMenu(_I18N.getString("showToolBarAs"));

        JRadioButtonMenuItem b1 = new JRadioButtonMenuItem(_I18N.getString("picAndText"));
        JRadioButtonMenuItem b2 = new JRadioButtonMenuItem(_I18N.getString("picOnly"));
        JRadioButtonMenuItem b3 = new JRadioButtonMenuItem(_I18N.getString("textOnly"));

        b1.setSelected(true);
        _toolBar.setShowPictures(true);
        _toolBar.setShowText(true);

        menu.add(b1);
        menu.add(b2);
        menu.add(b3);

        ButtonGroup group = new ButtonGroup();
        group.add(b1);
        group.add(b2);
        group.add(b3);

        ItemListener itemListener = e -> {
            JRadioButtonMenuItem rb = (JRadioButtonMenuItem) e.getSource();
            if (rb.isSelected()) {
                if (rb.getText().equals(_I18N.getString("picAndText"))) {
                    _toolBar.setShowPictures(true);
                    _toolBar.setShowText(true);
                    Preferences.set(_showToolBarAsPrefName, "1");
                } else if (rb.getText().equals(_I18N.getString("picOnly"))) {
                    _toolBar.setShowPictures(true);
                    _toolBar.setShowText(false);
                    Preferences.set(_showToolBarAsPrefName, "2");
                } else if (rb.getText().equals(_I18N.getString("textOnly"))) {
                    _toolBar.setShowPictures(false);
                    _toolBar.setShowText(true);
                    Preferences.set(_showToolBarAsPrefName, "3");
                }
            }
        };

        b1.addItemListener(itemListener);
        b2.addItemListener(itemListener);
        b3.addItemListener(itemListener);

        // check for a previous preference setting
        String pref = Preferences.get(_showToolBarAsPrefName);
        if (pref != null) {
            JRadioButtonMenuItem[] ar = new JRadioButtonMenuItem[]{null, b1, b2, b3};
            try {
                ar[Integer.parseInt(pref)].setSelected(true);
            } catch (Exception e) {
                // Ignore
            }
        }

        return menu;
    }
    /**
     * Create the Go menu.
     */
    protected JMenu createGoMenu(JMenu menu) {
        if (menu == null)
            menu = new JMenu(_I18N.getString("go"));

        menu.add(_navigator.getBackAction());
        menu.add(_navigator.getForwAction());
        menu.addSeparator();
        _navigator.addHistoryMenuItems(menu);
        menu.addSeparator();
        menu.add(createGoClearHistoryMenuItem());

        return menu;
    }

    /**
     * Create the Go => "Clear History" menu item.
     */
    protected JMenuItem createGoClearHistoryMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("clearHistory"));
        menuItem.addActionListener(ae -> {
            _navigator.clearHistory();
            _goMenu.removeAll();
            createGoMenu(_goMenu);
        });
        return menuItem;
    }

    /** Add a catalog menu to the catalog navigator frame */
    protected JMenu createCatalogMenu() {
        // to be defined in a derived class
        return new JMenu(_I18N.getString("catalog"));
    }

    /**
     * Create the Table menu.
     */
    protected JMenu createTableMenu() {
        JMenu menu = new JMenu(_I18N.getString("table"));
        menu.add(_navigator.getAddRowAction());
        menu.add(_navigator.getDeleteSelectedRowsAction());

        menu.addSeparator();
        menu.add(_tableCellsEditableMenuItem = createTableCellsEditableMenuItem());

        // Enable/disable/update the previous checkbutton
        _navigator.addChangeListener(e -> {
            JComponent c = _navigator.getResultComponent();
            if (c instanceof TableDisplayTool) {
                _tableCellsEditableMenuItem.setEnabled(true);
                TableQueryResult queryResult = ((TableDisplayTool) c).getTable();
                if (queryResult instanceof MemoryCatalog) {
                    MemoryCatalog cat = (MemoryCatalog) queryResult;
                    _tableCellsEditableMenuItem.setSelected(!cat.isReadOnly());
                }
            } else {
                _tableCellsEditableMenuItem.setSelected(false);
                _tableCellsEditableMenuItem.setEnabled(false);
            }
        });

        return menu;
    }

    /**
     * Create the Table => "Editable Table Cells" menu item
     */
    protected JCheckBoxMenuItem createTableCellsEditableMenuItem() {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(_I18N.getString("editableTableCells"));

        menuItem.addItemListener(e -> {
            JCheckBoxMenuItem rb = (JCheckBoxMenuItem) e.getSource();
            _navigator.setTableCellsEditable(rb.isSelected());
        });

        return menuItem;
    }


    /** Update the Query menu after a change in the list of available stored queries */
    private void _updateQueryMenu() {
        _queryMenu.removeAll();
        createQueryMenu(_queryMenu);
    }


    /**
     * Create the Query menu.
     */
    protected JMenu createQueryMenu(JMenu menu) {
        if (menu == null)
            menu = new JMenu(_I18N.getString("query"));

        menu.add(createQueryStoreMenu());
        menu.add(createQueryDeleteMenu());
        menu.addSeparator();
        _navigator.addQueryMenuItems(menu, null);

        return menu;
    }

    /**
     * Create the Query => Store menu.
     */
    protected JMenu createQueryStoreMenu() {
        JMenu menu = new JMenu(_I18N.getString("storeQuery"));
        menu.add(_navigator.getStoreNewQueryAction());
        menu.addSeparator();

        ActionListener l = e -> {
            JMenuItem menuItem = (JMenuItem) e.getSource();
            String name = menuItem.getText();
            _navigator.storeQuery(name);
        };
        _navigator.addQueryMenuItems(menu, l);
        return menu;
    }

    /**
     * Create the Query => Delete menu.
     */
    protected JMenu createQueryDeleteMenu() {
        JMenu menu = new JMenu(_I18N.getString("deleteQuery"));
        menu.add(_navigator.getDeleteAllQueryAction());
        menu.addSeparator();
        ActionListener l = e -> {
            JMenuItem menuItem = (JMenuItem) e.getSource();
            String name = menuItem.getText();
            _navigator.deleteQuery(name);
        };
        _navigator.addQueryMenuItems(menu, l);
        return menu;
    }


    /** Return the catalog navigator panel */
    public CatalogNavigator getNavigator() {
        return _navigator;
    }

    /** Return the toolbar associated with the image display */
    public GenericToolBar getToolBar() {
        return _toolBar;
    }

}
