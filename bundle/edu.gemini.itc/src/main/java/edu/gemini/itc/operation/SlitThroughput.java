package edu.gemini.itc.operation;

import edu.gemini.itc.base.DatFile;
import edu.gemini.itc.base.ITCConstants;

import java.util.Scanner;

public final class SlitThroughput {

    private static final double[] x_axis;
    private static final double[] y_axis;
    private static final double[][] _data;
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

    private final double im_qual;
    private final double pixel_size;
    private final double slit_width;
    private final double slit_ap;

    // constructor for the optimum aperture case
    public SlitThroughput(final double im_qual, final double pixel_size, final double slit_width) {
        this(im_qual, 1.4 * im_qual, pixel_size, slit_width);
    }

    //constructor for the user defined aperture case.
    public SlitThroughput(final double im_qual, final double user_def_ap, final double pixel_size, final double slit_width) {
        this.im_qual = im_qual;
        this.slit_ap = user_def_ap;
        this.pixel_size = pixel_size;
        this.slit_width = slit_width;
    }

    public double getSlitThroughput() {

        // find the x value
        final double spatial_pix = slit_ap / pixel_size;
        final int int_spatial_pix = new Double(spatial_pix + .5).intValue();
        double slit_spatial_ratio = int_spatial_pix * pixel_size / slit_width;

        // find the y value
        final double sigma = im_qual / 2.355;
        final double slit_spec_ratio = slit_width / sigma;

        //trap large values
        if (slit_spatial_ratio > 5.5) {
            slit_spatial_ratio = 5.499; //Slide in under the max
        }

        if (slit_spec_ratio > 8) {
            return 1;
        }

        // Do a 2D interpolation to find the return value using x= slit_spatial_ratio and y= slit_spec_ratio
        double slitThroughput = getSTvalue(slit_spatial_ratio, slit_spec_ratio);

        // Do a 2D interpolation to findethe return value using x= slit_spatial_ratio and y= slit_spec_ratio
        if (slitThroughput > 1.0) {
            return 1;
        } else {
            return slitThroughput;
        }
    }

    public double getSpatialPix() {
        return new Integer(new Double(slit_ap / pixel_size + 0.5).intValue()).doubleValue();
    }

    /**
     * @return y value at specified x using linear interpolation.
     * Silently returns zero if x is out of spectrum range.
     */
    public double getSTvalue(final double x, final double y) {
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
    public double getStartX() {
        return x_axis[0];
    }

    public double getXAxisValue(final int index) {
        return x_axis[index];
    }

    public double getYAxisValue(final int index) {
        return y_axis[index];
    }

    public int getXAxisSize() {
        return x_axis.length;
    }

    public int getYAxisSize() {
        return y_axis.length;
    }

    /**
     * @return starting y value
     */
    public double getStartY() {
        return y_axis[0];
    }

    /**
     * @return ending y value
     */
    public double getEndY() {
        return y_axis[y_axis.length - 1];
    }

    /**
     * Returns x value of specified data point.
     */
    public double getValue(final int index_x, final int index_y) {
        return _data[index_y][index_x];
    }

    /**
     * Returns the index of the data point with largest x value less than x
     */
    public int getLowerXIndex(final double x) {
        // x value is in.  The only solution is to search for it.
        // Small amt of data so just walk through.
        int low_index = 0;
        for (int i = 0; i < x_axis.length; ++i) {
            if (x >= x_axis[i])
                low_index = i;
        }
        return low_index;
    }

    public int getLowerYIndex(double x) {
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
