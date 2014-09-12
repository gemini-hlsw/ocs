// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: CBObsCompCase.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.config.test;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.config.HelperObsCompCB;
import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.data.config.DefaultConfig;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;


public class CBObsCompCase extends CBTestBase {

    protected ISPObsComponent testObsComp;
    protected IConfigBuilder testCB;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        ISysConfig sc = new DefaultSysConfig("testPS");
        testObsComp = createObsComponent(SPComponentType.UNKNOWN, sc);

        testCB = (IConfigBuilder) testObsComp.getClientData(IConfigBuilder.USER_OBJ_KEY);
    }

    @After
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testNoReset()  {
        assertNotNull("Expected a parameter builder.", testCB);

        // First see what happens when we try to set without first reseting.
        try {
            testCB.applyNext(new DefaultConfig(), new DefaultConfig());
            fail("Failure to reset should have throw an exception.");
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    @Test
    public void testEmptyObsComp()  {
        assertNotNull("Expected a parameter builder.", testCB);

        testCB.reset(null);
        assertTrue("Should have no configurations in empty obs.", !testCB.hasNext());
    }

    @Test
    public void testSingleParam()  {
        assertNotNull("expected a parameter builder", testCB);

        ISysConfig sc = new DefaultSysConfig("set0");
        _putParameterValue(sc, "param0", "value0");

        TestDataObject dataObj = (TestDataObject) testObsComp.getDataObject();
        dataObj.setSysConfig(sc);

        IConfig expected = new DefaultConfig();
        _putConfigParameterValue(expected, "set0", "param0", "value0");

        runApply(testCB, expected);
        runApply(testCB, expected);
    }

    @Test
    public void testMultipleParam()  {
        assertNotNull("expected a parameter builder", testCB);

        ISysConfig sc = new DefaultSysConfig("set0");
        _putParameterValue(sc, "param0", "value0");
        _putParameterValue(sc, "param1", "value1");
        _putParameterValue(sc, "param2", "value2");

        TestDataObject dataObj = (TestDataObject) testObsComp.getDataObject();
        dataObj.setSysConfig(sc);

        IConfig expected = new DefaultConfig();
        _putConfigParameterValue(expected, "set0", "param0", "value0");
        _putConfigParameterValue(expected, "set0", "param1", "value1");
        _putConfigParameterValue(expected, "set0", "param2", "value2");

        runApply(testCB, expected);
        runApply(testCB, expected);
    }

    @Test
    public void testSimpleMultipleParam()  {
        assertNotNull("expected a parameter builder", testCB);

        // Replace the cb
        HelperObsCompCB cb = new HelperObsCompCB(testObsComp);
        testObsComp.putClientData(IConfigBuilder.USER_OBJ_KEY, cb);

        ISysConfig sc = new DefaultSysConfig("set0");
        _putParameterValue(sc, "param0", "value0");
        _putParameterValue(sc, "param1", "value1");
        _putParameterValue(sc, "param2", "value2");

        TestDataObject dataObj = (TestDataObject) testObsComp.getDataObject();
        dataObj.setSysConfig(sc);

        IConfig expected = new DefaultConfig();
        _putConfigParameterValue(expected, "set0", "param0", "value0");
        _putConfigParameterValue(expected, "set0", "param1", "value1");
        _putConfigParameterValue(expected, "set0", "param2", "value2");

        runApply(testCB, expected);
        runApply(testCB, expected);
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
}

