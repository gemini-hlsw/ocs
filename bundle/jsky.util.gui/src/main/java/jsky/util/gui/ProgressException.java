/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ProgressException.java 4414 2004-02-03 16:21:36Z brighton $
 */

package jsky.util.gui;

import java.io.IOException;

/**
 * An exception that is thrown when (or at some point after) the user
 * presses the Stop button in a ProgressPanel.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public class ProgressException extends IOException {

    public ProgressException(String msg) {
        super(msg);
    }
}

