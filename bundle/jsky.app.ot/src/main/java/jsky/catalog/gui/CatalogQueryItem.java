/*
 * Copyright 2003 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: CatalogQueryItem.java 25133 2010-04-16 22:23:28Z swalker $
 */

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

    // A serializeable object describing the query
    private Object _queryInfo;

    // A serializeable object describing the result display settings
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

    //
    // Not sure why this doesn't seem to be called ...  Since it wasn't working,
    // a copy of the CatalogQueryItems is made in the CatalogQueryList._save()
    // method.
    //


    /*

    // Define read/write object to only store the name, queryInfo, and result
    // info.  We don't want the property change listeners in the query item
    // to be serialized.

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(_name);
        out.writeObject(_queryInfo);
        out.writeObject(_resultInfo);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        _name       = (String) in.readObject();
        _queryInfo  = in.readObject();
        _resultInfo = in.readObject();
        putValue(Action.NAME, _name);
    }
    */
}
