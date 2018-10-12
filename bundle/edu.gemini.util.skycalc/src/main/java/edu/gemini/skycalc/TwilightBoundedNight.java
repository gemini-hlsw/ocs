package edu.gemini.skycalc;

import edu.gemini.spModel.core.Site;
import jsky.util.DateUtil;

import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.Calendar;

/**
 * The start and end of a particular night, bounded by twilight as defined by
 * a {@link TwilightBoundType}.
 */
public final class TwilightBoundedNight implements Night {
    private static final Logger LOG = Logger.getLogger(TwilightBoundedNight.class.getName());

    /**
     * Creates a {@link Night} bounded by twilight for the specified date.
     * It will be the night that starts on that date and ends on the following
     * day.
     *
     * @param type definition of twilight to use (how far below the horizon in
     * the west that the sun must be before night starts, and how far below
     * in the east before night ends)
     *
     * @param date day of month (where the first day is 1)
     * @param month month (use the Calendar constants -- in other words
     * January is 0 not 1)
     * @param year year of interest (AD)
     *
     * @param site the location on the planet to which the times refer
     *
     * @return TwilightBoundedNight starting on the specified date
     */
    public static TwilightBoundedNight forDate(TwilightBoundType type, int date, int month, int year, Site site) {

        Calendar c = Calendar.getInstance(site.timezone());

        // Set to midnight on that date
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DATE, date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        return new TwilightBoundedNight(type, c.getTimeInMillis(), site);
    }

    /**
     * Creates a {@link Night} bounded by twilight, depending upon the
     * specified definition of twilight.
     *
     * @param type definition of twilight to use (how far below the horizon in
     * the west that that the sun must be before night starts, and how far
     * below in the east before night ends)
     *
     * @param time time of interest; if during a night local time, the night
     * returned will be that same night, if during the day local time, it
     * will be the night that starts on that day and ends on the following
     * day
     *
     * @param site location on the planet to which the times refer
     */
    public static TwilightBoundedNight forTime(TwilightBoundType type, long time, Site site) {
        TwilightBoundedNight tonight;
        tonight = new TwilightBoundedNight(type, time, site);

        // If we're in the time from the dusk (inclusive) to midnight
        // (exclusive), return the current night
        if (tonight.includes(time)) return tonight;

        // A night sits between two days.  It starts on one day and ends on the
        // next day.  TwilightBoundedNight will use the time to get a date,
        // for example October 9.  So from midnight local time on the 9th of
        // October to 11:59:59 PM the night it constructs is the night of
        // October 9/10.

        // So it is possible that the current time is during the night from
        // October 8/9. In other words, the time is between midnight and dawn.
        // So construct "yesterday night" and check.
        Calendar cal = Calendar.getInstance(site.timezone());
        cal.setTimeInMillis(time);
        cal.add(Calendar.DAY_OF_YEAR, -1);

        long timeYesterday = cal.getTimeInMillis();
        TwilightBoundedNight yesterdayNight;
        yesterdayNight = new TwilightBoundedNight(type, timeYesterday, site);
        if (yesterdayNight.includes(time)) return yesterdayNight;

        // Okay, the current time is during the day so return the night that
        // is coming.
        return tonight;
    }

    /**
     * Calculates a {@link Night} that occurs within the given
     * {@link ObservingNight}, as bounded by twilight periods defined in the
     * {@link TwilightBoundType}.
     *
     * @param type definition of twilight to use (how far below the horizon in
     * the west that the sun must be before night starts, and how far below
     * in the east before night ends)
     *
     * @param obsNight the observing night that contains the twilight bounded
     * night
     */
    public static TwilightBoundedNight forObservingNight(TwilightBoundType type, ObservingNight obsNight) {
        return new TwilightBoundedNight(type, obsNight.getStartTime(), obsNight.getSite());
    }

    private TwilightBoundType _type;
    private Site _site;
    private long _start;
    private long _end;

    /**
     * Creates a {@link Night} bounded by twilight, depending upon the
     * specified definition of twilight.
     *
     * @param type definition of twilight to use (how far below the horizon in
     * the west that that the sun must be before night starts, and how far
     * below in the east before night ends)
     *
     * @param time time of interest, which is only used to obtain the day;
     * midnight and 11:59:59 PM work will both yield the same result--a Night
     * configured for the night beginning on the date contained in
     * <code>time</code> in the timezone specified in <code>desc</code>
     *
     * @param site site of interest (where the
     */
    public TwilightBoundedNight(TwilightBoundType type, long time, Site site) {
        _type = type;
        _site = site;

        Calendar c = Calendar.getInstance(site.timezone());
        c.setTimeInMillis(time);

        // Set to midnight.
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_YEAR, 1);

        // Get sunset.
        final JulianDate jdmid = new JulianDate(c.getTimeInMillis());

        // Sunrise/set take altitude into account whereas the twilights don't.
        //
        // "The various twilights (6,12,18) describe how bright the sky is, and this does not depend on the altitude of
        // the observer, however, the time of sunrise and sunset does.  For example, consider Hilo and Maunakea.  The
        // sky brightness above them is the same, while the time when they see the sun dip below the horizon is not"
        // -- Andrew Stephens 2017-11-14

        final double angle;
        switch (type) {
            case OFFICIAL:
                // Horizon geometric correction from p. 24 of the Skycalc manual: sqrt(2 * elevation / Re) (radians)
                angle = type.getHorizonAngle() + Math.sqrt(2.0 * site.altitude / ImprovedSkyCalcMethods.EQUAT_RAD) *
                        ImprovedSkyCalcMethods.DEG_IN_RADIAN; break;
            default: angle = type.getHorizonAngle(); break;
        }
        _calcTimes(angle, jdmid, site);
    }

    private void _calcTimes(double angle, JulianDate jdmid, Site desc) {
        Coordinates sun = ImprovedSkyCalcMethods.lpsun(jdmid);
        double rasun  = sun.getRaDeg();
        double decsun = sun.getDecDeg();

        double lat    =   desc.latitude;
        double longit = -(desc.longitude / 15.0); // skycalc wants hours

        double hasunset = ImprovedSkyCalcMethods.ha_alt(decsun, lat, -angle);

        if (hasunset > 900.) {  // flag for never sets
            LOG.log(Level.WARNING, "Sun up all night on: " + jdmid.toDate());
            return;
        }

        if (hasunset < -900.) {
            LOG.log(Level.WARNING, "Sun down all day on: " + jdmid.toDate());
            return;
        }

        double stmid = ImprovedSkyCalcMethods.lst(jdmid, longit);

        // initial guess
        double tmp = jdmid.toDouble() + ImprovedSkyCalcMethods.adj_time(rasun + hasunset-stmid)/24.;
        JulianDate jdset = new JulianDate(tmp);

        // more accurate
        jdset = ImprovedSkyCalcMethods.jd_sun_alt(-angle, jdset, lat, longit);
        if (jdset == null) {
            LOG.log(Level.WARNING, "Sun doesn't set on: " + jdmid.toDate());
            return;
        }

        _start = jdset.toTimestamp();

        // initial guess
        tmp = jdmid.toDouble() + ImprovedSkyCalcMethods.adj_time(rasun - hasunset-stmid)/24.;
        JulianDate jdrise = new JulianDate(tmp);

        // more accurate
        jdrise = ImprovedSkyCalcMethods.jd_sun_alt(-angle, jdrise, lat, longit);
        if (jdrise == null) {
            LOG.log(Level.WARNING, "Sun doesn't rise on: " + jdmid.toDate());
            return;
        }

        _end = jdrise.toTimestamp();
    }

    public TwilightBoundType getType() {
        return _type;
    }

    public Site getSite() {
        return _site;
    }

    public long getStartTime() {
        return _start;
    }

    public long getEndTime() {
        return _end;
    }

    /** Start time rounded to the nearest minute in the given timezone. */
    public long getStartTimeRounded(TimeZone zone) {
        return DateUtil.roundToNearestMinute(_start, zone);
    }

    /** End time rounded to the nearest minute in the given timezone. */
    public long getEndTimeRounded(TimeZone zone) {
        return DateUtil.roundToNearestMinute(_end, zone);
    }

    public long getTotalTime() {
        return _end - _start;
    }

    public boolean includes(long time) {
        return (_start <= time) && (time < _end);
    }

    public ObservingNight getObservingNight() {
        return new ObservingNight(_site, _start);
    }

    public TwilightBoundedNight previous() {
        return TwilightBoundedNight.forObservingNight(_type, getObservingNight().previous());
    }

    public TwilightBoundedNight next() {
        return TwilightBoundedNight.forObservingNight(_type, getObservingNight().next());
    }
}
