package edu.gemini.shared.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

/**
 * Decorates an Image or the Image contained in an ImageIcon with a (presumably)
 * small badge whose location relative to the main Image may be specified.
 */
public final class ImageDecorator {
    private ImageDecorator() {}

    // Horrible interaction with the ImageObserver.
    private static final class ImageDimension implements ImageObserver {
        private boolean isReady = false;

        private final Image image;

        ImageDimension(Image image) { this.image = image; }

        private Dimension getSize() {
            int w = image.getWidth(this);
            int h = image.getHeight(this);

            if ((w >= 0) && (h >= 0)) {
                return new Dimension(w, h);
            } else {
                waitUntilReady();
                return new Dimension(image.getWidth(null), image.getHeight(null));
            }
        }

        private synchronized void waitUntilReady() {
            while (!isReady) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    return;
                }
            }
        }

        @Override
        public synchronized boolean imageUpdate(Image img, int infoFlags, int x, int y, int width, int height) {
            if (isReady) return true;

            if (infoFlags == ALLBITS) {
                isReady = true;
                notifyAll();
            }
            return isReady;
        }

        static Dimension apply(Image image) {
            return new ImageDimension(image).getSize();
        }
    }

    private enum LocationCalc {
        FIRST { int adj(int in, int dec) { return 0; } },
        MIDDLE { int adj(int in, int dec) { return (in - dec)/2; } },
        LAST { int adj(int in, int dec) { return in - dec; } };

        abstract int adj(int in, int dec);

        int inLoc(int in, int dec) {
            final int a = adj(in, dec);
            return (a < 0) ? -a : 0;
        }

        int decLoc(int in, int dec) {
            final int a = adj(in, dec);
            return (a < 0) ? 0 : a;
        }
    }

    public enum XLocation {
        LEFT(LocationCalc.FIRST),
        CENTER(LocationCalc.MIDDLE),
        RIGHT(LocationCalc.LAST),
        ;

        final LocationCalc calc;
        private XLocation(LocationCalc lc) { this.calc = lc; }
    }

    public enum YLocation {
        TOP(LocationCalc.FIRST),
        CENTER(LocationCalc.MIDDLE),
        BOTTOM(LocationCalc.LAST),
        ;

        final LocationCalc calc;
        private YLocation(LocationCalc lc) { this.calc = lc; }
    }

    /**
     * Decorates the <code>in</code> image with <code>decoration</code>, placing
     * the decoration in the bottom right corner and returning a new combined
     * Image.
     *
     * @param in main image upon which the decoration will be drawn
     * @param decoration decoration to draw on the main image
     *
     * @return new combined Image
     */
    public static Image decorate(Image in, Image decoration) {
        return decorate(in, decoration, XLocation.RIGHT, YLocation.BOTTOM);
    }

    /**
     * Decorates the <code>in</code> image with <code>decoration</code>, placing
     * the decoration according to the specified location.
     *
     * @param in main image upon which the decoration will be drawn
     * @param decoration decoration to draw on the main image
     * @param xLoc where to place the decoration horizontally relative to the
     *             main image
     * @param yLoc where to place the decoration vertically relative to the
     *             main image
     *
     * @return new combined Image
     */
    public static Image decorate(Image in, Image decoration, XLocation xLoc, YLocation yLoc) {
        final Dimension nSize = ImageDimension.apply(in);
        final Dimension dSize = ImageDimension.apply(decoration);

        final GraphicsEnvironment   env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice        dev = env.getDefaultScreenDevice();
        final GraphicsConfiguration cnf = dev.getDefaultConfiguration();

        final int w = Math.max(nSize.width, dSize.width);
        final int h = Math.max(nSize.height, dSize.height);

        final BufferedImage img = cnf.createCompatibleImage(w, h, Transparency.TRANSLUCENT);

        final int nX = xLoc.calc.inLoc(nSize.width, dSize.width);
        final int nY = yLoc.calc.inLoc(nSize.height, dSize.height);
        final int dX = xLoc.calc.decLoc(nSize.width, dSize.width);
        final int dY = yLoc.calc.decLoc(nSize.height, dSize.height);

        final Graphics2D g2d = (Graphics2D) img.getGraphics();
        g2d.drawImage(in, nX, nY, null);
        g2d.drawImage(decoration, dX, dY, null);
        g2d.dispose();

        return img;
    }

    /**
     * Decorates the <code>in</code> ImageIcon with <code>decoration</code>,
     * returning a new ImageIcon with the combined image.
     *
     * @param in main image icon upon whose Image the decoration will be drawn
     * @param decoration decoration to draw on the main image
     *
     * @return new combined Image in a new ImageIcon
     */
    public static ImageIcon decorate(ImageIcon in, ImageIcon decoration) {
        return decorate(in, decoration, XLocation.RIGHT, YLocation.BOTTOM);
    }

    /**
     * Decorates the <code>in</code> ImageIcon with <code>decoration</code>,
     * returning a new ImageIcon with the combined image according to the
     * specified location.
     *
     * @param in main image icon upon whose Image the decoration will be drawn
     * @param decoration decoration to draw on the main image
     * @param xLoc where to place the decoration horizontally relative to the
     *             main image
     * @param yLoc where to place the decoration vertically relative to the
     *             main image
     *
     * @return new combined Image in a new ImageIcon
     */
    public static ImageIcon decorate(ImageIcon in, ImageIcon decoration, XLocation xLoc, YLocation yLoc) {
        return new ImageIcon(decorate(in.getImage(), decoration.getImage(), xLoc, yLoc));
    }
}
