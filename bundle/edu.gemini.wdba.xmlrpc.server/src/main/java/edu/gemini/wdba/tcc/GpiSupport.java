package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.gpi.Gpi;

/**
 * TCC support for Gpi
 */
public class GpiSupport implements ITccInstrumentSupport {
    private ObservationEnvironment _oe;

    private GpiSupport(ObservationEnvironment oe) {
        if (oe == null) throw new IllegalArgumentException("Observation environment can not be null");
        _oe = oe;
    }

    /**
     * Factory for creating a new Gpi Instrument Support.
     *
     * @param oe The current ObservationEnvironment
     * @return A new instance
     */
    static public ITccInstrumentSupport create(ObservationEnvironment oe) {
        return new GpiSupport(oe);
    }

    @Override
    public String getWavelength() {
        return "0.806";
    }

    @Override
    public String getPositionAngle() {
        Gpi inst = (Gpi) _oe.getInstrument();
        return inst.getPosAngleDegreesStr();
    }

    @Override
    public String getTccConfigInstrument() {
        return "GPI";
    }

    @Override
    public String getTccConfigInstrumentOrigin() {
        Gpi inst = (Gpi) _oe.getInstrument();
        return (inst.getAdc() == Gpi.Adc.IN) ? "gpi_adc" : "gpi";
    }

    @Override
    public String getFixedRotatorConfigName() {
        return "GPIFixed";
    }

    @Override
    public String getChopState() {
        return TccNames.NOCHOP;
    }

    @Override
    public void addGuideDetails(ParamSet p) {
    }
}
