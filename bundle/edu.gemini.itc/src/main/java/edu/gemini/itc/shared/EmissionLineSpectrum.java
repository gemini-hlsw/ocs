package edu.gemini.itc.shared;

import edu.gemini.itc.service.SourceDefinition;

/**
 * This class creates a EmissionLine spectrum over the interval defined by the
 * blocking filter.  The code comes from PPuxley's Mathcad Demo
 * This Class implements Visitable sampled specturm to create the sed.
 */

public class EmissionLineSpectrum implements VisitableSampledSpectrum {
    private DefaultSampledSpectrum _spectrum;

    // Private c'tor to support clone()
    private EmissionLineSpectrum(DefaultSampledSpectrum spectrum) {
        _spectrum = spectrum;
    }

    public EmissionLineSpectrum(double wavelength, double width, double flux,
                                double continuumFlux, String lineFluxUnits,
                                String continuumFluxUnits, double z, double interval) {
        double _flux;
        // convert the wavelenght to Nanometers.
        double _wavelength = wavelength * 1000;
        double _sampling = interval;  //0.2
        double lambda;  // var to be used to construct sed

        double start = 300;//0.2*_wavelength;//.8
        double end = 30000;//1.8*_wavelength;//1.2
        //System.out.println("Start: "+ start +"End: " +end);

        //shift start and end depending on redshift
        start /= (1 + z);
        end /= (1 + z);

        int n = (int) ((end - start) / _sampling + 1);
        double[] fluxArray = new double[n];
//System.out.println("Array: " + (n+40) + " sample "+ _sampling);
        // convert the Units into internal units
        if (lineFluxUnits.equals(SourceDefinition.WATTS_FLUX))
            flux = flux * _wavelength / 1.988e-16;
        else flux = flux * _wavelength / 1.988e-13;

        if (continuumFluxUnits.equals(SourceDefinition.WATTS))
            continuumFlux = continuumFlux * _wavelength / 1.988e-13;
        else continuumFlux = continuumFlux * _wavelength / 1.988e-14;

        // calculate sigma

        double sigma = width * _wavelength / 7.05e5;


        int i = 0;

        for (double lam = start; lam <= end; lam += _sampling) {
            fluxArray[i] = _elineFlux(lam, sigma, flux, continuumFlux,
                    _wavelength);
            i++;

        }

        _spectrum = new DefaultSampledSpectrum(fluxArray, start, _sampling);

        //_spectrum.print();

    }


    private double _elineFlux(double lambda, double sigma, double flux,
                              double continuumFlux, double wave) {
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
        DefaultSampledSpectrum spectrum =
                (DefaultSampledSpectrum) _spectrum.clone();
        return new EmissionLineSpectrum(spectrum);
    }

    public void trim(double startWavelength, double endWavelength) {
        _spectrum.trim(startWavelength, endWavelength);
    }

    public void reset(double[] s, double v, double r) {
        _spectrum.reset(s, v, r);
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


    public void applyWavelengthCorrection() {
        _spectrum.applyWavelengthCorrection();
    }

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
     * @param minXIndex data is returned from the minimum specified x bin
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
