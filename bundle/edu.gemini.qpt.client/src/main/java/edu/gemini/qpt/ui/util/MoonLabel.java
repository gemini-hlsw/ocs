package edu.gemini.qpt.ui.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;

import javax.swing.Icon;
import javax.swing.JLabel;

@SuppressWarnings("serial")
public class MoonLabel extends JLabel {

    public MoonLabel(int size, double illum, boolean waxing) {
        if (illum < 0 || illum > 1) throw new IllegalArgumentException("Illumination ratio must be between 0 and 1.");
        setText(String.format("%1.0f%%", 100 * illum));
        setIcon(new MoonIcon(size, illum, waxing));
    }

    static class MoonIcon implements Icon {

        private final int size, baseAngle;
        private final double illum;
        
        public MoonIcon(int size, double illum, boolean waxing) {
            this.size = size;
            this.illum = illum;
            this.baseAngle = waxing ? 90 : 270;
        }

        public int getIconHeight() {
            return size;
        }

        public int getIconWidth() {
            return size;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g;

            // Dark side
            Arc2D.Double darkSize = halfMoon(x, y, getIconWidth() - 1, getIconHeight() - 1, 0);
            g2d.setColor(Color.BLACK);
            g2d.fill(darkSize);

            // Light side
            Arc2D.Double brightSide = halfMoon(x, y, getIconWidth() - 1, getIconHeight() - 1, 180);
            g2d.setColor(Color.WHITE);
            g2d.fill(brightSide);
            
            // Additional shadow or light
            if (illum < 0.5) {
                double width = (0.5 - illum) * (double) getIconWidth();
                Arc2D.Double shadow = halfMoon(x + size / 2 - width, y, width * 2 - 1, size - 1, 180);
                g2d.setColor(Color.BLACK);
                g2d.fill(shadow);                
            } else if (illum > 0.5) {
                double width = (illum - 0.5) * (double) getIconWidth();
                Arc2D.Double light = halfMoon(x + size / 2 - width, y, width * 2 - 1, size - 1, 0);
                g2d.setColor(Color.WHITE);
                g2d.fill(light);                
            }

            // Gray outline
            g2d.setColor(Color.GRAY);
            Arc2D.Double circle = new Arc2D.Double(x, y, size - 1, size - 1, 0, 360, Arc2D.OPEN);
            g2d.draw(circle);
        }
        
        private Arc2D.Double halfMoon(double x, double y, double width, double height, int angle, int degrees) {
            return new Arc2D.Double(x, y, width, height, baseAngle + angle, degrees, Arc2D.OPEN);
        }

        private Arc2D.Double halfMoon(double x, double y, double width, double height, int angle) {
            return halfMoon(x, y, width, height, angle, 180);
        }
        
    }

//    public static void main(String[] args) {        
//        JFrame frame = new JFrame();
//        frame.getContentPane().setLayout(new FlowLayout());
//        for (double i = 0; i <= 2.0; i += 0.1) {
//            if (i <= 1.0) {
//                frame.add(new MoonLabel(20, i, true));
//            } else {
//                frame.add(new MoonLabel(20, 2 - i, false));
//            }
//        }
//        frame.pack();
//        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//        frame.setVisible(true);
//    }
    
}
