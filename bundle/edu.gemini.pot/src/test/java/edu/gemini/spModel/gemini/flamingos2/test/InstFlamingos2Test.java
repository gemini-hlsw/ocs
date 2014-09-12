/**
 * $Id: InstFlamingos2Test.java 6913 2006-02-22 21:49:35Z brighton $
 */

package edu.gemini.spModel.gemini.flamingos2.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class InstFlamingos2Test {
    public static Test suite() {
        TestSuite suite = new TestSuite();

        suite.addTest(InstFlamingos2Case.suite());
        return suite;
    }

}
