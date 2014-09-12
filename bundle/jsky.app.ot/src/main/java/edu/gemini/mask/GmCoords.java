/**
 * $Id: GmCoords.java 6526 2005-08-03 21:27:13Z brighton $
 */

package edu.gemini.mask;

/**
 * This class manages the GMOS FoV values.
 */
class GmCoords {
    // gmmps read these values from the fov.dat file
    static final double XSTART = 67.4656;
    static final double YSTART = 8.9421;
    static final double XEND = 384.6557;
    static final double YEND = 326.1322;
    static final double XCORNER1 = 13.813;
    static final double XCORNER2 = 13.813;
    static final double YCORNER1 = 13.813;
    static final double YCORNER2 = 13.813;
    static final double YDELTA = 1.1632;
    static final double XDIM = 452.0486;
    static final double YDIM = 335.0016;

    private double _start;		// Starting coordinate
    private double _end;		// End coordinate.
    private double _corner1;    // First corner
    private double _corner2;	// Second corner
    private double _delta;		// Delta, slope scaling factor
    private double _dim;		// Photometric image x/y dimension

    public GmCoords(double pixelScale, double start, double end, double corner1,
                    double corner2, double delta, double dim) {
        _start = start / pixelScale;
        _end = end / pixelScale;
        _corner1 = corner1 / pixelScale;
        _corner2 = corner2 / pixelScale;
        _delta = delta / pixelScale;
        _dim = dim / pixelScale;
    }

    public static GmCoords getX(double pixelScale) {
        return new GmCoords(pixelScale, XSTART, XEND, XCORNER1, XCORNER2, 0, XDIM);
    }

    public static GmCoords getY(double pixelScale) {
        return new GmCoords(pixelScale, YSTART, YEND, YCORNER1, YCORNER2, YDELTA, YDIM);
    }

    public double getStart() {
        return _start;
    }

    public double getEnd() {
        return _end;
    }

    public double getCorner1() {
        return _corner1;
    }

    public double getCorner2() {
        return _corner2;
    }

    public double getDelta() {
        return _delta;
    }

    public double getDim() {
        return _dim;
    }
}
