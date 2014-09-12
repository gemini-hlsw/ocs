/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: MainImageDisplay.java 47126 2012-08-01 15:40:43Z swalker $
 */

package jsky.image.gui;

import java.awt.Component;
import java.awt.Graphics2D;
import java.net.URL;
import javax.swing.event.ChangeListener;

import edu.gemini.catalog.api.MagnitudeLimits;
import edu.gemini.catalog.api.RadiusLimits;
import jsky.coords.WorldCoords;
import jsky.util.gui.GenericToolBarTarget;

/**
 * This defines the interface for a main application image display window.
 *
 * @version $Revision: 47126 $
 * @author Allan Brighton
 */
public abstract interface MainImageDisplay
        extends GraphicsImageDisplay, GenericToolBarTarget {

    /** Set the image file to display. */
    public void setFilename(String fileOrUrl);

    /**
     * Set the image file to display, and indicate that the file was downloaded from
     * the given URL (for image history recording).
     */
    public void setFilename(String fileOrUrl, URL url);

    /** Return the image file name, if there is one. */
    public String getFilename();

    /** Set the URL for the image to display. */
    public void setURL(URL theURL);

    /** Return the image URL, if there is one, otherwise null. */
    public URL getURL();

    /**
     * Update the display to show the contents of the currently loaded image file.
     */
    public void updateImageData();

    /** Display the FITS table at the given HDU index (if supported). */
    public void displayFITSTable(int hdu);

    /** Return the name of the object being displayed, if known, otherwise null. */
    public String getObjectName();

    /** Paint the image and graphics to the given graphics object (for save and print features) */
    public void paintImageAndGraphics(Graphics2D g2D);

    /**
     * register to receive change events from this object whenever the
     * image or cut levels are changed.
     */
    public void addChangeListener(ChangeListener l);

    /**
     * Stop receiving change events from this object.
     */
    public void removeChangeListener(ChangeListener l);

    /**
     * Return the top level parent frame (or internal frame)
     * (Used to open and close the window displaying image).
     */
    public Component getRootComponent();

    /** Set to true if the image has been modified and needs saving. */
    public void setSaveNeeded(boolean b);

    /**
     * Pop up a dialog to ask the user for a file name, and then save the image
     * to the selected file.
     */
    public void saveAs();

    /**
     * Save the current image to the given file, using an image format
     * based on the file suffix, which should be one of ".fits", ".jpg",
     * ".png", or ".tif".
     */
    public void saveAs(String filename);

    /**
     * Pop up a dialog for printing the image.
     */
    public void print();

    /**
     * Return the base or center position in world coordinates.
     * If there is no base position, this method returns the center point
     * of the image. If the image does not support WCS, this method returns (0,0).
     * The position returned here should be used as the base position
     * for any catalog or image server requests.
     */
    public WorldCoords getBasePos();

    /**
     * Return the default min and max search radius values to use for catalog searches, in arcmin.
     *
     * @param centerPos the center position for the radius
     * @param useImageSize if true, use the image size to get the search radius
     * @return radius values
     */
    RadiusLimits getDefaultSearchRadius(WorldCoords centerPos, boolean useImageSize);

    /**
     * Return the default min and max magnitude values to use for catalog searches, or null
     * if there is no default.
     *
     * @return magnitude limits, including band
     */
    MagnitudeLimits getDefaultSearchMagRange();




}
