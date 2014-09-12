package edu.gemini.spModel.util.test;

//
// Test code for the TimeValue class
// $Id: TimeValueTest.java 15389 2008-11-03 20:02:08Z swalker $
//

import edu.gemini.shared.util.TimeValue;
import junit.framework.TestCase;

public class TimeValueTest extends TestCase {
    private static final double _ERROR = 10e-6;

    public void testNormalConstruction() {
        TimeValue t1 = TimeValue.ZERO_NIGHTS;
        assertEquals(0.0, t1.getTimeAmount(), _ERROR);
        assertEquals(0, t1.getMilliseconds());
        assertSame(TimeValue.Units.nights, t1.getTimeUnits());

        TimeValue t2;

        t1 = TimeValue.millisecondsToTimeValue(1000, TimeValue.Units.seconds);
        t2 = new TimeValue(1, TimeValue.Units.seconds);
        assertEquals(1.0, t1.getTimeAmount(), _ERROR);
        assertEquals(1.0, t2.getTimeAmount(), _ERROR);
        assertEquals(t1, t2);

        t1 = TimeValue.millisecondsToTimeValue(60000, TimeValue.Units.minutes);
        t2 = new TimeValue(1, TimeValue.Units.minutes);
        assertEquals(1.0, t1.getTimeAmount(), _ERROR);
        assertEquals(1.0, t2.getTimeAmount(), _ERROR);
        assertEquals(t1, t2);

        t1 = TimeValue.millisecondsToTimeValue(3600000, TimeValue.Units.hours);
        t2 = new TimeValue(1, TimeValue.Units.hours);
        assertEquals(1.0, t1.getTimeAmount(), _ERROR);
        assertEquals(1.0, t2.getTimeAmount(), _ERROR);
        assertEquals(t1, t2);

        t1 = TimeValue.millisecondsToTimeValue(3600000 * TimeValue.HOURS_PER_NIGHT, TimeValue.Units.nights);
        t2 = new TimeValue(1, TimeValue.Units.nights);
        assertEquals(1.0, t1.getTimeAmount(), _ERROR);
        assertEquals(1.0, t2.getTimeAmount(), _ERROR);
        assertEquals(t1, t2);
    }

    public void testStringConstructor() {
        // Not sure I actually like the string constructor ...
        String testString = "3 nights";
        double testTime = 3.0;
        TimeValue.Units testUnits = TimeValue.Units.nights;
        TimeValue t1 = TimeValue.parse(testString);
        assertNotNull(t1);
        assertEquals(testTime, t1.getTimeAmount(), _ERROR);
        assertSame(testUnits, t1.getTimeUnits());

        // Now test it without units
        String testString2 = "43";
        double testTime2 = 43.0;
        t1 = TimeValue.parse(testString2);
        assertNotNull(t1);
        assertEquals(testTime2, t1.getTimeAmount(), _ERROR);
        assertSame(TimeValue.Units.DEFAULT, t1.getTimeUnits());

        try {
            //noinspection UnusedAssignment
            t1 = TimeValue.parse("xyz");
            fail("should throw exception");
        } catch(Exception expected) {
        }
    }

    public void testConvertTimeAmount() {
        TimeValue tv = TimeValue.millisecondsToTimeValue(60000, TimeValue.Units.minutes);

        assertEquals(60.0, tv.convertTimeAmountTo(TimeValue.Units.seconds), _ERROR);
        assertEquals(1.0, tv.convertTimeAmountTo(TimeValue.Units.minutes), _ERROR);
        assertEquals(1.0/60.0, tv.convertTimeAmountTo(TimeValue.Units.hours), _ERROR);
        assertEquals(1.0/(60.0*TimeValue.HOURS_PER_NIGHT),
                     tv.convertTimeAmountTo(TimeValue.Units.nights), _ERROR);
    }

    public void testConvertToUnits() {
        TimeValue tv1 = TimeValue.millisecondsToTimeValue(60000, TimeValue.Units.minutes);

        TimeValue tvSec = tv1.convertTo(TimeValue.Units.seconds);
        TimeValue tvMin = tv1.convertTo(TimeValue.Units.minutes);
        TimeValue tvHrs = tv1.convertTo(TimeValue.Units.hours);
        TimeValue tvNgt = tv1.convertTo(TimeValue.Units.nights);

        assertEquals(60.0, tvSec.getTimeAmount(), _ERROR);
        assertEquals(1.0,  tvMin.getTimeAmount(), _ERROR);
        assertEquals(1.0/60.0, tvHrs.getTimeAmount(), _ERROR);
        assertEquals(1.0/(60*TimeValue.HOURS_PER_NIGHT),
                     tvNgt.getTimeAmount(), _ERROR);
    }

    public void testAdd() {
        TimeValue tv1min = TimeValue.millisecondsToTimeValue(60000, TimeValue.Units.minutes);
        TimeValue tv60sec = TimeValue.millisecondsToTimeValue(60000, TimeValue.Units.seconds);

        TimeValue res = tv1min.add(tv60sec);
        assertSame(TimeValue.Units.minutes, res.getTimeUnits());
        assertEquals(2.0, res.getTimeAmount(), _ERROR);
        assertEquals(120000, res.getMilliseconds());

        res = tv60sec.add(tv1min);
        assertSame(TimeValue.Units.seconds, res.getTimeUnits());
        assertEquals(120.0, res.getTimeAmount(), _ERROR);
        assertEquals(120000, res.getMilliseconds());
    }

    public void testCompare() {
        TimeValue t1;
        TimeValue t2;

        // same value and units
        t1 = new TimeValue(60.0, TimeValue.Units.seconds);
        t2 = new TimeValue(60.0, TimeValue.Units.seconds);
        assertEquals(0, t1.compareTo(t2));
        assertEquals(0, t2.compareTo(t1));

        // different value, same units
        t2 = new TimeValue(61.0, TimeValue.Units.seconds);
        assertTrue(t1.compareTo(t2) < 0);
        assertTrue(t2.compareTo(t1) > 0);

        // same time period, different units
        t2 = new TimeValue(1.0, TimeValue.Units.minutes);
        assertTrue(t1.compareTo(t2) > 0);
        assertTrue(t2.compareTo(t1) < 0);

        // units that would sort first, but time amount causes them to come
        // after
        t2 = new TimeValue(1.1, TimeValue.Units.minutes);
        assertTrue(t1.compareTo(t2) < 0);
        assertTrue(t2.compareTo(t1) > 0);
    }

    public void testStringConvert() {
        // This isn't really very good ...
        TimeValue t1 = new TimeValue(60.0, TimeValue.Units.seconds);
        String t1Str = t1.toString();
        assertEquals("60.0 seconds", t1Str);

        TimeValue t2 = TimeValue.parse(t1Str);
        assertEquals(t1, t2);
    }
}
