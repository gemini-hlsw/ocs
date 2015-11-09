package jsky.util;

/**
 * An interface for objects that can store their settings and restore them later.
 */
public interface Storeable {

    /** Store the current settings in a serializable object and return the object. */
    Object storeSettings();

    /** Restore the settings previously stored and return true if successful. */
    boolean restoreSettings(Object obj);
}
