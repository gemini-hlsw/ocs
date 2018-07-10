package edu.gemini.qpt.ui.html;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import edu.gemini.lch.services.model.*;
import edu.gemini.qpt.core.listeners.LimitsListener;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.core.util.LttsServicesClient;
import edu.gemini.shared.util.StringUtil;
import jsky.coords.WorldCoords;
import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Marker;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.Alloc.Grouping;
import edu.gemini.qpt.core.Marker.Severity;
import edu.gemini.qpt.core.util.ImprovedSkyCalc;
import edu.gemini.qpt.core.util.Interval;
import edu.gemini.qpt.shared.util.TimeUtils;
import edu.gemini.qpt.ui.util.CancelledException;
import edu.gemini.qpt.ui.util.ColorWheel;
import edu.gemini.qpt.ui.util.ProgressModel;
import edu.gemini.qpt.ui.util.TimePreference;
import edu.gemini.qpt.ui.view.visit.VisitController;
import edu.gemini.qpt.ui.view.visualizer.PlotViewer;
import edu.gemini.qpt.ui.view.visualizer.Visualizer;
import edu.gemini.skycalc.TwilightBoundType;
import edu.gemini.skycalc.TwilightBoundedNight;

public class ScheduleDocument {

    private static final Logger LOGGER = Logger.getLogger(ScheduleDocument.class.getName());

    private final Schedule schedule;
    private final ProgressModel pi;
    private final File root;
    private final String prefix;
    private int imageIndex = 0;
    private final boolean showQcMarkers;
    private final boolean utc;

    private final PlotViewer pv;
    private final Visualizer viz;

    /**
     * ProgressInfo will be incremented each time getImageFile() is called.
     *
     * @param schedule
     * @param pi
     */
    public ScheduleDocument(Schedule schedule, ProgressModel pi, File root, String prefix, boolean showQcMarkers, boolean utc) {
        this.schedule = schedule;
        this.pi = pi;
        this.root = root;
        this.prefix = prefix;
        this.showQcMarkers = showQcMarkers;
        this.utc = utc;
        pv = new PlotViewer(new VisitController(), false, utc ? TimePreference.UNIVERSAL : TimePreference.LOCAL);
        viz = pv.getControl();
        viz.setDoubleBuffered(false); // REL-1325: turn double buffering off for off-screen painting
    }

    public Schedule getSchedule() {
        return schedule;
    }

    /**
     * LCH-118: List propagation windows in the published queue plan.
     */
    public List<String> getClearanceWindows(Alloc a) {
        String fmt = "HH:mm:ss";
        Observation observation = LttsServicesClient.getInstance().getObservation(a.getObs());
        if (observation != null) {

            List<String> result = new ArrayList<String>();
            ObservationTarget scienceTarget = observation.getScienceTarget();
            LaserTarget scienceLaserTarget = scienceTarget.getLaserTarget();

            // LCH-183: Access list of dates where target rises above/below limit
            // REL-1324: Simplifying the code assuming that only the times for the science target are relevant
            // (times for all other targets should be practically identical)
            // -- list all propagation windows, list in bold the ones that overlap with the allocated time
            for (ClearanceWindow clearance : scienceLaserTarget.getClearanceWindows()) {
                long start = clearance.getStart().getTime();
                long end = clearance.getEnd().getTime();
                Interval interval = new Interval(start, end);
                String length = TimeUtils.msToHHMMSS(end - start);
                boolean overlaps = (start < a.getEnd() && end > a.getStart());
                StringBuffer sb = new StringBuffer();

                // -- bold if allocation is overlapping with clearance window
                if (overlaps) {
                    sb.append("<b>");
                }

                // construct the string for this clearance window
                sb.append(formatDate(fmt, start));
                sb.append(" => ");
                sb.append(formatDate(fmt, end));
                sb.append(" = ");
                sb.append(length);
                // LCH-1324: append a list of non-science targets with clearance windows different to the science target
                List<String> diff = getNonScienceTargetsWithDifferentClearance(observation, clearance);
                if (diff.size() > 0) {
                    sb.append("; Different for ").append(StringUtil.mkString(diff, "", ", ", ""));
                }
                // also list rise and set time in case the science target rises/sets above/below the laser limit during the allocation
                String sunsetSunrise = getLaserLimits(observation, clearance);
                if (!sunsetSunrise.isEmpty()) {
                    sb.append(" ").append(sunsetSunrise);
                }

                // -- turn bold off if necessary
                if (overlaps) {
                    sb.append("</b>");
                }

                result.add(sb.toString());
            }

            return result;

        } else {

            return Collections.emptyList();
        }
    }

    /**
     * Gets a collection with the names of all observation targets that have not the given clearance window.
     * Use this to get an array of all target names that have different clearance windows from the base.
     * @param observation
     * @return
     */
    // LCH-1324
    private List<String> getNonScienceTargetsWithDifferentClearance(Observation observation, ClearanceWindow clearance) {
        List<String> names = new ArrayList<String>();
        LaserTarget scienceLaserTarget = observation.getScienceTarget().getLaserTarget();
        for (ObservationTarget t : observation.getTargetsSortedByType()) {
            // only look at observation targets with a laser target different from the science target
            if (t.getLaserTarget() != scienceLaserTarget) {
                Boolean found = false;
                for (ClearanceWindow w : t.getLaserTarget().getClearanceWindows()) {
                    if (w.getStart().equals(clearance.getStart()) && w.getEnd().equals(clearance.getEnd())) {
                        found = true;
                    }
                }
                if (!found) {
                    names.add(t.getType() + ": " + t.getName());
                }
            }
        }
        return names;
    }

    private String getLaserLimits(Observation observation, ClearanceWindow clearance) {
        String fmt = "HH:mm:ss";
        LaserTarget scienceTarget = observation.getScienceTarget().getLaserTarget();
        for (Visibility.Interval visibility : scienceTarget.getVisibility().getAboveLaserLimit()) {
            long vStart = visibility.getStart().getTime() / 1000; // get rid of milliseconds
            long vEnd   = visibility.getEnd().getTime() / 1000;
            long cStart = clearance.getStart().getTime() / 1000;
            long cEnd   = clearance.getEnd().getTime() / 1000;
            // check if target rises above laser limit during this clearance window
            if (vStart > cStart && vStart < cEnd) {
                return "(rises above " + LimitsListener.MIN_ELEVATION_ERROR_LIMIT + "&deg; at "
                        + formatDate(fmt, visibility.getStart().getTime()) + ")"; // LCH-190 use &deg; in HTML
            }
            // check if target sets below laser limit during this clearance window
            if (vEnd > cStart && vEnd < cEnd) {
                return "(sets below " + LimitsListener.MIN_ELEVATION_ERROR_LIMIT + "&deg; at "
                        + formatDate(fmt, visibility.getEnd().getTime()) + ")"; // LCH-190 use &deg; in HTML
            }
        }

        return "";
    }

    public File getImageFile(Variant v) throws IOException, CancelledException {

        // ProgressInfo
        if (pi.isCancelled()) throw new CancelledException();
        pi.work();
        pi.setMessage(v.getName());

        // Draw visualization in a BufferedImage
        BufferedImage image = new BufferedImage(640, 240, BufferedImage.TYPE_INT_ARGB);
        viz.setSize(new Dimension(image.getWidth(), image.getHeight()));
        pv.setModel(v);
        viz.paint(image.createGraphics());
        image.flush();

        // Write the image to a temp file.
        File imageFile = new File(root, prefix + "-" + (++imageIndex) + ".png");
        LOGGER.fine("Writing visualization \"" + v + "\" to " + imageFile.getAbsolutePath());
        ImageIO.write(image, "png", imageFile);

        // Done
        if (pi.isCancelled()) throw new CancelledException();
        return imageFile;

    }

    public String trunc(String s, int length) {
        if (s == null) return "";
        return s.length() <= length ? s : s.substring(0, length) + "&hellip;";
    }

    public String formatDate(String pattern, Long ms) {
        if (utc) {
            return formatDate(pattern, ms, "UTC") + " UTC";
        } else {
            if (ms == null) return "";
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            df.setTimeZone(schedule.getSite().timezone());
            return df.format(new Date(ms));
        }
    }

    public String formatDate(String pattern, Long ms, String tz) {
        if (ms == null) return "";
        SimpleDateFormat df = new SimpleDateFormat(pattern);
        df.setTimeZone(TimeZone.getTimeZone(tz));
        return df.format(new Date(ms));
    }

    public String formatHHMMSS(long ms) {
        return TimeUtils.msToHHMM(ms);
    }

    public TimePreference getTimePreference() {
        return (utc) ? TimePreference.UNIVERSAL : TimePreference.LOCAL;
    }

    public String getColor(Severity sev) {
        if (sev != null) {
            switch (sev) {
                case Error:
                    return "red";
                case Notice:
                    return "blue";
                case Warning:
                    return "orange";
                case Info:
                    return "gray";
            }
        }
        return "black";
    }

    public boolean isEmpty(Object o) {
        return o == null || o.toString().trim().length() == 0;
    }

    public SortedSet<Marker> getMarkers(Alloc a) {
        SortedSet<Marker> ret = a.getMarkers();
        for (Iterator<Marker> it = ret.iterator(); it.hasNext(); ) {
            Marker marker = it.next();
            if (marker.isQcOnly() && !showQcMarkers)
                it.remove();
        }
        return ret;
    }

    public String getCond(byte value) {
        switch (value) {
            case 0:
                return "-";
            case 100:
                return "A";
            default:
                return Byte.toString(value);
        }
    }

    public RiseTransitSet getRTS(Alloc alloc) {
        return new RiseTransitSet(schedule.getSite(), alloc.getObs()::getCoords, alloc.getStart());
    }

    public String getSteps(Alloc a) {
        return (a.getFirstStep() + 1) + " - " + (a.getLastStep() + 1);
    }

    public TwilightBoundedNight getTwilightBoundedNight() {
        return new TwilightBoundedNight(TwilightBoundType.NAUTICAL, schedule.getStart(), schedule.getSite());
    }

    public SortedSet<Object> getEvents(Variant v) {

        SortedSet<Object> ret = new TreeSet<Object>(new Comparator<Object>() {

            public int compare(Object o1, Object o2) {
                long t1 = time(o1), t2 = time(o2);
                return Long.signum(t1 - t2);
            }

            long time(Object o) {
                if (o instanceof SimpleEvent) {
                    return ((SimpleEvent) o).getTime();
                } else if (o instanceof Alloc) {
                    return ((Alloc) o).getStart();
                }
                throw new IllegalArgumentException(o.toString());
            }

        });

        SortedSet<Alloc> allocs = v.getAllocs();

        // Add the allocs and a note at the end.
        ret.addAll(allocs);
        ret.add(new SimpleEvent(allocs.last().getEnd(), "End of plan variant."));

        // Add nautical twilight info.
        TwilightBoundedNight nautical = new TwilightBoundedNight(TwilightBoundType.NAUTICAL, schedule.getStart(), schedule.getSite());
        final TimeZone timezone = utc ? TimeZone.getTimeZone("UTC") : schedule.getSite().timezone();
        ret.add(new SimpleEvent(nautical.getStartTimeRounded(timezone), "Evening 12&deg; Twilight"));
        ret.add(new SimpleEvent(nautical.getEndTimeRounded(timezone), "Morning 12&deg; Twilight"));

        // Find illuminated fraction
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(nautical.getEndTime());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        final ImprovedSkyCalc isc = new ImprovedSkyCalc(schedule.getSite());
        isc.calculate(new WorldCoords(), cal.getTime(), true);

        // Calculate LST at midnight
        long lst = isc.getLst(cal.getTime()).getTime();
        String message =
                "LST at midnight " + formatDate("HH:mm ss", lst, "UTC") +
                        String.format("  &bull; Moon illuminated fraction %1.3f", isc.getLunarIlluminatedFraction());

        ret.add(new SimpleEvent(cal.getTimeInMillis(), message));

        // Get sun[rise|set] info.
        TwilightBoundedNight local = new TwilightBoundedNight(TwilightBoundType.OFFICIAL, schedule.getStart(), schedule.getSite());
        Interval sunBoundedNight = new Interval(local.getStartTimeRounded(timezone), local.getEndTimeRounded(timezone));

        // Find moon info
        MoonRiseTransitSet mrts = new MoonRiseTransitSet(schedule.getSite(), sunBoundedNight.getStart());

        // Add sunrise, sunset
        ret.add(new SimpleEvent(sunBoundedNight.getStart(), "Sunset"));
        ret.add(new SimpleEvent(sunBoundedNight.getEnd(), "Sunrise"));

        // Add moonrise/set if applicable
        if (sunBoundedNight.contains(mrts.getRise())) {
            ret.add(new SimpleEvent(mrts.getRise(), "Moonrise."));
        }
        if (sunBoundedNight.contains(mrts.getSet())) {
            ret.add(new SimpleEvent(mrts.getSet(), "Moonset."));
        }

        return ret;

    }

    public boolean isAlloc(Object o) {
        return o instanceof Alloc;
    }

    public boolean isSimpleEvent(Object o) {
        return o instanceof SimpleEvent;
    }

    public static class SimpleEvent {

        private final long time;
        private final String text;

        public SimpleEvent(final long time, final String text) {
            super();
            this.time = time;
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public long getTime() {
            return time;
        }

    }


    //    private static final DateFormat DF_MMMM= new  SimpleDateFormat("MMMM");
    private static final DateFormat DF_MMMM_D = new SimpleDateFormat("MMMM d");
    private static final DateFormat DF_D_YYYY = new SimpleDateFormat("d, yyyy");
    private static final DateFormat DF_MMMM_D_YYYY = new SimpleDateFormat("MMMM d, yyyy");

    public String getDateRangeString(Date start, Date end) {

        Calendar c1 = Calendar.getInstance();
        c1.setTime(start);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(end);

        int y1 = c1.get(Calendar.YEAR);
        int y2 = c2.get(Calendar.YEAR);

        if (y1 != y2) {

            // December 32, 2007 - January 14, 2008
            return DF_MMMM_D_YYYY.format(start) + " &rarr; " + DF_MMMM_D_YYYY.format(end);

        } else if (c1.get(Calendar.MONTH) != c2.get(Calendar.MONTH)) {

            // June 24 - July 7, 2007
            return DF_MMMM_D.format(start) + " &rarr; " + DF_MMMM_D_YYYY.format(end);


        } else if (c1.get(Calendar.DAY_OF_MONTH) != c2.get(Calendar.DAY_OF_MONTH)) {

            // June 7-12, 2006
            return DF_MMMM_D.format(start) + " &rarr; " + DF_D_YYYY.format(end);

        } else {

            // June 8, 2006
            return DF_MMMM_D_YYYY.format(start);
        }

    }

    public String getDateRangeString(long start, long end) {
        return getDateRangeString(new Date(start), new Date(end));
    }

//    public static void main(String[] args) {
//        ScheduleDocument doc = new ScheduleDocument(null, null, null, null);
//        long start = System.currentTimeMillis();
//        long end = start + 1;
//        for (int i = 1; i < 30; i++) {
//            System.out.println(doc.getDateRangeString(start, end));
//            end += TimeUtils.MS_PER_DAY * 7;
//        }
//    }

    public String getStyle(Object o) {

        if (o instanceof Alloc) {
            int i = ((Alloc) o).getGroupIndex();
            if (i == -1) {
                return "padding-left: 1.25em";
            } else {
                return "padding-left: 0.5em; border-left: 0.75em solid #" + Integer.toHexString(ColorWheel.get(i).getRGB()).substring(2);
            }
        } else {
            return "padding-left: 1.25em";
        }

    }

    public boolean isGroupStart(Alloc a) {
        return a.getGrouping() == Grouping.FIRST;
    }


    public SortedSet<String> getUniqueConfigs(Variant v) {
        SortedSet<String> ret = new TreeSet<String>();
        for (Alloc a : v.getAllocs())
            ret.add(a.getObs().getInstrumentStringWithConfig().replace("\u03BB", "&lambda;"));
        return ret;
    }

    public String getFilteredOptions(Obs o) {
        return o.getOptionsString().replace("\u03BB", "&lambda;");
    }
}








