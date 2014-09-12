package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.bhros.InstBHROS;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;

/**
 *
 */
public class BHROSSupport implements ITccInstrumentSupport {
    // private static final Logger LOG = LogUtil.getLogger(BHROSSupport.class);

    private ObservationEnvironment _oe;

    // Private constructor
    private BHROSSupport(ObservationEnvironment oe) throws NullPointerException {
        if (oe == null) throw new NullPointerException("observation environment can not be null");
        _oe = oe;
    }

    /**
     * Factory for creating a new bHROS Instrument Support.
     *
     * @param oe The current ObservationEnvironment
     * @return A new instance
     * @throws NullPointerException returned if the ObservationEnvironment is null.
     */
    static public ITccInstrumentSupport create(ObservationEnvironment oe) throws NullPointerException {
        return new BHROSSupport(oe);
    }

    /**
     * Returns the appropriate wavelength for the observation for the TCC config
     *
     * @return instrument wavelength as a String
     */
    public String getWavelength() {
        InstBHROS inst = (InstBHROS) _oe.getInstrument();

        // The central wavelength is in microns!
        return String.valueOf(inst.getCentralWavelength());
    }

    /**
     * Returns the rotator position angle
     *
     * @return the position angle as a string
     */
    public String getPositionAngle() {
        InstBHROS inst = (InstBHROS) _oe.getInstrument();
        return inst.getPosAngleDegreesStr();
    }

    /**
     * Returns the name of the bHROS instrument config
     *
     * @return the name of the
     */
    public String getTccConfigInstrument() {
        // Always BHROS for now
        // South
        return "BHROS";
    }

    /**
     * Support for instrument origins.
     *
     * @return String that is the name of a TCC config file.  See WDBA-5.
     */
    public String getTccConfigInstrumentOrigin() {
        return "bhros";
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
     * Return the gmos guide config name.  Always the same - this uses GMOS OIWFS so return same as GMOS
     */
    public void addGuideDetails(ParamSet guideConfig) {
        if (_oe.containsTargets(GmosOiwfsGuideProbe.instance)) {
            // don't seem to care whether there is a primary target?
            guideConfig.putParameter(TccNames.OIWFSWAVELENGTH, TccNames.GMOS_OIWFS);
        }
//        if (_oe.isOIWFS()) {
//            guideConfig.putParameter(TccNames.OIWFSWAVELENGTH, TccNames.GMOS_OIWFS);
//        }
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
