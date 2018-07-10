package edu.gemini.qpt.ui.util;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;

/**
 * Trivial state bundle that can capture the state of a Graphics2D, then restore the state
 * later. Useful when you want to draw without side-effects.
 * @author rnorris
 */
public class Graphics2DAttributes {

    private final Graphics2D g2d;
    private final Paint paint;
    private final Stroke stroke;
    private final AffineTransform xf;
    
    public Graphics2DAttributes(Graphics2D g2d) {
        this.g2d = g2d;
        this.paint = g2d.getPaint();
        this.stroke = g2d.getStroke();
        this.xf = g2d.getTransform();
    }
    
    public void restore() {
        g2d.setPaint(paint);
        g2d.setStroke(stroke);
        g2d.setTransform(xf);
    }
    
}
