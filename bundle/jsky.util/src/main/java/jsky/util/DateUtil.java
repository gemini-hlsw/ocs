// Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: DateUtil.java 4414 2004-02-03 16:21:36Z brighton $
//

package jsky.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


/**
 * Time/Date related static utility methods
 */
public class DateUtil {

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
}


