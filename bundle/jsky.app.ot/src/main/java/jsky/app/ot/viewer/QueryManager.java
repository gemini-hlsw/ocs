// Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: QueryManager.java 4725 2004-05-14 16:32:54Z brighton $
//
package jsky.app.ot.viewer;


/**
 * This is the interface for an (observatory specific) object responsible for displaying
 * a window where the user can query the science program database.
 */
public abstract interface QueryManager {
    /** Pop up a window for querying the science program database */
    public void queryDB();
}
