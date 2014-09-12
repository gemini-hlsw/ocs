/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SaveableWithDialog.java 4726 2004-05-14 16:50:12Z brighton $
 */

package jsky.util;


/**
 * An interface for widgets that can pop up a dialog to save their
 * contents to a file.
 */
public abstract interface SaveableWithDialog {

    /**
     * Display a dialog to save the contents of this object to a file.
     */
    public void saveAs();
}
