// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.


package edu.gemini.itc.operation;

import edu.gemini.itc.shared.ITCConstants;
import edu.gemini.itc.shared.TextFileReader;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class PeakPixelFluxCalc {

    private double im_qual, pixel_size, exp_time;
    private double summed_source, summed_background, dark_current;
    private double[][] _data;
    private String fileName = ITCConstants.CALC_LIB +
            ITCConstants.FLUX_IN_PEAK_PIXEL_FILENAME + ITCConstants.DATA_SUFFIX;


    public PeakPixelFluxCalc(double im_qual, double pixel_size,
                             double exp_time, double summed_source,
                             double summed_background, double dark_current)
            throws Exception {
        //set up the text file reader
        TextFileReader dfr = new TextFileReader(fileName);
        List x_values = new ArrayList();
        List y_values = new ArrayList();

        double x = 0;
        double y = 0;
        try {
            while (true) {
                x = dfr.readDouble();
                x_values.add(new Double(x));
                y = dfr.readDouble();
                y_values.add(new Double(y));
            }
        } catch (ParseException e) {
            throw e;
        } catch (IOException e) {
// This is normal and happens at the end of file
        }

        if (y_values.size() != x_values.size()) {
            throw new Exception("Error in file " + fileName + ", not same number of x and y data points.");
        }

        if (y_values.size() < 1) {
            throw new Exception("No values found in file " + fileName);
        }

        _initialize(x_values, y_values);  // throws Exception

        //stick a copy into our private vars
        this.im_qual = im_qual;
        this.pixel_size = pixel_size;
        this.exp_time = exp_time;
        this.summed_source = summed_source;
        this.summed_background = summed_background;
        this.dark_current = dark_current;
    }

    public double getFluxInPeakPixel() {
        double im_qual_in_pix, frac_in_peak, source_fraction_in_peak,
                source_in_peak;
        double background_in_peak;

        im_qual_in_pix = im_qual / pixel_size;
        frac_in_peak = getY(im_qual_in_pix);
        if (frac_in_peak > 1000)
            source_fraction_in_peak = 0;
        else
            source_fraction_in_peak = frac_in_peak;
        source_in_peak = summed_source * source_fraction_in_peak * exp_time;
        background_in_peak = summed_background * exp_time * pixel_size * pixel_size;
        return source_in_peak + background_in_peak + dark_current * exp_time;

    }

    public double getFluxInPeakPixelUSB(double source_fraction, double enclosedPixels) {
        double background_in_peak, source_in_peak;

        source_in_peak = summed_source * source_fraction * exp_time / enclosedPixels;
        background_in_peak = summed_background * exp_time * pixel_size * pixel_size;
        return source_in_peak + background_in_peak + dark_current * exp_time;
    }


    private void _initialize(List x_values, List y_values) throws Exception {
        if (x_values.size() != y_values.size()) {
            throw new Exception("Flux data invalid, " + x_values.size()
                    + " x values " + y_values.size()
                    + " y values");
        }
        try {
            _data = new double[2][x_values.size()];
            for (int i = 0; i < x_values.size(); ++i) {
                _data[0][i] = ((Double) x_values.get(i)).doubleValue();
                _data[1][i] = ((Double) y_values.get(i)).doubleValue();
            }
        } catch (Exception e) {
            throw new Exception("Flux data invalid");
        }
    }

    /**
     * @return y value at specified x using linear interpolation.
     * Silently returns zero if x is out of spectrum range.
     */
    public double getY(double x) {
        if (x < getStart() || x > getEnd()) return 0;
        int low_index = getLowerIndex(x);
        int high_index = low_index + 1;
        double y1 = getY(low_index);
        double y2 = getY(high_index);
        double x1 = getX(low_index);
        double x2 = getX(high_index);
        double slope = (y2 - y1) / (x2 - x1);
        return (slope * (x - x1) + y1);
    }

    /**
     * @return starting x value
     */
    public double getStart() {
        return _data[0][0];
    }

    /**
     * @return ending x value
     */
    public double getEnd() {
        return _data[0][_data[0].length - 1];
    }

    /**
     * Returns x value of specified data point.
     */
    public double getX(int index) {
        return _data[0][index];
    }

    /**
     * Returns y value of specified data point.
     */
    public double getY(int index) {
        return _data[1][index];
    }


    /**
     * Returns the index of the data point with largest x value less than x
     */
    public int getLowerIndex(double x) {
        // In a general spectrum we don't have an idea which bin a particular
        // x value is in.  The only solution is to search for it.
        // Could just walk through, but do a binary search for it.
        int low_index = 0;
        int high_index = _data[0].length;
        if (high_index - low_index <= 1) return low_index;
        while (high_index - low_index > 1) {
            int index = (high_index + low_index) / 2;
            if (getX(index) < x)
                low_index = index;
            else
                high_index = index;
        }
        return low_index;
    }
}
