package jsky.util.gui;

import java.awt.BorderLayout;

import javax.swing.JInternalFrame;

import jsky.util.Preferences;


/**
 * Provides an internal frame for a TabbedPanel and some dialog buttons.
 */
public class TabbedPanelInternalFrame extends JInternalFrame {

    private final TabbedPanel _tabbedPanel;

    /**
     * Create a top level window containing a TabbedPanel.
     */
    public TabbedPanelInternalFrame(final String title) {
        super(title);
        _tabbedPanel = new TabbedPanel(this);
        getContentPane().add(_tabbedPanel, BorderLayout.CENTER);
        pack();
        setClosable(true);
        setResizable(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        Preferences.manageLocation(this);
        setVisible(true);
    }

    public TabbedPanel getTabbedPanel() {
        return _tabbedPanel;
    }
}

