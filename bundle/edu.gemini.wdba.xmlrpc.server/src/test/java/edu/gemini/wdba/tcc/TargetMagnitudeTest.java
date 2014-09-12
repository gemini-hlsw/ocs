//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.spModel.ext.ObservationNode;
import edu.gemini.spModel.ext.TargetNode;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.system.ConicTarget;
import org.dom4j.Document;
import org.dom4j.Element;

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

        SPTarget base = new SPTarget();
        base.setName("Base Pos");

        pwfs1_1 = new SPTarget();
        pwfs1_1.setName("PWFS1-1");

        env = TargetEnvironment.create(base);
        GuideProbeTargets gpt = GuideProbeTargets.create(PwfsGuideProbe.pwfs1, pwfs1_1);
        GuideGroup grp = GuideGroup.create("Default Guide Group", gpt);
        env = env.setPrimaryGuideGroup(grp);
    }

    public void testNoMagnitudeInfo() throws Exception {
        testTargetEnvironment(env);
    }

    public void testOneMagnitude() throws Exception {
        pwfs1_1.putMagnitude(new Magnitude(Magnitude.Band.J, 10));
        testTargetEnvironment(env);
    }

    public void testNonSiderealMagnitude() throws Exception {
        pwfs1_1.putMagnitude(new Magnitude(Magnitude.Band.J, 10));
        pwfs1_1.setTarget(new ConicTarget());
        pwfs1_1.setName("PWFS1-1");
        testTargetEnvironment(env);
    }

    public void testTwoMagnitudes() throws Exception {
        pwfs1_1.putMagnitude(new Magnitude(Magnitude.Band.J, 10));
        pwfs1_1.putMagnitude(new Magnitude(Magnitude.Band.K, 20));
        testTargetEnvironment(env);
    }

    private void testTargetEnvironment(TargetEnvironment env) throws Exception {

        // Store the target environment.
        ObservationNode obsNode = getObsNode();
        TargetNode targetNode = obsNode.getTarget();

        TargetObsComp obsComp = targetNode.getDataObject();
        obsComp.setTargetEnvironment(env);
        targetNode.getRemoteNode().setDataObject(obsComp);

        // Get the results.
        Document doc = getSouthResults();

        // Check that there is a target for each target in the TargetEnvironment
        List<Element> targetElements = getTargets(doc);

        for (Element targetElement : targetElements) {
            final String name = targetElement.attributeValue(ParamSet.NAME);
            assertNotNull(name);

            Option<SPTarget> target = env.getTargets().find(new PredicateOp<SPTarget>() {
                public Boolean apply(SPTarget spTarget) {
                    return name.equals(spTarget.getName());
                }
            });

            assertFalse(target.isEmpty());

            // Check the magnitude information for each target
            validateMagnitudes(targetElement, target.getValue());
        }
    }

    private static String MAG_PATH = "paramset[@name='" + TccNames.MAGNITUDES + "']";

    private void validateMagnitudes(Element element, SPTarget target) {
        final Element magGroupElement = (Element) element.selectSingleNode(MAG_PATH);
        ImList<Magnitude> mags = target.getMagnitudes();
        if (magGroupElement == null) {
            assertEquals(0, mags.size());
            return;
        }

        List magElementList = magGroupElement.elements();

        // One magnitude element per magnitude in the target
        assertEquals(mags.size(), magElementList.size());

        mags.foreach(new ApplyOp<Magnitude>() {
            @Override public void apply(Magnitude mag) {
                String path = String.format("param[@name='%s']", mag.getBand().name());
                Element magElement = (Element) magGroupElement.selectSingleNode(path);
                String strValue = magElement.attributeValue("value");
                double doubleVal = Double.valueOf(strValue);

                assertEquals(mag.getBrightness(), doubleVal, 0.00001);
            }
        });
    }
}