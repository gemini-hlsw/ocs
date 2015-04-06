package edu.gemini.itc.gnirs;

import edu.gemini.itc.shared.CalculationMethod;
import edu.gemini.itc.shared.*;


/**
 * Gnirs specification class
 */
public abstract class Gnirs extends Instrument {
    /**
     * Related files will be in this subdir of lib
     */
    public static final String INSTR_DIR = "gnirs";

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

    public static double SHORT_CAMERA_PIXEL_SCALE;
    public static double LONG_CAMERA_PIXEL_SCALE;

    private static final double HIGH_BACK_READ_NOISE = 155;    // Old value: 160 (changed 2/27/2014)
    private static final double MEDIUM_BACK_READ_NOISE = 30;   // Old value: 35 (changed 2/27/2014)
    private static final double LOW_BACK_READ_NOISE = 10;      // Old value: 11 (changed 2/27/2014)
    private static final double VERY_LOW_BACK_READ_NOISE = 7;  // Old value: 9 (changed 2/27/2014)


    // Keep a reference to the color filter to ask for effective wavelength
    protected Filter _Filter;
    protected GnirsGratingOptics _gratingOptics;
    protected Detector _detector;
    protected double _sampling;
    protected String _filterUsed;
    protected String _grating;
    protected String _readNoise;
    protected String _focalPlaneMask;
    protected String _stringSlitWidth;
    protected CalculationMethod _mode;
    protected double _centralWavelength;

    public Gnirs(String FILENAME, String INSTUMENT_PREFIX) {
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

    public double getReadNoise() {

        if (_readNoise.equals(GnirsParameters.VERY_LOW_READ_NOISE))
            return VERY_LOW_BACK_READ_NOISE;
        else if (_readNoise.equals(GnirsParameters.LOW_READ_NOISE))
            return LOW_BACK_READ_NOISE;
        else if (_readNoise.equals(GnirsParameters.MED_READ_NOISE))
            return MEDIUM_BACK_READ_NOISE;
        else return HIGH_BACK_READ_NOISE;
    }

    //Abstract class for Detector Pixel Transmission  (i.e.  Create Detector gaps)
    public abstract edu.gemini.itc.operation.DetectorsTransmissionVisitor getDetectorTransmision();

    public abstract boolean XDisp_IsUsed();

    public abstract int getOrder();

    public abstract TransmissionElement getGratingOrderNTransmission(int order);

    public abstract void setCentralWavelength(double centralWavelength);

    public String toString() {
        //Used to format the strings
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(3);  // Two decimal places
        device.clear();


        String s = "Instrument configuration: \n";
        s += super.opticalComponentsToString();

        if (!_focalPlaneMask.equals(GnirsParameters.NO_SLIT))
            s += "<LI>Focal Plane Mask: " + _focalPlaneMask + "\n";

        s += "<LI>Grating: " + _grating + "\n"; // REL-469

        s += "<LI>Read Noise: " + getReadNoise() + "\n";
        s += "<LI>Well Depth: " + getWellDepth() + "\n";
        s += "\n";

        s += "<L1> Central Wavelength: " + _centralWavelength + " nm" + " \n";
        s += "Pixel Size in Spatial Direction: " + getPixelSize() + "arcsec\n";
        if (_mode.isSpectroscopy()) {
            if (XDisp_IsUsed()) {
                s += "Pixel Size in Spectral Direction(Order 3): " + device.toString(getGratingDispersion_nmppix() / 3) + "nm\n";
                s += "Pixel Size in Spectral Direction(Order 4): " + device.toString(getGratingDispersion_nmppix() / 4) + "nm\n";
                s += "Pixel Size in Spectral Direction(Order 5): " + device.toString(getGratingDispersion_nmppix() / 5) + "nm\n";
                s += "Pixel Size in Spectral Direction(Order 6): " + device.toString(getGratingDispersion_nmppix() / 6) + "nm\n";
                s += "Pixel Size in Spectral Direction(Order 7): " + device.toString(getGratingDispersion_nmppix() / 7) + "nm\n";
                s += "Pixel Size in Spectral Direction(Order 8): " + device.toString(getGratingDispersion_nmppix() / 8) + "nm\n";
            } else {
                s += "Pixel Size in Spectral Direction: " + device.toString(getGratingDispersion_nmppix()) + "nm\n";
            }
        }
        return s;
    }
}
