// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: GNIRSTest.java 4726 2004-05-14 16:50:12Z brighton $
//
package edu.gemini.spModel.gemini.gnirs.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class GNIRSTest {

    public static Test suite() {
        TestSuite suite = new TestSuite();

        suite.addTest(InstGNIRSCase.suite());
        return suite;
    }

}
