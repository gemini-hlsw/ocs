// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: CBNestedSeqCompCase.java 7805 2007-05-19 21:11:32Z swalker $
//

package edu.gemini.spModel.config.test;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.config.*;
import edu.gemini.spModel.data.config.DefaultConfig;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.pot.sp.ISPSeqComponent;
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;


public class CBNestedSeqCompCase extends CBSeqCompTestBase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testEmptySeqComp() throws Exception {
        assertNotNull("expected a parameter builder", testCB);

        // Add a child.
        ISysConfig sc = new DefaultSysConfig("childPS");
        _putParameterValue(sc, "xyz", "123");

        ISPSeqComponent seqComp = createSeqComponent(SPComponentType.UNKNOWN, sc);
        testSeqComp.addSeqComponent(seqComp);

        // Make sure that presence of a child won't effect the root sequence
        // component.
        testCB.reset(null);
        assertTrue("should have no configurations in empty seq comp", !testCB.hasNext());
    }

    @Test
    public void testSingleParam() throws Exception {
        // Add a child.
        ISysConfig childPS = new DefaultSysConfig("childPS");
        _putParameterValue(childPS, "childParam0", "childValue0");

        ISPSeqComponent seqComp = createSeqComponent(SPComponentType.UNKNOWN, childPS);
        testSeqComp.addSeqComponent(seqComp);

        ISysConfig sc = new DefaultSysConfig("set0");
        _putParameterValue(sc, "param0", "value0");

        TestDataObject dataObj = (TestDataObject) testSeqComp.getDataObject();
        dataObj.setSysConfig(sc);


        // Create the one expected configuration.
        IConfig conf = new DefaultConfig();
        _putConfigParameterValue(conf, "set0", "param0", "value0");
        _putConfigParameterValue(conf, "childPS", "childParam0", "childValue0");

        // Put it in the list of expected configurations.
        List<IConfig> confList = new LinkedList<IConfig>();
        confList.add(conf);

        // Run the test.
        runApply(confList);
    }

    @Test
    public void testSingleSeqParam() throws Exception {
        // Add a child.
        List<String> childValueList = new LinkedList<String>();
        childValueList.add("childValue0");
        childValueList.add("childValue1");

        ISysConfig childPS = new DefaultSysConfig("childPS");
        _putParameterValue(childPS, "childParam0", childValueList);

        ISPSeqComponent seqComp = createSeqComponent(SPComponentType.UNKNOWN, childPS);
        testSeqComp.addSeqComponent(seqComp);


        List<String> valueList = new LinkedList<String>();
        valueList.add("value0");
        valueList.add("value1");

        ISysConfig sc = new DefaultSysConfig("rootPS");
        _putParameterValue(sc, "param0", valueList);

        TestDataObject dataObj = (TestDataObject) testSeqComp.getDataObject();
        dataObj.setSysConfig(sc);


        // Create the one expected configurations.
        IConfig conf0 = new DefaultConfig();
        _putConfigParameterValue(conf0, "rootPS", "param0", "value0");
        _putConfigParameterValue(conf0, "childPS", "childParam0", "childValue0");

        IConfig conf1 = new DefaultConfig();
        _putConfigParameterValue(conf1, "childPS", "childParam0", "childValue1");

        IConfig conf2 = new DefaultConfig();
        _putConfigParameterValue(conf2, "rootPS", "param0", "value1");
        _putConfigParameterValue(conf2, "childPS", "childParam0", "childValue0");

        IConfig conf3 = new DefaultConfig();
        _putConfigParameterValue(conf3, "childPS", "childParam0", "childValue1");


        // Put them in the list of expected configurations.
        List<IConfig> confList = new LinkedList<IConfig>();
        confList.add(conf0);
        confList.add(conf1);
        confList.add(conf2);
        confList.add(conf3);

        // Run the test.
        runApply(confList);
    }

    @Test
    public void testMultipleSeqParam() throws Exception {
        // Set up the child's parameters.
        List<String> cvList0 = new LinkedList<String>();
        cvList0.add("cv0.0");
        cvList0.add("cv0.1");

        List<String> cvList1 = new LinkedList<String>();
        cvList1.add("cv1.0");
        cvList1.add("cv1.1");
        cvList1.add("cv1.2");

        ISysConfig childPS = new DefaultSysConfig("childPS");
        _putParameterValue(childPS, "cp0", cvList0);
        _putParameterValue(childPS, "cp1", cvList1);

        // Add the child.
        ISPSeqComponent seqComp = createSeqComponent(SPComponentType.UNKNOWN, childPS);
        testSeqComp.addSeqComponent(seqComp);


        // Set up the parent's parameters
        ISysConfig sc = new DefaultSysConfig("rootPS");

        List<String> valueList0 = new LinkedList<String>();
        valueList0.add("value0.0");
        valueList0.add("value0.1");

        _putParameterValue(sc, "param0", valueList0);

        List<String> valueList1 = new LinkedList<String>();
        valueList1.add("value1.0");
        valueList1.add("value1.1");
        valueList1.add("value1.2");

        _putParameterValue(sc, "param1", valueList1);

        TestDataObject dataObj = (TestDataObject) testSeqComp.getDataObject();
        dataObj.setSysConfig(sc);



        // Create the one expected configurations.
        IConfig conf0 = new DefaultConfig();
        _putConfigParameterValue(conf0, "rootPS", "param0", "value0.0");
        _putConfigParameterValue(conf0, "rootPS", "param1", "value1.0");
        _putConfigParameterValue(conf0, "childPS", "cp0", "cv0.0");
        _putConfigParameterValue(conf0, "childPS", "cp1", "cv1.0");

        IConfig conf1 = new DefaultConfig();
        _putConfigParameterValue(conf1, "childPS", "cp0", "cv0.1");
        _putConfigParameterValue(conf1, "childPS", "cp1", "cv1.1");

        IConfig conf2 = new DefaultConfig();
        _putConfigParameterValue(conf2, "childPS", "cp1", "cv1.2");

        IConfig conf3 = new DefaultConfig();
        _putConfigParameterValue(conf3, "rootPS", "param0", "value0.1");
        _putConfigParameterValue(conf3, "rootPS", "param1", "value1.1");
        _putConfigParameterValue(conf3, "childPS", "cp0", "cv0.0");
        _putConfigParameterValue(conf3, "childPS", "cp1", "cv1.0");

        IConfig conf4 = (IConfig) conf1.clone();
        IConfig conf5 = (IConfig) conf2.clone();

        IConfig conf6 = new DefaultConfig();
        _putConfigParameterValue(conf6, "rootPS", "param1", "value1.2");
        _putConfigParameterValue(conf6, "childPS", "cp0", "cv0.0");
        _putConfigParameterValue(conf6, "childPS", "cp1", "cv1.0");

        IConfig conf7 = (IConfig) conf1.clone();
        IConfig conf8 = (IConfig) conf2.clone();


        // Put them in the list of expected configurations.
        List<IConfig> confList = new LinkedList<IConfig>();
        confList.add(conf0);
        confList.add(conf1);
        confList.add(conf2);
        confList.add(conf3);
        confList.add(conf4);
        confList.add(conf5);
        confList.add(conf6);
        confList.add(conf7);
        confList.add(conf8);

        // Run the test.
        runApply(confList);
    }

    @Test
    public void testSimpleMultipleSeqParam() throws Exception {
        // Replace the cb
        HelperSeqCompCB cb = new HelperSeqCompCB(testSeqComp);
        testSeqComp.putClientData(IConfigBuilder.USER_OBJ_KEY, cb);
        testMultipleSeqParam();
    }
}

