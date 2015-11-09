package jsky.image.gui;

import java.awt.Graphics2D;
import java.util.EventListener;


/**
 *  A callback interface for classes that need to draw graphics over an image.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public interface ImageGraphicsHandler extends EventListener {

    /** Called each time the image is repainted */
    void drawImageGraphics(BasicImageDisplay imageDisplay, Graphics2D g);
}


