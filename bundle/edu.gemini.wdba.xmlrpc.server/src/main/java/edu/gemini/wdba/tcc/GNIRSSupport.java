package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.gemini.gnirs.GnirsOiwfsGuideProbe;
import edu.gemini.spModel.telescope.IssPort;

/**
 *
 */
public class GNIRSSupport implements ITccInstrumentSupport {
    //private static final Logger LOG = LogUtil.getLogger(GNIRSSupport.class);

    // TODO: determine correct port number
    public static final int GNIRS_SIDE_PORT = 3;

    private ObservationEnvironment _oe;

    // Private constructor
    private GNIRSSupport(ObservationEnvironment oe) {
        if (oe == null) throw new NullPointerException("observation environment can not be null");
        _oe = oe;
    }

    /**
     * Factory for creating a new GNIRS Instrument Support.
     *
     * @param oe The current ObservationEnvironment
     * @return A new instance
     * @throws NullPointerException returned if the ObservationEnvironment is null.
     */
    static public ITccInstrumentSupport create(ObservationEnvironment oe) throws NullPointerException {
        return new GNIRSSupport(oe);
    }

    /**
     * Returns the appropriate wavelength for the observation for the TCC config.  In GNIRS
     * this is the current value of the disperser wavelength in microns.
     *
     * @return Appropriate guide wavelength
     */
    public String getWavelength() {
        InstGNIRS inst = (InstGNIRS) _oe.getInstrument();
        return inst.getCentralWavelength().getStringValue();
    }

    public String getPositionAngle() {
        InstGNIRS inst = (InstGNIRS) _oe.getInstrument();
        return inst.getPosAngleDegreesStr();
    }

    private String getPortFreeTccConfigInstrument() {
        // Return the name of the GNIRS config
        // SCI-0163: WDBA entries for GNIRS+Altair
        // Updated for 2011A testing:
        // "However, the instrument loaded for gnirs is ngs or lgs2gnirs, rather
        // than ao2gnirs. This is the correct syntax for the instrument origin,
        // but not for the instrument parameter.  Tested 11/8 by Jesse Ball"
        switch (_oe.getAoAspect()) {
            case ngs:
            case lgs: return "AO2GNIRS";
            default : return "GNIRS";
        }
    }

    private String getPortSuffix() {
        String val = "";
        if (((InstGNIRS) _oe.getInstrument()).getIssPort() == IssPort.SIDE_LOOKING) {
            val = String.valueOf(GNIRS_SIDE_PORT);
        }
        return val;
    }

    public String getTccConfigInstrument() {
        return getPortFreeTccConfigInstrument() + getPortSuffix();
    }

    /**
     * Support for instrument origins.
     *
     * @return String that is the name of a TCC config file.  See WDBA-5.
     */
    public String getTccConfigInstrumentOrigin() {
        // Updates for SCI-0289-a
        switch (_oe.getAoAspect()) {
            case ngs : return "ngs2gnirs";
            case lgs : return _oe.adjustInstrumentOriginForLGS_P1("lgs2gnirs");
            default  : return "gnirs";
        }
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
        if (_oe.containsTargets(GnirsOiwfsGuideProbe.instance)) {
            guideConfig.putParameter(TccNames.OIWFSWAVELENGTH, "GNIRS OIWFS");
        }
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
