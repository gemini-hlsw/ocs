package edu.gemini.horizons.server.backend;

//$Id: CgiHorizonsConstants.java 895 2007-07-24 20:18:09Z anunez $
/**
 * Define a set of constants to be used to talk to the CGI
 * offered by the JPL Horizons service
 */
public final class CgiHorizonsConstants {
    public static final String HORIZONS_PROTOCOL = "https://";
    public static final String HORIZONS_SERVER = "ssd.jpl.nasa.gov";
    public static final String HORIZONS_CGI = "horizons_batch.cgi";
    public static final String HORIZONS_URL = HORIZONS_PROTOCOL + HORIZONS_SERVER + "/" + HORIZONS_CGI;

    public static final String BATCH = "batch";
    public static final String COMMAND = "COMMAND";
    public static final String EPHEMERIS = "MAKE_EPHEM";
    public static final String TABLE_TYPE = "TABLE_TYPE";
    public static final String OBSERVER_TABLE = "OBSERVER";
    public static final String START_TIME = "START_TIME";
    public static final String STOP_TIME = "STOP_TIME";
    public static final String STEP_SIZE = "STEP_SIZE";
    public static final String TABLE_FIELDS_ARG = "QUANTITIES";
    public static final String TABLE_FIELDS = "'1,3,8,9'";
    public static final String CSV_FORMAT = "CSV_FORMAT";
    public static final String CENTER = "CENTER";
    public static final String CENTER_COORD = "coord";
    public static final String COORD_TYPE = "COORD_TYPE";
    public static final String COORD_TYPE_GEO = "GEODETIC";
    public static final String SITE_COORD = "SITE_COORD";
    public static final String SITE_COORD_GS = "'289.23944,-30.237778,2.743'";
    public static final String SITE_COORD_GN = "'204.53094,19.823806,4.2134'";

    public static final String EXTRA_PRECISION = "extra_prec";
    public static final String TIME_DIGITS     = "time_digits";
    public static final String FRACTIONAL_SEC  = "FRACSEC";

    public static final String YES = "YES";
    public static final String NO = "NO";

    //These are the strings used to identify important blocks while parsing the output

    public static final String ORBITAL_ELEMENTS_KEYWORD = "EPOCH";
    public static final String MAJOR_PLANET_KEYWORD = "PHYSICAL PROPERTIES";
    public static final String MAJOR_PLANET_KEYWORD_2 = "PHYSICAL DATA";
    public static final String MULTIPLE_MAJOR_BODIES_KEYWORD = "Multiple major-bodies";
    public static final String MULTIPLE_MINOR_BODIES_KEYWORD = "Matching small-bodies";
    public static final String SPACECRAFT_KEYWORD = "SPACECRAFT";
    public static final String NO_RESULTS_KEYWORD = "No matches found";
    public static final String COMET_KEYWORD = "COMET";
    public static final String SPK_EPHEMERIS = "SPK-BASED EPHEMERIS";

}
