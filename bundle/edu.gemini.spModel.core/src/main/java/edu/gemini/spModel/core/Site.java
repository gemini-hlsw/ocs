package edu.gemini.spModel.core;

import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a Gemini site.
 */
public enum Site {
    // See http://www.gemini.edu/sciops/telescopes-and-sites/locations

    /** Gemini North */
    GN("Gemini North", "GN", "Mauna Kea", -155.46906, 19.823806, 4213, TimeZone.getTimeZone("Pacific/Honolulu")),
    /** Gemini South */
    GS("Gemini South", "GS", "Cerro Pachon", -70.736693, -30.24075, 2722, TimeZone.getTimeZone("America/Santiago"));

    public final String displayName;
    public final String abbreviation;
    public final String mountain;


    /** Longitude in degrees. */
    public final double longitude;

    /** Latitude in degrees. */
    public final double latitude;

    /** Altitude in meters. */
    public final double altitude;

    private final TimeZone timezone;

    private Site(String displayName, String abbreviation, String mountain, double longitude, double latitude, double altitude, TimeZone timezone) {
        this.displayName  = displayName;
        this.abbreviation = abbreviation;
        this.mountain     = mountain;
        this.longitude    = longitude;
        this.latitude     = latitude;
        this.altitude     = altitude;
        this.timezone     = timezone;
    }

    public TimeZone timezone() { return this.timezone; }

    public static Set<Site> SET_GS = EnumSet.of(GS);
    public static Set<Site> SET_GN = EnumSet.of(GN);
    public static Set<Site> SET_BOTH = EnumSet.of(GN, GS);
    public static Set<Site> SET_NONE = EnumSet.noneOf(Site.class);
    private static final Pattern NORTH_PATTERN = Pattern.compile("gn|mk|mko|north|mauna.?kea|gemini.?north");
    private static final Pattern SOUTH_PATTERN = Pattern.compile("gs|cp|cpo|south|cerro.?pachon|gemini.?south");

    /**
     * Contains the current site value, if any, based on the system property <code>edu.gemini.site</code> at the time
     * this class is loaded.
     */
    public static final Site currentSiteOrNull;

    private static final Logger LOG = Logger.getLogger(Site.class.getName());
    private static final String SITE_PROP = "edu.gemini.site";
    static {
        // N.B. this static block must appear after the patterns above are created
        final String prop = System.getProperty(SITE_PROP);
        currentSiteOrNull = tryParse(prop);
        LOG.info("Current site is " + currentSiteOrNull);
    }

    /**
     * Parse a given String and tries to match it to any of the sites.
     *
     * @param site String to parse
     * @return either one of the sites
     * @throws ParseException if cannot match the String to any of the sites
     */
    public static Site parse(String site) throws ParseException {
        if (site != null) {
            try {
                return Site.valueOf(site.toUpperCase());
            } catch (IllegalArgumentException ex) {
                final Matcher northMatcher = NORTH_PATTERN.matcher(site.toLowerCase());
                final Matcher southMatcher = SOUTH_PATTERN.matcher(site.toLowerCase());
                if (northMatcher.matches()) {
                    return Site.GN;
                } else if (southMatcher.matches()) {
                    return Site.GS;
                }
            }
        }
        throw new ParseException("Could not parse site: " + site, 0);
    }

    /**
     * Same as parse, but returns null on failure.
     * @param site
     * @return a Site, or null
     */
    public static Site tryParse(String site) {
        try {
            return parse(site);
        } catch (ParseException pe) {
            return null;
        }
    }

}
