/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TargetListFrame.java 4726 2004-05-14 16:50:12Z brighton $
 */

package jsky.plot;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import jsky.util.Preferences;

import java.awt.Dimension;

import jsky.util.gui.BasicWindowMonitor;


/**
 * Provides a frame with a TargetListPanel.
 *
 * @version $Revision: 4726 $
 * @author Allan Brighton
 */
public class TargetListFrame extends JFrame {

    private TargetListPanel _panel;

    /**
     * Create a top level window containing a TargetListPanel.
     */
    public TargetListFrame(String title) {
        super(title);
        _panel = new TargetListPanel(this);
        getContentPane().add(_panel, BorderLayout.CENTER);
        Preferences.manageLocation(this);
        Preferences.manageSize(_panel, new Dimension(400, 300));
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        pack();
        setVisible(true);
    }

    /**
     * Create a top level window containing a TargetListPanel with the default title.
     */
    public TargetListFrame() {
        this("Target List");
    }


    public TargetListPanel getTargetListPanel() {
        return _panel;
    }

    /**
     * test main
     */
    public static void main(String[] args) {
        TargetListFrame f = new TargetListFrame();
        f.addWindowListener(new BasicWindowMonitor());
    }
}

