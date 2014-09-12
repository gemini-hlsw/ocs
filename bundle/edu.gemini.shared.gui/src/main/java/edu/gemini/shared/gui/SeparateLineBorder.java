//
// $
//

package edu.gemini.shared.gui;

import javax.swing.border.AbstractBorder;
import java.awt.*;

/**
 * A simple border like the LineBorder, but which provides control over the
 * color and thickness of each of the four line segments that inclose the
 * component.  In other words, each line can be of a different thickness and
 * color.  The top and bottom lines extend over the left and right lines, like
 * a BorderLayout's north and south components.
 */
public final class SeparateLineBorder extends AbstractBorder {

    /**
     * Attributes for a single line in the border.
     */
    public static final class Line {
        private final Color color;
        private final int thickness;

        public Line(Color color, int thickness) {
            this.color     = color;
            this.thickness = thickness;
        }

        public Color getColor() {
            return color;
        }

        public int getThickness() {
            return thickness;
        }
    }

    /**
     * A black, 1 pixel wide line.
     */
    public static final Line THIN_BLACK = new Line(Color.black, 1);

    /**
     * An empty line.
     */
    public static final Line EMPTY_LINE = new Line(Color.black, 0);

    /**
     * Creates a {@link Line} of the given color and thickness.
     */
    public static Line line(Color c, int thickness) {
        return new Line(c, thickness);
    }

    /**
     * Creates a border with a single painted line along the top.
     *
     * @param c color of the line
     * @param thickness thickness of the line
     */
    public static SeparateLineBorder createTopLineBorder(Color c, int thickness) {
        return new SeparateLineBorder(line(c, thickness), null, null, null);
    }

    /**
     * Creates a border with a single painted line along the left side.
     *
     * @param c color of the line
     * @param thickness thickness of the line
     */
    public static SeparateLineBorder createLeftLineBorder(Color c, int thickness) {
        return new SeparateLineBorder(null, line(c, thickness), null, null);
    }

    /**
     * Creates a border with a single painted line along the bottom.
     *
     * @param c color of the line
     * @param thickness thickness of the line
     */
    public static SeparateLineBorder createBottomLineBorder(Color c, int thickness) {
        return new SeparateLineBorder(null, null, line(c, thickness), null);
    }

    /**
     * Creates a border with a single painted line along the right.
     *
     * @param c color of the line
     * @param thickness thickness of the line
     */
    public static SeparateLineBorder createRightLineBorder(Color c, int thickness) {
        return new SeparateLineBorder(null, null, null, line(c, thickness));
    }


    private final Line top;
    private final Line left;
    private final Line bottom;
    private final Line right;

    /**
     * Constructs with individual line specifications.  <code>null</code> is
     * interpreted as an empty line.
     */
    public SeparateLineBorder(Line top, Line left, Line bottom, Line right) {
        this.top    = top == null    ? EMPTY_LINE : top;
        this.left   = left == null   ? EMPTY_LINE : left;
        this.bottom = bottom == null ? EMPTY_LINE : bottom;
        this.right  = right == null  ? EMPTY_LINE : right;
    }

    public Insets getBorderInsets(Component c) {
        return getBorderInsets(c, new Insets(0, 0, 0, 0));
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        insets.top    = top.thickness;
        insets.left   = left.thickness;
        insets.bottom = bottom.thickness;
        insets.right  = right.thickness;
        return insets;
    }

    public boolean isBorderOpaque() { return true; }

    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        Color origColor = g.getColor();
        g.translate(x, y);

        g.setColor(top.color);
        for (int i=0; i<top.thickness; ++i) {
            g.drawLine(0, i, w, i);
        }

        g.setColor(left.color);
        for (int i=0; i<left.thickness; ++i) {
            g.drawLine(i, top.thickness, i, h-bottom.thickness);
        }

        g.setColor(bottom.color);
        for (int i=0; i<bottom.thickness; ++i) {
            g.drawLine(0, h-i-1, w, h-i-1);
        }

        g.setColor(right.color);
        for (int i=0; i<right.thickness; ++i) {
            g.drawLine(w-i, top.thickness, w-i, h-bottom.thickness);
        }

        g.translate(-x, -y);
        g.setColor(origColor);
    }
}
