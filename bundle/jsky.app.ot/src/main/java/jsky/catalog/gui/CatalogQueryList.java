package jsky.catalog.gui;

import java.util.*;

/**
 * Manages a list of saved query argument and display settings.
 */
public class CatalogQueryList {

    // List of CatalogQueryItem, for previously saved queries.
    private final LinkedList<CatalogQueryItem> _queryList = new LinkedList<>();

    /** Constructor */
    public CatalogQueryList() {
    }

    /**
     * Add the given item to the query stack, removing duplicates.
     */
    public void add(CatalogQueryItem queryItem) {
        // remove duplicates from query list
        ListIterator it = ((LinkedList) _queryList.clone()).listIterator(0);
        for (int i = 0; it.hasNext(); i++) {
            CatalogQueryItem item = (CatalogQueryItem) it.next();
            if (item.getName().equals(queryItem.getName())) {
                _queryList.remove(i);
                break;
            }
        }
        _queryList.add(queryItem);
    }

    /**
     * Remove the named item from the list.
     */
    public void remove(String name) {
        ListIterator it = ((LinkedList) _queryList.clone()).listIterator(0);
        for (int i = 0; it.hasNext(); i++) {
            CatalogQueryItem item = (CatalogQueryItem) it.next();
            if (item.getName().equals(name)) {
                _queryList.remove(i);
                break;
            }
        }
    }


    /** Return an iterator over the query list */
    public Iterator<CatalogQueryItem> iterator() {
        return _queryList.iterator();
    }

    /** Return the size of the query list */
    public int size() {
        return _queryList.size();
    }


    /** Make the query list empty */
    public void clear() {
        _queryList.clear();
    }
}

