package edu.gemini.wdba.tcc;

import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.gemini.gmos.InstGmosCommon;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.telescope.IssPort;

public final class GMOSSupport implements ITccInstrumentSupport {

    private String _wavelength;
    private final ObservationEnvironment _oe;

    // Private constructor
    private GMOSSupport(ObservationEnvironment oe) throws NullPointerException {
        if (oe == null) throw new NullPointerException("observation environment can not be null");
        _oe = oe;
    }

    /**
     * Factory for creating a new GMOS Instrument Support.
     *
     * @param oe The current ObservationEnvironment
     * @return A new instance
     * @throws NullPointerException returned if the ObservationEnvironment is null.
     */
    static public ITccInstrumentSupport create(ObservationEnvironment oe) throws NullPointerException {
        return new GMOSSupport(oe);
    }

    /**
     * Returns the appropriate wavelength for the observation for the TCC config
     *
     * @return wavelength in microns
     */
    public String getWavelength() {
        if (_wavelength != null) return _wavelength;

        InstGmosCommon<?, ?, ?, ?> inst = (InstGmosCommon) _oe.getInstrument();
        if (inst.getDisperser().isMirror()) {
            _wavelength = inst.getFilter().getWavelength();
            // Indicate there is no wavelength choice
            if (_wavelength.equals(TccNames.NONE)) return null;
            return _wavelength;
        }

        // Else it's the central wavelength must convert to microns
        double wavelength = inst.getDisperserLambda();
        _wavelength = String.valueOf(wavelength / 1000.0);
        return _wavelength;
    }

    public String getPositionAngle() {
        InstGmosCommon<?, ?, ?, ?> inst = (InstGmosCommon) _oe.getInstrument();
        return inst.getPosAngleDegreesStr();
    }

    private boolean usesP1() { return _oe.usesP1(); }
    private boolean usesP2() { return _oe.usesP2(); }
    private boolean usesOI() { return _oe.containsTargets(GmosOiwfsGuideProbe.instance); }

    private boolean isIfu() {
        final SPInstObsComp inst = _oe.getInstrument();

        final boolean ifu;
        switch (inst.getInstrument()) {
            case GmosNorth: ifu = ((InstGmosNorth) inst).getFPUnit().isIFU(); break;
            case GmosSouth: ifu = ((InstGmosSouth) inst).getFPUnit().isIFU(); break;
            default:        ifu = false;
        }
        return ifu;
    }

    private boolean usesAO() {
        return _oe.getAoAspect() != ObservationEnvironment.AoAspect.none;
    }

    private Option<Integer> port() {
        final SPInstObsComp inst = _oe.getInstrument();

        final Option<Integer> p;
        switch (inst.getInstrument()) {
            case GmosNorth:
                p = ImOption.apply(((InstGmosNorth) inst).getIssPort() == IssPort.SIDE_LOOKING ? 5 : 1);
                break;

            case GmosSouth:
                p = ImOption.apply(((InstGmosSouth) inst).getIssPort() == IssPort.SIDE_LOOKING ? 3 : 1);
                break;

            default:
                p = ImOption.empty();
        }
        return p;
    }

    public String getTccConfigInstrument() {
        return String.format(
          "%sGMOS%s%s%s%s%s",
          usesAO() ? "AO2" : "",
          isIfu()  ? "IFU" : "",
          port().filter(p -> p != 1).map(Object::toString).getOrElse(""),
          usesP1() ? "_P1" : "",
          usesP2() ? "_P2" : "",
          usesOI() ? "_OI" : ""
        );
    }

    /**
     * Return the gmos guide config name.  Always the same
     */
    public void addGuideDetails(ParamSet guideConfig) {
        if (_oe.containsTargets(GmosOiwfsGuideProbe.instance)) {
            guideConfig.putParameter(TccNames.OIWFSWAVELENGTH, TccNames.GMOS_OIWFS);
        }
    }

    /**
     * Support for instrument origins.
     *
     * @return String that is the name of a TCC config file.  See WDBA-5.
     */
    public String getTccConfigInstrumentOrigin() {
        final String ao;
        switch (_oe.getAoAspect()) {
            case lgs: ao = "lgs2"; break;
            case ngs: ao = "ngs2"; break;
            default:  ao = "";
        }
        return String.format(
          "%sgmos%s%s%s%s",
           ao,
           isIfu() ? "_ifu" : "",
           usesP1() ? "_p1" : "",
           usesP2() ? "_p2" : "",
           usesOI() ? "_oi" : ""
        );
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
