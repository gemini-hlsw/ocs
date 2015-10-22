package jsky.navigator;

import jsky.image.gui.DivaMainImageDisplay;
import jsky.image.gui.ImageDisplayControl;
import jsky.image.gui.ImageDisplayControlFrame;
import jsky.image.gui.ImageDisplayMenuBar;
import jsky.image.gui.ImageDisplayToolBar;

/**
 * Extends ImageDisplayControlFrame to add catalog support.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public class NavigatorImageDisplayFrame extends ImageDisplayControlFrame {

    /**
     * Create a top level window containing an ImageDisplayControl panel
     * with the default settings.
     */
    public NavigatorImageDisplayFrame() {
        super();
    }

    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param fileOrUrl The file name or URL of an image to display.
     */
    public NavigatorImageDisplayFrame(String fileOrUrl) {
        super(fileOrUrl);
    }

    /** Make and return the menubar */
    @Override
    protected ImageDisplayMenuBar makeMenuBar(DivaMainImageDisplay mainImageDisplay, ImageDisplayToolBar toolBar) {
        return new NavigatorImageDisplayMenuBar((NavigatorImageDisplay) mainImageDisplay,
                                                 toolBar);
    }

    /**
     * Make and return the image display control frame.
     *
     * @param size the size (width, height) to use for the pan and zoom windows.
     */
    @Override
    protected ImageDisplayControl makeImageDisplayControl(int size) {
        return new NavigatorImageDisplayControl(this, size);
    }
}

