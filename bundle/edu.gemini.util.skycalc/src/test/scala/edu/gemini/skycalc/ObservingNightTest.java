//
// $Id: ObservingNightTest.java 6519 2005-07-24 00:39:18Z shane $
//
package edu.gemini.skycalc;

import edu.gemini.spModel.core.Site;
import junit.framework.TestCase;

import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;


public class ObservingNightTest extends TestCase {
    private static final TimeZone CHILE_TIME  = Site.GS.timezone();


    public ObservingNightTest(String name) {
        super(name);
    }

    public void testNormalObservingNight() throws Exception {
        // 10 PM on Feb. 5, 2005 - a normal date
        Calendar c = new GregorianCalendar(2005, 1, 5, 22, 0, 0);
        c.setTimeZone(CHILE_TIME);

        ObservingNight on = new ObservingNight(Site.GS, c.getTimeInMillis());
        c = new GregorianCalendar(CHILE_TIME);
        c.setTimeInMillis(on.getStartTime());
        assertEquals(Calendar.FEBRUARY, c.get(Calendar.MONTH));
        assertEquals(5, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(ObservingNight.LOCAL_NIGHT_END_HOUR, c.get(Calendar.HOUR_OF_DAY));
        c.setTimeInMillis(on.getEndTime());

        assertEquals(Calendar.FEBRUARY, c.get(Calendar.MONTH));
        assertEquals(6, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(ObservingNight.LOCAL_NIGHT_END_HOUR, c.get(Calendar.HOUR_OF_DAY));

        long diff = on.getEndTime() - on.getStartTime();
        assertEquals(24 * 60 * 60 * 1000, diff); // one day of milliseconds
    }

    public void testDaylightSavingsObservingNight() throws Exception {
        // Midnight, 13th of March, 2005, end of daylight saving time.
        Calendar c = new GregorianCalendar(2005, 2, 13, 0, 0, 0);
        c.setTimeZone(CHILE_TIME);

        ObservingNight on = new ObservingNight(Site.GS, c.getTimeInMillis());
        c = new GregorianCalendar(CHILE_TIME);
        c.setTimeInMillis(on.getStartTime());
        assertEquals(Calendar.MARCH, c.get(Calendar.MONTH));
        assertEquals(12, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(ObservingNight.LOCAL_NIGHT_END_HOUR, c.get(Calendar.HOUR_OF_DAY));

        c.setTimeInMillis(on.getEndTime());
        assertEquals(Calendar.MARCH, c.get(Calendar.MONTH));
        assertEquals(13, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(ObservingNight.LOCAL_NIGHT_END_HOUR, c.get(Calendar.HOUR_OF_DAY));

        long diff = on.getEndTime() - on.getStartTime();
        assertEquals(25 * 60 * 60 * 1000, diff); // fall back, one extra hour

        // Midnight, 9th of October, 2005, start of daylight saving time.
        c = new GregorianCalendar(2005, 9, 9, 0, 0, 0);
        c.setTimeZone(CHILE_TIME);

        on = new ObservingNight(Site.GS, c.getTimeInMillis());
        c = new GregorianCalendar(CHILE_TIME);
        c.setTimeInMillis(on.getStartTime());
        assertEquals(Calendar.OCTOBER, c.get(Calendar.MONTH));
        assertEquals( 8, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(ObservingNight.LOCAL_NIGHT_END_HOUR, c.get(Calendar.HOUR_OF_DAY));

        c.setTimeInMillis(on.getEndTime());
        assertEquals(Calendar.OCTOBER, c.get(Calendar.MONTH));
        assertEquals( 9, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(ObservingNight.LOCAL_NIGHT_END_HOUR, c.get(Calendar.HOUR_OF_DAY));

        diff = on.getEndTime() - on.getStartTime();
        assertEquals(23 * 60 * 60 * 1000, diff); // spring forward, one less hour
    }

    public void testParsing() throws Exception {
        Site[] sdA = new Site[] {Site.GS, Site.GN };

        for (int i=0; i<sdA.length; ++i) {
            // Daylight savings time (in chile)
            ObservingNight on = ObservingNight.parseNightString("20051009", sdA[i]);

            Calendar c = new GregorianCalendar(sdA[i].timezone());
            c.setTimeInMillis(on.getStartTime());
            assertEquals(2005, c.get(Calendar.YEAR));
            assertEquals(Calendar.OCTOBER, c.get(Calendar.MONTH));
            assertEquals( 8, c.get(Calendar.DAY_OF_MONTH));
            assertEquals(ObservingNight.LOCAL_NIGHT_END_HOUR, c.get(Calendar.HOUR_OF_DAY));
            assertEquals( 0, c.get(Calendar.MINUTE));
            assertEquals( 0, c.get(Calendar.SECOND));
            assertEquals( 0, c.get(Calendar.MILLISECOND));

            c.setTimeInMillis(on.getEndTime());
            assertEquals(2005, c.get(Calendar.YEAR));
            assertEquals(Calendar.OCTOBER, c.get(Calendar.MONTH));
            assertEquals( 9, c.get(Calendar.DAY_OF_MONTH));
            assertEquals(ObservingNight.LOCAL_NIGHT_END_HOUR, c.get(Calendar.HOUR_OF_DAY));
            assertEquals( 0, c.get(Calendar.MINUTE));
            assertEquals( 0, c.get(Calendar.SECOND));
            assertEquals( 0, c.get(Calendar.MILLISECOND));


            // End of year
            on = ObservingNight.parseNightString("20000101", sdA[i]);

            c = new GregorianCalendar(sdA[i].timezone());
            c.setTimeInMillis(on.getStartTime());
            assertEquals(Calendar.DECEMBER, c.get(Calendar.MONTH));
            assertEquals(31, c.get(Calendar.DAY_OF_MONTH));
            assertEquals(ObservingNight.LOCAL_NIGHT_END_HOUR, c.get(Calendar.HOUR_OF_DAY));

            c.setTimeInMillis(on.getEndTime());
            assertEquals(Calendar.JANUARY, c.get(Calendar.MONTH));
            assertEquals( 1, c.get(Calendar.DAY_OF_MONTH));
            assertEquals(ObservingNight.LOCAL_NIGHT_END_HOUR, c.get(Calendar.HOUR_OF_DAY));
        }
    }

    public void testFailedParsing() throws Exception {
        try {
            ObservingNight.parseNightString("200510", Site.GS);
            fail("failed to throw exception");
        } catch (Exception ex) {
            // expected
        }

        try {
            ObservingNight.parseNightString("GS-200510", Site.GS);
            fail("failed to throw exception");
        } catch (Exception ex) {
            // expected
        }
    }
}
