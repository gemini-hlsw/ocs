/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: FITSKeywordsFrame.java 4414 2004-02-03 16:21:36Z brighton $
 */


package jsky.image.fits.gui;

import java.awt.BorderLayout;
import javax.swing.JFrame;

import jsky.util.gui.Resources;
import jsky.image.gui.MainImageDisplay;
import jsky.util.Preferences;


/**
 * Provides a top level window for an FITSKeywords panel.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public class FITSKeywordsFrame extends JFrame {

    private FITSKeywords fitsKeywords;


    /**
     * Create a top level window containing an FITSKeywords panel.
     */
    public FITSKeywordsFrame(MainImageDisplay imageDisplay) {
        super("FITS Keywords");
        fitsKeywords = new FITSKeywords(this, imageDisplay);
        getContentPane().add(fitsKeywords, BorderLayout.CENTER);
        Resources.setOTFrameIcon(this);
        pack();
        Preferences.manageLocation(this);
        setVisible(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    /**
     * Update the display from the current image
     */
    public void updateDisplay() {
        fitsKeywords.updateDisplay();
    }

    /** Return the internal panel object */
    public FITSKeywords getFITSKeywords() {
        return fitsKeywords;
    }
}

