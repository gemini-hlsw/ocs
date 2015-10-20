package jsky.app.ot.tpe;

import java.awt.*;
import java.net.*;

import jsky.image.gui.ImageDisplayStatusPanel;
import jsky.navigator.NavigatorImageDisplayControl;
import jsky.image.gui.DivaMainImageDisplay;

/**
 * Extends the NavigatorImageDisplayControl class by adding Gemini specific features.
 *
 * @version $Revision: 39998 $
 * @author Allan Brighton
 */
public class TpeImageDisplayControl extends NavigatorImageDisplayControl {

    /**
     * Construct a TpeImageDisplayControl widget.
     *
     * @param parent the top level parent frame (or internal frame) used to close the window
     * @param size   the size (width, height) to use for the pan and zoom windows.
     */
    public TpeImageDisplayControl(Component parent, int size) {
        super(parent, size);
    }

    /**
     * Make a TpeImageDisplayControl widget with the default settings.
     *
     * @param parent The top level parent frame (or internal frame) used to close the window
     */
    public TpeImageDisplayControl(Component parent) {
        super(parent);
    }


    /**
     * Make a TpeImageDisplayControl widget with the default settings and display the contents
     * of the image file pointed to by the URL.
     *
     * @param parent The top level parent frame (or internal frame) used to close the window
     * @param url The URL for the image to load
     */
    public TpeImageDisplayControl(Component parent, URL url) {
        super(parent, url);
    }


    /**
     * Make a TpeImageDisplayControl widget with the default settings and display the contents
     * of the image file.
     *
     * @param parent The top level parent frame (or internal frame) used to close the window
     * @param filename The image file to load
     */
    public TpeImageDisplayControl(Component parent, String filename) {
        super(parent, filename);
    }

    /** Make and return the image display window */
    @Override
    protected DivaMainImageDisplay makeImageDisplay() {
        return new TpeImageWidget(parent);
    }

    /** Make and return the status panel */
    @Override
    protected ImageDisplayStatusPanel makeStatusPanel() {
        return new TpeImageDisplayStatusPanel();
    }
}

