/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: CanvasFigureEvent.java 4416 2004-02-03 18:21:36Z brighton $
 */

package jsky.graphics;

import java.util.EventObject;


/**
 * This event is generated when a canvas figure is seletced, deselected,
 * resized or dragged.
 *
 * @version $Revision: 4416 $
 * @author Allan Brighton
 */
public class CanvasFigureEvent extends EventObject {

    public CanvasFigureEvent(CanvasFigure fig) {
        super(fig);
    }

    /** Return the figure for the event. */
    public CanvasFigure getFigure() {
        return (CanvasFigure) getSource();
    }
}
