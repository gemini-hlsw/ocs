package edu.gemini.wdba.tcc;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.ext.ObservationNode;
import edu.gemini.spModel.ext.TargetNode;
import edu.gemini.spModel.gemini.gems.CanopusWfs;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.env.UserTarget;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.telescope.IssPort;
import org.dom4j.Document;
import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

/**
 * Test cases for {@link GNIRSSupport}.
 */
public final class GsaoiSupportTest extends InstrumentSupportTestBase<Gsaoi> {

    private final SPTarget base;

    public GsaoiSupportTest() throws Exception {
        super(Gsaoi.SP_TYPE);

        base = new SPTarget();
        base.setName("Base Pos");
    }

    @Before public void setUp() throws Exception {
        super.setUp();
    }

    private static GuideProbeTargets createGuideTargets(final GuideProbe probe) {
        final SPTarget target = new SPTarget();
        return GuideProbeTargets.create(probe, target);
    }

    private static ImList<GuideProbeTargets> createGuideTargetsList(final GuideProbe... probes) {
        List<GuideProbeTargets> res = new ArrayList<>();
        for (final GuideProbe probe : probes) {
            res.add(createGuideTargets(probe));
        }
        return DefaultImList.create(res);
    }

    private TargetEnvironment create(final GuideProbe... probes) {
        final ImList<GuideProbeTargets> gtCollection = createGuideTargetsList(probes);
        final ImList<UserTarget>         userTargets = ImCollections.emptyList();
        return TargetEnvironment.create(base).setAllPrimaryGuideProbeTargets(gtCollection).setUserTargets(userTargets);
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

    private void setOdgw(final Gsaoi.OdgwSize size) throws Exception {
        final Gsaoi gsaoi = (Gsaoi) obsComp.getDataObject();
        gsaoi.setOdgwSize(size);
        obsComp.setDataObject(gsaoi);
    }

    private Option<String> getOdgwSize(final Document doc) throws Exception {
        final Element tccFieldConfig = getTccFieldConfig(doc);
        if (tccFieldConfig == null) fail("no tcc_tcs_config_file element");

        final Element pset = (Element) tccFieldConfig.selectSingleNode("//paramset[@name='guideConfig']");
        if (pset == null) fail("missing 'guideConfig' paramset");

        final Element gems = (Element) pset.selectSingleNode("paramset[@name='GeMS']");
        if (gems == null) return None.STRING;

        final Element odgw = (Element) gems.selectSingleNode("paramset[@name='odgw']");
        if (odgw == null) return None.STRING;

        final Element size = (Element) odgw.selectSingleNode("param[@name='size']");
        return ImOption.apply(size.attributeValue("value"));
    }

    private void verify(final Option<Gsaoi.OdgwSize> size) throws Exception {
        final Document doc = getSouthResults();
        final Option<String> expectOpt = size.map(Gsaoi.OdgwSize::displayValue);
        assertEquals(expectOpt, getOdgwSize(doc));
    }

    private void verify(final Gsaoi.OdgwSize size) throws Exception {
        verify(new Some<>(size));
    }

    @Test public void testDefaultGuideConfig() throws Exception {
        setTargetEnv(CanopusWfs.cwfs1, GsaoiOdgw.odgw1);
        verify(Gsaoi.OdgwSize.DEFAULT);
    }

    @Test public void testExplicitGuideConfig() throws Exception {
        setTargetEnv(CanopusWfs.cwfs1, GsaoiOdgw.odgw1);
        setOdgw(Gsaoi.OdgwSize.SIZE_8);
        verify(Gsaoi.OdgwSize.SIZE_8);
    }

    @Test public void testNoGsaoi() throws Exception {
        setTargetEnv(CanopusWfs.cwfs1);
        final Option<Gsaoi.OdgwSize> none = None.instance();
        verify(none);
    }

    @Test public void testNotGems() throws Exception {
        setTargetEnv(PwfsGuideProbe.pwfs1);
        final Option<Gsaoi.OdgwSize> none = None.instance();
        verify(none);
    }

    @Test public void testPointOrig() throws Exception {
        verifyPointOrig(getSouthResults(), "lgs2gsaoi");
    }

    @Test public void testConfig() throws Exception {
        final Gsaoi gsaoi = getInstrument();

        gsaoi.setIssPort(IssPort.SIDE_LOOKING);
        setInstrument(gsaoi);
        verifyInstrumentConfig(getSouthResults(), "GSAOI5");

        gsaoi.setIssPort(IssPort.UP_LOOKING);
        setInstrument(gsaoi);
        verifyInstrumentConfig(getSouthResults(), "GSAOI");
    }
}
