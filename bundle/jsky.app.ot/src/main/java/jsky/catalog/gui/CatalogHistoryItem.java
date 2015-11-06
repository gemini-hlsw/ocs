package jsky.catalog.gui;

import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.JComponent;

import jsky.catalog.URLQueryResult;
import jsky.util.gui.DialogUtil;

/**
 * Local class used to store information about previously viewed catalogs
 * or query results.
 * During a given session, the display component is saved and can be redisplayed
 * if needed. If the application is restarted, the URL can be used instead.
 */
public class CatalogHistoryItem extends AbstractAction implements Serializable {

    /** The URL of the catalog, table or FITS file, if known, otherwise null. */
    private String _urlStr;

    /** The catalogs's name */
    private String _name;

    /** The component displaying the catalog (used in this session). */
    private transient JComponent _queryComponent;

    /**
     * Create a catalog history item with the given name (for
     * display), URL string (for catalog files), and display
     * component. The component is used during this session, otherwise
     * the name or URL are used.
     *
     * @param name The catalogs's name
     * @param url The URL of the catalog, table or FITS file, if known, otherwise null
     * @param queryComponent The component displaying the catalog or query results (used in this session).
     */
    public CatalogHistoryItem(String name, URL url, JComponent queryComponent) {
        super(name);
        _name = name;
        if (url != null)
            _urlStr = url.toString();
        _queryComponent = queryComponent;
    }

    /** Display the catalog */
    public void actionPerformed(ActionEvent evt) {
        try {
            CatalogNavigator navigator = CatalogNavigatorMenuBar.getCurrentCatalogNavigator();
            URL url = null;
            if (_urlStr != null) {
                url = new URL(_urlStr);
            }

            if (_queryComponent != null) {
                navigator.setOrigURL(url);
                navigator.setQueryComponent(_queryComponent);
            } else if (url != null) {
                navigator.setQueryResult(new URLQueryResult(url));
            } else {
                System.out.println("XXX CatalogHistoryItem.actionPerformed: don't know how to display catalog");
            }

        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    /** Return the URL of the catalog, table or FITS file, if known, otherwise null. */
    public String getURLStr() {
        return _urlStr;
    }

    /** Return the catalogs's name. */
    public String getName() {
        return _name;
    }

    /** Return the component displaying the catalog or query results (used in this session). */
    public JComponent getQueryComponent() {
        return _queryComponent;
    }
}
