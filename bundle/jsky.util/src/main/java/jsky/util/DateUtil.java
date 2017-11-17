package jsky.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


/**
 * Time/Date related static utility methods
 */
public final class DateUtil {
    // Static access only.
    private DateUtil() {
    }

    /**
     * Return a string with the UTC time in the format yyyy-MMM-dd hh:mm:ss,
     * given the current UTC time in ms.
     */
    public static String formatUTC(long time) {
        Date date = new Date(time);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }


    /**
     * Return a string with the given UTC time in the format
     * YYYYMMDD.
     */
    public static String formatUTCyyyymmdd(long time) {
        Date date = new Date(time);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    /**
     * Algorithm to round a time to the nearest minute: add 30 seconds and then truncate seconds and milliseconds.
     */
    public static Date roundToNearestMinute(final Date date, final TimeZone zone) {
        final Calendar c = Calendar.getInstance(zone);
        c.setTime(date);
        c.add(Calendar.SECOND, 30);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}


