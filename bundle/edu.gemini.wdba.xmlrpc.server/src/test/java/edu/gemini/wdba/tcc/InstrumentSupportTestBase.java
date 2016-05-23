//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.gems.Gems;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.telescope.IssPort;
import edu.gemini.spModel.telescope.IssPortProvider;
import edu.gemini.spModel.util.SPTreeUtil;
import org.dom4j.Document;
import org.dom4j.Element;
import org.junit.Assert;
import org.junit.Before;

/**
 * Test cases for {@link edu.gemini.wdba.tcc.Flamingos2Support}.
 */
public abstract class InstrumentSupportTestBase<T extends ISPDataObject> extends TestBase {

    protected ISPObsComponent obsComp;
    protected SPComponentType instrumentType;

    protected InstrumentSupportTestBase(SPComponentType instrumentType) {
        this.instrumentType = instrumentType;
    }

    @Before public void setUp() throws Exception {
        super.setUp();

        // Add an instrument component to the observation.
        // Now, we need to add the instrument itself or the guide targets are
        // not enabled and not sent to the TCC.
        obsComp = odb.getFactory().createObsComponent(prog, instrumentType, null);
        obs.addObsComponent(obsComp);
    }

    protected class ObsComponentPair<D extends ISPDataObject> {
        public ISPObsComponent obsComp;
        public D dataObject;

        public ObsComponentPair(SPComponentType type) throws Exception {
            obsComp = odb.getFactory().createObsComponent(prog, type, null);
            obs.addObsComponent(obsComp);
            //noinspection unchecked
            dataObject = (D) obsComp.getDataObject();
        }

        public void store() throws Exception {
            obsComp.setDataObject(dataObject);
        }
    }

    protected TargetEnvironment getTargetEnvironment() {
        final ISPObsComponent oc = SPTreeUtil.findTargetEnvNode(obs);
        @SuppressWarnings("ConstantConditions")
        final TargetObsComp toc  = (TargetObsComp) oc.getDataObject();
        return toc.getTargetEnvironment();
    }

    protected void setTargetEnvironment(TargetEnvironment env) {
        final ISPObsComponent oc = SPTreeUtil.findTargetEnvNode(obs);
        @SuppressWarnings("ConstantConditions")
        final TargetObsComp toc  = (TargetObsComp) oc.getDataObject();
        toc.setTargetEnvironment(env);
        oc.setDataObject(toc);
    }

    protected void addGuideStar(GuideProbe probe) {
        TargetEnvironment env = getTargetEnvironment();
        GuideGroup        grp = env.getPrimaryGuideGroup();
        if (grp.isAutomatic()) {
            grp = GuideGroup.create(GuideGroup.ManualGroupDefaultName());
            env = env.setGuideEnvironment(env.getGuideEnvironment().setOptions(env.getGroups().append(grp))).setPrimaryGuideGroup(grp);
        }
        setTargetEnvironment(env.putPrimaryGuideProbeTargets(GuideProbeTargets.create(probe, new SPTarget())));
    }

    protected ObsComponentPair<InstAltair> addAltair() throws Exception {
        return new ObsComponentPair<>(InstAltair.SP_TYPE);
    }

    protected ObsComponentPair<InstAltair> addAltair(AltairParams.GuideStarType type) throws Exception {
        return addAltair(type == AltairParams.GuideStarType.NGS ? AltairParams.Mode.NGS : AltairParams.Mode.LGS);
    }

    protected ObsComponentPair<InstAltair> addAltair(AltairParams.Mode mode) throws Exception {
        ObsComponentPair<InstAltair> p = addAltair();
        p.dataObject.setMode(mode);
        p.store();
        return p;
    }

    protected ObsComponentPair<Gems> addGems() throws Exception {
        return new ObsComponentPair<>(Gems.SP_TYPE);
    }

    protected T getInstrument() throws Exception {
        //noinspection unchecked
        return (T) obsComp.getDataObject();
    }

    protected void setInstrument(T dataObj) throws Exception {
        obsComp.setDataObject(dataObj);
    }

    protected String getInstrumentConfig(Document doc) throws Exception {
        return getTcsConfigurationMap(doc).get(TccNames.INSTRUMENT);
    }

    protected String getInstrumentChop(Document doc) throws Exception {
        return getTcsConfigurationMap(doc).get(TccNames.CHOP);
    }

    protected String getPointOrig(Document doc) throws Exception {
        return getTcsConfigurationMap(doc).get(TccNames.POINT_ORIG);
    }

    protected void verifyPointOrig(Document doc, String expected) throws Exception {
        String actual = getPointOrig(doc);
        Assert.assertEquals(expected, actual);
    }

    protected void verifyInstrumentConfig(Document doc, String expected) throws Exception {
        String actual = getInstrumentConfig(doc);
        Assert.assertEquals("Instrument config mismatch", expected, actual);
    }

    protected void verifyInstrumentChopConfig(Document doc, String expected) throws Exception {
        String actual = getInstrumentChop(doc);
        Assert.assertEquals("Instrument chop config mismatch", expected, actual);
    }

    protected String getWavelength(Document doc) throws Exception {
        Element tccFieldConfig = getTccFieldConfig(doc);
        String path = "//param[@name='wavelength']";

        Element wavelength = (Element) tccFieldConfig.selectSingleNode(path);
        return wavelength.attributeValue("value");
    }

    protected void setPort(IssPort port) throws Exception {
        IssPortProvider pp = (IssPortProvider) obsComp.getDataObject();
        pp.setIssPort(port);
        obsComp.setDataObject((ISPDataObject) pp);
    }

}
