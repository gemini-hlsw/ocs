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
    protected final DatFile.Grating grating;

    public GratingOptics(final String directory,
                         final String gratingName,
                         final String gratingsName,
                         final double centralWavelength,
                         final int detectorPixels,
                         final int spectralBinning) {

        super(directory + gratingName + Instrument.getSuffix());

        final String file = directory + gratingsName + Instrument.getSuffix();
        this.data = DatFile.gratings().apply(file);
        this.grating = data.apply(gratingName);
        this.gratingName = gratingName;
        this._spectralBinning = spectralBinning;
        this.detectorPixels = detectorPixels;
        this.centralWavelength = centralWavelength;
        Log.fine("spectralBinning = " + _spectralBinning);
    }

    public double getStart() {  // wavelength of first pixel
        double start = centralWavelength - (grating.dispersion() * detectorPixels / 2);
        Log.fine("start = " + start + " nm");
        return start;
    }

    public double getEnd() {
        return centralWavelength + (grating.dispersion() * detectorPixels / 2);
    }

    public double getEffectiveWavelength() {
        return centralWavelength;
    }

    public double getPixelWidth() {
        return grating.dispersion() * _spectralBinning;
    }

    public double getGratingResolvingPower() {
        return grating.resolvingPower();
    }

    public double getGratingBlaze() {
        return grating.blaze();
    }

    public double resolutionHalfArcsecSlit() {
        return grating.resolution();
    }

    // Return the index of the value closest to val. v is ascending, as the dispersion files are.
    int getX(double[] v, double val) throws Exception {
        if (v.length == 0) {
            Log.warning("The vector provided does not have data");
            throw new Exception("Vector empty");
        }
        if (v[0] > val) {
            return 0;
        }
        if (v[v.length-1] < val) {
            return v.length-1;
        }

        // first i with v[i] >= val
        int lo = 0;
        int hi = v.length - 1;
        while (lo < hi) {
            final int mid = (lo + hi) >>> 1;
            if (v[mid] >= val) hi = mid; else lo = mid + 1;
        }
        if (lo - 1 >= 0 && Math.abs(v[lo] - val) > Math.abs(v[lo-1] - val)) {
            return lo - 1;
        }
        return lo;
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
        if (grating.dispersionArray() == null) {
            final double disp =grating.dispersion() * _spectralBinning;
            //Log.fine(String.format("Dispersion = %7.5f nm/pix", disp));
            return disp;
        }
        final double[][] data2 = grating.dispersionArray();

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
