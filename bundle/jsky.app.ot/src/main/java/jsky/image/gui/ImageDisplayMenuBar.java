/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 */

package jsky.image.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDesktopPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
    public static final float MAX_SCALE = 20.0F;

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

    /** File menu items needed externally. **/
    private final JMenuItem _newWindowMenuItem;

    /** View menu items needed externally. **/
    private final JMenuItem _imagePropertiesMenuItem;
    private final JMenuItem _fitsKeywordsMenuItem;
    private final JMenuItem _fitsExtensionsMenuItem;
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
        _newWindowMenuItem = createFileNewWindowMenuItem();
        _fileMenu = createFileMenu();
        add(_fileMenu);

        /** VIEW MENU **/
        _imagePropertiesMenuItem = createViewImagePropertiesMenuItem();
        _fitsKeywordsMenuItem    = createViewFitsKeywordsMenuItem();
        _fitsExtensionsMenuItem  = createViewFitsKeywordsMenuItem();
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
        imageDisplay.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent ce) {
                ImageChangeEvent e = (ImageChangeEvent) ce;
                if (e.isNewImage() && !e.isBefore()) {
                    _goMenu.removeAll();
                    createGoMenu(_goMenu);

                    // enable/disable some items
                    if (imageDisplay.getFitsImage() != null) {
                        _fitsExtensionsMenuItem.setEnabled(true);
                        _fitsKeywordsMenuItem.setEnabled(true);
                        _pickObjectMenuItem.setEnabled(true);
                        _imagePropertiesMenuItem.setEnabled(false);
                    } else {
                        _fitsExtensionsMenuItem.setEnabled(false);
                        _fitsKeywordsMenuItem.setEnabled(false);
                        _pickObjectMenuItem.setEnabled(false);
                        _imagePropertiesMenuItem.setEnabled(true);
                    }
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
        JMenu menu = new JMenu(_I18N.getString("file"));
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
        menu.add(_newWindowMenuItem);
        menu.add(createFileCloseMenuItem());

        // check if using internal frames before adding exit item
        JDesktopPane desktop = _imageDisplay.getDesktop();
        if (desktop == null && _imageDisplay.isMainWindow())
            menu.add(createFileExitMenuItem());

        return menu;
    }


    /**
     * Create the File => "Open URL" menu item
     */
    protected JMenuItem createFileOpenURLMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("openURL"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.openURL();
            }
        });
        return menuItem;
    }

    /**
     * Create the File => Clear Image menu item
     */
    protected JMenuItem createFileClearImageMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("clearImage"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.clear();
            }
        });
        return menuItem;
    }


    /**
     * Create the File => "New Window" menu item
     */
    protected JMenuItem createFileNewWindowMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("newWindow"));
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.newWindow();
            }
        });
        return menuItem;
    }


    /**
     * Create the File => Exit menu item
     */
    protected JMenuItem createFileExitMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("exit"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.exit();
            }
        });
        return menuItem;
    }


    /**
     * Create the File => Close menu item
     */
    protected JMenuItem createFileCloseMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("close"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.close();
            }
        });
        return menuItem;
    }

//    /**
//     * Create the Edit menu.
//     */
//    protected JMenu createEditMenu() {
//        JMenu menu = new JMenu(_I18N.getString("edit"));
//        menu.add(createEditPreferencesMenuItem());
//        return menu;
//    }

//    /**
//     * Create the Edit => "Preferences" menu item
//     */
//    protected JMenuItem createEditPreferencesMenuItem() {
//        JMenuItem menuItem = new JMenuItem(_I18N.getString("preferences"));
//        menuItem.addActionListener(new ActionListener() {
//
//            public void actionPerformed(ActionEvent ae) {
//                DialogUtil.error("Sorry, not implemented...");
//                //_imageDisplay.editPreferences();
//            }
//        });
//        return menuItem;
//    }

    /**
     * Create the View menu.
     */
    protected JMenu createViewMenu() {
        JMenu menu = new JMenu(_I18N.getString("view"));
        menu.add(createViewToolBarMenuItem());
        menu.add(new ImageShowToolbarAsMenu(_toolBar));
        menu.addSeparator();

        menu.add(_imageDisplay.getColorsAction());
        menu.add(_imageDisplay.getCutLevelsAction());

        menu.add(_pickObjectMenuItem);
        menu.add(_fitsExtensionsMenuItem);
        menu.add(_fitsKeywordsMenuItem);
        menu.add(_imagePropertiesMenuItem);
        menu.addSeparator();

        menu.add(new ImageScaleMenu(_imageDisplay));
        menu.add(new ImageScaleInterpolationMenu(_imageDisplay));

        // XXX doesn't currently work well with the non-square images pan window
        //menu.add(createViewRotateMenu());

        // XXX Works okay for jskycat, but not supported by OT yet
        //menu.add(createViewFlipXMenuItem());
        //menu.add(createViewFlipYMenuItem());

        menu.add(createViewSmoothScrollingMenuItem());
        return menu;
    }

    /**
     * Create the View => "Toolbar" menu item
     */
    protected JCheckBoxMenuItem createViewToolBarMenuItem() {
        final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(_I18N.getString("toolbar"));
        final String prefName = getClass().getName() + ".ShowToolBar";

        menuItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                JCheckBoxMenuItem rb = (JCheckBoxMenuItem) e.getSource();
                _toolBar.setVisible(rb.getState());
                Preferences.set(prefName, rb.getState());
            }
        });

        menuItem.setState(Preferences.get(prefName, true));
        return menuItem;
    }


//    /**
//     * Create the View => "Cut Levels" menu item
//     */
//    protected JMenuItem createViewCutLevelsMenuItem() {
//        JMenuItem menuItem = new JMenuItem(_I18N.getString("cutLevels") + "...");
//        menuItem.addActionListener(new ActionListener() {
//
//            public void actionPerformed(ActionEvent ae) {
//                _imageDisplay.editCutLevels();
//            }
//        });
//        return menuItem;
//    }


//    /**
//     * Create the View => "Colors" menu item
//     */
//    protected JMenuItem createViewColorsMenuItem() {
//        JMenuItem menuItem = new JMenuItem(_I18N.getString("colors") + "...");
//        menuItem.addActionListener(new ActionListener() {
//
//            public void actionPerformed(ActionEvent ae) {
//                _imageDisplay.editColors();
//            }
//        });
//        return menuItem;
//    }

    /**
     * Create the View => "Pick Object" menu item
     */
    protected JMenuItem createViewPickObjectMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("pickObjects"));
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.pickObject();
            }
        });
        return menuItem;
    }

//    /**
//     * Create the View => "FITS Extensions"  menu item
//     */
//    protected JMenuItem createViewFitsExtensionsMenuItem() {
//        JMenuItem menuItem = new JMenuItem(_I18N.getString("fitsExt"));
//        menuItem.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent ae) {
//                _imageDisplay.viewFitsExtensions();
//            }
//        });
//        return menuItem;
//    }

    /**
     * Create the View => "FITS Keywords"  menu item
     */
    protected JMenuItem createViewFitsKeywordsMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("fitsKeywords"));
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.viewFitsKeywords();
            }
        });
        return menuItem;
    }

    /**
     * Create the View => "Image Properties"  menu item
     */
    protected JMenuItem createViewImagePropertiesMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("imageProps"));
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.viewImageProperties();
            }
        });
        return menuItem;
    }


//    /**
//     * Create the View => "Rotate"  menu item
//     */
//    protected JMenu createViewRotateMenu() {
//        JMenu menu = new JMenu("Rotate");
//
//        JRadioButtonMenuItem b1 = new JRadioButtonMenuItem("No Rotation");
//        JRadioButtonMenuItem b2 = new JRadioButtonMenuItem("  90 deg");
//        JRadioButtonMenuItem b3 = new JRadioButtonMenuItem(" 180 deg");
//        JRadioButtonMenuItem b4 = new JRadioButtonMenuItem(" -90 deg");
//        //JRadioButtonMenuItem b5 = new JRadioButtonMenuItem("  45 deg (XXX not impl)");
//
//        b1.setSelected(true);
//        menu.add(b1);
//        menu.add(b2);
//        menu.add(b3);
//        menu.add(b4);
//        //menu.add(b5);
//
//        ButtonGroup group = new ButtonGroup();
//        group.add(b1);
//        group.add(b2);
//        group.add(b3);
//        group.add(b4);
//        //group.add(b5);
//
//        ItemListener itemListener = new ItemListener() {
//            public void itemStateChanged(ItemEvent e) {
//                JRadioButtonMenuItem rb = (JRadioButtonMenuItem) e.getSource();
//                double rad = Math.PI / 180.;
//                ImageProcessor imageProcessor = _imageDisplay.getImageProcessor();
//                if (rb.isSelected()) {
//                    if (rb.getText().equals("No Rotation")) {
//                        imageProcessor.setAngle(0.0);
//                    } else if (rb.getText().equals("  90 deg")) {
//                        imageProcessor.setAngle(90.0 * rad);
//                    } else if (rb.getText().equals(" 180 deg")) {
//                        imageProcessor.setAngle(180.0 * rad);
//                    } else if (rb.getText().equals(" -90 deg")) {
//                        imageProcessor.setAngle(-90.0 * rad);
//                    }
//                    //else if (rb.getText().equals("  45 deg (XXX not impl)")) {
//                    //    imageProcessor.setAngle(45.0*rad);
//                    //}
//                    imageProcessor.update();
//                }
//            }
//        };
//
//        b1.addItemListener(itemListener);
//        b2.addItemListener(itemListener);
//        b3.addItemListener(itemListener);
//        b4.addItemListener(itemListener);
//        //b5.addItemListener(itemListener);
//
//        return menu;
//    }


//    /**
//     * Create the View => "Flip X"  menu item
//     */
//    protected JCheckBoxMenuItem createViewFlipXMenuItem() {
//        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem("Flip X");
//        menuItem.addItemListener(new ItemListener() {
//            public void itemStateChanged(ItemEvent e) {
//                JCheckBoxMenuItem rb = (JCheckBoxMenuItem) e.getSource();
//                ImageProcessor imageProcessor = _imageDisplay.getImageProcessor();
//                imageProcessor.setFlipX(rb.getState());
//                imageProcessor.update();
//            }
//        });
//
//        return menuItem;
//    }

//    /**
//     * Create the View => "Flip Y"  menu item
//     */
//    protected JCheckBoxMenuItem createViewFlipYMenuItem() {
//        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem("Flip Y");
//        menuItem.addItemListener(new ItemListener() {
//            public void itemStateChanged(ItemEvent e) {
//                JCheckBoxMenuItem rb = (JCheckBoxMenuItem) e.getSource();
//                ImageProcessor imageProcessor = _imageDisplay.getImageProcessor();
//                imageProcessor.setFlipY(rb.getState());
//                imageProcessor.update();
//            }
//        });
//
//        return menuItem;
//    }


    /**
     * Create the View => "Smooth Scrolling"  menu item
     */
    protected JCheckBoxMenuItem createViewSmoothScrollingMenuItem() {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(_I18N.getString("smoothScrolling"));
        final String prefName = getClass().getName() + ".SmoothScrolling";

        menuItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                JCheckBoxMenuItem rb = (JCheckBoxMenuItem) e.getSource();
                _imageDisplay.setImmediateMode(rb.getState());
                _imageDisplay.updateImage();
                Preferences.set(prefName, rb.getState());
            }
        });

        menuItem.setState(Preferences.get(prefName, true));
        return menuItem;
    }


    /**
     * Create or update the Go (history) menu.
     */
    protected JMenu createGoMenu(JMenu menu) {
        if (menu == null)
            menu = new JMenu(_I18N.getString("go"));

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
        JMenuItem menuItem = new JMenuItem(_I18N.getString("clearHistory"));
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.clearHistory();
                _goMenu.removeAll();
                createGoMenu(_goMenu);
            }
        });
        return menuItem;
    }


    /**
     * Get the scale menu label for the given float scale factor.
     */
    public static String getScaleLabel(float f) {
        if (f < 1.0) {
            int i = Math.round(1.0F / f);
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

    /** Return the File => "New Window" menu item */
    public JMenuItem getNewWindowMenuItem() {
        return _newWindowMenuItem;
    }

    /** Return the Pick Object menu item */
    public JMenuItem getPickObjectMenuItem() {
        return _pickObjectMenuItem;
    }
}





