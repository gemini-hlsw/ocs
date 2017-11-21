package jsky.app.ot.too;

import edu.gemini.skycalc.TwilightBoundedNight;
import edu.gemini.skycalc.Interval;
import edu.gemini.skycalc.Union;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.ObsSchedulingReport;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * A panel that displays a TOO alert according to the spec in SCT-233.
 */
public final class TooAlertPanel extends JPanel {
    private static final String TIME_ONLY_PATTERN = "HH:mm:ss";
    private static final String TIME_DATE_PATTERN = "YYYY-MMM-dd HH:mm:ss";
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static String formatTimeOnly(long time) {
        return format(time, UTC, TIME_ONLY_PATTERN);
    }

    private static String formatDateAndTime(long time) {
        return format(time, UTC, TIME_DATE_PATTERN);
    }

    private static String format(long time, TimeZone zone, String pattern) {
        final DateTimeFormatter f = DateTimeFormatter.ofPattern(pattern).withZone(zone.toZoneId());
        return f.format(Instant.ofEpochMilli(time));
    }

    public TooAlertPanel(ObsSchedulingReport report) {
        super(new GridBagLayout());

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        int gridy = 0;

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weighty = 1.0;

        // Introduction "A TOO observation became ready at ___."
        String msg = String.format("A TOO observation became ready at UTC %s:",
                                   formatDateAndTime(report.getCurrentTime()));
        JLabel lab = new JLabel(msg);
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.gridx     = 0;
        gbc.gridy     = gridy++;
        gbc.insets    = new Insets(0, 0, 5, 0);
        gbc.gridwidth = 2;
        gbc.weightx   = 1.0;
        add(lab, gbc);

        // Obs ID and title of the observation.
        String titleStr = _formatTitleString(report);
        if (!"".equals(titleStr)) {
            lab = new JLabel(titleStr);
            Font f = lab.getFont();
            f = f.deriveFont(Font.BOLD, (float)(f.getSize() * 1.25));
            lab.setFont(f);

            gbc.anchor    = GridBagConstraints.CENTER;
            gbc.fill      = GridBagConstraints.HORIZONTAL;
            gbc.gridx     = 0;
            gbc.gridy     = gridy++;
            gbc.insets    = new Insets(10, 0, 10, 0);
            gbc.gridwidth = 2;
            gbc.weightx   = 1.0;
            add(lab, gbc);
        }

        // Position and observability statement.
//        MultilineLabel mlab = new MultilineLabel(_formatSummary(report));
        JLabel mlab = new JLabel(_formatSummary(report));
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.gridx     = 0;
        gbc.gridy     = gridy++;
        gbc.gridwidth = 2;
        gbc.insets    = new Insets(5, 0, 5, 0);
        gbc.weightx   = 1.0;
        add(mlab, gbc);

        // Observing conditions
        lab = new JLabel("Observing Conditions:");
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.fill    = GridBagConstraints.NONE;
        gbc.gridx   = 0;
        gbc.gridy   = gridy;
        gbc.gridwidth = 1;
        gbc.insets  = new Insets(5, 0, 5, 5);
        gbc.weightx = 0.0;
        add(lab, gbc);

        lab = new JLabel(_formatObsConditions(report));
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.gridx   = 1;
        gbc.gridy   = gridy++;
        gbc.gridwidth = 1;
        gbc.insets  = new Insets(5, 5, 5, 0);
        gbc.weightx = 1.0;
        add(lab, gbc);

        // Timing Windows
        lab = new JLabel("Timing Windows:");
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.fill    = GridBagConstraints.NONE;
        gbc.gridx   = 0;
        gbc.gridy   = gridy;
        gbc.gridwidth = 1;
        gbc.insets  = new Insets(5, 0, 5, 5);
        gbc.weightx = 0.0;
        add(lab, gbc);

        java.util.List<SPSiteQuality.TimingWindow> wins = report.getTimingWindows();
        for (SPSiteQuality.TimingWindow win : wins) {
            lab = new JLabel(_formatTimingWindow(win));
            gbc.anchor  = GridBagConstraints.WEST;
            gbc.fill    = GridBagConstraints.HORIZONTAL;
            gbc.gridx   = 1;
            gbc.gridy   = gridy++;
            gbc.gridwidth = 1;
            gbc.insets  = new Insets(5, 5, 5, 0);
            gbc.weightx = 1.0;
            add(lab, gbc);
        }
    }

    private static String _formatTitleString(ObsSchedulingReport report) {
        StringBuilder res = new StringBuilder();

        Object obsId = report.getObservationId();
        if (obsId != null) {
            res.append(obsId.toString());
            res.append(" ");
        }

        String obsTitle = report.getObservationDataObject().getEditableTitle();
        if (obsTitle != null) {
            res.append('\"').append(obsTitle).append('\"');
        }

        return res.toString();
    }

    private static String _formatSummary(ObsSchedulingReport report) {

        // If there was no target, we can't tell anything.
        if (report.getCoordinates() == null) {
            return "There is no target information for this observation.";
        }

        // Figure out where the target is at the moment.
        String prefix = "It";
        String positionMessage;
        long curTime = report.getCurrentTime();
        TwilightBoundedNight night = report.getNight();
        if (night == null) {
            return "Cannot compute target observability because the site (north or south) is not specified.";
        }
        if (!night.includes(curTime)) {
            // It isn't even daytime yet.
            positionMessage = String.format("It is daytime (twilight starts at UTC %s).",
                                        formatDateAndTime(night.getStartTime()));
            prefix = "The target";
        } else {
            // Nighttime, so the position in the sky is vaguely relevant.
            if (report.getAltitude() < 0) {
                positionMessage = "The target is below the horizon.";
            } else {
                positionMessage = String.format("The target is at airmass %.2f and %s.",
                    report.getAirmass(),
                    report.getDisposition());
            }
        }

        // Determine whether it will be observable.
        String obsMessage;
        Union<Interval> elevationIntervals = report.getElevationIntervals();
        Union<Interval> timingIntervals    = report.getTimingIntervals();
        Union<Interval> combinedIntervals  = new Union<Interval>(elevationIntervals);
        combinedIntervals.intersect(timingIntervals);


        if (elevationIntervals.isEmpty()) {
            obsMessage = String.format("%s will not meet the elevation constraints this night.", prefix);
        } else if (timingIntervals.isEmpty()) {
            obsMessage = String.format("%s will not meet the timing constraints this night.", prefix);
        } else if (combinedIntervals.isEmpty()) {
            obsMessage = String.format("%s will not meet the elevation and timing constraints this night.", prefix);
        } else {
            // We seem to have a time interval (or more) we can use.  But, maybe
            // it's already too late.  Intersect with an interval from now
            // until the end of the night.
            Union<Interval> combinedRemainingIntervals = new Union<Interval>(combinedIntervals);
            if (night.includes(curTime)) {
                Union<Interval> remainingNight = new Union<Interval>();
                remainingNight.add(new Interval(curTime, night.getEndTime()));
                combinedRemainingIntervals.intersect(remainingNight);
            }

            if (combinedRemainingIntervals.isEmpty()) {
                obsMessage = String.format("%s is no longer observable tonight (but was observable %s).", prefix,
                                           _formatTimeIntervals(combinedIntervals, curTime));
            } else {
                obsMessage = String.format("%s is observable tonight %s.", prefix,
                                           _formatTimeIntervals(combinedRemainingIntervals, curTime));
            }
        }

        // Blah.  Swing can't seem to make a multiline label without a lot of
        // grief.  So split the two sentences in an html message .... grim
        return String.format("<html>%s<br><b>%s</b></html>", positionMessage, obsMessage);
    }

    private static String _formatTimeIntervals(Union<Interval> intervals, long now) {
        StringBuilder buf = new StringBuilder();

        String sep = "";
        for (Interval inv : intervals) {
            buf.append(sep);

            buf.append("from ");
            long start = inv.getStart();
            if (now == start) {
                buf.append("now");
            } else {
                buf.append(formatTimeOnly(start));
            }

            buf.append(" until ");
            long end = inv.getEnd();
            if (now == end) {
                buf.append("now");
            } else {
                buf.append(formatTimeOnly(end));
            }

            sep = ", and ";
        }

        return buf.toString();
    }

//    private static String _formatTimeInterval(Interval inv) {
//        StringBuilder buf = new StringBuilder();
//
//        buf.append(Util.formatDateAndTime(inv.getStart()));
//        buf.append(" - ");
//        buf.append(Util.formatDateAndTime(inv.getEnd()));
//
//        return buf.toString();
//    }

    private static String _formatObsConditions(ObsSchedulingReport report) {
        SPSiteQuality sq = report.getSiteQuality();
        if (sq == null) return "";

        SPSiteQuality.SkyBackground bg = sq.getSkyBackground();
        SPSiteQuality.CloudCover    cc = sq.getCloudCover();
        SPSiteQuality.ImageQuality  iq = sq.getImageQuality();
        SPSiteQuality.WaterVapor    wv = sq.getWaterVapor();

        StringBuilder buf = new StringBuilder();
        buf.append("BG=");
        buf.append(bg == SPSiteQuality.SkyBackground.ANY ? "Any" : bg.getPercentage());
        buf.append(", CC=");
        buf.append(cc == SPSiteQuality.CloudCover.ANY ? "Any" : cc.getPercentage());
        buf.append(", IQ=");
        buf.append(iq == SPSiteQuality.ImageQuality.ANY ? "Any" : iq.getPercentage());
        buf.append(", WV=");
        buf.append(wv == SPSiteQuality.WaterVapor.ANY ? "Any" : wv.getPercentage());

        return buf.toString();
    }

    private static final long MS_PER_SECOND = 1000;
    private static final long MS_PER_MINUTE = MS_PER_SECOND * 60;
    private static final long MS_PER_HOUR = MS_PER_MINUTE * 60;

    private static String _formatTimingWindow(SPSiteQuality.TimingWindow win) {
        StringBuilder buf = new StringBuilder();

        long start = win.getStart();
        buf.append(formatDateAndTime(start));

        long duration = win.getDuration();
        if (duration < 0) {
            buf.append(" - forever");
        } else {
            long end = start + duration;
            buf.append(" - ").append(formatDateAndTime(end));

            int repeat = win.getRepeat();
            if (repeat != SPSiteQuality.TimingWindow.REPEAT_NEVER) {
                buf.append(", repeats ");
                if (repeat == SPSiteQuality.TimingWindow.REPEAT_FOREVER) {
                    buf.append("forever");
                } else {
                    buf.append(repeat).append(" times");
                }
                buf.append(" with a period of ");
                long ms = win.getPeriod();
                buf.append(String.format("%d:%02d", ms / MS_PER_HOUR, (ms % MS_PER_HOUR) / MS_PER_MINUTE));
                buf.append("(hrs:min)");
            }
        }

        return buf.toString();
    }
}
