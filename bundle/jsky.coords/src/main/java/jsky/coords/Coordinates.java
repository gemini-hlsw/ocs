package jsky.coords;

/**
 * Abstract interface for coordinates. This interface defines
 * the common methods for all image coordinate systems used.
 */
public interface Coordinates {

    /** Returns the name of the coordinate system as a string. */
    String getCoordinateSystemName();

    /**
     * Return the coordinates as a string.
     */
    String toString();

    /**
     * Return the distance between this position and the given one in
     * the standard units of the coordinate system being used (arcmin
     * for WorldCoords, pixels for ImageCoords, ...).
     *
     * @param pos The other point.
     *
     * @return The distance to the given point.
     */
    double dist(Coordinates pos);

    /** return the X coordinate as a double */
    double getX();

    /** return the Y coordinate as a double */
    double getY();
}
