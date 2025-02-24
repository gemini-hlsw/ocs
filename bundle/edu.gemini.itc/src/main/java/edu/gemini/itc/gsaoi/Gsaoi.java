package edu.gemini.itc.gsaoi;

import edu.gemini.itc.base.*;
import edu.gemini.itc.shared.GsaoiParameters;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.spModel.core.Site;

import java.util.ArrayList;
import java.util.List;

/**
 * Gsaoi specification class
 */
public final class Gsaoi extends Instrument {

    // values are taken from instrument's web documentation
    private static final double WellDepth      = 126000;
    private static final double LinearityLimit = 96000;


    /**
     * Related files will be in this subdir of lib
     */
    public static final String INSTR_DIR = "gsaoi";

    /**
     * Related files will start with this prefix
     */
    public static final String INSTR_PREFIX = "";

    // Instrument reads its configuration from here.
    private static final String FILENAME = "gsaoi" + getSuffix();

    // ITC-2:
    // Please use the following read noises:
    // Bright object: 28e-
    // Faint object: 13e-
    // Very faint object: 10e-
    private static final double BRIGHT_OBJECTS__READ_NOISE = 28.0; // e-
    private static final double FAINT_OBJECTS_READ_NOISE = 13.0;
    private static final double VERY_FAINT_OBJECTS_READ_NOISE = 10.0;

    private final Filter _filter;
    private final GsaoiParameters params;

    /**
     * construct an Gsaoi with specified Broadband filter or Narrowband filter
     * and camera type.
     */
    public Gsaoi(final GsaoiParameters np, final ObservationDetails odp) {
        super(Site.GS, Bands.NEAR_IR, INSTR_DIR, FILENAME);

        this.params = np;
        _filter = Filter.fromFile(getPrefix(), np.filter().name(), getDirectory() + "/");
        addFilter(_filter);

        FixedOptics test = new FixedOptics(getDirectory() + "/", getPrefix());
        addComponent(test);

        addComponent(new Camera(getDirectory() + "/"));

        addComponent(new Detector(getDirectory() + "/", getPrefix(), "detector",
                "2048x2048 HAWAII-2RG HgCdTe"));
    }

    /**
     * Returns the effective observing wavelength.
     * This is properly calculated as a flux-weighted averate of
     * observed spectrum.  So this may be temporary.
     *
     * @return Effective wavelength in nm
     */
    public int getEffectiveWavelength() {
        return (int) _filter.getEffectiveWavelength();

    }

    public double getReadNoise() {
        switch (params.readMode()) {
            case BRIGHT:        return BRIGHT_OBJECTS__READ_NOISE;
            case FAINT:         return FAINT_OBJECTS_READ_NOISE;
            case VERY_FAINT:    return VERY_FAINT_OBJECTS_READ_NOISE;
            default:            throw new Error();
        }
    }

    /**
     * Returns the subdirectory where this instrument's data files are.
     */
    public String getDirectory() {
        return ITCConstants.LIB + "/" + INSTR_DIR;
    }

    public double getPixelSize() {
        return 0.02;
    }

    /**
     * The prefix on data file names for this instrument.
     */
    public static String getPrefix() {
        return INSTR_PREFIX;
    }


    @Override public double wellDepth() {
        return WellDepth;
    }

    @Override public double gain() {
        return 2.4;
    }

    public double maxFlux() {
        return LinearityLimit;
    }

    @Override public double getMinExposureTime() {
        return params.readMode().minExposureTimeSecs();
    }

    @Override public List<WarningRule> warnings() {
        return new ArrayList<WarningRule>() {{
            add(new LinearityLimitRule(LinearityLimit, 0.80));
            add(new SaturationLimitRule(WellDepth, 0.85));
        }};
    }

}
