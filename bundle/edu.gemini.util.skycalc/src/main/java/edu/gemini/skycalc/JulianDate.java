//
// $Id: JulianDate.java 6519 2005-07-24 00:39:18Z shane $
//

/*
 *	Copyright (c) 1986-2002, Hiram Clawson - curator@hiram.ws.NoSpam
 *	All rights reserved.
 *
 *	Redistribution and use in source and binary forms, with or
 *	without modification, are permitted provided that the following
 *	conditions are met:
 *
 *		Redistributions of source code must retain the above
 *		copyright notice, this list of conditions and the
 *		following disclaimer.
 *
 *		Redistributions in binary form must reproduce the
 *		above copyright notice, this list of conditions and
 *		the following disclaimer in the documentation and/or
 *		other materials provided with the distribution.
 *
 *		Neither name of The Museum of Hiram nor the names of
 *		its contributors may be used to endorse or promote products
 *		derived from this software without specific prior
 *		written permission.
 *
 *	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 *	CONTRIBUTORS ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
 *	INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *	MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *	IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 *	INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *	(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 *	OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 *	HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 *	STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 *	IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 *	THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.gemini.skycalc;

import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * JulianDate class (modified from C source by Hiram Clawson).
 */
public final class JulianDate implements Comparable {
    /** Julian date at standard epoch. */
    public static final JulianDate J2000 = new JulianDate(2451545.0);

    private final double jd;
    private final long timestamp;

    public JulianDate(long timestamp) {
        this.timestamp = timestamp;
        jd = toJulianDate(timestamp);
    }

    public JulianDate(double julianDate) {
        timestamp = toTimestamp(julianDate);
        jd = julianDate;
    }

    public double toDouble() { return jd; }
    public long toTimestamp() { return timestamp; }
    public Date toDate() {
        return new Date(timestamp);
    }

    public String toString() {
        return Double.toString(jd);
    }

    public boolean equals(Object other) {
        if (other == null) return false;
        if (other.getClass() != this.getClass()) return false;

        final JulianDate that = (JulianDate) other;
        return that.timestamp == timestamp;
    }

    public int hashCode() {
        return (int)(timestamp ^ (timestamp >>> 32));
    }

    public int compareTo(Object o) {
        final JulianDate that = (JulianDate) o;
        return (timestamp<that.timestamp ? -1 : (timestamp==that.timestamp ? 0 : 1));
    }

    /**
     * Converts a double representing a Julian date to a Java Date.
     */
    public static long toTimestamp(double julianDate) {
        int jd = (int) (julianDate + 0.5); // integer julian date
        double frac = julianDate + 0.5 - (double) jd + 1.0e-10; // day fraction
        int ka = jd;
        if (jd >= 2299161) {
            int ialp = (int) (((double) jd - 1867216.25) / 36524.25);
            ka = jd + 1 + ialp - ( ialp >> 2 );
        }
        int kb = ka + 1524;
        int kc = (int) (((double) kb - 122.1) / 365.25);
        int kd = (int) (kc * 365.25);
        int ke = (int) (((double)(kb - kd))/30.6001);

        int day = (kb - kd - ((int) ((double) ke * 30.6001)));

        int month;
        if (ke > 13) {
            month = ke - 13;
        } else {
            month = ke - 1;
        }

        if ((month == 2) && (day > 28)) {
            day = 29;
        }

        int year;
        if ((month == 2) && (day == 29) && (ke == 3L)) {
            year = kc - 4716;
        } else if (month > 2) {
            year = kc - 4716;
        } else {
            year = kc - 4715;
        }

        double d_hour = frac * 24.0;
        int    i_hour = (int) d_hour;

        double d_minute = (d_hour - (double) i_hour) * 60.0;
        int    i_minute = (int) d_minute;

        double d_second = (d_minute - (double) i_minute) * 60.0;
        int    i_second = (int) d_second;

        int milli = (int) ((d_second - (double) i_second) * 1000.0);


        Calendar cal = new GregorianCalendar(ImprovedSkyCalcMethods.UTC);
        cal.clear();
        // Set the time fields (recall month is 0 based in Calendar
        cal.set(year, month-1, day, i_hour, i_minute, i_second);
        cal.set(Calendar.MILLISECOND, milli);
        return cal.getTimeInMillis();
    }


    /**
     * Converts a Java date to a Julian dates.
     */
    public static double toJulianDate(long timestamp) {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(timestamp);
        cal.setTimeZone(ImprovedSkyCalcMethods.UTC);

        int year  = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1; // calendar is 0 based
        int day   = cal.get(Calendar.DAY_OF_MONTH);
        int hour  = cal.get(Calendar.HOUR_OF_DAY);
        int min   = cal.get(Calendar.MINUTE);
        int second= cal.get(Calendar.SECOND);
        int milli = cal.get(Calendar.MILLISECOND);

        int jd;

        // decimal day fraction
        double frac = (hour/ 24.0) + (min / 1440.0) + (second / 86400.0) +
                      (milli / 86400000.0);

        // convert date to format YYYY.MMDDdd
        double gyr = (double) year + (0.01 * month) + (0.0001 * day) +
                     (0.0001 * frac) + 1.0e-9;

        int iy0, im0;

        // conversion factors
        if (month <= 2) {
            iy0 = year - 1;
            im0 = month + 12;
        } else {
            iy0 = year;
            im0 = month;
        }
        int ia = iy0 / 100;
        int ib = 2 - ia + (ia >> 2);

        // calculate julian date
        if (year <= 0) {
            jd = (int) ((365.25 * (double) iy0) - 0.75)
                    + (int) (30.6001 * (im0 + 1) )
                    + day + 1720994;
        } else {
            jd = (int) (365.25 * (double) iy0)
                    + (int) (30.6001 * (double) (im0 + 1))
                    + day + 1720994;
        }
        if ( gyr >= 1582.1015 )	{
            /* on or after 15 October 1582	*/
            jd += ib;
        }

        return (double) jd + frac + 0.5;
    }
}

