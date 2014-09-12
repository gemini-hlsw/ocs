package jsky.catalog.skycat;

import java.io.InputStream;
import java.io.IOException;

/**
 * Used to filter the content response from a Catalog. This can be used for instance
 * when the content of a catalog is not in a standard form or we are using options that
 * generates extra output
 * (like in  http://archive.eso.org/skycat/servers/sim-server?&o=%id/mimetype=full-rec)
 *
 * Filters are usually implemented as singletons.
 *
 * $Id: ICatalogFilter.java 7016 2006-05-09 20:36:46Z anunez $
 */
public interface ICatalogFilter {
    /**
     * Filter the content that comes into the input stream <code>is</code>. The filtered
     * result is returned in a new Input Strem. Multiple <code>ICatalogFilter</code> can
     * be nested.
     *
     * @param is The original Input Stream to Filter
     * @return a new Input Stream, with the original input stream Filtered.
     */
    public InputStream filterContent(InputStream is) throws IOException;
}
