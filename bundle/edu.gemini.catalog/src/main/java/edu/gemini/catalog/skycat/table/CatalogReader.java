//
// $
//

package edu.gemini.catalog.skycat.table;

import edu.gemini.shared.util.immutable.Option;

import java.io.Closeable;
import java.io.IOException;

/**
 * Describes an interface to an object used for reading a catalog source.  It
 * provides an optional {@link CatalogHeader} and an iterator of
 * {@link CatalogRow}s.  The CatalogReader abstracts the process of reading
 * from a catalog so that multiple mechanisms may be supported.  For example,
 * an implementation that simply reads from an existing Swing table model is
 * provided along with a version that reads output from a skycat server.
 *
 * <p>The {@link CatalogTableUtil} provides a
 * {@link edu.gemini.catalog.skycat.table.CatalogTableUtil#readSkyObjects(CatalogReader, SkyObjectFactory) method}
 * that accepts a reader and exercises the interface in producing a collection
 * of {@link edu.gemini.shared.skyobject.SkyObject}.
 */
public interface CatalogReader extends Closeable {

    /**
     * Opens the reader to begin reading the catalog source's content.  The
     * reader must be opened before reading.  Ensure that the
     * <code>close</code> method is called once the reader has been opened.
     * It is suggested that the close method always be called in finally block
     * as in:
     *
     * <pre>
     * reader.open();
     * try {
     *     // read
     * } finally {
     *     reader.close();
     * }
     * </pre>
     *
     * @throws IOException if there is a problem reading
     */
    void open() throws IOException;

    /**
     * Gets the optional header of this catalog.  It is not required that this
     * method return a valid result before the reader is opened, nor after it
     * is closed.
     *
     * @return catalog header, if available
     *
     * @throws IOException if there is a problem reading the catalog source
     */
    Option<CatalogHeader> getHeader() throws IOException;

    /**
     * Determines whether there are more {@link CatalogRow}s waiting to be
     * iterated over.  Calling {@link #next} after this method returns
     * <code>false</code> or after this method would have returned
     * <code>false</code> had it been called is unsupported.
     *
     * @return <code>true</code> if the reader has pending {@link CatalogRow}s;
     * <code>false</code> otherwise
     *
     * @throws IOException if there is a problem reading the catalog source
     */
    boolean hasNext() throws IOException;

    /**
     * Returns the next {@link CatalogRow} available in the reader, if any.
     * If a previous call to {@link #hasNext} returned <code>false</code> or
     * would have returned <code>false</code> had it been called, calling this
     * method is unsupported.  Always ensure that there is a pending
     * {@link CatalogRow} by first calling {@link #hasNext}.
     *
     * @return next {@link CatalogRow} if any; unsupported otherwise
     *
     * @throws IOException if there is a problem reading the catalog source
     */
    CatalogRow next() throws IOException;
}
