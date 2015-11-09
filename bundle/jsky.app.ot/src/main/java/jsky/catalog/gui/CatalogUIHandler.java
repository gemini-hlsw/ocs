package jsky.catalog.gui;

import javax.swing.JComponent;

import jsky.catalog.Catalog;
import jsky.catalog.QueryResult;
import jsky.catalog.TableQueryResult;

/**
 * Defines the interface for classes wishing to define their own catalog
 * query or query result components.
 */
public interface CatalogUIHandler {

    /**
     * This interface may be implemented by Catalog and QueryResult objects that
     * wish to define custom user interfaces.
     *
     * @param display can be used to display the results of a catalog query
     *
     * @return a user interface component for the catalog or queryResult object, or null,
     *         in which case a default component will be used, based on the object type
     */
    JComponent makeComponent(QueryResultDisplay display);
}
