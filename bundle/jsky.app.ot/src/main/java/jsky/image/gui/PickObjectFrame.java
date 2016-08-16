package jsky.image.gui;

import java.awt.BorderLayout;
import javax.swing.JFrame;

import jsky.util.gui.Resources;
import jsky.util.Preferences;

/**
 * Provides a top level window for a PickObject panel.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public class PickObjectFrame extends JFrame {

    /** The main panel */
    private PickObject pickObject;

    /**
     * Create a top level window containing an PickObject panel.
     */
    public PickObjectFrame(MainImageDisplay imageDisplay) {
        super("Pick Objects");
        pickObject = new PickObject(this, imageDisplay);
        getContentPane().add(pickObject, BorderLayout.CENTER);
        Resources.setOTFrameIcon(this);
        pack();
        Preferences.manageLocation(this);
        setVisible(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    /** Return the internal panel object */
    public PickObject getPickObject() {
        return pickObject;
    }
}

