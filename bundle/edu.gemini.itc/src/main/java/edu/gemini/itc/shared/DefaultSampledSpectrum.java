// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.shared;

import java.text.NumberFormat;
import java.text.DecimalFormat;

import java.util.List;
import java.util.ArrayList;

/**
 * Default implementation of SampledSpectrum interface.
 * This implementation internally has a uniformly-spaced data points.
 * A SampledSpectrum can conceptually be thought of as a 2-D spectrum
 * (data points on axes of real numbers) where the data points occur
 * at regular intervals on the x axis.
 * This interface provides functionality for defining and manipulating a
 * SampledSpectrum.
 * Units are not stored for either axis so the client must know from context.
 * The SampledSpectrum plays the rold of Element in a visitor pattern.
 * This pattern is used to separate operations from the SampledSpectrum
 * elements.
 * The SampledSpectrum plays the rold of Element in a visitor pattern.
 * This pattern is used to separate operations from the elements.
 * Because of this separation, Concrete Elements must offer enough
 * accessors for the separate Concrete Visitor class to perform the
 * manipulation.
 * DefaultSampledSpectrum plays the role of a Concrete Element.
 */
public class DefaultSampledSpectrum implements VisitableSampledSpectrum {
    private double[] _y; //Array containing flux values in relative units

    //Values of start and end points
    private double _xStart, _xEnd;

    private double _xInterval;     //Size of each particluar element

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
     * @param sp Spectrum to sample
     * @param xInterval Sampling interval
     */
    public DefaultSampledSpectrum(ArraySpectrum sp, double xInterval) {
        double xStart = sp.getStart();
        double xEnd = sp.getEnd();
        int numIntervals = (int) ((xEnd - xStart) / xInterval);
        double[] data = new double[numIntervals + 1];
        for (int i = 0; i <= numIntervals; ++i) {
            data[i] = sp.getY(i * xInterval + xStart);
        }
        reset(data, xStart, xInterval);
    }

    /**
     * Implements the Cloneable interface.
     */
    public Object clone() {
        double[] data = new double[getLength()];
        System.arraycopy(getValues(), 0, data, 0, getLength());
        return new DefaultSampledSpectrum(data, getStart(), getSampling());
    }
    
    public void trim(double newStart, double newEnd) {
        if (newStart < getStart()) { newStart = getStart();} 
        if (newEnd > getEnd()) { newEnd = getEnd();} 
        if (newEnd < getStart() || newStart > getEnd()) { return; }
        double[] data = new double[new Double((newEnd-newStart)/_xInterval).intValue()+4];
        //System.out.println("os: " + getStart() + "ns: " + newStart + " oe: " + getEnd() + " ne: " + newEnd);
        //System.out.println("startpos: " + new Double((newStart-getStart())/_xInterval).intValue() + "length: " + getLength() + " copylength: " + new Double((newEnd-newStart)/_xInterval).intValue());
        
        System.arraycopy(getValues(), new Double((newStart-getStart())/_xInterval).intValue(), data, 0, new Double((newEnd-newStart)/_xInterval).intValue());
        reset(data , newStart, _xInterval);
    }
        
    
    /**
     * Sets all these SampledSpectrum parameters.
     * I don't like a method that sets everything at once, but I inherited
     * this code so I will leave it.
     */
    public void reset(double[] y, double xStart,
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
     *
     * Example:
     *
     * SampledSpectrum s = SampledSpectrumFactory.getSampledSpectrum("SampledSpectrumFILE");
     * SampledSpectrumVisitor r = new Resample();
     * s.Accept(r);
     *
     */
    public void accept(SampledSpectrumVisitor v) throws Exception {
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
    public double[] getValues() {
        return _y;
    }

    /** @return starting x */
    public double getStart() {
        return _xStart;
    }

    /** @return ending x */
    public double getEnd() {
        return _xEnd;
    }

    /** @return x sample size (bin size) */
    public double getSampling() {
        return _xInterval;
    }

    /** @return flux value in specified bin */
    public double getY(int index) {
        return _y[index];
    }

    /** @return x of specified bin */
    public double getX(int index) {
        return getStart() + index * getSampling();
    }

    /**
     * @return y value at specified x using linear interpolation.
     * Silently returns zero if x is out of spectrum range.
     */
    public double getY(double x) {
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

    /** Returns the index of the data point with largest x value less than x */
    public int getLowerIndex(double x) {
        return (int) ((x - getStart()) / getSampling());
    }

    /** @return number of bins in the histogram (number of data points) */
    public int getLength() {
        return _y.length;
    }


    //**********************
    // Mutators
    //


    public void applyWavelengthCorrection() {
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
    public void setY(int bin, double y) {
        if (bin < 0 || bin >= getLength()) return;  // no-op
        _y[bin] = y;
    }

    /** Rescales X axis by specified factor. Doesn't change sampling size. */
    public void rescaleX(double factor) {
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

    /** Rescales Y axis by specified factor. */
    public void rescaleY(double factor) {
        if (factor == 1.0) return;
        for (int i = 0; i < getLength(); ++i) {
            _y[i] *= factor;
        }
    }

    public void smoothY(int smoothing_element) {
        double[] _y_temp;
        _y_temp = new double[_y.length];

        //System.out.println("Smoothing: " + smoothing_element);
        if (smoothing_element == 1.0) return;
        //System.out.print("Start:");
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

    /** Returns the sum of all the y values in the SampledSpectrum */
    public double getSum() {
        double sum = 0;
        try {
            sum = getSum(0, getLength() - 1);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return sum;
    }

    /** Returns the integral of all the y values in the SampledSpectrum */
    public double getIntegral() {
        double integral = 0;
        try {
            integral = getIntegral(getStart(), getEnd());
        } catch (Exception e) {
        }
        return integral;
    }

    /** Returns the average of all the y values in the SampledSpectrum */
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

        double sum = 0.0;
        for (int i = startIndex; i <= endIndex; ++i) {
            sum += getY(i);
            //System.out.print(""+new Double(getY(i)).intValue()+":s"+startIndex+":e"+endIndex+":i"+i+":");
        }
        return sum;
    }

    /**
     * Returns the sum of y values in the spectrum in
     * the specified range.
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
     * Returns the integral of values in the SampledSpectrum in the
     * specified range between specified indices.
     */
    public double getIntegral(int start_index, int end_index) throws Exception {
        if (start_index < 0 || start_index >= getLength() ||
                end_index < 0 || end_index >= getLength()) {
            throw new Exception("Integral out of bounds: integrating from index "
                                + start_index + " to " + end_index
                                + " for spectra of length " + getLength());
        }

        if (start_index == end_index) {
            return 0.0; // REL-478
        }

        boolean negative = false;
        if (start_index > end_index) {
            int temp = start_index;
            start_index = end_index;
            end_index = temp;
            negative = true;
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

        return (negative) ? -area : area;
    }

    /**
     * Returns the average of values in the SampledSpectrum in
     * the specified range.
     * @throws Exception If either limit is out of range.
     */
    public double getAverage(double x_start, double x_end) throws Exception {
	//System.out.println("X start/end: "+x_start+" "+x_end);
	//for (double i = x_start; i<x_end; i++)
	//    System.out.println("(AVERAGE) X val: "+ i +" Y val: "+ getY(i));
        //System.out.println( "int" + getIntegral(x_start,x_end) + "points:"+ (x_end - x_start));
	//double total=0;
	//for (double i=x_start; i <=x_end; i++){
	//    total = total+getY(i);

	//    System.out.println("Averaging: X: "+i+" Y: "+getY(i));
	//}
	//System.out.println("Average: "+total);
   
        return getIntegral(x_start, x_end) / (x_end - x_start);
    }

    /**
     * Returns the average of values in the SampledSpectrum in
     * the specified range.
     * @throws Exception If either limit is out of range.
     */
    public double getAverage(int indexStart, int indexEnd) throws Exception {
        return getIntegral(indexStart, indexEnd) /
                (getX(indexEnd) - getX(indexStart));
    }
    
    private int getFirstNonZeroPosition() {
        for (int i = 0; i < getLength(); ++i) {
            if (_y[i] != 0)
                return i;
        }
        return 0;
    }
    
    private int getLastNonZeroPosition() {
        for (int i = getLength()-1; i >= 0; --i) {
            if (_y[i] != 0)
                return i;
        }
        return getLength();
    }


    /**
     * This returns a 2d array of the data used to chart the SampledSpectrum
     * using JClass Chart.  The array has the following dimensions
     *    double data[][] = new double[2][getLength()];
     * data[0][i] = x values
     * data[1][i] = y values
     */
    public double[][] getData() {
        return getData(_y.length - 1);  // the whole SampledSpectrum
    }

    /**
     * This returns a 2d array of the data used to chart the SampledSpectrum
     * using JClass Chart.  The array has the following dimensions
     *    double data[][] = new double[2][getLength()];
     * data[0][i] = x values
     * data[1][i] = y values
     *
     * @param maxXIndex data is returned up to maximum specified x bin
     */
    public double[][] getData(int maxXIndex) {
        return getData(0, maxXIndex);
    }

    /**
     * This returns a 2d array of the data used to chart the SampledSpectrum
     * using JClass Chart.  The array has the following dimensions
     *    double data[][] = new double[2][getLength()];
     * data[0][i] = x values
     * data[1][i] = y values
     *
     * @param minXIndex data is returned starts at minimum specified x bin
     * @param maxXIndex data is returned up to maximum specified x bin
     */
    public double[][] getData(int minXIndex, int maxXIndex) {
        if (maxXIndex >= _y.length) maxXIndex = _y.length - 1;
        if (minXIndex < 0) maxXIndex = 0;
        double data[][] = new double[2][maxXIndex-minXIndex+1];
        for (int i = minXIndex; i <= maxXIndex; i++) {
            data[0][i-minXIndex] = getStart() + i * getSampling();
            data[1][i-minXIndex] = _y[i];
        }
        return data;
    }

    public String toString() {
        String s = "Spectrum - start: " + getStart() + " end: " + getEnd()
                + " sampling: " + getSampling() + " length: " + getLength();
        return s;
    }

    public String printSpecAsString() {

        StringBuffer result = new StringBuffer();
        NumberFormat nf = DecimalFormat.getInstance();
        nf.setGroupingUsed(false);
        nf.setMaximumFractionDigits(3);
        nf.setMinimumFractionDigits(3);
        int n = getLastNonZeroPosition();
        for (int i = getFirstNonZeroPosition(); i < n; ++i) {
            result.append(nf.format(getX(i)) + "\t" + nf.format(_y[i]) + "\n");
        }
        return result.toString();
    }

    public String printSpecAsString(int firstIndex, int lastIndex) {

        if (firstIndex >= _y.length) firstIndex = _y.length-1;
        if (lastIndex >= _y.length) lastIndex = _y.length-1;
        StringBuffer result = new StringBuffer();
        NumberFormat nf = DecimalFormat.getInstance();
        nf.setGroupingUsed(false);
        nf.setMaximumFractionDigits(3);
        nf.setMinimumFractionDigits(3);

        for (int i = firstIndex; i <= lastIndex; ++i) {
            result.append(nf.format(getX(i)) + "\t" + nf.format(_y[i]) + "\n");
        }
        return result.toString();
    }

}
