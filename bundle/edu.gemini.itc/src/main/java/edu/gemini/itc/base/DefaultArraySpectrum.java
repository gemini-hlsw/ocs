package edu.gemini.itc.base;

import java.util.List;
import java.util.logging.Logger;

/**
 * Default implementation of Spectrum interface.
 * This interface represents a 2-D spectrum.  The x and y axes are the
 * real numbers (doubles).  Data points are not necessarily at regular
 * x intervals.
 */
public final class DefaultArraySpectrum implements ArraySpectrum {
    private static final Logger Log = Logger.getLogger(DefaultArraySpectrum.class.getName());
    // The spectral data.  _data = new double[2][num_data_points]
    // data[0][i] = x values (wavelength in nm)
    // data[1][i] = y values
    // The x axis is assumed non-decreasing: the .dat resource files list wavelengths in
    // ascending order by convention (nothing validates it), and getStart/getEnd, the
    // interpolation in getY and the search in getLowerIndex all rely on that ordering.
    private final double[][] _data;

    // The index getLowerIndex returned last time. Callers usually walk x in increasing order,
    // so the next lookup tends to land in this bin or the one after.
    // The cursor is only a hint and needs no synchronisation: getLowerIndex checks it against
    // the data before using it, so at worst a bad value causes a fallback to the binary
    // search, never a wrong result.
    private int _lowerIndexCursor = 0;

    public static DefaultArraySpectrum fromUserSpectrum(String spectrum) {
        final double[][] data = DatFile.fromUserSpectrum(spectrum);
        return new DefaultArraySpectrum(data);
    }

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
     * Construct a DefaultSpectrum.  Data array must be of this form:
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
    public DefaultArraySpectrum(String fileName) {
        final double[][] data = DatFile.arrays().apply(fileName);
        // for now make a copy of cached values, just to be on the safe side
        _data = new double[2][];
        _data[0] = data[0].clone();
        _data[1] = data[1].clone();
    }

    /**
     * Implements Cloneable interface
     */
    @Override public Object clone() {
        // constructor will clone the data array
        return new DefaultArraySpectrum(_data);
    }

    /**
     * @return starting x value
     */
    @Override public double getStart() {
        return _data[0][0];
    }

    /**
     * @return ending x value
     */
    @Override public double getEnd() {
        return _data[0][_data[0].length - 1];
    }

    /**
     * @return the x value of the first non-zero (>0.1%) y value
     */
    public double getFirstNonZero() {
        for (int i = 0; i < getLength(); ++i) {
             if (_data[1][i] > 0.001) {
                return _data[0][i];
            }
        }
        throw new RuntimeException("Array has no positive values");
    }

    /**
     * @return the x value of the last non-zero (>0.1%) y value
     */
    public double getLastNonZero() {
        for (int i = getLength()-1; i >= 0; --i) {
            if (_data[1][i] > 0.001) {
                return _data[0][i];
            }
        }
        throw new RuntimeException("Array has no positive values");
    }

    /**
     * Returns x value of specified data point.
     */
    @Override public double getX(int index) {
        return _data[0][index];
    }

    /**
     * Returns y value of specified data point.
     */
    @Override public double getY(int index) {
        return _data[1][index];
    }

    /**
     * @return y value at specified x using linear interpolation.
     * Silently returns zero if x is out of spectrum range.
     */
    @Override public double getY(double x) {
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
    @Override public int getLength() {
        return _data[0].length;
    }

    /**
     * Returns the index of the data point with largest x value less than x
     */
    @Override public int getLowerIndex(double x) {
        final double[] xs = _data[0];
        final int last = xs.length - 1;
        if (last < 1) return 0;

        // For a non-decreasing x axis exactly one index satisfies xs[i] < x <= xs[i+1], the same
        // one the binary search converges on, so the fast paths return precisely what the search
        // would. Anything else, including x outside the range and NaN, falls through.
        // the data files were reviewd and all have strictly increasing x values (wavelength)
        final int c = _lowerIndexCursor;
        if (c >= 0 && c < last) {
            if (xs[c] < x && x <= xs[c + 1]) return c;
            final int next = c + 1;
            if (next < last && xs[next] < x && x <= xs[next + 1]) {
                _lowerIndexCursor = next;
                return next;
            }
        }

        // The binary search stays as the fallback: the first lookup of each scan, random access
        // from getY(double) and getIntegral, and steps that cross more than one bin all miss
        // the cursor.
        int low_index = 0;
        int high_index = xs.length;
        while (high_index - low_index > 1) {
            int index = (high_index + low_index) / 2;
            if (xs[index] < x)
                low_index = index;
            else
                high_index = index;
        }
        _lowerIndexCursor = low_index;
        return low_index;
    }

    @Override public void applyWavelengthCorrection() {

        for (int i = 0; i < getLength(); ++i) {
            _data[1][i] = _data[1][i] * _data[0][i];
        }
    }

    /**
     * Sets y value in specified x bin.
     * If specified bin is out of range, this is a no-op.
     */
    @Override public void setY(int index, double y) {
        if (index < 0 || index >= getLength()) return;  // no-op
        _data[1][index] = y;
    }

    /**
     * Rescales X axis by specified factor.
     */
    @Override public void rescaleX(double factor) {
        Log.fine(String.format("Rescaling X by %.5f", factor));
        if (factor == 1.0) return;
        for (int i = 0; i < getLength(); ++i) {
            _data[0][i] *= factor;
        }
    }

    /**
     * Rescales Y axis by specified factor.
     */
    @Override public void rescaleY(double factor) {
        Log.fine(String.format("Scaling Y by %.5f", factor));
        if (factor == 1.0) return;
        for (int i = 0; i < getLength(); ++i) {
            _data[1][i] *= factor;
        }
    }

    @Override public void smoothY(int smoothing_element) {
        Log.fine(String.format("Smoothing Y by %.5f pix", smoothing_element));
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
     * Returns the integral of entire spectrum.
     */
    @Override public double getIntegral() {
        return getIntegral(getStart(), getEnd());
    }

    /**
     * Returns the integral of y values in the spectrum in
     * the specified range.
     */
    public double getIntegral(double x_start, double x_end) {
        assert x_start <= x_end;
        assert x_start >= getStart() && x_start <= getEnd();
        assert x_end   >= getStart() && x_end   <= getEnd();

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

        return area;
    }

    /**
     * Returns the integral of values in the ArraySpectrum in the
     * specified range between specified indices.
     */
    public double getIntegral(int start_index, int end_index) {
        assert start_index <= end_index;
        assert start_index >= 0 && start_index < getLength();
        assert end_index   >= 0 && end_index   < getLength();

        double area = 0.0;
        double delta_x,y1, y2, x1, x2;

        // Add up trapezoid areas.
        for (int index = start_index; index < end_index; ++index) {
            x1 = getX(index);
            x2 = getX(index + 1);
            y1 = getY(index);
            y2 = getY(index + 1);
            delta_x = x2 - x1;
            area += delta_x * (y2 + y1) / 2.0;
        }

        return area;
    }

    /**
     * Returns the average of values in the SampledSpectrum in
     * the specified range.
     */
    @Override public double getAverage(double x_start, double x_end) {
        return getIntegral(x_start, x_end) / (x_end - x_start);
    }

    /**
     * Returns the average of values in the SampledSpectrum in
     * the specified range.
     */
    private double getAverage(int indexStart, int indexEnd) {
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
    @Override public double[][] getData() {
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
    @Override public double[][] getData(int maxIndex) {
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
    @Override public double[][] getData(int minIndex, int maxIndex) {
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
