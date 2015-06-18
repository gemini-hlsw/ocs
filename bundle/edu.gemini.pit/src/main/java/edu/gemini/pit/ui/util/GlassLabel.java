package edu.gemini.pit.ui.util;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingConstants;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

public class GlassLabel {

    public static void show(final JRootPane frame, final String message) {
        synchronized (frame) {
            // State gets messed up somewhere if we don't set the glasspane to
            // invisible when we swap. Swing guys did GlassPane wrong.
            frame.getGlassPane().setVisible(false);
            frame.setGlassPane(new JPanel() {
                // REL-2255 Starting in Java 1.7, painting doesn't obey the alpha value set on the background color
                // overriding paintComponent we can achieve the same result
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(getBackground());
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    // Paint children before disposing
                    super.paintChildren(g);
                    g2.dispose();
                }

                {
                setLayout(new BorderLayout());
                setOpaque(false);
                setBackground(new Color(0, 0, 0, 64));
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
