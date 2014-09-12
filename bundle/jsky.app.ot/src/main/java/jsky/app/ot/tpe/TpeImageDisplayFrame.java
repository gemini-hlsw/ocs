/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TpeImageDisplayFrame.java 21408 2009-08-05 22:17:07Z swalker $
 */

package jsky.app.ot.tpe;

import jsky.image.gui.DivaMainImageDisplay;
import jsky.image.gui.ImageDisplayControl;
import jsky.image.gui.ImageDisplayMenuBar;
import jsky.image.gui.ImageDisplayToolBar;
import jsky.navigator.NavigatorImageDisplayFrame;
import jsky.navigator.NavigatorImageDisplayToolBar;

import java.awt.*;

/**
 * Extends NavigatorImageDisplayFrame to add OT/TPE specific features.
 *
 * @version $Revision: 21408 $
 * @author Allan Brighton
 */
public class TpeImageDisplayFrame extends NavigatorImageDisplayFrame {

    /** Tool bar with Tpe specific commands */
    TpeToolBar tpeToolBar;


    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param size   the size (width, height) to use for the pan and zoom windows.
     */
    public TpeImageDisplayFrame(int size) {
        super(size);
    }

    /**
     * Create a top level window containing an ImageDisplayControl panel
     * with the default settings.
     */
    public TpeImageDisplayFrame() {
        super();
    }


    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param size   the size (width, height) to use for the pan and zoom windows.
     * @param fileOrUrl The file name or URL of an image to display.
     */
    public TpeImageDisplayFrame(int size, String fileOrUrl) {
        super(size, fileOrUrl);
    }

    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param fileOrUrl The file name or URL of an image to display.
     */
    public TpeImageDisplayFrame(String fileOrUrl) {
        super(fileOrUrl);
    }

    /** Make and return the menubar */
    protected ImageDisplayMenuBar makeMenuBar(DivaMainImageDisplay mainImageDisplay, ImageDisplayToolBar toolBar) {
        return new TpeImageDisplayMenuBar((TpeImageWidget) mainImageDisplay,
                                          (NavigatorImageDisplayToolBar) toolBar);
    }

    /**
     * Make and return the image display control frame.
     *
     * @param size the size (width, height) to use for the pan and zoom windows.
     */
    protected ImageDisplayControl makeImageDisplayControl(int size) {
        return new TpeImageDisplayControl(this, size);
    }

    /** Make and return the toolbar */
    protected ImageDisplayToolBar makeToolBar(DivaMainImageDisplay mainImageDisplay) {
        // add the Tpe tool bar while we are at it...
        addTpeToolBar();

        // Dragging can cause problems with two tool bars...
        ImageDisplayToolBar toolBar =
                new TpeImageDisplayToolBar((TpeImageWidget)mainImageDisplay);
        toolBar.setFloatable(false);
        return toolBar;
    }

    /** Add a tool bar for OT/TPE specific operations. */
    protected void addTpeToolBar() {
//        TpeImageWidget imageDisplay = (TpeImageWidget) imageDisplayControl.getImageDisplay();
        tpeToolBar = new TpeToolBar();
        getContentPane().add(tpeToolBar, BorderLayout.WEST);
    }

    /** Return the Tool bar with OT/TPE specific commands */
    TpeToolBar getTpeToolBar() {
        return tpeToolBar;
    }

}

