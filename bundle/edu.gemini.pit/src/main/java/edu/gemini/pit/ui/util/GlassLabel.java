package edu.gemini.pit.ui.util;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingConstants;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

public class GlassLabel {

    private static final Timer timer = new Timer();

    public static void show(final JRootPane frame, final String message) {
        synchronized (frame) {
            // State gets messed up somewhere if we don't set the glasspane to
            // invisible when we swap. Swing guys did GlassPane wrong.
            frame.getGlassPane().setVisible(false);
            frame.setGlassPane(new JPanel() {{
                setLayout(new BorderLayout());
                setOpaque(true);
                setBackground(new Color(0, 0, 0, 32));
                add(new FlashLabel(frame, message), BorderLayout.CENTER);
            }});
            frame.getGlassPane().setVisible(true);
            frame.getGlassPane().invalidate();
        }
    }

    public static void hide(JRootPane frame) {
        synchronized (frame) {
            frame.getGlassPane().setVisible(false);
        }
    }

    @SuppressWarnings("serial")
    static class FlashLabel extends JLabel {

        private final JRootPane frame;
        private Component prev;
        private int alpha = 196;

        FlashLabel(JRootPane frame, String message) {
            super(" " + message + " "); // :-/
            this.frame = frame;
            this.prev = frame.getGlassPane();
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            Font f = getFont();
            setFont(f.deriveFont(f.getSize2D() * 2));
        }

        private static int width = 500;
        
        @Override
        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            int x = getBounds().width / 2 - width / 2;
            int y = getBounds().height / 2 - 30;
            g2d.setColor(new Color(64, 64, 64, alpha));
            g2d.fillRoundRect(x, y, width, 60, 20, 20);
            setForeground(new Color(255, 255, 255, alpha));
            super.paint(g);
        }

        class FadeTask extends TimerTask {
            public void run() {
                synchronized (frame) {
                    if (frame.getGlassPane() == FlashLabel.this) {
                        if (alpha > 0) {
                            alpha = Math.max(0, alpha - 32);
                            repaint();
                        } else {
                            while (prev instanceof FlashLabel)
                                prev = ((FlashLabel) prev).prev;
                            frame.getGlassPane().setVisible(false);
                            frame.setGlassPane(prev);
                        }
                    }
                }
            }
        }

    }

}
