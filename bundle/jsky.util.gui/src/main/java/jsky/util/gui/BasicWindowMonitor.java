package jsky.util.gui;

import java.awt.event.*;
import java.awt.Window;

/**
 * This simple utility class is used to delete the main window
 * when an application exits.
 */
public class BasicWindowMonitor extends WindowAdapter {

    public void windowClosing(final WindowEvent e) {
        final Window w = e.getWindow();
        w.setVisible(false);
        w.dispose();
        System.exit(0);
    }
}
