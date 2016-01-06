package jsky.util.gui;

import java.io.IOException;

/**
 * An exception that is thrown when (or at some point after) the user
 * presses the Stop button in a ProgressPanel.
 */
public class ProgressException extends IOException {

    public ProgressException(String msg) {
        super(msg);
    }
}

