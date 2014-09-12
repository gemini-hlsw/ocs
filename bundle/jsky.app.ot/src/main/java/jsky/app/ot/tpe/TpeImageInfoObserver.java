// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: TpeImageInfoObserver.java 4336 2004-01-20 07:57:42Z gillies $
//
package jsky.app.ot.tpe;

/**
 * An interface supported by TpeImageWidget clients that wish to
 * know when the image info is updated.
 */
public interface TpeImageInfoObserver {
    /**
     * Notify that image info has been updated.
     */
    public void imageInfoUpdate(TpeImageWidget iw, TpeImageInfo tii);
}

