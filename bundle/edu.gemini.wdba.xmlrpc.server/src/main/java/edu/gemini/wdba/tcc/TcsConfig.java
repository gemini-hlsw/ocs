package edu.gemini.wdba.tcc;

/**
 *
 */
public class TcsConfig extends ParamSet {

    private ObservationEnvironment _oe;

    public TcsConfig(ObservationEnvironment oe) {
        super("");
        if (oe == null) throw new NullPointerException("Config requires a non-null observation environment");
        // First add the special parameter pointing to the base
        addAttribute(NAME, oe.getObservationTitle());
        addAttribute(TYPE, TccNames.TCS_CONFIGURATION);
        _oe = oe;
    }

    /**
     * build will use the <code>(@link TargetEnv}</code> to construct
     * an XML document.
     */
    public boolean build() {
        putParameter(TccNames.FIELD, _oe.getBasePositionName());
        putParameter(TccNames.SLEWOPTIONS, TccNames.NORMAL);
        ITccInstrumentSupport is = _oe.getInstrumentSupport();
        putParameter(TccNames.POINT_ORIG, is.getTccConfigInstrumentOrigin());
        putParameter(TccNames.INSTRUMENT, is.getTccConfigInstrument());
        String chopState = is.getChopState();
        if (chopState != null) {
            putParameter(TccNames.CHOP, chopState);
        }
        return true;
    }
}
