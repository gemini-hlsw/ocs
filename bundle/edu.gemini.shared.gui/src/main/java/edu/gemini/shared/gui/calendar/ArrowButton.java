/*
===================================================================

    ArrowButton.java

    Created by Claude Duguay
    Copyright (c) 1999

===================================================================
*/

package edu.gemini.shared.gui.calendar;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.*;

public class ArrowButton extends BasicArrowButton {

    protected static final int DEFAULT_MARGIN = 2;

    protected static final double DEFAULT_FRACTION = 1.0 / 3.0;

    private double _fraction = DEFAULT_FRACTION;

    /**
     * Constructs single arrow button in specified direction.
     * @param direction BasicArrowButton.EAST, WEST, NORTH, or SOUTH.
     */
    public ArrowButton(int direction) {
        super(direction);
        setMargin(new Insets(DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN));
    }

    public void paint(Graphics g) {
        Color origColor;
        boolean isPressed, isEnabled;
        int w, h, size;
        w = getSize().width;
        h = getSize().height;
        origColor = g.getColor();
        isPressed = getModel().isPressed();
        isEnabled = isEnabled();
        g.setColor(getBackground());
        g.fillRect(1, 1, w - 2, h - 2);

        /// Draw the proper Border
        if (isPressed) {
            g.setColor(UIManager.getColor("controlShadow"));
            g.drawRect(0, 0, w - 1, h - 1);
        } else {
            g.setColor(UIManager.getColor("controlLtHighlight"));
            g.drawLine(0, 0, 0, h - 1);
            g.drawLine(1, 0, w - 2, 0);
            g.setColor(UIManager.getColor("controlShadow"));
            g.drawLine(0, h - 1, w - 1, h - 1);
            g.drawLine(w - 1, h - 1, w - 1, 0);
        }
        Insets insets = getMargin();

        // If there's no room to draw arrow, bail
        if (h <= insets.top + insets.bottom || w <= insets.left + insets.right) {
            g.setColor(origColor);
            return;
        }
        if (isPressed) {
            g.translate(1, 1);
        }

        // Draw the arrow
        // calculate bounding box of triangle image
        size = (int) Math.min((h - insets.top - insets.bottom) * getFraction(), (w - insets.left - insets.right) * getFraction());
        size = Math.max(size, 2);
        // inherited from BasicArrowButton
        paintTriangle(g, (w - size) / 2, (h - size) / 2, size, direction, isEnabled);

        // Reset the Graphics back to it's original settings
        if (isPressed) {
            g.translate(-1, -1);
        }
        g.setColor(origColor);
    }

    /**
     * Size of image will be this fraction of cell size minus margin.
     */
    public double getFraction() {
        return _fraction;
    }

    /**
     * Size of image will be this fraction of cell size minus margin.
     */
    public void setFraction(double fraction) {
        _fraction = fraction;
    }
}

