/*
=====================================================================

	ThinBorder.java

	Created by Claude Duguay
	Copyright (c) 1999

=====================================================================
*/

package edu.gemini.shared.gui;

import java.awt.*;
import javax.swing.border.*;

/**
 * This is a thin and less obtrusive border than stock Swing borders.
 */

public class ThinBorder implements Border {

    public static final int RAISED = 0;

    public static final int LOWERED = 1;

    public static final int thickness = 1;

    protected int type = RAISED;

    protected Color highlight;

    protected Color shadow;

    public ThinBorder() {
        this(LOWERED, null, null);
    }

    public ThinBorder(int type) {
        this(type, null, null);
    }

    public ThinBorder(int type, Color highlight, Color shadow) {
        this.type = type;
        this.highlight = highlight;
        this.shadow = shadow;
    }

    public boolean isBorderOpaque() {
        return true;
    }

    public Insets getBorderInsets(Component component) {
        return new Insets(thickness, thickness, thickness, thickness);
    }

    public Color getHightlightColor(Component c) {
        if (highlight == null)
            highlight = c.getBackground().brighter();
        return highlight;
    }

    public Color getShadowColor(Component c) {
        if (shadow == null)
            shadow = c.getBackground().darker();
        return shadow;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        Color hi = (type == RAISED ? getHightlightColor(c) : getShadowColor(c));
        Color lo = (type == RAISED ? getShadowColor(c) : getHightlightColor(c));
        for (int i = thickness - 1; i >= 0; i--) {
            g.setColor(hi);
            g.drawLine(x + i, y + i, x + w - i - 1, y + i);
            g.drawLine(x + i, y + i, x + i, x + h - i - 1);
            g.setColor(lo);
            g.drawLine(x + w - i - 1, y + i, x + w - i - 1, y + h - i - 1);
            g.drawLine(x + i, y + h - i - 1, x + w - i - 1, y + h - i - 1);
        }
    }

}
