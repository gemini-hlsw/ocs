package edu.gemini.shared.gui.calendar;

import java.awt.Graphics;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Dimension;

/**
 * This class contains general utility functions for dealing with text.
 */
public class TextUtil {

    // Font calculations will start at this size and then try to
    // narrow in on appropriate size.
    private static final int DEFAULT_FONT_SIZE_TARGET = 12;

    /** Font size calculations will not return a font size smaller than this. */
    public static final int MIN_FONT_SIZE = 1;

    // The plot of rendered size vs. font size has an offset
    private static final int WIDTH_OFFSET = 1;  // multiply by string len

    private static final int HEIGHT_OFFSET = 3;

    /**
     * Returns the largest font size of the specified name and style
     * that will render the specified text string smaller than the
     * target width.
     * @param g - need a graphics context to get a FontMetrics object to
     * do the calculations.
     * @param text - calculate font size so that this string is rendered as close
     * to but smaller than targetWidth.
     * @param  targetWidth - want the string to be as close to this width
     * as possible.
     * @param fontName - calculate using this font name
     * @param fontStyle - calculate using this font style
     * @param sizeHint - calculation might go faster if you supply a
     * font size near the correct answer, algorithm will try different fonts
     * starting at size sizeHint until it determines the answer.
     * Algorithm converges rapidly, gets correct usually in 4 iterations or less.
     * @return - the ideal font size.
     */
    public static int fontSizeForWidth(Graphics g, String text, int targetWidth, String fontName, int fontStyle, int sizeHint) {
        int high = Integer.MAX_VALUE;
        int low = MIN_FONT_SIZE;
        FontMetrics fm;
        Font font;
        int size = sizeHint;
        int width;
        int widthOffset = text.length() * WIDTH_OFFSET;
        while (high - low > 1) {
            // If client passed in bad font name, Java will produce a default font.
            font = new Font(fontName, fontStyle, size);
            g.setFont(font);
            fm = g.getFontMetrics();
            width = fm.stringWidth(text);
            if (width > targetWidth) {
                if (size <= MIN_FONT_SIZE)
                    return MIN_FONT_SIZE;
                high = size;
            } else {
                low = size;
            }
            size = (int) ((double) size / (width - widthOffset) * (targetWidth - width) + size);
            if (size >= high)
                size = high - 1;
            else if (size <= low)
                size = low + 1;
        }
        return low;
    }

    /**
     * Returns the largest font size of the specified name and style
     * that will render the specified text string smaller than the
     * target width.
     * @param g - need a graphics context to get a FontMetrics object to
     * do the calculations.
     * @param text - calculate font size so that this string is rendered as close
     * to but smaller than targetWidth.
     * @param  targetWidth - want the string to be as close to this width
     * as possible.
     * @param fontName - calculate using this font name
     * @param fontStyle - calculate using this font style
     * @return - the ideal font size.
     */
    public static int fontSizeForWidth(Graphics g, String text, int targetWidth, String fontName, int fontStyle) {
        return fontSizeForWidth(g, text, targetWidth, fontName, fontStyle, DEFAULT_FONT_SIZE_TARGET);
    }

    /**
     * Returns the largest font size of the specified name and style
     * that will render the specified text string smaller than the
     * target height.
     * @param g - need a graphics context to get a FontMetrics object to
     * do the calculations.
     * @param  targetHeight - want the string to be as close to this height
     * as possible.
     * @param fontName - calculate using this font name
     * @param fontStyle - calculate using this font style
     * @param sizeHint - calculation might go faster if you supply a
     * font size near the correct answer, algorithm will try different fonts
     * starting at size sizeHint until it determines the answer.
     * Algorithm converges rapidly, gets correct usually in 4 iterations or less.
     * @return - the ideal font size.
     */
    public static int fontSizeForHeight(Graphics g, int targetHeight, String fontName, int fontStyle, int sizeHint) {
        int high = Integer.MAX_VALUE;
        int low = MIN_FONT_SIZE;
        FontMetrics fm;
        Font font;
        int size = sizeHint;
        int height;
        int heightOffset = HEIGHT_OFFSET;
        while (high - low > 1) {
            // If client passed in bad font name, Java will produce a default font.
            font = new Font(fontName, fontStyle, size);
            g.setFont(font);
            fm = g.getFontMetrics();
            height = getFontHeight(fm);
            if (height > targetHeight) {
                if (size <= MIN_FONT_SIZE)
                    return MIN_FONT_SIZE;
                high = size;
            } else {
                low = size;
            }
            size = (int) ((double) size / (height - heightOffset) * (targetHeight - height) + size);
            if (size >= high)
                size = high - 1;
            else if (size <= low)
                size = low + 1;
        }
        return low;
    }

    /**
     * Returns the largest font size of the specified name and style
     * that will render the specified text string smaller than the
     * target height.
     * @param g - need a graphics context to get a FontMetrics object to
     * do the calculations.
     * @param  targetHeight - want the string to be as close to this height
     * as possible.
     * @param fontName - calculate using this font name
     * @param fontStyle - calculate using this font style
     * @return - the ideal font size.
     */
    public static int fontSizeForHeight(Graphics g, int targetHeight, String fontName, int fontStyle) {
        return fontSizeForHeight(g, targetHeight, fontName, fontStyle, DEFAULT_FONT_SIZE_TARGET);
    }

    /**
     * Returns the largest font size of the specified name and style
     * that will render the specified text string that fits in the
     * specified dimensions.
     * @param g - need a graphics context to get a FontMetrics object to
     * do the calculations.
     * @param text - calculate font size so that this string is rendered as
     * large as possible but still fits in the specified box.
     * @param  targetDimension - want the string to be as large as possible
     * and still fit in this box.
     * @param fontName - calculate using this font name
     * @param fontStyle - calculate using this font style
     * @param sizeHint - calculation might go faster if you supply a
     * font size near the correct answer, algorithm will try different fonts
     * starting at size sizeHint until it determines the answer.
     * Algorithm converges rapidly, gets correct usually in 4 iterations or less.
     * @return - the ideal font size.
     */
    public static int fontSizeForDimension(Graphics g, String text, Dimension targetDimension, String fontName, int fontStyle, int sizeHint) {
        int size = fontSizeForWidth(g, text, targetDimension.width, fontName, fontStyle, sizeHint);
        int size2 = fontSizeForHeight(g, targetDimension.height, fontName, fontStyle, sizeHint);
        if (size2 < size)
            size = size2;
        return size;
    }

    /**
     * Returns the largest font size of the specified name and style
     * that will render the specified text string that fits in the
     * specified dimensions.
     * @param g - need a graphics context to get a FontMetrics object to
     * do the calculations.
     * @param text - calculate font size so that this string is rendered as
     * large as possible but still fits in the specified box.
     * @param  targetDimension - want the string to be as large as possible
     * and still fit in this box.
     * @param fontName - calculate using this font name
     * @param fontStyle - calculate using this font style
     * @return - the ideal font size.
     */
    public static int fontSizeForDimension(Graphics g, String text, Dimension targetDimension, String fontName, int fontStyle) {
        return fontSizeForDimension(g, text, targetDimension, fontName, fontStyle, DEFAULT_FONT_SIZE_TARGET);
    }

    /**
     * Returns the height of the font implied by the specified FontMetrics object.
     */
    public static int getFontHeight(FontMetrics fm) {
        return (fm.getAscent() + fm.getDescent());
    }

    /**
     * Returns the height of a line of text of the font implied by the
     * specified FontMetrics object.
     */
    public static int getLineHeight(FontMetrics fm) {
        return (fm.getAscent() + fm.getDescent() + fm.getLeading());
    }

}
