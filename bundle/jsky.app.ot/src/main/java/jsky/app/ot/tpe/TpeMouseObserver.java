// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: TpeMouseObserver.java 4336 2004-01-20 07:57:42Z gillies $
//
package jsky.app.ot.tpe;

/**
 * An interface supported by TpeImageWidget clients that which to
 * be notified for each mouse event.
 *
 * @see TpeImageWidget
 */
public interface TpeMouseObserver {
    /**
     * Notification that a new mouse event has arrived.
     */
    public void tpeMouseEvent(TpeImageWidget iw, TpeMouseEvent tme);
}

