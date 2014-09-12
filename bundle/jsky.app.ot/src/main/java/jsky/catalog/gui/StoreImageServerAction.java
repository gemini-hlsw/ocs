package jsky.catalog.gui;

import jsky.navigator.NavigatorManager;
import jsky.navigator.Navigator;
import jsky.catalog.Catalog;
import jsky.util.Preferences;

import java.awt.event.ActionEvent;
import javax.swing.*;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * This action saves the catalog selection based on user selection.
 * It also updates the user interface associated to the catalog,
 * specifically the AbstracButtons that need to show up as selected.
 *
 * This class provides a factory that allows the creation of only
 * one action associated to every singe catalog. Every action can
 * update one or many AbstractButtons.
 * 
 */
public class StoreImageServerAction extends AbstractAction {

    private final Catalog cat;


    private StoreImageServerAction(Catalog catalog) {
        super();
        cat = catalog;
    }

    public void actionPerformed(ActionEvent e) {
        Preferences.set(Catalog.SKY_USER_CATALOG, cat.getName());
        List<AbstractButton> list = (List<AbstractButton>)getValue("MenuItem");
        if (list != null) {
            for (AbstractButton b: list) {
                b.setSelected(true);
            }
            //update the TreeCell, to reflect the user selection
            Navigator nav = NavigatorManager.get();
            if (nav == null) return;
            CatalogTree tree = nav.getCatalogTree();
            if (tree == null) return;
            tree.repaint();
        }
    }

    private static HashMap<Catalog, StoreImageServerAction> map = new HashMap<Catalog, StoreImageServerAction>();

    public static StoreImageServerAction getAction(Catalog cat) {
        StoreImageServerAction action = map.get(cat);
        if (action == null) {
            action = new StoreImageServerAction(cat);
            map.put(cat, action);
        }
        return action;
    }

    public void appendValue(String key, AbstractButton b) {
        List<AbstractButton> l = (List<AbstractButton>)getValue(key);
        if (l == null) {
            l = new ArrayList<AbstractButton>();
            putValue(key, l);
        }
        l.add(b);
    }


}
