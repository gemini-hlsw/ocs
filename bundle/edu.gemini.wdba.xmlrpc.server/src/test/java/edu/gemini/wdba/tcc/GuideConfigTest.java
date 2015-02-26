//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImCollections;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.ext.ObservationNode;
import edu.gemini.spModel.ext.TargetNode;
import edu.gemini.spModel.gemini.altair.AltairAowfsGuider;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2OiwfsGuideProbe;
import edu.gemini.spModel.gemini.gems.Canopus;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw;
import edu.gemini.spModel.gemini.nici.NiciOiwfsGuideProbe;
import edu.gemini.spModel.gemini.niri.NiriOiwfsGuideProbe;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import org.dom4j.Document;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class GuideConfigTest extends TestBase {

    private SPTarget base;

    protected void setUp() throws Exception {
        super.setUp();

        base = new SPTarget();
        base.getTarget().setName("Base Pos");
        base.notifyOfGenericUpdate();
    }

    private static GuideProbeTargets createGuideTargets(GuideProbe probe) {
        ImList<SPTarget> targetList = ImCollections.singletonList(new SPTarget());
        return GuideProbeTargets.create(probe, targetList);
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

    public void testNoGuide() throws Exception {
        testTargetEnvironment(TccNames.NO_GUIDING, create());
    }

    public void testP1() throws Exception {
        testTargetEnvironment(TccNames.P1, create(PwfsGuideProbe.pwfs1));
    }

    public void testP2() throws Exception {
        testTargetEnvironment(TccNames.P2, create(PwfsGuideProbe.pwfs2));
    }

    public void testP1P2() throws Exception {
        testTargetEnvironment(TccNames.P1P2, create(PwfsGuideProbe.values()));
    }

    public void testOI() throws Exception {
        testTargetEnvironment(TccNames.OI, create(NiciOiwfsGuideProbe.instance));
    }

    public void testP1OI() throws Exception {
        testTargetEnvironment(TccNames.P1OI, create(PwfsGuideProbe.pwfs1, NiciOiwfsGuideProbe.instance));
    }

    public void testP2OI() throws Exception {
        testTargetEnvironment(TccNames.P2OI, create(PwfsGuideProbe.pwfs2, NiciOiwfsGuideProbe.instance));
    }

    public void testGeMS() throws Exception {
        testTargetEnvironment(TccNames.GeMS, create(Canopus.Wfs.values()));
        testTargetEnvironment(TccNames.GeMS, create(Canopus.Wfs.cwfs1, Canopus.Wfs.cwfs2, GsaoiOdgw.odgw1));
    }

    public void testGeMSOI() throws Exception {
        testTargetEnvironment(TccNames.GeMSOI, create(Canopus.Wfs.cwfs1, Flamingos2OiwfsGuideProbe.instance));
        testTargetEnvironment(TccNames.GeMSOI, create(Canopus.Wfs.cwfs1, GsaoiOdgw.odgw1, Flamingos2OiwfsGuideProbe.instance));
    }

    public void testGeMSP1() throws Exception {
        testTargetEnvironment(TccNames.GeMSP1, create(PwfsGuideProbe.pwfs1, Canopus.Wfs.cwfs1));
    }

    public void testGeMSP1OI() throws Exception {
        testTargetEnvironment(TccNames.GeMSP1OI, create(PwfsGuideProbe.pwfs1, Canopus.Wfs.cwfs1, Flamingos2OiwfsGuideProbe.instance));
    }

    public void testAO() throws Exception {
        testTargetEnvironment(TccNames.AO, create(AltairAowfsGuider.instance));
    }

    public void testAOOI() throws Exception {
        testTargetEnvironment(TccNames.AOOI, create(AltairAowfsGuider.instance, NiriOiwfsGuideProbe.instance));
    }

    public void testAOP1() throws Exception {
        testTargetEnvironment(TccNames.AOP1, create(AltairAowfsGuider.instance, PwfsGuideProbe.pwfs1));
    }

    private void addAltair(AltairParams.Mode mode) throws Exception {
        ISPObsComponent altairComp =  odb.getFactory().createObsComponent(prog, InstAltair.SP_TYPE, null);
        InstAltair altair = (InstAltair) altairComp.getDataObject();
        altair.setMode(mode);
        altairComp.setDataObject(altair);
        obs.addObsComponent(altairComp);
    }

    public void testLGSP1() throws Exception {
        addAltair(AltairParams.Mode.LGS_P1);
        testTargetEnvironment(TccNames.AOP1, create(PwfsGuideProbe.pwfs1));
    }

    public void testLGSOI() throws Exception {
        addAltair(AltairParams.Mode.LGS_OI);
        testTargetEnvironment(TccNames.AOOI, create(GmosOiwfsGuideProbe.instance));
    }

    public void testNGSAO() throws Exception {
        addAltair(AltairParams.Mode.NGS); // altair but no AO guide star
        testTargetEnvironment(TccNames.P1, create(PwfsGuideProbe.pwfs1));
    }

    public void testAOP2() throws Exception {
        testTargetEnvironment(TccNames.AOP2, create(AltairAowfsGuider.instance, PwfsGuideProbe.pwfs2));
    }

    private String getGuideConfig(Document doc) throws Exception {
        Element tccFieldConfig = getTccFieldConfig(doc);
        if (tccFieldConfig == null) fail("no tcc_tcs_config_file element");

        Element pset = (Element) tccFieldConfig.selectSingleNode("//paramset[@name='guideConfig']");
        if (pset == null) fail("missing 'guideConfig' paramset");

        Element param = (Element) pset.selectSingleNode("param[@name='guideWith']");
        if (param == null) fail("missing 'guide' param");
        return param.attributeValue("value");
    }

    private void testTargetEnvironment(String guideConfig, TargetEnvironment env) throws Exception {
        // Store the target environment.
        ObservationNode obsNode = getObsNode();
        TargetNode targetNode = obsNode.getTarget();

        TargetObsComp obsComp = targetNode.getDataObject();
        obsComp.setTargetEnvironment(env);
        targetNode.getRemoteNode().setDataObject(obsComp);

        // Get the results.
        Document doc = getSouthResults();

        assertEquals(guideConfig, getGuideConfig(doc));

//        doc.write(new OutputStreamWriter(System.out));
//        System.out.println(doc);
    }
}
