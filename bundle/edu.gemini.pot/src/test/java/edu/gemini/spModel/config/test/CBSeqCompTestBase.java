// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: CBSeqCompTestBase.java 6017 2005-05-02 22:49:39Z shane $
//

package edu.gemini.spModel.config.test;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.config.*;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.pot.sp.ISPSeqComponent;
import static org.junit.Assert.*;

import java.util.List;


public abstract class CBSeqCompTestBase extends CBTestBase {

    static final String PARAM_SET_NAME = "testPS";

    protected ISPSeqComponent testSeqComp;
    protected IConfigBuilder testCB;


    public void setUp() throws Exception {
        super.setUp();

        ISysConfig sc = new DefaultSysConfig(PARAM_SET_NAME);
        testSeqComp = createSeqComponent(SPComponentType.UNKNOWN, sc);

        testCB = (IConfigBuilder) testSeqComp.getClientData(IConfigBuilder.USER_OBJ_KEY);
    }

    protected ISPSeqComponent createSeqComponent(SPComponentType compType, ISysConfig sc) throws Exception {
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

    protected void runApply(List expectedList) throws Exception {
        assertNotNull("expected a parameter builder", testCB);
        runApply(testCB, expectedList);
    }

}

