package edu.gemini.itc.shared;

import edu.gemini.itc.parameters.SourceDefinitionParameters;

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

    public BlackBodySpectrum(double temp, double start, double end,
                             double interval, double flux, String units,
                             String band, double z) throws Exception {
        double _flux;
        double _S;
        // This is to buffer the sed for normailisation.
        //start-=20;
        //end+=20;
        start = 300;
        end = 30000;

        //rescale the start and end depending on the redshift

        start /= ((1 + z));
        end /= ((1 + z));

        int n = (int) ((end - start) / interval + 1);
        double[] fluxArray = new double[n + 40];
        // System.out.println("array:" +(n+40)+ "int"+interval);
        int i = 0;
        //if units need to be converted do it.
        if (units.equals(SourceDefinitionParameters.MAG) || units.equals(
                SourceDefinitionParameters.MAG_PSA))
            _flux = flux;
        else
            _flux = _convertToMag(flux, units, band);
        //System.out.println("flux:"+flux+"   _flux:"+ _flux + " unit " + units + " band " + band);
        // get the scaling factor S

        double band_start = WavebandDefinition.getStart(band) / (1 + z);
        double band_end = WavebandDefinition.getEnd(band) / (1 + z);
        double band_sum = 0;
        int sum_counter = 0;

        _S = _getScalingFactor(band_start, band_end);
        //System.out.println("Scale:"+_S);
        //for (double wavelength=start; wavelength <= end+20; wavelength+=interval)

        for (double wavelength = start; wavelength <= end; wavelength += interval) {
            fluxArray[i] = _blackbodyFlux(wavelength, temp, _flux, _S);//*wavelength;//1.988e-13;
            if (wavelength >= band_start / (1 + z) && wavelength <= band_end / (1 + z)) {
                band_sum = fluxArray[i];
                sum_counter++;

            }
            i = i + 1;
            //System.out.print("i:"+i);

        }

        //for ( i = 1130-300 ; i< 1130-200 ; i++)
        //		System.out.println("flux:"+fluxArray[i]);
        _spectrum = new DefaultSampledSpectrum(fluxArray, start, interval);
        //System.out.println("inte: " + _spectrum.getIntegral()+ "added up:" + t+ " " +t/n);
        //with blackbody convert W m^2 um^-1 to phot....

        double zeropoint = ZeroMagnitudeStar.getAverageFlux(band);
        double phot_norm = zeropoint * (java.lang.Math.pow(10.0, -0.4 * _flux));
        //double watt_norm= phot_norm/WavebandDefinition.getCenter(band)*1.988e-13;
        double norm = phot_norm;//watt_norm * WavebandDefinition.getCenter(band) / 1.988e-13;
        double average = _spectrum.getAverage((double) WavebandDefinition.getStart(band) / (1 + z), (double) WavebandDefinition.getEnd(band) / (1 + z));
        //double average =  band_sum/(sum_counter*2);

        //System.out.println("zero: " +zeropoint+ " photn: "+ phot_norm + " z: " + z+ " average: " + average+ " start: " +WavebandDefinition.getStart(band)+" end: "+WavebandDefinition.getEnd(band)+" " +sum_counter);

        //     for (double wavelength=start; wavelength <= end+20; wavelength+=interval)
        //       {
        //		System.out.println("Value: " + getY(wavelength));
        //}
        // Calculate multiplier.
        double multiplier = norm / average;
        //System.out.println(":"+multiplier);
        // Apply normalization, multiply every value in the SED by
        // multiplier and then its average in specified band will be
        // the required amount.
        //print();
        //System.out.println("Mult: " + multiplier);
        _spectrum.rescaleY(multiplier);
        //print();
        //return _spectrum;

    }


    private double _blackbodyFlux(double lambda, double temp, double flux,
                                  double scaleFactor) {
        //this funtion will calculate the blacbody spectum for a given wavelen
        // and effective temp (specified by the user. The flux is just the mag
        // of the object in question.  The units are returned as W m^-2 um^-1.
        // That is good so we dont have to do any thing to the result.  It will
        // be changed in normalize visitor.

        double returnFlux;

        returnFlux = //scaleFactor* java.lang.Math.pow(10.0,-8)*
                //java.lang.Math.pow(10.0,-0.4*flux)*
                (1 / java.lang.Math.pow(lambda / 1000, 4)) *
                        (1 / (java.lang.Math.exp(14387 / (lambda / 1000 * temp)) - 1));//
        //(2.99792458e8/(lambda/1e6)*6.62618e-34);

        return returnFlux;
    }


    private double _convertToMag(double flux, String units, String band) {
        //THis method should convert the flux into units of magnitude.
        //same code as in NormalizeVisitor.java.  Eventually should come out
        // into a genral purpose conversion class if needs to be used again.
        double norm = -1;
        //The firstpart converts the units to our internal units.
        if (units.equals(SourceDefinitionParameters.JY)) {
            norm = flux * 1.509e7 / WavebandDefinition.getCenter(band);
        } else if (units.equals(SourceDefinitionParameters.WATTS)) {
            norm = flux * WavebandDefinition.getCenter(band) / 1.988e-13;
        } else if (units.equals(SourceDefinitionParameters.ERGS_WAVELENGTH)) {
            norm = flux * WavebandDefinition.getCenter(band) / 1.988e-14;
        } else if (units.equals(SourceDefinitionParameters.ERGS_FREQUENCY)) {
            norm = flux * 1.509e30 / WavebandDefinition.getCenter(band);
        } else if (units.equals(SourceDefinitionParameters.ABMAG)) {
            norm = 5.632e10 * java.lang.Math.pow(10, -0.4 * flux) /
                    WavebandDefinition.getCenter(band);
        } else if (units.equals(SourceDefinitionParameters.MAG_PSA)) {
            double zeropoint = ZeroMagnitudeStar.getAverageFlux(band);
            norm = zeropoint * (java.lang.Math.pow(10.0, -0.4 * flux));
        } else if (units.equals(SourceDefinitionParameters.JY_PSA)) {
            norm = flux * 1.509e7 / WavebandDefinition.getCenter(band);
        } else if (units.equals(SourceDefinitionParameters.WATTS_PSA)) {
            norm = flux * WavebandDefinition.getCenter(band) / 1.988e-13;
        } else if (units.equals(SourceDefinitionParameters.ERGS_WAVELENGTH_PSA)) {
            norm = flux * WavebandDefinition.getCenter(band) / 1.988e-14;
        } else if (units.equals(SourceDefinitionParameters.ERGS_FREQUENCY_PSA)) {
            norm = flux * 1.509e30 / WavebandDefinition.getCenter(band);
        } else if (units.equals(SourceDefinitionParameters.ABMAG_PSA)) {
            norm = 5.632e10 * java.lang.Math.pow(10, -0.4 * flux) /
                    WavebandDefinition.getCenter(band);
        }


        double zeropoint = ZeroMagnitudeStar.getAverageFlux(band);
        return -(java.lang.Math.log(norm / zeropoint) / java.lang.Math.log(10)) / .4;
    }

    private double _getScalingFactor(double start, double end) {
        // this method will return the Scaling factor dermined by the mid pt
        // of the filter. The mid pt should eventually be replaced by the
        // true central wavelen of the filter.

        double midpt = (start / 1000 + end / 1000) / 2;

        if (midpt <= 1.434)
            return 2.1797;
        else if (midpt <= 1.917)
            return 2.1398;
        else if (midpt <= 2.863)
            return 2.0473;
        else if (midpt <= 3.654)
            return 1.9756;
        else if (midpt <= 4.265)
            return 1.9675;
        else
            return 1.9502;
    }


    //Implements the clonable interface
    public Object clone() {
        DefaultSampledSpectrum spectrum =
                (DefaultSampledSpectrum) _spectrum.clone();
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

    public void accept(SampledSpectrumVisitor v) throws Exception {
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
     *
     * @throws Exception If either limit is out of range.
     */
    public double getSum(int startIndex, int endIndex) throws Exception {
        return _spectrum.getSum(startIndex, endIndex);
    }


    /**
     * Returns the sum of y values in the spectrum in
     * the specified range.
     *
     * @throws Exception If either limit is out of range.
     */
    public double getSum(double x_start, double x_end) throws Exception {
        return _spectrum.getSum(x_start, x_end);
    }

    /**
     * Returns the average of values in the SampledSpectrum in
     * the specified range.
     *
     * @throws Exception If either limit is out of range.
     */
    public double getAverage(double x_start, double x_end) throws Exception {
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
