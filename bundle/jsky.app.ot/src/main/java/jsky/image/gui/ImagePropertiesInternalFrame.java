/*
 * ESO Archive
 *
 * $Id: ImagePropertiesInternalFrame.java 4414 2004-02-03 16:21:36Z brighton $
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
public class ImagePropertiesInternalFrame extends JInternalFrame {

    private ImageProperties imageProperties;


    /**
     * Create a top level window containing an ImageProperties panel.
     */
    public ImagePropertiesInternalFrame(MainImageDisplay imageDisplay) {
        super("Image Properties", true, false, true, true);
        imageProperties = new ImageProperties(this, imageDisplay);
        getContentPane().add(imageProperties, BorderLayout.CENTER);
        pack();
        setClosable(true);
        setIconifiable(false);
        setMaximizable(false);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        Preferences.manageSize(imageProperties, new Dimension(400, 400));
        Preferences.manageLocation(this);
        setVisible(true);
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

