package edu.gemini.obslog.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class SummaryUtils {

    /**
     * Return a string with the UTC time in the format yyyy-MMM-dd HH:mm:ss,
     * given the current UTC time in ms.
     */
    public static String formatUTCDateTime(long time) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss")
                .withZone(ZoneId.of("UTC"));
        return dateFormat.format(Instant.ofEpochMilli(time));
    }

    /**
     * Return a string with the UTC time in the format HH:mm:ss,
     * given the current UTC time in ms.
     */
    public static String formatUTCTime(long time) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss")
                .withZone(ZoneId.of("UTC"));
        return dateFormat.format(Instant.ofEpochMilli(time));
    }
}
