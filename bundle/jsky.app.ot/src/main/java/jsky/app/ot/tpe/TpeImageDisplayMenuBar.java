package jsky.app.ot.tpe;

import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import jsky.app.ot.util.BasicPropertyList;
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

        for (BasicPropertyList.BasicProperty bp : pl.getProperties()) {
            if (bp instanceof BasicPropertyList.BooleanProperty)
                menu.add(_createBooleanPropertyMenuItem((BasicPropertyList.BooleanProperty)bp, pl));
            else if (bp instanceof BasicPropertyList.ChoiceProperty)
                menu.add(_createChoicePropertyMenu((BasicPropertyList.ChoiceProperty)bp, pl));
        }
    }


    /**
     * Create and return a checkbox menu item for the given name and boolean property.
     */
    private JCheckBoxMenuItem _createBooleanPropertyMenuItem(final BasicPropertyList.BooleanProperty bp,
                                                             final BasicPropertyList pl) {
        final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(bp.getName());
        menuItem.setSelected(bp.getValue());
        menuItem.addItemListener(e -> bp.setValue(menuItem.getState()));

        // keep view and popup menus in sync
        pl.addWatcher(propName -> {
            if (propName.equals(bp.getName())) {
                boolean b = bp.getValue();
                if (menuItem.isSelected() != b) {
                    menuItem.setSelected(b);
                }
            }
        });

        return menuItem;
    }

    /**
     * Create and return a menu for the given name and choice propery.
     */
    private JMenu _createChoicePropertyMenu(final BasicPropertyList.ChoiceProperty cp,
                                            final BasicPropertyList pl) {

        final JMenu menu = new JMenu(cp.getName());
        final List<JRadioButtonMenuItem> menuItems = new ArrayList<>();
        final ButtonGroup group = new ButtonGroup();

        for (String choice : cp.getChoices()) {
            final JRadioButtonMenuItem b = new JRadioButtonMenuItem(choice);
            // If this is the selected index, mark it as such.
            if (menuItems.size() == cp.getSelection())
                b.setSelected(true);

            menuItems.add(b);
            menu.add(b);
            group.add(b);

            b.addItemListener(e -> {
                if (b.isSelected())
                    cp.setSelection(menuItems.indexOf(b));
            });

        }

        // keep view and popup menus in sync
        pl.addWatcher(propName -> {
            if (propName.equals(cp.getName())) {
                int index = cp.getSelection();
                JRadioButtonMenuItem mi = menuItems.get(index);
                if (!mi.isSelected()) {
                    mi.setSelected(true);
                }
            }
        });

        return menu;
    }
}
