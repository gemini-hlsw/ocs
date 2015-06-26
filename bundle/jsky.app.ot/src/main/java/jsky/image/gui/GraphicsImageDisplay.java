package jsky.image.gui;

import java.awt.Rectangle;
import java.awt.geom.Point2D;

import jsky.graphics.CanvasGraphics;
import jsky.image.fits.codec.FITSImage;

/**
 * This defines the interface for a widget that can display images and
 * draw graphics overlays.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public interface GraphicsImageDisplay extends BasicImageDisplay {

    /** Return an object to be used for drawing persistent graphics over the image */
    CanvasGraphics getCanvasGraphics();

    /**
     * Clear the image display, freeing up any resources currently used by the image
     * so that they can be garbage collected.
     */
    void clear();

    /**
     * Return true if the image has been cleared.
     */
    boolean isClear();

    /**
     * Display a blank image with the given center coordinates.
     *
     * @param ra  RA center coordinate in deg J2000
     * @param dec Dec center coordinate in deg J2000
     */
    void blankImage(double ra, double dec);

    /**
     * If the current image is in FITS format, return the FITSImage object managing it,
     * otherwise return null. (The FITSImage object is available via the "#fits_image"
     * property from the FITS codec, which implements FITS support for JAI.)
     */
    FITSImage getFitsImage();

    /**
     * Return the value of the pixel in the given band at the given user coordinates
     */
    float getPixelValue(Point2D.Double p, int band);

    /**
     * Return an array containing the values of the pixels in the given band in the given
     * user coordinates region. The pixel values are converted to float. Any pixels outside
     * the image bounds are set to the mean image value, if known, otherwise 0.
     *
     * @param region describes the region of the image to get in user coordinates
     * @param band the band of the image to get
     */
    float[] getPixelValues(Rectangle region, int band);
}
