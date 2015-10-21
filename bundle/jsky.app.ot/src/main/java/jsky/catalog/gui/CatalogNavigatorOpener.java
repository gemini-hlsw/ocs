package jsky.catalog.gui;

import jsky.catalog.Catalog;

/**
 * This interface is implemented by classes that can create and/or
 * open the catalog navigator window to display the contents of a
 * given catalog.
 */
@Deprecated
public interface CatalogNavigatorOpener {

    /** Open the catalog window. */
    void openCatalogWindow();

    /** Open the catalog window and display the interface for given catalog, if not null. */
    @Deprecated
    void openCatalogWindow(Catalog cat);

    /** Open a catalog window for the named catalog, if found. */
    @Deprecated
    void openCatalogWindow(String name);

    /** Pop up a file browser to select a local catalog file to open. */
    @Deprecated
    void openLocalCatalog();
}


