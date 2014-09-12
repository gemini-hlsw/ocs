/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SessionQueueInternalFrame.java 7892 2007-06-04 20:48:36Z gillies $
 */

package jsky.app.ot.session;

import jsky.util.Preferences;

import javax.swing.*;
import java.awt.*;


/**
 * Provides a top level window for a SessionQueue panel.
 *
 * @version $Revision: 7892 $
 * @author Allan Brighton
 */
public class SessionQueueInternalFrame extends JInternalFrame {

    // The GUI panel
    private SessionQueuePanel sessionQueuePanel;

    /**
     * Create a top level window containing a SessionQueuePanel.
     */
    public SessionQueueInternalFrame() {
        super("Session Queue", true, true, false, false);
        sessionQueuePanel = new SessionQueuePanel();
        getContentPane().add(sessionQueuePanel, BorderLayout.CENTER);
        pack();
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        Preferences.manageLocation(this);

        // Unfortunately this seems to be ignored and the size is governed by
        // the preferredScrollableViewportSize of the contained table.
        Preferences.manageSize(sessionQueuePanel, new Dimension(325, 350));

        setVisible(true);
    }


    /** Return the internal panel object */
    public SessionQueuePanel getSessionQueuePanel() {
        return sessionQueuePanel;
    }
}

