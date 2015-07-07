package jsky.image;

import java.awt.geom.Rectangle2D;

/**
 * Responsible for providing read only information for image processing.
 *
 *
 * @version $Revision: 4414 $
 * @author Franklin R. Tanner
 */
public interface BasicImageReadableProcessor {

    /**
     * Return the image processor object.
     *
     * @return The Object responsible for most image transformations
     *         that processes PlanarImages.
     * @see javax.media.jai.PlanarImage
     */
    ImageProcessor getImageProcessor();

    /**
     * Return a rectangle describing the visible area of the image
     * (in user coordinates).
     *
     * @return User Coordinates of retangle.  Note, this may be a
     *         a SuperRectangle if the PlanarImage is non-rectangular.
     */
    Rectangle2D.Double getVisibleArea();
}
