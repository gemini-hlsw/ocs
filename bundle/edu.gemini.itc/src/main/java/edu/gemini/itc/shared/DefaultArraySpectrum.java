package edu.gemini.itc.shared;

import java.util.List;

/**
 * Default implementation of Spectrum interface.
 * This interface represents a 2-D spectrum.  The x and y axes are the
 * real numbers (doubles).  Data points are not necessarily at regular
 * x intervals.
 */
public final class DefaultArraySpectrum implements ArraySpectrum {
    // The spectral data.  _data = new double[2][num_data_points]
    // data[0][i] = x values
    // data[1][i] = y values
    private final double[][] _data;

    /**
     * Construct a DefaultSpectrum.  Lists represent data point pairs
     * so they must have the same length.
     */
    public DefaultArraySpectrum(final List<Double> xs, final List<Double> ys) {
        assert xs.size() == ys.size();

        _data = new double[2][xs.size()];
        for (int i = 0; i < xs.size(); ++i) {
            _data[0][i] = xs.get(i);
            _data[1][i] = ys.get(i);
        }
    }

    /**
     * Construce a DefaultSpectrum.  Data array must be of this form:
     * double data[][] = new double[2][length];
     * data[0][i] = x values
     * data[1][i] = y values
     */
    public DefaultArraySpectrum(final double[][] data) {
        assert data.length == 2;
        assert data[0].length == data[1].length;

        _data = new double[2][];
        _data[0] = data[0].clone();
        _data[1] = data[1].clone();
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
        final double[][] data  = SpectrumParser$.MODULE$.loadFromFile(fileName);
        // for now make a copy of cached values, just to be on the safe side
        _data = new double[2][];
        _data[0] = data[0].clone();
        _data[1] = data[1].clone();
    }

    /**
     * Implements Cloneable interface
     */
    public Object clone() {
        // constructor will clone the data array
        return new DefaultArraySpectrum(_data);
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
        return getSum(0, getLength() - 1);
    }

    /**
     * Returns the integral of entire spectrum.
     */
    public double getIntegral() {
        return getIntegral(getStart(), getEnd());
    }

    /**
     * Returns the average of all the y values in the entire spectrum
     */
    public double getAverage() {
        return getAverage(getStart(), getEnd());
    }

    /**
     * Returns the sum of y values in the spectrum in
     * the specified index range.
     */
    public double getSum(int startIndex, int endIndex) {
        assert startIndex >= 0 && startIndex < getLength();
        assert endIndex   >= 0 && endIndex   < getLength();

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
     */
    public double getSum(double x_start, double x_end) {
        assert x_start >= getStart() && x_start <= getEnd();
        assert x_end   >= getStart() && x_end   <= getEnd();

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
     */
    public double getIntegral(double x_start, double x_end) {
        assert x_start >= getStart() && x_start <= getEnd();
        assert x_end   >= getStart() && x_end   <= getEnd();

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
        double delta_x, y1, y2, x1, x2;

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
     */
    public double getIntegral(int start_index, int end_index) {
        assert start_index >= 0 && start_index < getLength();
        assert end_index   >= 0 && end_index   < getLength();

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
     */
    public double getAverage(double x_start, double x_end) {
        return getIntegral(x_start, x_end) / (x_end - x_start);
    }

    /**
     * Returns the average of values in the SampledSpectrum in
     * the specified range.
     */
    public double getAverage(int indexStart, int indexEnd) {
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
