package jsky.util;

import java.io.IOException;


/**
 * An interface for objects that can be saved to a file in HTML format.
 * This is intended to be used to implement "Save as HTML..." menu items.
 */
public interface SaveableAsHTML {

    /**
     * Save the current object to the given file in HTML format.
     */
    void saveAsHTML(String filename) throws IOException;
}
