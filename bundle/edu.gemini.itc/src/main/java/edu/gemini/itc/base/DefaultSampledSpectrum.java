package edu.gemini.itc.base;
import java.util.logging.Logger;

/**
 * Default implementation of SampledSpectrum interface.
 * This implementation internally has a uniformly-spaced data points.
 * A SampledSpectrum can conceptually be thought of as a 2-D spectrum
 * (data points on axes of real numbers) where the data points occur
 * at regular intervals on the x axis.
 * This interface provides functionality for defining and manipulating a
 * SampledSpectrum.
 * Units are not stored for either axis so the client must know from context.
 * The SampledSpectrum plays the role of Element in a visitor pattern.
 * This pattern is used to separate operations from the SampledSpectrum
 * elements.
 * The SampledSpectrum plays the role of Element in a visitor pattern.
 * This pattern is used to separate operations from the elements.
 * Because of this separation, Concrete Elements must offer enough
 * accessors for the separate Concrete Visitor class to perform the
 * manipulation.
 * DefaultSampledSpectrum plays the role of a Concrete Element.
 */
public class DefaultSampledSpectrum implements VisitableSampledSpectrum {
    private static final Logger Log = Logger.getLogger(DefaultSampledSpectrum.class.getName());
    private double[] _y; //Array containing flux values in relative units

    //Values of start and end points
    private double _xStart, _xEnd;
    private double _xInterval;     //Size of each particular element

    /**
     * Construct a DefaultSampledSpectrum.  End x value is determined by
     * number of data points and the specified interval.
     */
    public DefaultSampledSpectrum(double[] y, double xStart,
                                  double xInterval) {
        reset(y, xStart, xInterval);
    }

    /**
     * Construct a DefaultSampledSpectrum by sampling the given
     * ArraySpectrum at specified interval.
     *
     * @param sp        Spectrum to sample
     * @param xInterval Sampling interval (nm)
     */
    public DefaultSampledSpectrum(ArraySpectrum sp, double xInterval) {
        double xStart = sp.getStart();
        double xEnd = sp.getEnd();
        Log.fine("xStart = " + xStart + " nm (from SED)");
        Log.fine("xEnd = " + xEnd + " nm");
        int numIntervals = (int) ((xEnd - xStart) / xInterval);
        double[] data = new double[numIntervals + 1];
        for (int i = 0; i <= numIntervals; ++i) {
            data[i] = sp.getY(i * xInterval + xStart);
        }
        reset(data, xStart, xInterval);
    }

    /**
     * Construct a DefaultSampledSpectrum by sampling the given ArraySpectrum
     * over the specified wavelength range at the specified interval.
     *
     * @param sp        Spectrum to resample
     * @param xStart    Starting wavelength of instrument configuration (nm)
     * @param xEnd      Ending wavelength of instrument configuration (nm)
     * @param xInterval Sampling interval (nm)
     * @param z         Redshift of target
     */
    public DefaultSampledSpectrum(ArraySpectrum sp, double xStart, double xEnd, double xInterval, double z) {
        Log.fine("xStart = " + xStart + " nm (specified)");
        Log.fine("xEnd = " + xEnd + " nm");

        if ( (1+z) * sp.getStart() > xStart || (1+z) * sp.getEnd() < xEnd ) {
            throw new IllegalArgumentException(
                    String.format("Redshifted SED (%.1f - %.1f nm) does not cover the range of the instrument and normalization band " +
                                    "(%.1f - %.1f nm).", (1+z)*sp.getStart(), (1+z)*sp.getEnd(), xStart, xEnd));
        }

        // The SED will be redshifted later so resample over range / (1+z):
        xStart /= 1+z;
        xEnd /= 1+z;

        int numIntervals = (int) Math.round((xEnd - xStart) / xInterval + 2); // +2 to allow for truncation
        double[] data = new double[numIntervals + 1];
        for (int i = 0; i <= numIntervals; ++i) {
           data[i] = sp.getY(xStart + i * xInterval);
        }
        reset(data, xStart, xInterval);
    }

    /**
     * Implements the Cloneable interface.
     */
    @Override public Object clone() {
        double[] data = new double[getLength()];
        System.arraycopy(getValues(), 0, data, 0, getLength());
        return new DefaultSampledSpectrum(data, getStart(), getSampling());
    }

    @Override public void trim(double newStart, double newEnd) {
        if (newStart < getStart()) {
            newStart = getStart();
        }
        if (newEnd > getEnd()) {
            newEnd = getEnd();
        }
        if (newEnd < getStart() || newStart > getEnd()) {
            return;
        }
        double[] data = new double[new Double((newEnd - newStart) / _xInterval).intValue() + 4];
        //System.out.println("os: " + getStart() + "ns: " + newStart + " oe: " + getEnd() + " ne: " + newEnd);
        //System.out.println("startpos: " + new Double((newStart-getStart())/_xInterval).intValue() + "length: " + getLength() + " copylength: " + new Double((newEnd-newStart)/_xInterval).intValue());

        System.arraycopy(getValues(), new Double((newStart - getStart()) / _xInterval).intValue(), data, 0, new Double((newEnd - newStart) / _xInterval).intValue());
        reset(data, newStart, _xInterval);
    }


    /**
     * Sets all these SampledSpectrum parameters.
     * I don't like a method that sets everything at once, but I inherited
     * this code so I will leave it.
     */
    @Override public void reset(double[] y, double xStart,
                      double xInterval) {
        _y = new double[y.length];
        // need our own copy so client can't mess with it.
        System.arraycopy(y, 0, _y, 0, y.length);
        _xStart = xStart;
        _xInterval = xInterval;
        _xEnd = _xStart + (_y.length - 1) * _xInterval;
    }


    /**
     * The accept(SampledSpectrumVisitor) method is used by Visitors to
     * visit the SampledSpectrum.
     * This is the way a SampledSpectrum is manipulated.
     * <p/>
     * Example:
     * <p/>
     * SampledSpectrum s = SampledSpectrumFactory.getSampledSpectrum("SampledSpectrumFILE");
     * SampledSpectrumVisitor r = new Resample();
     * s.Accept(r);
     */
    @Override public void accept(SampledSpectrumVisitor v) {
        v.visit(this);
    }

    //**********************
    // Accessors
    //

    /**
     * @return array of flux values.  For efficiency, it may return a
     * referenct to actual member data.  The client must not alter this
     * return value.
     */
    @Override public double[] getValues() {
        return _y;
    }

    /**
     * @return starting x
     */
    @Override public double getStart() {
        return _xStart;
    }

    /**
     * @return ending x
     */
    @Override public double getEnd() {
        return _xEnd;
    }

    /**
     * @return x sample size (bin size)
     */
    @Override public double getSampling() {
        return _xInterval;
    }

    /**
     * @return flux value in specified bin
     */
    @Override public double getY(int index) {
        return _y[index];
    }

    /**
     * @return x of specified bin
     */
    @Override public double getX(int index) {
        return getStart() + index * getSampling();
    }

    /**
     * @return y value at specified x using linear interpolation.
     * Silently returns zero if x is out of spectrum range.
     */
    @Override public double getY(double x) {
        if (x < getStart() || x > getEnd()) return 0;
        if (x == getEnd()) return getY(getLength() - 1);
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
     * Returns the index of the data point with largest x value less than x
     */
    @Override public int getLowerIndex(double x) {
        return (int) ((x - getStart()) / getSampling());
    }

    /**
     * @return number of bins in the histogram (number of data points)
     */
    @Override public int getLength() {
        return _y.length;
    }


    //**********************
    // Mutators
    //


    @Override public void applyWavelengthCorrection() {
        double start = getStart();
        double sampling = getSampling();

        for (int i = 0; i < getLength(); ++i) {
            _y[i] = _y[i] * (start + i * sampling);
        }
    }

    /**
     * Sets y value in specified x bin.
     * If specified bin is out of range, this is a no-op.
     */
    @Override public void setY(int bin, double y) {
        if (bin < 0 || bin >= getLength()) return;  // no-op
        _y[bin] = y;
    }

    /**
     * Rescales X axis by specified factor. Doesn't change sampling size.
     */
    public void rescaleXwithFixedSampleSize(double factor) {
        Log.fine(String.format("Rescaling X by %.5f", factor));
        if (factor == 1.0) return;
        double xStart = getStart() * factor;
        double xEnd = getEnd() * factor;
        int numIntervals = (int) ((xEnd - xStart) / getSampling());
        double[] data = new double[numIntervals];
        double x;
        for (int i = 0; i < numIntervals; ++i) {
            x = (double) i * getSampling() + xStart;
            data[i] = getY(x / factor);
        }
        reset(data, xStart, getSampling());
    }

    /**
     * Rescales X axis by specified factor.  Doesn't change the number of samples.
     */
    public void rescaleX(double factor) {
        Log.fine(String.format("Rescaling X by %.5f", factor));
        if (factor == 1.0) return;
        int numIntervals = getLength();
        double xStart = getStart() * factor;
        double xEnd = getEnd() * factor;
        double sampling = (xEnd - xStart) / numIntervals;
        Log.fine(String.format("New sampling = %.5f nm", sampling));
        double[] data = new double[numIntervals];
        double x;
        for (int i = 0; i < numIntervals; ++i) {
            x = (double) i * sampling + xStart;
            data[i] = getY(x / factor);
        }
        reset(data, xStart, sampling);
    }

    /**
     * Rescales Y axis by specified factor.
     */
    @Override public void rescaleY(double factor) {
        if (factor == 1.0) return;
        Log.fine(String.format("Rescaling Y by %.5f", factor));
        for (int i = 0; i < getLength(); ++i) {
            _y[i] *= factor;
        }
    }

    @Override public void smoothY(int smoothing_element) {
        Log.fine(String.format("Smoothing Y by %d pix", smoothing_element));
        double[] _y_temp;
        _y_temp = new double[_y.length];
        if (smoothing_element == 1.0) return;
        for (int i = 0; i < getLength() - 1; ++i) {
            try {
                if (i + smoothing_element / 2 >= getLength())
                    _y_temp[i] = getAverage(i, getLength() - 1);
                else if (i - smoothing_element / 2 > 0 && smoothing_element % 2 != 0) { //if odd
                    //System.out.println(" mod: " +smoothing_element%2);
                    //double temp = _y[i-2]+_y[i-1]+_y[i]+_y[i+1]+_y[i+2];
                    _y_temp[i] = getAverage(i - (new Double((smoothing_element) / 2).intValue()), i + (new Double((smoothing_element) / 2).intValue()));
                    //_y[i]=temp/5;
                } else if (i - smoothing_element / 2 > 0) //if even
                    _y_temp[i] = getAverage(new Double(i - smoothing_element / 2).intValue() + 1, new Double(i + smoothing_element / 2).intValue());
            } catch (Exception e) {
                System.out.println("Smooth: " + e.toString());
            }
        }
        //System.out.println("End"+ new Double(smoothing_element/2).intValue()+ "  " + getSampling());
        _y = _y_temp;
    }

    /**
     * Returns the integral of all the y values in the SampledSpectrum
     */
    @Override public double getIntegral() {
        return getIntegral(getStart(), getEnd());
    }

    /**
     * Returns the sum of y values in the spectrum in
     * the specified index range.
     *
     * @throws Exception If either limit is out of range.
     */
    private double getSum(int startIndex, int endIndex) {
        assert startIndex <= endIndex;
        assert startIndex >= 0 && startIndex < getLength();
        assert endIndex   >= 0 && endIndex   < getLength();

        double sum = 0.0;
        for (int i = startIndex; i <= endIndex; ++i) {
            sum += getY(i);
        }
        return sum;
    }

    /**
     * Returns the integral of y values in the spectrum in
     * the specified range.
     *
     * @throws Exception If either limit is out of range.
     */

    // Andy solution
    public double getIntegral(double x_start, double x_end) {
        assert x_start <= x_end;
        assert x_start >= getStart() && x_start <= getEnd();
        assert x_end   >= getStart() && x_end   <= getEnd();

        double area = 0.0;
        int start_index, end_index;
        double y1, y2, x1, x2;

        // x_start and x_end may not fall exactly on the underlying sampling grid.

        // If both x_start and x_end fall in the same sample grid just interpolate:
        if (getLowerIndex(x_start) == getLowerIndex(x_end)) {
            area += (x_end - x_start) * (getY(x_start) + getY(x_end)) / 2.0;
        }
        else {
            // Add up the area on either side of the sample grid and then add that to the area inside the grid.
            // calculate the area between x_start and the first sample point
            x1 = x_start;
            start_index = getLowerIndex(x1);
            start_index++;  // right side of first trapezoid
            x2 = getX(start_index);
            y1 = getY(x1);
            y2 = getY(start_index);
            area += (x2 - x1) * (y1 + y2) / 2.0;

            // calculate the area between the last sample point and x_end:
            x2 = x_end;
            end_index = getLowerIndex(x2);  // left side of last trapezoid
            x1 = getX(end_index);
            y2 = getY(x2);
            y1 = getY(end_index);
            area += (x2 - x1) * (y1 + y2) / 2.0;

            // add to the area inside the grid:
            area += getIntegral(start_index, end_index);
        }
        return area;
    }

    /**
     * Returns the integral of values in the SampledSpectrum in the
     * specified range between specified indices.
     */
    private double getIntegral(int start_index, int end_index) {
        assert start_index <= end_index;
        assert start_index >= 0 && start_index < getLength();
        assert end_index   >= 0 && end_index   < getLength();

        if (start_index == end_index) {
            return 0.0; // REL-478
        }

        // Add up trapezoidal areas.
        // We take advantage of the fact that the sampling is even.
        // area = (delta_x/2) * (y1 + 2y2 + 2y3 + ... * 2yn-1 + yn)
        // If the width is 0, i.e. start_index == end_index, the area is 0.
        double area = 0.0;
        if (end_index - start_index > 1) {
            area += 2.0 * getSum(start_index + 1, end_index - 1);
        }
        area += getY(start_index) + getY(end_index);
        area *= getSampling() / 2.0;

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
     * This returns a 2d array of the data used to chart the SampledSpectrum
     * using JClass Chart.  The array has the following dimensions
     * double data[][] = new double[2][getLength()];
     * data[0][i] = x values
     * data[1][i] = y values
     */
    @Override public double[][] getData() {
        return getData(_y.length - 1);  // the whole SampledSpectrum
    }

    /**
     * This returns a 2d array of the data used to chart the SampledSpectrum
     * using JClass Chart.  The array has the following dimensions
     * double data[][] = new double[2][getLength()];
     * data[0][i] = x values
     * data[1][i] = y values
     *
     * @param maxXIndex data is returned up to maximum specified x bin
     */
    @Override public double[][] getData(int maxXIndex) {
        return getData(0, maxXIndex);
    }

    /**
     * This returns a 2d array of the data used to chart the SampledSpectrum
     * using JClass Chart.  The array has the following dimensions
     * double data[][] = new double[2][getLength()];
     * data[0][i] = x values
     * data[1][i] = y values
     *
     * @param minXIndex data is returned starts at minimum specified x bin
     * @param maxXIndex data is returned up to maximum specified x bin
     */
    @Override public double[][] getData(int minXIndex, int maxXIndex) {
        if (maxXIndex >= _y.length) maxXIndex = _y.length - 1;
        if (minXIndex < 0) maxXIndex = 0;
        double data[][] = new double[2][maxXIndex - minXIndex + 1];
        for (int i = minXIndex; i <= maxXIndex; i++) {
            data[0][i - minXIndex] = getStart() + i * getSampling();
            data[1][i - minXIndex] = _y[i];
        }
        return data;
    }

}
