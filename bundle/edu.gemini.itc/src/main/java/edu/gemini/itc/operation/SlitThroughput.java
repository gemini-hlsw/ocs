package edu.gemini.itc.operation;

import edu.gemini.itc.base.DatFile;
import edu.gemini.itc.base.ITCConstants;
import edu.gemini.itc.shared.*;
import java.util.logging.Logger;

import java.util.Scanner;

public final class SlitThroughput {

    private static final double[] x_axis;
    private static final double[] y_axis;
    private static final double[][] _data;
    private static final Logger Log = Logger.getLogger(SlitThroughput.class.getName());
    static {
        final String file = ITCConstants.CALC_LIB + ITCConstants.SLIT_THROUGHPUT_FILENAME + ITCConstants.DATA_SUFFIX;
        try (final Scanner scan = DatFile.scanFile(file)) {
            // read x and y dimensions
            final int xD = scan.nextInt();
            final int yD = scan.nextInt();

            // read x- and y-axes values
            x_axis = new double[xD];
            for (int x = 0; x < xD; x++) {
                x_axis[x] = scan.nextDouble();
            }
            y_axis = new double[yD];
            for (int y = 0; y < yD; y++) {
                y_axis[y] = scan.nextDouble();
            }

            // read array of data values
            _data = new double[yD][xD];
            for (int y = 0; y < yD; y++) {
                for (int x = 0; x < xD; x++) {
                    _data[y][x] = scan.nextDouble();
                }
            }
        }
    }

    private final double throughput;
    private final double onePixelThroughput;

    public SlitThroughput(final double throughput, final double onePixelThroughput) {
        this.throughput = throughput;
        this.onePixelThroughput = onePixelThroughput;
    }


    public SlitThroughput(final SourceDefinition src, final Slit slit, final double im_qual) {
        this.throughput             = calculateThroughput(src, slit, im_qual);
        this.onePixelThroughput     = calculateThroughput(src, new OnePixelSlit(slit.width(), slit.pixelSize()), im_qual);
    }

    public double throughput() {
        return throughput;
    }

    public double onePixelThroughput() {
        return onePixelThroughput;
    }

    // For point sources and gaussian sources: returns the fraction of the source flux that goes through the slit.
    // For uniform surface brightness: either return 1 arcsec² for auto aperture or the slit area.
    private double calculateThroughput(final SourceDefinition src, final Slit slit, final double im_qual) {

        // For the usb case we want the resolution to be determined by the slit area and not the image quality.
        if (src.isUniform()) {

            // return the aperture area in arcsec²
            return slit.area();

        // Non-USB case (point source/gaussian)
        } else {

            // find the slit length in the aperture
            double slit_spatial_ratio = slit.lengthPixels() * slit.pixelSize() / slit.width();

            // find the slit width
            final double sigma = im_qual / 2.355;
            final double slit_spec_ratio = slit.width() / sigma;
            Log.fine("Slit width / Image quality (sigma) = " + slit_spec_ratio);

            // deal with values that are outside the range of the x- and y-axes
            // defined in slit_throughput.dat
            if (slit_spatial_ratio > 5.5) {
                slit_spatial_ratio = 5.499; //Slide in under the max
            }
            if (slit_spec_ratio > 8) {
                return 1;
            }

            // Do a 2D interpolation to find the return value using x= slit_spatial_ratio and y= slit_spec_ratio
            final double slitThroughput = getSTvalue(slit_spatial_ratio, slit_spec_ratio);
            Log.fine("slitThroughput (" + slit.width() + " x " + slit.lengthPixels() * slit.pixelSize() + ") = " + slitThroughput);

            // return a value in the range 0..1
            return Math.min(1, slitThroughput);

        }

    }

    /**
     * @return y value at specified x using linear interpolation.
     * Silently returns zero if x is out of spectrum range.
     */
    private double getSTvalue(final double x, final double y) {
        if (x < getStartX() || y < getStartY()) {
            return 0;
        }
        if (y > getEndY()) return 1;

        final int low_index_x = getLowerXIndex(x);
        final int low_index_y = getLowerYIndex(y);

        final int high_index_x;
        final int high_index_y;
        if (low_index_y == getYAxisSize() - 1) {
            return 1.0;
        } else if (low_index_x == getXAxisSize() - 1) {
            return getValue(low_index_x, low_index_y) * (y / getYAxisValue(low_index_y));
        } else {
            high_index_x = low_index_x + 1;
            high_index_y = low_index_y + 1;
        }

        final double y1 = getValue(low_index_x, low_index_y);
        final double y2 = getValue(high_index_x, low_index_y);
        final double y3 = getValue(high_index_x, high_index_y);
        final double y4 = getValue(low_index_x, high_index_y);

        final double t = (x - getXAxisValue(low_index_x)) / (getXAxisValue(high_index_x) - getXAxisValue(low_index_x));
        final double u = (y - getYAxisValue(low_index_y)) / (getYAxisValue(high_index_y) - getYAxisValue(low_index_y));

        return ((1.0 - t) * (1.0 - u) * y1 + t * (1.0 - u) * y2 + t * u * y3 + (1.0 - t) * u * y4);
    }

    /**
     * @return starting x value
     */
    private double getStartX() {
        return x_axis[0];
    }

    private double getXAxisValue(final int index) {
        return x_axis[index];
    }

    private double getYAxisValue(final int index) {
        return y_axis[index];
    }

    private int getXAxisSize() {
        return x_axis.length;
    }

    private int getYAxisSize() {
        return y_axis.length;
    }

    /**
     * @return starting y value
     */
    private double getStartY() {
        return y_axis[0];
    }

    /**
     * @return ending y value
     */
    private double getEndY() {
        return y_axis[y_axis.length - 1];
    }

    /**
     * Returns x value of specified data point.
     */
    private double getValue(final int index_x, final int index_y) {
        return _data[index_y][index_x];
    }

    /**
     * Returns the index of the data point with largest x value less than x
     */
    private int getLowerXIndex(final double x) {
        // x value is in.  The only solution is to search for it.
        // Small amt of data so just walk through.
        int low_index = 0;
        for (int i = 0; i < x_axis.length; ++i) {
            if (x >= x_axis[i])
                low_index = i;
        }
        return low_index;
    }

    private int getLowerYIndex(double x) {
        // x value is in.  The only solution is to search for it.
        // Small amt of data so just walk through.
        int low_index = 0;
        for (int i = 0; i < y_axis.length; ++i) {
            if (x >= y_axis[i])
                low_index = i;
        }
        return low_index;
    }


}
