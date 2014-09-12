/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TpeImageDisplayMenuBar.java 38003 2011-10-13 22:56:01Z abrighton $
 */

package jsky.app.ot.tpe;

import java.awt.event.*;

import javax.swing.*;

import jsky.app.ot.util.BasicPropertyList;
import jsky.app.ot.util.ChoiceProperty;
import jsky.app.ot.util.PropertyWatcher;
import jsky.navigator.NavigatorImageDisplayMenuBar;
import jsky.navigator.NavigatorImageDisplayToolBar;

/**
 * Extends the image display menubar by adding Gemini position editor features.
 *
 * @version $Revision: 38003 $
 * @author Allan Brighton
 */
public class TpeImageDisplayMenuBar extends NavigatorImageDisplayMenuBar {

    // Popup menu displayed over image
    private JPopupMenu _popupMenu;

    /**
     * Create the menubar for the given main image display.
     *
     * @param imageDisplay the target image display
     * @param toolBar the toolbar associated with this menubar (shares some actions)
     */
    public TpeImageDisplayMenuBar(TpeImageWidget imageDisplay, NavigatorImageDisplayToolBar toolBar) {
        super(imageDisplay, toolBar);

        // Add the guide star search item to the end of the catalog menu
        JMenu catalogMenu = getCatalogMenu();
        catalogMenu.addSeparator();
        catalogMenu.add(imageDisplay.getManualGuideStarAction());
        catalogMenu.add(imageDisplay.getAutoGuideStarAction());

        _popupMenu = new JPopupMenu();

        // Add a separator for some OT specific menu items to be added later
        getViewMenu().addSeparator();

        imageDisplay.addMouseListener(new MouseAdapter() {
            int _x, _y;

            public void mousePressed(MouseEvent e) {
                _x = e.getX();
                _y = e.getY();
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() && _x == e.getX() && _y == e.getY()) {
                    _popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    /**
     * Add a menu item to the View and popup menus to configure the given property list.
     * The layout and type of menus used depends on the contents of the property
     * list.
     */
    public void addPropertyConfigMenuItem(String name, BasicPropertyList pl) {
        _addPropertyConfigMenuItem(getViewMenu(), name, pl);
        _addPropertyConfigMenuItem(_popupMenu, name, pl);
    }

    // Add a menu item to the given parent menu for the given property list
    public void _addPropertyConfigMenuItem(JComponent parentMenu, String name, BasicPropertyList pl) {
        JMenu menu = new JMenu(name + " Display");
        parentMenu.add(menu);

        String[] propNames = pl.getPropertyNames();
        for (int i = 0; i < propNames.length; i++) {
            Object value = pl.getValue(propNames[i]);
            if (value instanceof Boolean) {
                menu.add(_createBooleanPropertyMenuItem(propNames[i], pl));
            } else if (value instanceof ChoiceProperty) {
                int selectedIndex = ((ChoiceProperty) value).getCurValue();
                if (selectedIndex < 0)
                    selectedIndex = 0;
                menu.add(_createChoicePropertyMenu(propNames[i], pl, selectedIndex));
            }
        }
    }


    /**
     * Create and return a checkbox menu item for the given name and boolean property.
     */
    private JCheckBoxMenuItem _createBooleanPropertyMenuItem(final String propertyName, final BasicPropertyList pl) {
        final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(propertyName);
        menuItem.setSelected(pl.getBoolean(propertyName, false));
        menuItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                pl.setBoolean(propertyName, menuItem.getState());
                pl.saveSettings();
            }
        });

        // keep view and popup menus in sync
        pl.addWatcher(new PropertyWatcher() {
            public void propertyChange(String propName) {
                if (propName.equals(propertyName)) {
                    boolean b = pl.getBoolean(propertyName, false);
                    if (menuItem.isSelected() != b) {
                        menuItem.setSelected(b);
                    }
                }
            }
        });

        return menuItem;
    }

    /**
     * Create and return a menu for the given name and choice propery.
     */
    private JMenu _createChoicePropertyMenu(final String propertyName, final BasicPropertyList pl,
                                            int selectedIndex) {

        final JMenu menu = new JMenu(propertyName);

        String[] choices = pl.getChoiceOptions(propertyName);
        final JRadioButtonMenuItem[] menuItems = new JRadioButtonMenuItem[choices.length];
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < choices.length; i++) {
            final JRadioButtonMenuItem b = new JRadioButtonMenuItem(choices[i]);
            menuItems[i] = b;
            final int buttonIndex = i;
            if (i == selectedIndex)
                b.setSelected(true);
            menu.add(b);
            group.add(b);
            b.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (b.isSelected()) {
                        pl.setChoice(propertyName, buttonIndex);
                        pl.saveSettings();
                    }
                }
            });
        }

        // keep view and popup menus in sync
        pl.addWatcher(new PropertyWatcher() {
            public void propertyChange(String propName) {
                if (propName.equals(propertyName)) {
                    int index = pl.getChoice(propertyName, 0);
                    if (!menuItems[index].isSelected()) {
                        menuItems[index].setSelected(true);
                    }
                }
            }
        });

        return menu;
    }
}




