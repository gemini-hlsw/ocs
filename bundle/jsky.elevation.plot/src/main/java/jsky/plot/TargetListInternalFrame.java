/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TargetListInternalFrame.java 4726 2004-05-14 16:50:12Z brighton $
 */

package jsky.plot;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JInternalFrame;

import jsky.util.Preferences;


/**
 * Provides an internal frame with a TargetListPanel.
 *
 * @version $Revision: 4726 $
 * @author Allan Brighton
 */
public class TargetListInternalFrame extends JInternalFrame {

    private TargetListPanel _panel;

    /**
     * Create a top level window containing a TargetListPanel.
     */
    public TargetListInternalFrame(String title) {
        super(title);
        _panel = new TargetListPanel(this);
        getContentPane().add(_panel, BorderLayout.CENTER);
        pack();
        setClosable(true);
        setResizable(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        Preferences.manageLocation(this);
        Preferences.manageSize(_panel, new Dimension(400, 300));
        setVisible(true);
    }

    /**
     * Create a top level window containing a TargetListPanel with the default title.
     */
    public TargetListInternalFrame() {
        this("Target List");
    }

    public TargetListPanel getTargetListPanel() {
        return _panel;
    }
}

