package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImCollections;
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
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jluhrs
 * Date: 11-08-11
 * Time: 21:45
 */
public final class GuideGroupTest extends TestBase {
    private SPTarget base;

    protected void setUp() throws Exception {
        super.setUp();

        base = new SPTarget();
        base.setName("Base Pos");
    }

    private static GuideProbeTargets createGuideTargets(GuideProbe probe) {
        final SPTarget target = new SPTarget();
        return GuideProbeTargets.create(probe, target).withExistingPrimary(target);
    }

    private static ImList<GuideProbeTargets> createGuideTargetsList(GuideProbe... probes) {
        List<GuideProbeTargets> res = new ArrayList<GuideProbeTargets>();
        for (GuideProbe probe : probes) {
            res.add(createGuideTargets(probe));
        }
        return DefaultImList.create(res);
    }

    private TargetEnvironment create(GuideProbe... probes) {
        ImList<GuideProbeTargets> gtCollection = createGuideTargetsList(probes);
        ImList<SPTarget> userTargets = ImCollections.emptyList();
        return TargetEnvironment.create(base).setAllPrimaryGuideProbeTargets(gtCollection).setUserTargets(userTargets);
    }

    @Test
    public void testLoneGroup() throws Exception {
        // Create a target environment that uses Gems canopus wfs.
        TargetEnvironment env = TargetEnvironment.create(base);
        ImList<GuideProbeTargets> gtCollection = createGuideTargetsList(Canopus.Wfs.cwfs1, Canopus.Wfs.cwfs2, Canopus.Wfs.cwfs3);
        GuideGroup gg = GuideGroup.create("LoneGroup", gtCollection);
        env = env.setPrimaryGuideGroup(gg);
        // Now, we need to add Gems or the guide targets are
        // not enabled and not sent to the TCC.
        ISPObsComponent gemsComp;
        gemsComp = odb.getFactory().createObsComponent(prog, Gems.SP_TYPE, null);
        obs.addObsComponent(gemsComp);

        testTargetEnvironment("", env);
    }

    @Test
    public void testUnnamedGroup() throws Exception {
        // Create a target environment that uses Gems canopus wfs.
        TargetEnvironment env = create(Canopus.Wfs.cwfs1, Canopus.Wfs.cwfs2, Canopus.Wfs.cwfs3);
        ImList<GuideProbeTargets> gtCollection = createGuideTargetsList(Canopus.Wfs.cwfs1, Canopus.Wfs.cwfs2, Canopus.Wfs.cwfs3);
        GuideGroup gg = GuideGroup.create("", gtCollection);
        env = env.setGuideEnvironment(GuideEnvironment.create(OptionsListImpl.create(env.getOrCreatePrimaryGuideGroup(), gg)));

        // Now, we need to add Gems or the guide targets are
        // not enabled and not sent to the TCC.
        ISPObsComponent gemsComp;
        gemsComp = odb.getFactory().createObsComponent(prog, Gems.SP_TYPE, null);
        obs.addObsComponent(gemsComp);

        testTargetEnvironment("Guide Group 1", env);
    }

    @Test
    public void testNamedGroup() throws Exception {
        // Create a target environment that uses Gems canopus wfs.
        TargetEnvironment env = TargetEnvironment.create(base);
        ImList<GuideProbeTargets> gtCollection = createGuideTargetsList(Canopus.Wfs.cwfs1, Canopus.Wfs.cwfs2, Canopus.Wfs.cwfs3);
        GuideGroup gg1 = GuideGroup.create("NamedGroup", gtCollection);
        gtCollection = createGuideTargetsList(Canopus.Wfs.cwfs1, Canopus.Wfs.cwfs2, Canopus.Wfs.cwfs3);
        GuideGroup gg2 = GuideGroup.create("", gtCollection);
        env = env.setGuideEnvironment(GuideEnvironment.create(OptionsListImpl.create(gg1, gg2)));

        // Now, we need to add Gems or the guide targets are
        // not enabled and not sent to the TCC.
        ISPObsComponent gemsComp;
        gemsComp = odb.getFactory().createObsComponent(prog, Gems.SP_TYPE, null);
        obs.addObsComponent(gemsComp);

        testTargetEnvironment("NamedGroup", env);
    }

    private String getGuideGroup(Document doc) throws Exception {
        Element tccFieldConfig = getTccFieldConfig(doc);
        if (tccFieldConfig == null) fail("no tcc_tcs_config_file element");

        Element param = (Element) tccFieldConfig.selectSingleNode("//param[@name='guideGroup']");
        if (param == null) fail("missing 'guideGroup' param");
        return param.attributeValue("value");
    }

    private void testTargetEnvironment(String guideGroupName, TargetEnvironment env) throws Exception {
        // Store the target environment.
        ObservationNode obsNode = getObsNode();
        TargetNode targetNode = obsNode.getTarget();

        TargetObsComp obsComp = targetNode.getDataObject();
        obsComp.setTargetEnvironment(env);
        targetNode.getRemoteNode().setDataObject(obsComp);

        // Get the results.
        Document doc = getSouthResults();

        assertEquals(guideGroupName, getGuideGroup(doc));
    }

}
