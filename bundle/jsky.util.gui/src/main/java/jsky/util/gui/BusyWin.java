package jsky.util.gui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Utility class used to disable GUI input while work is in progress.
 *
 * @author Allan Brighton
 */
public class BusyWin {

    /**
     * Temporarily disable (or enable) all frames except the given one.
     *
     * @param busy   if true, display the busy cursor and disable all frames except
     *               the given one.
     *
     * @param parent if not null, this frame (JFrame or JInternalFrame)
     *               is ignored (not enabled/disabled)
     */
    public static void setBusy(boolean busy, Component parent) {

        for (Frame c : Frame.getFrames()) {
            if (c == parent || !(c instanceof JFrame) || !c.isVisible())
                continue;
            JFrame frame = (JFrame) c;
            Component glassPane = frame.getGlassPane();
            if (!(glassPane instanceof GlassPane)) {
                glassPane = new GlassPane();
                frame.setGlassPane(glassPane);
            }
            glassPane.setVisible(busy);

            if (busy) {
                // force immediate update
                Graphics g = frame.getGraphics();
                if (g != null) {
                    glassPane.paint(g);
                }
            }
        }

    }


    /**
     * Temporarily disable (or enable) all of the application's frames.
     *
     * @param busy if true, display the busy cursor and disable all frames except
     *             the given one.
     */
    public static void setBusy(final boolean busy) {
        setBusy(busy, null);
    }

    /**
     * Temporarily show the busy cursor for all frames except the given one.
     *
     * @param parent if not null, this frame (JFrame or JInternalFrame)
     *               is ignored (not enabled/disabled)
     */
    public static void showBusy(final Component parent) {
        setBusy(true, parent);
        SwingUtilities.invokeLater(() -> setBusy(false, parent));
    }


    /**
     * Temporarily show the busy cursor for all application frames.
     *
     */
    public static void showBusy() {
        showBusy(null);
    }

}

/**
 * This local class is used to block input events while in busy mode.
 */
class GlassPane extends JComponent implements MouseListener, MouseMotionListener {

    public GlassPane() {
        addMouseListener(this);
        addMouseMotionListener(this);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public void mouseMoved(MouseEvent e) {
        e.consume();
    }

    public void mouseDragged(MouseEvent e) {
        e.consume();
    }

    public void mouseClicked(MouseEvent e) {
        e.consume();
    }

    public void mouseEntered(MouseEvent e) {
        e.consume();
    }

    public void mouseExited(MouseEvent e) {
        e.consume();
    }

    public void mousePressed(MouseEvent e) {
        e.consume();
    }

    public void mouseReleased(MouseEvent e) {
        e.consume();
    }
}
