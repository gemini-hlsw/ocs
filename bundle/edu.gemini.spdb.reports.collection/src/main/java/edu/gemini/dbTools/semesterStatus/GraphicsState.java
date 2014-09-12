//
// $Id: GraphicsState.java 4816 2004-07-08 12:40:46Z shane $
//
package edu.gemini.dbTools.semesterStatus;

import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Class used to save and restore the graphics context.  The state is saved
 * before an operation is performed that changes attributes like the Paint
 * and then restored afterwards.
 */
final class GraphicsState {
    private final Font  _font;
    private final Paint _paint;
    private final AffineTransform _xform;

    static GraphicsState save(final Graphics2D gc) {
        return new GraphicsState(gc);
    }

    private GraphicsState(final Graphics2D gc) {
        _font  = gc.getFont();
        _paint = gc.getPaint();
        _xform = gc.getTransform();
    }

    void restore(final Graphics2D gc) {
        gc.setFont(_font);
        gc.setPaint(_paint);
        gc.setTransform(_xform);
    }
}
