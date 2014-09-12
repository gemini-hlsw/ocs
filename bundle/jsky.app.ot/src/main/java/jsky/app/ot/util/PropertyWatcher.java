// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: PropertyWatcher.java 4336 2004-01-20 07:57:42Z gillies $
//
package jsky.app.ot.util;

/**
 * Interface implemented by clients of the BasicPropertyList that
 * want to be informed when a property changes.
 */
public interface PropertyWatcher {
    /** The named property changed. */
    public void propertyChange(String propertyName);
}
