//
// $Id: ObservingNight.java 7646 2007-03-02 12:55:17Z shane $
//
package edu.gemini.skycalc;

import edu.gemini.spModel.core.Site;

import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Contains information about a single observing night in a particular TimeZone.
 * This is an immutable class.  An ObservingNight is defined as a period of
 * time between {@link #LOCAL_NIGHT_END_HOUR} on one day and
 * {@link #LOCAL_NIGHT_END_HOUR} on the next day.  Therefore, in a time zone
 * that honors daylight savings time, it is sometimes longer and sometimes
 * shorter than 24 hours (also true of days which contain leap seconds).
 *
 * <p>This class contains several constructors used to make a new ObservngNight
 * instance that encompasses a particular time, either the current time or
 * a specific time.  A time zone is needed as well since the start and end of
 * the observing night is dependent on the time zone.
 */
public final class ObservingNight implements Night {

    /**
     * The hour, in the local time zone, at which the night is considered to
     * officially end.  Is expressed in terms of a 24 hour day, so 12 always
     * equals noon for example.
     */
    public static final int LOCAL_NIGHT_END_HOUR = 14;

    private static final DateFormat UTC_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    static {
        UTC_DATE_FORMAT.setTimeZone(UTC);
    }

    private Site _site;
    private long _endTime;

    /**
     * Constructs with the current time and the given {@link Site}.
     * The ObservingNight object will represent the ObservingNight
     * for the current instance in the provided <code>site</code>.
     */
    public ObservingNight(Site site) {
        this(site, System.currentTimeMillis());
    }

    /**
     * Constructs an ObservingNight that includes <code>time</code> at the
     * specified <code>site</code>.
     *
     * @param time instant that falls within the ObservingNight of interest
     * @param site location to which the ObservingNight is relative
     */
    public ObservingNight(Site site, long time) {
        _site = site;

        // Compute the time at the end of the night.  Start with the current
        // time in a Calendar using the local time zone.
        Calendar cal = new GregorianCalendar(site.timezone());
        cal.setTimeInMillis(time);

        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Adjust the date to be the date at the end of the night, local time.
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour >= LOCAL_NIGHT_END_HOUR) {
            // add a day
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Adjust the hour to be the end of the end of the night, local time.
        cal.set(Calendar.HOUR_OF_DAY, LOCAL_NIGHT_END_HOUR);

        // Get that time, which of course is UTC.
        _endTime = cal.getTimeInMillis();
    }

    /**
     * Gets the site to which the ObservingNight is relative.
     */
    public Site getSite() {
        return _site;
    }

    /**
     * Gets the time at which the ObservingNight starts.  The ObservingNight
     * will include this time.
     */
    public long getStartTime() {
        Calendar c = new GregorianCalendar(_site.timezone());
        c.setTimeInMillis(_endTime);

        c.add(Calendar.DAY_OF_YEAR, -1);
        return c.getTimeInMillis();
    }

    /**
     * Gets the time at which the ObservingNight ends (exclusive).  This
     * time falls outside of the ObservingNight range.
     */
    public long getEndTime() {
        return _endTime;
    }

    public long getTotalTime() {
        return _endTime - getStartTime();
    }

    /**
     * Gets the dark time for this observing night, according to the given
     * definition of twilight.
     */
    public Night getDarkTime(final TwilightBoundType boundType) {
        return TwilightBoundedNight.forObservingNight(boundType, this);
    }

    public boolean includes(long time) {
        return (getStartTime() <= time) && (time < _endTime);
    }

    public ObservingNight previous() {
        return new ObservingNight(_site, getStartTime() - 1);
    }

    public ObservingNight next() {
        return new ObservingNight(_site, getEndTime());
    }

    /**
     * Gets a formatted String representing the observing night.  The String
     * is of the form <code>YYMMDD</code> for the UTC date on which the night
     * ends.
     */
    public String getNightString() {
        // _endTime is outside of the bounds of the current observing night
        // so we want the date at a time an instant before _endTime
//        return UTC_DATE_FORMAT.format(_endTime);
        synchronized (UTC_DATE_FORMAT) {
            return UTC_DATE_FORMAT.format(new Date(_endTime-1));
        }
    }

    public String toString() {
        return getNightString();
    }


    /**
     * Parses a String returned by {@link #getNightString} into an
     * ObservingNight, if possible.  The <code>nightString</code> should be of
     * the form <code>YYYYMMDD</code>.
     *
     * @throws ParseException if there is a problem parsing the
     * <code>nightString</code>
     */
    public static ObservingNight parseNightString(String nightString, Site site)
            throws ParseException {
        Calendar c = new GregorianCalendar(site.timezone());
        synchronized (UTC_DATE_FORMAT) {
            c.setTime(UTC_DATE_FORMAT.parse(nightString));
        }
        return new ObservingNight(site, c.getTimeInMillis());
    }

    public boolean equals(Object o) {
        if (!(o instanceof ObservingNight)) return false;

        ObservingNight that = (ObservingNight) o;
        if (_endTime != that._endTime) return false;
        return _site.equals(that._site);
    }

    public int hashCode() {
        int res = _site.hashCode();
        return 37*res +  (int)(_endTime^(_endTime>>>32));
    }

    public static void main(String[] args) {
        Calendar c = GregorianCalendar.getInstance(TimeZone.getTimeZone("America/Santiago"));
        c.set(Calendar.YEAR,      2007);
        c.set(Calendar.MONTH,        1);
        c.set(Calendar.DATE,        28);
        c.set(Calendar.HOUR_OF_DAY, 13);
        c.set(Calendar.MINUTE,       0);
        c.set(Calendar.SECOND,       0);
        c.set(Calendar.MILLISECOND,  0);

        ObservingNight on;
        on = new ObservingNight(Site.GS, c.getTime().getTime());

        while ("20070228".equals(on.getNightString())) {
            c.add(Calendar.MILLISECOND, 1);
            on = new ObservingNight(Site.GS, c.getTime().getTime());
        }

        System.out.println(c.get(Calendar.YEAR));
        System.out.println(c.get(Calendar.MONTH));
        System.out.println(c.get(Calendar.DATE));
        System.out.println(c.get(Calendar.HOUR_OF_DAY));
        System.out.println(c.get(Calendar.MINUTE));
        System.out.println(c.get(Calendar.SECOND));
        System.out.println(c.get(Calendar.MILLISECOND));


//        System.out.println(on.getNightString());
//        try {
//            on = ObservingNight.parseNightString("20070228", SiteDesc.CERRO_PACHON);
//            System.out.println(on.getNightString());
//            on = ObservingNight.parseNightString("20070301", SiteDesc.CERRO_PACHON);
//            System.out.println(on.getNightString());
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }

    }
}
