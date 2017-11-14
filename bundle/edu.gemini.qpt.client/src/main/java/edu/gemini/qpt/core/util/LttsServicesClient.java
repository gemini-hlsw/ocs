package edu.gemini.qpt.core.util;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import edu.gemini.lch.services.model.*;
import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.listeners.LimitsListener;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.shared.util.StructuredProgramID;
import edu.gemini.qpt.shared.util.TimeUtils;
import edu.gemini.skycalc.TwilightBoundType;
import edu.gemini.skycalc.TwilightBoundedNight;
import edu.gemini.spModel.core.Peer;
import edu.gemini.spModel.core.Site;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.ws.rs.core.MediaType;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * A client for the LTTS web services.
 * This service is used to get the clearance windows for the laser targets as provided by the Laser Clearance House
 * (LCH). The service {host:port}/ltts-services/nights/{yyyy}/{MM}/{dd} returns all relevant LTTS information
 * for a full night (observations, laser targets and their clearance windows).
 * Note: The date passed down denotes the date on which the night starts (sunrise), e.g. 2014-01-14 for the night
 * starting on 2014-01-14 (sunset) and ending on 2014-01-15 (sunrise). This is different from how
 * dates are assigned to nights in other apps, e.g. the QPT which uses the date of the end of the night.
 * Potentially this should be done the same way in LTTS but for now that's how it is.
 */
public class LttsServicesClient {

    private static final Logger LOGGER = Logger.getLogger(LttsServicesClient.class.getName());

    // See LCH-182: http://localhost:1973/ltts-services/nights?date=20130105&laserAltitudeLimit=40&full=true
//    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        DATE_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    // URL for LCH web service. Set in Activator from bundle.properties
    public static String LTTS_SERVICES_NORTH_URL = "http://localhost:1973/ltts-services/test";
    public static String LTTS_SERVICES_SOUTH_URL = "http://localhost:1973/ltts-services/test";

    private static LttsServicesClient instance;

    // Maps obs id to observation
    private final Map<String, Observation> observationMap;

    // True if OK, false if could not connect to server
    private boolean status;

    /**
     * Returns the the current instance (can be updated by calling newInstance())
     */
    public static LttsServicesClient getInstance() {
        return instance;
    }

    /**
     * Creates and returns a new instance of this class.
     *
     * @param time The date of the day before the observation night in ms (i.e. the date on which the night starts/sunset).
     * @return a new instance of this class
     */
    public static LttsServicesClient newInstance(long time, Peer peer) {
        long sunset = getStartOfNight(peer.site, time);
        LOGGER.info("LTTS Service: new instance for: " + new Date(sunset) + " at " + peer);
        instance = new LttsServicesClient(sunset, peer);
        return instance;
    }

    /**
     * Resets the instance to null (for example before opening a new file with a potentially different date)
     */
    public static void clearInstance() {
        instance = null;
    }

    LttsServicesClient(long start, Peer peer) {
        Client client = Client.create();
        Date date = new Date(start);
        // See LCH-182: http://localhost:1973/ltts-services/nights?date=20130105&laserAltitudeLimit=40&full=true
        String baseUrl = (peer.site == Site.GN) ? LTTS_SERVICES_NORTH_URL : LTTS_SERVICES_SOUTH_URL;

//        String baseUrl = String.format("http://%s:%d/ltts-services", peer.host, peer.port);

        String url = baseUrl + "/nights?date=" + DATE_FORMAT.format(date)
                + "&laserLimit=" + LimitsListener.MIN_ELEVATION_ERROR_LIMIT
                + "&full=true";
        LOGGER.info("Accessing LCH web service at " + url);

        observationMap = new HashMap<String, Observation>();

        try {
            WebResource webResource = client.resource(url);
            ClientResponse response = webResource.accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
            if (response.getStatus() != 404) {
                if (response.getStatus() != 200) {
                    throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
                }

                NightFull night = response.getEntity(NightFull.class);
                for (Observation obs : night.getObservations()) {
                    try {
                        // NOTE: Not all observations are planned with QPT; E.g. engineering observations are currently
                        // NOT planned in QPT, their IDs (for example Chad-LGS-123) provoke an exception when trying
                        // to create the corresponding structured ID. For now, we just ignore those observations.
                        observationMap.put(getStructuredObsID(obs.getId()), obs);
                    } catch (IllegalArgumentException e) {
                        // intentionally left blank, filter observations with IDs QPT does not understand
                    }
                }
                //printNight(night);
            }
            status = true;
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.warning("Could not access LCH web service at " + url);
            status = false;
        } finally {
            client.destroy();
        }
    }

    // Returns the observation id in the form used in the qpt, given a normal OT style observation id
    private String getStructuredObsID(String obsId) {
        int i = obsId.lastIndexOf('-');
        if (i == -1) return obsId;
        String progId = obsId.substring(0, i);
        String obsNumber = obsId.substring(i + 1);
        return new StructuredProgramID(progId).getShortName() + " [" + obsNumber + "]";
    }


    /**
     * Returns true if the LCH web service was contacted sucessfully to read the data stored here.
     */
    public boolean getStatus() {
        return status;
    }

    /**
     * Displays a warning message if the web service could not be contacted
     *
     * @param frame the parent frame
     */
    public void showStatus(final JFrame frame) {
        if (!status) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(
                            frame,
                            "Could not contact the LCH web service to get the laser clearance information.",
                            "LCH Web Service Not Found",
                            JOptionPane.WARNING_MESSAGE);
                }
            });
        }
    }


    /**
     * Returns the Observation (JAXB) corresponding to the given Obs (qpt) object.
     */
    public Observation getObservation(Obs obs) {
        return observationMap.get(obs.getObsId());
    }

    /**
     * Returns a warning message if laser shuttering windows intersect the given time allocation, otherwise null.
     */
    public String getShutterWindowWarningMessage(Alloc a) {
        int count = 0;
        long overlapTime = 0;
        long totalTime = 0;
        Observation observation = getObservation(a.getObs());
        if (observation != null) {
            for (LaserTarget laserTarget : observation.getLaserTargets()) {
                for (ShutteringWindow shutteringWindow : laserTarget.getShutteringWindows()) {
                    Interval interval = new Interval(shutteringWindow.getStart().getTime(), shutteringWindow.getEnd().getTime());
                    if (a.overlaps(interval, Interval.Overlap.EITHER)) {
                        count++;
                        totalTime += a.getLength();
                        // Assuming partial overlap is the same as complete overlap, since you always have to stop for the complete shutter
                        overlapTime += interval.getLength();
                    }
                }
            }
        }

        if (count != 0) {
            String s = (count > 1) ? "s" : "";
            double percent = 100. * overlapTime / totalTime;
            String timeStr = TimeUtils.msToHHMMSS(overlapTime);
            return String.format("LGS Observation overlaps with %d "
                    + "shuttering window%s with a total duration of %s "
                    + "(%2.1f%% of total observation time).",
                    count, s, timeStr, percent);
        }
        return null;
    }

    /**
     * Returns the interval's end time in ms, adjusted for the amount of overlaping time of any laser shutter windows
     * in the given time interval, plus 3 minutes per overlap.
     *
     * @param obs the observation
     * @param a   the allocated time interval
     * @return the interval end time plus the amount of overlap in ms plus 3 minutes per overlap
     */
    public long getShutterOverlap(Obs obs, Interval a) {
        Observation observation = getObservation(obs);
        if (observation != null) {
            Set<Interval> set = new HashSet<Interval>(); // Used to ignore duplicate intervals
            return getShutterOverlap(observation, a, set);
        }
        return a.getEnd();
    }

    private long getShutterOverlap(Observation observation, Interval a, Set<Interval> set) {
        long overlapTime = 0;
        for (ObservationTarget observationTarget : observation.getTargetsSortedByType()) {
            for (ShutteringWindow shutteringWindow : observationTarget.getLaserTarget().getShutteringWindows()) {
                Interval interval = new Interval(shutteringWindow.getStart().getTime(), shutteringWindow.getEnd().getTime());
                if (a.overlaps(interval, Interval.Overlap.EITHER)) {
                    if (set.contains(interval)) continue;
                    set.add(interval); // do this here in case the added time overlaps yet another window!
                    // Assuming partial overlap is the same as complete overlap, since you always have to stop for the complete shutter.
                    // See LCH-153: Simple formula just adds 3 minutes + overlap time (for each overlap)
                    overlapTime += interval.getLength() + 3 * TimeUtils.MS_PER_MINUTE;
                }
            }
        }
        if (overlapTime != 0L) {
            // catch any newly aquired overlaps!
            return getShutterOverlap(observation, new Interval(a.getStart(), a.getEnd() + overlapTime), set);
        }
        return a.getEnd();
    }


    // Debugging help
    private void printNight(NightFull night) {
        List<Observation> obsList = night.getObservations();
        for (Observation observation : obsList) {
            System.out.println("Observation = " + observation.getId());
            try {
                for(Visibility.Interval interval : observation.getScienceTarget().getLaserTarget().getVisibility().getAboveLaserLimit()) {
                    System.out.println("    aboveAltitudeLimit = " + DATE_TIME_FORMAT.format(interval.getStart()));
                    System.out.println("    belowAltitudeLimit = " + DATE_TIME_FORMAT.format(interval.getEnd()));
                }
            } catch(Exception e) {
                e.printStackTrace();
            }

            for (ObservationTarget observationTarget : observation.getTargetsSortedByType()) {
                List<ClearanceWindow> clearanceWindows = observationTarget.getLaserTarget().getClearanceWindows();
                List<ShutteringWindow> shutteringWindows = observationTarget.getLaserTarget().getShutteringWindows();
                for (ClearanceWindow clearanceWindow : clearanceWindows) {
                    System.out.println("    clearanceWindow: " + DATE_TIME_FORMAT.format(clearanceWindow.getStart())
                            + " - " + DATE_TIME_FORMAT.format(clearanceWindow.getEnd())
                            + " for " + observationTarget.getType() + ": " + observationTarget.getName());
                }
                for (ShutteringWindow shutteringWindow : shutteringWindows) {
                    System.out.println("    shutteringWindow: " + DATE_TIME_FORMAT.format(shutteringWindow.getStart())
                            + " - " + DATE_TIME_FORMAT.format(shutteringWindow.getEnd())
                            + " for " + observationTarget.getType() + ": " + observationTarget.getName());
                }
            }
        }
    }


    /**
     * Returns the local time of sunset of the night that covers the given time at a site.
     * @param site
     * @param time
     * @return
     */
    private static long getStartOfNight(Site site, long time) {
        return new TwilightBoundedNight(TwilightBoundType.OFFICIAL, time, site).getStartTime();
    }

}
