package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.igrins2.Igrins2;

import java.util.Objects;

final public class Igrins2Support implements ITccInstrumentSupport {
    final private ObservationEnvironment oe;

    private Igrins2Support(final ObservationEnvironment oe) {
        Objects.requireNonNull(oe, "Observation environment can not be null");
        if (!(oe.getInstrument() instanceof Igrins2))
            throw new RuntimeException("Incompatible instrument in ObservationEnvironment.");
        this.oe = oe;
    }

    /**
     * Factory for creating a new Igrins2 Instrument Support.
     *
     * @param oe The current ObservationEnvironment
     * @return A new instance
     * @throws NullPointerException returned if the ObservationEnvironment is null.
     */
    static public ITccInstrumentSupport create(final ObservationEnvironment oe) throws NullPointerException {
        return new Igrins2Support(oe);
    }

    @Override
    public String getWavelength() {
        // in microns
        return "2.15";
    }

    @Override
    public String getPositionAngle() {
        Igrins2 inst = (Igrins2) oe.getInstrument();
        return inst.getPosAngleDegreesStr();
    }

    @Override
    public String getTccConfigInstrument() {
        return "IGRINS2";
    }

    @Override
    public String getTccConfigInstrumentOrigin() {
        return "igrins2";
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
