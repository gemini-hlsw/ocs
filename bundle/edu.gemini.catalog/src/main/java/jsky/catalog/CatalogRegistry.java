/*
 * ESO Archive
 *
 * $Id: CatalogRegistry.java 23553 2010-01-22 19:24:12Z swalker $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/17  Created
 */

package jsky.catalog;

import java.util.*;


/**
 * Used to manage access to a known list of catalogs.
 * Catalogs may be registered by name and later searched for by name.
 */
public enum CatalogRegistry implements Iterable<Catalog> {
    instance;


    // Sorted list of known catalogs
    private TreeSet<Catalog> _catalogSet = new TreeSet<Catalog>(
        new Comparator<Catalog>() {
            public int compare(Catalog c1, Catalog c2) {
                return (c1.getName().compareTo(c2.getName()));
            }
        }
    );

    // The list of catalogs, unsorted, in the order they were registered
    private List<Catalog> _catalogList = new ArrayList<Catalog>();


    /**
     * Register the given catalog. The argument may be any object that
     * implements the Catalog interface and will be used for any access
     * to that catalog. Since the catalog may not actually be used, the
     * constructor should not open any connections until needed.
     *
     * @param catalog An object to use to query the catalog.
     * @param overwrite if true, the given catalog object replaces any
     *                previously defined catalog with the same name,
     *                otherwise only the first catalog registered with a
     *                given name is actually registered.
     */
    public void register(Catalog catalog, boolean overwrite) {
        if (lookup(catalog.getName()) != null) {
            if (overwrite) {
                _catalogSet.remove(catalog);
                _catalogList.remove(catalog);
            } else {
                return;
            }
        }
        _catalogSet.add(catalog);
        _catalogList.add(catalog);
    }


    /**
     * This method returns a Catalog object that can be used to query
     * the given catalog, or null if no such object was found.
     *
     * @param catalogName The name of a registered catalog
     *
     * @return The object to use to query the catalog, or null if not found.
     */
    public Catalog lookup(String catalogName) {
        for (Catalog catalog : _catalogSet) {
            String s = catalog.getName();
            if (s != null && s.equals(catalogName)) {
                return catalog;
            }
        }

        return null;
    }

    /**
     * This method returns a list of Catalog objects that have the given type,
     * in the order in which they were registered.
     *
     * @param type The catalog type (as returned by <code>Catalog.getType()</code>)
     * @return the list of Catalog objects found
     */
    public List<Catalog> getCatalogsByType(String type) {
        List<Catalog> l = new ArrayList<Catalog>();
        for (Catalog catalog : _catalogList) {
            String s = catalog.getType();
            if (s != null && s.equals(type)) {
                l.add(catalog);
            }
        }

        return l;
    }


    /**
     * Unregister the given catalog, removing it from the list of known
     * catalogs.
     *
     * @param catalog The catalog to be removed from the list.
     */
    public void unregister(Catalog catalog) {
        if (_catalogSet.contains(catalog)) {
            _catalogSet.remove(catalog);
            _catalogList.remove(catalog);
        }
    }


    /**
     * Returns an Iterator to visit each registered catalog in sorted order.
     * @return The Iterator object for a sorted list of Catalogs.
     */
    public Iterator<Catalog> iterator() {
        return _catalogSet.iterator();
    }
}
