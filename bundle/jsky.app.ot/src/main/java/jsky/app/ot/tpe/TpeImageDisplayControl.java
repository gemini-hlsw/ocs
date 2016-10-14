package jsky.app.ot.tpe;

import java.awt.*;

import jsky.image.gui.ImageDisplayControl;
import jsky.image.gui.ImageDisplayStatusPanel;
import jsky.image.gui.DivaMainImageDisplay;

/**
 * Extends the NavigatorImageDisplayControl class by adding Gemini specific features.
 *
 * @version $Revision: 39998 $
 * @author Allan Brighton
 */
public class TpeImageDisplayControl extends ImageDisplayControl {

    /**
     * Construct a TpeImageDisplayControl widget.
     *
     * @param parent the top level parent frame (or internal frame) used to close the window
     * @param size   the size (width, height) to use for the pan and zoom windows.
     */
    TpeImageDisplayControl(Component parent, int size) {
        super(parent, size);
    }

    /** Make and return the image display window */
    @Override
    protected TpeImageWidget makeImageDisplay() {
        return new TpeImageWidget(parent);
    }

    /** Make and return the status panel */
    @Override
    protected ImageDisplayStatusPanel makeStatusPanel() {
        return new TpeImageDisplayStatusPanel();
    }
}

