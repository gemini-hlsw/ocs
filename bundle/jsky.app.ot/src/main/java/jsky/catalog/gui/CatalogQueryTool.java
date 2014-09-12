/*
 * ESO Archive
 *
 * $Id: CatalogQueryTool.java 7122 2006-06-06 16:38:01Z anunez $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/06/02  Created
 */

package jsky.catalog.gui;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jsky.catalog.Catalog;
import jsky.catalog.QueryArgs;
import jsky.catalog.QueryResult;
import jsky.catalog.CatalogException;
import jsky.util.*;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.GridBagUtil;
import jsky.util.gui.ProgressException;
import jsky.util.gui.SwingWorker;

/**
 * Displays a CatalogQueryPanel in a JScrollPane and implements a search() method.
 */
public class CatalogQueryTool extends JPanel
        implements ActionListener, Storeable {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(CatalogQueryTool.class);

    /**
     * The catalog to use
     */
    private Catalog _catalog;

    /**
     * Panel containing labels and entries for searching the catalog
     */
    private CatalogQueryPanel _catalogQueryPanel;

    /**
     * Used to display query results
     */
    private QueryResultDisplay _queryResultDisplay;

    /**
     * Utility object used to control background thread
     */
    private SwingWorker _worker;


    /**
     * Create a CatalogQueryTool for searching the given catalog.
     *
     * @param catalog    The catalog to use.
     * @param scrollable set to true to allow scrolling of the query panel
     */
    public CatalogQueryTool(Catalog catalog, boolean scrollable) {
        _catalog = catalog;
        JLabel _catalogTitleLabel = makeCatalogPanelLabel(catalog);
        _catalogQueryPanel = makeCatalogQueryPanel(catalog);
        _catalogQueryPanel.addActionListener(this);


        GridBagUtil layout = new GridBagUtil(this);
        layout.add(_catalogTitleLabel, 0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.NONE,
                GridBagConstraints.CENTER,
                new Insets(3, 0, 3, 0));

        if (scrollable) {
            layout.add(new JScrollPane(_catalogQueryPanel), 0, 1, 1, 1, 1.0, 1.0,
                    GridBagConstraints.BOTH,
                    GridBagConstraints.CENTER,
                    new Insets(0, 0, 0, 0));
        } else {
            layout.add(_catalogQueryPanel, 0, 1, 1, 1, 1.0, 1.0,
                    GridBagConstraints.BOTH,
                    GridBagConstraints.CENTER,
                    new Insets(0, 0, 0, 0));
        }

        layout.add(makeButtonPanel(), 0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.HORIZONTAL,
                GridBagConstraints.CENTER,
                new Insets(5, 0, 0, 5));
    }


    /**
     * Create a CatalogQueryTool for searching the given catalog.
     *
     * @param catalog            The catalog to use.
     * @param queryResultDisplay object used to display query results
     * @param scrollable         set to true to allow scrolling of the query panel
     */
    public CatalogQueryTool(Catalog catalog, QueryResultDisplay queryResultDisplay, boolean scrollable) {
        this(catalog, scrollable);
        _queryResultDisplay = queryResultDisplay;
    }

    /**
     * Create a CatalogQueryTool for searching the given catalog.
     *
     * @param catalog            The catalog to use.
     * @param queryResultDisplay object used to display query results
     */
    public CatalogQueryTool(Catalog catalog, QueryResultDisplay queryResultDisplay) {
        this(catalog, queryResultDisplay, true);
    }


    /**
     * Make and return the catalog panel label
     */
    protected JLabel makeCatalogPanelLabel(Catalog catalog) {
        String title = catalog.toString();
        setName(title);
        return new JLabel(title, JLabel.CENTER);
    }


    /**
     * Make and return the catalog query panel
     */
    protected CatalogQueryPanel makeCatalogQueryPanel(Catalog catalog) {
        return new CatalogQueryPanel(catalog, 2);
    }


    /**
     * Make and return the button panel
     */
    protected JPanel makeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JButton goButton = new JButton(_I18N.getString("query"));
        goButton.setToolTipText(_I18N.getString("startQuery"));
        goButton.addActionListener(this);
        buttonPanel.add(goButton);

        //Only show "Store Preferences" button for Image Server catalogs
        if (_catalog != null && _catalog.isImageServer()) {
            final JButton store = new JButton();

            store.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (_catalogQueryPanel != null) {
                        _catalogQueryPanel.storeUserSelection();
                    }
                }
            });
            store.setAction(StoreImageServerAction.getAction(_catalog));
            store.setText(_I18N.getString("store"));
            store.setToolTipText(_I18N.getString("storeToolTip"));

            buttonPanel.add(store);
        }

        return buttonPanel;
    }


    /**
     * Set the object used to diplay the result of a query
     */
    public void setQueryResultDisplay(QueryResultDisplay q) {
        _queryResultDisplay = q;
    }

    /**
     * Return the object used to diplay the result of a query
     */
    public QueryResultDisplay getQueryResultDisplay() {
        return _queryResultDisplay;
    }

    /**
     * Stop the background loading thread if it is running
     */
    public void interrupt() {
        if (_worker != null) {
            _worker.interrupt();
        }
        _worker = null;
    }

    /**
     * Return the name of this component (based on the data being displayed)
     */
    public String getName() {
        if (_catalog != null)
            return _catalog.getName();
        return _I18N.getString("catalog");
    }


    /**
     * Return the catalog for this object
     */
    public Catalog getCatalog() {
        return _catalog;
    }


    /**
     * Return the panel containing labels and entries for searching the catalog
     */
    public CatalogQueryPanel getCatalogQueryPanel() {
        return _catalogQueryPanel;
    }


    /**
     * Called when return is typed in one of the query panel text fields
     * to start the query.
     */
    public void actionPerformed(ActionEvent ev) {
        search();
    }


    /**
     * Query the catalog based on the settings in the query panel and display
     * the results.
     */
    public void search() {
        if (_queryResultDisplay == null)
            return;

        // run in a separate thread, so the user can monitor progress and cancel it, if needed
        _worker = new SwingWorker() {

            public Object construct() {
                try {
                    QueryArgs queryArgs = _catalogQueryPanel.getQueryArgs();
                    return _catalog.query(queryArgs);
                } catch (Exception e) {
                    return e;
                }
            }

            public void finished() {
                _worker = null;
                Object o = getValue();
                if (o instanceof ProgressException) {
                    // user canceled operation (pressed Stop button in progress panel): ignore
                    return;
                }
                if (o instanceof IOException) {
                    String msg = ((IOException) o).getMessage();
                    if (msg == null) {
                        msg = "";
                    } else {
                        msg = ": " + msg;
                    }
                    DialogUtil.error("Connection to catalog server broken" + msg);
                    return;
                }
                if (o instanceof CatalogException) {
                    String msg = ((CatalogException) o).getMessage();
                    if (msg == null) {
                        msg = "";
                    } else {
                        msg = ": " + msg;
                    }
                    DialogUtil.error("Catalog query error " + msg);
                    return;
                }
                if (o instanceof Exception) {
                    DialogUtil.error((Exception) o);
                    return;
                }
                setQueryResult((QueryResult) o);
            }
        };
        _worker.start();
    }


    /**
     * Display the given query result.
     */
    public void setQueryResult(QueryResult queryResult) {
        _queryResultDisplay.setQueryResult(queryResult);
    }


    /**
     * Store the current settings in a serializable object and return the object.
     */
    public Object storeSettings() {
        return _catalogQueryPanel.storeSettings();
    }

    /**
     * Restore the settings previously stored.
     */
    public boolean restoreSettings(Object obj) {
        if (_catalogQueryPanel.restoreSettings(obj)) {
            search();
            return true;
        }
        return false;
    }
}

