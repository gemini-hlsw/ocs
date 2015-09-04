package edu.gemini.itc.base;

import edu.gemini.spModel.core.Wavelength;
import edu.gemini.spModel.target.EmissionLine;
import squants.radio.Irradiance;

/**
 * This class creates a EmissionLine spectrum over the interval defined by the
 * blocking filter.  The code comes from PPuxley's Mathcad Demo
 * This Class implements Visitable sampled specturm to create the sed.
 */

public final class EmissionLineSpectrum implements VisitableSampledSpectrum {

    private final DefaultSampledSpectrum _spectrum;

    // Private c'tor to support clone()
    private EmissionLineSpectrum(final DefaultSampledSpectrum spectrum) {
        _spectrum = spectrum;
    }

    public EmissionLineSpectrum(final Wavelength wavelength, final double width, final Irradiance flux,
                                final EmissionLine.Continuum continuum, final double z, final double interval) {

        //shift start and end depending on redshift
        final double start = 300 / (1 + z); //ancient, potentially obsolete comment for start value: 0.2*_wavelength;//.8
        final double end = 30000 / (1 + z); //ancient, potentially obsolete comment for end value  : 1.8*_wavelength;//1.2

        final int n = (int) ((end - start) / interval + 1);
        final double[] fluxArray = new double[n];

        // convert values to internal units
        final double _wavelength    = wavelength.toNanometers();
        final double _flux          = flux.toWattsPerSquareMeter() * _wavelength / 1.988e-16;
        final double _continuumFlux = continuum.toWatts() * _wavelength / 1.988e-13;

        // calculate sigma
        final double sigma = width * _wavelength / 7.05e5;
        int i = 0;
        for (double lam = start; lam <= end; lam += interval) {
            fluxArray[i] = _elineFlux(lam, sigma, _flux, _continuumFlux, _wavelength);
            i++;
        }

        _spectrum = new DefaultSampledSpectrum(fluxArray, start, interval);

    }


    private double _elineFlux(final double lambda, final double sigma, final double flux,
                              final double continuumFlux, final double wave) {
        //this funtion will calculate the eline spectum for a given wavelen
        // and sigme (specified by the user. The flux is just the line flux
        // of the object in question.  The units are returned internal units.
        // That is good so we dont have to do any thing to the result.

        double returnFlux;

        returnFlux = (1 / (java.lang.Math.sqrt(2 * java.lang.Math.PI) * sigma) *
                java.lang.Math.exp(-java.lang.Math.pow(lambda - wave, 2) / (2 *
                        java.lang.Math.pow(sigma, 2))));
        returnFlux = returnFlux * flux + continuumFlux;

        return returnFlux;
    }


    //Implements the clonable interface
    public Object clone() {
        final DefaultSampledSpectrum spectrum =
                (DefaultSampledSpectrum) _spectrum.clone();
        return new EmissionLineSpectrum(spectrum);
    }

    public void trim(final double startWavelength, final double endWavelength) {
        _spectrum.trim(startWavelength, endWavelength);
    }

    public void reset(final double[] s, final double v, final double r) {
        _spectrum.reset(s, v, r);
    }

    public void accept(final SampledSpectrumVisitor v) {
        _spectrum.accept(v);
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
        return _spectrum.getValues();
    }

    /**
     * @return starting x
     */
    public double getStart() {
        return _spectrum.getStart();
    }

    /**
     * @return ending x
     */
    public double getEnd() {
        return _spectrum.getEnd();
    }

    /**
     * @return x sample size (bin size)
     */
    public double getSampling() {
        return _spectrum.getSampling();
    }

    /**
     * @return flux value in specified bin
     */
    public double getY(final int index) {
        return _spectrum.getY(index);
    }

    /**
     * @return x of specified bin
     */
    public double getX(final int index) {
        return _spectrum.getX(index);
    }


    /**
     * @return y value at specified x using linear interpolation.
     * Silently returns zero if x is out of spectrum range.
     */
    public double getY(final double x) {
        return _spectrum.getY(x);
    }


    /**
     * Returns the index of the data point with largest x value less than x
     */
    public int getLowerIndex(final double x) {
        return _spectrum.getLowerIndex(x);
    }


    /**
     * @return number of bins in the histogram (number of data points)
     */
    public int getLength() {
        return _spectrum.getLength();
    }


    //**********************
    // Mutators
    //


    public void applyWavelengthCorrection() {
        _spectrum.applyWavelengthCorrection();
    }

    /**
     * Sets y value in specified x bin.
     * If specified bin is out of range, this is a no-op.
     */
    public void setY(final int bin, final double y) {
        _spectrum.setY(bin, y);
    }

    /**
     * Rescales X axis by specified factor. Doesn't change sampling size.
     */
    public void rescaleX(final double factor) {
        _spectrum.rescaleX(factor);
    }


    /**
     * Rescales Y axis by specified factor.
     */
    public void rescaleY(final double factor) {
        _spectrum.rescaleY(factor);
    }

    public void smoothY(final int factor) {
        _spectrum.smoothY(factor);
    }

    /**
     * Returns the sum of all the y values in the SampledSpectrum
     */
    public double getSum() {
        return _spectrum.getSum();
    }

    /**
     * Returns the integral of all the y values in the SampledSpectrum
     */
    public double getIntegral() {
        return _spectrum.getIntegral();
    }


    /**
     * Returns the average of all the y values in the SampledSpectrum
     */
    public double getAverage() {
        return _spectrum.getAverage();
    }


    /**
     * Returns the sum of y values in the spectrum in
     * the specified index range.
     */
    public double getSum(final int startIndex, final int endIndex) {
        return _spectrum.getSum(startIndex, endIndex);
    }


    /**
     * Returns the sum of y values in the spectrum in
     * the specified range.
     */
    public double getSum(final double x_start, final double x_end) {
        return _spectrum.getSum(x_start, x_end);
    }

    /**
     * Returns the average of values in the SampledSpectrum in
     * the specified range.
     */
    public double getAverage(final double x_start, final double x_end) {
        return _spectrum.getAverage(x_start, x_end);

    }

    /**
     * This returns a 2d array of the data used to chart the SampledSpectrum
     * using JClass Chart.  The array has the following dimensions
     * double data[][] = new double[2][getLength()];
     * data[0][i] = x values
     * data[1][i] = y values
     */
    public double[][] getData() {
        return _spectrum.getData();

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
    public double[][] getData(final int maxXIndex) {
        return _spectrum.getData(maxXIndex);
    }

    /**
     * This returns a 2d array of the data used to chart the SampledSpectrum
     * using JClass Chart.  The array has the following dimensions
     * double data[][] = new double[2][getLength()];
     * data[0][i] = x values
     * data[1][i] = y values
     *
     * @param minXIndex data is returned from the minimum specified x bin
     * @param maxXIndex data is returned up to maximum specified x bin
     */
    public double[][] getData(final int minXIndex, final int maxXIndex) {
        return _spectrum.getData(minXIndex, maxXIndex);
    }

    public String toString() {
        return _spectrum.toString();
    }

    public String printSpecAsString() {
        return _spectrum.printSpecAsString();
    }

    public String printSpecAsString(final int firstIndex, final int lastIndex) {
        return _spectrum.printSpecAsString(firstIndex, lastIndex);
    }

}
