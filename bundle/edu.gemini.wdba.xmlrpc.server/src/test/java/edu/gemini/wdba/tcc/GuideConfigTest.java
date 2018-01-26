package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImCollections;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.None;
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
import edu.gemini.spModel.target.env.*;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import org.dom4j.Document;
import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;


public final class GuideConfigTest extends TestBase {

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
        for (final GuideProbe probe : probes) {
            res.add(createGuideTargets(probe));
        }
        return DefaultImList.create(res);
    }

    private TargetEnvironment create(GuideProbe... probes) {
        final ImList<GuideProbeTargets> gtCollection = createGuideTargetsList(probes);
        final ImList<UserTarget>         userTargets = ImCollections.emptyList();
        return TargetEnvironment.create(base).setAllPrimaryGuideProbeTargets(gtCollection).setUserTargets(userTargets);
    }

    @Test public void testNoGuide() throws Exception {
        testTargetEnvironment(TccNames.NO_GUIDING, create());
    }

    @Test public void testP1() throws Exception {
        testTargetEnvironment(TccNames.P1, create(PwfsGuideProbe.pwfs1));
    }

    @Test public void testP2() throws Exception {
        testTargetEnvironment(TccNames.P2, create(PwfsGuideProbe.pwfs2));
    }

    @Test public void testP1P2() throws Exception {
        testTargetEnvironment(TccNames.P1P2, create(PwfsGuideProbe.values()));
    }

    @Test public void testOI() throws Exception {
        testTargetEnvironment(TccNames.OI, create(NiciOiwfsGuideProbe.instance));
    }

    @Test public void testP1OI() throws Exception {
        testTargetEnvironment(TccNames.P1OI, create(PwfsGuideProbe.pwfs1, NiciOiwfsGuideProbe.instance));
    }

    @Test public void testP2OI() throws Exception {
        testTargetEnvironment(TccNames.P2OI, create(PwfsGuideProbe.pwfs2, NiciOiwfsGuideProbe.instance));
    }

    // If you have a disabled P2 and an enabled OI, the guide config should
    // be OI only, not P2OI.
    @Test public void testRel2789() throws Exception {
        // Make a target environment with a manual group with active PWFS2 and
        // OI targets.
        final GuideProbeTargets      gptP2 = GuideProbeTargets.create(PwfsGuideProbe.pwfs2, new SPTarget());
        final GuideProbeTargets      gptOi = GuideProbeTargets.create(NiciOiwfsGuideProbe.instance, new SPTarget());
        final GuideGroup              grp  = GuideGroup.create("Manual Group", gptP2, gptOi);
        final OptionsList<GuideGroup> opts = OptionsListImpl.create(GuideGroup.AutomaticInitial(), grp).setPrimaryIndex(1);
        final GuideEnvironment        genv = GuideEnvironment.create(opts);
        final TargetEnvironment        env = TargetEnvironment.create(new SPTarget(), genv, DefaultImList.create());

        // Disable the PWFS2 target in env.
        final TargetEnvironment env2 = env.putPrimaryGuideProbeTargets(gptP2.setPrimaryIndex(None.instance()));

        // Primary now only includes OIWFS
        final Set<GuideProbe> guiders = new HashSet<>();
        guiders.add(NiciOiwfsGuideProbe.instance);
        assertEquals(guiders, env2.getGuideEnvironment().getPrimaryReferencedGuiders());

        // All guiders still includes both PWFS2 and OI
        guiders.add(PwfsGuideProbe.pwfs2);
        assertEquals(guiders, env2.getGuideEnvironment().getReferencedGuiders());

        // But the guide config will only have OI since it is the only one active.
        testTargetEnvironment(TccNames.OI, env2);
    }

    @Test public void testGeMS() throws Exception {
        testTargetEnvironment(TccNames.GeMS, create(Canopus.Wfs.values()));
        testTargetEnvironment(TccNames.GeMS, create(Canopus.Wfs.cwfs1, Canopus.Wfs.cwfs2, GsaoiOdgw.odgw1));
    }

    @Test public void testGeMSOI() throws Exception {
        testTargetEnvironment(TccNames.GeMSOI, create(Canopus.Wfs.cwfs1, Flamingos2OiwfsGuideProbe.instance));
        testTargetEnvironment(TccNames.GeMSOI, create(Canopus.Wfs.cwfs1, GsaoiOdgw.odgw1, Flamingos2OiwfsGuideProbe.instance));
    }

    @Test public void testGeMSP1() throws Exception {
        testTargetEnvironment(TccNames.GeMSP1, create(PwfsGuideProbe.pwfs1, Canopus.Wfs.cwfs1));
    }

    @Test public void testGeMSP1OI() throws Exception {
        testTargetEnvironment(TccNames.GeMSP1OI, create(PwfsGuideProbe.pwfs1, Canopus.Wfs.cwfs1, Flamingos2OiwfsGuideProbe.instance));
    }

    @Test public void testAO() throws Exception {
        testTargetEnvironment(TccNames.AO, create(AltairAowfsGuider.instance));
    }

    @Test public void testAOOI() throws Exception {
        testTargetEnvironment(TccNames.AOOI, create(AltairAowfsGuider.instance, NiriOiwfsGuideProbe.instance));
    }

    @Test public void testAOP1() throws Exception {
        testTargetEnvironment(TccNames.AOP1, create(AltairAowfsGuider.instance, PwfsGuideProbe.pwfs1));
    }

    private void addAltair(final AltairParams.Mode mode) throws Exception {
        final ISPObsComponent altairComp =  odb.getFactory().createObsComponent(prog, InstAltair.SP_TYPE, null);
        final InstAltair altair = (InstAltair) altairComp.getDataObject();
        altair.setMode(mode);
        altairComp.setDataObject(altair);
        obs.addObsComponent(altairComp);
    }

    @Test public void testLGSP1() throws Exception {
        addAltair(AltairParams.Mode.LGS_P1);
        testTargetEnvironment(TccNames.AOP1, create(PwfsGuideProbe.pwfs1));
    }

    @Test public void testLGSOI() throws Exception {
        addAltair(AltairParams.Mode.LGS_OI);
        testTargetEnvironment(TccNames.AOOI, create(GmosOiwfsGuideProbe.instance));
    }

    @Test public void testNGSAO() throws Exception {
        addAltair(AltairParams.Mode.NGS); // altair but no AO guide star
        testTargetEnvironment(TccNames.P1, create(PwfsGuideProbe.pwfs1));
    }

    @Test public void testAOP2() throws Exception {
        testTargetEnvironment(TccNames.AOP2, create(AltairAowfsGuider.instance, PwfsGuideProbe.pwfs2));
    }

    private String getGuideConfig(final Document doc) throws Exception {
        final Element tccFieldConfig = getTccFieldConfig(doc);
        if (tccFieldConfig == null) fail("no tcc_tcs_config_file element");

        final Element pset = (Element) tccFieldConfig.selectSingleNode("//paramset[@name='guideConfig']");
        if (pset == null) fail("missing 'guideConfig' paramset");

        final Element param = (Element) pset.selectSingleNode("param[@name='guideWith']");
        if (param == null) fail("missing 'guide' param");
        return param.attributeValue("value");
    }

    private void testTargetEnvironment(final String guideConfig, final TargetEnvironment env) throws Exception {
        // Store the target environment.
        final ObservationNode obsNode = getObsNode();
        final TargetNode targetNode = obsNode.getTarget();

        final TargetObsComp obsComp = targetNode.getDataObject();
        obsComp.setTargetEnvironment(env);
        targetNode.getRemoteNode().setDataObject(obsComp);

        // Get the results.
        final Document doc = getSouthResults();

        assertEquals(guideConfig, getGuideConfig(doc));
    }
}
