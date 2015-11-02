package jsky.navigator;

import java.awt.*;
import java.net.*;

import jsky.image.gui.ImageDisplayControl;
import jsky.image.gui.DivaMainImageDisplay;

/**
 * Extends the ImageDisplayControl class by adding support for
 * browsing catalogs and plotting catalog symbols on the image.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public class NavigatorImageDisplayControl extends ImageDisplayControl {

    /**
     * Construct a NavigatorImageDisplayControl widget.
     *
     * @param parent the top level parent frame (or internal frame) used to close the window
     * @param size   the size (width, height) to use for the pan and zoom windows.
     */
    public NavigatorImageDisplayControl(Component parent, int size) {
        super(parent, size);
    }

    /** Make and return the image display window */
    @Override
    protected DivaMainImageDisplay makeImageDisplay() {
        return new NavigatorImageDisplay(parent);
    }
}

