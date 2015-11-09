package jsky.coords;

import java.awt.geom.Point2D;

import javax.swing.event.ChangeListener;

/**
 * This defines the interface for converting between image and world coordinates.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public interface WorldCoordinateConverter {

    /**
     * Return true if world coordinates conversion is available. This method
     * should be called to check before calling any of the world coordinates
     * conversion methods.
     */
    boolean isWCS();

    /** Return the equinox used for coordinates (usually the equionx of the image) */
    double getEquinox();

    /**
     * Convert the given image coordinates to world coordinates degrees in the equinox
     * of the current image.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    void imageToWorldCoords(Point2D.Double p, boolean isDistance);

    /**
     * Convert the given world coordinates (degrees, in the equinox of the current image)
     * to image coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    void worldToImageCoords(Point2D.Double p, boolean isDistance);

    /** Return the center RA,Dec coordinates in degrees. */
    Point2D.Double getWCSCenter();

    /** Set the center RA,Dec coordinates in degrees. */
    //void setWCSCenter(Point2D.Double p);

    /** return the width in deg */
    double getWidthInDeg();

    /** return the height in deg */
    double getHeightInDeg();

    /** Return the image center coordinates in pixels (image coordinates). */
    Point2D.Double getImageCenter();

    /** Return the image width in pixels. */
    double getWidth();

    /** Return the image height in pixels. */
    double getHeight();
}

