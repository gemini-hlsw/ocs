package edu.gemini.wdba.tcc;

/**
 *
 */
public class WavelengthConfig extends ParamSet {

    private String _name;

    public WavelengthConfig(ObservationEnvironment oe) {
        super("");
        if (oe == null) throw new NullPointerException("Config requires a non-null observation environment");
        ITccInstrumentSupport is = oe.getInstrumentSupport();
        _name = is.getWavelength();
    }

    /**
     * The name to be used in the "value" of the field wavelength param
     * @return String that is name
     */
    String getConfigName() {
        return _name;
    }

    /**
     * build will use the <code>(@link TargetEnv}</code> to construct
     * an XML document.
     * @return false if the config is not ready to build.
     */
    public boolean build() {
        // Check for the special case where the instrument has no filter/wavelength
        if (_name == null) return false;

        addAttribute(NAME, _name);
        addAttribute(TYPE, TccNames.WAVELENGTH);

        putParameter(TccNames.WAVEL, _name);
        return true;
    }

}
