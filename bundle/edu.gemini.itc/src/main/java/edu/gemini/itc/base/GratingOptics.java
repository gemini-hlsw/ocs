package edu.gemini.itc.base;

import edu.gemini.itc.ghost.Ghost;
import scala.collection.immutable.Map;

import java.util.logging.Logger;

/**
 * Base class for all grating optics elements.
 */
public abstract class GratingOptics extends TransmissionElement implements Disperser {

    private static final Logger Log = Logger.getLogger(GratingOptics.class.getName());
    protected final String gratingName;
    protected final double centralWavelength;
    protected final int detectorPixels;
    protected final int _spectralBinning;
    protected final Map<String, DatFile.Grating> data;

    public GratingOptics(final String directory,
                         final String gratingName,
                         final String gratingsName,
                         final double centralWavelength,
                         final int detectorPixels,
                         final int spectralBinning) {

        super(directory + gratingName + Instrument.getSuffix());

        final String file = directory + gratingsName + Instrument.getSuffix();
        this.data = DatFile.gratings().apply(file);
        this.gratingName = gratingName;
        this._spectralBinning = spectralBinning;
        this.detectorPixels = detectorPixels;
        this.centralWavelength = centralWavelength;
        Log.fine("spectralBinning = " + _spectralBinning);
    }

    public double getStart() {
        return centralWavelength - (data.apply(gratingName).dispersion() * detectorPixels / 2);
    }

    public double getEnd() {
        return centralWavelength + (data.apply(gratingName).dispersion() * detectorPixels / 2);
    }

    public double getEffectiveWavelength() {
        return centralWavelength;
    }

    public double getPixelWidth() {
        return data.apply(gratingName).dispersion() * _spectralBinning;

    }

    public double getGratingResolvingPower() {
        return data.apply(gratingName).resolvingPower();
    }

    public double getGratingBlaze() {
        return data.apply(gratingName).blaze();
    }

    public double resolutionHalfArcsecSlit() {
        return data.apply(gratingName).resolution();
    }

    // Return the closest value
    int getX(double[] v, double val) throws Exception {
        if (v.length == 0) {
            Log.warning("The vector provided does not have data");
            throw new Exception("Vector empty");
        }
        if (v[0] > val) {
            //log.warning("The wavelength requested is lower than the first element of the vector, value: " + val +". The first element of the vector will be returned");
            return 0;
        }
        if (v[v.length-1] < val) {
            //Log.warning("The wavelength requested is larger than the last element of the vector. " +
            //                  "The last element of the vector will be returned. wv: "+ val + " lastElement: "+ v[v.length-1]);
            return v.length-1;
        }

        for (int i=0; i < v.length; i++)
            if (v[i] >= val) {
                if (i-1>=0) {
                    if (Math.abs(v[i] - val) > Math.abs(v[i-1] - val) )
                        return i-1;
                }
                return i;
            }
        Log.fine("wavelength is: " + val);
        return -1;
    }

    double getAverage(double[] array) {
        double sum = 0.0;
        for (double a : array) {
            sum += a;
        }
        return sum / array.length;
    }

    /*
     * The grating dispersion is defined in the file <instrument>_grating.dat
     * After the ghost implementation this value may be a scalar or an array as a function of wavelength.
     * When the dispersion is an array, this returns the dispersion nearest to the specified wavelength.
     * When wavelength = -1, this returns the average dispersion.
     */
    public double dispersion(double wavelength) {

        // if an array is not defined then use the scalar value:
        if (data.apply(gratingName).dispersionArray() == null) {
            final double disp =data.apply(gratingName).dispersion() * _spectralBinning;
            Log.fine(String.format("Dispersion = %7.5f nm/pix", disp));
            return disp;
        }
        final double[][] data2 = data.apply(gratingName).dispersionArray();

        if (wavelength == -1) {
            final double disp = getAverage(data2[1]) * _spectralBinning;
            Log.fine(String.format("Average dispersion = %.5f nm/pix", disp));
            return disp;
        }

        // find the value closest in wavelength
        int i = 0;
        try {
            i = getX(data2[0], wavelength);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        final double disp = data2[1][i] * _spectralBinning;
        if ((wavelength % 100) < 0.003) Log.fine(String.format("Dispersion @ %7.3f nm = %7.5f", wavelength, disp));
        return disp;
    }

    public String toString() {
        return "Grating Optics: " + gratingName;
    }

}
