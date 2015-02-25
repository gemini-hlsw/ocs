package edu.gemini.itc.gmos;

import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.shared.*;

import java.awt.*;

/**
 * Gmos specification class
 */
public abstract class Gmos extends Instrument {

    //Plate scales for original and Hamamatsu CCD's (temporary)
    public static final double ORIG_PLATE_SCALE = 0.0727;
    public static final double HAM_PLATE_SCALE = 0.080778;

    protected DetectorsTransmissionVisitor _dtv;

    // Colors to use for charts corresponding to the detectorCcdIndex
    protected static final Color[] DETECTOR_CCD_COLORS = {Color.blue, Color.green, Color.red};

    /**
     * Related files will be in this subdir of lib
     */
    public static final String INSTR_DIR = "gmos";

    // Instrument reads its configuration from here.
    private static final double WELL_DEPTH = 125000.0;
    private static final double AD_SATURATION = 56636;
    private static final double HIGH_GAIN = 4.4;
    private static final double LOW_GAIN = 2.18;
    protected static final int DETECTOR_PIXELS = 6218;

    // Used as a desperate solution when multiple detectors need to be handled differently (See REL-478).
    // For EEV holds the one instance one the Gmos instrument, for Hamamatsu, contains 3 one Gmos instance for
    // each of the three detectors.
    protected Gmos[] _instruments;

    protected final GmosParameters gp;
    protected final ObservationDetailsParameters odp;

    // Keep a reference to the color filter to ask for effective wavelength
    protected Filter _Filter;
    protected IFUComponent _IFU;
    protected GmosGratingOptics _gratingOptics;
    protected Detector _detector;
    protected double _sampling;

    // These are the limits of observable wavelength with this configuration.

    private int _detectorCcdIndex = 0; // 0, 1, or 2 when there are multiple CCDs in the detector

    public Gmos(GmosParameters gp, ObservationDetailsParameters odp, String FILENAME, String prefix, int detectorCcdIndex) throws Exception {
        super(INSTR_DIR, FILENAME);

        this.odp    = odp;
        this.gp     = gp;

        _detectorCcdIndex = detectorCcdIndex;

        _sampling = super.getSampling();

    }

    /**
     * Returns an array containing this instrument, or, if there are multiple detector CCDs,
     * an array containing instances of this instrument with the CCD set differently
     * (Used to implement hamamatsu CCD support).
     */
    public Gmos[] getDetectorCcdInstruments() {
        return _instruments;
    }

    /**
     * Index of current CCD in detector
     *
     * @return 0, 1, or 2 when there are multiple CCDs in the detector (default: 0)
     */
    public int getDetectorCcdIndex() {
        return _detectorCcdIndex;
    }

    /**
     * Returns the name of the detector CCD
     */
    public String getDetectorCcdName() {
        return _detector.getName();
    }

    /**
     * Returns the color to use in plots of the detector CCD
     */
    public Color getDetectorCcdColor() {
        return _detector.getColor();
    }

    /**
     * Returns the effective observing wavelength.
     * This is properly calculated as a flux-weighted averate of
     * observed spectrum.  So this may be temporary.
     *
     * @return Effective wavelength in nm
     */
    public int getEffectiveWavelength() {
        if (gp.getGrating().equals("none")) return (int) _Filter.getEffectiveWavelength();
        else return (int) _gratingOptics.getEffectiveWavelength();

    }

    public double getGratingResolution() {
        return _gratingOptics.getGratingResolution();
    }

    public String getGrating() {
        return gp.getGrating();
    }

    public double getGratingDispersion_nm() {
        return _gratingOptics.getGratingDispersion_nm();
    }

    public double getGratingDispersion_nmppix() {
        return _gratingOptics.getGratingDispersion_nmppix();
    }


    /**
     * Returns the subdirectory where this instrument's data files are.
     */
    public String getDirectory() {
        return ITCConstants.LIB + "/" + INSTR_DIR;
    }

    public double getPixelSize() {
        return super.getPixelSize() * gp.getSpatialBinning();
    }


    public double getSpectralPixelWidth() {
        return _gratingOptics.getPixelWidth();
    }

    public double getWellDepth() {
        return WELL_DEPTH;
    }

    public double getSampling() {
        return _sampling;
    }

    public int getSpectralBinning() {
        return gp.getSpectralBinning();
    }

    public int getSpatialBinning() {
        return gp.getSpatialBinning();
    }

    public double getADSaturation() {
        return AD_SATURATION;
    }

    public double getHighGain() {
        return HIGH_GAIN;
    }

    public double getLowGain() {
        return LOW_GAIN;
    }

    public IFUComponent getIFU() {
        return _IFU;
    }

    public boolean isIfuUsed() {
        return gp.getFocalPlaneMask().equals(GmosParameters.IFU);
    }

    //Abstract class for Detector Pixel Transmission  (i.e.  Create Detector gaps)
    public DetectorsTransmissionVisitor getDetectorTransmision() {
        return _dtv;
    }

    public String toString() {

        String s = "Instrument configuration: \n";
        s += super.opticalComponentsToString();

        if (!gp.getFocalPlaneMask().equals(GmosParameters.NO_SLIT))
            s += "<LI> Focal Plane Mask: " + gp.getFocalPlaneMask() + "\n";
        s += "\n";
        if (odp.getMethod().isSpectroscopy())
            s += "<L1> Central Wavelength: " + gp.getCentralWavelength() + " nm" + "\n";
        s += "Spatial Binning: " + getSpatialBinning() + "\n";
        if (odp.getMethod().isSpectroscopy())
            s += "Spectral Binning: " + getSpectralBinning() + "\n";
        s += "Pixel Size in Spatial Direction: " + getPixelSize() + "arcsec\n";
        if (odp.getMethod().isSpectroscopy())
            s += "Pixel Size in Spectral Direction: " + getGratingDispersion_nmppix() + "nm\n";
        if (isIfuUsed()) {
            s += "IFU is selected,";
            if (gp.getIFUMethod().equals(GmosParameters.SINGLE_IFU))
                s += "with a single IFU element at " + gp.getIFUOffset() + "arcsecs.";
            else
                s += "with mulitple IFU elements arranged from " + gp.getIFUMinOffset() + " to " + gp.getIFUMaxOffset() + "arcsecs.";
            s += "\n";
        }
        return s;
    }
}
