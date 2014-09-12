//
// $
//

package edu.gemini.catalog.skycat;

/**
 * An exception indicating an unexpected catalog result.  For example, an
 * output that cannot be parsed as expected or that is required but missing.
 */
public class CatalogException extends Exception {
    public CatalogException() {
    }

    public CatalogException(String message) {
        super(message);
    }

    public CatalogException(String message, Throwable cause) {
        super(message, cause);
    }

    public CatalogException(Throwable cause) {
        super(cause);
    }
}
