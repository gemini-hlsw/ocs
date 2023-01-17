package edu.gemini.itc.base;

import edu.gemini.itc.ghost.Ghost;
import scala.collection.immutable.Map;

import java.util.logging.Logger;

/**
 * Base class for all grating optics elements.
 */
public abstract class GratingOptics extends TransmissionElement implements Disperser {

    private static final Logger log = Logger.getLogger(GratingOptics.class.getName());
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
        //System.out.println("getGratingResolvingPower: "+ gratingName + "  resolvingPower: " + data.apply(gratingName).resolvingPower());
        return data.apply(gratingName).resolvingPower();
    }

    public double getGratingBlaze() {
        //System.out.println("getGratingBlaze, "+ gratingName + "  blaze: " + data.apply(gratingName).blaze());
        return data.apply(gratingName).blaze();
    }

    public double resolutionHalfArcsecSlit() {
        //System.out.println("resolutionHalfArcsecSlit: , "+ gratingName + "  resolution: " + data.apply(gratingName).resolution());
        return data.apply(gratingName).resolution();
    }

    int getX(double v[], double val) throws Exception {
        if (v.length <= 0 ) {
            log.warning("The vector provided does not have data");
            throw new Exception("Vector empty");
        }
        if (v[0] > val) {
            //log.warning("The wavelength requested is lower than the first element of the vector, value: " + val +". The first element of the vector will be returned");
            return 0;
        }
        if (v[v.length-1] < val) {
            log.warning("The wavelength requested is lower than the last element of the vector. " +
                              "The last element of the vector will be returned. wv: "+ val + " lastElement: "+ v[v.length-1]);
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
        System.out.println("wavelength is: "+ val);
        return -1;
    }


    /*
     * The dispersion value is defined in the xxx_grating.dat file. The xxxx is the instrument name.
     * After the ghost implementation this value can be a scalar value or an array. Therefore, the wv
     * parameter represents the wavelength to get the dispersion value (nm/pix). When this method is called
     * with wv equal -1, this method will return the middle value.
     */
    public double dispersion(double wv) {
        if (data.apply(gratingName).dispersionArray() == null) {
            return data.apply(gratingName).dispersion() * _spectralBinning;
        }
        final double[][] data2 =  data.apply(gratingName).dispersionArray();
        if (wv == -1) {
            wv = getStart() + (getEnd() - getStart()) / 2;
            log.warning("FThe wavelength requested is -1. Therefore the dispersion value returned will be the middle of the wv which is: "+ wv + " . wv_start: "+ getStart() + " wv_end: "+ getEnd());
            //wv = getX(middleWV);
            //log.info("middle")
            //return data2[0][i] * _spectralBinning;
        }
        int i = 0;
        try {
            i = getX(data2[0], wv);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return  data2[1][i] * _spectralBinning;
    }

    public String toString() {
        return "Grating Optics: " + gratingName;
    }

}
