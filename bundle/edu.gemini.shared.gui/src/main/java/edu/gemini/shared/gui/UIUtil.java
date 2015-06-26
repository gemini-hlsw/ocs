package edu.gemini.shared.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Collection of user interface utility routines.
 */
public class UIUtil {

    /**
     * Computes the upper left-hand corner of a square (window) of the given
     * dimensions centered on the screen.  If the square is wider than the
     * screen size, the x coordinate of the returned Point will be 0.  If the
     * square is taller than the screen size, the y coordinate of the returned
     * Point will be 0.
     *
     * @return Point in screen coordinates of where the upper left-hand corner of
     * the window should be displayed
     */
    public static Point getUpperLeftCenteringCoordinate(int width, int height) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width / 2 - width / 2;
        int y = screenSize.height / 2 - height / 2;
        if ((x + width) > screenSize.width) {
            x = 0;
        }
        if ((y + height) > screenSize.height) {
            y = 0;
        }
        return new Point(x, y);
    }

    /**
     * Display an error message in a dialog box.
     */
    public static void error(String message) {
        JOptionPane jp = new JOptionPane(message, JOptionPane.ERROR_MESSAGE);
        JDialog jd = jp.createDialog(null, "Error");
        jd.setVisible(true);
    }

    /** Convenience routine to avoid working with a <code>Dimension</code>. */
    static public void setPreferredWidth(JComponent c, int iSize) {
        Dimension d = c.getPreferredSize();
        d.width = iSize;
        c.setPreferredSize(d);
    }

    /** Convenience routine to avoid working with a <code>Dimension</code>. */
    static public int getWidth(Component c) {
        Dimension d = c.getSize();
        return d.width;
    }

    /** Convenience routine to avoid working with a <code>Dimension</code>. */
    static public int getHeight(Component c) {
        Dimension d = c.getSize();
        return d.height;
    }

}

