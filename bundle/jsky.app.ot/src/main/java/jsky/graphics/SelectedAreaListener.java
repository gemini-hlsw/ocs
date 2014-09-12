/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SelectedAreaListener.java 4416 2004-02-03 18:21:36Z brighton $
 */

package jsky.graphics;

import java.awt.geom.Rectangle2D;
import java.util.EventListener;

/**
 * This defines the interface for listening for area selection events.
 *
 * @version $Revision: 4416 $
 * @author Allan Brighton
 */
public abstract interface SelectedAreaListener extends EventListener {

    /**
     * Invoked when an area of the canvas has been dragged out.
     */
    public void setSelectedArea(Rectangle2D r);

}
