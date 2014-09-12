package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.trecs.InstTReCS;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;

/**
 *
 */
public class TRECSSupport implements ITccInstrumentSupport {
    //private static final Logger LOG = LogUtil.getLogger(TRECSSupport.class);

    // Set to ten microns
    static private String DEFAULT_WAVELENGTH = "10.0";
    private ObservationEnvironment _oe;

    // Private constructor
    private TRECSSupport(ObservationEnvironment oe) {
        if (oe == null) throw new NullPointerException("observation environment can not be null");
        _oe = oe;
    }

    /**
     * Factory for creating a new TReCS Instrument Support.
     *
     * @param oe The current ObservationEnvironment
     * @return A new instance
     * @throws NullPointerException returned if the ObservationEnvironment is null.
     */
    static public ITccInstrumentSupport create(ObservationEnvironment oe) throws NullPointerException {
        return new TRECSSupport(oe);
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
        InstTReCS inst = (InstTReCS) _oe.getInstrument();
        return inst.getPosAngleDegreesStr();
    }

    public String getTccConfigInstrument() {
        // Always TRECS for now
        return "TRECS";
    }

    /**
     * Support for instrument origins.
     *
     * @return String that is the name of a TCC config file.  See WDBA-5.
     */
    public String getTccConfigInstrumentOrigin() {
        return "trecs";
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
        // For TReCS we check the guide config to see if there is a P2WFS if so, set it to nod.
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
