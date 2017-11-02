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
}


