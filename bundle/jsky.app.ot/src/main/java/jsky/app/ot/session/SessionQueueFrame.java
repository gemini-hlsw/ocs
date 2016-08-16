/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SessionQueueFrame.java 7892 2007-06-04 20:48:36Z gillies $
 */

package jsky.app.ot.session;

import jsky.util.gui.Resources;
import jsky.util.Preferences;

import javax.swing.*;
import java.awt.*;


/**
 * Provides a top level window for an SessionQueue panel.
 *
 * @version $Revision: 7892 $
 * @author Allan Brighton
 */
public class SessionQueueFrame extends JFrame {

    // The GUI panel
    private SessionQueuePanel sessionQueuePanel;

    /**
     * Create a top level window containing an SessionQueuePanel.
     */
    public SessionQueueFrame() {
        super("Session Queue");
        sessionQueuePanel = new SessionQueuePanel();
        getContentPane().add(sessionQueuePanel, BorderLayout.CENTER);
        Resources.setOTFrameIcon(this);
        pack();
        Preferences.manageLocation(this);

        // Unfortunately this seems to be ignored and the size is governed by
        // the preferredScrollableViewportSize of the contained table.
        Preferences.manageSize(sessionQueuePanel, new Dimension(600, 300));
        setVisible(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    /** Return the internal panel object */
    public SessionQueuePanel getSessionQueuePanel() {
        return sessionQueuePanel;
    }
}

