// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: TpeManagerWatcher.java 4336 2004-01-20 07:57:42Z gillies $
//
package jsky.app.ot.tpe;

/**
 * An interface supported by clients of the TpeManager who want
 * to be informed of when a position editor is opened.
 */
public interface TpeManagerWatcher {
    /** The position editor has been opened, closed. */
    public void tpeOpened(TelescopePosEditor tpe);
}

