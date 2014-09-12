/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TpeImageDisplayInternalFrame.java 21408 2009-08-05 22:17:07Z swalker $
 */

package jsky.app.ot.tpe;

import jsky.image.gui.DivaMainImageDisplay;
import jsky.image.gui.ImageDisplayControl;
import jsky.image.gui.ImageDisplayMenuBar;
import jsky.image.gui.ImageDisplayToolBar;
import jsky.navigator.NavigatorImageDisplayInternalFrame;
import jsky.navigator.NavigatorImageDisplayToolBar;

import javax.swing.*;
import java.awt.*;


/**
 * Extends NavigatorImageDisplayInternalFrame to add OT/TPE specific features.
 *
 * @version $Revision: 21408 $
 * @author Allan Brighton
 */
public class TpeImageDisplayInternalFrame extends NavigatorImageDisplayInternalFrame {

    /** Tool bar with OT/TPE specific commands */
    TpeToolBar tpeToolBar;


    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param size   the size (width, height) to use for the pan and zoom windows.
     */
    public TpeImageDisplayInternalFrame(JDesktopPane desktop, int size) {
        super(desktop, size);
    }

    /**
     * Create a top level window containing an ImageDisplayControl panel
     * with the default settings.
     */
    public TpeImageDisplayInternalFrame(JDesktopPane desktop) {
        super(desktop);
    }


    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param desktop The JDesktopPane to add the frame to.
     * @param size   the size (width, height) to use for the pan and zoom windows.
     * @param fileOrUrl The file name or URL of an image to display.
     */
    public TpeImageDisplayInternalFrame(JDesktopPane desktop, int size, String fileOrUrl) {
        super(desktop, size, fileOrUrl);
    }

    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param desktop The JDesktopPane to add the frame to.
     * @param fileOrUrl The file name or URL of an image to display.
     */
    public TpeImageDisplayInternalFrame(JDesktopPane desktop, String fileOrUrl) {
        super(desktop, fileOrUrl);
    }

    /** Make and return the menubar */
    protected ImageDisplayMenuBar makeMenuBar(DivaMainImageDisplay mainImageDisplay, ImageDisplayToolBar toolBar) {
        return new TpeImageDisplayMenuBar((TpeImageWidget) mainImageDisplay, (NavigatorImageDisplayToolBar) toolBar);
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
        ImageDisplayToolBar toolBar = super.makeToolBar(mainImageDisplay);
        toolBar.setFloatable(false);
        return toolBar;
    }


    /** Add a tool bar for OT/TPE specific operations. */
    protected void addTpeToolBar() {
        tpeToolBar = new TpeToolBar();
        getContentPane().add(tpeToolBar, BorderLayout.WEST);
    }

    /** Return the Tool bar with OT/TPE specific commands */
    TpeToolBar getTpeToolBar() {
        return tpeToolBar;
    }
}

