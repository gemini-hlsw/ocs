package edu.gemini.skycalc;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * This class contains a collection of routines used to calculate astronomical
 * data for the moon. (Copied from old edu.gemini.shared.util.LunarAstronomer.)
 */
public final class MoonCalc {
    public enum Phase {
        NEW(0.0),
        FIRST_QUARTER(0.25),
        FULL(0.5),
        LAST_QUARTER(0.75);

        public final double constant;
        Phase(double constant) { this.constant = constant; }
    }

    /**
     * Gives the JDE of the indicated phase of the moon.
     * This algorithm comes from pp 319-320 of "Astronomical Algorithms"
     * by Jean Meeus, ISBN 0-943396-35-2
     * This method does not apply certain corrections to the algorithm
     * that can amount to a large fraction of a day.
     * @param period Number of moon orbit periods since new moon of Jan 6, 2000.
     * Could be positive or negative.
     * @param phase desired phase
     * According to "Astronomical Algorithms" the period number can be estimated
     * by period = (year - 2000) * 12.3685
     * where year is a fractional year (e.g. mid feb 1977 = 1977.13)
     */
    public static JulianDate julianDateOfPhase(int period, Phase phase) {
        final double k = period + phase.constant;
        final double t = k / 1236.85;
        return new JulianDate(2451550.09765 + 29.530588853 * k + 0.0001337 * t * t - 0.000000150 * t * t * t + 0.00000000073 * t * t * t * t);
    }

    /**
     * Estimates the lunar period relative to new moon of Jan 6, 2000.
     * This period can be used as an argument to julianDateOfPhase().
     */
    public static int approximatePeriod(long timestamp) {
        final Calendar cal = new GregorianCalendar();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(timestamp);
        double year = cal.get(Calendar.YEAR) + ((double) (cal.get(Calendar.DAY_OF_YEAR)) / 365.0);
        double period = (year - 2000) * 12.3685;
        if (period < 0) {
            return (int) (period - .5);
        }
        else {
            return (int) (period + .5);
        }
    }

    /**
     * Returns the time of the specified phase of the moon for the
     * specified period.
     * @param period Number of moon orbit periods since new moon of Jan 6, 2000.
     * Could be positive or negative.
     * @param phase desired phase.
     * @return The time of the next phase as a timestamp
     */
    public static long getMoonTime(int period, Phase phase) {
        return julianDateOfPhase(period, phase).toTimestamp();
    }

}
