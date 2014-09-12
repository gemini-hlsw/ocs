//
// $Id: SPProgramIDSortTest.java 47163 2012-08-01 23:09:47Z rnorris $
//
package edu.gemini.pot.sp.test;

import junit.framework.TestCase;
import edu.gemini.spModel.core.SPProgramID;

/**
 * Test for the sort order of program ids.
 */
public class SPProgramIDSortTest extends TestCase {

    public SPProgramIDSortTest(String name) {
        super(name);
    }

    private void _assertEquals(SPProgramID id1, SPProgramID id2) {
        assertEquals(0, id1.compareTo(id2));
        assertEquals(0, id2.compareTo(id1));
    }

    private void _assertOrder(SPProgramID lesser, SPProgramID greater) {
        assertTrue(lesser.compareTo(greater) < 0);
        assertTrue(greater.compareTo(lesser) > 0);
    }

    public void testSortEquals() throws Exception {
        SPProgramID id1 = SPProgramID.toProgramID("GN-2003B-Q-5");
        SPProgramID id2 = SPProgramID.toProgramID("GN-2003B-Q-5");
        _assertEquals(id1, id2);
    }

    public void testSortEmpties() throws Exception {
        SPProgramID id1 = SPProgramID.toProgramID("");
        SPProgramID id2 = SPProgramID.toProgramID("");
        _assertEquals(id1, id2);
    }

    public void testSortOneDash() throws Exception {
        SPProgramID id1 = SPProgramID.toProgramID("-");
        SPProgramID id2 = SPProgramID.toProgramID("-");
        _assertEquals(id1, id2);
    }

    public void testSortTwoDashes() throws Exception {
        SPProgramID id1 = SPProgramID.toProgramID("--");
        SPProgramID id2 = SPProgramID.toProgramID("--");
        _assertEquals(id1, id2);
    }

    public void testSortDifferentLength() throws Exception {
        SPProgramID id1 = SPProgramID.toProgramID("GN-2003B-Q-5");
        SPProgramID id2 = SPProgramID.toProgramID("GN-2003B-Q-5-copy");
        _assertOrder(id1, id2);
    }

    public void testSortWithTrailingDash() throws Exception {
        SPProgramID id1 = SPProgramID.toProgramID("GN-2003B-Q-5");
        SPProgramID id2 = SPProgramID.toProgramID("GN-2003B-Q-5-");
        _assertOrder(id1, id2);
    }

    public void testSortMixedDashes() throws Exception {
        SPProgramID id1 = SPProgramID.toProgramID("--");
        SPProgramID id2 = SPProgramID.toProgramID("---");
        _assertOrder(id1, id2);
    }

    public void testSortNumeric() throws Exception {
        SPProgramID id1 = SPProgramID.toProgramID("GN-2003B-Q-5");
        SPProgramID id2 = SPProgramID.toProgramID("GN-2003B-Q-32");
        _assertOrder(id1, id2);
    }

    // one part is numeric, the matching part is a string
    public void testSortMixed() throws Exception {
        SPProgramID id1 = SPProgramID.toProgramID("GN-2003-Q-5");
        SPProgramID id2 = SPProgramID.toProgramID("GN-2003B-Q-5");
        _assertOrder(id1, id2);
    }

    public void testSortOnePart() throws Exception {
        SPProgramID id1 = SPProgramID.toProgramID("GN2003BQ5");
        SPProgramID id2 = SPProgramID.toProgramID("GS2003BQ5");
        _assertOrder(id1, id2);
    }

    public void testSortEmpty() throws Exception {
        SPProgramID id1 = SPProgramID.toProgramID("");
        SPProgramID id2 = SPProgramID.toProgramID("GN-2003B-Q-5");
        _assertOrder(id1, id2);
    }
}
