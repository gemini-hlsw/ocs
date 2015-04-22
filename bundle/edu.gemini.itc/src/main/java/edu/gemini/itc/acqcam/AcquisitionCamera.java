package edu.gemini.itc.acqcam;

import edu.gemini.itc.shared.*;

/**
 * Aquisition Camera specification class
 */
public class AcquisitionCamera extends Instrument {
    /**
     * Related files will be in this subdir of lib
     */
    public static final String INSTR_DIR = "acqcam";

    /**
     * Related files will start with this prefix
     */
    public static final String INSTR_PREFIX = "";

    // Instrument reads its configuration from here.
    private static final String FILENAME = "acquisition_camera" + getSuffix();

    // Well Depth
    private static final double WELL_DEPTH = 98940.0;


    // Keep a reference to the color filter to ask for effective wavelength
    private Filter _colorFilter;

    /**
     * construct an AcquisitionCamera with specified color filter and ND filter.
     */
    public AcquisitionCamera(final AcquisitionCamParameters params) {
        super(INSTR_DIR, FILENAME);
        _colorFilter = Filter.fromFile(getPrefix(), "colfilt_" + params.colorFilter().name(), getDirectory() + "/");
        addFilter(_colorFilter);
        addComponent(new NDFilterWheel(params.ndFilter(), getDirectory() + "/"));
        addComponent(new FixedOptics(getDirectory() + "/", getPrefix()));
        addComponent(new Detector(getDirectory() + "/", getPrefix(), "detector", "1024x1024 CCD47 Chip"));
    }

    /**
     * Returns the effective observing wavelength.
     * This is properly calculated as a flux-weighted averate of
     * observed spectrum.  So this may be temporary.
     *
     * @return Effective wavelength in nm
     */
    public int getEffectiveWavelength() {
        return (int) _colorFilter.getEffectiveWavelength();
    }

    /**
     * Returns the subdirectory where this instrument's data files are.
     */
    public String getDirectory() {
        return ITCConstants.LIB + "/" + INSTR_DIR;
    }

    /**
     * The prefix on data file names for this instrument.
     */
    public static String getPrefix() {
        return INSTR_PREFIX;
    }

    public double getWellDepth() {
        return WELL_DEPTH;
    }
}
