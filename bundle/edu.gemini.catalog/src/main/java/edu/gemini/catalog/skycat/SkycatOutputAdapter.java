package edu.gemini.catalog.skycat;

import java.io.Closeable;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Adpats the output from a catalog server to a format that is digestible by
 * Allan's skycat code.
 */
public interface SkycatOutputAdapter extends Closeable {
    /**
     * Creates a SkycatServerOutputAdapter given a BufferedReader.
     */
    interface Factory {
        SkycatOutputAdapter create(BufferedReader rdr);
    }

    /**
     * Reads the next line from the input source, returning <code>null</code>
     * when there is no more data.
     *
     * @return next line of input or <code>null</code> if the end of the
     * stream has been reached.
     *
     * @throws java.io.IOException if there is a problem reading from the
     * catalog source
     */
    String readLine() throws IOException;    
}
