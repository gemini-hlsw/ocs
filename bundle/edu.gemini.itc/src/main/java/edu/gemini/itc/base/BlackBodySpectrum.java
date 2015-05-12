package edu.gemini.itc.base;

import edu.gemini.itc.shared.BrightnessUnit;
import edu.gemini.itc.shared.WavebandDefinition;

/**
 * This class creates a black body spectrum over the interval defined by the
 * blocking filter.  The code comes from Inger Jorgensen and Tom Geballe's
 * paper on IR spectrophotometric calibrations.  This Class implements
 * Visitable sampled specturm to create the sed.
 */
public class BlackBodySpectrum implements VisitableSampledSpectrum {
    private DefaultSampledSpectrum _spectrum;

    // Private c'tor to support clone()
    private BlackBodySpectrum(DefaultSampledSpectrum spectrum) {
        _spectrum = spectrum;
    }

    public BlackBodySpectrum(double temp, double interval, double flux, BrightnessUnit units, WavebandDefinition band, double z) {

        //rescale the start and end depending on the redshift
        final double start = 300 / (1 + z);
        final double end   = 30000 / (1 + z);

        final int n = (int) ((end - start) / interval + 1);
        final double[] fluxArray = new double[n + 40];

        //if units need to be converted do it.
        final double _flux;
        switch (units) {
            case MAG:
            case MAG_PSA:
                _flux = flux;
                break;
            default:
                _flux = _convertToMag(flux, units, band);
        }

        int i = 0;
        for (double wavelength = start; wavelength <= end; wavelength += interval) {
            fluxArray[i] = _blackbodyFlux(wavelength, temp);
            i = i + 1;
        }

        _spectrum = new DefaultSampledSpectrum(fluxArray, start, interval);

        //with blackbody convert W m^2 um^-1 to phot....
        final double zeropoint = ZeroMagnitudeStar.getAverageFlux(band);
        final double phot_norm = zeropoint * (java.lang.Math.pow(10.0, -0.4 * _flux));
        final double average   = _spectrum.getAverage(band.getStart() / (1 + z), band.getEnd() / (1 + z));

        // Calculate multiplier.
        final double multiplier = phot_norm / average;
        _spectrum.rescaleY(multiplier);

    }


    private double _blackbodyFlux(final double lambda, final double temp) {
        //this funtion will calculate the blackbody spectum for a given wavelen
        // and effective temp (specified by the user. The flux is just the mag
        // of the object in question.  The units are returned as W m^-2 um^-1.
        // That is good so we dont have to do any thing to the result.  It will
        // be changed in normalize visitor.

        return (1 / java.lang.Math.pow(lambda / 1000, 4)) * (1 / (java.lang.Math.exp(14387 / (lambda / 1000 * temp)) - 1));

    }


    private double _convertToMag(final double flux, final BrightnessUnit units, final WavebandDefinition band) {
        //THis method should convert the flux into units of magnitude.
        //same code as in NormalizeVisitor.java.  Eventually should come out
        // into a genral purpose conversion class if needs to be used again.
        final double norm;
        //The firstpart converts the units to our internal units.
        switch (units) {
            case JY:                norm = flux * 1.509e7 / band.getCenter();       break;
            case WATTS:             norm = flux * band.getCenter() / 1.988e-13;     break;
            case ERGS_WAVELENGTH:   norm = flux * band.getCenter() / 1.988e-14;     break;
            case ERGS_FREQUENCY:    norm = flux * 1.509e30 / band.getCenter();      break;
            case ABMAG:             norm = 5.632e10 * Math.pow(10, -0.4 * flux) / band.getCenter(); break;
            case MAG_PSA:
                double zeropoint = ZeroMagnitudeStar.getAverageFlux(band);
                norm = zeropoint * (Math.pow(10.0, -0.4 * flux));
                break;
            case JY_PSA:            norm = flux * 1.509e7 / band.getCenter();       break;
            case WATTS_PSA:         norm = flux * band.getCenter() / 1.988e-13;     break;
            case ERGS_WAVELENGTH_PSA: norm = flux * band.getCenter() / 1.988e-14;   break;
            case ERGS_FREQUENCY_PSA:norm = flux * 1.509e30 / band.getCenter();      break;
            case ABMAG_PSA:         norm = 5.632e10 * Math.pow(10, -0.4 * flux) / band.getCenter(); break;
            default:
                throw new IllegalArgumentException("invalid units " + units);
        }


        final double zeropoint = ZeroMagnitudeStar.getAverageFlux(band);
        return -(Math.log(norm / zeropoint) / Math.log(10)) / .4;
    }

    //Implements the clonable interface
    public Object clone() {
        DefaultSampledSpectrum spectrum = (DefaultSampledSpectrum) _spectrum.clone();
        return new BlackBodySpectrum(spectrum);
    }

    public void trim(double startWavelength, double endWavelength) {
        _spectrum.trim(startWavelength, endWavelength);
    }

    public void reset(double[] s, double v, double r) {
        _spectrum.reset(s, v, r);
    }

    public void applyWavelengthCorrection() {
        _spectrum.applyWavelengthCorrection();
    }

    public void accept(SampledSpectrumVisitor v) {
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
    public double getY(int index) {
        return _spectrum.getY(index);
    }

    /**
     * @return x of specified bin
     */
    public double getX(int index) {
        return _spectrum.getX(index);
    }


    /**
     * @return y value at specified x using linear interpolation.
     * Silently returns zero if x is out of spectrum range.
     */
    public double getY(double x) {
        return _spectrum.getY(x);
    }


    /**
     * Returns the index of the data point with largest x value less than x
     */
    public int getLowerIndex(double x) {
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

    /**
     * Sets y value in specified x bin.
     * If specified bin is out of range, this is a no-op.
     */
    public void setY(int bin, double y) {
        _spectrum.setY(bin, y);
    }

    /**
     * Rescales X axis by specified factor. Doesn't change sampling size.
     */
    public void rescaleX(double factor) {
        _spectrum.rescaleX(factor);
    }


    /**
     * Rescales Y axis by specified factor.
     */
    public void rescaleY(double factor) {
        _spectrum.rescaleY(factor);
    }

    public void smoothY(int factor) {
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
    public double getSum(int startIndex, int endIndex) {
        return _spectrum.getSum(startIndex, endIndex);
    }


    /**
     * Returns the sum of y values in the spectrum in
     * the specified range.
     */
    public double getSum(double x_start, double x_end) {
        return _spectrum.getSum(x_start, x_end);
    }

    /**
     * Returns the average of values in the SampledSpectrum in
     * the specified range.
     */
    public double getAverage(double x_start, double x_end) {
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
    public double[][] getData(int maxXIndex) {
        return _spectrum.getData(maxXIndex);

    }

    /**
     * This returns a 2d array of the data used to chart the SampledSpectrum
     * using JClass Chart.  The array has the following dimensions
     * double data[][] = new double[2][getLength()];
     * data[0][i] = x values
     * data[1][i] = y values
     *
     * @param maxXIndex data is returned from minimum specified x bin
     * @param maxXIndex data is returned up to maximum specified x bin
     */
    public double[][] getData(int minXIndex, int maxXIndex) {
        return _spectrum.getData(minXIndex, maxXIndex);

    }

    public String toString() {
        return _spectrum.toString();
    }

    public String printSpecAsString() {
        return _spectrum.printSpecAsString();
    }

    public String printSpecAsString(int firstIndex, int lastIndex) {
        return _spectrum.printSpecAsString(firstIndex, lastIndex);
    }

}
