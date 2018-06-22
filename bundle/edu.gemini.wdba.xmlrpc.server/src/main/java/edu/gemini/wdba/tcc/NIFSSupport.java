package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.nifs.NIFSParams;
import edu.gemini.spModel.gemini.nifs.NifsOiwfsGuideProbe;

/**
 *
 */
public class NIFSSupport implements ITccInstrumentSupport {

    // Should be set somehow for different telescope configurations (property?)
    //private static final int NIRI_PORT = 3;

    private ObservationEnvironment _oe;

    // Private constructor
    private NIFSSupport(ObservationEnvironment oe) throws NullPointerException {
        if (oe == null) throw new NullPointerException("Observation environment can not be null");
        _oe = oe;
    }

    /**
     * Factory for creating a new NIFS Instrument Support.
     *
     * @param oe The current ObservationEnvironment
     * @return A new instance
     * @throws NullPointerException returned if the ObservationEnvironment is null.
     */
    static public ITccInstrumentSupport create(ObservationEnvironment oe) throws NullPointerException {
        return new NIFSSupport(oe);
    }

    /**
     * Returns the appropriate wavelength for the observation for the TCC config
     *
     * @return wavelenth in microns
     */
    public String getWavelength() {
        InstNIFS inst = (InstNIFS) _oe.getInstrument();
        NIFSParams.Disperser d = inst.getDisperser();
        NIFSParams.Filter f = inst.getFilter();
        double wl = inst.getCentralWavelength();

        if ((d == null) || (f == null)) {
            return String.valueOf(InstNIFS.DEF_CENTRAL_WAVELENGTH);
        }
        String res = InstNIFS.calcWavelength(d, f, wl);
        if ((res == null) || "".equals(res)) {
            return String.valueOf(InstNIFS.DEF_CENTRAL_WAVELENGTH);
        }
        return res;
    }

    /**
     * Return the position angle.
     *
     * @return a String value for the position angle
     */
    public String getPositionAngle() {
        InstNIFS inst = (InstNIFS) _oe.getInstrument();
        return inst.getPosAngleDegreesStr();
    }

    /**
     * Return the niri wavelength if needed name.
     */
    public void addGuideDetails(ParamSet p) {
        if (_oe.containsTargets(NifsOiwfsGuideProbe.instance)) {
            p.putParameter(TccNames.OIWFSWAVELENGTH, "NIFS OIWFS");
        }
    }

    /**
     * Return the proper instrument config
     *
     * @return a String value for the config name
     */
    public String getTccConfigInstrument() {
        // Changed for WDBA-10
        return _oe.isAltair() ? "AO2NIFS1" : "NIFS";
    }

    /**
     * Support for instrument origins.
     *
     * @return String that is the name of a TCC config file.  See WDBA-5.
     *         Changed for WDBA-19 to account for Altair.
     */
    public String getTccConfigInstrumentOrigin() {
        // Updated for SCI-0289-a.
        switch (_oe.getAoAspect()) {
            case ngs : return "ngs2nifs";
            case lgs : return _oe.adjustInstrumentOriginForLGS_P1("lgs2nifs");
            default  : return "nifs";
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
     * Returns the TCC chop parameter value.
     *
     * @return Chop value or null if there is no chop parameter for this instrument.
     */
    public String getChopState() {
        return TccNames.NOCHOP;
    }
}

