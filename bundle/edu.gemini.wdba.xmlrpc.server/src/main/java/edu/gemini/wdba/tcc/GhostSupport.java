package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.ghost.Ghost;

import java.util.Objects;

final public class GhostSupport implements ITccInstrumentSupport {
    final private ObservationEnvironment oe;

    private GhostSupport(final ObservationEnvironment oe) {
        Objects.requireNonNull(oe, "Observation environment can not be null");
        this.oe = oe;
    }

    /**
     * Factor for creating a new GHOST Instrument Support.
     *
     * @param oe The current ObservationEnvironment
     * @return A new instance
     * @throws NullPointerException returned if the ObservationEnvironment is null.
     */
    static public ITccInstrumentSupport create(final ObservationEnvironment oe) throws NullPointerException {
        return new GhostSupport(oe);
    }

    @Override
    public String getWavelength() {
        return null;
    }

    @Override
    public String getPositionAngle() {
        Ghost inst = (Ghost) oe.getInstrument();
        return inst.getPosAngleDegreesStr();
    }

    @Override
    public String getTccConfigInstrument() {
        return "GHOST";
    }

    @Override
    public String getTccConfigInstrumentOrigin() {
        return "ghost";
    }

    @Override
    public String getFixedRotatorConfigName() {
        return null;
    }

    @Override
    public String getChopState() {
        return TccNames.NOCHOP;
    }

    @Override
    public void addGuideDetails(final ParamSet p) {

    }
}
