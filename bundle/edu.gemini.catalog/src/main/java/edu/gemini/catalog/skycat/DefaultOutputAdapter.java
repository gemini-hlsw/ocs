//
// $
//

package edu.gemini.catalog.skycat;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * An implementation of SkycatServerOutputAdapter that simply passes requests
 * to the underlying BufferedReader without modification.  Use this "adapter"
 * for the majority of catalogs that return output in the expected format.
 */
public class DefaultOutputAdapter implements SkycatOutputAdapter {
    private final BufferedReader rdr;

    /**
     * Constructs with the BufferedReader source of catalog data.
     */
    public DefaultOutputAdapter(BufferedReader rdr) {
        this.rdr = rdr;
    }

    /**
     * Gets the underlying BufferedReader.
     */
    protected BufferedReader getReader() {
        return rdr;
    }

    /**
     * Returns the result of calling <code>readLine</code> on the contained
     * BufferedReader.
     */
    public String readLine() throws IOException {
        return rdr.readLine();
    }

    /**
     * Closes the contained BufferedReader.
     */
    public void close() throws IOException {
        rdr.close();
    }
}
