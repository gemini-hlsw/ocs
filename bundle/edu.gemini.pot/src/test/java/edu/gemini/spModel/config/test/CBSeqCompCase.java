// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: CBSeqCompCase.java 27568 2010-10-25 18:03:42Z swalker $
//

package edu.gemini.spModel.config.test;

import edu.gemini.spModel.config.HelperSeqCompCB;
import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.data.config.DefaultConfig;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.ISysConfig;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;


public class CBSeqCompCase extends CBSeqCompTestBase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() {
        super.tearDown();
    }


    @Test
    public void testNoReset() throws Exception {
        assertNotNull("expected a parameter builder", testCB);

        // First see what happens when we try to set without first reseting.
        try {
            testCB.applyNext(new DefaultConfig(), new DefaultConfig());
            fail("Failure to reset should have throw an exception.");
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    @Test
    public void testEmptySeqComp() throws Exception {
        assertNotNull("expected a parameter builder", testCB);

        testCB.reset(null);
        assertTrue("should have no configurations in empty seq comp", !testCB.hasNext());
    }

    @Test
    public void testSingleParam() throws Exception {
        ISysConfig sc = new DefaultSysConfig("set0");
        _putParameterValue(sc, "param0", "value0");

        TestDataObject dataObj = (TestDataObject) testSeqComp.getDataObject();
        dataObj.setSysConfig(sc);
        testSeqComp.setDataObject(dataObj);

        // Create the one expected configuration.
        IConfig conf = new DefaultConfig();
        _putConfigParameterValue(conf, "set0", "param0", "value0");

        // Put it in the list of expected configurations.
        List<IConfig> confList = new LinkedList<IConfig>();
        confList.add(conf);

        // Run the test.
        runApply(confList);
    }


    @Test
    public void testSingleSeqParam() throws Exception {
        List<String> valueList = new LinkedList<String>();
        valueList.add("value0");
        valueList.add("value1");
        valueList.add("value2");

        ISysConfig sc = new DefaultSysConfig("set0");
        _putParameterValue(sc, "param0", valueList);

        TestDataObject dataObj = (TestDataObject) testSeqComp.getDataObject();
        dataObj.setSysConfig(sc);
        testSeqComp.setDataObject(dataObj);


        // Create the one expected configurations.
        IConfig conf0 = new DefaultConfig();
        _putConfigParameterValue(conf0, "set0", "param0", "value0");

        IConfig conf1 = new DefaultConfig();
        _putConfigParameterValue(conf1, "set0", "param0", "value1");

        IConfig conf2 = new DefaultConfig();
        _putConfigParameterValue(conf2, "set0", "param0", "value2");

        // Put them in the list of expected configurations.
        List<IConfig> confList = new LinkedList<IConfig>();
        confList.add(conf0);
        confList.add(conf1);
        confList.add(conf2);

        // Run the test.
        runApply(confList);
    }

    @Test
    public void testMultipleSeqParam() throws Exception {
        List<String> valueList0 = new LinkedList<String>();
        valueList0.add("value0.0");
        valueList0.add("value0.1");

        List<String> valueList1 = new LinkedList<String>();
        valueList1.add("value1.0");
        valueList1.add("value1.1");

        ISysConfig sc = new DefaultSysConfig("set0");
        _putParameterValue(sc, "param0", valueList0);
        _putParameterValue(sc, "param1", valueList1);

        TestDataObject dataObj = (TestDataObject) testSeqComp.getDataObject();
        dataObj.setSysConfig(sc);
        testSeqComp.setDataObject(dataObj);

        // Create the one expected configurations.
        IConfig conf0 = new DefaultConfig();
        _putConfigParameterValue(conf0, "set0", "param0", "value0.0");
        _putConfigParameterValue(conf0, "set0", "param1", "value1.0");

        IConfig conf1 = new DefaultConfig();
        _putConfigParameterValue(conf1, "set0", "param0", "value0.1");
        _putConfigParameterValue(conf1, "set0", "param1", "value1.1");

        // Put them in the list of expected configurations.
        List<IConfig> confList = new LinkedList<IConfig>();
        confList.add(conf0);
        confList.add(conf1);

        // Run the test.
        runApply(confList);
    }

    @Test
    public void testMultipleUnevenSeqParam() throws Exception {
        List<String> valueList0 = new LinkedList<String>();
        valueList0.add("value0.0");
        valueList0.add("value0.1");

        // valueList 1 will have one more element than list 0
        List<String> valueList1 = new LinkedList<String>();
        valueList1.add("value1.0");
        valueList1.add("value1.1");
        valueList1.add("value1.2");

        ISysConfig sc = new DefaultSysConfig("set0");
        _putParameterValue(sc, "param0", valueList0);
        _putParameterValue(sc, "param1", valueList1);

        TestDataObject dataObj = (TestDataObject) testSeqComp.getDataObject();
        dataObj.setSysConfig(sc);
        testSeqComp.setDataObject(dataObj);


        // Create the one expected configurations.
        IConfig conf0 = new DefaultConfig();
        _putConfigParameterValue(conf0, "set0", "param0", "value0.0");
        _putConfigParameterValue(conf0, "set0", "param1", "value1.0");

        IConfig conf1 = new DefaultConfig();
        _putConfigParameterValue(conf1, "set0", "param0", "value0.1");
        _putConfigParameterValue(conf1, "set0", "param1", "value1.1");

        IConfig conf2 = new DefaultConfig();
        _putConfigParameterValue(conf2, "set0", "param1", "value1.2");

        // Put them in the list of expected configurations.
        List<IConfig> confList = new LinkedList<IConfig>();
        confList.add(conf0);
        confList.add(conf1);
        confList.add(conf2);

        // Run the test.
        runApply(confList);
    }

    @Test
    public void testSimpleMultipleUnevenSeqParam() throws Exception {
        // Replace the cb
        HelperSeqCompCB cb = new HelperSeqCompCB(testSeqComp);
        testSeqComp.putClientData(IConfigBuilder.USER_OBJ_KEY, cb);
        testMultipleUnevenSeqParam();
    }
}

