package edu.gemini.p2checker.rules;

import edu.gemini.p2checker.api.ObservationElements;
import edu.gemini.p2checker.api.Problem;
import edu.gemini.p2checker.rules.general.GeneralRule;
import edu.gemini.pot.sp.*;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Class GeneralRuleTest
 *
 * @author Nicolas A. Barriga
 *         Date: 4/8/11
 */
public final class GeneralRuleTest extends AbstractRuleTest {

    private static final String ALTAIR_MESSAGE = "Error:Altair is currently only commissioned for use with NIRI, NIFS, GNIRS and GMOS-N";

    @Test
    public void testNoErrors() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        addTargetObsCompAO();
        addAltair(AltairParams.Mode.NGS_FL);


        final ObservationElements elems = new ObservationElements(obs);
        assertTrue(elems.hasAltair());

        final GeneralRule rules = new GeneralRule();
        final List<Problem> problems = rules.check(elems).getProblems();

        assert problems.size() == 1 && problems.get(0).toString().startsWith(ALTAIR_MESSAGE);
    }

    @Test
    public void testAOP1() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        addMichelle();
        addAltair(AltairParams.Mode.LGS_P1);
        addTargetObsCompAOP1();


        final ObservationElements elems = new ObservationElements(obs);
        assertTrue(elems.hasAltair());

        final GeneralRule rules = new GeneralRule();
        final List<Problem> problems = rules.check(elems).getProblems();

        assertEquals(2, problems.size());
        Problem p0 = problems.get(0);
        Problem p1 = problems.get(1);

        // These can come out in either order.  We're expecting an error and a
        // warning in either order.
        if (p0.getType() == Problem.Type.ERROR) {
            // swap them
            Problem t = p1;
            p1 = p0;
            p0 = t;
        }

        assertEquals(Problem.Type.WARNING, p0.getType());
        assertEquals(Problem.Type.ERROR, p1.getType());
        assertTrue(p0.toString(), p0.toString().startsWith("Warning:PWFS2 is the preferred peripheral wavefront sensor."));
        assertTrue(p1.toString(), p1.toString().startsWith(ALTAIR_MESSAGE));
    }

    @Test
    public void testEmpty() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        addTargetObsCompEmpty();
        addAltair(AltairParams.Mode.NGS_FL);

        final ObservationElements elems = new ObservationElements(obs);
        assertTrue(elems.hasAltair());

        final GeneralRule rules = new GeneralRule();
        final List<Problem> problems = rules.check(elems).getProblems();

        assertEquals(1, problems.size());
        final Problem p = problems.get(0);
        assertEquals(Problem.Type.ERROR, p.getType());
        assertTrue(p.toString(), p.toString().startsWith(ALTAIR_MESSAGE));
    }

    @Test
    public void testLGS() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        addTargetObsCompEmpty();
        addAltair(AltairParams.Mode.LGS);

        final ObservationElements elems = new ObservationElements(obs);
        assertTrue(elems.hasAltair());

        final GeneralRule rules = new GeneralRule();
        final List<Problem> problems = rules.check(elems).getProblems();
        assertEquals(1, problems.size());
        final Problem p = problems.get(0);
        assertEquals(Problem.Type.ERROR, p.getType());
        assertTrue(p.toString(), p.toString().startsWith(ALTAIR_MESSAGE));
    }

    @Test
    public void testAsterismType() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        // This should add GHOST, and then add an asterism of type Single, which is incompatible.
        addGhost();
        addTargetObsCompEmpty();

        final ObservationElements elems = new ObservationElements(obs);
        final GeneralRule rules = new GeneralRule();
        final List<Problem> problems = rules.check(elems).getProblems();
        assertEquals(1, problems.size());
        final Problem p = problems.get(0);
        assertEquals(Problem.Type.ERROR, p.getType());
        assertTrue(p.toString(), p.toString().contains("unsupported asterism"));
    }

    @Test
    public void testGhostDualTargetOk() throws Exception {
        SPSiteQuality p1 = new SPSiteQuality();
        p1.setCloudCover(SPSiteQuality.CloudCover.PERCENT_50);
        addTemplateFolder(p1);

        addGhost();
        addGhostDualTarget();
        addSiteQuality(SPSiteQuality.ImageQuality.DEFAULT,
                SPSiteQuality.CloudCover.PERCENT_50, // Match Conditions
                SPSiteQuality.SkyBackground.DEFAULT,
                SPSiteQuality.WaterVapor.DEFAULT);

        final ObservationElements elems = new ObservationElements(obs);
        final GeneralRule rules = new GeneralRule();
        final List<Problem> problems = rules.check(elems).getProblems();

        assertTrue(problems.isEmpty());
    }

    @Test
    public void testGhostDualTargetBad() throws Exception {
        SPSiteQuality p1 = new SPSiteQuality();
        p1.setCloudCover(SPSiteQuality.CloudCover.PERCENT_80);
        addTemplateFolder(p1);

        addGhost();
        addGhostDualTarget();
        addSiteQuality(SPSiteQuality.ImageQuality.DEFAULT,
                SPSiteQuality.CloudCover.PERCENT_50, // better conditions than p1
                SPSiteQuality.SkyBackground.DEFAULT,
                SPSiteQuality.WaterVapor.DEFAULT);

        final ObservationElements elems = new ObservationElements(obs);
        final GeneralRule rules = new GeneralRule();
        final List<Problem> problems = rules.check(elems).getProblems();

        // I'd have expected 2 problems, one per target but they are smashed into a set
        assertEquals(problems.size(), 1);
        for (Problem p : problems) {
            if (p.toString().contains("Conditions have to be the same")) {
                assertEquals(Problem.Type.ERROR, p.getType());
            }
        }
    }
}
