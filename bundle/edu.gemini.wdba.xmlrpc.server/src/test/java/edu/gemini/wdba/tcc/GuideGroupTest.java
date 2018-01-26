package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.ext.ObservationNode;
import edu.gemini.spModel.ext.TargetNode;
import edu.gemini.spModel.gemini.gems.Canopus;
import edu.gemini.spModel.gemini.gems.Gems;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideEnvironment;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.env.OptionsListImpl;
import org.dom4j.Document;
import org.dom4j.Element;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public final class GuideGroupTest extends TestBase {
    private SPTarget base;

    @Before public void setUp() throws Exception {
        super.setUp();

        base = new SPTarget();
        base.setName("Base Pos");
    }

    private static GuideProbeTargets createGuideTargets(final GuideProbe probe) {
        final SPTarget target = new SPTarget();
        return GuideProbeTargets.create(probe, target);
    }

    private static ImList<GuideProbeTargets> createGuideTargetsList(final GuideProbe... probes) {
        final List<GuideProbeTargets> res = new ArrayList<>();
        for (GuideProbe probe : probes) {
            res.add(createGuideTargets(probe));
        }
        return DefaultImList.create(res);
    }

    @Test public void testLoneGroup() throws Exception {
        // Create a target environment that uses Gems canopus wfs.
        final TargetEnvironment env = TargetEnvironment.create(base);
        final ImList<GuideProbeTargets> gtCollection = createGuideTargetsList(Canopus.Wfs.cwfs1, Canopus.Wfs.cwfs2, Canopus.Wfs.cwfs3);
        final GuideGroup gg = GuideGroup.create("LoneGroup", gtCollection);
        final TargetEnvironment env2 = env.setPrimaryGuideGroup(gg);

        // Now, we need to add Gems or the guide targets are
        // not enabled and not sent to the TCC.
        final ISPObsComponent gemsComp = odb.getFactory().createObsComponent(prog, Gems.SP_TYPE, null);
        obs.addObsComponent(gemsComp);

        testTargetEnvironment("LoneGroup", env2);
    }

    @Test public void testNamedGroup() throws Exception {
        // Create a target environment that uses Gems canopus wfs.
        final TargetEnvironment env = TargetEnvironment.create(base);
        final ImList<GuideProbeTargets> gtCollection = createGuideTargetsList(Canopus.Wfs.cwfs1, Canopus.Wfs.cwfs2, Canopus.Wfs.cwfs3);
        final GuideGroup gg1 = GuideGroup.create("Group1", gtCollection);

        final ImList<GuideProbeTargets> gtCollection2 = createGuideTargetsList(Canopus.Wfs.cwfs1, Canopus.Wfs.cwfs2, Canopus.Wfs.cwfs3);
        final GuideGroup gg2 = GuideGroup.create("Group2", gtCollection2);
        final TargetEnvironment env2 = env.setGuideEnvironment(GuideEnvironment.create(OptionsListImpl.create(gg1, gg2)));

        // Now, we need to add Gems or the guide targets are
        // not enabled and not sent to the TCC.
        final ISPObsComponent gemsComp = odb.getFactory().createObsComponent(prog, Gems.SP_TYPE, null);
        obs.addObsComponent(gemsComp);

        testTargetEnvironment("Group1", env2);
    }

    private String getGuideGroup(final Document doc) throws Exception {
        final Element tccFieldConfig = getTccFieldConfig(doc);
        if (tccFieldConfig == null) Assert.fail("no tcc_tcs_config_file element");

        final Element param = (Element) tccFieldConfig.selectSingleNode("//param[@name='guideGroup']");
        if (param == null) Assert.fail("missing 'guideGroup' param");
        return param.attributeValue("value");
    }

    private void testTargetEnvironment(final String guideGroupName, final TargetEnvironment env) throws Exception {
        // Store the target environment.
        final ObservationNode obsNode = getObsNode();
        final TargetNode targetNode = obsNode.getTarget();

        final TargetObsComp obsComp = targetNode.getDataObject();
        obsComp.setTargetEnvironment(env);
        targetNode.getRemoteNode().setDataObject(obsComp);

        // Get the results.
        final Document doc = getSouthResults();

        Assert.assertEquals(guideGroupName, getGuideGroup(doc));
    }

}
