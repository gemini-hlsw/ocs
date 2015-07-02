package jsky.app.ot.tpe;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * TPE cursor types.
 */
public enum TpeCursor {
    browse() {
        public Cursor create() {
            return Cursor.getDefaultCursor();
        }
    },
    drag() {
        public Cursor create() {
            return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        }
    },
    erase() {
        public Cursor create() {
            // Create a custom red X cursor.
            BufferedImage bim = createImage();
            int w = bim.getWidth();
            int h = bim.getHeight();

            Graphics2D g2d = bim.createGraphics();

            Color transparent = new Color(0, 0, 0, 0);
            g2d.setColor(transparent);
            g2d.setComposite(AlphaComposite.Src);
            g2d.fill(new Rectangle2D.Float(0, 0, w, h));

            g2d.setColor(Color.red);

            g2d.drawLine(0, 1, w-1, h);
            g2d.drawLine(0, 0, w,   h);
            g2d.drawLine(1, 0, w,   h-1);

            g2d.drawLine(0, h-2, w-2, 0);
            g2d.drawLine(0, h-1, w-1, 0);
            g2d.drawLine(1, h-1, w-1, 1);

            int mw = w/2;
            int mh = h/2;

            g2d.dispose();

            Toolkit tk = Toolkit.getDefaultToolkit();
            Image image = tk.createImage(bim.getSource());
            return tk.createCustomCursor(image, new Point(mw, mh), "erase");
        }
    },
    add() {
        public Cursor create() {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
    },
    ;

    private Cursor cursor;

    public abstract Cursor create();

    public Cursor get() {
        if (cursor == null) cursor = create();
        return cursor;
    }

    private static BufferedImage createImage() {
        int width = 13;
        int height = 13;
        Dimension dim = Toolkit.getDefaultToolkit().getBestCursorSize(width, height);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        return gc.createCompatibleImage(dim.width, dim.height, Transparency.BITMASK);
    }
}
