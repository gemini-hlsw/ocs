package edu.gemini.itc.niri;

import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.itc.shared.CalculationMethod;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.gemini.niri.Niri.ReadMode;
import edu.gemini.spModel.gemini.niri.Niri.Mask;
import edu.gemini.spModel.gemini.niri.Niri.WellDepth;
import edu.gemini.spModel.gemini.niri.Niri.Disperser;

/**
 * Niri specification class
 */
public class Niri extends Instrument {
    /**
     * Related files will be in this subdir of lib
     */
    public static final String INSTR_DIR = "niri";

    /**
     * Related files will start with this prefix
     */
    public static final String INSTR_PREFIX = "";

    // Instrument reads its configuration from here.
    private static final String FILENAME = "niri" + getSuffix();

    private static final double LOW_BACK_WELL_DEPTH = 200000.0;
    private static final double HIGH_BACK_WELL_DEPTH = 280000.0; //updated Feb 13. 2001

    private static final double LOW_BACK_READ_NOISE = 12.0;
    private static final double MED_BACK_READ_NOISE = 35.0;
    private static final double HIGH_BACK_READ_NOISE = 70.0;

    private final NiriParameters params;

    // _Filter2 is used only in the case that the PK50 filter is required
    private Filter _Filter, _Filter2;
    private GrismOptics _grismOptics;
    private CalculationMethod _mode;

    /**
     * construct an Niri with specified Broadband filter or Narrowband filter.
     * grism, and camera type.
     */
    public Niri(final NiriParameters np, final ObservationDetails odp) {
        super(INSTR_DIR, FILENAME);

        this.params = np;

        _mode = odp.getMethod();


        _Filter = Filter.fromFile(getPrefix(), np.getFilter().name(), getDirectory() + "/");
        addFilter(_Filter);

        switch (np.getFilter()) {
            case BBF_Y:
                //The PK50 filter is used with many NIRI narrow-band filters but has
                //not been included until now (20100105).  Most of NIRI's filter curves don't
                //extend far enough for it to matter.
                //To do this right we should have full transmission curves for all filters
                //that are used with the PK50 and include them all.
                _Filter2 = Filter.fromFile(getPrefix(), "PK50-fake", getDirectory() + "/");
                addComponent(_Filter2); // TODO: SHOULD THIS ONE LIMIT WAVELENGTHS, TOO?????
        }

        FixedOptics test = new FixedOptics(getDirectory() + "/", getPrefix());
        addComponent(test);

        // F32_PV is only meant to be used for engineering, not supported in ITC
        switch (np.getCamera()) {
            case F32_PV:
                throw new RuntimeException("ITC does not support the " + np.getCamera().displayValue() + " camera.");
        }


        //Test to see that all conditions for Spectroscopy are met
        if (_mode.isSpectroscopy()) {
            if (np.getGrism() == Disperser.NONE)
                throw new RuntimeException("Spectroscopy calculation method is selected but a grism" +
                        " is not.\nPlease select a grism and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");

            if (np.getFocalPlaneMask() == Mask.MASK_IMAGING)
                throw new RuntimeException("Spectroscopy calculation method is selected but a focal" +
                        " plane mask is not.\nPlease select a " +
                        "grism and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");

            switch (np.getCamera()) {
                case F14:
                    throw new RuntimeException("The " + np.getCamera().displayValue() + " camera cannot be used in Spectroscopy");
                case F32:
                    throw new RuntimeException("ITC does currently not support the " + np.getCamera().displayValue() + " camera in Spectroscopy.");
            }


            _grismOptics = new GrismOptics(getDirectory() + "/", np.getGrism().name()+"-grism", np.getCamera().name(),
                    getFPMaskOffset(),
                    getStringSlitWidth());

            resetBackGround(INSTR_DIR, "spec_");  //Niri has spectroscopic scattering from grisms
            addGrism(_grismOptics);
        }

        if (_mode.isImaging()) {
            if (np.getGrism() != Disperser.NONE)
                throw new RuntimeException("Imaging calculation method is selected but a grism" +
                        " is also selected.\nPlease deselect the " +
                        "grism or change the method to spectroscopy.");
            if (np.getFocalPlaneMask() != Mask.MASK_IMAGING)
                throw new RuntimeException("Imaging calculation method is selected but a Focal" +
                        " Plane Mask is also selected.\nPlease " +
                        "deselect the Focal Plane Mask" +
                        " or change the method to spectroscopy.");
        }


        switch (np.getCamera()) {
            case F6:    addComponent(new F6Optics(getDirectory() + "/"));  break;
            case F14:   addComponent(new F14Optics(getDirectory() + "/")); break;
            case F32:   addComponent(new F32Optics(getDirectory() + "/")); break;
            default: throw new Error();
        }


        addComponent(new Detector(getDirectory() + "/", getPrefix(), "detector", "1024x1024-pixel ALADDIN InSb array"));

    }

    /**
     * Returns the effective observing wavelength.
     * This is properly calculated as a flux-weighted averate of
     * observed spectrum.  So this may be temporary.
     *
     * @return Effective wavelength in nm
     */
    public int getEffectiveWavelength() {
        switch (params.getGrism()) {
            case NONE:   return (int) _Filter.getEffectiveWavelength();
            default:     return (int) _grismOptics.getEffectiveWavelength();
        }
    }

    public double getGrismResolution() {
        return _grismOptics.getGrismResolution();
    }

    public double getReadNoise() {
        switch (params.getReadMode()) {
            case IMAG_SPEC_NB:      return LOW_BACK_READ_NOISE;
            case IMAG_1TO25:        return MED_BACK_READ_NOISE;
            case IMAG_SPEC_3TO5:    return HIGH_BACK_READ_NOISE;
            default:                throw new Error();
        }
    }

    public ReadMode getReadMode() {
        return params.getReadMode();
    }

    public Mask getFocalPlaneMask() {
        return params.getFocalPlaneMask();
    }

    /**
     * Returns the subdirectory where this instrument's data files are.
     */
    public String getDirectory() {
        return ITCConstants.LIB + "/" + INSTR_DIR;
    }

    // TODO: This mimics the result of the original convoluted code.
    // TODO: Verify with science and use unified getObservingStart() method from the base class?
    // TODO: Left as is for now to keep regression tests working.
    public double getObservingStart() {
        switch (params.getGrism()) {
            case NONE:  return _Filter.getStart();
            default:    return Math.max(_Filter.getStart(), _grismOptics.getStart());
        }
    }

    // TODO: This mimics the result of the original convoluted code.
    // TODO: Verify with science and use unified getObservingStart() method from the base class?
    // TODO: Left as is for now to keep regression tests working.
    public double getObservingEnd() {
        switch (params.getGrism()) {
            case NONE:  return _Filter.getEnd();
            default:    return Math.min(_Filter.getEnd(), _grismOptics.getEnd());
        }
    }

    public double getPixelSize() {
        switch (params.getCamera()) {
            case F6:  return super.getPixelSize();
            case F14: return 0.05;
            case F32: return 0.022;
            default:  throw new Error();
        }
    }

    public double getSpectralPixelWidth() {
        return _grismOptics.getPixelWidth();
    }

    public double getWellDepthValue() {
        switch (params.getWellDepth()) {
            case SHALLOW:   return LOW_BACK_WELL_DEPTH;
            case DEEP:      return HIGH_BACK_WELL_DEPTH;
            default:        throw new Error();
        }
    }

    public WellDepth getWellDepth() {
        return params.getWellDepth();
    }

    public double getFPMask() {
        // TODO: use size values provided by masks, this will make an update of baseline necessary
        switch (params.getFocalPlaneMask()) {
            case MASK_1:        // f6 2pix center
            case MASK_4:        // f6 2pix blue
                return 0.23;
            case MASK_2:        // f6 4pix center
                return 0.47;
            case MASK_5:        // f6 4pix blue
                return 0.46;
            case MASK_3:        // f6 6pix center
                return 0.75;
            case MASK_6:        // f6 6pix blue
                return 0.7;
            default:
                throw new Error();
        }

    }

    private String getFPMaskOffset() {
        switch (params.getFocalPlaneMask()) {
            case MASK_1:
            case MASK_2:
            case MASK_3:
                return "center";
            case MASK_4:
            case MASK_5:
            case MASK_6:
                return "blue";
            default:
                throw new Error();
        }
    }

    private String getStringSlitWidth() {
        // TODO: use size values provided by masks, this will make an update of baseline necessary
        switch (params.getFocalPlaneMask()) {
            case MASK_1:        // f6 2pix center
            case MASK_4:        // f6 2pix blue
                return "023";
            case MASK_2:        // f6 4pix center
            case MASK_5:        // f6 4pix blue
                return "046";
            case MASK_3:        // f6 6pix center
            case MASK_6:        // f6 6pix blue
                return "070";
            default:
                throw new Error();
        }
    }

    /**
     * The prefix on data file names for this instrument.
     */
    public static String getPrefix() {
        return INSTR_PREFIX;
    }

}
