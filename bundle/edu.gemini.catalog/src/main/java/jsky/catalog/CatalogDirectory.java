package jsky.catalog;

import java.util.List;

import javax.swing.tree.TreeModel;

/**
 * This defines the interface for a catalog directory, allowing catalogs
 * to be organized in a hierarchy.
 */
@Deprecated
public interface CatalogDirectory extends Catalog, TreeModel {

    /** Return the number of catalogs in this directory */
    int getNumCatalogs();

    /** Return the ith catalog in the directory */
    Catalog getCatalog(int i);

    /** Return the named catalog, if found in this directory */
    Catalog getCatalog(String catalogName);

    /** Return the index of the given catalog in the directory */
    int indexOf(Catalog cat);

    /** Return a memory catalog describing the list of catalogs in the directory */
    TableQueryResult getCatalogList();

    /** Return an array of catalogs describing the path to the given catalog or catalog directory. */
    Catalog[] getPath(Catalog cat);

    /**
     * Add the given catalog to the catalog list. An error message is displayed if the
     * catalog is already in the list. If a separate catalog with the same name is in the list,
     * the user is asked if it should be removed.
     */
    void addCatalog(Catalog cat);

    /** Remove the given catalog from the catalog list. */
    void removeCatalog(Catalog cat);

    /**
     * Save the contents of this catalog directory to make it permanent
     * (for example, in a config file under ~/.jsky/...).
     */
    void save();

    /**
     * Return a list of name servers (Catalogs with type
     * equal to "namesvr") to use to resolve astronomical object names.
     */
    List<Catalog> getNameServers();
}


