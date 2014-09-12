// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
//

package edu.gemini.shared.gui.calendar;

import java.awt.Color;

/**
 * This class contains general utility functions for dealing with Color..
 */
public class ColorUtil {

    public static final int RED = 0;

    public static final int GREEN = 1;

    public static final int BLUE = 2;

    public static final int COLOR_MAX = 255;

    public static final int BASE_LEVEL = 100;

    public static final float BRIGHTEN_FACTOR = (float) 1.5;

    public static final float DARKEN_FACTOR = (float) 0.92;

    /**
     * Returns a new color with specified components.
     */
    public static Color newColor(int[] components) {
        return new Color(components[0], components[1], components[2]);
    }

    public static int[] getComponents(Color c) {
        int[] componentValues = new int[3];
        componentValues[RED] = c.getRed();
        componentValues[GREEN] = c.getGreen();
        componentValues[BLUE] = c.getBlue();
        return componentValues;
    }

    /**
     * Returns whether the brightest component is RED, GREEN, or BLUE
     */
    public static int getDominantPrimaryColor(Color c) {
        int[] componentValues = getComponents(c);
        int dominantColor = RED;
        if (componentValues[GREEN] > componentValues[dominantColor]) {
            dominantColor = GREEN;
        }
        if (componentValues[BLUE] > componentValues[dominantColor]) {
            dominantColor = BLUE;
        }
        return dominantColor;
    }

    /**
     * Raises the value of all components to at least specified minimum.
     */
    public static void floor(int[] components, int min) {
        for (int i = 0; i < 3; ++i) {
            if (components[i] < min)
                components[i] = min;
        }
    }

    /**
     * Raises the value of all components to at least specified minimum.
     */
    public static void ceiling(int[] components, int max) {
        for (int i = 0; i < 3; ++i) {
            if (components[i] > max)
                components[i] = max;
        }
    }

    /**
     * Multiplies the value of all components by specified factor.
     */
    public static void multiply(int[] components, float factor) {
        for (int i = 0; i < 3; ++i) {
            components[i] = (int) (components[i] * factor);
        }
    }

    /**
     * This method attempts to create a light pleasing background color based
     * on given color.  It checks to see if any of R, G or B are dominant
     * and tries to create a light color based on that color.
     */
    public static Color lightBackground(Color c) {
        int[] componentValues = getComponents(c);
        int dominantColor = getDominantPrimaryColor(c);
        for (int i = 0; i < 3; ++i) {
            componentValues[i] = COLOR_MAX;
        }
        for (int i = 0; i < 3; ++i) {
            if (dominantColor != i) {
                componentValues[i] *= DARKEN_FACTOR;
            }
        }


        /*
          for (int i = 0; i < 3; ++i)
          {
            if (dominantColor != i && componentValues[i] < BASE_LEVEL)
            {
              componentValues[i] = BASE_LEVEL;
            }
          }
          for (int i = 0; i < 3; ++i)
          {
            componentValues[i] *= BRIGHTEN_FACTOR;
          }
        */
        return new Color(componentValues[0], componentValues[1], componentValues[2]);
    }

    /**
     * Returns a darkened form of specified color suitable for highlighting
     */
    public static Color highlightDarker(Color c) {
        return c.darker().darker().darker();
    }

    /**
     * Returns a brightened form of specified color suitable for highlighting
     */
    public static Color highlightBrighter(Color c) {
        int[] componentValues = getComponents(c);
        int dominantColor = getDominantPrimaryColor(c);
        floor(componentValues, BASE_LEVEL);
        multiply(componentValues, (float) 1.5);
        ceiling(componentValues, COLOR_MAX);
        int diff = COLOR_MAX - dominantColor;
        int target = (int) (COLOR_MAX - .3 * diff);
        if (componentValues[dominantColor] < target) {
            componentValues[dominantColor] = target;
        }
        return newColor(componentValues);
    }
}
