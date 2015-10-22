package jsky.app.ot.viewer;

/**
 * This is the interface for an (observatory specific) object responsible for displaying
 * a window where the user can query the science program database.
 */
public interface QueryManager {
    /** Pop up a window for querying the science program database */
    void queryDB();
}
