//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.ext.ObservationNode;
import edu.gemini.spModel.ext.TargetNode;
import edu.gemini.spModel.gemini.altair.AltairAowfsGuider;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.gems.Canopus;
import edu.gemini.spModel.gemini.gems.Gems;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.*;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import org.dom4j.Document;
import org.dom4j.Element;

import java.util.*;

/**
 * Test code for the {@link TargetGroupConfig}
 *
 * The property HLPG_PROJECT_BASE must be set to OCS installation dir.
 */
public final class TargetGroupTest extends TestBase {

    // Used to hold unnamed targets and guide probes whose key doesn't match
    // the value sent to the TCC.  Unnamed targets are given a name before they
    // are sent to the TCC, and some guide probes have keys that don't match
    // what is expected by the TCC.  The nameMap should store these
    // discrepancies so that we can validate the XML against what we expect.

    private NameMap nameMap;
    private SPTarget base, pwfs1_1, pwfs1_2, pwfs2_1, pwfs2_2;

    protected void setUp() throws Exception {
        super.setUp();
        nameMap = new NameMap();

        base = new SPTarget();
        base.getTarget().setName("Base Pos");

        pwfs1_1 = new SPTarget();
        pwfs1_1.getTarget().setName("PWFS1-1");
        pwfs1_2 = new SPTarget();
        pwfs1_2.getTarget().setName("PWFS1-2");

        pwfs2_1 = new SPTarget();
        pwfs2_1.getTarget().setName("PWFS2-1");
        pwfs2_2 = new SPTarget();
        pwfs2_2.getTarget().setName("PWFS2-2");
    }

    /**
     * Single unamed base position.
     */
    public void testUnamedBase() throws Exception {
        SPTarget base = new SPTarget();
        nameMap.putTargetName(base, TccNames.BASE);
        testTargetEnvironment(TargetEnvironment.create(base));
    }

    /**
     * Single unamed base position.
     */
    public void testNamedBase() throws Exception {
        testTargetEnvironment(TargetEnvironment.create(base));
    }

    /**
     * An unamed base and user target.
     */
    public void testUnamedBaseAndUser() throws Exception {
        SPTarget base = new SPTarget();
        SPTarget user = new SPTarget();
        ImList<SPTarget> userTargets = ImCollections.singletonList(user);

        nameMap.putTargetName(base, TccNames.BASE);
        nameMap.putTargetName(user, "User (1)");
        testTargetEnvironment(TargetEnvironment.create(base).setUserTargets(userTargets));
    }

    private void testSingleGuider(int primary, SPTarget... targets) throws Exception {

        ImList<SPTarget> targetList = ImCollections.emptyList();
        for (int i=targets.length-1; i>=0; --i) {
            targetList = targetList.cons(targets[i]);
        }

        GuideProbeTargets gt = GuideProbeTargets.create(PwfsGuideProbe.pwfs2, targetList);

        ImList<GuideProbeTargets> gtCollection = DefaultImList.create(gt);
        ImList<SPTarget> userTargets = ImCollections.emptyList();

        TargetEnvironment env;
        env = TargetEnvironment.create(base).setAllPrimaryGuideProbeTargets(gtCollection).setUserTargets(userTargets);
        env = env.addActive(PwfsGuideProbe.pwfs2);

        testTargetEnvironment(env);
    }

    /**
     * A base position and single guide target for single guider.
     */
    public void testSingleGuideTargetSingleGuider() throws Exception {
        testSingleGuider(1, pwfs2_1);
    }

    /**
     * A base position and multiple guide targets for a single guider.
     */
    public void testMultipleGuideTargetsSingleGuider() throws Exception {
        testSingleGuider(1, pwfs2_1, pwfs2_2);
    }

    /**
     * Using the second guide star as the primary wfs star.
     */
    public void testNonDefaultPrimary() throws Exception {
        testSingleGuider(2, pwfs2_1, pwfs2_2);
    }

    /**
     * Having no primary guide star.
     */
    public void testNoExplicitPrimary() throws Exception {
        testSingleGuider(-1, pwfs2_1, pwfs2_2);
    }

    /**
     * Disabled guide targets.
     */
    public void testDisabledGuideTargets() throws Exception {
        SPTarget guide1 = new SPTarget();
        guide1.getTarget().setName("OIWFS-1");
        SPTarget guide2 = new SPTarget();
        guide2.getTarget().setName("OIWFS-2");

        ImList<SPTarget> targetList;
        targetList = ImCollections.singletonList(guide2).cons(guide1);

        GuideProbeTargets gt = GuideProbeTargets.create(GmosOiwfsGuideProbe.instance, targetList).setPrimaryIndex(1);
        nameMap.putGuiderName(GmosOiwfsGuideProbe.instance, "OIWFS");

        ImList<GuideProbeTargets> gtCollection = DefaultImList.create(gt);
        ImList<SPTarget> userTargets = ImCollections.emptyList();

        TargetEnvironment env;
        env = TargetEnvironment.create(base).setAllPrimaryGuideProbeTargets(gtCollection).setUserTargets(userTargets);

        testTargetEnvironment(env);
    }

    public void testMultipleGuiders() throws Exception {

        // Create the target environment with multiple guiders.
        ImList<SPTarget> targetList = ImCollections.singletonList(pwfs1_2).cons(pwfs1_1);
        GuideProbeTargets pwfs1 = GuideProbeTargets.create(PwfsGuideProbe.pwfs1, targetList);

        targetList = ImCollections.singletonList(pwfs2_2).cons(pwfs2_1);
        GuideProbeTargets pwfs2 = GuideProbeTargets.create(PwfsGuideProbe.pwfs2, targetList);

        TargetEnvironment env = TargetEnvironment.create(base);
        env = env.putPrimaryGuideProbeTargets(pwfs1).putPrimaryGuideProbeTargets(pwfs2);
        env = env.addActive(PwfsGuideProbe.pwfs1).addActive(PwfsGuideProbe.pwfs2);

        testTargetEnvironment(env);
    }

    public void testNoPrimary() throws Exception {

        // Create the target environment with multiple guiders.
        ImList<SPTarget> targetList = ImCollections.singletonList(pwfs1_2).cons(pwfs1_1);
        GuideProbeTargets pwfs1 = GuideProbeTargets.create(PwfsGuideProbe.pwfs1, targetList);

        targetList = ImCollections.singletonList(pwfs2_2).cons(pwfs2_1);
        GuideProbeTargets pwfs2 = GuideProbeTargets.create(PwfsGuideProbe.pwfs2, targetList).setPrimaryIndex(None.INTEGER);

        TargetEnvironment env = TargetEnvironment.create(base);
        env = env.putPrimaryGuideProbeTargets(pwfs1).putPrimaryGuideProbeTargets(pwfs2);
        env = env.addActive(PwfsGuideProbe.pwfs1).addActive(PwfsGuideProbe.pwfs2);

        testTargetEnvironment(env);
    }

    public void testOiwfsMapping() throws Exception {
        // Create a target environment that uses an instrument OIWFS.
        SPTarget oiwfsTarget = new SPTarget();

        ImList<SPTarget> targetList = ImCollections.singletonList(oiwfsTarget);
        GuideProbeTargets gt = GuideProbeTargets.create(GmosOiwfsGuideProbe.instance, targetList);

        TargetEnvironment env = TargetEnvironment.create(base).putPrimaryGuideProbeTargets(gt);
        env = env.addActive(GmosOiwfsGuideProbe.instance);

        // Now, we need to add the instrument itself or the guide targets are
        // not enabled and not sent to the TCC.
        ISPObsComponent gmosComp;
        gmosComp = odb.getFactory().createObsComponent(prog, InstGmosSouth.SP_TYPE, null);
        obs.addObsComponent(gmosComp);

        // Now do the test.  The GMOS OIWFS is mapped to just "OIWFS".  The
        // target name then defaults to "OIWFS (1)"
        nameMap.putGuiderName(GmosOiwfsGuideProbe.instance, "OIWFS");
        nameMap.putTargetName(oiwfsTarget, "OIWFS (1)");
        testTargetEnvironment(env);
    }

    public void testGsaoiOdgwMapping() throws Exception {
        // Create a target environment that uses an instrument OIWFS.
        SPTarget odgwTarget = new SPTarget();

        ImList<SPTarget> targetList = ImCollections.singletonList(odgwTarget);
        GuideProbeTargets gt = GuideProbeTargets.create(GsaoiOdgw.odgw1, targetList);

        TargetEnvironment env = TargetEnvironment.create(base).putPrimaryGuideProbeTargets(gt);
        env = env.addActive(GsaoiOdgw.odgw1);

        // Now, we need to add the instrument itself or the guide targets are
        // not enabled and not sent to the TCC.
        ISPObsComponent gsaoiComp;
        gsaoiComp = odb.getFactory().createObsComponent(prog, Gsaoi.SP_TYPE, null);
        obs.addObsComponent(gsaoiComp);

        // Now do the test.  The GSAOI OIWFS keys do not get mapped.  They
        // are ODGW1, ODGW2, etc.
        // So the target name then defaults to "ODGW1 (1)"
        nameMap.putTargetName(odgwTarget, "ODGW1 (1)");
        testTargetEnvironment(env);
    }

    public void testAowfsMapping() throws Exception {
        // Create a target environment that uses the altair AOWFS.
        SPTarget aowfsTarget = new SPTarget();

        ImList<SPTarget> targetList = ImCollections.singletonList(aowfsTarget);
        GuideProbeTargets gt = GuideProbeTargets.create(AltairAowfsGuider.instance, targetList);

        TargetEnvironment env = TargetEnvironment.create(base).putPrimaryGuideProbeTargets(gt);
        env = env.addActive(AltairAowfsGuider.instance);

        // Now, we need to add Altair or the guide targets are
        // not enabled and not sent to the TCC.
        ISPObsComponent gmosComp;
        gmosComp = odb.getFactory().createObsComponent(prog, InstAltair.SP_TYPE, null);
        obs.addObsComponent(gmosComp);

        // Now do the test.  The "Altair AOWFS" is mapped to just "AOWFS".  The
        // target name then defaults to "AOWFS (1)"
        nameMap.putGuiderName(AltairAowfsGuider.instance, "AOWFS");
        nameMap.putTargetName(aowfsTarget, "AOWFS (1)");
        testTargetEnvironment(env);
    }

    public void testGemsMapping() throws Exception {
        // Create a target environment that uses Gems canopus wfs.
        SPTarget cwfsTarget = new SPTarget();

        ImList<SPTarget> targetList = ImCollections.singletonList(cwfsTarget);
        GuideProbeTargets gt = GuideProbeTargets.create(Canopus.Wfs.cwfs1, targetList);

        TargetEnvironment env = TargetEnvironment.create(base).putPrimaryGuideProbeTargets(gt);
        env = env.addActive(Canopus.Wfs.cwfs1);

        // Now, we need to add Gems or the guide targets are
        // not enabled and not sent to the TCC.
        ISPObsComponent gemsComp;
        gemsComp = odb.getFactory().createObsComponent(prog, Gems.SP_TYPE, null);
        obs.addObsComponent(gemsComp);

        // Now do the test.  The CWFS? keys do not get mapped.  They
        // are CWFS1, CWFS2, etc.
        // So the target name then defaults to "CWFS1 (1)"
        nameMap.putTargetName(cwfsTarget, "CWFS1 (1)");
        testTargetEnvironment(env);
    }


    public void testDefaultGroupName() throws Exception {
        ImList<SPTarget> targetList1 = ImCollections.singletonList(pwfs1_1);
        GuideProbeTargets pwfs1_1 = GuideProbeTargets.create(PwfsGuideProbe.pwfs1, targetList1);
        ImList<GuideProbeTargets> gpt1 = ImCollections.singletonList(pwfs1_1);

        ImList<SPTarget> targetList2 = ImCollections.singletonList(pwfs1_2);
        GuideProbeTargets pwfs1_2 = GuideProbeTargets.create(PwfsGuideProbe.pwfs1, targetList2);
        ImList<GuideProbeTargets> gpt2 = ImCollections.singletonList(pwfs1_2);

        // grp1 -> "Explict Name"
        // grp2 -> unnamed
        GuideGroup grp1 = GuideGroup.create("Explicit Name", gpt1);
        GuideGroup grp2 = GuideGroup.create(None.STRING, gpt2);
        OptionsList<GuideGroup> lst = OptionsListImpl.createP(0, grp1, grp2);
        GuideEnvironment genv = GuideEnvironment.create(lst);

        // Test explicit name.
        TargetEnvironment env = TargetEnvironment.create(base).setGuideEnvironment(genv);
        testName(env, "Explicit Name");

        // Make the unamed group primary, and check that the name is defaulted.
        env = env.setGuideEnvironment(genv.setPrimaryIndex(1));
        testName(env, "Guide Group 2");
    }

    private void testName(TargetEnvironment env, String name) throws Exception {
        // Store the target environment.
        ObservationNode obsNode = getObsNode();
        TargetNode targetNode = obsNode.getTarget();

        TargetObsComp obsComp = targetNode.getDataObject();
        obsComp.setTargetEnvironment(env);
        targetNode.getRemoteNode().setDataObject(obsComp);

        // Get the results.
        Document doc = getSouthResults();
        Element e = getTccFieldConfig(doc);

        Element param = (Element) e.selectSingleNode("//param[@name='guideGroup']");
        String actualName = param.attributeValue("value");
        assertEquals(name, actualName);
    }
    // -------------

    private static class NameMap {
        private final Map<GuideProbe, String> guiderNameMap = new HashMap<GuideProbe, String>();
        private final Map<SPTarget, String> targetNameMap = new HashMap<SPTarget, String>();

        public String getGuiderName(GuideProbe guider) {
            String name = guiderNameMap.get(guider);
            return (name == null) ? guider.getKey() : name;
        }

        public void putGuiderName(GuideProbe guider, String name) {
            guiderNameMap.put(guider, name);
        }

        public String getTargetName(SPTarget target) {
            String name = targetNameMap.get(target);
            return (name == null) ? target.getTarget().getName() : name;
        }

        public void putTargetName(SPTarget target, String name) {
            targetNameMap.put(target, name);
        }
    }

    private static String DEFAULT_PARAM_PATH = "param[@name='" + TccNames.PRIMARY + "']";
    private static String TARGETS_PATH = "param[@name='" + TccNames.TARGETS + "']";

    private void validateGroup(Element targetGroup, String groupName, String defaultTarget, ImList<SPTarget> targets) {
        // Check the group name.
        assertEquals(groupName, targetGroup.attributeValue(ParamSet.NAME));

        // Check the default target value.
        Element defaultParam = (Element) targetGroup.selectSingleNode(DEFAULT_PARAM_PATH);
        if (defaultTarget == null) {
            assertNull(defaultParam);
        } else {
            assertEquals(defaultTarget, defaultParam.attributeValue(ParamSet.VALUE));
        }

        // Check the names of the targets in the group.
        Element targetsParam = (Element) targetGroup.selectSingleNode(TARGETS_PATH);
        if (targets.size() == 1) {
            // Only one target in the group, so make sure it is the value of
            // the parameter.
            String val = targetsParam.attributeValue(ParamSet.VALUE);
            assertEquals(nameMap.getTargetName(targets.get(0)), val);

            // And that there are no nested value elements.
            List lst = targetsParam.elements(ParamSet.VALUE);
            assertTrue((lst == null) || (lst.size() == 0));
        } else {
            // There are multiple elements, so make sure that they are
            // represented in multiple nested value elements.
            assertNull(targetsParam.attributeValue(ParamSet.VALUE));

            @SuppressWarnings({"unchecked"}) List<Element> children = targetsParam.elements(ParamSet.VALUE);
            assertEquals(targets.size(), children.size());

            for (int i=0; i<targets.size(); ++i) {
                assertEquals(nameMap.getTargetName(targets.get(i)), children.get(i).getTextTrim());
            }
        }
    }

    private static Element getGroupElement(String name, List<Element> groupElements) {
        for (Element groupElement : groupElements) {
            String thisName = groupElement.attributeValue(ParamSet.NAME);
            if (name.equals(thisName)) return groupElement;
        }
        return null;
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

        // Count the number of targets (discounting the disabled guide targets)
        int targetCount = 1 + env.getUserTargets().size(); // base position + user
        int groupCount = 1; // base group
        for (GuideProbeTargets gt : env.getOrCreatePrimaryGuideGroup()) {
            if (!env.isActive(gt.getGuider())) continue;
            ++groupCount;
            targetCount += gt.getOptions().size();
        }
        assertEquals(targetCount, targetElements.size());

        for (Element targetElement : targetElements) {
            final String name = targetElement.attributeValue(ParamSet.NAME);
            assertNotNull(name);

            Option<SPTarget> target = env.getTargets().find(new PredicateOp<SPTarget>() {
                public Boolean apply(SPTarget spTarget) {
                    String targetName = nameMap.getTargetName(spTarget);
                    return name.equals(targetName);
                }
            });

            assertFalse(target.isEmpty());
        }

        // There should be one group for each guider, plus one for the base
        // and user targets.
        List<Element> groupElements = getTargetGroups(doc);
        assertEquals(groupCount, groupElements.size());

        // Check the base element.
        Element baseGroupElement = getGroupElement(TccNames.BASE, groupElements);
        assertNotNull(baseGroupElement);
        ImList<SPTarget> targets = env.getUserTargets();
        targets = targets.cons(env.getBase());

        String baseName = nameMap.getTargetName(env.getBase());
        validateGroup(baseGroupElement, TccNames.BASE, baseName, targets);

        // Check each guide group.
        for (GuideProbeTargets gt : env.getOrCreatePrimaryGuideGroup()) {
            GuideProbe guider = gt.getGuider();
            String name = nameMap.getGuiderName(guider);

            Element guideGroupElement = getGroupElement(name, groupElements);
            if (!env.isActive(guider) || (gt.getOptions().size() == 0)) {
                assertNull(guideGroupElement);
                continue;
            }

            assertNotNull(guideGroupElement);

            Option<SPTarget> primary = gt.getPrimary();
//            if (primary == null) primary = gt.imList().head();
            String primaryName = null;
            if (!primary.isEmpty()) primaryName = nameMap.getTargetName(primary.getValue());
            validateGroup(guideGroupElement, name, primaryName, gt.getOptions());
        }
    }

    protected static GuideProbeTargets createGuideTargets(GuideProbe guider, String... names) {
        ImList<SPTarget> targets = ImCollections.emptyList();
        for (int i=names.length-1; i>=0; --i) {
            SPTarget target = new SPTarget();
            target.getTarget().setName(names[i]);
            target.notifyOfGenericUpdate();
            targets = targets.cons(target);
        }
        return GuideProbeTargets.create(guider, targets).setPrimaryIndex(1);
    }
}