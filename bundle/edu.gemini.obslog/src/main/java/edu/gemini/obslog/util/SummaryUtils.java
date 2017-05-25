package edu.gemini.obslog.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class SummaryUtils {

    /**
     * Return a string with the UTC time in the format /mm/dd/yy hh:mm:ss,
     * given the current UTC time in ms.
     */
    public static String formatUTCDateTime(long time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
        return _doFormatUTC(dateFormat, time);
    }

    public static String formatUTCTime(long time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        return _doFormatUTC(dateFormat, time);
    }

    private static String _doFormatUTC(SimpleDateFormat dateFormat, long time) {
        Date date = new Date(time);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

}
