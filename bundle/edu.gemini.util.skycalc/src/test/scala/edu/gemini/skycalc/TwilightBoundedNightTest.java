//
// $Id: TwilightBoundedNightTest.java 8169 2007-10-10 14:09:26Z swalker $
//

package edu.gemini.skycalc;

import edu.gemini.spModel.core.Site;
import junit.framework.TestCase;

import java.util.Calendar;
import java.util.TimeZone;

public class TwilightBoundedNightTest extends TestCase {

    private final class TestTime {
        final int year;
        final int month;
        final int day;
        final int hour;
        final int minute;
        final int second;

        public TestTime(int year, int month, int day, int hour, int minute, int second) {
            this.year   = year;
            this.month  = month;
            this.day    = day;
            this.hour   = hour;
            this.minute = minute;
            this.second = second;
        }

        public TestTime(long time, TimeZone zone) {
            Calendar c = Calendar.getInstance(zone);
            c.setTimeInMillis(time);

            if (c.get(Calendar.MILLISECOND) >= 500) {
                // round up
                c.add(Calendar.SECOND, 1);
            }

            this.year   = c.get(Calendar.YEAR);
            this.month  = c.get(Calendar.MONTH);
            this.day    = c.get(Calendar.DAY_OF_MONTH);
            this.hour   = c.get(Calendar.HOUR_OF_DAY);
            this.minute = c.get(Calendar.MINUTE);
            this.second = c.get(Calendar.SECOND);


        }

//        long getTime(TimeZone zone) {
//            Calendar c = Calendar.getInstance(zone);
//            c.set(year, month, day, hour, minute, second);
//            c.set(Calendar.MILLISECOND, 0);
//            return c.getTimeInMillis();
//        }

        public boolean equals(Object o) {
            if (!(o instanceof TestTime)) return false;
            TestTime that = (TestTime) o;
            if (this.year   != that.year) return false;
            if (this.month  != that.month) return false;
            if (this.day    != that.day) return false;
            if (this.hour   != that.hour) return false;
            if (this.minute != that.minute) return false;
            return this.second == that.second;
        }

        public int hashCode() {
            int res = year;
            res = res*37 + month;
            res = res*37 + day;
            res = res*37 + hour;
            res = res*37 + minute;
            res = res*37 + second;

            return res;
        }

        public String toString() {
            return String.format("[%d %d %d %d %d %s]", year, month, day, hour, minute, second);
        }
    }

    public TwilightBoundedNightTest(String name) {
        super(name);
    }

    public void testChileDaylightSavings() throws Exception {
        // Testing around daylight savings time in Chile.

        // Day before time change.
        TestTime evening = new TestTime(2005, Calendar.MARCH, 11, 20, 56, 44);
        TestTime morning = new TestTime(2005, Calendar.MARCH, 12,  6, 49, 10);

        TestTime input = new TestTime(2005, Calendar.MARCH, 11,  0,  0,  0);
        _testNight(Site.GS, input, evening, morning);


        // Day of the time change.
        evening = new TestTime(2005, Calendar.MARCH, 12, 20, 55, 30);
        morning = new TestTime(2005, Calendar.MARCH, 13,  5, 49, 52);

        // The hour specified shouldn't matter.  whatever hour on that day
        // counts the same.  We are really just specifying the day whose night
        // we're interested in.
        for (int h=0; h<24; ++h) {
            input = new TestTime(2005, Calendar.MARCH, 12,  h,  0,  0);
            _testNight(Site.GS, input, evening, morning);
        }

        // Day after time change.
        evening = new TestTime(2005, Calendar.MARCH, 13, 19, 54, 15);
        morning = new TestTime(2005, Calendar.MARCH, 14,  5, 50, 34);

        input = new TestTime(2005, Calendar.MARCH, 13,  0,  0,  0);
        _testNight(Site.GS, input, evening, morning);
    }

    public void testHawaiiNewYear() throws Exception {
        TestTime evening = new TestTime(2004, Calendar.DECEMBER, 31, 18, 45, 23);
        TestTime morning = new TestTime(2005, Calendar.JANUARY,   1,  6,  5, 34);

        TestTime input = new TestTime(2004, Calendar.DECEMBER, 31, 11, 59, 59);
        _testNight(Site.GN, input, evening, morning);

    }

    private void _testNight(Site site, TestTime inputTime, TestTime evening, TestTime morning) throws Exception {
        Night n = TwilightBoundedNight.forDate(TwilightBoundType.NAUTICAL, inputTime.day, inputTime.month, inputTime.year, site);

        TestTime expected = new TestTime(n.getStartTime(), site.timezone());
        assertEquals(expected, evening);

        expected = new TestTime(n.getEndTime(), site.timezone());
        assertEquals(expected, morning);
    }
}
