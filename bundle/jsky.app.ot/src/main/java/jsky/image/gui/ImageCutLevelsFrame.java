/*
 * ESO Archive
 *
 * $Id: ImageCutLevelsFrame.java 4414 2004-02-03 16:21:36Z brighton $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;

import jsky.util.I18N;
import jsky.util.Preferences;


/**
 * Provides a top level window for an ImageCutLevels panel.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public class ImageCutLevelsFrame extends JFrame {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(ImageCutLevelsFrame.class);

    // The GUI panel
    private ImageCutLevels imageCutLevels;


    /**
     * Create a top level window containing an ImageCutLevels panel.
     */
    public ImageCutLevelsFrame(BasicImageDisplay imageDisplay) {
        super(_I18N.getString("imageCutLevels"));
        imageCutLevels = new ImageCutLevels(this, imageDisplay);
        getContentPane().add(imageCutLevels, BorderLayout.CENTER);
        pack();
        Preferences.manageLocation(this);
        Preferences.manageSize(imageCutLevels, new Dimension(300, 350));
        setVisible(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    /**
     * Update the display from the current image
     */
    public void updateDisplay() {
        imageCutLevels.updateDisplay();
    }
}

