package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.gmos.InstGmosCommon;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.telescope.IssPort;

public class GMOSSupport implements ITccInstrumentSupport {

    private String _wavelength;
    private ObservationEnvironment _oe;

    // Private constructor
    private GMOSSupport(ObservationEnvironment oe) throws NullPointerException {
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
        String side = _oe.isNorth() ? "5" : "3";
        String port = (((InstGmosCommon) _oe.getInstrument()).getIssPort() == IssPort.UP_LOOKING) ? "" : side;
        String ao   = (_oe.isNorth() && _oe.isAltair()) ? "AO2" : "";
        String p2   = (_oe.containsTargets(PwfsGuideProbe.pwfs2)) ? "_P2" : "";
        return ao + "GMOS" + port + p2;
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
        InstGmosCommon<?, ?, ?, ?> inst = (InstGmosCommon) _oe.getInstrument();
        switch (_oe.getAoAspect()) {
            case ngs : return "ngs2gmos";
            case lgs : return _oe.adjustInstrumentOriginForLGS_P1("lgs2gmos");
            default:
                return inst.getFPUnit().isIFU() ? "gmos_ifu" : "gmos";
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
