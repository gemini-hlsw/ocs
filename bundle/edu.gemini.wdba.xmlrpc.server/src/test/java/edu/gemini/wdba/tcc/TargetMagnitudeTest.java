package edu.gemini.wdba.tcc;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.*;
import edu.gemini.spModel.ext.ObservationNode;
import edu.gemini.spModel.ext.TargetNode;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import org.dom4j.Document;
import org.dom4j.Element;
import scala.collection.JavaConversions$;

import java.util.*;

/**
 * Test code for the {@link edu.gemini.wdba.tcc.TargetGroupConfig}
 *
 * The property HLPG_PROJECT_BASE must be set to OCS installation dir.
 */
public final class TargetMagnitudeTest extends TestBase {

    private SPTarget pwfs1_1;
    private TargetEnvironment env;

    protected void setUp() throws Exception {
        super.setUp();

        final SPTarget base = new SPTarget();
        base.setName("Base Pos");

        pwfs1_1 = new SPTarget();
        pwfs1_1.setName("PWFS1-1");

        final GuideProbeTargets gpt = GuideProbeTargets.create(PwfsGuideProbe.pwfs1, pwfs1_1);
        final GuideGroup grp = GuideGroup.create("Default Guide Group", gpt);
        env = TargetEnvironment.create(base).setPrimaryGuideGroup(grp);
    }

    public void testNoMagnitudeInfo() throws Exception {
        testTargetEnvironment(env);
    }

    public void testOneMagnitude() throws Exception {
        pwfs1_1.putNewMagnitude(new Magnitude(10, MagnitudeBand.J$.MODULE$));
        testTargetEnvironment(env);
    }

    public void testNonSiderealMagnitude() throws Exception {
        pwfs1_1.putNewMagnitude(new Magnitude(10, MagnitudeBand.J$.MODULE$));
        pwfs1_1.setNonSidereal();
        pwfs1_1.setName("PWFS1-1");
        testTargetEnvironment(env);
    }

    public void testTwoMagnitudes() throws Exception {
        pwfs1_1.putNewMagnitude(new Magnitude(10, MagnitudeBand.J$.MODULE$));
        pwfs1_1.putNewMagnitude(new Magnitude(10, MagnitudeBand.K$.MODULE$));
        testTargetEnvironment(env);
    }

    private void testTargetEnvironment(final TargetEnvironment env) throws Exception {
        // Store the target environment.
        final ObservationNode obsNode = getObsNode();
        final TargetNode targetNode = obsNode.getTarget();

        final TargetObsComp obsComp = targetNode.getDataObject();
        obsComp.setTargetEnvironment(env);
        targetNode.getRemoteNode().setDataObject(obsComp);

        // Get the results.
        final Document doc = getSouthResults();

        // Check that there is a target for each target in the TargetEnvironment
        getTargets(doc).forEach(targetElement -> {
            final String name = targetElement.attributeValue(ParamSet.NAME);
            assertNotNull(name);

            final Option<SPTarget> target = env.getTargets().find(spTarget -> name.equals(spTarget.getName()));
            assertFalse(target.isEmpty());

            // Check the magnitude information for each target
            validateMagnitudes(targetElement, target.getValue());
        });
    }

    private void validateMagnitudes(final Element element, final SPTarget target) {
        final String MAG_PATH = "paramset[@name='" + TccNames.MAGNITUDES + "']";
        final Element magGroupElement = (Element) element.selectSingleNode(MAG_PATH);
        final scala.collection.immutable.List<Magnitude> mags = target.getNewMagnitudes();
        if (magGroupElement == null) {
            assertEquals(0, mags.size());
            return;
        }

        final List<?> magElementList = magGroupElement.elements();

        // One magnitude element per magnitude in the target
        assertEquals(mags.size(), magElementList.size());

        JavaConversions$.MODULE$.seqAsJavaList(mags).forEach(mag -> {
            final String path = String.format("param[@name='%s']", mag.band().name());
            final Element magElement = (Element) magGroupElement.selectSingleNode(path);
            final String strValue = magElement.attributeValue("value");
            final double doubleVal = Double.valueOf(strValue);

            assertEquals(mag.value(), doubleVal, 0.00001);
        });
    }
}
