package jsky.app.ot.tpe;

import jsky.image.gui.*;

import java.awt.*;

/**
 * Extends NavigatorImageDisplayFrame to add OT/TPE specific features.
 *
 * @version $Revision: 21408 $
 * @author Allan Brighton
 */
public class TpeImageDisplayFrame extends ImageDisplayControlFrame {

    /** Tool bar with Tpe specific commands */
    private TpeToolBar tpeToolBar;

    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param fileOrUrl The file name or URL of an image to display.
     */
    TpeImageDisplayFrame(String fileOrUrl) {
        super(fileOrUrl);
    }

    /** Make and return the menubar */
    @Override
    protected ImageDisplayMenuBar makeMenuBar(DivaMainImageDisplay mainImageDisplay, ImageDisplayToolBar toolBar) {
        return new TpeImageDisplayMenuBar((TpeImageWidget) mainImageDisplay, toolBar);
    }

    /**
     * Make and return the image display control frame.
     *
     * @param size the size (width, height) to use for the pan and zoom windows.
     */
    @Override
    protected ImageDisplayControl makeImageDisplayControl(int size) {
        return new TpeImageDisplayControl(this, size);
    }

    /** Make and return the toolbar */
    @Override
    protected ImageDisplayToolBar makeToolBar(TpeImageWidget mainImageDisplay) {
        // add the Tpe side bar while we are at it...
        tpeToolBar = new TpeToolBar(mainImageDisplay);
        getContentPane().add(tpeToolBar, BorderLayout.WEST);

        // Dragging can cause problems with two tool bars...
        ImageDisplayToolBar toolBar = new TpeImageDisplayToolBar(mainImageDisplay);
        toolBar.setFloatable(false);
        return toolBar;
    }

    /** Return the Tool bar with OT/TPE specific commands */
    TpeToolBar getTpeToolBar() {
        return tpeToolBar;
    }

}

