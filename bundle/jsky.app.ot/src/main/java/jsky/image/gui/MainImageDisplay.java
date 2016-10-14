package jsky.image.gui;

import java.awt.Graphics2D;
import java.io.IOException;
import java.net.URL;
import javax.swing.event.ChangeListener;

import jsky.coords.WorldCoords;
import jsky.util.gui.GenericToolBarTarget;
import nom.tam.fits.FitsException;

/**
 * This defines the interface for a main application image display window.
 *
 * @version $Revision: 47126 $
 * @author Allan Brighton
 */
public interface MainImageDisplay
        extends GraphicsImageDisplay, GenericToolBarTarget {

    /** Set the image file to display. */
    void setFilename(String fileOrUrl, boolean displayError);

    /**
     * Set the image file to display, and indicate that the file was downloaded from
     * the given URL (for image history recording).
     */
    void setFilename(String fileOrUrl, URL url);

    /** Return the image file name, if there is one. */
    String getFilename();

    /** Set the URL for the image to display. */
    void setURL(URL theURL);

    /** Return the image URL, if there is one, otherwise null. */
    URL getURL();

    /**
     * Update the display to show the contents of the currently loaded image file.
     */
    void updateImageData();

    /** Display the FITS table at the given HDU index (if supported). */
    void displayFITSTable(int hdu);

    /** Return the name of the object being displayed, if known, otherwise null. */
    String getObjectName();

    /** Paint the image and graphics to the given graphics object (for save and print features) */
    void paintImageAndGraphics(Graphics2D g2D);

    /**
     * register to receive change events from this object whenever the
     * image or cut levels are changed.
     */
    void addChangeListener(ChangeListener l);

    /**
     * Stop receiving change events from this object.
     */
    void removeChangeListener(ChangeListener l);

    /** Set to true if the image has been modified and needs saving. */
    void setSaveNeeded(boolean b);

    /**
     * Pop up a dialog to ask the user for a file name, and then save the image
     * to the selected file.
     */
    void saveAs();

    /**
     * Save the current image to the given file, using an image format
     * based on the file suffix, which should be one of ".fits", ".jpg",
     * ".png", or ".tif".
     */
    void saveAs(String filename);

    /**
     * Pop up a dialog for printing the image.
     */
    void print();

    /**
     * Return the base or center position in world coordinates.
     * If there is no base position, this method returns the center point
     * of the image. If the image does not support WCS, this method returns (0,0).
     * The position returned here should be used as the base position
     * for any catalog or image server requests.
     */
    WorldCoords getBasePos();

}
