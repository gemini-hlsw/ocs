package jsky.util.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;

import javax.swing.JDialog;


/**
 * Provides a top level window for an ProgressPanel panel.
 */
public class ProgressPanelDialog extends JDialog {

    private final ProgressPanel progressPanel;


    /**
     * Create a top level window containing an ProgressPanel panel.
     *
     * @param parent the parent frame (may be null)
     * @param title the title to display in the dialog
     */
    public ProgressPanelDialog(final String title, final Frame parent) {
        super(parent, "Progress");

        // center dialog in screen
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width / 2 - 150, screenSize.height / 2 - 100);

        progressPanel = new ProgressPanel(this, title);
        getContentPane().add(progressPanel, BorderLayout.CENTER);
        pack();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
    }


    /** Return the internal panel object */
    public ProgressPanel getProgressPanel() {
        return progressPanel;
    }
}


