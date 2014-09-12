/*
 * ESO Archive
 *
 * $Id: ImagePropertiesFrame.java 4414 2004-02-03 16:21:36Z brighton $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.gui;


import java.awt.*;
import javax.swing.*;

import jsky.util.Preferences;


/**
 * Provides a top level window for an ImageProperties panel.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public class ImagePropertiesFrame extends JFrame {

    private ImageProperties imageProperties;


    /**
     * Create a top level window containing an ImageProperties panel.
     */
    public ImagePropertiesFrame(MainImageDisplay imageDisplay) {
        super("Image Properties");
        imageProperties = new ImageProperties(this, imageDisplay);
        getContentPane().add(imageProperties, BorderLayout.CENTER);
        pack();
        Preferences.manageLocation(this);
        setVisible(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    /**
     * Update the display from the current image
     */
    public void updateDisplay() {
        imageProperties.updateDisplay();
    }

    /** Return the internal panel object */
    public ImageProperties getImageProperties() {
        return imageProperties;
    }
}

