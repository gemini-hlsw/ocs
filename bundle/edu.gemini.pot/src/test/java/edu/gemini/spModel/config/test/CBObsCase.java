// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: CBObsCase.java 27568 2010-10-25 18:03:42Z swalker $
//

package edu.gemini.spModel.config.test;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.config.*;
import edu.gemini.spModel.data.config.DefaultConfig;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.ISysConfig;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPSeqComponent;

import java.util.LinkedList;
import java.util.List;

import edu.gemini.spModel.target.obsComp.TargetObsCompConstants;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;


public class CBObsCase extends CBTestBase {

    protected ISPObservation testObs;
    protected IConfigBuilder testCB;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        testObs = createObservation();
        testProg.addObservation(testObs);
        testCB = (IConfigBuilder) testObs.getClientData(IConfigBuilder.USER_OBJ_KEY);
    }

    @After
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testLeafObs() throws Exception {
        assertNotNull("expected a parameter builder", testCB);

        // First see what happens when we try to set without first resetting.
        try {
            testCB.applyNext(new DefaultConfig(), new DefaultConfig());
            fail("Failure to reset should have throw an exception.");
        } catch (IllegalStateException ex) {
            // expected
        }

        testCB.reset(null);

        assertTrue(testCB.hasNext());

        final IConfig config = new DefaultConfig();
        testCB.applyNext(config, new DefaultConfig());

        final String[] sysConfigs = { TargetObsCompConstants.CONFIG_NAME, "ocs" };
        for (String name : sysConfigs) {
            assertNotNull(config.getSysConfig(name));
            config.removeSysConfig(name);
        }

        assertEquals(0, config.getParameterCount());

        assertTrue("expected no configurations in empty obs", !testCB.hasNext());
    }

    // Test an observation with a single observation component.
    @Test
    public void testObsCompObs() throws Exception {
        assertNotNull("expected a parameter builder", testCB);

        // SR: Clear the components in the observation.
        // These lines don't seem to make any difference.
        testObs.removeSeqComponent();
        testCB.reset(null);
        // /SR

        // Add an obs component.
        ISysConfig sc = new DefaultSysConfig("ocConfig");
        _putParameterValue(sc, "param0", "value0");

        ISPObsComponent obsComp = createObsComponent(SPComponentType.UNKNOWN, sc);
        testObs.addObsComponent(obsComp);

        // Create the expected configuration.
        IConfig expected = new DefaultConfig();
        _putConfigParameterValue(expected, "ocConfig", "param0", "value0");

        // Run the test.
        runApply(testCB, expected);
    }

    // Test an observation with a single sequence component.
    @Test
    public void testSeqCompObs() throws Exception {
        assertNotNull("expected a parameter builder", testCB);

        // Add a seq component.
        ISysConfig sc = new DefaultSysConfig("seqConfig");
        List<String> valueList = new LinkedList<>();
        valueList.add("value0");
        valueList.add("value1");
        _putParameterValue(sc, "param0", valueList);

        // Make a "fake root" to contain multiple children
        ISPSeqComponent root = createSeqComponent(SPComponentType.UNKNOWN);

        ISPSeqComponent seqComp = createSeqComponent(SPComponentType.UNKNOWN, sc);
        root.addSeqComponent(seqComp);
        testObs.setSeqComponent(root);

        // Create the expected configurations.
        IConfig conf0 = new DefaultConfig();
        _putConfigParameterValue(conf0, "seqConfig", "param0", "value0");

        IConfig conf1 = new DefaultConfig();
        _putConfigParameterValue(conf1, "seqConfig", "param0", "value1");

        List<IConfig> expectedConfList = new LinkedList<>();
        expectedConfList.add(conf0);
        expectedConfList.add(conf1);

        // Run the test.
        runApply(testCB, expectedConfList);
    }

    // Test an observation with a single sequence component and single
    // observation component.
    @Test
    public void testSimpleObs() throws Exception {
        assertNotNull("expected a parameter builder", testCB);


        // Add an obs component.
        ISysConfig obsCompConfig = new DefaultSysConfig("ocConfig");
        _putParameterValue(obsCompConfig, "param0", "value0");

        ISPObsComponent obsComp = createObsComponent(SPComponentType.UNKNOWN, obsCompConfig);
        testObs.addObsComponent(obsComp);


        // Add a seq component.
        ISysConfig seqCompConfig = new DefaultSysConfig("seqConfig");
        List<String> valueList = new LinkedList<>();
        valueList.add("value0");
        valueList.add("value1");
        _putParameterValue(seqCompConfig, "param0", valueList);

        // Make a "fake root" to contain multiple children
        ISPSeqComponent root = createSeqComponent(SPComponentType.UNKNOWN);

        ISPSeqComponent seqComp = createSeqComponent(SPComponentType.UNKNOWN, seqCompConfig);
        root.addSeqComponent(seqComp);
        testObs.setSeqComponent(root);


        // Create the expected configurations.
        // Note that these are added such
        IConfig conf0 = new DefaultConfig();

        // Note that these are added in "bottom up" order since
        // Put config places configs at front
        ISysConfig sc1 = new DefaultSysConfig("seqConfig");
        _putParameterValue(sc1, "param0", "value0");
        conf0.putSysConfig(sc1);

        ISysConfig sc2 = new DefaultSysConfig("ocConfig");
        _putParameterValue(sc2, "param0", "value0");
        conf0.putSysConfig(sc2);

        IConfig conf1 = new DefaultConfig();
        _putConfigParameterValue(conf1, "seqConfig", "param0", "value1");

        List<IConfig> expectedConfList = new LinkedList<>();
        expectedConfList.add(conf0);
        expectedConfList.add(conf1);


        // Run the test.
        runApply(testCB, expectedConfList);
    }

    // Test an observation with a single sequence component and single
    // observation component.  This one adds multiple parameters.
    @Test
    public void test2SimpleObs() throws Exception {
        assertNotNull("expected a parameter builder", testCB);


        // Add an obs component.
        ISysConfig obsCompConfig = new DefaultSysConfig("ocConfig");
        _putParameterValue(obsCompConfig, "param0", "value0");
        _putParameterValue(obsCompConfig, "param1", "value1");

        ISPObsComponent obsComp = createObsComponent(SPComponentType.UNKNOWN, obsCompConfig);
        testObs.addObsComponent(obsComp);


        // Add a seq component.
        ISysConfig seqCompConfig = new DefaultSysConfig("seqConfig");
        List<String> valueList = new LinkedList<>();
        valueList.add("value0");
        valueList.add("value1");
        _putParameterValue(seqCompConfig, "param0", valueList);
        valueList = new LinkedList<String>();
        valueList.add("nvalue0");
        valueList.add("nvalue1");
        valueList.add("nvalue2");
        _putParameterValue(seqCompConfig, "param1", valueList);

        // Make a "fake root" to contain multiple children
        ISPSeqComponent root = createSeqComponent(SPComponentType.UNKNOWN);

        ISPSeqComponent seqComp = createSeqComponent(SPComponentType.UNKNOWN, seqCompConfig);
        root.addSeqComponent(seqComp);
        testObs.setSeqComponent(root);

        // Create the expected configurations.  Note ocConfig is "first"
        IConfig conf0 = new DefaultConfig();
        ISysConfig sc1 = new DefaultSysConfig("ocConfig");
        _putParameterValue(sc1, "param0", "value0");
        _putParameterValue(sc1, "param1", "value1");
        ISysConfig sc2 = new DefaultSysConfig("seqConfig");
        _putParameterValue(sc2, "param0", "value0");
        _putParameterValue(sc2, "param1", "nvalue0");
        conf0.appendSysConfig(sc1);
        conf0.appendSysConfig(sc2);

        IConfig conf1 = new DefaultConfig();
        _putConfigParameterValue(conf1, "seqConfig", "param0", "value1");
        _putConfigParameterValue(conf1, "seqConfig", "param1", "nvalue1");

        IConfig conf2 = new DefaultConfig();
        _putConfigParameterValue(conf2, "seqConfig", "param1", "nvalue2");

        List<IConfig> expectedConfList = new LinkedList<>();
        expectedConfList.add(conf0);
        expectedConfList.add(conf1);
        expectedConfList.add(conf2);

        // Run the test.
        runApply(testCB, expectedConfList);
    }

    // Test an observation with a single sequence component and single
    // observation component.  This one adds multiple parameters and
    // uses the HelperCB.
    @Test
    public void testHelper2SimpleObs() throws Exception {
        assertNotNull("expected a parameter builder", testCB);

        // Add an obs component.
        ISysConfig obsCompConfig = new DefaultSysConfig("ocConfig");
        _putParameterValue(obsCompConfig, "param0", "value0");
        _putParameterValue(obsCompConfig, "param1", "value1");

        ISPObsComponent obsComp = createObsComponent(SPComponentType.UNKNOWN, obsCompConfig);
        // Replace the cb
        HelperObsCompCB occb = new HelperObsCompCB(obsComp);
        obsComp.putClientData(IConfigBuilder.USER_OBJ_KEY, occb);
        testObs.addObsComponent(obsComp);

        // Add a seq component.
        ISysConfig seqCompConfig = new DefaultSysConfig("seqConfig");
        List<String> valueList = new LinkedList<>();
        valueList.add("value0");
        valueList.add("value1");
        _putParameterValue(seqCompConfig, "param0", valueList);
        valueList = new LinkedList<String>();
        valueList.add("nvalue0");
        valueList.add("nvalue1");
        valueList.add("nvalue2");
        _putParameterValue(seqCompConfig, "param1", valueList);

        // Make a "fake root" to contain multiple children
        ISPSeqComponent root = createSeqComponent(SPComponentType.UNKNOWN);

        ISPSeqComponent seqComp = createSeqComponent(SPComponentType.UNKNOWN, seqCompConfig);
        HelperSeqCompCB sccb = new HelperSeqCompCB(seqComp);
        seqComp.putClientData(IConfigBuilder.USER_OBJ_KEY, sccb);

        root.addSeqComponent(seqComp);
        testObs.setSeqComponent(root);

        // Create the expected configurations.
        IConfig conf0 = new DefaultConfig();
        ISysConfig sc1 = new DefaultSysConfig("ocConfig");
        _putParameterValue(sc1, "param0", "value0");
        _putParameterValue(sc1, "param1", "value1");
        ISysConfig sc2 = new DefaultSysConfig("seqConfig");
        _putParameterValue(sc2, "param0", "value0");
        _putParameterValue(sc2, "param1", "nvalue0");
        conf0.appendSysConfig(sc1);
        conf0.appendSysConfig(sc2);

        IConfig conf1 = new DefaultConfig();
        _putConfigParameterValue(conf1, "seqConfig", "param0", "value1");
        _putConfigParameterValue(conf1, "seqConfig", "param1", "nvalue1");

        IConfig conf2 = new DefaultConfig();
        _putConfigParameterValue(conf2, "seqConfig", "param1", "nvalue2");

        List<IConfig> expectedConfList = new LinkedList<>();
        expectedConfList.add(conf0);
        expectedConfList.add(conf1);
        expectedConfList.add(conf2);


        // Run the test.
        runApply(testCB, expectedConfList);
    }

    // Test an observation with a single sequence component and single
    // observation component where the sequence component overrides the
    // static configuration in the obs component.
    @Test
    public void testOverrideObs() throws Exception {
        assertNotNull("expected a parameter builder", testCB);


        // Add an obs component.
        ISysConfig obsCompConfig = new DefaultSysConfig("niri");
        _putParameterValue(obsCompConfig, "filter", "red");

        ISPObsComponent obsComp = createObsComponent(SPComponentType.UNKNOWN, obsCompConfig);
        testObs.addObsComponent(obsComp);


        // Add a seq component.
        ISysConfig seqCompConfig = new DefaultSysConfig("niri");
        List<String> valueList = new LinkedList<>();
        valueList.add("green");
        valueList.add("blue");
        _putParameterValue(seqCompConfig, "filter", valueList);

        // Make a "fake root" to contain multiple children
        ISPSeqComponent root = createSeqComponent(SPComponentType.UNKNOWN);

        ISPSeqComponent seqComp = createSeqComponent(SPComponentType.UNKNOWN, seqCompConfig);
        root.addSeqComponent(seqComp);
        testObs.setSeqComponent(root);

        // Create the expected configurations.
        IConfig conf0 = new DefaultConfig();
        // conf0.putParameterValue("niri", "filter", "red");  <-- overriden
        _putConfigParameterValue(conf0, "niri", "filter", "green");

        IConfig conf1 = new DefaultConfig();
        _putConfigParameterValue(conf1, "niri", "filter", "blue");

        List<IConfig> expectedConfList = new LinkedList<>();
        expectedConfList.add(conf0);
        expectedConfList.add(conf1);


        // Run the test.
        runApply(testCB, expectedConfList);
    }

    public ISPObsComponent createObsComponent(SPComponentType compType, ISysConfig sc) throws Exception {
        // Create the observation component.
        ISPObsComponent obsComp = createObsComponent(compType);

        // Create and add the test data object.
        TestDataObject tdo = new TestDataObject();
        tdo.setSysConfig(sc);
        obsComp.setDataObject(tdo);

        // Add the correct parameter builder.
        TestDataObjectObsCompCB cb = new TestDataObjectObsCompCB(obsComp);
        obsComp.putClientData(IConfigBuilder.USER_OBJ_KEY, cb);
        return obsComp;
    }

    public ISPSeqComponent createSeqComponent(SPComponentType compType, ISysConfig sc) throws Exception {
        // Create the observation component.
        ISPSeqComponent seqComp = createSeqComponent(compType);

        // Create and add the test data object.
        TestDataObject tdo = new TestDataObject();
        tdo.setSysConfig(sc);
        seqComp.setDataObject(tdo);

        // Add the correct parameter builder.
        TestDataObjectSeqCompCB cb = new TestDataObjectSeqCompCB(seqComp);
        seqComp.putClientData(IConfigBuilder.USER_OBJ_KEY, cb);
        return seqComp;
    }
}

