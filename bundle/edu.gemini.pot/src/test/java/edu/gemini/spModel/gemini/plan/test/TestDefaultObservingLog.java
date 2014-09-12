package edu.gemini.spModel.gemini.plan.test;

import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.gemini.plan.NightlyRecord;
import edu.gemini.spModel.gemini.plan.WeatherInfo;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.Iterator;
import java.util.List;

//
// Gemini Observatory/AURA
// $Id: TestDefaultObservingLog.java 7487 2006-12-05 15:14:03Z gillies $
//

public final class TestDefaultObservingLog extends TestCase {
    private static final Logger LOG = Logger.getLogger(NightlyRecord.class.getName());

    private static final String[] _testObservations = new String[]{"GN2004A-Q-1-1", "GN2004A-Q-2-1",
                                                                   "GN2004A-Q-3-1", "GN2004A-Q-4-1"};

    private static final WeatherInfo[] _testWeatherInfo = {
        new WeatherInfo(0, "12 deg", "13 kph", "WNW", "30.06 in", "77%", "?", "15%", "some comment 1"),
        new WeatherInfo(1, "11 deg", "12 kph", "WNW", "35.06 in", "73%", "?", "16%", "some other comment 2"),
        new WeatherInfo(2, "10 deg", "10 kph", "WNW", "30.06 in", "70%", "?", "17%", "other comment 3"),
        new WeatherInfo(3, "11 deg", "11 kph", "WNW", "31.06 in", "69%", "?", "18%", "other comment 4"),
    };

    /**
     * Test the initial state of the object
     */
    public void testInitState() {

        NightlyRecord obsLog = new NightlyRecord();

        List l = obsLog.getObservationList();
        assertNotNull(l);
        assertTrue(l.size() == 0);

        l = obsLog.getWeatherLog();
        assertNotNull(l);
        assertTrue(l.size() == 0);
    }

    // private method to convert a <code>String</code> to an <code>SPObservationID</code> object.
    private SPObservationID _getObsID(String observationID) {
        assertNotNull(observationID);

        SPObservationID id = null;
        try {
            id = new SPObservationID(observationID);
        } catch (SPBadIDException ex) {
            fail("_getObsID failed to cerate SPObservationID");
        }
        assertNotNull(id);
        return id;
    }

    // private method to populate the object with a set of test observations
    private void _fillObservations(NightlyRecord obsLog) {
        for (String testObservation : _testObservations) {
            obsLog.addObservation(_getObsID(testObservation));
        }
    }

    // private method to populate the object with a test weather log
    private void _fillWeatherLog(NightlyRecord obsLog) {
        for (WeatherInfo a_testWeatherInfo : _testWeatherInfo) {
            obsLog.addWeatherInfo(a_testWeatherInfo);
        }
    }

    // create an object to test with
    private NightlyRecord _createDefaultObservingLog() {
        NightlyRecord obsLog = new NightlyRecord();
        _fillObservations(obsLog);
        _fillWeatherLog(obsLog);
        obsLog.setDayObservers("D1,D2,D3");
        obsLog.setNightObservers("N1, N2");
        obsLog.setSSA("S1, S2");
        obsLog.setDataProc("DP1");
        obsLog.setFilePrefix("/tmp/files");
        obsLog.setCCSoftwareVersion("1.0");
        obsLog.setDCSoftwareVersion("2.0");
        obsLog.setSoftwareVersionNote("some note");
        obsLog.setNightComment("some comment");
        return obsLog;
    }

    /**
     * Test the addObservations method
     */
    public void testAddObservations() {
        NightlyRecord obsLog = _createDefaultObservingLog();
        assertEquals(_testObservations.length, obsLog.getObservationListSize());
    }

    /**
     * Test adding items to the weather log
     */
    public void testAddToWeatherLog() {
        NightlyRecord obsLog = _createDefaultObservingLog();
        assertEquals(_testWeatherInfo.length, obsLog.getWeatherLogSize());
    }

    /**
     * Dump the list of observations
     *
     * @param obsLog  dump this observing log
     */
    public void dumpList(NightlyRecord obsLog) {
        Iterator it = obsLog.observationIterator();
        while (it.hasNext()) {
            SPObservationID obsID = (SPObservationID) it.next();
            LOG.log(Level.INFO, obsID.toString());
        }
    }

    /**
     * Test the remove observations method
     */
    public void testRemoveObservations() {
        NightlyRecord obsLog = _createDefaultObservingLog();

        assertEquals(_testObservations.length, obsLog.getObservationListSize());

        // remove 1 - second in test list
        boolean success = false;
        try {
            success = obsLog.removeObservation(new SPObservationID(_testObservations[1]));
        } catch (SPBadIDException ex) {
            fail("_failed to remove an observations ID");
        }
        assertTrue(success);

        assertEquals(_testObservations.length - 1, obsLog.getObservationListSize());
        List l = obsLog.getObservationList();
        assertEquals("0", _testObservations[0], ((SPObservationID) (l.get(0))).stringValue());
        assertEquals("1", _testObservations[2], ((SPObservationID) (l.get(1))).stringValue());
        assertEquals("2", _testObservations[3], ((SPObservationID) (l.get(2))).stringValue());

        // Remove 2 - third in test list
        try {
            success = obsLog.removeObservation(new SPObservationID(_testObservations[2]));
        } catch (SPBadIDException ex) {
            fail("_failed to remove an observations ID");
        }
        assertTrue(success);

        assertEquals(_testObservations.length - 2, obsLog.getObservationListSize());
        l = obsLog.getObservationList();
        assertEquals(_testObservations.length - 2, l.size());

        assertEquals("3", _testObservations[0], ((SPObservationID) (l.get(0))).stringValue());
        assertEquals("4", _testObservations[3], ((SPObservationID) (l.get(1))).stringValue());

    }

    /**
     * Test the removing items from the  weather log
     */
    public void testRemoveFromWeatherLog() {
        NightlyRecord obsLog = _createDefaultObservingLog();
        assertEquals(_testWeatherInfo.length, obsLog.getWeatherLogSize());

        // remove 1 - second in test list
        boolean success = obsLog.removeWeatherlog(_testWeatherInfo[1]);
        assertTrue(success);

        assertEquals(_testWeatherInfo.length - 1, obsLog.getWeatherLog().size());
        List<WeatherInfo> l = obsLog.getWeatherLog();
        assertEquals("0", _testWeatherInfo[0], l.get(0));
        assertEquals("1", _testWeatherInfo[2], l.get(1));
        assertEquals("2", _testWeatherInfo[3], l.get(2));
    }

    /**
     * Test the getParamSet method
     */
    public void testParamSetOut() {
        NightlyRecord obsLog = _createDefaultObservingLog();

        PioXmlFactory fact = new PioXmlFactory();

        ParamSet pset = obsLog.getParamSet(fact);
        assertNotNull(pset);
    }

    /**
     * Test the setParamSet method
     */
    public void testParamSetIn() {
        NightlyRecord obsLog = _createDefaultObservingLog();

        PioXmlFactory fact = new PioXmlFactory();
        ParamSet pset = obsLog.getParamSet(fact);
        assertNotNull(pset);

        NightlyRecord obsLog2 = new NightlyRecord();
        obsLog2.setParamSet(pset);

        // Verify that it's setup correctly
        assertEquals(_testObservations.length, obsLog2.getObservationListSize());
        List l = obsLog2.getObservationList();
        Iterator it = l.iterator();
        int i = 0;
        while (it.hasNext()) {
            SPObservationID obsID = (SPObservationID) it.next();
            assertEquals(_testObservations[i++], obsID.toString());
        }

        i = 0;
        for (WeatherInfo weatherInfo : obsLog2.getWeatherLog()) {
            assertEquals(_testWeatherInfo[i++], weatherInfo);
        }

        assertEquals(obsLog.getDayObservers(), obsLog2.getDayObservers());
        assertEquals(obsLog.getNightObservers(), obsLog2.getNightObservers());
        assertEquals(obsLog.getSSA(), obsLog2.getSSA());
        assertEquals(obsLog.getDataProc(), obsLog2.getDataProc());
        assertEquals(obsLog.getFilePrefix(), obsLog2.getFilePrefix());
        assertEquals(obsLog.getCCSoftwareVersion(), obsLog2.getCCSoftwareVersion());
        assertEquals(obsLog.getDCSoftwareVersion(), obsLog2.getDCSoftwareVersion());
        assertEquals(obsLog.getSoftwareVersionNote(), obsLog2.getSoftwareVersionNote());
        assertEquals(obsLog.getNightComment(), obsLog2.getNightComment());
    }

    public static Test suite() {
        return new TestSuite(TestDefaultObservingLog.class);
    }

}
