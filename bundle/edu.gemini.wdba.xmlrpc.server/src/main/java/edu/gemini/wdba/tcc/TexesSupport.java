package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.texes.InstTexes;

/**
 *
 */
public class TexesSupport implements ITccInstrumentSupport {
    //private static final Logger LOG = LogUtil.getLogger(TexesSupport.class);

    private ObservationEnvironment _oe;

    // Private constructor
    private TexesSupport(ObservationEnvironment oe) {
        if (oe == null) throw new NullPointerException("observation environment can not be null");
        _oe = oe;
    }

    /**
     * Factory for creating a new Texes Instrument Support.
     *
     * @param oe The current ObservationEnvironment
     * @return A new instance
     * @throws NullPointerException returned if the ObservationEnvironment is null.
     */
    static public ITccInstrumentSupport create(ObservationEnvironment oe) throws NullPointerException {
        return new TexesSupport(oe);
    }

    /**
     * Returns the appropriate wavelength for the observation for the TCC config.  In Texes
     * this is the current value of the grating wavelength in microns.
     *
     * @return Appropriate guide wavelength
     */
    public String getWavelength() {
        InstTexes inst = (InstTexes) _oe.getInstrument();
        return Double.toString(inst.getWavelength());
    }

    public String getPositionAngle() {
        InstTexes inst = (InstTexes) _oe.getInstrument();
        return inst.getPosAngleDegreesStr();
    }

    public String getTccConfigInstrument() {
        // Return the name of the TEXES config, currently no imaging
        return "TEXES_SPEC";
    }

    /**
     * Support for instrument origins.
     *
     * @return String that is the name of a TCC config file.
     */
    public String getTccConfigInstrumentOrigin() {
        return "texes";
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
        // Do nothing with Texes.  No OIWFS.
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
