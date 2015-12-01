package jsky.catalog;

import java.net.*;

/**
 * This interface defines a method that classes can call when an
 * HTTP server unexpectedly returns an HTML page, which normally
 * contains an error message about a broken link, etc.
 */
public interface HTMLQueryResultHandler {

    /** Display the contents of the HTML page given by the the URL */
    void displayHTMLPage(URL url);
}
