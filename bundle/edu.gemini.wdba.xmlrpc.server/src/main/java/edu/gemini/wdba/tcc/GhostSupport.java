package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.ghost.Ghost;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;

import java.util.Objects;

final public class GhostSupport implements ITccInstrumentSupport {
    final private ObservationEnvironment oe;

    private GhostSupport(final ObservationEnvironment oe) {
        Objects.requireNonNull(oe, "Observation environment can not be null");
        if (!(oe.getInstrument() instanceof Ghost))
            throw new RuntimeException("Incompatible instrument in ObservationEnvironment.");
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
        return "0.655";
    }

    @Override
    public String getPositionAngle() {
        Ghost inst = (Ghost) oe.getInstrument();
        return inst.getPosAngleDegreesStr();
    }

    private boolean usesProbe(GuideProbe gp) {
        return oe.containsTargets(gp);
    }

    private boolean usesP2() {
        return usesProbe(PwfsGuideProbe.pwfs2);
    }

    @Override
    public String getTccConfigInstrument() {
        final String result;

        final String usesP2 = usesP2() ? "_P2" : "";
        switch (oe.getTargetEnvironment().getAsterism().asterismType()) {

            case GhostSingleTarget:
                result = "GHOST_SINGLE_TARGET";
                break;

            case GhostDualTarget:
                result = "GHOST_DUAL_TARGET";
                break;

            case GhostTargetPlusSky:
                result = "GHOST_TARGET_PLUS_SKY";
                break;

            case GhostSkyPlusTarget:
                result = "GHOST_SKY_PLUS_TARGET";
                break;

            case GhostHighResolutionTargetPlusSky:
            case GhostHighResolutionTargetPlusSkyPrv:
                result = "GHOST_HIGH_RESOLUTION";
                break;

            default:
                result = "GHOST";
        }
        return result + usesP2;
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
