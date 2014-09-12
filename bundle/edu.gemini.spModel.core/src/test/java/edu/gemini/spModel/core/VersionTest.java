//
// $
//

package edu.gemini.spModel.core;

import static edu.gemini.spModel.core.Semester.Half.A;
import static edu.gemini.spModel.core.Semester.Half.B;
import static edu.gemini.spModel.core.Version.Compatibility.*;
import junit.framework.TestCase;

import java.text.ParseException;

/**
 * Test cases for Version.
 */
public class VersionTest extends TestCase {
    private final Semester s2009A = new Semester(2009, A);
    private final Semester s2009B = new Semester(2009, B);
    private final Semester s2010A = new Semester(2010, A);

    private final Version v2009A_1_1_1 = new Version(s2009A, 1, 1, 1);
    private final Version v2009A_2_1_1 = new Version(s2009A, 2, 1, 1);
    private final Version v2009A_1_2_1 = new Version(s2009A, 1, 2, 1);
    private final Version v2009A_1_1_2 = new Version(s2009A, 1, 1, 2);
    private final Version v2009B_1_1_1 = new Version(s2009B, 1, 1, 1);

    private final Version v2009A_1_1_1_copy = new Version(s2009A, 1, 1, 1);

    public void testConstruction() throws Exception {
        assertEquals(s2009A, v2009A_1_1_1.getSemester());
        assertEquals(2, v2009A_2_1_1.getXmlCompatibility());
        assertEquals(2, v2009A_1_2_1.getSerialCompatibility());
        assertEquals(2, v2009A_1_1_2.getMinor());

        try {
            new Version(null, 1, 1, 1);
            fail("null version");
        } catch (Exception ex) {
            // expected
        }

        try {
            new Version(s2009A, -1, 1, 1);
            fail("negative xml");
        } catch (Exception ex) {
            // expected
        }

        try {
            new Version(s2009A, -1, 1, 1);
            fail("negative xml");
        } catch (Exception ex) {
            // expected
        }

        try {
            new Version(s2009A, 1, 1, -1);
            fail("negative minor");
        } catch (Exception ex) {
            // expected
        }
    }

    public void testIsCompatible() throws Exception {
        assertFalse(v2009A_1_1_1.isCompatible(v2009B_1_1_1, semester));
        assertFalse(v2009A_1_1_1.isCompatible(v2009B_1_1_1, xml));
        assertFalse(v2009A_1_1_1.isCompatible(v2009B_1_1_1, serial));
        assertFalse(v2009A_1_1_1.isCompatible(v2009B_1_1_1, minor));

        assertTrue(v2009A_1_1_1.isCompatible(v2009A_2_1_1, semester));
        assertFalse(v2009A_1_1_1.isCompatible(v2009A_2_1_1, xml));
        assertFalse(v2009A_1_1_1.isCompatible(v2009A_2_1_1, serial));
        assertFalse(v2009A_1_1_1.isCompatible(v2009A_2_1_1, minor));

        assertTrue(v2009A_1_1_1.isCompatible(v2009A_1_2_1, semester));
        assertTrue(v2009A_1_1_1.isCompatible(v2009A_1_2_1, xml));
        assertFalse(v2009A_1_1_1.isCompatible(v2009A_1_2_1, serial));
        assertFalse(v2009A_1_1_1.isCompatible(v2009A_1_2_1, minor));

        assertTrue(v2009A_1_1_1.isCompatible(v2009A_1_1_2, semester));
        assertTrue(v2009A_1_1_1.isCompatible(v2009A_1_1_2, xml));
        assertTrue(v2009A_1_1_1.isCompatible(v2009A_1_1_2, serial));
        assertFalse(v2009A_1_1_1.isCompatible(v2009A_1_1_2, minor));
    }

    public void testCompareTo() throws Exception {
        assertEquals(0, v2009A_1_1_1.compareTo(v2009A_1_1_1_copy));
        assertTrue(v2009A_1_1_1.compareTo(v2009B_1_1_1) < 0);
        assertTrue(v2009B_1_1_1.compareTo(v2009A_1_1_1) > 0);
        assertTrue(v2009A_1_1_1.compareTo(v2009A_2_1_1) < 0);
        assertTrue(v2009A_2_1_1.compareTo(v2009A_1_1_1) > 0);
        assertTrue(v2009A_1_1_1.compareTo(v2009A_1_2_1) < 0);
        assertTrue(v2009A_1_2_1.compareTo(v2009A_1_1_1) > 0);
        assertTrue(v2009A_1_1_1.compareTo(v2009A_1_1_2) < 0);
        assertTrue(v2009A_1_1_2.compareTo(v2009A_1_1_1) > 0);
    }

    public void testParse() throws Exception {
        assertEquals(v2009A_1_1_1, Version.parse(v2009A_1_1_1.toString()));
        assertEquals("2009A.10.20.30", Version.parse("2009A.10.20.30").toString());
        assertEquals("2009A.1.22.333", Version.parse("2009A.1.22.333").toString());

        String[] badVersions = {
                "2009A",
                "2009A.1",
                "2009A.1.1",
                "2009A.1.1.1.1",
                "2009A.-1.1.1",
                "x.1.1.1",
        };

        for (String badVersion : badVersions) {
            try {
                Version.parse("2009A");
                fail("Should have failed parsing: " + badVersion);
            } catch (ParseException ex) {
                // ignore
            }
        }
    }

    public void testEqualsHashCode() throws Exception {
        assertEquals(v2009A_1_1_1, v2009A_1_1_1_copy);
        assertEquals(v2009A_1_1_1.hashCode(), v2009A_1_1_1_copy.hashCode());

        Version[] versions = {
                v2009B_1_1_1,
                v2009A_2_1_1,
                v2009A_1_2_1,
                v2009A_1_1_2,
        };
        for (Version v : versions) {
            assertFalse(v2009A_1_1_1.equals(v));
            assertFalse(v2009A_1_1_1.hashCode() == v.hashCode());
        }

        //noinspection ObjectEqualsNull
        assertFalse(v2009A_1_1_1.equals(null));
        assertFalse(v2009A_1_1_1.equals(new Object()));
        //noinspection EqualsBetweenInconvertibleTypes
        assertFalse(v2009A_1_1_1.equals(s2009A));
    }

    public void testTestVersion() throws Exception {
        Version testV = Version.parse("2010A-test.1.2.3");
        assertTrue(testV.isTest());
        assertEquals(s2010A, testV.getSemester());
        assertEquals(1, testV.getXmlCompatibility());
        assertEquals(2, testV.getSerialCompatibility());
        assertEquals(3, testV.getMinor());

        Version realV = Version.parse("2010A.1.2.3");

        assertFalse(testV.isCompatible(realV, test));
        assertFalse(testV.isCompatible(realV, semester));
        assertFalse(testV.equals(realV));

        Version testV2 = Version.parse("2010A-test.1.2.4");
        assertTrue(testV.isCompatible(testV2, test));
        assertTrue(testV.isCompatible(testV2, semester));
        assertTrue(testV.isCompatible(testV2, xml));
        assertTrue(testV.isCompatible(testV2, serial));
        assertFalse(testV.isCompatible(testV2, minor));
        assertFalse(testV.equals(testV2));

        assertEquals("2010A-test.1.2.3", testV.toString());
        assertTrue(testV.equals(new Version(new Semester(2010, A), true, 1, 2, 3)));
        assertEquals(testV.hashCode(), (new Version(new Semester(2010, A), true, 1, 2, 3)).hashCode());
    }
}
