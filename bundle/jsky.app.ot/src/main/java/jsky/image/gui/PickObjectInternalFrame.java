/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: PickObjectInternalFrame.java 4414 2004-02-03 16:21:36Z brighton $
 */

package jsky.image.gui;

import java.awt.BorderLayout;
import javax.swing.JInternalFrame;

import jsky.util.Preferences;


/**
 * Provides a top level window for a PickObject panel.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public class PickObjectInternalFrame extends JInternalFrame {

    /** The main panel */
    private PickObject pickObject;

    /**
     * Create a top level window containing an PickObject panel.
     */
    public PickObjectInternalFrame(MainImageDisplay imageDisplay) {
        super("Pick Object");
        pickObject = new PickObject(this, imageDisplay);
        getContentPane().add(pickObject, BorderLayout.CENTER);
        pack();
        setClosable(true);
        setIconifiable(false);
        setMaximizable(false);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        Preferences.manageLocation(this);
        setVisible(true);
    }

    /** Return the internal panel object */
    public PickObject getPickObject() {
        return pickObject;
    }
}

