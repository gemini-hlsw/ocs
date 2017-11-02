// Copyright 2003 Association for Universities for Research in
// Astronomy, Inc., Observatory Control System, Gemini Telescopes
// Project.
//
// $Id: SunRiseSet.java 8039 2007-08-07 20:32:06Z swalker $

package edu.gemini.skycalc;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import edu.gemini.shared.util.CalendarUtil;
import edu.gemini.spModel.core.Site;

/**
 * Utility class for calculating the times for sunrise, sunset and twilight for a given location and date.
 * Based on the algorithm found <a href="http://williams.best.vwh.net/sunrise_sunset_algorithm.htm">here</a>.
 *
 * @version $Revision: 8039 $
 * @author Allan Brighton
 */
public class SunRiseSet {

    public final long timestamp;
    public final Site site;

    public final long sunset;
    public final long sunrise;
    public final long civilTwilightStart;
    public final long civilTwilightEnd;
    public final long nauticalTwilightStart;
    public final long nauticalTwilightEnd;
    public final long astronomicalTwilightStart;
    public final long astronomicalTwilightEnd;

    /**
     * Calculates the times for sunrise, sunset and twilights for the given date and site.
     */
    public SunRiseSet(Long timestamp, Site site) {
        this.timestamp = timestamp;
        this.site      = site;

        Calendar cal = Calendar.getInstance(site.timezone());
        cal.setTimeInMillis(timestamp);

        TwilightBoundedNight official     = new TwilightBoundedNight(TwilightBoundType.OFFICIAL, timestamp, site);
        TwilightBoundedNight civil        = new TwilightBoundedNight(TwilightBoundType.CIVIL, timestamp, site);
        TwilightBoundedNight nautical     = new TwilightBoundedNight(TwilightBoundType.NAUTICAL, timestamp, site);
        TwilightBoundedNight astronomical = new TwilightBoundedNight(TwilightBoundType.ASTRONOMICAL, timestamp, site);

        // calculate the times for nautical and astronomical
        this.sunset                    = official.getStartTime();
        this.sunrise                   = official.getEndTime();
        this.civilTwilightStart        = civil.getStartTime();
        this.civilTwilightEnd          = civil.getEndTime();
        this.nauticalTwilightStart     = nautical.getStartTime();
        this.nauticalTwilightEnd       = nautical.getEndTime();
        this.astronomicalTwilightStart = astronomical.getStartTime();
        this.astronomicalTwilightEnd   = astronomical.getEndTime();
    }

    // Shortcut for math functions in degrees
    private static double _sin(double d) {
        return Math.sin(Math.toRadians(d));
    }

    private static double _cos(double d) {
        return Math.cos(Math.toRadians(d));
    }

    private static double _tan(double d) {
        return Math.tan(Math.toRadians(d));
    }

    private static double _asin(double d) {
        return Math.toDegrees(Math.asin(d));
    }

    private static double _acos(double d) {
        return Math.toDegrees(Math.acos(d));
    }

    private static double _atan(double d) {
        return Math.toDegrees(Math.atan(d));
    }


    // Returns the time for sunrise (if rise is true) or sunset (if false),
    // using the given zenith (in degrees):
    //    offical      = 90 degrees 50'
    //	  civil        = 96 degrees
    //    nautical     = 102 degrees
    //    astronomical = 108 degrees
    // Assumes sunrise is on next day.
    private long _calcTime(boolean rise, double zenith, int dayOfYear,
                           double lngHour, double latitude) {

        if (rise) {
            dayOfYear++;
        }
        double t = (rise ? (dayOfYear + ((6. - lngHour) / 24.)) : (dayOfYear + ((18. - lngHour) / 24.)));

        // calculate the Sun's mean anomaly
        double m = (0.9856 * t) - 3.289;

        // calculate the Sun's true longitude
        double l = m + (1.916 * _sin(m)) + (0.020 * _sin(2 * m)) + 282.634;

        // l potentially needs to be adjusted into the range [0,360)
        l = _normalize(l, 360.);

        // calculate the Sun's right ascension
        double ra = _atan(0.91764 * _tan(l));

        // ra potentially needs to be adjusted into the range [0,360)
        ra = _normalize(ra, 360.);

        // right ascension value needs to be in the same quadrant as l
        double lQuadrant = (Math.floor(l / 90)) * 90;
        double raQuadrant = (Math.floor(ra / 90)) * 90;
        ra += (lQuadrant - raQuadrant);

        // right ascension value needs to be converted into hours
        ra /= 15;

        // calculate the Sun's declination
        double sinDec = 0.39782 * _sin(l);
        double cosDec = _cos(_asin(sinDec));

        // calculate the Sun's local hour angle
        double cosH = (_cos(zenith) - (sinDec * _sin(latitude))) / (cosDec * _cos(latitude));
        // This should never happen if site is constrained to GN and GS.
        if (cosH > 1) {
            // the sun never rises on this location (on the specified date)
            throw new RuntimeException("Sun never rises at " + site + " on " + new Date(timestamp));
        } else if (cosH < -1) {
            // the sun never sets on this location (on the specified date)
            throw new RuntimeException("Sun never sets at " + site + " on " + new Date(timestamp));
        }

        // finish calculating H and convert into hours
        double h = (rise ? (360 - _acos(cosH)) : _acos(cosH)) / 15.;

        // calculate local mean time of rising/setting
        t = h + ra - (0.06571 * t) - 6.622;

        // adjust back to UTC
        double ut = t - lngHour;

        // UT potentially needs to be adjusted into the range [0,24) by adding/subtracting 24
//        ut = _normalize(ut, 24.);

        return _getDate(ut);
    }


    // Return a value between 0 and maxValue by possibly adding or subtracting maxValue
    private static double _normalize(double value, double maxValue) {
        if (value > maxValue)
            value -= maxValue;
        else if (value < 0)
            value += maxValue;
        return value;
    }

    // print out the date in the given time zone
//    private static String _fmt(Date date, TimeZone tz) {
//        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
//        dateFormat.setTimeZone(tz);
//        return dateFormat.format(date);
//    }

    // Return a Date object for the given UT hours (0..24).
    // If rise is true, use the next day.
    private long _getDate(double hours) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UT"));
        cal.setTimeInMillis(timestamp);
        int h = cal.get(Calendar.HOUR_OF_DAY);
        boolean nextDay = (hours < h);
        CalendarUtil.setHours(cal, hours, nextDay);
        return cal.getTimeInMillis();
    }
}


