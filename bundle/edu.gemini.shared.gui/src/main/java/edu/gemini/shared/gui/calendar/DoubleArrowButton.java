/*
===================================================================

    DoubleArrowButton.java

    Created by Darrell Denlinger
    Copyright (c) 1999

===================================================================
*/

package edu.gemini.shared.gui.calendar;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.*;

public class DoubleArrowButton extends ArrowButton {

    /**
     * Constructs double arrow button in specified direction.
     * @param direction BasicArrowButton.EAST, WEST, NORTH, or SOUTH.
     */
    public DoubleArrowButton(int direction) {
        super(direction);
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
        if (h <= insets.top + insets.bottom || w <= insets.right + insets.left) {
            g.setColor(origColor);
            return;
        }
        if (isPressed)
            g.translate(1, 1);

        // Draw the arrow
        size = (int) Math.min((h - insets.top - insets.bottom) * getFraction(), (w - insets.left - insets.right) * getFraction());
        size = Math.max(size, 2);
        int x, y;
        if (direction == BasicArrowButton.EAST || direction == BasicArrowButton.WEST) {
            x = (w - 2 * size) / 2;
            y = (h - size) / 2;
        } else {
            x = (w - size) / 2;
            y = (h - 2 * size) / 2;
        }
        paintTriangle(g, x, y, size, direction, isEnabled);
        if (direction == BasicArrowButton.EAST || direction == BasicArrowButton.WEST) {
            x = (w - 2 * size) / 2 + size;
            y = (h - size) / 2;
        } else {
            x = (w - size) / 2;
            y = (h - 2 * size) / 2 + size;
        }
        paintTriangle(g, x, y, size, direction, isEnabled);

        // Reset the Graphics back to it's original settings
        if (isPressed) {
            g.translate(-1, -1);
        }
        g.setColor(origColor);
    }
}

