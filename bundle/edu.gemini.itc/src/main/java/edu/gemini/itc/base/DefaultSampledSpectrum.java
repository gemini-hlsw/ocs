package edu.gemini.itc.base;
import java.util.logging.Logger;
import edu.gemini.itc.shared.SourceDefinition;

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

    // x(i) = _xOrigin + (_xOffset + i) * _xInterval; a cut-out keeps the full spectrum's origin so
    // its x values are the same doubles. Usually _xOffset is 0 and _xOrigin equals _xStart.
    private double _xOrigin;
    private int _xOffset;

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
        adopt(data, xStart, xInterval);
    }

    /** Samples sp at xInterval keeping only the samples that cover [lo, hi] (nm), on the grid of the two argument constructor. */
    public DefaultSampledSpectrum(ArraySpectrum sp, double xInterval, double lo, double hi) {
        double xStart = sp.getStart();
        double xEnd = sp.getEnd();
        int numIntervals = (int) ((xEnd - xStart) / xInterval);
        int first = Math.max(0, Math.min(numIntervals, (int) Math.floor((lo - xStart) / xInterval)));
        int last  = Math.max(first, Math.min(numIntervals, (int) Math.ceil((hi - xStart) / xInterval)));
        Log.fine("Sampling " + xStart + " - " + xEnd + " nm restricted to " + lo + " - " + hi + " nm: "
                + (last - first + 1) + " of " + (numIntervals + 1) + " samples");
        double[] data = new double[last - first + 1];
        for (int i = first; i <= last; ++i) {
            data[i - first] = sp.getY(i * xInterval + xStart);
        }
        adoptAt(data, xStart, first, xInterval);
    }

    /** A cut-out starting at index xOffset of the grid with origin xOrigin. Takes ownership of y. */
    public static DefaultSampledSpectrum offset(double[] y, double xOrigin, int xOffset, double xInterval) {
        DefaultSampledSpectrum s = new DefaultSampledSpectrum();
        s.adoptAt(y, xOrigin, xOffset, xInterval);
        return s;
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

        // Validate spectrum range covers required range after redshift
        // SEDFactory.validateUserSpectrumRange(sp, xStart, xEnd, z);

        // The SED will be redshifted later so resample over range / (1+z):
        xStart /= 1+z;
        xEnd /= 1+z;

        int numIntervals = (int) Math.round((xEnd - xStart) / xInterval + 2); // +2 to allow for truncation
        double[] data = new double[numIntervals + 1];
        for (int i = 0; i <= numIntervals; ++i) {
           data[i] = sp.getY(xStart + i * xInterval);
        }
        adopt(data, xStart, xInterval);
    }

    /**
     * Implements the Cloneable interface.
     */
    @Override public Object clone() {
        double[] data = new double[getLength()];
        System.arraycopy(getValues(), 0, data, 0, getLength());
        DefaultSampledSpectrum copy = new DefaultSampledSpectrum();
        copy.adoptAt(data, _xOrigin, _xOffset, _xInterval);
        return copy;
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
        int copyLength = (int) ((newEnd - newStart) / _xInterval);
        int startPos = getLowerIndex(newStart);
        double[] data = new double[copyLength + 4];
        System.arraycopy(getValues(), startPos, data, 0, copyLength);
        adopt(data, newStart, _xInterval);
    }


    /**
     * Sets all these SampledSpectrum parameters.
     * I don't like a method that sets everything at once, but I inherited
     * this code so I will leave it.
     */
    @Override public void reset(double[] y, double xStart,
                      double xInterval) {
        // need our own copy so client can't mess with it.
        double[] copy = new double[y.length];
        System.arraycopy(y, 0, copy, 0, y.length);
        adopt(copy, xStart, xInterval);
    }

    // Uninitialised instance for internal use; callers must adopt() before returning it.
    private DefaultSampledSpectrum() {
    }

    // Takes ownership of y without copying. Only for arrays freshly allocated in this class
    // that nothing else references.
    private void adopt(double[] y, double xStart, double xInterval) {
        adoptAt(y, xStart, 0, xInterval);
    }

    private void adoptAt(double[] y, double xOrigin, int xOffset, double xInterval) {
        _y = y;
        _xOrigin = xOrigin;
        _xOffset = xOffset;
        _xInterval = xInterval;
        _xStart = getX(0);
        _xEnd = getX(_y.length - 1);
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
        return _xOrigin + (_xOffset + index) * _xInterval;
    }

    /**
     * @return y value at specified x using linear interpolation.
     * Silently returns zero if x is out of spectrum range.
     */
    @Override public double getY(double x) {
        if (x < getStart() || x > getEnd()) return 0;
        if (x == getEnd()) return getY(getLength() - 1);
        int low_index = getLowerIndex(x);
        // x within rounding of the last sample can land on the last index and leave no upper neighbour
        if (low_index >= getLength() - 1) return getY(getLength() - 1);
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
        final int i = (int) ((x - _xOrigin) / _xInterval) - _xOffset;
        // arithmetic on the untrimmed origin can round a point on the first sample to the one before it
        return (i < 0 && x >= getStart()) ? 0 : i;
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
        for (int i = 0; i < getLength(); ++i) {
            _y[i] = _y[i] * getX(i);
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
     * Rescales X axis by specified factor.  Doesn't change the number of samples.
     */
    public void rescaleX(double factor) {
        Log.fine(String.format("Rescaling X by %.5f", factor));
        if (factor == 1.0) return;
        int numIntervals = getLength();
        double sampling = getSampling() * factor;
        Log.fine(String.format("New sampling = %.5f nm", sampling));
        double origin = _xOrigin * factor;
        double[] data = new double[numIntervals];
        double x;
        for (int i = 0; i < numIntervals; ++i) {
            x = (double) (_xOffset + i) * sampling + origin;
            data[i] = getY(x / factor);
        }
        adoptAt(data, origin, _xOffset, sampling);
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
        if (smoothing_element == 1) return;
        int half = smoothing_element / 2;
        double[] _y_temp = new double[_y.length];
        for (int i = 0; i < getLength() - 1; ++i) {
            try {
                if (i + half >= getLength())
                    _y_temp[i] = getAverage(i, getLength() - 1);
                else if (i - half > 0 && smoothing_element % 2 != 0) //if odd
                    _y_temp[i] = getAverage(i - half, i + half);
                else if (i - half > 0) //if even
                    _y_temp[i] = getAverage(i - half + 1, i + half);
            } catch (Exception e) {
                System.out.println("Smooth: " + e.toString());
            }
        }
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
            data[0][i - minXIndex] = getX(i);
            data[1][i - minXIndex] = _y[i];
        }
        return data;
    }

}
