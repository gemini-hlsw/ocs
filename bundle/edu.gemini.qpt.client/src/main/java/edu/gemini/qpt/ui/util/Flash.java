package edu.gemini.qpt.ui.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;


public class Flash {
        
    private static final Timer timer = new Timer();
    
    public static void flash(JFrame frame, String message) {
        synchronized (frame) {
            // State gets messed up somewhere if we don't set the glasspane to
            // invisible when we swap. Swing guys did GlassPane wrong.
            frame.getGlassPane().setVisible(false);
            frame.setGlassPane(new FlashLabel(frame, message));
            frame.getGlassPane().setVisible(true);
            frame.getGlassPane().invalidate();
        }
    }
    
    @SuppressWarnings("serial")
    private static class FlashLabel extends JLabel {
        
        private final JFrame frame;
        private Component prev;        
        private int alpha = 196;
        
        FlashLabel(JFrame frame, String message) {            
            super(" " + message + " "); // :-/
            this.frame = frame;
            this.prev = frame.getGlassPane();            
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            Font f = getFont();
            setFont(f.deriveFont(f.getSize2D() * 2));
            timer.schedule(new FadeTask(), 500, 50);        
        }
        
        @Override
        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            int x = getBounds().width / 2 - 100;
            int y = getBounds().height / 2 - 30;
            g2d.setColor(new Color(64, 64, 64, alpha));
            g2d.fillRoundRect(x, y, 200, 60, 20, 20);
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
