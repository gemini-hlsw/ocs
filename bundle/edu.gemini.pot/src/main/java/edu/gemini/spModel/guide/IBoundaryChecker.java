package edu.gemini.spModel.guide;

import java.awt.geom.Rectangle2D;

/**
 * TODO: Why does this file exist?
 */
public interface IBoundaryChecker {
    public Rectangle2D.Double getFovIn();

    public Rectangle2D.Double getFov();

    public Rectangle2D.Double getFovOut();
}
