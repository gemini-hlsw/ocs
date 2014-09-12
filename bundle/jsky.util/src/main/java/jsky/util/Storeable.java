/*
 * Copyright 2003 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: Storeable.java 4726 2004-05-14 16:50:12Z brighton $
 */

package jsky.util;


/**
 * An interface for objects that can store their settings and restore them later.
 */
public abstract interface Storeable {

    /** Store the current settings in a serializable object and return the object. */
    public Object storeSettings();

    /** Restore the settings previously stored and return true if successful. */
    public boolean restoreSettings(Object obj);
}
