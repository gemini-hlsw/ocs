//
// $Id: Util.java 4823 2004-07-09 01:17:35Z shane $
//
package edu.gemini.dbTools.semesterStatus;

import java.awt.*;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Small utility class.
 */
final class Util {

    /**
     * Gets rendering hints to be used in drawing methods.
     */
    static Map<RenderingHints.Key, Object>  getRenderingHints() {
        final Map<RenderingHints.Key, Object> hints = new HashMap<RenderingHints.Key, Object> ();
        hints.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
                  RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        hints.put(RenderingHints.KEY_ANTIALIASING,
                  RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_COLOR_RENDERING,
                  RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        hints.put(RenderingHints.KEY_RENDERING,
                  RenderingHints.VALUE_RENDER_QUALITY);
        hints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return hints;
    }

    // This is lame, but there is a chicken and egg problem to solve.
    // In order to calculate the size of the image to create, a graphics
    // context is needed.  In order to get a graphics context, you need
    // an image...  So this is an image used solely for calculating
    // String bounds.
    private static final BufferedImage FONT_MEASURE_IMAGE = new BufferedImage(500, 100, BufferedImage.TYPE_INT_RGB);
    private static final Graphics2D FONT_MEASURE_GC = FONT_MEASURE_IMAGE.createGraphics();

    static {
        FONT_MEASURE_GC.setRenderingHints(getRenderingHints());
    }

    static Rectangle2D getStringBounds(final String text, final Font font) {
        return font.getStringBounds(text, FONT_MEASURE_GC.getFontRenderContext());
    }

    static LineMetrics getLineMetrics(final String text, final Font font) {
        return font.getLineMetrics(text, FONT_MEASURE_GC.getFontRenderContext());
    }

}
