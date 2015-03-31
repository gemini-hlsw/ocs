package edu.gemini.itc.nifs;

import edu.gemini.itc.shared.CalculationMethod;
import edu.gemini.itc.shared.*;

/**
 * Nifs specification class
 */
public abstract class Nifs extends Instrument {
    /**
     * Related files will be in this subdir of lib
     */
    public static final String INSTR_DIR = "nifs";

    /**
     * Related files will start with this prefix
     */
    public static String INSTR_PREFIX;
    // Instrument reads its configuration from here.
    protected static double WELL_DEPTH;

    protected static double AD_SATURATION;

    protected static double HIGH_GAIN;
    protected static double LOW_GAIN;
    public static int DETECTOR_PIXELS;


    // Keep a reference to the color filter to ask for effective wavelength
    protected Filter _Filter;
    protected NifsGratingOptics _gratingOptics;
    protected Detector _detector;
    protected double _sampling;
    protected String _filterUsed;
    protected String _grating;
    protected String _readNoise;
    protected CalculationMethod _mode;
    protected double _centralWavelength;

    protected String _IFUMethod;
    protected double _IFUOffset;
    protected double _IFUMinOffset;
    protected double _IFUMaxOffset;
    protected int _IFUNumX;
    protected int _IFUNumY;
    protected double _IFUCenterX;
    protected double _IFUCenterY;
    protected IFUComponent _IFU;
    protected boolean _IFU_IsSingle = false;
    protected boolean _IFU_IsSummed = false;

    public Nifs(String FILENAME, String INSTUMENT_PREFIX) {
        super(INSTR_DIR, FILENAME);
        _sampling = super.getSampling();
        INSTR_PREFIX = INSTUMENT_PREFIX;
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


    /**
     * Returns the subdirectory where this instrument's data files are.
     */
    public String getDirectory() {
        return ITCConstants.LIB + "/" + INSTR_DIR;
    }

    public double getPixelSize() {
        return super.getPixelSize();
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

    /**
     * The prefix on data file names for this instrument.
     */
    public static String getPrefix() {
        return INSTR_PREFIX;
    }

    public double getGratingResolution() {
        return _gratingOptics.getGratingResolution();
    }

    public String getGrating() {
        return _grating;
    }

    public double getGratingBlaze() {
        return _gratingOptics.getGratingBlaze();
    }

    public double getGratingDispersion_nm() {
        return _gratingOptics.getGratingDispersion_nm();
    }

    public double getGratingDispersion_nmppix() {
        return _gratingOptics.getGratingDispersion_nmppix();
    }

    //Abstract class for Detector Pixel Transmission  (i.e.  Create Detector gaps)
    public abstract edu.gemini.itc.operation.DetectorsTransmissionVisitor getDetectorTransmision();

    public abstract void setCentralWavelength(double centralWavelength);

    public String toString() {
        //Used to format the strings
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(3);  // Two decimal places
        device.clear();


        String s = "Instrument configuration: \n";
        s += super.opticalComponentsToString();
        s += "<LI>Focal Plane Mask: ifu\n";
        s += "<LI>Read Noise: " + getReadNoise() + "\n";
        s += "<LI>Well Depth: " + getWellDepth() + "\n";
        s += "\n";

        s += "<L1> Central Wavelength: " + _centralWavelength + " nm" + "\n";
        s += "Pixel Size in Spatial Direction: " + getPixelSize() + "arcsec\n";

        s += "Pixel Size in Spectral Direction: " + device.toString(getGratingDispersion_nmppix()) + "nm\n";

        s += "IFU is selected,";
        if (_IFU_IsSingle)
            s += "with a single IFU element at " + _IFUOffset + "arcsecs.";
        else if (_IFU_IsSummed)
            s += "with multiple summed IFU elements arranged in a " + _IFUNumX + "x" + _IFUNumY +
                    " (" + device.toString(_IFUNumX * getIFU().IFU_LEN_X) + "\"x" +
                    device.toString(_IFUNumY * getIFU().IFU_LEN_Y) + "\") grid.";
        else
            s += "with mulitple IFU elements arranged from " + _IFUMinOffset + " to " + _IFUMaxOffset + "arcsecs.";
        s += "\n";

        return s;
    }
}
