package edu.gemini.p2checker.rules;

import edu.gemini.p2checker.api.ObservationElements;
import edu.gemini.p2checker.api.Problem;
import edu.gemini.p2checker.rules.general.GeneralRule;
import edu.gemini.pot.sp.*;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Class GeneralRuleTest
 *
 * @author Nicolas A. Barriga
 *         Date: 4/8/11
 */
public class GeneralRuleTest extends AbstractRuleTest {

    private static final String ALTAIR_MESSAGE = "Error:Altair is currently only commissioned for use with NIRI, NIFS, GNIRS and GMOS-N";

    @Test
    public void testNoErrors() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        //addGmosNorth(GmosCommonType.Binning.DEFAULT, GmosCommonType.Binning.DEFAULT);
        addTargetObsCompAO();
        addAltair(AltairParams.Mode.NGS_FL);


        ObservationElements elems = new ObservationElements(obs);
        assertTrue(elems.hasAltair());

        GeneralRule rules = new GeneralRule();
        List<Problem> problems = rules.check(elems).getProblems();

//        for (Problem p : problems) {
//            fail(p.toString());
//        }

        assert problems.size() == 1 && problems.get(0).toString().startsWith(ALTAIR_MESSAGE);
    }

    @Test
    public void testAOP1() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        addMichelle();
        addAltair(AltairParams.Mode.LGS_P1); // NGS_FL);
        addTargetObsCompAOP1();


        ObservationElements elems = new ObservationElements(obs);
        assertTrue(elems.hasAltair());

        GeneralRule rules = new GeneralRule();
        List<Problem> problems = rules.check(elems).getProblems();

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
        //addGmosNorth(GmosCommonType.Binning.DEFAULT, GmosCommonType.Binning.DEFAULT);
        addTargetObsCompEmpty();
        addAltair(AltairParams.Mode.NGS_FL);


        ObservationElements elems = new ObservationElements(obs);
        assertTrue(elems.hasAltair());

        GeneralRule rules = new GeneralRule();
        List<Problem> problems = rules.check(elems).getProblems();

        assertEquals(1, problems.size());
        Problem p = problems.get(0);
        assertEquals(Problem.Type.ERROR, p.getType());
        assertTrue(p.toString(), p.toString().startsWith(ALTAIR_MESSAGE));

    }

    @Test
    public void testLGS() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        //addGmosNorth(GmosCommonType.Binning.DEFAULT, GmosCommonType.Binning.DEFAULT);
        addTargetObsCompEmpty();
        addAltair(AltairParams.Mode.LGS);


        ObservationElements elems = new ObservationElements(obs);
        assertTrue(elems.hasAltair());

        GeneralRule rules = new GeneralRule();
        List<Problem> problems = rules.check(elems).getProblems();
//        for (Problem p : problems) {
//            System.out.println(p.toString());
//        }
        assertEquals(1, problems.size());
        Problem p = problems.get(0);
        assertEquals(Problem.Type.ERROR, p.getType());
        assertTrue(p.toString(), p.toString().startsWith(ALTAIR_MESSAGE));

    }
}
