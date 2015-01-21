// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.shared;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of Spectrum interface.
 * This interface represents a 2-D spectrum.  The x and y axes are the
 * real numbers (doubles).  Data points are not necessarily at regular
 * x intevals.
 */
public class DefaultArraySpectrum implements ArraySpectrum {
    // The spectral data.  _data = new double[2][num_data_points]
    // data[0][i] = x values
    // data[1][i] = y values
    private double[][] _data;

    /**
     * Construce a DefaultSpectrum.  Lists represent data point pairs
     * so they must have the same length.
     *
     * @throws Exception If lists do not have same length or if they contain
     *                   anything other than Doubles
     */
    public DefaultArraySpectrum(List x_values, List y_values) throws Exception {
        _initialize(x_values, y_values);
    }

    /**
     * Construce a DefaultSpectrum.  Data array must be of this form:
     * double data[][] = new double[2][length];
     * data[0][i] = x values
     * data[1][i] = y values
     *
     * @throws Exception If array is not of proper form Exception is thrown
     */
    public DefaultArraySpectrum(double[][] data) throws Exception {
        // System.arraycopy() works only on 1-D arrays.
        int length = data.length;
        if (length != 2) {
            throw new Exception("Spectrum array dimension must be 2, but it is "
                    + length);
        }
        int x_len = data[0].length;
        int y_len = data[1].length;
        if (x_len != y_len) {
            throw new Exception("Spectrum data invalid, " + x_len + " x values "
                    + y_len + " y values");
        }
        _data = new double[2][x_len];
        for (int i = 0; i < x_len; ++i) {
            _data[0][i] = data[0][i];
            _data[1][i] = data[1][i];
        }
    }

    /**
     * Constructs a DefaultArraySpectrum from the specified file.
     *
     * @param fileName This method uses class.getResource() to search
     *                 for this resource.  So fileName should be relative to some
     *                 location on the classpath.
     *                 <p/>
     *                 Each line of a ArraySpectrum data file consists of two doubles
     *                 separated by whitespace or comma.
     */
    public DefaultArraySpectrum(String fileName) throws Exception {
        TextFileReader dfr = new TextFileReader(fileName);

        // These lists hold doubles from the data file.
        List<Double> x_values = new ArrayList<Double>();
        List<Double> y_values = new ArrayList<Double>();

        double x = 0;
        double y = 0;
        double dummy;
        //Check to see if the first line has an effective wavelength. some will.
        //If it does read in as a dummy value

        try {
            if (dfr.countTokens() == 1)
                dummy = dfr.readDouble();

            while (true) {
                x = dfr.readDouble();
                x_values.add(x);
                y = dfr.readDouble();
                y_values.add(y);
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
        x_values.clear();
        x_values = null;
        y_values.clear();
        y_values = null;
    }

    private void _initialize(List x_values, List y_values) throws Exception {
        if (x_values.size() != y_values.size()) {
            throw new Exception("Spectrum data invalid, " + x_values.size()
                    + " x values " + y_values.size() + " y values");
        }
        try {
            _data = new double[2][x_values.size()];
            for (int i = 0; i < x_values.size(); ++i) {
                _data[0][i] = ((Double) x_values.get(i)).doubleValue();
                _data[1][i] = ((Double) y_values.get(i)).doubleValue();
            }
        } catch (Exception e) {
            throw new Exception("Spectrum data invalid");
        }
    }

    /**
     * Implements Cloneable interface
     */
    public Object clone() {
        int x_len = _data[0].length;
        double[][] data = new double[2][x_len];
        for (int i = 0; i < x_len; ++i) {
            data[0][i] = _data[0][i];
            data[1][i] = _data[1][i];
        }
        DefaultArraySpectrum sp = null;
        try {
            sp = new DefaultArraySpectrum(data);
        } catch (Exception e) {/* shouldn't ever fail*/
        }
        return sp;
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
     * @return number of bins in the histogram (number of data points)
     */
    public int getLength() {
        return _data[0].length;
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

    public void applyWavelengthCorrection() {

        for (int i = 0; i < getLength(); ++i) {
            _data[1][i] = _data[1][i] * _data[0][i];
        }
    }

    /**
     * Sets y value in specified x bin.
     * If specified bin is out of range, this is a no-op.
     */
    public void setY(int index, double y) {
        if (index < 0 || index >= getLength()) return;  // no-op
        _data[1][index] = y;
    }

    /**
     * Rescales X axis by specified factor.
     */
    public void rescaleX(double factor) {
        if (factor == 1.0) return;
        for (int i = 0; i < getLength(); ++i) {
            _data[0][i] *= factor;
        }
    }

    /**
     * Rescales Y axis by specified factor.
     */
    public void rescaleY(double factor) {
        if (factor == 1.0) return;
        for (int i = 0; i < getLength(); ++i) {
            _data[1][i] *= factor;
        }
    }

    public void smoothY(int smoothing_element) {
        if (smoothing_element == 1.0) return;
        for (int i = 0; i < getLength(); ++i) {
            try {
                if (i + smoothing_element > getLength())
                    _data[1][i] = getAverage(i, getLength());
                else
                    _data[1][i] = getAverage(i, i + smoothing_element);

            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
    }


    /**
     * Returns the sum of all the y values in the entire spectrum.
     */
    public double getSum() {
        double sum = 0;
        try {
            sum = getSum(0, getLength() - 1);
        } catch (Exception e) {
        }
        return sum;
    }

    /**
     * Returns the integral of entire spectrum.
     */
    public double getIntegral() {
        double integral = 0;
        try {
            integral = getIntegral(getStart(), getEnd());
        } catch (Exception e) {
        }
        return integral;
    }

    /**
     * Returns the average of all the y values in the entire spectrum
     */
    public double getAverage() {
        double average = 0;
        try {
            average = getAverage(getStart(), getEnd());
        } catch (Exception e) {
        }
        return average;
    }

    /**
     * Returns the sum of y values in the spectrum in
     * the specified index range.
     *
     * @throws Exception If either limit is out of range.
     */
    public double getSum(int startIndex, int endIndex) throws Exception {
        if (startIndex < 0 || startIndex >= getLength() ||
                endIndex < 0 || endIndex >= getLength()) {
            throw new Exception("Sum out of bounds: summing " +
                    startIndex + " to " + endIndex +
                    " for spectra from " +
                    +getStart() + " to " + getEnd());
        }

        if (startIndex > endIndex) {
            int temp = startIndex;
            startIndex = endIndex;
            endIndex = temp;
        }

        double sum = 0;
        for (int i = startIndex; i <= endIndex; ++i) {
            sum += getY(i);
        }
        return sum;
    }

    /**
     * Returns the sum of y values in the spectrum in
     * the specified range.
     *
     * @throws Exception If either limit is out of range.
     */
    public double getSum(double x_start, double x_end) throws Exception {
        if (x_start < getStart() || x_start > getEnd() ||
                x_end < getStart() || x_end > getEnd()) {
            throw new Exception("Sum out of bounds: summing " +
                    x_start + " to " + x_end + " for spectra from " +
                    +getStart() + " to " + getEnd());
        }

        if (x_start > x_end) {
            double temp = x_start;
            x_start = x_end;
            x_end = temp;
        }

        int startIndex = getLowerIndex(x_start);
        int endIndex = getLowerIndex(x_end);
        if (getX(endIndex) < x_end) endIndex++;

        return getSum(startIndex, endIndex);
    }

    /**
     * Returns the integral of y values in the spectrum in
     * the specified range.
     *
     * @throws Exception If either limit is out of range.
     */
    public double getIntegral(double x_start, double x_end) throws Exception {
        if (x_start < getStart() || x_start > getEnd() ||
                x_end < getStart() || x_end > getEnd()) {
            throw new Exception("Integral out of bounds: integrating " +
                    x_start + " to " + x_end + " for spectra from " +
                    +getStart() + " to " + getEnd());
        }

        boolean negative = false;
        if (x_start > x_end) {
            double temp = x_start;
            x_start = x_end;
            x_end = temp;
            negative = true;
        }

        // Add up trapezoid areas.
        // x1 and x2 may not be exactly on sampling points so do
        // first and last trapezoid separately.
        double area = 0.0;
        int start_index, end_index;
        double delta_x, delta_y, y_min, y1, y2, x1, x2;

        x1 = x_start;
        start_index = getLowerIndex(x1);
        start_index++;  // right side of first trapezoid
        x2 = getX(start_index);
        y1 = getY(x1);
        y2 = getY(start_index);
        delta_x = x2 - x1;
        area += delta_x * (y1 + y2) / 2.0;

        x2 = x_end;
        end_index = getLowerIndex(x2);
        x1 = getX(end_index);
        y2 = getY(x2);
        y1 = getY(end_index);
        delta_x = x2 - x1;
        area += delta_x * (y1 + y2) / 2.0;

        area += getIntegral(start_index, end_index);

        return (negative) ? -area : area;
    }

    /**
     * Returns the integral of values in the ArraySpectrum in the
     * specified range between specified indices.
     *
     * @throws Exception If either limit is out of range.
     */
    public double getIntegral(int start_index, int end_index) throws Exception {
        if (start_index < 0 || start_index >= getLength() ||
                end_index < 0 || end_index >= getLength()) {
            throw new Exception("Integral out of bounds: integrating from index "
                    + start_index + " to " + end_index
                    + " for spectra of length " + getLength());
        }

        boolean negative = false;
        if (start_index > end_index) {
            int temp = start_index;
            start_index = end_index;
            end_index = temp;
            negative = true;
        }

        double area = 0.0;
        double delta_x, delta_y, y_min, y1, y2, x1, x2;

        // Add up trapezoid areas.
        for (int index = start_index; index < end_index; ++index) {
            x1 = getX(index);
            x2 = getX(index + 1);
            y1 = getY(index);
            y2 = getY(index + 1);
            delta_x = x2 - x1;
            area += delta_x * (y2 + y1) / 2.0;
        }

        return (negative) ? -area : area;
    }

    /**
     * Returns the average of values in the SampledSpectrum in
     * the specified range.
     *
     * @throws Exception If either limit is out of range.
     */
    public double getAverage(double x_start, double x_end) throws Exception {
        return getIntegral(x_start, x_end) / (x_end - x_start);
    }

    /**
     * Returns the average of values in the SampledSpectrum in
     * the specified range.
     *
     * @throws Exception If either limit is out of range.
     */
    public double getAverage(int indexStart, int indexEnd) throws Exception {
        return getIntegral(indexStart, indexEnd) /
                (getX(indexEnd) - getX(indexStart));
    }

    /**
     * This returns a 2d array of the SED data used to chart the SED
     * using JClass Chart.  The array has the following dimensions
     * double data[][] = new double[2][getLength()];
     * data[0][i] = x values
     * data[1][i] = y values
     * May return reference to member data, so client should not
     * alter the return value.
     */
    public double[][] getData() {
        return _data;
    }

    /**
     * This returns a 2d array of the SED data used to chart the SED
     * using JClass Chart.  The array has the following dimensions
     * double data[][] = new double[2][getLength()];
     * data[0][i] = x values
     * data[1][i] = y values
     *
     * @param maxIndex data returned up to maximum specified
     *                 x bin.  If maxIndex >= number of data points, maxIndex
     *                 will be truncated.
     */
    public double[][] getData(int maxIndex) {
        return getData(0, maxIndex);
    }

    /**
     * This returns a 2d array of the SED data used to chart the SED
     * using JClass Chart.  The array has the following dimensions
     * double data[][] = new double[2][getLength()];
     * data[0][i] = x values
     * data[1][i] = y values
     *
     * @param minIndex data returned from minimum specified
     * @param maxIndex data returned up to maximum specified
     *                 x bin.  If maxIndex >= number of data points, maxIndex
     *                 will be truncated.
     */
    public double[][] getData(int minIndex, int maxIndex) {
        if (minIndex < 0) minIndex = 0;
        if (maxIndex >= _data[0].length) maxIndex = _data[0].length - 1;
        double[][] data = new double[2][maxIndex - minIndex + 1];
        for (int i = minIndex; i <= maxIndex; i++) {
            data[0][i - minIndex] = _data[0][i];
            data[1][i - minIndex] = _data[1][i];
        }
        return data;
    }
}
