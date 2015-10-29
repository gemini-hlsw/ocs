package jsky.catalog.gui;

import jsky.util.Storeable;
import jsky.util.gui.DialogUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.Serializable;

/**
 * Local class used to store information about a query and its results, so that
 * it can be repeated at a later time.
 */
public class CatalogQueryItem extends AbstractAction implements Serializable {

    // Store the query under this name
    private String _name;

    // A serializable object describing the query
    private Object _queryInfo;

    // A serializable object describing the result display settings
    private Object _resultInfo;

    /**
     * Create a catalog query item with the given name.
     *
     * @param name store the query under this name
     * @param queryInfo a serializeable object describing the query
     * @param resultInfo a serializeable object describing the result display settings
     */
    public CatalogQueryItem(String name, Object queryInfo, Object resultInfo) {
        super(name);
        _name = name;
        _queryInfo = queryInfo;
        _resultInfo = resultInfo;
    }

    /** Display the catalog */
    public void actionPerformed(ActionEvent evt) {
        try {
            CatalogNavigator navigator = CatalogNavigatorMenuBar.getCurrentCatalogNavigator();

            JComponent queryComponent = navigator.getQueryComponent();
            if (queryComponent instanceof Storeable && _queryInfo != null) {
                if (!((Storeable) queryComponent).restoreSettings(_queryInfo))
                    return;
            }

            JComponent resultComponent = navigator.getResultComponent();
            if (resultComponent instanceof Storeable && _resultInfo != null) {
                ((Storeable) resultComponent).restoreSettings(_resultInfo);
            }

            // run the query with the new settings
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    /** Return the name for the query settings. */
    public String getName() {
        return _name;
    }

    public Object getQueryInfo() {
        return _queryInfo;
    }

    public Object getResultInfo() {
        return _resultInfo;
    }

}
