package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.gmos.InstGmosCommon;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.telescope.IssPort;

public final class GMOSSupport implements ITccInstrumentSupport {

    private String _wavelength;
    private ObservationEnvironment _oe;

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

    public String getTccConfigInstrument() {
        final String side = _oe.isNorth() ? "5" : "3";
        final String port = (((InstGmosCommon) _oe.getInstrument()).getIssPort() == IssPort.UP_LOOKING) ? "" : side;

        if (_oe.isNorth() && _oe.isAltair()) {
            final String oi = _oe.containsTargets(GmosOiwfsGuideProbe.instance) ? "_OI" : "";
            final String p1 = _oe.containsTargets(PwfsGuideProbe.pwfs1) ? "_P1" : "";
            return "AO2GMOS" + port + p1 + oi;
        } else {
            final String p2 = (_oe.containsTargets(PwfsGuideProbe.pwfs2)) ? "_P2" : "";
            return "GMOS" + port + p2;
        }
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
        final InstGmosCommon<?, ?, ?, ?> inst = (InstGmosCommon) _oe.getInstrument();
        return (inst instanceof InstGmosNorth) ? northOrigin(inst) : southOrigin(inst);
    }

    private static final String NGS   = "ngs2gmos";
    private static final String LGS   = "lgs2gmos";
    private static final String NO_AO = "gmos";

    private String oi() {
        return _oe.containsTargets(GmosOiwfsGuideProbe.instance) ? "_oi" : "";
    }

    private String p1() {
        return _oe.containsTargets(PwfsGuideProbe.pwfs1) ? "_p1" : "";
    }

    private static String ifu(InstGmosCommon<?, ?, ?, ?> inst) {
        return inst.getFPUnit().isIFU() ? "_ifu" : "";
    }

    private String northOrigin(InstGmosCommon<?, ?, ?, ?> gmos) {
        switch (_oe.getAoAspect()) {
            case ngs: return NGS   + oi();
            case lgs: return LGS   + p1() + oi();
            default:  return NO_AO + ifu(gmos);
        }
    }

    private String southOrigin(InstGmosCommon<?, ?, ?, ?> gmos) {
        switch (_oe.getAoAspect()) {
            case ngs : return NGS;
            case lgs : return LGS   + p1();
            default:   return NO_AO + ifu(gmos);
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
