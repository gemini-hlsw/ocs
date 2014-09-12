/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TabbedPanelInternalFrame.java 4414 2004-02-03 16:21:36Z brighton $
 */

package jsky.util.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JInternalFrame;

import jsky.util.Preferences;


/**
 * Provides an internal frame for a TabbedPanel and some dialog buttons.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public class TabbedPanelInternalFrame extends JInternalFrame {

    private TabbedPanel _tabbedPanel;

    /**
     * Create a top level window containing a TabbedPanel.
     */
    public TabbedPanelInternalFrame(String title) {
        super(title);
        _tabbedPanel = new TabbedPanel(this);
        getContentPane().add(_tabbedPanel, BorderLayout.CENTER);
        pack();
        setClosable(true);
        setResizable(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        Preferences.manageLocation(this);
        //Preferences.manageSize(_tabbedPanel, new Dimension(400, 450));
        setVisible(true);
    }

    public TabbedPanel getTabbedPanel() {
        return _tabbedPanel;
    }
}

