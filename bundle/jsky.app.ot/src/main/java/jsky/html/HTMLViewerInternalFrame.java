/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: HTMLViewerInternalFrame.java 4414 2004-02-03 16:21:36Z brighton $
 */

package jsky.html;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JInternalFrame;

import jsky.util.Preferences;
import jsky.util.gui.GenericToolBar;

/**
 * Provides a top level window for an HTMLViewer panel.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public class HTMLViewerInternalFrame extends JInternalFrame {

    private HTMLViewer viewer;


    /**
     * Create a top level window containing an HTMLViewer panel.
     */
    public HTMLViewerInternalFrame() {
        super("HTML Viewer", true, false, true, true);
        viewer = new HTMLViewer(this);
        GenericToolBar toolbar = new GenericToolBar(viewer);
        getContentPane().add(toolbar, BorderLayout.NORTH);
        getContentPane().add(viewer, BorderLayout.CENTER);
        setJMenuBar(new HTMLViewerMenuBar(viewer, toolbar));

        Preferences.manageLocation(this);
        Preferences.manageSize(viewer, new Dimension(600, 500));

        pack();
        setClosable(true);
        setIconifiable(false);
        setMaximizable(false);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setVisible(true);
    }

    /** Return the internal panel object */
    public HTMLViewer getHTMLViewer() {
        return viewer;
    }
}

