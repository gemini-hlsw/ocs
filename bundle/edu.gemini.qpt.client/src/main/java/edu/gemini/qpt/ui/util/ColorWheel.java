package edu.gemini.qpt.ui.util;

import java.awt.Color;

/**
 * A set of seven background pastels.
 */
public class ColorWheel {

    private static final Color[] colors = {
        new Color(0xDADAFF),
        new Color(0xDAFFDA),
        new Color(0xFFDADA),
        new Color(0xFFDAFF),
        new Color(0xFFFFDA),
        new Color(0xFFFFFF),
        new Color(0xFFDADA),
    };
    
    /**
     * Returns color n % 7.
     */
    public static Color get(int index) {
        return colors[index % colors.length];
    }
    
}
