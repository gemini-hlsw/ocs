package edu.gemini.itc.acqcam;

import edu.gemini.itc.base.*;
import edu.gemini.itc.shared.AcquisitionCamParameters;
import edu.gemini.spModel.core.Site;

import java.util.ArrayList;
import java.util.List;

/**
 * Aquisition Camera specification class
 */
public class AcquisitionCamera extends Instrument {

    // value taken from instrument's web documentation
    private final static double WellDepth = 98940;


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

    // Keep a reference to the color filter to ask for effective wavelength
    private Filter _colorFilter;

    /**
     * construct an AcquisitionCamera with specified color filter and ND filter.
     */
    public AcquisitionCamera(final AcquisitionCamParameters params) {
        super(Site.GN, Bands.VISIBLE, INSTR_DIR, FILENAME);
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

    public double maxFlux() {
        return WellDepth;
    }

    @Override public double wellDepth() {
        return WellDepth;
    }

    @Override public double gain() {
        // TODO: the correct value is 0.71 for GN and 1.01 for GS
        // Currently we don't know which acq cam (GN / GS) we are talking about, so for now
        // Andy suggested to just use the GS value. In the future we might want to improve this.
        return 1.01;
    }

    @Override public List<WarningRule> warnings() {
        return new ArrayList<WarningRule>() {{
            add(new SaturationLimitRule(WellDepth, 0.80));
        }};
    }
}
