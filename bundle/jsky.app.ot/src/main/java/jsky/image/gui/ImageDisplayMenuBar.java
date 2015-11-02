package jsky.image.gui;

import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import jsky.image.ImageChangeEvent;
import jsky.image.graphics.gui.ImageGraphicsMenu;
import jsky.util.I18N;
import jsky.util.Preferences;
import jsky.util.gui.GenericToolBar;

/**
 * Implements a menu bar for an ImageDisplayControl.
 *
 * @author Allan Brighton
 */
public class ImageDisplayMenuBar extends JMenuBar {
    // Used to access internationalized strings (see i18n/gui*.properties)
    private static final I18N _I18N = I18N.getInstance(ImageDisplayMenuBar.class);

    /** Maximum scale (zoom) factor for menu. **/
    public static final int   MAX_SCALE = 20;

    /** Minimum scale (zoom) factor for menu. **/
    public static final float MIN_SCALE = 1.0F / MAX_SCALE;

    // Used to format magnification settings < 1.
    private static final NumberFormat _scaleFormat = NumberFormat.getInstance(Locale.US);
    static {
        _scaleFormat.setMaximumFractionDigits(1);
    }

    // Target image window
    private final DivaMainImageDisplay _imageDisplay;

    // The current image window (for the Go/history menu, which may be shared by
    // multiple image displays)
    private static DivaMainImageDisplay _currentImageDisplay;

    // The toolbar associated with the image display
    private final GenericToolBar _toolBar;

    /** Handles for the menus. **/
    private final JMenu _fileMenu;
    private final JMenu _viewMenu;
    private final JMenu _goMenu;
    private final JMenu _graphicsMenu;

    /** View menu items needed externally. **/
    private final JMenuItem _imagePropertiesMenuItem;
    private final JMenuItem _fitsKeywordsMenuItem;
    private final JMenuItem _pickObjectMenuItem;


    /**
     * Create the menubar for the given main image display.
     *
     * @param imageDisplay the target image display
     * @param toolBar the toolbar associated with this menubar (shares some actions)
     */
    public ImageDisplayMenuBar(final DivaMainImageDisplay imageDisplay, final GenericToolBar toolBar) {
        super();
        _imageDisplay = imageDisplay;
        _toolBar = toolBar;

        /** FILE MENU **/
        _fileMenu = createFileMenu();
        add(_fileMenu);

        /** VIEW MENU **/
        _imagePropertiesMenuItem = createViewImagePropertiesMenuItem();
        _fitsKeywordsMenuItem    = createViewFitsKeywordsMenuItem();
        _pickObjectMenuItem      = createViewPickObjectMenuItem();
        add(_viewMenu = createViewMenu());

        add(_goMenu = createGoMenu(null));
        add(_graphicsMenu = new ImageGraphicsMenu(imageDisplay.getCanvasDraw()));

        // Arrange to always set the current image display for use by the ImageHistoryItem class,
        // since the same items may be in the menus of multiple image displays
        _goMenu.addMenuListener(new MenuListener() {
            public void menuSelected(MenuEvent e) {
                _currentImageDisplay = imageDisplay;
            }

            public void menuDeselected(MenuEvent e) {
            }

            public void menuCanceled(MenuEvent e) {
            }
        });

        // keep the Go history menu up to date
        imageDisplay.addChangeListener(ce -> {
            final ImageChangeEvent e = (ImageChangeEvent) ce;
            if (e.isNewImage() && !e.isBefore()) {
                _goMenu.removeAll();
                createGoMenu(_goMenu);

                // enable/disable some items
                if (imageDisplay.getFitsImage() != null) {
                    _fitsKeywordsMenuItem.setEnabled(true);
                    _pickObjectMenuItem.setEnabled(true);
                    _imagePropertiesMenuItem.setEnabled(false);
                } else {
                    _fitsKeywordsMenuItem.setEnabled(false);
                    _pickObjectMenuItem.setEnabled(false);
                    _imagePropertiesMenuItem.setEnabled(true);
                }
            }
        });
    }

    /**
     * Return the current image window (for the Go/history menu, which may be shared by
     * multiple image displays);
     */
    public static DivaMainImageDisplay getCurrentImageDisplay() {
        return _currentImageDisplay;
    }

    /**
     * Set the current image window (for the Go/history menu, which may be shared by
     * multiple image displays);
     */
    public static void setCurrentImageDisplay(DivaMainImageDisplay imageDisplay) {
        _currentImageDisplay = imageDisplay;
    }


    /**
     * Create the File menu.
     */
    protected JMenu createFileMenu() {
        final JMenu menu = new JMenu(_I18N.getString("file"));
        menu.add(_imageDisplay.getOpenAction());
        menu.add(createFileOpenURLMenuItem());
        menu.addSeparator();
        menu.add(createFileClearImageMenuItem());
        menu.addSeparator();
        menu.add(_imageDisplay.getSaveAction());
        menu.add(_imageDisplay.getSaveAsAction());
        menu.addSeparator();
        menu.add(_imageDisplay.getPrintPreviewAction());
        menu.add(_imageDisplay.getPrintAction());
        menu.addSeparator();
        menu.add(createFileCloseMenuItem());
        return menu;
    }


    /**
     * Create the File => "Open URL" menu item
     */
    protected JMenuItem createFileOpenURLMenuItem() {
        final JMenuItem menuItem = new JMenuItem(_I18N.getString("openURL"));
        menuItem.addActionListener(ae -> _imageDisplay.openURL());
        return menuItem;
    }

    /**
     * Create the File => Clear Image menu item
     */
    protected JMenuItem createFileClearImageMenuItem() {
        final JMenuItem menuItem = new JMenuItem(_I18N.getString("clearImage"));
        menuItem.addActionListener(ae -> _imageDisplay.clear());
        return menuItem;
    }


    /**
     * Create the File => Close menu item
     */
    protected JMenuItem createFileCloseMenuItem() {
        final JMenuItem menuItem = new JMenuItem(_I18N.getString("close"));
        menuItem.addActionListener(ae -> _imageDisplay.close());
        return menuItem;
    }


    /**
     * Create the View menu.
     */
    protected JMenu createViewMenu() {
        final JMenu menu = new JMenu(_I18N.getString("view"));
        menu.add(createViewToolBarMenuItem());
        menu.add(new ImageShowToolbarAsMenu(_toolBar));
        menu.addSeparator();
        menu.add(_imageDisplay.getColorsAction());
        menu.add(_imageDisplay.getCutLevelsAction());
        menu.add(_pickObjectMenuItem);
        menu.add(_fitsKeywordsMenuItem);
        menu.add(_imagePropertiesMenuItem);
        menu.addSeparator();
        menu.add(new ImageScaleMenu(_imageDisplay));
        menu.add(new ImageScaleInterpolationMenu(_imageDisplay));
        menu.add(createViewSmoothScrollingMenuItem());
        return menu;
    }

    /**
     * Create the View => "Toolbar" menu item
     */
    protected JCheckBoxMenuItem createViewToolBarMenuItem() {
        final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(_I18N.getString("toolbar"));
        final String prefName = getClass().getName() + ".ShowToolBar";

        menuItem.addItemListener(e -> {
            final JCheckBoxMenuItem rb = (JCheckBoxMenuItem) e.getSource();
            _toolBar.setVisible(rb.getState());
            Preferences.set(prefName, rb.getState());
        });

        menuItem.setState(Preferences.get(prefName, true));
        return menuItem;
    }


    /**
     * Create the View => "Pick Object" menu item
     */
    protected JMenuItem createViewPickObjectMenuItem() {
        final JMenuItem menuItem = new JMenuItem(_I18N.getString("pickObjects"));
        menuItem.addActionListener(ae -> _imageDisplay.pickObject());
        return menuItem;
    }


    /**
     * Create the View => "FITS Keywords"  menu item
     */
    protected JMenuItem createViewFitsKeywordsMenuItem() {
        final JMenuItem menuItem = new JMenuItem(_I18N.getString("fitsKeywords"));
        menuItem.addActionListener(ae -> _imageDisplay.viewFitsKeywords());
        return menuItem;
    }


    /**
     * Create the View => "Image Properties"  menu item
     */
    protected JMenuItem createViewImagePropertiesMenuItem() {
        final JMenuItem menuItem = new JMenuItem(_I18N.getString("imageProps"));
        menuItem.addActionListener(ae -> _imageDisplay.viewImageProperties());
        return menuItem;
    }


    /**
     * Create the View => "Smooth Scrolling"  menu item
     */
    protected JCheckBoxMenuItem createViewSmoothScrollingMenuItem() {
        final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(_I18N.getString("smoothScrolling"));
        final String prefName = getClass().getName() + ".SmoothScrolling";

        menuItem.addItemListener(e -> {
            final JCheckBoxMenuItem rb = (JCheckBoxMenuItem) e.getSource();
            _imageDisplay.setImmediateMode(rb.getState());
            _imageDisplay.updateImage();
            Preferences.set(prefName, rb.getState());
        });

        menuItem.setState(Preferences.get(prefName, true));
        return menuItem;
    }


    /**
     * Create or update the Go (history) menu.
     */
    protected JMenu createGoMenu(final JMenu oldMenu) {
        final JMenu menu = oldMenu != null ? oldMenu : new JMenu(_I18N.getString("go"));
        menu.add(_imageDisplay.getBackAction());
        menu.add(_imageDisplay.getForwAction());
        menu.addSeparator();
        _imageDisplay.addHistoryMenuItems(menu);
        menu.addSeparator();
        menu.add(createGoClearHistoryMenuItem());
        return menu;
    }


    /**
     * Create the Go => "Clear History" menu item.
     */
    protected JMenuItem createGoClearHistoryMenuItem() {
        final JMenuItem menuItem = new JMenuItem(_I18N.getString("clearHistory"));
        menuItem.addActionListener(ae -> {
            _imageDisplay.clearHistory();
            _goMenu.removeAll();
            createGoMenu(_goMenu);
        });
        return menuItem;
    }


    /**
     * Get the scale menu label for the given float scale factor.
     */
    public static String getScaleLabel(float f) {
        if (f < 1.0) {
            final int i = Math.round(1.0F / f);
            return "1/" + i + "x";
        }
        return Integer.toString(Math.round(f)) + "x";
    }

    /** Return the target image window */
    public DivaMainImageDisplay getImageDisplay() {
        return _imageDisplay;
    }

    /** Return the handle for the File menu */
    public JMenu getFileMenu() {
        return _fileMenu;
    }

    /** Return the handle for the View menu */
    public JMenu getViewMenu() {
        return _viewMenu;
    }

    /** Return the handle for the Go menu */
    public JMenu getGoMenu() {
        return _goMenu;
    }

    /** Return the handle for the Graphics menu */
    public JMenu getGraphicsMenu() {
        return _graphicsMenu;
    }

    /** Return the Pick Object menu item */
    public JMenuItem getPickObjectMenuItem() {
        return _pickObjectMenuItem;
    }
}





