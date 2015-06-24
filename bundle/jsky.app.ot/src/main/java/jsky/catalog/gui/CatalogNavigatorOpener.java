/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: CatalogNavigatorOpener.java 4414 2004-02-03 16:21:36Z brighton $
 */

package jsky.catalog.gui;

import jsky.catalog.Catalog;

/**
 * This interface is implemented by classes that can create and/or
 * open the catalog navigator window to display the contents of a
 * given catalog.
 */
public interface CatalogNavigatorOpener {

    /** Open the catalog window. */
    void openCatalogWindow();

    /** Open the catalog window and display the interface for given catalog, if not null. */
    void openCatalogWindow(Catalog cat);

    /** Open a catalog window for the named catalog, if found. */
    void openCatalogWindow(String name);

    /** Pop up a file browser to select a local catalog file to open. */
    void openLocalCatalog();
}


