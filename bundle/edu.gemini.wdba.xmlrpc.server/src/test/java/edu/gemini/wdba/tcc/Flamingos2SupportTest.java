package edu.gemini.wdba.tcc;

import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImCollections;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.Pair;
import edu.gemini.spModel.ext.ObservationNode;
import edu.gemini.spModel.ext.TargetNode;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.env.UserTarget;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.telescope.IssPort;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import static edu.gemini.spModel.gemini.flamingos2.Flamingos2.Disperser;
import static edu.gemini.spModel.gemini.flamingos2.Flamingos2.Filter;

/**
 * Test cases for {@link Flamingos2Support}.
 */
public final class Flamingos2SupportTest extends InstrumentSupportTestBase<Flamingos2> {

    private final SPTarget base;

    public Flamingos2SupportTest() {
        super(Flamingos2.SP_TYPE);

        base = new SPTarget();
        base.setName("Base Pos");
    }

    private static GuideProbeTargets createGuideTargets(final GuideProbe probe) {
        final SPTarget target = new SPTarget();
        return GuideProbeTargets.create(probe, target);
    }


    private TargetEnvironment create(final GuideProbe... probes) {
        final ImList<GuideProbeTargets> gtCollection = createGuideTargetsList(probes);
        final ImList<UserTarget>         userTargets = ImCollections.emptyList();
        return TargetEnvironment.create(base).setAllPrimaryGuideProbeTargets(gtCollection).setUserTargets(userTargets);
    }

    private static ImList<GuideProbeTargets> createGuideTargetsList(final GuideProbe... probes) {
        final List<GuideProbeTargets> res = new ArrayList<>();
        for (GuideProbe probe : probes) {
            res.add(createGuideTargets(probe));
        }
        return DefaultImList.create(res);
    }

    private void setTargetEnv(final GuideProbe... probes) throws Exception {
        final TargetEnvironment env = create(probes);

        // Store the target environment.
        final ObservationNode obsNode = getObsNode();
        final TargetNode targetNode = obsNode.getTarget();

        final TargetObsComp obsComp = targetNode.getDataObject();
        obsComp.setTargetEnvironment(env);
        targetNode.getRemoteNode().setDataObject(obsComp);
    }

    @Test public void testF2_SIDE() throws Exception {
        final Flamingos2 flam2 = getInstrument();
        flam2.setIssPort(IssPort.SIDE_LOOKING);
        assertEquals(flam2.getDisperser(), Flamingos2.Disperser.NONE);
        setInstrument(flam2);

        verifyInstrumentConfig(getSouthResults(), "F25");
    }

    @Test public void testF2_UP() throws Exception {
        final Flamingos2 flam2 = getInstrument();
        flam2.setIssPort(IssPort.UP_LOOKING);
        assertEquals(flam2.getDisperser(), Flamingos2.Disperser.NONE);
        setInstrument(flam2);

        verifyInstrumentConfig(getSouthResults(), "F2");
    }

    public void testF2_P2_SIDE() throws Exception {
        final Flamingos2 flam2 = getInstrument();
        flam2.setIssPort(IssPort.SIDE_LOOKING);
        assertEquals(flam2.getDisperser(), Flamingos2.Disperser.NONE);
        setInstrument(flam2);
        setTargetEnv(PwfsGuideProbe.pwfs2);

        verifyInstrumentConfig(getSouthResults(), "F25_P2");
    }

    @Test public void testF2_P2_UP() throws Exception {
        final Flamingos2 flam2 = getInstrument();
        flam2.setIssPort(IssPort.UP_LOOKING);
        assertEquals(flam2.getDisperser(), Flamingos2.Disperser.NONE);
        setInstrument(flam2);
        setTargetEnv(PwfsGuideProbe.pwfs2);

        verifyInstrumentConfig(getSouthResults(), "F2_P2");
    }

    @Test public void testF2_SIDE_SPEC() throws Exception {
        final Flamingos2 flam2 = getInstrument();
        flam2.setIssPort(IssPort.SIDE_LOOKING);
        flam2.setDisperser(Flamingos2.Disperser.R3000);
        setInstrument(flam2);

        verifyInstrumentConfig(getSouthResults(), "F25");
    }

    @Test public void testF2_UP_SPEC() throws Exception {
        final Flamingos2 flam2 = getInstrument();
        flam2.setIssPort(IssPort.UP_LOOKING);
        flam2.setDisperser(Flamingos2.Disperser.R3000);
        setInstrument(flam2);

        verifyInstrumentConfig(getSouthResults(), "F2");
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored","unchecked","rawtypes"})
    @Test public void testWavelength() throws Exception {
        final Flamingos2 flam2 = getInstrument();
        flam2.setFilter(Filter.OPEN);
        flam2.setDisperser(Disperser.NONE);
        setInstrument(flam2);

        // Use imaging mode wavelength if no disperser and no filter.
        assertEquals("1.6", getWavelength(getSouthResults()));

        // Use the disperser wavelength in spectroscopy mode with no filter
        flam2.setFilter(Filter.OPEN);

        final Pair<Disperser,String>[] dtA = new Pair[] {
            new Pair<>(Disperser.R1200JH, "1.3385"),
            new Pair<>(Disperser.R1200HK, "1.9"),
            new Pair<>(Disperser.R3000,   "1.65"),
        };

        for (final Pair<Disperser, String> t : dtA) {
            flam2.setDisperser(t._1());
            setInstrument(flam2);
            assertEquals(t._2(), getWavelength(getSouthResults()));
        }

        // Use the filter wavelength even when in spectroscopy mode when the
        // filter is specified.
        flam2.setDisperser(Disperser.R3000);

        final Pair<Filter,String>[] ftA = new Pair[] {
            new Pair<>(Filter.Y,       "1.02"),
            new Pair<>(Filter.J_LOW,   "1.15"),
            new Pair<>(Filter.J,       "1.25"),
            new Pair<>(Filter.H,       "1.65"),
            new Pair<>(Filter.K_LONG,  "2.2" ),
            new Pair<>(Filter.K_SHORT, "2.15"),
            new Pair<>(Filter.K_BLUE, "2.06"),
            new Pair<>(Filter.K_RED,  "2.31"),
            new Pair<>(Filter.JH,      "1.3385"),
            new Pair<>(Filter.HK,      "1.9"),
        };

        for (final Pair<Filter, String> t : ftA) {
            flam2.setFilter(t._1());
            setInstrument(flam2);
            assertEquals(t._2(), getWavelength(getSouthResults()));
        }

        // Use the filter when in imaging mode.
        flam2.setDisperser(Disperser.NONE);

        for (final Pair<Filter, String> t : ftA) {
            flam2.setFilter(t._1());
            setInstrument(flam2);
            assertEquals(t._2(), getWavelength(getSouthResults()));
        }

        // For Darks, anything goes but make sure it doesn't crash.
        flam2.setFilter(Filter.DARK);
        setInstrument(flam2);
        try {
            Double.parseDouble(getWavelength(getSouthResults()));
        } catch (final Exception ex) {
            fail("dark wavelength not set");
        }
    }

    @Test public void testNoAoPointOrig() throws Exception {
        verifyPointOrig(getSouthResults(), "f2");
    }

    @Test public void testLgsPointOrig() throws Exception {
        addGems();
        verifyPointOrig(getSouthResults(), "lgs2f2");
    }

}
