package edu.gemini.wdba.tcc;

import edu.gemini.spModel.data.YesNoType;
import edu.gemini.spModel.gemini.michelle.InstMichelle;
import edu.gemini.spModel.gemini.michelle.MichelleParams;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;

/**
 *
 */
public class MichelleSupport implements ITccInstrumentSupport {
    //private static final Logger LOG = LogUtil.getLogger(MichelleSupport.class);

    private static final String MICHELLE_SPEC = "MICHELLE_SPEC";
    private static final String MICHELLE = "MICHELLE";
    private static final String MICHELLE_POL = "MICHELLE_POL";
    private static final String MICHELLE_POL_SPEC = "MICHELLE_POL_SPEC";

    // Set to ten microns
    static private String DEFAULT_WAVELENGTH = "10.0";
    private ObservationEnvironment _oe;

    // Private constructor
    private MichelleSupport(ObservationEnvironment oe) {
        if (oe == null) throw new NullPointerException("observation environment can not be null");
        _oe = oe;
    }

    /**
     * Factory for creating a new Michelle Instrument Support.
     *
     * @param oe The current ObservationEnvironment
     * @return A new instance
     * @throws NullPointerException returned if the ObservationEnvironment is null.
     */
    static public ITccInstrumentSupport create(ObservationEnvironment oe) throws NullPointerException {
        return new MichelleSupport(oe);
    }

    /**
     * Returns the appropriate wavelength for the observation for the TCC config
     *
     * @return Appropriate guide wavelength
     */
    public String getWavelength() {
        return DEFAULT_WAVELENGTH;
    }

    /**
     * Returns the position angle from the instrument data object.
     *
     * @return An angle in degrees.
     */
    public String getPositionAngle() {
        InstMichelle inst = (InstMichelle) _oe.getInstrument();
        String otPosAngle = inst.getPosAngleDegreesStr();

        // As in WDBA-27, if in a SPEC mode, return 0 for the pos angle
        return isSpecConfig() ? "0.0" : otPosAngle;
    }

    // Test for spectroscopy by checking against the determined instrument config
    private boolean isSpecConfig() {
        String instConfig = getTccConfigInstrument();
        return (instConfig.equals(MICHELLE_SPEC) || instConfig.equals(MICHELLE_POL_SPEC));
    }

    /**
     * Return the instrument config name for Michelle
     *
     * @return the name of the instrument config stored in TCC
     */
    public String getTccConfigInstrument() {
        // Setup Michelle as in OCS-60
        InstMichelle inst = (InstMichelle) _oe.getInstrument();
        MichelleParams.Disperser disperser = inst.getDisperser();

        // If disperser is not equal to mirror, then do spec with polarimetry or not
        if (!disperser.equals(MichelleParams.Disperser.MIRROR)) {
            return inst.getPolarimetry() == YesNoType.YES ? MICHELLE_POL_SPEC : MICHELLE_SPEC;
        }

        // We are here if the disperser is MIRROR so look at the total on-site time
        // Issue OCS-60 says compare to 1.0 so I will!
        String onSourceTime = inst.getTotalOnSourceTimeAsString();
        // This hack is being updated for WDBA-17
        if (onSourceTime.equals("1.0")) {
            return inst.getPolarimetry() == YesNoType.YES ? MICHELLE_POL_SPEC : MICHELLE_SPEC;
        }
        // In this case, it's MIRROR and not 1.0
        return inst.getPolarimetry() == YesNoType.YES ? MICHELLE_POL : MICHELLE;
    }

    /**
     * Support for instrument origins.
     *
     * @return String that is the name of a TCC config file.  See WDBA-5.
     */
    public String getTccConfigInstrumentOrigin() {
        // WDBA-31
        InstMichelle inst = (InstMichelle) _oe.getInstrument();
        return inst.getPolarimetry() == YesNoType.YES ? "michelle_pol" : "michelle";
    }

    /**
     * Return true if the instrument is using a fixed rotator position.  In this case the pos angle is used
     * in a special rotator config
     *
     * @return String value that is the name of the fixed rotator config or null if no special name is needed
     */
    public String getFixedRotatorConfigName() {
        return null;
    }

    /**
     * Add the OIWFS wavelength.
     */
    public void addGuideDetails(ParamSet guideConfig) {
        // For Michelle we check the guide config to see if there is a P2WFS if so, set it to nod.
        if (_oe.containsTargets(PwfsGuideProbe.pwfs2)) {
            guideConfig.putParameter(TccNames.PWFS2ACTIVE, TccNames.NOD);
        }
    }

    /**
     * Returns the TCC chop parameter value.
     *
     * @return Chop value or null if there is no chop parameter for this instrument.
     */
    public String getChopState() {
        return TccNames.BASICCHOP;
    }
}
