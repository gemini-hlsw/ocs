//
// $Id: TimeConverter.java 231 2005-11-15 20:11:55Z shane $
//

package edu.gemini.datasetfile.impl;

import edu.gemini.fits.Header;
import edu.gemini.fits.HeaderItem;
import edu.gemini.fits.DefaultHeaderItem;
import edu.gemini.datasetfile.DatasetFileException;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 *
 */
final class TimeConverter {
    private static final Logger LOG = Logger.getLogger(TimeConverter.class.getName());

    private TimeConverter() {
    }

    static final String RELEASE  = "RELEASE";

    // Various date keywords used by the instruments
    static final String DATE_OBS = "DATE-OBS";
    static final String UTC_DATE = "UTC_DATE";
    static final String UTDATE   = "UTDATE";
    static final String DATE     = "DATE";

    // Various time keywords
    static final String TIME_OBS = "TIME-OBS";
    static final String UTSTART  = "UTSTART";
    static final String UT       = "UT";

    static final Set<String> KEYWORDS;

    static {
        Set<String> tmp = new TreeSet<String>();
        tmp.add(RELEASE);
        tmp.add(DATE_OBS);
        tmp.add(UTC_DATE);
        tmp.add(UTDATE);
        tmp.add(TIME_OBS);
        tmp.add(UTSTART);
        tmp.add(UT);
        KEYWORDS = Collections.unmodifiableSet(tmp);
    }

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    static {
        DATE_FORMAT.setTimeZone(UTC);
        TIME_FORMAT.setTimeZone(UTC);
    }

    private static final Pattern TIME_PAT = Pattern.compile("(\\d\\d:\\d\\d:\\d\\d)(\\.(\\d)\\d*)?");

    static Date getObsDate(Header h) throws DatasetFileException {
        // The obs date can be one of several different header keywords.
        HeaderItem dateItem = null;

        String[] dateKeys = {DATE_OBS, UTC_DATE, UTDATE, DATE};
        String dateKey = DATE_OBS;
        for (String key : dateKeys) {
            dateItem = h.get(key);
            if (dateItem != null) {
                dateKey = key;
                break;
            }
        }
        if (dateItem == null) {
            throw new DatasetFileException("missing observe date");
        }

        String dateVal = dateItem.getValue();
        if (dateVal == null) {
            throw new DatasetFileException("missing value of '"+ dateKey +'\'');
        }

        try {
            synchronized (DATE_FORMAT) {
                return DATE_FORMAT.parse(dateVal);
            }
        } catch (ParseException ex) {
            LOG.log(Level.INFO, ex.getMessage(), ex);
            throw new DatasetFileException("illegal '" + dateKey + '\'');
        }
    }

    static Date getObsTime(Header h) throws DatasetFileException {
        // The obs time can be one of several different header keywords.
        HeaderItem timeItem = null;

        String[] timeKeys = {TIME_OBS, UT, UTSTART};
        String timeKey = DATE_OBS;
        for (String key : timeKeys) {
            timeItem = h.get(key);
            if (timeItem != null) {
                timeKey = key;
                break;
            }
        }
        if (timeItem == null) {
            throw new DatasetFileException("missing observe time");
        }

        String timeVal = timeItem.getValue();
        if (timeVal == null) {
            throw new DatasetFileException("missing value of '"+ timeKey +'\'');
        }

        Matcher m = TIME_PAT.matcher(timeVal);
        if (!m.matches()) {
            throw new DatasetFileException("illegal '" + timeKey + '\'');
        }
        String hmsPart = m.group(1);

        Date time;
        try {
            synchronized (TIME_FORMAT) {
                time = TIME_FORMAT.parse(hmsPart);
            }
        } catch (ParseException ex) {
            LOG.log(Level.INFO, ex.getMessage(), ex);
            throw new DatasetFileException("illegal '" + timeKey + '\'');
        }

        // If there is a trailing decimal, add that many milliseconds
        // SW: Something not right about this, discarding ms for now
//        if (m.groupCount() == 3) {
//            int ms = Integer.parseInt(m.group(3)) * 100;
//            time.setTime(time.getTime() + ms);
//        }

        return time;
    }

    static Date getTimestamp(Header h) throws DatasetFileException {
        Date d = getObsDate(h);
        d.setTime(d.getTime() + getObsTime(h).getTime());
        return d;
    }

    static Date getReleaseDate(Header h) {
        HeaderItem dateItem = h.get(RELEASE);
        if (dateItem == null) return null;

        String dateVal = dateItem.getValue();
        if (dateVal == null) return null;

        try {
            synchronized (DATE_FORMAT) {
                return DATE_FORMAT.parse(dateVal);
            }
        } catch (ParseException ex) {
            LOG.log(Level.INFO, ex.getMessage(), ex);
            return null;
        }
    }

    static HeaderItem toReleaseItem(Date date) {
        final String val     = formatDate(date);
        final String comment = "End of proprietary period YYYY-MM-DD";
        return DefaultHeaderItem.create(RELEASE, val, comment);
    }

    static String formatDate(Date date) {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(date);
        }
    }

    static String formatTime(Date time) {
        synchronized (TIME_FORMAT) {
            return TIME_FORMAT.format(time);
        }
    }
}
