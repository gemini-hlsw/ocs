package edu.gemini.itc.gmos;

import edu.gemini.itc.shared.*;

import java.awt.*;

/**
 * Gmos specification class
 */
public abstract class Gmos extends Instrument {


    /**
     * Related files will be in this subdir of lib
     */
    public static final String INSTR_DIR = "gmos";

    /**
     * Related files will start with this prefix
     */
    public static String INSTR_PREFIX;

    // Instrument reads its configuration from here.
    protected static double WELL_DEPTH;
    protected static double AD_SATURATION;
    protected static double HIGH_GAIN;
    protected static double LOW_GAIN;
    protected static int DETECTOR_PIXELS;

    // Used as a desperate solution when multiple detectors need to be handled differently (See REL-478).
    // For EEV holds the one instance one the Gmos instrument, for Hamamatsu, contains 3 one Gmos instance for
    // each of the three detectors.
    protected Gmos[] _instruments;

    // Keep a reference to the color filter to ask for effective wavelength
    protected Filter _Filter;
    protected GmosGratingOptics _gratingOptics;
    protected Detector _detector;
    protected double _sampling;
    protected String _filterUsed;
    protected String _grating;
    protected String _focalPlaneMask;
    protected CalculationMethod _mode;
    protected double _centralWavelength;
    protected int _spectralBinning;
    protected int _spatialBinning;

    protected boolean _IFUUsed = false;
    protected String _IFUMethod;
    protected double _IFUOffset;
    protected double _IFUMinOffset;
    protected double _IFUMaxOffset;
    protected IFUComponent _IFU;
    protected boolean _IFU_IsSingle = false;

    // These are the limits of observable wavelength with this configuration.
    protected double _observingStart;
    protected double _observingEnd;

    private int _detectorCcdIndex = 0; // 0, 1, or 2 when there are multiple CCDs in the detector

    public Gmos(String FILENAME, String INSTUMENT_PREFIX, int detectorCcdIndex) throws Exception {
        super(INSTR_DIR, FILENAME);
        _detectorCcdIndex = detectorCcdIndex;

        // The instrument data file gives a start/end wavelength for
        // the instrument.  But with a filter in place, the filter
        // transmits wavelengths that are a subset of the original range.

        _observingStart = super.getStart();
        _observingEnd = super.getEnd();
        _sampling = super.getSampling();

        INSTR_PREFIX = INSTUMENT_PREFIX;

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
        if (_grating.equals("none")) return (int) _Filter.getEffectiveWavelength();
        else return (int) _gratingOptics.getEffectiveWavelength();

    }

    public double getGratingResolution() {
        return _gratingOptics.getGratingResolution();
    }

    public String getGrating() {
        return _grating;
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

    public double getObservingStart() {
        return _observingStart;
    }

    public double getObservingEnd() {
        return _observingEnd;
    }


    public double getPixelSize() {
        return super.getPixelSize() * _spatialBinning;
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
        return _spectralBinning;
    }

    public int getSpatialBinning() {
        return _spatialBinning;
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

    public boolean IFU_IsUsed() {
        return _IFUUsed;
    }

    /**
     * The prefix on data file names for this instrument.
     */
    public static String getPrefix() {
        return INSTR_PREFIX;
    }

    //Abstract class for Detector Pixel Transmission  (i.e.  Create Detector gaps)
    public abstract edu.gemini.itc.operation.DetectorsTransmissionVisitor getDetectorTransmision();

    public String toString() {

        String s = "Instrument configuration: \n";
        s += super.opticalComponentsToString();

        if (!_focalPlaneMask.equals(GmosParameters.NO_SLIT))
            s += "<LI> Focal Plane Mask: " + _focalPlaneMask + "\n";
        s += "\n";
        if (_mode.isSpectroscopy())
            s += "<L1> Central Wavelength: " + _centralWavelength + " nm" + "\n";
        s += "Spatial Binning: " + getSpatialBinning() + "\n";
        if (_mode.isSpectroscopy())
            s += "Spectral Binning: " + getSpectralBinning() + "\n";
        s += "Pixel Size in Spatial Direction: " + getPixelSize() + "arcsec\n";
        if (_mode.isSpectroscopy())
            s += "Pixel Size in Spectral Direction: " + getGratingDispersion_nmppix() + "nm\n";
        if (IFU_IsUsed()) {
            s += "IFU is selected,";
            if (_IFU_IsSingle)
                s += "with a single IFU element at " + _IFUOffset + "arcsecs.";
            else
                s += "with mulitple IFU elements arranged from " + _IFUMinOffset + " to " + _IFUMaxOffset + "arcsecs.";
            s += "\n";
        }
        return s;
    }
}
