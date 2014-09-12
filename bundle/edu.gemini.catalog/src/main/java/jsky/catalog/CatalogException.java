package jsky.catalog;

/**
 * A super simple exception class for reporting problems with the catalog queries
 */
public class CatalogException extends Exception {
    public CatalogException(String s) {
        super(s);
    }
}
