//
// $Id: NightlyProgIdGeneratorTest.java 6519 2005-07-24 00:39:18Z shane $
//
package edu.gemini.spModel.util.test;

import edu.gemini.spModel.core.Site;

import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;

import edu.gemini.spModel.util.NightlyProgIdGenerator;
import edu.gemini.skycalc.ObservingNight;
import edu.gemini.spModel.core.SPProgramID;
import org.junit.Test;

import static org.junit.Assert.*;

public class NightlyProgIdGeneratorTest {
    private static final TimeZone CHILE_TIME  = Site.GS.timezone();
    private static final TimeZone HAWAII_TIME = Site.GN.timezone();


//    public NightlyProgIdGeneratorTest(String name) {
//        super(name);
//    }

    @Test public void testNormalDateId() throws Exception {
        SPProgramID id;

        // 10 PM on Feb. 5, 2005 - a normal date
        Calendar c = new GregorianCalendar(2005, 1, 5, 22, 0, 0);
        c.setTimeZone(CHILE_TIME);
        id = NightlyProgIdGenerator.getProgramID("PLAN", Site.GS, c.getTimeInMillis());
        assertEquals("GS-PLAN20050206", id.stringValue());

        // 2 AM on Feb. 6, 2005
        c = new GregorianCalendar(2005, 1, 6, 2, 0, 0);
        c.setTimeZone(CHILE_TIME);
        id = NightlyProgIdGenerator.getProgramID("PLAN", Site.GS, c.getTimeInMillis());
        assertEquals("GS-PLAN20050206", id.stringValue());
    }

    @Test public void testBorderTimeId() throws Exception {
        SPProgramID id;

        // Just before the end of the night on Feb. 5, 2005
        Calendar c = new GregorianCalendar(2005, 1, 5, ObservingNight.LOCAL_NIGHT_END_HOUR - 1, 59, 59);
        c.setTimeZone(CHILE_TIME);
        id = NightlyProgIdGenerator.getProgramID("PLAN", Site.GS, c.getTimeInMillis());
        assertEquals("GS-PLAN20050205", id.stringValue());

        // Exactly on the start of a new night
        c = new GregorianCalendar(2005, 1, 5, ObservingNight.LOCAL_NIGHT_END_HOUR, 0, 0);
        c.setTimeZone(CHILE_TIME);
        id = NightlyProgIdGenerator.getProgramID("PLAN", Site.GS, c.getTimeInMillis());
        assertEquals("GS-PLAN20050206", id.stringValue());
    }

    @Test public void testDaylightSavingsId() throws Exception {

        SPProgramID id;

        // A few times at the daylight savings transition.
        Calendar[] ca = new Calendar[] {
            new GregorianCalendar(2005, 2, 12, ObservingNight.LOCAL_NIGHT_END_HOUR, 0, 0),
            new GregorianCalendar(2005, 2, 12, 23, 59, 59),
            new GregorianCalendar(2005, 2, 13, 0, 0, 0),
            new GregorianCalendar(2005, 2, 13, 1, 0, 0),
            new GregorianCalendar(2005, 2, 13, ObservingNight.LOCAL_NIGHT_END_HOUR-1, 59, 59),
        };
        for (int i=0; i<ca.length; ++i) {
            ca[i].setTimeZone(CHILE_TIME);
            id = NightlyProgIdGenerator.getProgramID("PLAN", Site.GS, ca[i].getTimeInMillis());
            assertEquals("GS-PLAN20050313", id.stringValue());
        }

        // A few times at the daylight savings transition.
        ca = new Calendar[] {
            new GregorianCalendar(2005, 9, 8, ObservingNight.LOCAL_NIGHT_END_HOUR, 0, 0),
            new GregorianCalendar(2005, 9, 8, 23, 59, 59),
            new GregorianCalendar(2005, 9, 9, 0, 0, 0),
            new GregorianCalendar(2005, 9, 9, 1, 0, 0),
            new GregorianCalendar(2005, 9, 9, ObservingNight.LOCAL_NIGHT_END_HOUR-1, 59, 59),
        };
        for (int i=0; i<ca.length; ++i) {
            ca[i].setTimeZone(CHILE_TIME);
            id = NightlyProgIdGenerator.getProgramID("PLAN", Site.GS, ca[i].getTimeInMillis());
            assertEquals("GS-PLAN20051009", id.stringValue());
        }
    }

    @Test public void testWrapId() throws Exception {
        // December 31, 1999, 8:00 PM
        Calendar c = new GregorianCalendar(1999, 11, 31, 16, 0, 0);
        c.setTimeZone(CHILE_TIME);

        SPProgramID id;
        id = NightlyProgIdGenerator.getProgramID("PLAN", Site.GS, c.getTimeInMillis());
        assertEquals("GS-PLAN20000101", id.stringValue());
    }

    @Test public void testHawaiiZone() throws Exception {
        SPProgramID id;

        // Just before the end of the night on Feb. 5, 2005
        Calendar c = new GregorianCalendar(2005, 1, 5, ObservingNight.LOCAL_NIGHT_END_HOUR - 1, 59, 59);
        c.setTimeZone(HAWAII_TIME);
        id = NightlyProgIdGenerator.getProgramID("PLAN", Site.GN, c.getTimeInMillis());
        assertEquals("GN-PLAN20050205", id.stringValue());

        // Exactly on the start of a new night
        c = new GregorianCalendar(2005, 1, 5, ObservingNight.LOCAL_NIGHT_END_HOUR, 0, 0);
        c.setTimeZone(HAWAII_TIME);
        id = NightlyProgIdGenerator.getProgramID("PLAN", Site.GN, c.getTimeInMillis());
        assertEquals("GN-PLAN20050206", id.stringValue());
    }

    @Test public void testTimeShift() throws Exception {
        // Exactly on the start of a new night
        Calendar c = new GregorianCalendar(2013, 3, 24, ObservingNight.LOCAL_NIGHT_END_HOUR - 1, 0, 0);
        c.setTimeZone(CHILE_TIME);
        SPProgramID id = NightlyProgIdGenerator.getProgramID("PLAN", Site.GS, c.getTimeInMillis());
        assertEquals("GS-PLAN20130424", id.stringValue());

        System.setProperty(NightlyProgIdGenerator.LOCAL_TIME_SHIFT_FACTOR, "3600");
        id = NightlyProgIdGenerator.getProgramID("PLAN", Site.GS, c.getTimeInMillis());
        assertEquals("GS-PLAN20130425", id.stringValue());

        c = new GregorianCalendar(2013, 3, 24, ObservingNight.LOCAL_NIGHT_END_HOUR, 0, 0);
        c.setTimeZone(CHILE_TIME);
        System.setProperty(NightlyProgIdGenerator.LOCAL_TIME_SHIFT_FACTOR, "-3600");
        id = NightlyProgIdGenerator.getProgramID("PLAN", Site.GS, c.getTimeInMillis());
        assertEquals("GS-PLAN20130424", id.stringValue());

        System.clearProperty(NightlyProgIdGenerator.LOCAL_TIME_SHIFT_FACTOR);
    }
}
