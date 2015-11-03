package jsky.util;

import java.io.IOException;

/**
 * An interface for objects that can be saved to a file in some format.
 * This is intended to be used to implement "Save as..." menu items.
 */
public interface Saveable {

    /**
     * Save the current object to the given file. In some cases the
     * format may depend on the file suffix.
     */
    void saveAs(String filename) throws IOException;
}
