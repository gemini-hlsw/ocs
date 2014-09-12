/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ImageGraphicsHandler.java 4414 2004-02-03 16:21:36Z brighton $
 */

package jsky.image.gui;

import java.awt.Graphics2D;
import java.util.EventListener;


/**
 *  A callback interface for classes that need to draw graphics over an image.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public abstract interface ImageGraphicsHandler extends EventListener {

    /** Called each time the image is repainted */
    public void drawImageGraphics(BasicImageDisplay imageDisplay, Graphics2D g);
}


