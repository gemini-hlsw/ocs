//
// $
//

package jsky.app.ot.util;

import java.awt.*;

/**
 * Commonly used colors in the Observing Tool.
 */
public final class OtColor {
    private OtColor() { /* empty */ }

    public static final Color CANTALOUPE      = new Color(255, 238, 204);
    public static final Color TANGERINE       = new Color(253, 163,  83);
    public static final Color HONEY_DEW       = new Color(238, 255, 204);
    public static final Color BANANA          = new Color(255, 255, 204);
    public static final Color SKY             = new Color(204, 238, 255);
    public static final Color SALMON          = new Color(255, 102, 102);
    public static final Color LIGHT_SALMON    = new Color(253, 177, 177);

    public static final Color LIGHT_ORANGE    = new Color(255, 225, 172);
    public static final Color VERY_LIGHT_GREY = new Color(247, 243, 239);
    public static final Color LIGHT_GREY      = new Color(238, 238, 238);
    public static final Color BG_GREY         = new Color(225, 225, 225);
    public static final Color DARKER_BG_GREY  = new Color(204, 204, 204);

    // Creates a color that is the same as the give color, but with a .5 alpha
    public static Color makeSlightlyTransparent(Color c) {
        return makeTransparent(c, 0.5);
    }

    public static Color makeTransparent(Color c, double alpha) {
        if ((alpha < 0.0) || (alpha > 1.0)) {
            throw new IllegalArgumentException();
        }

        int a = (int) Math.round(255 * alpha);
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        return new Color(r, g, b, a);
    }

    public static Color makeSlightlyDarker(Color c) {
        return new Color(Math.max((int)(c.getRed()  * 0.9), 0),
                   Math.max((int)(c.getGreen()* 0.9), 0),
                   Math.max((int)(c.getBlue() * 0.9), 0));
    }

}
