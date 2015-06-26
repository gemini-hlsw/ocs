package jsky.image.gui;

import jsky.coords.CoordinateConverter;
import jsky.coords.WorldCoordinateConverter;
import jsky.image.BasicImageReadableProcessor;
import jsky.image.ImageProcessor;

import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * This defines the common interface for classes that display an image
 * (a JAI PlanarImage).
 * <p>
 * This interface also assumes that the ImageProcessor class is used to
 * process the source image to produce the actual image to be displayed.
 * <p>
 * Any JAI PlanarImage can be displayed. Grayscale images may be displayed with
 * false colors, depending on the ImageProcessor options specified.
 *
 * @version $Revision: 7004 $
 * @author Allan Brighton
 */
public interface BasicImageDisplay extends BasicImageReadableProcessor {

    /**
     * Set the source image to display.
     */
    void setImage(PlanarImage im);

    /**
     * Return the source image (before processing).
     */
    PlanarImage getImage();

    /**
     * Return the image being displayed (after image processing).
     */
    PlanarImage getDisplayImage();

    /** Return true if this widget has been initialized and is displaying an image. */
    boolean isInitialized();

    /**
     * Update the image display after a change has been made.
     */
    void updateImage();

    /**
     * Set the image processor to use to get the image to display.
     */
    void setImageProcessor(ImageProcessor imageProcessor);

    /**
     * Return the image processor object.
     */
    ImageProcessor getImageProcessor();

    /**
     * Return the width of the source image in pixels
     */
    int getImageWidth();

    /**
     * Return the height of the source image in pixels
     */
    int getImageHeight();

    /**
     * Return the origin of the displayed image in canvas coordinates.
     */
    Point2D.Double getOrigin();

    /**
     * Set the scale (zoom factor) for the image.
     */
    void setScale(float scale);

    /**
     * Return the current scale (zoom factor) for the image.
     */
    float getScale();

    /**
     * Set the scaling factor so that the image will fit in the current window.
     * <p>
     * Note that only integer scaling factors are used, for example
     * 2, 1, 1/2, 1/3, etc., for performance reasons.
     */
    void scaleToFit();

    /**
     * Set to true if the image being displayed has been prescaled (such as
     * for a pan window or thumbnail image).
     * If true, the scale value will only be used to calculate coordinate
     * transformations, but the image will not actually be scaled.
     */
    void setPrescaled(boolean b);

    /** Return true if the image has been prescaled */
    boolean isPrescaled();


    /** Set the optional rendering hints for the image scale operation */
    void setScaleHints(RenderingHints hints);

    /** Set to true (default) to automatically center the image, if it is smaller than the window. */
    void setAutoCenterImage(boolean b);

    /**
     * Set the interpolation object used to scale the image (a subclass
     * of Interpolation, such as InterpolationNearest (default), or
     * InterpolationBilinear (better, but slower)).
     */
    void setInterpolation(Interpolation i);

    /** Return the interpolation object used to scale the image */
    Interpolation getInterpolation();

    /**
     * Return the value of the pixel in the given band at the given user coordinates
     */
    float getPixelValue(Point2D.Double p, int band);

    /**
     * Return a rectangle describing the visible area of the image
     * (in user coordinates).
     */
    Rectangle2D.Double getVisibleArea();

    /**
     * Set the origin of the image to display in canvas coordinates.
     */
    void setOrigin(Point2D.Double origin);

    /**
     * Return the image canvas component.
     */
    JComponent getCanvas();

    /**
     * Set to true if scrolling and other operations should update the image immediately,
     * otherwise only on button release.
     */
    void setImmediateMode(boolean b);

    /** Return true if immediate mode is turned on. */
    boolean isImmediateMode();

    /**
     * Return true if the current image supports world coordinates
     * (has the necessary keywords in the header).
     */
    boolean isWCS();

    /**
     * Return the object used to convert between image and world coordinates,
     * or null if none is available.
     */
    WorldCoordinateConverter getWCS();

    /**
     * Set the object used to convert between image and world coordinates.
     */
    void setWCS(WorldCoordinateConverter wcs);

    /** Return the object used to convert coordinates. */
    CoordinateConverter getCoordinateConverter();

    /**
     * Register as an image graphics handler.
     */
    void addImageGraphicsHandler(ImageGraphicsHandler igh);

}
