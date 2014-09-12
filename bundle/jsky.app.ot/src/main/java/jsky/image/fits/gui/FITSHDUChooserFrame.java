/*
 * ESO Archive
 *
 * $Id: FITSHDUChooserFrame.java 6769 2005-11-28 22:53:55Z brighton $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.fits.gui;

import java.awt.BorderLayout;
import javax.swing.JFrame;

import jsky.image.fits.codec.FITSImage;
import jsky.image.gui.MainImageDisplay;
import jsky.util.Preferences;


/**
 * Provides a top level window for an FITSHDUChooser panel.
 *
 * @version $Revision: 6769 $
 * @author Allan Brighton
 */
public class FITSHDUChooserFrame extends JFrame {

    private FITSHDUChooser fitsHDUChooser;


    /**
     * Create a top level window containing an FITSHDUChooser panel.
     */
    public FITSHDUChooserFrame(MainImageDisplay imageDisplay, FITSImage fitsImage) {
        super("Image Extensions");
        fitsHDUChooser = new FITSHDUChooser(this, imageDisplay, fitsImage);
        getContentPane().add(fitsHDUChooser, BorderLayout.CENTER);
        pack();
        Preferences.manageLocation(this);
        setVisible(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    /** Return the internal panel object */
    public FITSHDUChooser getFitsHDUChooser() {
        return fitsHDUChooser;
    }
}

