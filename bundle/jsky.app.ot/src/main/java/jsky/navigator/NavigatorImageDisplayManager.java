// Copyright 2003 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: NavigatorImageDisplayManager.java 4726 2004-05-14 16:50:12Z brighton $
//
package jsky.navigator;

import java.awt.Component;


/**
 * An interface for classes that manage the creation of and a reference to
 * an image display window and its frame.
 */
public abstract interface NavigatorImageDisplayManager {

    /** Return the image display widget */
    public NavigatorImageDisplay getImageDisplay();

    /** Return the image display frame, creating it if needed */
    public Component getImageDisplayControlFrame();
}

