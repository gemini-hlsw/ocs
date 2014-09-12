package edu.gemini.shared.gui;

import javax.swing.*;
import java.awt.*;

/**
 * An Icon that accepts a decoration on it. The decoration can be
 * located in any of the bordes of the main icon, specified by
 * the <code>Location</code>.
 * <p/>
 * Code borrowed from http://www.javalobby.org/forums/thread.jspa?threadID=16326&tstart=15
 * $Id: DecoratedIcon.java 7706 2007-04-13 21:12:18Z anunez $
 *
 */
public class DecoratedIcon implements Icon {

    private Icon originalIcon;
    private Icon decorationIcon;
    private int xDiff;
    private int yDiff;
    private Location location;


    public enum Location {
        UPPER_LEFT,
        UPPER_RIGHT,
        LOWER_LEFT,
        LOWER_RIGHT
    }


    public DecoratedIcon(Icon original, Icon decoration) {
        this(original, decoration, Location.LOWER_RIGHT);
    }

    public DecoratedIcon(Icon original, Icon decoration, Location location) {
        this.originalIcon = original;
        this.decorationIcon = decoration;
        this.location = location;
        if (decoration.getIconHeight() > original.getIconHeight() ||
                decoration.getIconWidth() > original.getIconWidth()) {
            throw new IllegalArgumentException("Decoration must be smaller than the original icon");
        }
        xDiff = originalIcon.getIconWidth() - decorationIcon.getIconWidth();
        yDiff = originalIcon.getIconHeight() - decorationIcon.getIconHeight();
    }

    public void paintIcon(Component owner, Graphics g, int x, int y) {

        //first the original icon
        originalIcon.paintIcon(owner, g, x, y);
        int decorationX = x;
        int decorationY = y;

        //augment x
        if (location == Location.UPPER_RIGHT || location == Location.LOWER_RIGHT) {
            decorationX += xDiff;
        }
        // augment y.
        if (location == Location.LOWER_LEFT || location == Location.LOWER_RIGHT) {
            decorationY += yDiff;
        }
        //add the decoration
        decorationIcon.paintIcon(owner, g, decorationX, decorationY);

    }

    public int getIconWidth() {
        return originalIcon.getIconHeight();
    }

    public int getIconHeight() {
        return originalIcon.getIconWidth();

    }
}
