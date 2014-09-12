package jsky.app.ot.viewer.action;

import jsky.app.ot.viewer.SPViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Close the current program.
 */
public final class CloseAction extends AbstractViewerAction {
    public CloseAction(SPViewer viewer) {
        super(viewer, "Close Program");
        putValue(SHORT_NAME, "Close Program");
        putValue(SHORT_DESCRIPTION, "Close the current program.");

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W, AbstractViewerAction.platformEventMask()));
        setEnabled(true);
    }

    public boolean computeEnabledState() throws Exception {
        return viewer.getProgram() != null;
    }

    public void actionPerformed(ActionEvent e) {
        viewer.closeProgram();
    }
}
