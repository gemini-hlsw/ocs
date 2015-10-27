package edu.gemini.wdba.tcc;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.telescope.IssPort;

/**
 *
 */
public class Flamingos2Support implements ITccInstrumentSupport {
    //private static final Logger LOG = LogUtil.getLogger(TRECSSupport.class);

    // Set to ten microns
    static private String DEFAULT_WAVELENGTH = "10.0";
    private ObservationEnvironment _oe;

    // Private constructor
    private Flamingos2Support(ObservationEnvironment oe) {
        if (oe == null) throw new NullPointerException("observation environment can not be null");
        _oe = oe;
    }

    /**
     * Factory for creating a new Flamingos2 Instrument Support.
     *
     * @param oe The current ObservationEnvironment
     * @return A new instance
     * @throws NullPointerException returned if the ObservationEnvironment is null.
     */
    static public ITccInstrumentSupport create(ObservationEnvironment oe) throws NullPointerException {
        return new Flamingos2Support(oe);
    }

    /**
     * Returns the appropriate wavelength for the observation for the TCC config
     *
     * @return Appropriate guide wavelength
     */
    public String getWavelength() {
        Flamingos2 inst = (Flamingos2) _oe.getInstrument();
        Option<Double> wavelength = inst.getObservingWavelength();
        if (wavelength.isEmpty()) return DEFAULT_WAVELENGTH;
        return Double.toString(wavelength.getValue());
    }

    /**
     * Returns the position angle from the instrument data object.
     *
     * @return An angle in degrees.
     */
    public String getPositionAngle() {
        Flamingos2 inst = (Flamingos2) _oe.getInstrument();
        return inst.getPosAngleDegreesStr();
    }

    public String getTccConfigInstrument() {
        Flamingos2 inst = (Flamingos2) _oe.getInstrument();
        if (_oe.containsTargets(PwfsGuideProbe.pwfs2)) {
            return (inst.getIssPort() == IssPort.SIDE_LOOKING) ? "F25_P2" : "F2_P2";
        } else {
            return (inst.getIssPort() == IssPort.SIDE_LOOKING) ? "F25" : "F2";
        }
    }

    /**
     * Support for instrument origins.
     *
     * @return String that is the name of a TCC config file.  See WDBA-5.
     */
    public String getTccConfigInstrumentOrigin() {
        // Updates for SCI-0289-a.
        switch (_oe.getAoAspect()) {
            case lgs : return "lgs2f2";
            default  : return "f2";
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
        // For ir we check the guide config to see if there is a P2WFS if so, set it to nod.
        // Doing same as other IR instruments for now
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
        return TccNames.NOCHOP;
    }
}
