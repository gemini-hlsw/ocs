package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.phoenix.InstPhoenix;
import edu.gemini.spModel.gemini.phoenix.PhoenixParams;

/**
 *
 */
public class PhoenixSupport implements ITccInstrumentSupport {
    //private static final Logger LOG = LogUtil.getLogger(PhoenixSupport.class);

    private ObservationEnvironment _oe;

    // Private constructor
    private PhoenixSupport(ObservationEnvironment oe) {
        if (oe == null) throw new NullPointerException("observation environment can not be null");
        _oe = oe;
    }

    /**
     * Factory for creating a new Phoenix Instrument Support.
     *
     * @param oe The current ObservationEnvironment
     * @return A new instance
     * @throws NullPointerException returned if the ObservationEnvironment is null.
     */
    static public ITccInstrumentSupport create(ObservationEnvironment oe) throws NullPointerException {
        return new PhoenixSupport(oe);
    }

    /**
     * Returns the appropriate wavelength for the observation for the TCC config.  In Phoenix
     * this is the current value of the grating wavelength in microns.
     *
     * @return Appropriate guide wavelength
     */
    public String getWavelength() {
        InstPhoenix inst = (InstPhoenix) _oe.getInstrument();
        return inst.getGratingWavelengthAsString();
    }

    public String getPositionAngle() {
        InstPhoenix inst = (InstPhoenix) _oe.getInstrument();
        return inst.getPosAngleDegreesStr();
    }

    public String getTccConfigInstrument() {
        // Return the name of the PHOENIX config
        InstPhoenix inst = (InstPhoenix) _oe.getInstrument();
        PhoenixParams.Filter f = inst.getFilter();
        // See WDBA-37 - J
        return f == PhoenixParams.Filter.J9232 ? "PHOENIX4_J" : "PHOENIX4";
    }

    /**
     * Support for instrument origins.
     *
     * @return String that is the name of a TCC config file.  See WDBA-5.
     */
    public String getTccConfigInstrumentOrigin() {
        return "phoenix";
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
        // Do nothing with Phoenix.  No OIWFS.
    }

    /**
     * Returns the TCC chop parameter value.
     *
     * @return Chop value or null if there is no chop parameter for this instrument.
     */
    public String getChopState() {
        return TccNames.NOCHOP;
    }
}
