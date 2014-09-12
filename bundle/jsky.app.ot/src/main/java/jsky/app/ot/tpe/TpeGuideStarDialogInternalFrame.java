/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TpeGuideStarDialogInternalFrame.java 4725 2004-05-14 16:32:54Z brighton $
 */

package jsky.app.ot.tpe;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JInternalFrame;

import jsky.util.Preferences;
import jsky.app.ot.tpe.TpeGuideStarDialog;


/**
 * Provides an internal frame for a TpeGuideStarDialog.
 */
class TpeGuideStarDialogInternalFrame extends JInternalFrame {

    /**
     * Create a top level window containing a TpeGuideStarDialog.
     */
    public TpeGuideStarDialogInternalFrame(TpeGuideStarDialog dialog) {
        super("Guide Star Selection");
        getContentPane().add(dialog, BorderLayout.CENTER);
        pack();
        setClosable(true);
        setResizable(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        Preferences.manageLocation(this);
        Preferences.manageSize(dialog, new Dimension(400, 172));
        setVisible(true);
    }
}

