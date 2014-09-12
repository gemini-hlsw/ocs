/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SaveableAsHTML.java 4414 2004-02-03 16:21:36Z brighton $
 */

package jsky.util;

import java.io.IOException;


/**
 * An interface for objects that can be saved to a file in HTML format.
 * This is intended to be used to implement "Save as HTML..." menu items.
 */
public abstract interface SaveableAsHTML {

    /**
     * Save the current object to the given file in HTML format.
     */
    public void saveAsHTML(String filename) throws IOException;
}
