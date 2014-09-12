// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: TpeViewObserver.java 4336 2004-01-20 07:57:42Z gillies $
//
package jsky.app.ot.tpe;

/**
 * An interface supported by TpeImageWidget clients that which to
 * be notified when the view changes.
 *
 * @see TpeImageWidget
 */
public interface TpeViewObserver {
    /**
     * Notify that the view has changed.
     */
    public void tpeViewChange(TpeImageWidget iw);
}

