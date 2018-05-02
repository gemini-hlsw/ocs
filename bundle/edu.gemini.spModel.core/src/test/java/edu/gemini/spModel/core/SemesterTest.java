//
// $
//

package edu.gemini.spModel.core;

import static edu.gemini.spModel.core.Semester.Half.A;
import static edu.gemini.spModel.core.Semester.Half.B;
import junit.framework.TestCase;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Test cases for {@link Semester}.
 */
public class SemesterTest extends TestCase {
    private final Semester s2009A_1 = new Semester(2009, A);
    private final Semester s2009A_2 = new Semester(2009, A);
    private final Semester s2009B   = new Semester(2009, B);
    private final Semester s2010A   = new Semester(2010, A);


    public void testConstruction() throws Exception {
        Semester s = new Semester(2009, A);
        assertEquals(2009, s.getYear());
        assertEquals(A, s.getHalf());

        s = new Semester(2009, B);
        assertEquals(B, s.getHalf());

        // Try constructing with a negative year.
        try {
            //noinspection UnusedAssignment
            s = new Semester(-1, A);
            fail("constructed with negative year");
        } catch (IllegalArgumentException ex) {
            // expected
        }

        // Try constructing without a half.
        try {
            //noinspection UnusedAssignment
            s = new Semester(2009, null);
            fail("constructed without a Semester.Half");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    public void testEqualsHashCode() throws Exception {
        assertEquals(s2009A_1, s2009A_1);
        assertEquals(s2009A_1, s2009A_2);
        assertEquals(s2009A_1.hashCode(), s2009A_2.hashCode());
        assertFalse(s2009A_1.equals(s2009B));
        assertFalse(s2009A_1.hashCode() == s2009B.hashCode());
        assertFalse(s2009A_1.equals(s2010A));
        assertFalse(s2009A_1.hashCode() == s2010A.hashCode());

        //noinspection ObjectEqualsNull
        assertFalse(s2009A_1.equals(null));
        assertFalse(s2009A_1.equals(new Object()));
    }

    public void testCompareTo() throws Exception {
        assertTrue(s2009A_1.compareTo(s2009A_1) == 0);
        assertTrue(s2009A_1.compareTo(s2009A_2) == 0);
        assertTrue(s2009A_1.compareTo(s2009B) < 0);
        assertTrue(s2009B.compareTo(s2009A_1) > 0);
        assertTrue(s2009A_1.compareTo(s2010A) < 0);
    }

    public void testParse() throws Exception {
        assertEquals("2009A", s2009A_1.toString());
        assertEquals(s2009A_1, Semester.parse("2009A"));

        try {
            Semester.parse(null);
        } catch (NullPointerException ex) {
            // expected
        }

        try {
            Semester.parse("");
        } catch (ParseException ex) {
            // expected
        }

        try {
            Semester.parse("2009");
        } catch (ParseException ex) {
            // expected
        }

        try {
            Semester.parse("209A");
        } catch (ParseException ex) {
            // expected
        }

        try {
            Semester.parse("-209A");
        } catch (ParseException ex) {
            // expected
        }

        try {
            Semester.parse("2009C");
        } catch (ParseException ex) {
            // expected
        }

        assertEquals(new Semester(0, A), Semester.parse("0000A"));
    }

    public void testNextPrev() {
        assertEquals(s2009B.prev(), new Semester(2009, Semester.Half.A));
        assertEquals(s2010A.next(), new Semester(2010, Semester.Half.B));
        assertEquals(s2010A.prev(), s2009B);
        assertEquals(s2009B.next(), s2010A);
    }

    public void testCreationFromTime() {
        Calendar cal = new GregorianCalendar(Site.GN.timezone());
        cal.clear(Calendar.MILLISECOND);

        // check switch between A and B (happens on July 31st at 14:00)
        cal.set(2009, 6, 31, 13, 59, 59);
        Date date1 = cal.getTime();
        cal.set(2009, 6, 31, 14, 0, 0);
        Date date2 = cal.getTime();
        assertEquals(s2009A_1, new Semester(Site.GN, date1));
        assertEquals(s2009B,   new Semester(Site.GN, date2));

        // check switch between B and A (happens on January 31st at 14:00)
        cal.set(2010, 0, 31, 13, 59, 59);
        Date date3 = cal.getTime();
        cal.set(2010, 0, 31, 14, 0, 0);
        Date date4 = cal.getTime();
        assertEquals(s2009B,   new Semester(Site.GN, date3));
        assertEquals(s2010A,   new Semester(Site.GN, date4));
    }

    public void testStartEndDate() {
        Calendar cal = new GregorianCalendar(Site.GN.timezone());
        cal.clear(Calendar.MILLISECOND);

        // create expected start/end dates, note that end of night is relevant for start of semester
        // (i.e. first night that ENDs in new semester will start that semester)
        cal.set(2009, 6, 31, 14, 0, 0);
        Date start2009B = cal.getTime();
        cal.set(2010, 0, 31, 14, 0, 0);
        Date start2010A = cal.getTime();
        cal.set(2010, 6, 31, 14, 0, 0);
        Date start2010B = cal.getTime();

        // check start / end
        assertEquals(start2009B, s2009B.getStartDate(Site.GN));
        assertEquals(start2010A, s2009B.getEndDate(Site.GN));
        assertEquals(start2010A, s2010A.getStartDate(Site.GN));
        assertEquals(start2010B, s2010A.getEndDate(Site.GN));
    }

    public void testMidpointDate() {
        final Semester s = new Semester(2018, Semester.Half.A);
        final long start = s.getStartDate(Site.GN).getTime();
        final long end   = s.getEndDate(Site.GN).getTime();
        final long mid   = s.getMidpointDate(Site.GN).getTime();

        final long quarter0 = mid - start;
        final long quarter1 = end - mid;

        final long diff     = Math.abs(quarter1 - quarter0);
        assertTrue(diff <= 1);
    }
}
