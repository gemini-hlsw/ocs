// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: TelescopePosWatcher.java 18053 2009-02-20 20:16:23Z swalker $
//

package edu.gemini.spModel.target;

/**
 * An interface supported by clients of TelescopePos who want to
 * be notified when the positions changes in some way.
 */
public interface TelescopePosWatcher {

    /**
     * The location of the position has changed.
     * @param tp
     */
    public void telescopePosLocationUpdate(WatchablePos tp);

    /**
     * Some other sort of change has been made, for instance its tag
     * may have been updated.
     * @param tp
     */
    public void telescopePosGenericUpdate(WatchablePos tp);
}



