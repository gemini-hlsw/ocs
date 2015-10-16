package jsky.navigator;

import java.awt.Component;


/**
 * An interface for classes that manage the creation of and a reference to
 * an image display window and its frame.
 */
public interface NavigatorImageDisplayManager {

    /** Return the image display widget */
    NavigatorImageDisplay getImageDisplay();

    /** Return the image display frame, creating it if needed */
    Component getImageDisplayControlFrame();
}

