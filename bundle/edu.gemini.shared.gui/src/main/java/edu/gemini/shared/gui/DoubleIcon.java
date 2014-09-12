package edu.gemini.shared.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Combines two distinct icons side-by-side into one single icon.
 */
public class DoubleIcon implements Icon {
    private final Icon left;
    private final Icon right;
    private final int gap;

    /**
     * Creates a double icon with its left and right icons and the gap between
     * them.
     * @param left icon to show on the left
     * @param right icon to show on the right
     * @param gap gap in pixels between the left and right icons
     */
    public DoubleIcon(Icon left, Icon right, int gap) {
        if (gap < 0) throw new IllegalArgumentException("gap less than zero: " + gap);
        this.left  = left;
        this.right = right;
        this.gap   = gap;
    }

    public void paintIcon(Component owner, Graphics g, int x, int y) {
        int height = getIconHeight();
        int leftY  = (height - left.getIconHeight())/2;
        int rightY = (height - right.getIconHeight())/2;

        left.paintIcon(owner, g, x, y + leftY);
        right.paintIcon(owner, g, x + left.getIconWidth() + gap, y + rightY);
    }

    public int getIconWidth() {
        return left.getIconWidth() + gap + right.getIconWidth();
    }

    public int getIconHeight() {
        return Math.max(left.getIconHeight(), right.getIconHeight());
    }
}
