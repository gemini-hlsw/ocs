/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: CanvasFigureGroup.java 4416 2004-02-03 18:21:36Z brighton $
 */

package jsky.graphics;

/**
 * This defines an abstract interface for a group of canvas figures that should be
 * displayed and selected together as a unit.
 *
 * @version $Revision: 4416 $
 * @author Allan Brighton
 */
public abstract interface CanvasFigureGroup extends CanvasFigure {

    /**
     * Add a figure to the group.
     */
    public void add(CanvasFigure fig);

    /**
     * Remove a figure from the group.
     */
    public void remove(CanvasFigure fig);
}

