//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.ext.ObservationNode;
import edu.gemini.spModel.ext.TargetNode;
import edu.gemini.spModel.gemini.gems.Canopus;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.telescope.IssPort;
import org.dom4j.Document;
import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test cases for {@link GNIRSSupport}.
 */
public final class GsaoiSupportTest extends InstrumentSupportTestBase<Gsaoi> {

    private SPTarget base;

    public GsaoiSupportTest() throws Exception {
        super(Gsaoi.SP_TYPE);

        base = new SPTarget();
        base.setName("Base Pos");
    }

    @Before public void setUp() throws Exception {
        super.setUp();
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

    private void setTargetEnv(GuideProbe... probes) throws Exception {
        TargetEnvironment env = create(probes);

        // Store the target environment.
        ObservationNode obsNode = getObsNode();
        TargetNode targetNode = obsNode.getTarget();

        TargetObsComp obsComp = targetNode.getDataObject();
        obsComp.setTargetEnvironment(env);
        targetNode.getRemoteNode().setDataObject(obsComp);
    }

    private void setOdgw(Gsaoi.OdgwSize size) throws Exception {
        Gsaoi gsaoi = (Gsaoi) obsComp.getDataObject();
        gsaoi.setOdgwSize(size);
        obsComp.setDataObject(gsaoi);
    }

    private Option<String> getOdgwSize(Document doc) throws Exception {
        Element tccFieldConfig = getTccFieldConfig(doc);
        if (tccFieldConfig == null) fail("no tcc_tcs_config_file element");

        Element pset = (Element) tccFieldConfig.selectSingleNode("//paramset[@name='guideConfig']");
        if (pset == null) fail("missing 'guideConfig' paramset");

        Element gems = (Element) pset.selectSingleNode("paramset[@name='GeMS']");
        if (gems == null) return None.STRING;

        Element odgw = (Element) gems.selectSingleNode("paramset[@name='odgw']");
        if (odgw == null) return None.STRING;

        Element size = (Element) odgw.selectSingleNode("param[@name='size']");
        String val = size.attributeValue("value");
        return (val == null) ? None.STRING : new Some<String>(val);
    }

    private void verify(Option<Gsaoi.OdgwSize> size) throws Exception {
        Document doc = getSouthResults();

        Option<String> expectOpt = size.map(new Function1<Gsaoi.OdgwSize, String>() {
            @Override public String apply(Gsaoi.OdgwSize odgwSize) {
                return odgwSize.displayValue();
            }
        });
        assertEquals(expectOpt, getOdgwSize(doc));
    }

    private void verify(Gsaoi.OdgwSize size) throws Exception {
        verify(new Some<Gsaoi.OdgwSize>(size));
    }

    @Test public void testDefaultGuideConfig() throws Exception {
        setTargetEnv(Canopus.Wfs.cwfs1, GsaoiOdgw.odgw1);
        verify(Gsaoi.OdgwSize.DEFAULT);
    }

    @Test public void testExplicitGuideConfig() throws Exception {
        setTargetEnv(Canopus.Wfs.cwfs1, GsaoiOdgw.odgw1);
        setOdgw(Gsaoi.OdgwSize.SIZE_8);
        verify(Gsaoi.OdgwSize.SIZE_8);
    }

    @Test public void testNoGsaoi() throws Exception {
        setTargetEnv(Canopus.Wfs.cwfs1);
        Option<Gsaoi.OdgwSize> none = None.instance();
        verify(none);
    }

    @Test public void testNotGems() throws Exception {
        setTargetEnv(PwfsGuideProbe.pwfs1);
        Option<Gsaoi.OdgwSize> none = None.instance();
        verify(none);
    }

    @Test public void testPointOrig() throws Exception {
        verifyPointOrig(getSouthResults(), "lgs2gsaoi");
    }

    @Test
    public void testConfig() throws Exception {
        Gsaoi gsaoi = getInstrument();

        gsaoi.setIssPort(IssPort.SIDE_LOOKING);
        setInstrument(gsaoi);
        verifyInstrumentConfig(getSouthResults(), "GSAOI5");

        gsaoi.setIssPort(IssPort.UP_LOOKING);
        setInstrument(gsaoi);
        verifyInstrumentConfig(getSouthResults(), "GSAOI");
    }
}
