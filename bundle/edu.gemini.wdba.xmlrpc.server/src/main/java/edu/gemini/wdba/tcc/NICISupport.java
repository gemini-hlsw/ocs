package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.nici.InstNICI;
import static edu.gemini.spModel.gemini.nici.NICIParams.CassRotator;
import static edu.gemini.spModel.gemini.nici.NICIParams.FocalPlaneMask;
import edu.gemini.spModel.gemini.nici.NiciOiwfsGuideProbe;
import edu.gemini.spModel.telescope.IssPort;

/**
 *
 */
public class NICISupport implements ITccInstrumentSupport {

    // Should be set somehow for different telescope configurations (property?)
    //private static final int NIRI_PORT = 3;

    private ObservationEnvironment _oe;

    // Private constructor
    private NICISupport(ObservationEnvironment oe) throws NullPointerException {
        if (oe == null) throw new NullPointerException("Observation environment can not be null");
        _oe = oe;
    }

    /**
     * Factory for creating a new NICI Instrument Support.
     *
     * @param oe The current ObservationEnvironment
     * @return A new instance
     * @throws NullPointerException returned if the ObservationEnvironment is null.
     */
    static public ITccInstrumentSupport create(ObservationEnvironment oe) throws NullPointerException {
        return new NICISupport(oe);
    }

    /**
     * Returns the appropriate wavelength for the observation for the TCC config
     *
     * @return wavelenth in microns
     */
    public String getWavelength() {
        InstNICI nici = (InstNICI) _oe.getInstrument();
        return String.valueOf(nici.getCentralWavelength());
    }

    /**
     * Return the position angle.
     *
     * @return a String value for the position angle
     */
    public String getPositionAngle() {
        InstNICI inst = (InstNICI) _oe.getInstrument();
        return inst.getPosAngleDegreesStr();
    }

    /**
     * Return the niri wavelength if needed name.
     */
    public void addGuideDetails(ParamSet p) {
        if (_oe.containsTargets(NiciOiwfsGuideProbe.instance)) {
            p.putParameter(TccNames.OIWFSWAVELENGTH, "NICI OIWFS");
        }
    }

    // This routine performs what is requested in SCT-231/WDBA-35
    private String _makeConfigName(FocalPlaneMask mask) {
        String smask = mask.displayValue();
        // Remove all blanks
        smask = smask.replaceAll(" ", "");
        return "NICI_" + smask;
    }

    private String _makeInstOrigin(FocalPlaneMask mask) {
        String result = _makeConfigName(mask);
        return result.toLowerCase();
    }

    /**
     * Return the proper instrument config
     * Now updated for SCT-269/WDBA-40
     *
     * @return a String value for the config name
     */
    public String getTccConfigInstrument() {
        InstNICI inst = (InstNICI) _oe.getInstrument();
        return (inst.getIssPort() == IssPort.UP_LOOKING) ? "NICI" : "NICI5";
    }

    /**
     * Return true if the instrument is using a fixed rotator position.  In this case the pos angle is used
     * in a special rotator config
     *
     * @return String value that is the name of the fixed rotator config or null if no special name is needed
     */
    public String getFixedRotatorConfigName() {
        InstNICI inst = (InstNICI) _oe.getInstrument();
        CassRotator rotator = inst.getCassRotator();
        return rotator == CassRotator.FIXED ? "NICIFixed" : null;
    }

    /**
     * Support for instrument origins.
     *
     * @return String that is the name of a TCC config file.  See WDBA-5.
     */
    public String getTccConfigInstrumentOrigin() {
        InstNICI inst = (InstNICI) _oe.getInstrument();

        FocalPlaneMask mask = inst.getFocalPlaneMask();
        return _makeInstOrigin(mask);
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
