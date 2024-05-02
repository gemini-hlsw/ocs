package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.visitor.VisitorConfig;
import edu.gemini.spModel.gemini.visitor.VisitorInstrument;
import edu.gemini.spModel.gemini.visitor.VisitorPosAngleMode;

/**
 * TCC support for Visitor Instruments
 */
public class VisitorInstrumentSupport implements ITccInstrumentSupport {
    private final ObservationEnvironment _oe;

    private VisitorInstrumentSupport(ObservationEnvironment oe) {
        if (oe == null) throw new IllegalArgumentException("Observation environment can not be null");
        _oe = oe;
    }

    /**
     * Factory for creating a new Visitor Instrument Support.
     */
    static public ITccInstrumentSupport create(ObservationEnvironment oe) {
        return new VisitorInstrumentSupport(oe);
    }

    @Override
    public String getWavelength() {
        VisitorInstrument inst = (VisitorInstrument) _oe.getInstrument();
        return inst.getWavelengthStr();
    }

    @Override
    public String getPositionAngle() {
        VisitorInstrument inst = (VisitorInstrument) _oe.getInstrument();
        return inst.getPosAngleDegreesStr();
    }

    @Override
    public String getTccConfigInstrument() {
        final VisitorInstrument inst = (VisitorInstrument) _oe.getInstrument();
        final VisitorConfig vc = inst.getVisitorConfig();
        final String name = vc.tccInstrumentName();
        return vc.hasPwfsSpecificConfigs() ? _oe.pwfsConfigInstrument(name) : name;
    }

    @Override
    public String getTccConfigInstrumentOrigin() {
        final VisitorInstrument inst = (VisitorInstrument) _oe.getInstrument();
        final VisitorConfig vc = inst.getVisitorConfig();
        final String origin = vc.tccInstrumentOrigin();
        return vc.hasPwfsSpecificConfigs() ? _oe.pwfsConfigInstrumentOrigin(origin) : origin;
    }

    @Override
    public String getFixedRotatorConfigName() {

        final VisitorInstrument inst = (VisitorInstrument) _oe.getInstrument();

        // Assuming fixed here, which theoretically might not be the case for
        // visitors other than MaroonX in the future.  If so, we might need a
        // bit more `VisitorConfig` information.

        return (inst.getVisitorConfig().positionAngleMode() == VisitorPosAngleMode.Fixed0$.MODULE$) ?
                TccNames.FIXED.toLowerCase() : null;

    }

    @Override
    public String getChopState() {
        return TccNames.NOCHOP;
    }

    @Override
    public void addGuideDetails(ParamSet p) {

    }
}
