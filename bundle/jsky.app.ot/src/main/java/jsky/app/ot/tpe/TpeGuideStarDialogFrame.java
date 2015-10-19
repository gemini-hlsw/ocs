package jsky.app.ot.tpe;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;

import jsky.util.Preferences;

/**
 * Provides a frame containing a TpeGuideStarDialog.
 */
@Deprecated
class TpeGuideStarDialogFrame extends JFrame {

    /**
     * Create a top level window containing a TabbedPanel.
     */
    public TpeGuideStarDialogFrame(TpeGuideStarDialog dialog) {
        super("Guide Star Selection");
        getContentPane().add(dialog, BorderLayout.CENTER);
        pack();
        Preferences.manageLocation(this);
        Preferences.manageSize(dialog, new Dimension(400, 172));
        setVisible(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }
}

