package edu.gemini.qpt.shared.util;


/**
 * Some constants and methods for dealing with times in ms.
 * @author rnorris
 */
public class TimeUtils {

    public static final long MS_PER_SECOND = 1000;
    public static final long MS_PER_MINUTE = MS_PER_SECOND * 60;
    public static final long MS_PER_HOUR = MS_PER_MINUTE * 60;
    public static final long MS_PER_DAY = MS_PER_HOUR * 24;
    
    public static String msToHHMM(long span) {
        boolean neg = span < 0;
        span = Math.abs(span);
        long hh = span / MS_PER_HOUR;
        long mm = (span % MS_PER_HOUR) / MS_PER_MINUTE;
        return (neg ? "-" : "") + (hh < 10 ? "0" : "") + hh + ":" + (mm < 10 ? "0" : "") + mm;
    }

    public static String msToHMM(long span) {
        boolean neg = span < 0;
        span = Math.abs(span);
        long hh = span / MS_PER_HOUR;
        long mm = (span % MS_PER_HOUR) / MS_PER_MINUTE;
        return (neg ? "-" : "") + hh + ":" + (mm < 10 ? "0" : "") + mm;
    }

    public static String msToHHMMSS(long span) {
        boolean neg = span < 0;
        span = Math.abs(span);
        long hh = span / MS_PER_HOUR;
        long mm = (span % MS_PER_HOUR) / MS_PER_MINUTE;
        long ss = (span % MS_PER_MINUTE) / MS_PER_SECOND;
        return  (neg ? "-" : "") + hh + ":" + (mm < 10 ? "0" : "") + mm + ":" + (ss < 10 ? "0" : "") + ss;
    }

    public static String msToMMSS(long span) {
        boolean neg = span < 0;
        span = Math.abs(span);
        long mm = span / MS_PER_MINUTE;
        long ss = (span % MS_PER_MINUTE) / MS_PER_SECOND;
        return  (neg ? "-" : "") + mm + ":" + (ss < 10 ? "0" : "") + ss;
    }

    public static String hoursToHHMMSS(double hours) {
        return msToHHMM((long) (MS_PER_HOUR * hours));
    }
    
}
